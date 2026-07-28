import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  AbstractControl,
  FormArray,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin, map, Observable, of, switchMap } from 'rxjs';
import { AcademicYearService } from '../academic-year.service';
import { environment } from '../../../../environments';
import { uniqueFieldValidator } from '../../../shared/validators/unique-field.validator';
import {
  academicYearDateRangeValidator,
  academicYearOverlapValidator,
  computeAcademicYearDateBounds,
  AcademicYearRange,
} from '../../../shared/validators/academic-year-date.validators';
import { SettingsService } from '../../settings/settings.service';
import {
  AcademicYearFullUpdateRequest,
  AcademicYearRequest,
  CohortSeatAllocationRequest,
  CohortSummary,
  LateFeeType,
  TermBillingSchedule,
  TermBillingScheduleRequest,
  TermInstance,
  TermInstanceStatus,
} from '../academic-year.model';
import { Course } from '../../course/course.model';
import { CourseService } from '../../course/course.service';
import { ToastService } from '../../../core/toast/toast.service';
import { PermissionService } from '../../../core/permissions/permission.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { ACADEMIC_YEAR_FORM_TOUR } from '../../../shared/tour/tours/academic-year.tours';
import { CmsTipsCardComponent, CmsTip } from '../../../shared/tips-card/tips-card.component';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-academic-year-form',
  standalone: true,
  imports: [
    RouterLink, ReactiveFormsModule, MatDialogModule,
    CmsTourButtonComponent, CmsTipsCardComponent, AppDatePipe,
  ],
  templateUrl: './academic-year-form.component.html',
  styleUrl: './academic-year-form.component.scss',
})
export class AcademicYearFormComponent implements OnInit {
  private readonly fb                  = inject(FormBuilder);
  protected readonly route             = inject(ActivatedRoute);
  private readonly router              = inject(Router);
  private readonly academicYearService = inject(AcademicYearService);
  private readonly settingsService     = inject(SettingsService);
  private readonly courseService       = inject(CourseService);
  private readonly toast               = inject(ToastService);
  private readonly tourService         = inject(TourService);
  private readonly destroyRef          = inject(DestroyRef);
  private readonly http                = inject(HttpClient);
  private readonly permissionService   = inject(PermissionService);
  private readonly dialog              = inject(MatDialog);

  protected readonly loading    = signal(false);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly isViewMode = signal(false);

  /** Default fee-collection advance window (days) used until the system configuration loads. */
  private static readonly DEFAULT_ADVANCE_DAYS = 30;
  protected readonly advanceDays = signal(AcademicYearFormComponent.DEFAULT_ADVANCE_DAYS);

  /** Every other academic year's date range (this one excluded), used to lock the date pickers. */
  protected readonly otherAcademicYears = signal<AcademicYearRange[]>([]);

  /** Mirrors the form's live value so `dateBounds` can recompute as a signal. */
  private readonly formValue = signal<Record<string, string | null | undefined>>({});

  /** <input type="date"> min/max for every date field — locks the picker instead of just flagging it after. */
  protected readonly dateBounds = computed(() =>
    computeAcademicYearDateBounds(this.formValue(), this.otherAcademicYears(), this.advanceDays()));

  protected readonly isCreateMode = computed(() => !this.isEditMode() && !this.isViewMode());

  // Term instances (loaded in edit + view mode)
  protected readonly termInstances    = signal<TermInstance[]>([]);
  protected readonly billingSchedules = signal<TermBillingSchedule[]>([]);
  protected readonly advancingOdd     = signal(false);
  protected readonly advancingEven    = signal(false);
  protected readonly generatingOdd    = signal(false);
  protected readonly generatingEven   = signal(false);

  protected readonly oddTermInstance  = computed(() => this.termInstances().find(t => t.termType === 'ODD')  ?? null);
  protected readonly evenTermInstance = computed(() => this.termInstances().find(t => t.termType === 'EVEN') ?? null);

  // Counselling toggle
  protected readonly togglingCounsellingId = signal<number | null>(null);

  // Live preview signals
  protected readonly previewName  = signal('');
  protected readonly previewStart = signal<string | null>(null);
  protected readonly previewEnd   = signal<string | null>(null);

  protected readonly TIPS: CmsTip[] = [
    { icon: 'label',       title: 'Naming convention',  subtitle: 'Use a hyphenated range like "2025-2026" — sorts naturally and is recognised across the app.' },
    { icon: 'date_range',  title: 'Term dates',          subtitle: 'Odd term typically June–Nov; Even term Dec–May. These can be updated later if needed.' },
    { icon: 'groups',      title: 'Seat allocation',      subtitle: 'Create intake cohorts for every active course/program and split seats between management and counselling quotas.' },
    { icon: 'payments',    title: 'Billing schedule',    subtitle: 'Due date drives overdue flags on student fees. Grace days allow a buffer before late fees apply.' },
    { icon: 'check_circle',title: 'Current Year',        subtitle: 'Only one year can be marked Current — used as the system-wide default.' },
  ];

  private academicYearId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name:      ['', [Validators.required, Validators.maxLength(100)]],
    startDate: ['', Validators.required],
    endDate:   ['', Validators.required],
    isCurrent: [false],

    oddStartDate: ['', Validators.required],
    oddEndDate:   ['', Validators.required],
    evenStartDate: ['', Validators.required],
    evenEndDate:   ['', Validators.required],

    oddDueDate:       ['', Validators.required],
    oddLateFeeType:   ['FLAT' as LateFeeType, Validators.required],
    oddLateFeeAmount: [0,  [Validators.required, Validators.min(0)]],
    oddGraceDays:     [0,  [Validators.required, Validators.min(0)]],

    evenDueDate:       ['', Validators.required],
    evenLateFeeType:   ['FLAT' as LateFeeType, Validators.required],
    evenLateFeeAmount: [0,  [Validators.required, Validators.min(0)]],
    evenGraceDays:     [0,  [Validators.required, Validators.min(0)]],

    seatAllocations: this.fb.array([]),
  }, {
    validators: [
      academicYearDateRangeValidator(() => this.advanceDays()),
      academicYearOverlapValidator(() => this.otherAcademicYears()),
    ],
  });

  constructor() {
    this.formValue.set(this.form.value);
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(v => {
        this.previewName.set((v.name ?? '').trim());
        this.previewStart.set(v.startDate || null);
        this.previewEnd.set(v.endDate || null);
        this.formValue.set(v);
      });
  }

  ngOnInit(): void {
    this.tourService.register('academic-year-form', ACADEMIC_YEAR_FORM_TOUR);
    const idParam  = this.route.snapshot.paramMap.get('id');
    const urlSegs  = this.route.snapshot.url.map(s => s.path);
    const isDetail = urlSegs.includes('detail');

    if (idParam) {
      this.academicYearId = Number(idParam);
      if (isDetail) {
        this.isViewMode.set(true);
        this.form.disable();
      } else {
        this.isEditMode.set(true);
      }
      this.loadForEditOrView();
    } else {
      this.loadActiveProgramsForCreate();
    }
    if (!this.isViewMode()) {
      this.loadAdvanceDaysConfig();
      this.loadOtherAcademicYears();
    }
    this.setupUniquenessValidators();
  }

  /**
   * Loads every other academic year's date range (excluding this one, when editing) so the date
   * pickers can be locked to a window that can't overlap an existing year — mirrors the backend's
   * AcademicYearRepository.existsOverlapping check.
   */
  private loadOtherAcademicYears(): void {
    this.fetchOtherAcademicYears().subscribe({
      next: (others) => {
        this.otherAcademicYears.set(others);
        this.form.updateValueAndValidity();
      },
      error: () => { /* leave empty — no extra picker restriction if this fails */ },
    });
  }

  /**
   * The list loaded on init can go stale if another admin creates/edits a conflicting academic
   * year while this form is open. The backend's own overlap check remains authoritative regardless,
   * but re-fetching right before submit (see onSubmit) keeps the inline check/picker lock as fresh
   * as practically possible at the moment it actually matters.
   */
  private fetchOtherAcademicYears(): Observable<AcademicYearRange[]> {
    return this.academicYearService.getAllAcademicYears().pipe(
      map(years => years
        .filter(y => y.id !== this.academicYearId)
        .map(y => ({ id: y.id, name: y.name, startDate: y.startDate, endDate: y.endDate }))),
    );
  }

  /**
   * Loads the configurable fee-collection advance window (how many days before a term starts its
   * due date may be set) so the inline date validators match the backend's rule exactly. Falls
   * back to the default if the config row is missing, mirroring TermBillingScheduleService.
   */
  private loadAdvanceDaysConfig(): void {
    this.settingsService.getByKey('fee.collection_advance_days').subscribe({
      next: (config) => {
        const parsed = Number(config.configValue);
        // Negative values would push "earliest allowed" past the term start, potentially
        // inverting the allowed range — clamp to 0, mirroring TermBillingScheduleService.
        if (!Number.isNaN(parsed)) {
          this.advanceDays.set(Math.max(0, parsed));
          this.form.updateValueAndValidity();
        }
      },
      error: () => { /* keep default advance window */ },
    });
  }

  private setupUniquenessValidators(): void {
    const nameCtrl = this.form.get('name');
    if (nameCtrl) {
      nameCtrl.setAsyncValidators(
        uniqueFieldValidator(this.http, `${environment.apiUrl}/academic-years/name-exists`, () => this.academicYearId)
      );
      nameCtrl.updateValueAndValidity({ emitEvent: false });
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }

    // Refresh the overlap-check list right before submitting — closes most of the window where
    // another admin's concurrent edit could go undetected client-side (backend remains the
    // authoritative check regardless).
    this.fetchOtherAcademicYears().subscribe({
      next: (others) => {
        this.otherAcademicYears.set(others);
        this.form.updateValueAndValidity();
        this.proceedWithSubmit();
      },
      error: () => this.proceedWithSubmit(),
    });
  }

  private proceedWithSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }

    this.saving.set(true);

    if (this.isEditMode()) {
      this.submitEdit();
    } else {
      this.submitCreate();
    }
  }

  /**
   * Edit mode updates the academic year's dates, both terms' dates, and both billing schedules in
   * one atomic backend call instead of three sequential ones. Sequential calls deadlocked when an
   * admin shrank (or widened) the academic year and its term together in the same save — each call
   * validated its own dates against the other's still-persisted, not-yet-updated value.
   */
  private submitEdit(): void {
    const v = this.form.value;
    const request: AcademicYearFullUpdateRequest = {
      name:      (v.name ?? '').trim(),
      startDate: v.startDate,
      endDate:   v.endDate,
      isCurrent: v.isCurrent ?? false,
      oddTerm:  { startDate: v.oddStartDate,  endDate: v.oddEndDate },
      evenTerm: { startDate: v.evenStartDate, endDate: v.evenEndDate },
      oddBilling:  { dueDate: v.oddDueDate,  lateFeeType: v.oddLateFeeType,  lateFeeAmount: v.oddLateFeeAmount,  graceDays: v.oddGraceDays },
      evenBilling: { dueDate: v.evenDueDate, lateFeeType: v.evenLateFeeType, lateFeeAmount: v.evenLateFeeAmount, graceDays: v.evenGraceDays },
    };

    this.academicYearService.updateAcademicYearFull(this.academicYearId!, request).pipe(
      switchMap(() => this.academicYearService.initializeCohorts(this.academicYearId!)),
      switchMap((allCohorts) => {
        const rows = this.seatAllocationControls();
        const updates = rows
          .map(row => {
            const code = row.get('courseCode')?.value as string;
            const cohort = allCohorts.find(c => c.courseCode === code);
            if (!cohort) return null;
            return this.academicYearService.updateCohortSeats(cohort.id, {
              totalSeats:           this.toSeatCount(row.get('totalSeats')?.value),
              managementPercentage: this.toPercentage(row.get('managementPercentage')?.value),
            });
          })
          .filter((u): u is NonNullable<typeof u> => u !== null);
        return updates.length ? forkJoin(updates) : of([]);
      })
    ).subscribe({
      next: () => {
        this.toast.success('Academic year updated');
        void this.router.navigate(['/academic-years']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to update');
        this.saving.set(false);
      },
    });
  }

  /**
   * Create mode has no deadlock risk — the academic year (and its auto-created terms) doesn't
   * exist yet, so there's no stale persisted state for the term/billing calls to collide with.
   */
  private submitCreate(): void {
    const v = this.form.value;
    const ayRequest: AcademicYearRequest = {
      name:      (v.name ?? '').trim(),
      startDate: v.startDate,
      endDate:   v.endDate,
      isCurrent: v.isCurrent ?? false,
      cohortSeatAllocations: this.buildSeatAllocations(),
    };

    this.academicYearService.createAcademicYear(ayRequest).pipe(
      switchMap(ay =>
        this.academicYearService.getTermInstancesByAcademicYear(ay.id).pipe(
          switchMap(terms => {
            const odd  = terms.find(t => t.termType === 'ODD');
            const even = terms.find(t => t.termType === 'EVEN');
            const updates = [];
            if (odd)  updates.push(this.academicYearService.updateTermInstance(odd.id,  { startDate: v.oddStartDate,  endDate: v.oddEndDate  }));
            if (even) updates.push(this.academicYearService.updateTermInstance(even.id, { startDate: v.evenStartDate, endDate: v.evenEndDate }));
            return (updates.length ? forkJoin(updates) : of([])).pipe(
              switchMap(() => {
                const oddBilling:  TermBillingScheduleRequest = { academicYearId: ay.id, termType: 'ODD',  dueDate: v.oddDueDate,  lateFeeType: v.oddLateFeeType,  lateFeeAmount: v.oddLateFeeAmount,  graceDays: v.oddGraceDays  };
                const evenBilling: TermBillingScheduleRequest = { academicYearId: ay.id, termType: 'EVEN', dueDate: v.evenDueDate, lateFeeType: v.evenLateFeeType, lateFeeAmount: v.evenLateFeeAmount, graceDays: v.evenGraceDays };
                return forkJoin([
                  this.academicYearService.createOrUpdateTermBillingSchedule(oddBilling),
                  this.academicYearService.createOrUpdateTermBillingSchedule(evenBilling),
                ]);
              }),
            );
          })
        )
      )
    ).subscribe({
      next: () => {
        this.toast.success('Academic year created');
        void this.router.navigate(['/academic-years']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to create');
        this.saving.set(false);
      },
    });
  }

  // ── Term status operations ────────────────────────────────────────────────────

  /** BR-53: consequence text shown before a term status advance — PLANNED→OPEN and OPEN→LOCKED
   *  each do something real behind the scenes, so an admin should see what before confirming. */
  private termAdvanceConfirmMessage(termType: 'ODD' | 'EVEN', next: TermInstanceStatus): string {
    const ayName = this.form.get('name')?.value ?? 'this academic year';
    const termLabel = `${termType} term for ${ayName}`;
    if (next === 'OPEN') {
      return `Opening the ${termLabel} will generate course offerings from the curriculum, `
        + `making it available for course registration and fee collection. Continue?`;
    }
    return `Locking the ${termLabel} is permanent and cannot be undone. It deactivates all `
      + `course offerings for this term. Make sure exam results are published and fee collection `
      + `is finalized first. Continue?`;
  }

  protected advanceTermStatus(termType: 'ODD' | 'EVEN'): void {
    const term = termType === 'ODD' ? this.oddTermInstance() : this.evenTermInstance();
    const next = term ? this.getNextStatus(term.status) : null;
    if (!term || !next) return;

    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: next === 'OPEN' ? 'Open Term' : 'Lock Term',
        message: this.termAdvanceConfirmMessage(termType, next),
        confirmText: next === 'OPEN' ? 'Open Term' : 'Lock Term',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) this.performAdvanceTermStatus(termType, term.id, next);
    });
  }

  private performAdvanceTermStatus(termType: 'ODD' | 'EVEN', termId: number, next: TermInstanceStatus): void {
    const advancing = termType === 'ODD' ? this.advancingOdd : this.advancingEven;
    advancing.set(true);
    this.academicYearService.updateTermInstance(termId, { status: next }).subscribe({
      next: () => {
        this.toast.success(`Term advanced to ${next}`);
        this.reloadTermInstances();
        advancing.set(false);
      },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to advance term status'); advancing.set(false); },
    });
  }

  protected generateEnrollments(termType: 'ODD' | 'EVEN'): void {
    const term = termType === 'ODD' ? this.oddTermInstance() : this.evenTermInstance();
    if (!term) return;
    const generating = termType === 'ODD' ? this.generatingOdd : this.generatingEven;
    generating.set(true);
    this.academicYearService.generateEnrollments(term.id).subscribe({
      next: (res) => { this.toast.success(`Generated ${res.enrollmentsCreated} enrollment(s)`); generating.set(false); },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to generate enrollments'); generating.set(false); },
    });
  }

  protected getNextStatus(current: TermInstanceStatus): TermInstanceStatus | null {
    if (current === 'PLANNED') return 'OPEN';
    if (current === 'OPEN')    return 'LOCKED';
    return null;
  }

  protected getStatusStepClass(current: TermInstanceStatus, step: TermInstanceStatus): string {
    const order: TermInstanceStatus[] = ['PLANNED', 'OPEN', 'LOCKED'];
    const ci = order.indexOf(current), si = order.indexOf(step);
    if (si < ci) return 'step--done';
    if (si === ci) return 'step--active';
    return 'step--pending';
  }

  protected canViewCourseOfferings(): boolean {
    return this.permissionService.has('COURSE_VIEW') || this.permissionService.has('COURSE_MANAGE');
  }

  /** Backend gates PUT /term-instances/{id} on SEMESTER_MANAGE, not COURSE_VIEW/COURSE_MANAGE —
   *  the Advance to OPEN/LOCKED action must match or a user sees a button that 403s on click. */
  protected canAdvanceTermStatus(): boolean {
    return this.permissionService.has('SEMESTER_MANAGE');
  }

  // ── Counselling toggle ────────────────────────────────────────────────────────

  protected toggleQuotaStatus(row: FormGroup, quota: 'MANAGEMENT' | 'COUNSELLING'): void {
    const cohortId = row.get('cohortId')?.value as number | null;
    if (!cohortId) return;
    this.togglingCounsellingId.set(cohortId * (quota === 'MANAGEMENT' ? -1 : 1));
    const closedField = quota === 'MANAGEMENT' ? 'managementClosed' : 'counsellingClosed';
    const dateField   = quota === 'MANAGEMENT' ? 'managementClosedDate' : 'counsellingClosedDate';
    const closing = !row.get(closedField)?.value;
    this.academicYearService.setQuotaStatus(cohortId, quota, closing).subscribe({
      next: (updated) => {
        row.patchValue({
          counsellingClosed:     updated.counsellingClosed,
          counsellingClosedDate: updated.counsellingClosedDate,
          managementClosed:      updated.managementClosed,
          managementClosedDate:  updated.managementClosedDate,
        });
        this.togglingCounsellingId.set(null);
        const label = quota === 'MANAGEMENT' ? 'Management' : 'Counselling';
        this.toast.success(closing ? `${label} seats locked` : `${label} seats reopened`);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to update quota status');
        this.togglingCounsellingId.set(null);
      },
    });
  }

  protected isToggling(row: FormGroup, quota: 'MANAGEMENT' | 'COUNSELLING'): boolean {
    const cohortId = row.get('cohortId')?.value as number | null;
    if (!cohortId) return false;
    const key = cohortId * (quota === 'MANAGEMENT' ? -1 : 1);
    return this.togglingCounsellingId() === key;
  }

  // ── Helpers ───────────────────────────────────────────────────────────────────

  protected getError(field: string): string {
    const ctrl = this.form.get(field);
    if (!ctrl?.errors || !ctrl.touched) return '';
    if (ctrl.errors['required'])   return 'This field is required';
    if (ctrl.errors['maxlength'])  return `Max ${ctrl.errors['maxlength'].requiredLength} characters`;
    if (ctrl.errors['min'])        return 'Must be 0 or more';
    if (ctrl.errors['duplicate'])  return 'This name already exists';
    if (ctrl.errors['dateOrder'])  return 'End date must be after start date';
    if (ctrl.errors['outOfAcademicYear']) return "Must fall within the academic year's dates";
    if (ctrl.errors['termOverlap']) return 'Overlaps with the other term\'s dates';
    if (ctrl.errors['dueDateRange']) {
      const { earliest, latest } = ctrl.errors['dueDateRange'];
      return `Due date must be between ${earliest} and ${latest}`;
    }
    if (ctrl.errors['academicYearOverlap']) {
      return `Dates overlap with academic year "${ctrl.errors['academicYearOverlap'].name}"`;
    }
    return '';
  }

  protected isInvalid(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!(ctrl?.invalid && ctrl.touched);
  }

  protected seatAllocationControls(): FormGroup[] {
    return this.seatAllocationArray.controls as FormGroup[];
  }

  protected getSeatTotal(row: AbstractControl): number {
    return Number(row.get('totalSeats')?.value ?? 0) || 0;
  }

  protected getDerivedManagementSeats(row: AbstractControl): number {
    const total = Number(row.get('totalSeats')?.value ?? 0) || 0;
    const pct   = Number(row.get('managementPercentage')?.value ?? 0) || 0;
    return Math.round(total * pct / 100);
  }

  protected getDerivedCounsellingSeats(row: AbstractControl): number {
    return this.getSeatTotal(row) - this.getDerivedManagementSeats(row);
  }

  protected getDerivedCounsellingPercentage(row: AbstractControl): number {
    const pct = Number(row.get('managementPercentage')?.value ?? 0) || 0;
    return Math.max(0, 100 - pct);
  }

  /** Grand totals across every course/program row — recomputed on each change detection pass since
   *  the underlying FormArray isn't itself a signal. */
  protected grandSeatTotals(): { total: number; management: number; counselling: number } {
    let total = 0, management = 0, counselling = 0;
    for (const row of this.seatAllocationControls()) {
      total       += this.getSeatTotal(row);
      management  += this.getDerivedManagementSeats(row);
      counselling += this.getDerivedCounsellingSeats(row);
    }
    return { total, management, counselling };
  }

  protected lateFeeLabel(type: string, amount: number): string {
    return type === 'FLAT' ? `₹${amount} flat` : `₹${amount}/day`;
  }

  // ── Private ───────────────────────────────────────────────────────────────────

  private get seatAllocationArray(): FormArray {
    return this.form.get('seatAllocations') as FormArray;
  }

  private loadActiveProgramsForCreate(): void {
    this.loading.set(true);
    this.courseService.getAll().subscribe({
      next: (courses) => {
        const sorted = courses.slice().sort((a, b) => a.name.localeCompare(b.name));
        this.setSeatAllocationRows(sorted);
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load courses for seat allocation');
        this.loading.set(false);
      },
    });
  }

  private loadForEditOrView(): void {
    if (!this.academicYearId) return;
    this.loading.set(true);

    forkJoin([
      this.academicYearService.getAcademicYearById(this.academicYearId),
      this.academicYearService.getTermInstancesByAcademicYear(this.academicYearId),
      this.academicYearService.getTermBillingSchedulesByAcademicYear(this.academicYearId),
      this.courseService.getAll(),
      this.academicYearService.getCohortsByAcademicYear(this.academicYearId),
    ]).subscribe({
      next: ([ay, terms, schedules, courses, cohorts]) => {
        const odd        = terms.find(t => t.termType === 'ODD');
        const even       = terms.find(t => t.termType === 'EVEN');
        const oddBilling  = schedules.find(b => b.termType === 'ODD');
        const evenBilling = schedules.find(b => b.termType === 'EVEN');

        this.form.patchValue({
          name: ay.name, startDate: ay.startDate, endDate: ay.endDate, isCurrent: ay.isCurrent,
          oddStartDate:  odd?.startDate  ?? '',
          oddEndDate:    odd?.endDate    ?? '',
          evenStartDate: even?.startDate ?? '',
          evenEndDate:   even?.endDate   ?? '',
          oddDueDate:       oddBilling?.dueDate       ?? '',
          oddLateFeeType:   oddBilling?.lateFeeType   ?? 'FLAT',
          oddLateFeeAmount: oddBilling?.lateFeeAmount ?? 0,
          oddGraceDays:     oddBilling?.graceDays     ?? 0,
          evenDueDate:       evenBilling?.dueDate       ?? '',
          evenLateFeeType:   evenBilling?.lateFeeType   ?? 'FLAT',
          evenLateFeeAmount: evenBilling?.lateFeeAmount ?? 0,
          evenGraceDays:     evenBilling?.graceDays     ?? 0,
        });

        this.termInstances.set(terms);
        this.billingSchedules.set(schedules);

        const sorted = courses.slice().sort((a, b) => a.name.localeCompare(b.name));
        this.setSeatAllocationRows(sorted, cohorts);
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load academic year');
        void this.router.navigate(['/academic-years']);
      },
    });
  }

  private reloadTermInstances(): void {
    if (!this.academicYearId) return;
    this.academicYearService.getTermInstancesByAcademicYear(this.academicYearId).subscribe({
      next: (terms) => this.termInstances.set(terms),
    });
  }

  private setSeatAllocationRows(courses: Course[], cohorts: CohortSummary[] = []): void {
    this.seatAllocationArray.clear();
    for (const course of courses) {
      const cohort = cohorts.find(c => c.courseCode === course.code);
      this.seatAllocationArray.push(this.fb.group({
        cohortId:              [cohort?.id ?? null],
        courseId:              [course.id, Validators.required],
        courseCode:            [course.code],
        courseName:            [course.name],
        totalSeats:            [cohort?.totalSeats ?? 0, [Validators.min(0)]],
        managementPercentage:  [cohort?.managementPercentage ?? 35, [Validators.min(0), Validators.max(100)]],
        counsellingClosed:     [cohort?.counsellingClosed     ?? false],
        counsellingClosedDate: [cohort?.counsellingClosedDate ?? null],
        managementClosed:      [cohort?.managementClosed      ?? false],
        managementClosedDate:  [cohort?.managementClosedDate  ?? null],
      }));
    }
  }

  private buildSeatAllocations(): CohortSeatAllocationRequest[] {
    return this.seatAllocationArray.controls.map(row => ({
      courseId:             Number(row.get('courseId')?.value),
      totalSeats:           this.toSeatCount(row.get('totalSeats')?.value),
      managementPercentage: this.toPercentage(row.get('managementPercentage')?.value),
    }));
  }

  private toSeatCount(value: unknown): number {
    if (value === '' || value == null) return 0;
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : 0;
  }

  private toPercentage(value: unknown): number {
    if (value === '' || value == null) return 0;
    const parsed = Number(value);
    if (!Number.isFinite(parsed)) return 0;
    return Math.max(0, Math.min(100, parsed));
  }
}

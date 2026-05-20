import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin, of, switchMap } from 'rxjs';
import { AcademicYearService } from '../academic-year.service';
import { LateFeeType, TermBillingScheduleRequest } from '../academic-year.model';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { ACADEMIC_YEAR_FORM_TOUR } from '../../../shared/tour/tours/academic-year.tours';
import { CmsTipsCardComponent, CmsTip } from '../../../shared/tips-card/tips-card.component';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';

@Component({
  selector: 'app-academic-year-form',
  standalone: true,
  imports: [
    RouterLink, ReactiveFormsModule,
    CmsTourButtonComponent, CmsTipsCardComponent, AppDatePipe,
  ],
  templateUrl: './academic-year-form.component.html',
  styleUrl: './academic-year-form.component.scss',
})
export class AcademicYearFormComponent implements OnInit {
  private readonly fb                 = inject(FormBuilder);
  private readonly route              = inject(ActivatedRoute);
  private readonly router             = inject(Router);
  private readonly academicYearService = inject(AcademicYearService);
  private readonly toast              = inject(ToastService);
  private readonly tourService        = inject(TourService);
  private readonly destroyRef         = inject(DestroyRef);

  protected readonly loading  = signal(false);
  protected readonly saving   = signal(false);
  protected readonly isEditMode = signal(false);

  // Live preview signals
  protected readonly previewName  = signal('');
  protected readonly previewStart = signal<string | null>(null);
  protected readonly previewEnd   = signal<string | null>(null);

  protected readonly TIPS: CmsTip[] = [
    { icon: 'label',       title: 'Naming convention',  subtitle: 'Use a hyphenated range like "2025-2026" — sorts naturally and is recognised across the app.' },
    { icon: 'date_range',  title: 'Term dates',          subtitle: 'Odd term typically June–Nov; Even term Dec–May. These can be updated later if needed.' },
    { icon: 'payments',    title: 'Billing schedule',    subtitle: 'Due date drives overdue flags on student fees. Grace days allow a buffer before late fees apply.' },
    { icon: 'check_circle',title: 'Current Year',        subtitle: 'Only one year can be marked Current — used as the system-wide default.' },
  ];

  private academicYearId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    // ── Academic Year ─────────────────────────────────────────────────────────
    name:      ['', [Validators.required, Validators.maxLength(100)]],
    startDate: ['', Validators.required],
    endDate:   ['', Validators.required],
    isCurrent: [false],

    // ── ODD Term ──────────────────────────────────────────────────────────────
    oddStartDate: ['', Validators.required],
    oddEndDate:   ['', Validators.required],

    // ── EVEN Term ─────────────────────────────────────────────────────────────
    evenStartDate: ['', Validators.required],
    evenEndDate:   ['', Validators.required],

    // ── ODD Billing ───────────────────────────────────────────────────────────
    oddDueDate:      ['', Validators.required],
    oddLateFeeType:  ['FLAT' as LateFeeType, Validators.required],
    oddLateFeeAmount: [0,  [Validators.required, Validators.min(0)]],
    oddGraceDays:    [0,  [Validators.required, Validators.min(0)]],

    // ── EVEN Billing ──────────────────────────────────────────────────────────
    evenDueDate:      ['', Validators.required],
    evenLateFeeType:  ['FLAT' as LateFeeType, Validators.required],
    evenLateFeeAmount: [0,  [Validators.required, Validators.min(0)]],
    evenGraceDays:    [0,  [Validators.required, Validators.min(0)]],
  });

  constructor() {
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(v => {
        this.previewName.set((v.name ?? '').trim());
        this.previewStart.set(v.startDate || null);
        this.previewEnd.set(v.endDate || null);
      });
  }

  ngOnInit(): void {
    this.tourService.register('academic-year-form', ACADEMIC_YEAR_FORM_TOUR);
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.academicYearId = Number(idParam);
      this.isEditMode.set(true);
      this.loadForEdit();
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }

    const v = this.form.value;
    const ayRequest = {
      name:      (v.name ?? '').trim(),
      startDate: v.startDate,
      endDate:   v.endDate,
      isCurrent: v.isCurrent ?? false,
    };

    this.saving.set(true);

    const ay$ = this.isEditMode()
      ? this.academicYearService.updateAcademicYear(this.academicYearId!, ayRequest)
      : this.academicYearService.createAcademicYear(ayRequest);

    ay$.pipe(
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
              })
            );
          })
        )
      )
    ).subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Academic year updated' : 'Academic year created');
        void this.router.navigate(['/academic-years']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? (this.isEditMode() ? 'Failed to update' : 'Failed to create'));
        this.saving.set(false);
      },
    });
  }

  protected getError(field: string): string {
    const ctrl = this.form.get(field);
    if (!ctrl?.errors || !ctrl.touched) return '';
    if (ctrl.errors['required'])   return 'This field is required';
    if (ctrl.errors['maxlength'])  return `Max ${ctrl.errors['maxlength'].requiredLength} characters`;
    if (ctrl.errors['min'])        return 'Must be 0 or more';
    return '';
  }

  protected isInvalid(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!(ctrl?.invalid && ctrl.touched);
  }

  private loadForEdit(): void {
    if (!this.academicYearId) return;
    this.loading.set(true);

    forkJoin([
      this.academicYearService.getAcademicYearById(this.academicYearId),
      this.academicYearService.getTermInstancesByAcademicYear(this.academicYearId),
      this.academicYearService.getTermBillingSchedulesByAcademicYear(this.academicYearId),
    ]).subscribe({
      next: ([ay, terms, schedules]) => {
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
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load academic year');
        void this.router.navigate(['/academic-years']);
      },
    });
  }
}

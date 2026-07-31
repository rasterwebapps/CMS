import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpClient } from '@angular/common/http';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { LabScheduleService } from '../lab-schedule.service';
import { ClassSessionType, DAYS_OF_WEEK, LabScheduleRequest } from '../lab-schedule.model';
import { environment } from '../../../../environments/environment';
import { ToastService } from '../../../core/toast/toast.service';
import { BatchService } from '../../batch/batch.service';
import { Batch } from '../../batch/batch.model';
import { ClassroomService } from '../../classroom/classroom.service';
import { Classroom } from '../../classroom/classroom.model';
import { PeriodService } from '../../period/period.service';
import { Period } from '../../period/period.model';
import { ClinicalVenueService } from '../../clinical-venue/clinical-venue.service';
import { ClinicalVenue } from '../../clinical-venue/clinical-venue.model';
import { CmsPreviewCardComponent } from '../../../shared/preview-card/preview-card.component';
import { CmsTipsCardComponent, CmsTip } from '../../../shared/tips-card/tips-card.component';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';

@Component({
  selector: 'app-lab-schedule-form',
  standalone: true,
  imports: [
    RouterLink, ReactiveFormsModule, CmsTourButtonComponent,
    MatCheckboxModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule,
    CmsPreviewCardComponent, CmsTipsCardComponent,
  ],
  templateUrl: './lab-schedule-form.component.html',
  styleUrl: './lab-schedule-form.component.scss',
})
export class LabScheduleFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly labScheduleService = inject(LabScheduleService);
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly batchService = inject(BatchService);
  private readonly classroomService = inject(ClassroomService);
  private readonly periodService = inject(PeriodService);
  private readonly clinicalVenueService = inject(ClinicalVenueService);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Class Schedule');
  protected readonly labs = signal<{ id: number; name: string }[]>([]);
  protected readonly subjects = signal<{ id: number; name: string; code: string; specialityId: number | null; specialityName: string | null }[]>([]);
  protected readonly faculty = signal<{ id: number; name: string; specialityId: number | null }[]>([]);
  protected readonly classrooms = signal<Classroom[]>([]);
  protected readonly periods = signal<Period[]>([]);
  protected readonly clinicalVenues = signal<ClinicalVenue[]>([]);
  protected readonly termInstances = signal<{ id: number; termType: string; startDate: string; endDate: string; academicYearName: string }[]>([]);
  protected readonly daysOfWeek = DAYS_OF_WEEK;
  protected readonly availableBatches = signal<Batch[]>([]);

  protected readonly sessionType = signal<ClassSessionType>('LAB');
  protected readonly isTheory = computed(() => this.sessionType() === 'THEORY');
  protected readonly isLab = computed(() => this.sessionType() === 'LAB');
  protected readonly isClinical = computed(() => this.sessionType() === 'CLINICAL');
  protected readonly isBatchScoped = computed(() => this.isLab() || this.isClinical());
  protected readonly slotFieldLabel = computed(() => {
    if (this.isTheory()) return 'Period';
    if (this.isClinical()) return 'Clinical Slot';
    return 'Lab Slot';
  });

  // Preview signals
  protected readonly previewLabId       = signal<number | null>(null);
  protected readonly previewSubjectId   = signal<number | null>(null);
  protected readonly previewFacultyId   = signal<number | null>(null);
  protected readonly previewClassroomId = signal<number | null>(null);
  protected readonly previewClinicalVenueId = signal<number | null>(null);
  protected readonly previewPeriodId    = signal<number | null>(null);
  protected readonly previewBatchId     = signal<number | null>(null);
  protected readonly previewBatch       = signal('');
  protected readonly previewDay         = signal('');
  protected readonly previewTermId      = signal<number | null>(null);
  protected readonly previewActive      = signal(true);
  protected readonly previewLabName     = computed(() => this.labs().find(l => l.id === this.previewLabId())?.name ?? '');
  protected readonly previewSubjectName = computed(() => {
    const c = this.subjects().find(x => x.id === this.previewSubjectId());
    return c ? c.name : '';
  });
  protected readonly previewFacultyName = computed(() => this.faculty().find(f => f.id === this.previewFacultyId())?.name ?? '');
  protected readonly previewTermLabel   = computed(() => {
    const t = this.termInstances().find(t => t.id === this.previewTermId());
    return t ? `${t.academicYearName} ${t.termType}` : '';
  });
  protected readonly previewSlotLabel   = computed(() => {
    const p = this.periods().find(x => x.id === this.previewPeriodId());
    return p ? `${p.startTime}-${p.endTime}` : '';
  });
  protected readonly previewClinicalVenueName = computed(() =>
    this.clinicalVenues().find(v => v.id === this.previewClinicalVenueId())?.name ?? '');
  protected readonly previewTheorySectionName = computed(() => {
    if (!this.isTheory()) return '';
    return this.availableBatches().find(b => b.id === this.previewBatchId())?.name ?? '';
  });
  protected readonly previewRoomName = computed(() => {
    if (this.isTheory()) {
      return this.classrooms().find(c => c.id === this.previewClassroomId())?.name ?? '';
    }
    if (this.isClinical()) {
      return this.previewClinicalVenueName();
    }
    return this.previewLabName();
  });

  /** Faculty must belong to the selected subject's own department (Speciality) to be
   *  assignable. Skipped (full list) when the subject has no speciality set. The row's original
   *  faculty (edit mode) stays selectable even if it predates the rule, so editing an unrelated
   *  field doesn't drop the currently-assigned faculty out of the dropdown. */
  protected readonly selectedSubjectId = signal<number | null>(null);
  private originalFacultyId: number | null = null;

  protected readonly selectedSubjectSpecialityName = computed(() => {
    const subject = this.subjects().find(s => s.id === this.selectedSubjectId());
    return subject?.specialityName ?? null;
  });

  protected readonly eligibleFaculty = computed(() => {
    const subject = this.subjects().find(s => s.id === this.selectedSubjectId());
    const specialityId = subject?.specialityId ?? null;
    if (!specialityId) return this.faculty();
    return this.faculty().filter(f => f.specialityId === specialityId || f.id === this.originalFacultyId);
  });

  protected readonly TIPS: CmsTip[] = [
    { icon: 'group',          title: 'Batch',     subtitle: 'Use a clear name (e.g., "Batch A") so students know which schedule applies to them.' },
    { icon: 'today',          title: 'Day & Slot', subtitle: 'Each room can host one schedule per Day + Slot — conflicts are blocked by the system.' },
    { icon: 'manage_accounts',title: 'Faculty',   subtitle: 'Pick a faculty member with subject expertise; they receive automatic calendar reminders.' },
  ];

  private itemId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    sessionType: ['LAB' as ClassSessionType, Validators.required],
    labId: [null as number | null],
    subjectId: [null, Validators.required],
    facultyId: [null, Validators.required],
    batchName: [''],
    batchId: [null],
    dayOfWeek: ['', Validators.required],
    termInstanceId: [null, Validators.required],
    isActive: [true],
    classroomId: [null as number | null],
    periodId: [null as number | null, Validators.required],
    clinicalVenueId: [null as number | null],
    courseOfferingId: [null as number | null],
  });

  private lastBatchLookupKey = '';

  constructor() {
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(v => {
        this.sessionType.set(v.sessionType ?? 'LAB');
        this.previewLabId.set(v.labId ?? null);
        this.previewSubjectId.set(v.subjectId ?? null);
        this.selectedSubjectId.set(v.subjectId ?? null);
        this.previewFacultyId.set(v.facultyId ?? null);
        this.previewClassroomId.set(v.classroomId ?? null);
        this.previewClinicalVenueId.set(v.clinicalVenueId ?? null);
        this.previewPeriodId.set(v.periodId ?? null);
        this.previewBatchId.set(v.batchId ?? null);
        this.previewBatch.set((v.batchName ?? '').trim());
        this.previewDay.set(v.dayOfWeek ?? '');
        this.previewTermId.set(v.termInstanceId ?? null);
        this.previewActive.set(!!v.isActive);

        const lookupKey = `${v.subjectId ?? ''}-${v.termInstanceId ?? ''}`;
        if (v.subjectId && v.termInstanceId && lookupKey !== this.lastBatchLookupKey) {
          this.lastBatchLookupKey = lookupKey;
          this.batchService.getBySubjectAndTerm(v.subjectId, v.termInstanceId).subscribe({
            next: (batches) => this.availableBatches.set(batches),
            error: () => this.availableBatches.set([]),
          });
        }
      });

    this.form.get('sessionType')?.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((type: ClassSessionType) => this.applySessionTypeValidators(type));

    this.applySessionTypeValidators('LAB');
  }

  private applySessionTypeValidators(type: ClassSessionType): void {
    const labId = this.form.get('labId');
    const batchName = this.form.get('batchName');
    const classroomId = this.form.get('classroomId');
    const clinicalVenueId = this.form.get('clinicalVenueId');

    labId?.clearValidators();
    classroomId?.clearValidators();
    clinicalVenueId?.clearValidators();
    batchName?.clearValidators();

    if (type === 'THEORY') {
      classroomId?.setValidators(Validators.required);
    } else if (type === 'CLINICAL') {
      clinicalVenueId?.setValidators(Validators.required);
      batchName?.setValidators([Validators.required, Validators.maxLength(100)]);
    } else {
      labId?.setValidators(Validators.required);
      batchName?.setValidators([Validators.required, Validators.maxLength(100)]);
    }
    labId?.updateValueAndValidity({ emitEvent: false });
    batchName?.updateValueAndValidity({ emitEvent: false });
    classroomId?.updateValueAndValidity({ emitEvent: false });
    clinicalVenueId?.updateValueAndValidity({ emitEvent: false });
  }

  protected onBatchSelect(batchId: number | null): void {
    const batch = this.availableBatches().find((b) => b.id === batchId);
    if (batch) {
      this.form.patchValue({ batchName: batch.name });
    }
  }

  ngOnInit(): void {
    this.http.get<{ id: number; name: string }[]>(`${environment.apiUrl}/labs`).subscribe({
      next: (data) => this.labs.set(data),
      error: () => { this.toast.error('Failed to load labs'); },
    });
    this.http.get<{ id: number; name: string; code: string; speciality: { id: number; name: string } | null }[]>(
      `${environment.apiUrl}/subjects`).subscribe({
      next: (data) => this.subjects.set(data.map((s) => ({
        id: s.id, name: s.name, code: s.code,
        specialityId: s.speciality?.id ?? null, specialityName: s.speciality?.name ?? null,
      }))),
      error: () => { this.toast.error('Failed to load subjects'); },
    });
    this.http.get<{ id: number; fullName: string; specialityId: number | null }[]>(`${environment.apiUrl}/faculty`).subscribe({
      next: (data) => this.faculty.set(data.map((f) => ({ id: f.id, name: f.fullName, specialityId: f.specialityId }))),
      error: () => { this.toast.error('Failed to load faculty'); },
    });
    this.classroomService.getAll(true).subscribe({
      next: (data) => this.classrooms.set(data),
      error: () => { this.toast.error('Failed to load classrooms'); },
    });
    this.periodService.getAll(true).subscribe({
      next: (data) => this.periods.set(data),
      error: () => { this.toast.error('Failed to load periods'); },
    });
    this.clinicalVenueService.getAll(true).subscribe({
      next: (data) => this.clinicalVenues.set(data),
      error: () => { this.toast.error('Failed to load clinical venues'); },
    });
    this.http.get<{ id: number; termType: string; startDate: string; endDate: string; academicYear: { id: number; name: string } }[]>(
      `${environment.apiUrl}/term-instances`).subscribe({
      next: (data) => this.termInstances.set(data.map(t => ({
        id: t.id,
        termType: t.termType,
        startDate: t.startDate,
        endDate: t.endDate,
        academicYearName: t.academicYear?.name ?? '',
      }))),
      error: () => { this.toast.error('Failed to load term instances'); },
    });

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.itemId = Number(id);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Class Schedule');
      this.loading.set(true);
      this.labScheduleService.getById(this.itemId).subscribe({
        next: (item) => {
          this.originalFacultyId = item.facultyId;
          this.form.patchValue({
            sessionType: item.sessionType,
            labId: item.labId,
            subjectId: item.subjectId,
            facultyId: item.facultyId,
            batchName: item.batchName ?? '',
            batchId: item.batchId,
            dayOfWeek: item.dayOfWeek,
            termInstanceId: item.termInstanceId,
            isActive: item.isActive,
            classroomId: item.classroomId,
            periodId: item.periodId,
            clinicalVenueId: item.clinicalVenueId,
            courseOfferingId: item.courseOfferingId,
          });
          this.applySessionTypeValidators(item.sessionType);
          this.loading.set(false);
        },
        error: () => { this.toast.error('Failed to load'); void this.router.navigate(['/lab-schedules']); },
      });
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) { scrollToFirstInvalid(this.form); return; }
    const v = this.form.value;
    const isBatchScoped = v.sessionType === 'LAB' || v.sessionType === 'CLINICAL';
    const request: LabScheduleRequest = {
      sessionType: v.sessionType,
      labId: v.sessionType === 'LAB' ? v.labId : null,
      subjectId: v.subjectId,
      facultyId: v.facultyId,
      batchName: isBatchScoped ? (v.batchName ?? '').trim() : null,
      // THEORY may also carry an optional batchId (R3 Phase 3) to scope that subject's Theory
      // schedule to one section instead of the whole cohort -- left null means "whole cohort".
      batchId: v.batchId ?? null,
      dayOfWeek: v.dayOfWeek,
      termInstanceId: v.termInstanceId,
      isActive: v.isActive,
      classroomId: v.sessionType === 'THEORY' ? v.classroomId : null,
      periodId: v.periodId,
      clinicalVenueId: v.sessionType === 'CLINICAL' ? v.clinicalVenueId : null,
      courseOfferingId: v.courseOfferingId ?? null,
    };
    this.saving.set(true);
    const op$ = this.isEditMode() ? this.labScheduleService.update(this.itemId!, request) : this.labScheduleService.create(request);
    op$.subscribe({
      next: () => { this.toast.success(this.isEditMode() ? 'Updated' : 'Created'); void this.router.navigate(['/lab-schedules']); },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to save'); this.saving.set(false); },
    });
  }
}

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
import { ClassSessionType, DAYS_OF_WEEK, LabScheduleRequest, LabSlot } from '../lab-schedule.model';
import { environment } from '../../../../environments/environment';
import { ToastService } from '../../../core/toast/toast.service';
import { BatchService } from '../../batch/batch.service';
import { Batch } from '../../batch/batch.model';
import { ClassroomService } from '../../classroom/classroom.service';
import { Classroom } from '../../classroom/classroom.model';
import { PeriodService } from '../../period/period.service';
import { Period } from '../../period/period.model';
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

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Class Schedule');
  protected readonly labs = signal<{ id: number; name: string }[]>([]);
  protected readonly subjects = signal<{ id: number; name: string; code: string }[]>([]);
  protected readonly faculty = signal<{ id: number; name: string }[]>([]);
  protected readonly labSlots = signal<LabSlot[]>([]);
  protected readonly classrooms = signal<Classroom[]>([]);
  protected readonly periods = signal<Period[]>([]);
  protected readonly termInstances = signal<{ id: number; termType: string; startDate: string; endDate: string; academicYearName: string }[]>([]);
  protected readonly daysOfWeek = DAYS_OF_WEEK;
  protected readonly availableBatches = signal<Batch[]>([]);

  protected readonly sessionType = signal<ClassSessionType>('LAB');
  protected readonly isTheory = computed(() => this.sessionType() === 'THEORY');

  // Preview signals
  protected readonly previewLabId       = signal<number | null>(null);
  protected readonly previewSubjectId   = signal<number | null>(null);
  protected readonly previewFacultyId   = signal<number | null>(null);
  protected readonly previewSlotId      = signal<number | null>(null);
  protected readonly previewClassroomId = signal<number | null>(null);
  protected readonly previewPeriodId    = signal<number | null>(null);
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
    if (this.isTheory()) {
      const p = this.periods().find(x => x.id === this.previewPeriodId());
      return p ? `${p.startTime}-${p.endTime}` : '';
    }
    const s = this.labSlots().find(x => x.id === this.previewSlotId());
    return s ? `${s.startTime}-${s.endTime}` : '';
  });
  protected readonly previewRoomName = computed(() => {
    if (this.isTheory()) {
      return this.classrooms().find(c => c.id === this.previewClassroomId())?.name ?? '';
    }
    return this.previewLabName();
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
    labSlotId: [null as number | null],
    batchName: [''],
    batchId: [null],
    dayOfWeek: ['', Validators.required],
    termInstanceId: [null, Validators.required],
    isActive: [true],
    classroomId: [null as number | null],
    periodId: [null as number | null],
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
        this.previewFacultyId.set(v.facultyId ?? null);
        this.previewSlotId.set(v.labSlotId ?? null);
        this.previewClassroomId.set(v.classroomId ?? null);
        this.previewPeriodId.set(v.periodId ?? null);
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
    const labSlotId = this.form.get('labSlotId');
    const batchName = this.form.get('batchName');
    const classroomId = this.form.get('classroomId');
    const periodId = this.form.get('periodId');

    if (type === 'THEORY') {
      labId?.clearValidators();
      labSlotId?.clearValidators();
      batchName?.clearValidators();
      classroomId?.setValidators(Validators.required);
      periodId?.setValidators(Validators.required);
    } else {
      labId?.setValidators(Validators.required);
      labSlotId?.setValidators(Validators.required);
      batchName?.setValidators([Validators.required, Validators.maxLength(100)]);
      classroomId?.clearValidators();
      periodId?.clearValidators();
    }
    labId?.updateValueAndValidity({ emitEvent: false });
    labSlotId?.updateValueAndValidity({ emitEvent: false });
    batchName?.updateValueAndValidity({ emitEvent: false });
    classroomId?.updateValueAndValidity({ emitEvent: false });
    periodId?.updateValueAndValidity({ emitEvent: false });
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
    this.http.get<{ id: number; name: string; code: string }[]>(`${environment.apiUrl}/courses`).subscribe({
      next: (data) => this.subjects.set(data),
      error: () => { this.toast.error('Failed to load subjects'); },
    });
    this.http.get<{ id: number; name: string }[]>(`${environment.apiUrl}/faculty`).subscribe({
      next: (data) => this.faculty.set(data),
      error: () => { this.toast.error('Failed to load faculty'); },
    });
    this.labScheduleService.getAllSlots().subscribe({
      next: (data) => this.labSlots.set(data),
      error: () => { this.toast.error('Failed to load lab slots'); },
    });
    this.classroomService.getAll(true).subscribe({
      next: (data) => this.classrooms.set(data),
      error: () => { this.toast.error('Failed to load classrooms'); },
    });
    this.periodService.getAll(true).subscribe({
      next: (data) => this.periods.set(data),
      error: () => { this.toast.error('Failed to load periods'); },
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
          this.form.patchValue({
            sessionType: item.sessionType,
            labId: item.labId,
            subjectId: item.subjectId,
            facultyId: item.facultyId,
            labSlotId: item.labSlotId,
            batchName: item.batchName ?? '',
            batchId: item.batchId,
            dayOfWeek: item.dayOfWeek,
            termInstanceId: item.termInstanceId,
            isActive: item.isActive,
            classroomId: item.classroomId,
            periodId: item.periodId,
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
    const request: LabScheduleRequest = {
      sessionType: v.sessionType,
      labId: v.sessionType === 'LAB' ? v.labId : null,
      subjectId: v.subjectId,
      facultyId: v.facultyId,
      labSlotId: v.sessionType === 'LAB' ? v.labSlotId : null,
      batchName: v.sessionType === 'LAB' ? (v.batchName ?? '').trim() : null,
      batchId: v.sessionType === 'LAB' ? (v.batchId ?? null) : null,
      dayOfWeek: v.dayOfWeek,
      termInstanceId: v.termInstanceId,
      isActive: v.isActive,
      classroomId: v.sessionType === 'THEORY' ? v.classroomId : null,
      periodId: v.sessionType === 'THEORY' ? v.periodId : null,
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

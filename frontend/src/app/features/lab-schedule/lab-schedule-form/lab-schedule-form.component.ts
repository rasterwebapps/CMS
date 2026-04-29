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
import { DAYS_OF_WEEK, LabScheduleRequest, LabSlot } from '../lab-schedule.model';
import { environment } from '../../../../environments/environment';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsPreviewCardComponent } from '../../../shared/preview-card/preview-card.component';
import { CmsTipsCardComponent, CmsTip } from '../../../shared/tips-card/tips-card.component';

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

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Lab Schedule');
  protected readonly labs = signal<{ id: number; name: string }[]>([]);
  protected readonly courses = signal<{ id: number; name: string; code: string }[]>([]);
  protected readonly faculty = signal<{ id: number; name: string }[]>([]);
  protected readonly labSlots = signal<LabSlot[]>([]);
  protected readonly semesters = signal<{ id: number; name: string }[]>([]);
  protected readonly daysOfWeek = DAYS_OF_WEEK;

  // Preview signals
  protected readonly previewLabId     = signal<number | null>(null);
  protected readonly previewCourseId  = signal<number | null>(null);
  protected readonly previewFacultyId = signal<number | null>(null);
  protected readonly previewSlotId    = signal<number | null>(null);
  protected readonly previewBatch     = signal('');
  protected readonly previewDay       = signal('');
  protected readonly previewSemId     = signal<number | null>(null);
  protected readonly previewActive    = signal(true);
  protected readonly previewLabName     = computed(() => this.labs().find(l => l.id === this.previewLabId())?.name ?? '');
  protected readonly previewCourseName  = computed(() => {
    const c = this.courses().find(x => x.id === this.previewCourseId());
    return c ? `${c.name} (${c.code})` : '';
  });
  protected readonly previewFacultyName = computed(() => this.faculty().find(f => f.id === this.previewFacultyId())?.name ?? '');
  protected readonly previewSemName     = computed(() => this.semesters().find(s => s.id === this.previewSemId())?.name ?? '');
  protected readonly previewSlotLabel   = computed(() => {
    const s = this.labSlots().find(x => x.id === this.previewSlotId());
    return s ? `${s.startTime}-${s.endTime}` : '';
  });

  protected readonly TIPS: CmsTip[] = [
    { icon: 'group',          title: 'Batch',     subtitle: 'Use a clear name (e.g., "Batch A") so students know which schedule applies to them.' },
    { icon: 'today',          title: 'Day & Slot', subtitle: 'Each lab can host one schedule per Day + Slot — conflicts are blocked by the system.' },
    { icon: 'manage_accounts',title: 'Faculty',   subtitle: 'Pick a faculty member with subject expertise; they receive automatic calendar reminders.' },
  ];

  private itemId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    labId: [null, Validators.required],
    courseId: [null, Validators.required],
    facultyId: [null, Validators.required],
    labSlotId: [null, Validators.required],
    batchName: ['', [Validators.required, Validators.maxLength(100)]],
    dayOfWeek: ['', Validators.required],
    semesterId: [null, Validators.required],
    isActive: [true],
  });

  constructor() {
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(v => {
        this.previewLabId.set(v.labId ?? null);
        this.previewCourseId.set(v.courseId ?? null);
        this.previewFacultyId.set(v.facultyId ?? null);
        this.previewSlotId.set(v.labSlotId ?? null);
        this.previewBatch.set((v.batchName ?? '').trim());
        this.previewDay.set(v.dayOfWeek ?? '');
        this.previewSemId.set(v.semesterId ?? null);
        this.previewActive.set(!!v.isActive);
      });
  }

  ngOnInit(): void {
    this.http.get<{ id: number; name: string }[]>(`${environment.apiUrl}/labs`).subscribe({
      next: (data) => this.labs.set(data),
      error: () => { this.toast.error('Failed to load labs'); },
    });
    this.http.get<{ id: number; name: string; code: string }[]>(`${environment.apiUrl}/courses`).subscribe({
      next: (data) => this.courses.set(data),
      error: () => { this.toast.error('Failed to load courses'); },
    });
    this.http.get<{ id: number; name: string }[]>(`${environment.apiUrl}/faculty`).subscribe({
      next: (data) => this.faculty.set(data),
      error: () => { this.toast.error('Failed to load faculty'); },
    });
    this.labScheduleService.getAllSlots().subscribe({
      next: (data) => this.labSlots.set(data),
      error: () => { this.toast.error('Failed to load lab slots'); },
    });
    this.http.get<{ id: number; name: string }[]>(`${environment.apiUrl}/semesters`).subscribe({
      next: (data) => this.semesters.set(data),
      error: () => { this.toast.error('Failed to load semesters'); },
    });

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.itemId = Number(id);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Lab Schedule');
      this.loading.set(true);
      this.labScheduleService.getById(this.itemId).subscribe({
        next: (item) => {
          this.form.patchValue({ labId: item.labId, courseId: item.courseId, facultyId: item.facultyId, labSlotId: item.labSlotId, batchName: item.batchName, dayOfWeek: item.dayOfWeek, semesterId: item.semesterId, isActive: item.isActive });
          this.loading.set(false);
        },
        error: () => { this.toast.error('Failed to load'); void this.router.navigate(['/lab-schedules']); },
      });
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.value;
    const request: LabScheduleRequest = { labId: v.labId, courseId: v.courseId, facultyId: v.facultyId, labSlotId: v.labSlotId, batchName: v.batchName.trim(), dayOfWeek: v.dayOfWeek, semesterId: v.semesterId, isActive: v.isActive };
    this.saving.set(true);
    const op$ = this.isEditMode() ? this.labScheduleService.update(this.itemId!, request) : this.labScheduleService.create(request);
    op$.subscribe({
      next: () => { this.toast.success(this.isEditMode() ? 'Updated' : 'Created'); void this.router.navigate(['/lab-schedules']); },
      error: () => { this.toast.error('Failed to save'); this.saving.set(false); },
    });
  }
}

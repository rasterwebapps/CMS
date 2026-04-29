import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpClient } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ExaminationService } from '../examination.service';
import { ExaminationRequest } from '../examination.model';
import { environment } from '../../../../environments/environment';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsPreviewCardComponent } from '../../../shared/preview-card/preview-card.component';
import { CmsTipsCardComponent, CmsTip } from '../../../shared/tips-card/tips-card.component';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';

@Component({
  selector: 'app-examination-form',
  standalone: true,
  imports: [
    RouterLink, ReactiveFormsModule,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule,
    CmsPreviewCardComponent, CmsTipsCardComponent, AppDatePipe,
  ],
  templateUrl: './examination-form.component.html',
  styleUrl: './examination-form.component.scss',
})
export class ExaminationFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly examinationService = inject(ExaminationService);
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Examination');
  protected readonly courses = signal<{ id: number; name: string }[]>([]);
  protected readonly semesters = signal<{ id: number; name: string }[]>([]);

  // Preview signals
  protected readonly previewName     = signal('');
  protected readonly previewCourseId = signal<number | null>(null);
  protected readonly previewType     = signal('');
  protected readonly previewDate     = signal<string | null>(null);
  protected readonly previewDuration = signal<number | null>(null);
  protected readonly previewMaxMarks = signal<number | null>(null);
  protected readonly previewSemId    = signal<number | null>(null);
  protected readonly previewCourseName = computed(() => this.courses().find(c => c.id === this.previewCourseId())?.name ?? '');
  protected readonly previewSemName    = computed(() => this.semesters().find(s => s.id === this.previewSemId())?.name ?? '');

  protected readonly TIPS: CmsTip[] = [
    { icon: 'edit_note',  title: 'Naming',   subtitle: 'Use descriptive names (e.g., "Mid-Sem Theory") so students can identify exams in the calendar.' },
    { icon: 'schedule',   title: 'Duration', subtitle: 'Time in minutes — used to auto-generate exam slot conflict warnings.' },
    { icon: 'grade',      title: 'Max Marks',subtitle: 'Marks entered per student must not exceed this — used for percentage calculations.' },
  ];

  private itemId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    courseId: [null, Validators.required],
    examType: ['', Validators.required],
    date: [''],
    duration: [null, [Validators.min(1)]],
    maxMarks: [null, [Validators.min(0)]],
    semesterId: [null],
  });

  constructor() {
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(v => {
        this.previewName.set((v.name ?? '').trim());
        this.previewCourseId.set(v.courseId ?? null);
        this.previewType.set(v.examType ?? '');
        this.previewDate.set(v.date || null);
        this.previewDuration.set(v.duration ? Number(v.duration) : null);
        this.previewMaxMarks.set(v.maxMarks != null && v.maxMarks !== '' ? Number(v.maxMarks) : null);
        this.previewSemId.set(v.semesterId ?? null);
      });
  }

  ngOnInit(): void {
    this.http.get<{ id: number; name: string }[]>(`${environment.apiUrl}/courses`).subscribe({
      next: (data) => this.courses.set(data),
    });
    this.http.get<{ id: number; name: string }[]>(`${environment.apiUrl}/semesters`).subscribe({
      next: (data) => this.semesters.set(data),
    });
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.itemId = Number(id);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Examination');
      this.loading.set(true);
      this.examinationService.getById(this.itemId).subscribe({
        next: (item) => {
          this.form.patchValue({ name: item.name, courseId: item.courseId, examType: item.examType, date: item.date || '', duration: item.duration, maxMarks: item.maxMarks, semesterId: item.semesterId });
          this.loading.set(false);
        },
        error: () => { this.toast.error('Failed to load'); void this.router.navigate(['/examinations']); },
      });
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.value;
    const request: ExaminationRequest = { name: v.name.trim(), courseId: v.courseId, examType: v.examType, date: v.date || undefined, duration: v.duration || undefined, maxMarks: v.maxMarks ?? undefined, semesterId: v.semesterId || undefined };
    this.saving.set(true);
    const op$ = this.isEditMode() ? this.examinationService.update(this.itemId!, request) : this.examinationService.create(request);
    op$.subscribe({
      next: () => { this.toast.success(this.isEditMode() ? 'Updated' : 'Created'); void this.router.navigate(['/examinations']); },
      error: () => { this.toast.error('Failed to save'); this.saving.set(false); },
    });
  }
}

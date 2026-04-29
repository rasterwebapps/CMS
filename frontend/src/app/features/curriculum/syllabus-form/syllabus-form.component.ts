import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpClient } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CurriculumService } from '../curriculum.service';
import { SyllabusRequest } from '../curriculum.model';
import { environment } from '../../../../environments/environment';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { SYLLABUS_FORM_TOUR } from '../../../shared/tour/tours/syllabus.tours';
import { CmsPreviewCardComponent } from '../../../shared/preview-card/preview-card.component';
import { CmsTipsCardComponent, CmsTip } from '../../../shared/tips-card/tips-card.component';

@Component({
  selector: 'app-syllabus-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    CmsTourButtonComponent,
    CmsPreviewCardComponent,
    CmsTipsCardComponent,
  ],
  templateUrl: './syllabus-form.component.html',
  styleUrl: './syllabus-form.component.scss',
})
export class SyllabusFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly curriculumService = inject(CurriculumService);
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);
  private readonly tourService = inject(TourService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Syllabus');
  protected readonly courses = signal<{ id: number; name: string; code: string }[]>([]);

  // Preview signals
  protected readonly previewCourseId   = signal<number | null>(null);
  protected readonly previewVersionNum = signal<number | null>(null);
  protected readonly previewTheory     = signal<number | null>(null);
  protected readonly previewLab        = signal<number | null>(null);
  protected readonly previewTutorial   = signal<number | null>(null);
  protected readonly previewActive     = signal(true);
  protected readonly previewCourseName = computed(() => {
    const c = this.courses().find(x => x.id === this.previewCourseId());
    return c ? `${c.name} (${c.code})` : '';
  });
  protected readonly totalHours = computed(() => (this.previewTheory() ?? 0) + (this.previewLab() ?? 0) + (this.previewTutorial() ?? 0));

  protected readonly TIPS: CmsTip[] = [
    { icon: 'history',   title: 'Versioning',       subtitle: 'Increment version on substantive content changes; old versions stay attached to past batches.' },
    { icon: 'schedule',  title: 'Hours allocation', subtitle: 'Total weekly hours = Theory + Lab + Tutorial — used for credit and load calculations.' },
    { icon: 'menu_book', title: 'References',       subtitle: 'List one book per line including author and edition for accreditation reports.' },
  ];

  private itemId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    courseId: [null, Validators.required],
    version: [null, [Validators.required, Validators.min(1)]],
    theoryHours: [null, [Validators.min(0)]],
    labHours: [null, [Validators.min(0)]],
    tutorialHours: [null, [Validators.min(0)]],
    objectives: [''],
    content: [''],
    textBooks: [''],
    referenceBooks: [''],
    courseOutcomes: [''],
    isActive: [true],
  });

  constructor() {
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(v => {
        this.previewCourseId.set(v.courseId ?? null);
        this.previewVersionNum.set(v.version ? Number(v.version) : null);
        this.previewTheory.set(v.theoryHours != null && v.theoryHours !== '' ? Number(v.theoryHours) : null);
        this.previewLab.set(v.labHours != null && v.labHours !== '' ? Number(v.labHours) : null);
        this.previewTutorial.set(v.tutorialHours != null && v.tutorialHours !== '' ? Number(v.tutorialHours) : null);
        this.previewActive.set(!!v.isActive);
      });
  }

  ngOnInit(): void {
    this.tourService.register('syllabus-form', SYLLABUS_FORM_TOUR);
    this.http
      .get<{ id: number; name: string; code: string }[]>(`${environment.apiUrl}/courses`)
      .subscribe({
        next: (data) => this.courses.set(data),
        error: () => {
          this.toast.error('Failed to load courses');
        },
      });
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.itemId = Number(id);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Syllabus');
      this.loading.set(true);
      this.curriculumService.getSyllabusById(this.itemId).subscribe({
        next: (item) => {
          this.form.patchValue({
            courseId: item.courseId,
            version: item.version,
            theoryHours: item.theoryHours,
            labHours: item.labHours,
            tutorialHours: item.tutorialHours,
            objectives: item.objectives || '',
            content: item.content || '',
            textBooks: item.textBooks || '',
            referenceBooks: item.referenceBooks || '',
            courseOutcomes: item.courseOutcomes || '',
            isActive: item.isActive,
          });
          this.loading.set(false);
        },
        error: () => {
          this.toast.error('Failed to load');
          void this.router.navigate(['/syllabi']);
        },
      });
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.value;
    const request: SyllabusRequest = {
      courseId: v.courseId,
      version: v.version,
      theoryHours: v.theoryHours ?? undefined,
      labHours: v.labHours ?? undefined,
      tutorialHours: v.tutorialHours ?? undefined,
      objectives: v.objectives?.trim() || undefined,
      content: v.content?.trim() || undefined,
      textBooks: v.textBooks?.trim() || undefined,
      referenceBooks: v.referenceBooks?.trim() || undefined,
      courseOutcomes: v.courseOutcomes?.trim() || undefined,
      isActive: v.isActive,
    };
    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.curriculumService.updateSyllabus(this.itemId!, request)
      : this.curriculumService.createSyllabus(request);
    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Updated' : 'Created');
        void this.router.navigate(['/syllabi']);
      },
      error: () => {
        this.toast.error('Failed to save');
        this.saving.set(false);
      },
    });
  }
}

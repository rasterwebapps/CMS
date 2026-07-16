import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CurriculumService } from '../curriculum.service';
import { CurriculumVersionService } from '../curriculum-version.service';
import { SyllabusRequest } from '../curriculum.model';
import { CurriculumVersion, CurriculumFullView, CurriculumSemesterCourse } from '../curriculum-version.model';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { SYLLABUS_FORM_TOUR } from '../../../shared/tour/tours/syllabus.tours';
import { CmsPreviewCardComponent } from '../../../shared/preview-card/preview-card.component';
import { CmsTipsCardComponent, CmsTip } from '../../../shared/tips-card/tips-card.component';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';

/** Create-only — a syllabus version is immutable once saved (see BR: syllabus versioning).
 *  There is no edit route; changing content means creating a new version here again, and
 *  activating/deactivating an existing version happens from the list screen instead. */
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
  private readonly router = inject(Router);
  private readonly curriculumService = inject(CurriculumService);
  private readonly curriculumVersionService = inject(CurriculumVersionService);
  private readonly toast = inject(ToastService);
  private readonly tourService = inject(TourService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly saving = signal(false);

  protected readonly curriculumVersions = signal<CurriculumVersion[]>([]);
  /** Full term/subject tree for the currently-selected curriculum version — drives the
   *  Term and Subject dropdowns, and is where the read-only Theory/Lab/Clinical hours
   *  ultimately come from (Curriculum Map is the source of truth, not this form). */
  protected readonly curriculum = signal<CurriculumFullView | null>(null);
  protected readonly loadingCurriculum = signal(false);

  protected readonly selectedMapping = computed<CurriculumSemesterCourse | null>(() => {
    const c = this.curriculum();
    const termNumber = this.form?.get('termNumber')?.value;
    const mappingId = this.form?.get('curriculumTermCourseId')?.value;
    if (!c || !termNumber || !mappingId) return null;
    const term = c.terms.find(t => t.termNumber === Number(termNumber));
    return term?.courses.find(course => course.id === Number(mappingId)) ?? null;
  });

  // Preview signals
  protected readonly previewActive     = signal(true);
  protected readonly previewSubjectName = computed(() => this.selectedMapping()?.subjectName ?? '');
  protected readonly totalHours = computed(() => {
    const m = this.selectedMapping();
    return m ? m.theoryHours + m.labHours + m.clinicalHours : 0;
  });

  protected readonly TIPS: CmsTip[] = [
    { icon: 'history',   title: 'Versioning',       subtitle: 'A syllabus is locked once saved — the version number is assigned automatically. To change content later, create a new version here; activate it from the list to make it current.' },
    { icon: 'schedule',  title: 'Hours allocation', subtitle: 'Theory/Lab/Clinical hours are set on the Curriculum Map screen, not here — this form only links to that mapping.' },
    { icon: 'menu_book', title: 'References',       subtitle: 'List one book per line including author and edition for accreditation reports.' },
  ];

  protected readonly form: FormGroup = this.fb.group({
    curriculumVersionId: [null, Validators.required],
    termNumber: [{ value: null, disabled: true }, Validators.required],
    curriculumTermCourseId: [{ value: null, disabled: true }, Validators.required],
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
        this.previewActive.set(!!v.isActive);
      });

    this.form.get('curriculumVersionId')!.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((curriculumVersionId: number | null) => {
        this.form.get('termNumber')!.reset(null, { emitEvent: false });
        this.form.get('curriculumTermCourseId')!.reset(null, { emitEvent: false });
        this.curriculum.set(null);
        if (curriculumVersionId == null) {
          this.form.get('termNumber')!.disable({ emitEvent: false });
          this.form.get('curriculumTermCourseId')!.disable({ emitEvent: false });
          return;
        }
        this.loadingCurriculum.set(true);
        this.curriculumVersionService.getFullCurriculum(curriculumVersionId).subscribe({
          next: (data) => {
            this.curriculum.set(data);
            this.form.get('termNumber')!.enable({ emitEvent: false });
            this.loadingCurriculum.set(false);
          },
          error: () => {
            this.toast.error('Failed to load curriculum terms');
            this.loadingCurriculum.set(false);
          },
        });
      });

    this.form.get('termNumber')!.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((termNumber: number | null) => {
        this.form.get('curriculumTermCourseId')!.reset(null, { emitEvent: false });
        if (termNumber == null) {
          this.form.get('curriculumTermCourseId')!.disable({ emitEvent: false });
        } else {
          this.form.get('curriculumTermCourseId')!.enable({ emitEvent: false });
        }
      });
  }

  ngOnInit(): void {
    this.tourService.register('syllabus-form', SYLLABUS_FORM_TOUR);
    this.curriculumVersionService.getPage({ size: 500, sort: 'versionName' }).subscribe({
      next: (page) => this.curriculumVersions.set(page.content),
      error: () => this.toast.error('Failed to load curriculum versions'),
    });
  }

  protected termLabel(n: number): string {
    return this.curriculum()?.assessmentPattern === 'YEARLY' ? `Year ${n}` : `Term ${n}`;
  }

  protected getTermNumbers(): number[] {
    const c = this.curriculum();
    if (!c) return [];
    return Array.from({ length: c.totalTerms }, (_, i) => i + 1);
  }

  protected getCoursesForSelectedTerm(): CurriculumSemesterCourse[] {
    const c = this.curriculum();
    const termNumber = this.form.get('termNumber')?.value;
    if (!c || !termNumber) return [];
    return c.terms.find(t => t.termNumber === Number(termNumber))?.courses ?? [];
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }
    const v = this.form.getRawValue();
    const request: SyllabusRequest = {
      curriculumTermCourseId: v.curriculumTermCourseId,
      objectives: v.objectives?.trim() || undefined,
      content: v.content?.trim() || undefined,
      textBooks: v.textBooks?.trim() || undefined,
      referenceBooks: v.referenceBooks?.trim() || undefined,
      courseOutcomes: v.courseOutcomes?.trim() || undefined,
      isActive: v.isActive,
    };
    this.saving.set(true);
    this.curriculumService.createSyllabus(request).subscribe({
      next: () => {
        this.toast.success('Syllabus version created');
        void this.router.navigate(['/syllabi']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to save');
        this.saving.set(false);
      },
    });
  }
}

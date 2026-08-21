import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CourseService } from '../course.service';
import { CourseRequest } from '../course.model';
import { ProgramService } from '../../program/program.service';
import { Program } from '../../program/program.model';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { COURSE_FORM_TOUR, COURSE_FORM_FLOW_MAP } from '../../../shared/tour/tours/course.tours';
import { CmsPreviewCardComponent } from '../../../shared/preview-card/preview-card.component';
import { CmsTipsCardComponent, CmsTip } from '../../../shared/tips-card/tips-card.component';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';
import { noConsecutiveSpaces, noInternalSpaces, trimmedMinLength, cmsFieldError, stripSpaces } from '../../../shared/validators/cms-validators';
import { environment } from '../../../../environments';
import { uniqueFieldValidator } from '../../../shared/validators/unique-field.validator';

@Component({
  selector: 'app-course-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    CmsTourButtonComponent,
    CmsPreviewCardComponent,
    CmsTipsCardComponent,
  ],
  templateUrl: './course-form.component.html',
  styleUrl: './course-form.component.scss',
})
export class CourseFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly courseService = inject(CourseService);
  private readonly programService = inject(ProgramService);
  private readonly toast = inject(ToastService);
  private readonly tourService = inject(TourService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly http = inject(HttpClient);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Course');
  protected readonly programs = signal<Program[]>([]);

  // Live preview signals
  protected readonly previewName = signal('');
  protected readonly previewCode = signal('');
  protected readonly previewSpec = signal('');
  protected readonly previewProgramId = signal<number | null>(null);
  protected readonly previewProgramName = computed(() => {
    const id = this.previewProgramId();
    if (!id) return '';
    return this.programs().find(p => p.id === id)?.name ?? '';
  });

  protected readonly TIPS: CmsTip[] = [
    { icon: 'tag',     title: 'Course Code',      subtitle: 'Use a short uppercase identifier unique within the parent program.' },
    { icon: 'confirmation_number', title: 'Roll Number Code', subtitle: 'Exactly 2 characters — embedded in every student roll number and admission number for this course.' },
    { icon: 'school',  title: 'Parent Program',   subtitle: 'Every course must belong to one program (e.g., Bachelor, Master).' },
    { icon: 'science', title: 'Specialization',   subtitle: 'Optional — name the focus area for general programs (e.g., Cardiac, Pediatric).' },
  ];

  private courseId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100), trimmedMinLength(2), noConsecutiveSpaces()]],
    code: ['', [Validators.required, Validators.maxLength(20), noInternalSpaces()]],
    rollNumberCode: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(2), noInternalSpaces()]],
    specialization: [''],
    programId: [null as number | null, [Validators.required]],
    isActive: [true],
  });

  constructor() {
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(v => {
        this.previewName.set((v.name ?? '').trim());
        this.previewCode.set(stripSpaces(v.code ?? '').toUpperCase());
        this.previewSpec.set((v.specialization ?? '').trim());
        this.previewProgramId.set(v.programId ?? null);
      });
  }

  protected onCodeInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const upper = input.value.toUpperCase();
    if (upper !== input.value) {
      this.form.get('code')?.setValue(upper, { emitEvent: true });
    }
  }

  protected onRollNumberCodeInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const upper = stripSpaces(input.value).toUpperCase();
    if (upper !== input.value) {
      this.form.get('rollNumberCode')?.setValue(upper, { emitEvent: true });
    }
  }

  ngOnInit(): void {
    this.tourService.register('course-form', COURSE_FORM_TOUR);
    this.tourService.registerFlowMap('course-form', COURSE_FORM_FLOW_MAP);
    this.loadPrograms();

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.courseId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Course');
      this.loadCourse();
    }
    this.setupUniquenessValidators();
  }

  private setupUniquenessValidators(): void {
    const nameCtrl = this.form.get('name');
    if (nameCtrl) {
      nameCtrl.setAsyncValidators(
        uniqueFieldValidator(this.http, `${environment.apiUrl}/courses/name-exists`, () => this.courseId)
      );
      nameCtrl.updateValueAndValidity({ emitEvent: false });
    }
    const codeCtrl = this.form.get('code');
    if (codeCtrl) {
      codeCtrl.setAsyncValidators(
        uniqueFieldValidator(this.http, `${environment.apiUrl}/courses/code-exists`, () => this.courseId)
      );
      codeCtrl.updateValueAndValidity({ emitEvent: false });
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }

    const request: CourseRequest = {
      name: (this.form.value.name ?? '').trim(),
      code: (this.form.value.code ?? '').trim(),
      rollNumberCode: stripSpaces(this.form.value.rollNumberCode ?? '').toUpperCase(),
      specialization: this.form.value.specialization?.trim() || null,
      programId: this.form.value.programId,
      isActive: this.form.value.isActive,
    };

    this.saving.set(true);

    const operation$ = this.isEditMode()
      ? this.courseService.update(this.courseId!, request)
      : this.courseService.create(request);

    operation$.subscribe({
      next: () => {
        const message = this.isEditMode()
          ? 'Course updated successfully'
          : 'Course created successfully';
        this.toast.success(message);
        void this.router.navigate(['/courses']);
      },
      error: (err) => {
        const message = err?.error?.message
          ?? (this.isEditMode() ? 'Failed to update course' : 'Failed to create course');
        this.toast.error(message);
        this.saving.set(false);
      },
    });
  }

  private static readonly FIELD_LABELS: Record<string, string> = {
    name: 'Course Name',
    code: 'Code',
    rollNumberCode: 'Roll Number Code',
    specialization: 'Specialization',
  };

  protected getErrorMessage(fieldName: string): string {
    return cmsFieldError(this.form.get(fieldName), CourseFormComponent.FIELD_LABELS[fieldName] ?? fieldName);
  }

  private loadPrograms(): void {
    this.programService.getAll().subscribe({
      next: (programs) => {
        this.programs.set(programs);
      },
      error: () => {
        this.toast.error('Failed to load programs');
      },
    });
  }

  private loadCourse(): void {
    if (!this.courseId) return;

    this.loading.set(true);
    this.courseService.getById(this.courseId).subscribe({
      next: (course) => {
        this.form.patchValue({
          name: course.name,
          code: course.code,
          rollNumberCode: course.rollNumberCode,
          specialization: course.specialization,
          programId: course.program?.id,
          isActive: course.isActive,
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load course');
        void this.router.navigate(['/courses']);
      },
    });
  }
}

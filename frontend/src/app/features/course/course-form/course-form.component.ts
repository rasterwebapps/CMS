import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { CourseService } from '../course.service';
import { CourseRequest } from '../course.model';
import { ProgramService } from '../../program/program.service';
import { Program } from '../../program/program.model';

@Component({
  selector: 'app-course-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
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
  private readonly snackBar = inject(MatSnackBar);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Course');
  protected readonly programs = signal<Program[]>([]);

  private courseId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    code: ['', [Validators.required, Validators.maxLength(20)]],
    specialization: [''],
    programId: [null as number | null, [Validators.required]],
  });

  ngOnInit(): void {
    this.loadPrograms();

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.courseId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Course');
      this.loadCourse();
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const request: CourseRequest = {
      name: (this.form.value.name ?? '').trim(),
      code: (this.form.value.code ?? '').trim(),
      specialization: this.form.value.specialization?.trim() || null,
      programId: this.form.value.programId,
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
        this.snackBar.open(message, 'Close', { duration: 3000 });
        void this.router.navigate(['/courses']);
      },
      error: (err) => {
        const message = err?.error?.message
          ?? (this.isEditMode() ? 'Failed to update course' : 'Failed to create course');
        this.snackBar.open(message, 'Close', { duration: 4000 });
        this.saving.set(false);
      },
    });
  }

  protected getErrorMessage(fieldName: string): string {
    const control = this.form.get(fieldName);
    if (!control || !control.errors) {
      return '';
    }

    if (control.errors['required']) {
      return `${this.getFieldLabel(fieldName)} is required`;
    }
    if (control.errors['maxlength']) {
      const maxLength = control.errors['maxlength'].requiredLength;
      return `${this.getFieldLabel(fieldName)} must be at most ${maxLength} characters`;
    }
    if (control.errors['min']) {
      const min = control.errors['min'].min;
      return `${this.getFieldLabel(fieldName)} must be at least ${min}`;
    }
    if (control.errors['max']) {
      const max = control.errors['max'].max;
      return `${this.getFieldLabel(fieldName)} must be at most ${max}`;
    }

    return '';
  }

  private getFieldLabel(fieldName: string): string {
    const labels: Record<string, string> = {
      name: 'Name',
      code: 'Code',
      specialization: 'Specialization',
      programId: 'Program',
    };
    return labels[fieldName] || fieldName;
  }

  private loadPrograms(): void {
    this.programService.getAll().subscribe({
      next: (programs) => {
        this.programs.set(programs);
      },
      error: () => {
        this.snackBar.open('Failed to load programs', 'Close', { duration: 3000 });
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
          specialization: course.specialization,
          programId: course.program?.id,
        });
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Failed to load course', 'Close', { duration: 3000 });
        void this.router.navigate(['/courses']);
      },
    });
  }
}

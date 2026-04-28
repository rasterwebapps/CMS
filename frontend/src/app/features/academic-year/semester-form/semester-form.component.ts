import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AcademicYearService } from '../academic-year.service';
import { AcademicYear, SemesterRequest } from '../academic-year.model';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { SEMESTER_FORM_TOUR } from '../../../shared/tour/tours/semester.tours';

@Component({
  selector: 'app-semester-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    CmsTourButtonComponent,
  ],
  templateUrl: './semester-form.component.html',
  styleUrl: './semester-form.component.scss',
})
export class SemesterFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly academicYearService = inject(AcademicYearService);
  private readonly toast = inject(ToastService);
  private readonly tourService = inject(TourService);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Semester');
  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly semesterNumbers = [1, 2, 3, 4, 5, 6, 7, 8];

  private semesterId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    semesterNumber: [null as number | null, [Validators.required]],
    startDate: ['', [Validators.required]],
    endDate: ['', [Validators.required]],
    academicYearId: [null as number | null, [Validators.required]],
  });

  ngOnInit(): void {
    this.tourService.register('semester-form', SEMESTER_FORM_TOUR);
    this.loadAcademicYears();

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.semesterId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Semester');
      this.loadSemester();
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const request: SemesterRequest = {
      name: (this.form.value.name ?? '').trim(),
      semesterNumber: this.form.value.semesterNumber,
      startDate: this.form.value.startDate,
      endDate: this.form.value.endDate,
      academicYearId: this.form.value.academicYearId,
    };

    this.saving.set(true);

    const operation$ = this.isEditMode()
      ? this.academicYearService.updateSemester(this.semesterId!, request)
      : this.academicYearService.createSemester(request);

    operation$.subscribe({
      next: () => {
        const message = this.isEditMode()
          ? 'Semester updated successfully'
          : 'Semester created successfully';
        this.toast.success(message);
        void this.router.navigate(['/semesters']);
      },
      error: () => {
        const message = this.isEditMode()
          ? 'Failed to update semester'
          : 'Failed to create semester';
        this.toast.error(message);
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

    return '';
  }

  private getFieldLabel(fieldName: string): string {
    const labels: Record<string, string> = {
      name: 'Name',
      semesterNumber: 'Semester Number',
      startDate: 'Start Date',
      endDate: 'End Date',
      academicYearId: 'Academic Year',
    };
    return labels[fieldName] || fieldName;
  }

  private loadAcademicYears(): void {
    this.academicYearService.getAllAcademicYears().subscribe({
      next: (academicYears) => {
        this.academicYears.set(academicYears);
      },
      error: () => {
        this.toast.error('Failed to load academic years');
      },
    });
  }

  private loadSemester(): void {
    if (!this.semesterId) return;

    this.loading.set(true);
    this.academicYearService.getSemesterById(this.semesterId).subscribe({
      next: (semester) => {
        this.form.patchValue({
          name: semester.name,
          semesterNumber: semester.semesterNumber,
          startDate: semester.startDate,
          endDate: semester.endDate,
          academicYearId: semester.academicYear?.id,
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load semester');
        void this.router.navigate(['/semesters']);
      },
    });
  }
}

import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { provideNativeDateAdapter } from '@angular/material/core';
import { AcademicYearService } from '../academic-year.service';
import { AcademicYearRequest } from '../academic-year.model';

@Component({
  selector: 'app-academic-year-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDatepickerModule,
    MatSlideToggleModule,
  ],
  providers: [provideNativeDateAdapter()],
  templateUrl: './academic-year-form.component.html',
  styleUrl: './academic-year-form.component.scss',
})
export class AcademicYearFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly academicYearService = inject(AcademicYearService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Academic Year');

  private academicYearId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    startDate: [null as Date | null, [Validators.required]],
    endDate: [null as Date | null, [Validators.required]],
    isCurrent: [false],
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.academicYearId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Academic Year');
      this.loadAcademicYear();
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const startDate = this.form.value.startDate as Date;
    const endDate = this.form.value.endDate as Date;

    const request: AcademicYearRequest = {
      name: (this.form.value.name ?? '').trim(),
      startDate: this.formatDate(startDate),
      endDate: this.formatDate(endDate),
      isCurrent: this.form.value.isCurrent ?? false,
    };

    this.saving.set(true);

    const operation$ = this.isEditMode()
      ? this.academicYearService.updateAcademicYear(this.academicYearId!, request)
      : this.academicYearService.createAcademicYear(request);

    operation$.subscribe({
      next: () => {
        const message = this.isEditMode()
          ? 'Academic year updated successfully'
          : 'Academic year created successfully';
        this.snackBar.open(message, 'Close', { duration: 3000 });
        void this.router.navigate(['/academic-years']);
      },
      error: () => {
        const message = this.isEditMode()
          ? 'Failed to update academic year'
          : 'Failed to create academic year';
        this.snackBar.open(message, 'Close', { duration: 3000 });
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
      startDate: 'Start Date',
      endDate: 'End Date',
    };
    return labels[fieldName] || fieldName;
  }

  private formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private loadAcademicYear(): void {
    if (!this.academicYearId) return;

    this.loading.set(true);
    this.academicYearService.getAcademicYearById(this.academicYearId).subscribe({
      next: (academicYear) => {
        this.form.patchValue({
          name: academicYear.name,
          startDate: new Date(academicYear.startDate),
          endDate: new Date(academicYear.endDate),
          isCurrent: academicYear.isCurrent,
        });
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Failed to load academic year', 'Close', { duration: 3000 });
        void this.router.navigate(['/academic-years']);
      },
    });
  }
}

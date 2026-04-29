import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { AcademicYearService } from '../academic-year.service';
import { AcademicYearRequest } from '../academic-year.model';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { ACADEMIC_YEAR_FORM_TOUR } from '../../../shared/tour/tours/academic-year.tours';
import { CmsPreviewCardComponent } from '../../../shared/preview-card/preview-card.component';
import { CmsTipsCardComponent, CmsTip } from '../../../shared/tips-card/tips-card.component';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';

@Component({
  selector: 'app-academic-year-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSlideToggleModule,
    CmsTourButtonComponent,
    CmsPreviewCardComponent,
    CmsTipsCardComponent,
    AppDatePipe,
  ],
  templateUrl: './academic-year-form.component.html',
  styleUrl: './academic-year-form.component.scss',
})
export class AcademicYearFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly academicYearService = inject(AcademicYearService);
  private readonly toast = inject(ToastService);
  private readonly tourService = inject(TourService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Academic Year');

  // Preview signals
  protected readonly previewName      = signal('');
  protected readonly previewStart     = signal<string | null>(null);
  protected readonly previewEnd       = signal<string | null>(null);
  protected readonly previewIsCurrent = signal(false);

  protected readonly TIPS: CmsTip[] = [
    { icon: 'label',       title: 'Naming convention', subtitle: 'Use a hyphenated range like "2024-2025" so it sorts naturally.' },
    { icon: 'date_range',  title: 'Period',            subtitle: 'Start and end dates define the full academic calendar window.' },
    { icon: 'check_circle',title: 'Current Year',      subtitle: 'Only one year can be marked Current — used as the default across the app.' },
  ];

  private academicYearId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    startDate: ['', [Validators.required]],
    endDate: ['', [Validators.required]],
    isCurrent: [false],
  });

  constructor() {
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(v => {
        this.previewName.set((v.name ?? '').trim());
        this.previewStart.set(v.startDate || null);
        this.previewEnd.set(v.endDate || null);
        this.previewIsCurrent.set(!!v.isCurrent);
      });
  }

  ngOnInit(): void {
    this.tourService.register('academic-year-form', ACADEMIC_YEAR_FORM_TOUR);
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

    const request: AcademicYearRequest = {
      name: (this.form.value.name ?? '').trim(),
      startDate: this.form.value.startDate,
      endDate: this.form.value.endDate,
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
        this.toast.success(message);
        void this.router.navigate(['/academic-years']);
      },
      error: (err) => {
        const message = err?.error?.message
          ?? (this.isEditMode() ? 'Failed to update academic year' : 'Failed to create academic year');
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
      startDate: 'Start Date',
      endDate: 'End Date',
    };
    return labels[fieldName] || fieldName;
  }

  private loadAcademicYear(): void {
    if (!this.academicYearId) return;

    this.loading.set(true);
    this.academicYearService.getAcademicYearById(this.academicYearId).subscribe({
      next: (academicYear) => {
        this.form.patchValue({
          name: academicYear.name,
          startDate: academicYear.startDate,
          endDate: academicYear.endDate,
          isCurrent: academicYear.isCurrent,
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load academic year');
        void this.router.navigate(['/academic-years']);
      },
    });
  }
}

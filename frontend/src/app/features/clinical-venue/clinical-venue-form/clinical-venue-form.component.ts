import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ClinicalVenueService } from '../clinical-venue.service';
import { ClinicalVenueRequest } from '../clinical-venue.model';
import { ToastService } from '../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';
import { noConsecutiveSpaces, trimmedMinLength, cmsFieldError } from '../../../shared/validators/cms-validators';
import { environment } from '../../../../environments';
import { uniqueFieldValidator } from '../../../shared/validators/unique-field.validator';

@Component({
  selector: 'app-clinical-venue-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './clinical-venue-form.component.html',
  styleUrl: './clinical-venue-form.component.scss',
})
export class ClinicalVenueFormComponent implements OnInit {
  private readonly fb                 = inject(FormBuilder);
  private readonly route              = inject(ActivatedRoute);
  private readonly router             = inject(Router);
  private readonly clinicalVenueService = inject(ClinicalVenueService);
  private readonly toast              = inject(ToastService);
  private readonly destroyRef         = inject(DestroyRef);
  private readonly http               = inject(HttpClient);

  protected readonly loading    = signal(false);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle  = signal('Add Clinical Venue');

  protected readonly previewName         = signal('');
  protected readonly previewHospitalName = signal('');
  protected readonly previewDepartment   = signal('');

  private venueId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name:         ['', [Validators.required, trimmedMinLength(2), Validators.maxLength(255), noConsecutiveSpaces()]],
    hospitalName: ['', [Validators.maxLength(255)]],
    department:   ['', [Validators.maxLength(255)]],
    capacity:     [null, [Validators.min(1)]],
  });

  constructor() {
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(v => {
        this.previewName.set((v.name ?? '').trim());
        this.previewHospitalName.set((v.hospitalName ?? '').trim());
        this.previewDepartment.set((v.department ?? '').trim());
      });
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.venueId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Clinical Venue');
      this.loadVenue();
    }
    this.setupUniquenessValidators();
  }

  private setupUniquenessValidators(): void {
    const nameCtrl = this.form.get('name');
    if (nameCtrl) {
      nameCtrl.setAsyncValidators(
        uniqueFieldValidator(this.http, `${environment.apiUrl}/clinical-venues/name-exists`, () => this.venueId)
      );
      nameCtrl.updateValueAndValidity({ emitEvent: false });
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }

    const request: ClinicalVenueRequest = {
      name:         (this.form.value.name ?? '').trim(),
      hospitalName: this.form.value.hospitalName?.trim() || undefined,
      department:   this.form.value.department?.trim() || undefined,
      capacity:     this.form.value.capacity ?? undefined,
    };

    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.clinicalVenueService.update(this.venueId!, request)
      : this.clinicalVenueService.create(request);

    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Clinical venue updated successfully' : 'Clinical venue created successfully');
        this.saving.set(false);
        void this.router.navigate(['/clinical-venues']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? (this.isEditMode() ? 'Failed to update clinical venue' : 'Failed to create clinical venue'));
        this.saving.set(false);
      },
    });
  }

  private static readonly FIELD_LABELS: Record<string, string> = {
    name: 'Name', hospitalName: 'Hospital Name', department: 'Department', capacity: 'Capacity',
  };

  protected getErrorMessage(fieldName: string): string {
    return cmsFieldError(this.form.get(fieldName), ClinicalVenueFormComponent.FIELD_LABELS[fieldName] ?? fieldName);
  }

  private loadVenue(): void {
    if (!this.venueId) return;
    this.loading.set(true);
    this.clinicalVenueService.getById(this.venueId).subscribe({
      next: (v) => {
        this.form.patchValue({
          name: v.name,
          hospitalName: v.hospitalName || '',
          department: v.department || '',
          capacity: v.capacity ?? null,
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load clinical venue');
        void this.router.navigate(['/clinical-venues']);
      },
    });
  }
}

import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CourseOffering } from '../../academic-year/academic-year.model';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { ToastService } from '../../../core/toast/toast.service';

export interface ClinicalShiftConfigDialogData {
  offering: CourseOffering;
}

/** Per-offering configurable off-campus clinical shift duration + travel buffer (OC-175) — some
 *  postings run 6h, others 8h, per real-hours-covered inspection requirements. Must be set before
 *  any Clinical Shift Group can be created for this offering. */
@Component({
  selector: 'app-clinical-shift-config-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './clinical-shift-config-dialog.component.html',
  styleUrl: './clinical-shift-config-dialog.component.scss',
})
export class ClinicalShiftConfigDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<ClinicalShiftConfigDialogComponent>);
  protected readonly data: ClinicalShiftConfigDialogData = inject(MAT_DIALOG_DATA);
  private readonly academicYearService = inject(AcademicYearService);
  private readonly toast = inject(ToastService);

  protected readonly saving = signal(false);

  protected readonly form: FormGroup = this.fb.group({
    clinicalShiftDurationMinutes: [
      this.data.offering.clinicalShiftDurationMinutes,
      [Validators.min(1)],
    ],
    clinicalTravelBufferMinutes: [
      this.data.offering.clinicalTravelBufferMinutes,
      [Validators.min(0)],
    ],
  });

  protected submit(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    this.academicYearService.updateClinicalShiftConfig(this.data.offering.id, {
      clinicalShiftDurationMinutes: this.form.value.clinicalShiftDurationMinutes ?? null,
      clinicalTravelBufferMinutes: this.form.value.clinicalTravelBufferMinutes ?? null,
    }).subscribe({
      next: (updated) => {
        this.toast.success('Clinical shift config saved');
        this.saving.set(false);
        this.dialogRef.close(updated);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to save clinical shift config');
        this.saving.set(false);
      },
    });
  }

  protected onClose(): void {
    this.dialogRef.close();
  }
}

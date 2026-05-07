import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ScholarshipApplication, ScholarshipApprovalRequest, DisbursementFrequency } from '../scholarship.model';
import { ScholarshipService } from '../scholarship.service';
import { ToastService } from '../../../core/toast/toast.service';

export interface ScholarshipApproveDialogData {
  application: ScholarshipApplication;
}

@Component({
  selector: 'app-scholarship-approve-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './scholarship-approve-dialog.component.html',
  styleUrl: './scholarship-approve-dialog.component.scss',
})
export class ScholarshipApproveDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<ScholarshipApproveDialogComponent>);
  protected readonly data: ScholarshipApproveDialogData = inject(MAT_DIALOG_DATA);
  private readonly scholarshipService = inject(ScholarshipService);
  private readonly toast = inject(ToastService);

  protected saving = signal(false);

  protected readonly disbursementFrequencies: DisbursementFrequency[] = ['ANNUAL', 'SEMESTER', 'ONE_TIME'];

  protected readonly form: FormGroup = this.fb.group({
    approvedAmount: [this.data.application.approvedAmount ?? null, [Validators.required, Validators.min(1)]],
    disbursementFrequency: [this.data.application.disbursementFrequency ?? 'ANNUAL', Validators.required],
    validFrom: [this.data.application.validFrom ?? ''],
    validTill: [this.data.application.validTill ?? ''],
    remarks: [''],
  });

  protected onApprove(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.value;
    const request: ScholarshipApprovalRequest = {
      approvedAmount: v.approvedAmount,
      disbursementFrequency: v.disbursementFrequency as DisbursementFrequency,
      validFrom: v.validFrom || null,
      validTill: v.validTill || null,
      remarks: v.remarks?.trim() || null,
    };

    this.saving.set(true);
    this.scholarshipService.approve(this.data.application.id, request).subscribe({
      next: (updated) => {
        this.saving.set(false);
        this.toast.success('Scholarship approved successfully');
        this.dialogRef.close(updated);
      },
      error: (err) => {
        this.saving.set(false);
        this.toast.error(err?.error?.message ?? 'Approval failed');
      },
    });
  }

  protected onCancel(): void {
    this.dialogRef.close();
  }

  protected freqLabel(f: DisbursementFrequency): string {
    return { ANNUAL: 'Annual (once per year)', SEMESTER: 'Per Semester', ONE_TIME: 'One Time Only' }[f];
  }
}


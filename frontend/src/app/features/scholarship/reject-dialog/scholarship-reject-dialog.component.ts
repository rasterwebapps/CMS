import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ScholarshipApplication } from '../scholarship.model';
import { ScholarshipService } from '../scholarship.service';
import { ToastService } from '../../../core/toast/toast.service';

export interface ScholarshipRejectDialogData {
  application: ScholarshipApplication;
}

@Component({
  selector: 'app-scholarship-reject-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  template: `
    <h2 mat-dialog-title class="reject-title">
      <mat-icon>cancel</mat-icon>
      Reject Scholarship Application
    </h2>

    <mat-dialog-content>
      <div class="srd-student-info">
        <span class="srd-name">{{ data.application.studentName }}</span>
        <span class="srd-scholarship">{{ data.application.scholarshipName }}</span>
        <span class="srd-year">{{ data.application.academicYearName }}</span>
      </div>
      <form [formGroup]="form" class="dialog-form">
        <div class="field-group">
          <label for="srd-reason" class="field-label">
            Rejection Reason <span class="field-required">*</span>
          </label>
          <textarea
            id="srd-reason"
            class="field-textarea"
            formControlName="reason"
            rows="4"
            placeholder="e.g. Income certificate is missing / Community certificate expired / Ineligible category"
            [class.field-input--error]="form.get('reason')?.invalid && form.get('reason')?.touched">
          </textarea>
          @if (form.get('reason')?.invalid && form.get('reason')?.touched) {
            <p class="field-error">Rejection reason is required</p>
          }
        </div>
      </form>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-stroked-button type="button" (click)="onCancel()" [disabled]="saving()">Cancel</button>
      <button mat-flat-button color="warn" type="button" (click)="onReject()" [disabled]="saving()">
        @if (saving()) {
          <mat-spinner diameter="18" class="btn-spinner"></mat-spinner>
        } @else {
          <mat-icon>cancel</mat-icon>
        }
        Reject Application
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .reject-title {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      mat-icon { color: var(--mat-sys-error); }
    }
    .srd-student-info {
      display: flex;
      flex-wrap: wrap;
      gap: 0.4rem 1rem;
      align-items: center;
      padding: 0.75rem 1rem;
      border-radius: 8px;
      background: var(--mat-sys-surface-container);
      margin-bottom: 1.25rem;
      font-size: 0.875rem;
    }
    .srd-name { font-weight: 600; }
    .srd-scholarship { color: var(--mat-sys-primary); font-weight: 500; }
    .srd-year { color: var(--mat-sys-on-surface-variant); font-size: 0.8rem; margin-left: auto; }
  `,
})
export class ScholarshipRejectDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<ScholarshipRejectDialogComponent>);
  protected readonly data: ScholarshipRejectDialogData = inject(MAT_DIALOG_DATA);
  private readonly scholarshipService = inject(ScholarshipService);
  private readonly toast = inject(ToastService);

  protected saving = signal(false);

  protected readonly form: FormGroup = this.fb.group({
    reason: ['', [Validators.required, Validators.minLength(5)]],
  });

  protected onReject(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const reason = this.form.get('reason')!.value.trim();
    this.saving.set(true);
    this.scholarshipService.reject(this.data.application.id, { reason }).subscribe({
      next: (updated) => {
        this.saving.set(false);
        this.toast.success('Application rejected');
        this.dialogRef.close(updated);
      },
      error: (err) => {
        this.saving.set(false);
        this.toast.error(err?.error?.message ?? 'Rejection failed');
      },
    });
  }

  protected onCancel(): void {
    this.dialogRef.close();
  }
}


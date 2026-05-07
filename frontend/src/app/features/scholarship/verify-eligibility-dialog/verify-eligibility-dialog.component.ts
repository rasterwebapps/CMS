import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ScholarshipEligibility } from '../scholarship.model';
import { ScholarshipService } from '../scholarship.service';
import { ToastService } from '../../../core/toast/toast.service';

export interface VerifyEligibilityDialogData {
  studentId: number;
  studentName: string;
  eligibility: ScholarshipEligibility | null;
}

@Component({
  selector: 'app-verify-eligibility-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  template: `
    <h2 mat-dialog-title class="dialog-title">
      <mat-icon>verified</mat-icon>
      Verify Eligibility
      <span class="dialog-title__sub">{{ data.studentName }}</span>
    </h2>

    <mat-dialog-content>
      <form [formGroup]="form" class="dialog-form">
        @if (data.eligibility?.verifiedAt) {
          <div class="verify-already-done">
            <mat-icon>check_circle</mat-icon>
            Already verified by <strong>{{ data.eligibility!.verifiedBy }}</strong>
          </div>
        }
        <div class="field-group">
          <label for="ved-remarks" class="field-label">Verification Remarks</label>
          <textarea
            id="ved-remarks"
            class="field-textarea"
            formControlName="remarks"
            rows="3"
            placeholder="Documents checked and verified. Certificates look genuine.">
          </textarea>
          <p class="field-hint">This remark is recorded as an audit trail. Leave blank if no notes needed.</p>
        </div>
      </form>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-stroked-button type="button" (click)="onCancel()" [disabled]="saving()">Cancel</button>
      <button mat-flat-button color="primary" type="button" (click)="onVerify()" [disabled]="saving()">
        @if (saving()) {
          <mat-spinner diameter="18" class="btn-spinner"></mat-spinner>
        } @else {
          <mat-icon>verified</mat-icon>
        }
        Confirm Verification
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .dialog-title {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      &__sub {
        font-size: 0.8rem;
        font-weight: 400;
        color: var(--mat-sys-on-surface-variant);
        margin-left: auto;
      }
    }
    .verify-already-done {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.75rem 1rem;
      border-radius: 8px;
      background: var(--mat-sys-surface-container);
      color: var(--mat-sys-on-surface-variant);
      font-size: 0.875rem;
      margin-bottom: 1rem;
      mat-icon { color: var(--mat-sys-primary); }
    }
  `,
})
export class VerifyEligibilityDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<VerifyEligibilityDialogComponent>);
  protected readonly data: VerifyEligibilityDialogData = inject(MAT_DIALOG_DATA);
  private readonly scholarshipService = inject(ScholarshipService);
  private readonly toast = inject(ToastService);

  protected saving = signal(false);

  protected readonly form: FormGroup = this.fb.group({
    remarks: [this.data.eligibility?.verificationRemarks ?? ''],
  });

  protected onVerify(): void {
    const remarks = this.form.get('remarks')?.value?.trim() || undefined;
    this.saving.set(true);
    this.scholarshipService.verifyEligibility(this.data.studentId, remarks).subscribe({
      next: (updated) => {
        this.saving.set(false);
        this.toast.success('Eligibility verified successfully');
        this.dialogRef.close(updated);
      },
      error: (err) => {
        this.saving.set(false);
        this.toast.error(err?.error?.message ?? 'Failed to verify eligibility');
      },
    });
  }

  protected onCancel(): void {
    this.dialogRef.close();
  }
}


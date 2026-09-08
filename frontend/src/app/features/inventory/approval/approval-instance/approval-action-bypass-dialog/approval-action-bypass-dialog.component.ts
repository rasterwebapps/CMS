import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { APPROVAL_EXCEPTION_REASONS, ApprovalExceptionReason } from '../approval-instance.model';

export interface ApprovalActionBypassDialogData {
  stepName: string;
}

export interface ApprovalActionBypassDialogResult {
  reason: ApprovalExceptionReason;
  notes?: string;
}

/** Structured exception reason + optional notes for bypassing an approval step — mirrors the
 *  Wanted List reject dialog's fixed-taxonomy-plus-notes shape. See the "Exception handling
 *  slice" decision-log entry. */
@Component({
  selector: 'app-approval-action-bypass-dialog',
  standalone: true,
  imports: [FormsModule, MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>Bypass Step (Exception)</h2>
    <mat-dialog-content class="ab-bypass-content">
      <p class="ab-bypass-step">{{ data.stepName }}</p>
      <p class="ab-bypass-warning">This overrides the step's normal approver requirement. Document why.</p>
      <div class="field-group">
        <label class="field-label">Reason</label>
        <select class="field-select" [(ngModel)]="reason" aria-label="Exception reason">
          @for (r of reasons; track r.value) {
            <option [ngValue]="r.value">{{ r.label }}</option>
          }
        </select>
      </div>
      <div class="field-group">
        <label class="field-label">Notes (optional)</label>
        <textarea class="field-input" rows="3" [(ngModel)]="notes" placeholder="Add any context..." aria-label="Bypass notes"></textarea>
      </div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button (click)="dialogRef.close(null)">Cancel</button>
      <button mat-flat-button color="warn" (click)="confirm()">Bypass Step</button>
    </mat-dialog-actions>
  `,
  styles: `
    .ab-bypass-content { display: flex; flex-direction: column; gap: 12px; min-width: 340px; }
    .ab-bypass-step { margin: 0; font-weight: 600; }
    .ab-bypass-warning { margin: 0; font-size: 0.8125rem; color: var(--cms-text-muted); }
  `,
})
export class ApprovalActionBypassDialogComponent {
  protected readonly dialogRef = inject(MatDialogRef<ApprovalActionBypassDialogComponent, ApprovalActionBypassDialogResult | null>);
  protected readonly data: ApprovalActionBypassDialogData = inject(MAT_DIALOG_DATA);
  protected readonly reasons = APPROVAL_EXCEPTION_REASONS;

  protected reason: ApprovalExceptionReason = 'OTHER';
  protected notes = '';

  protected confirm(): void {
    this.dialogRef.close({ reason: this.reason, notes: this.notes.trim() || undefined });
  }
}

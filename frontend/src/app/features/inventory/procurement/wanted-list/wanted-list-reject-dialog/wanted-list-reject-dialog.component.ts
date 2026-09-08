import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { WANTED_LIST_REJECTION_REASONS, WantedListRejectionReason } from '../wanted-list.model';

export interface WantedListRejectDialogData {
  productName: string;
}

export interface WantedListRejectDialogResult {
  reason: WantedListRejectionReason;
  notes?: string;
}

/** Structured reason + optional notes for dismissing a Wanted List shortage line — see the
 *  "Wanted List slice" decision-log entry for why this pairs a fixed taxonomy with free text. */
@Component({
  selector: 'app-wanted-list-reject-dialog',
  standalone: true,
  imports: [FormsModule, MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>Reject Shortage Line</h2>
    <mat-dialog-content class="wl-reject-content">
      <p class="wl-reject-product">{{ data.productName }}</p>
      <div class="field-group">
        <label class="field-label">Reason</label>
        <select class="field-select" [(ngModel)]="reason" aria-label="Rejection reason">
          @for (r of reasons; track r.value) {
            <option [ngValue]="r.value">{{ r.label }}</option>
          }
        </select>
      </div>
      <div class="field-group">
        <label class="field-label">Notes (optional)</label>
        <textarea class="field-input" rows="3" [(ngModel)]="notes" placeholder="Add any context..." aria-label="Rejection notes"></textarea>
      </div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button (click)="dialogRef.close(null)">Cancel</button>
      <button mat-flat-button color="warn" (click)="confirm()">Reject Line</button>
    </mat-dialog-actions>
  `,
  styles: `
    .wl-reject-content { display: flex; flex-direction: column; gap: 12px; min-width: 320px; }
    .wl-reject-product { margin: 0; font-weight: 600; }
  `,
})
export class WantedListRejectDialogComponent {
  protected readonly dialogRef = inject(MatDialogRef<WantedListRejectDialogComponent, WantedListRejectDialogResult | null>);
  protected readonly data: WantedListRejectDialogData = inject(MAT_DIALOG_DATA);
  protected readonly reasons = WANTED_LIST_REJECTION_REASONS;

  protected reason: WantedListRejectionReason = 'OTHER';
  protected notes = '';

  protected confirm(): void {
    this.dialogRef.close({ reason: this.reason, notes: this.notes.trim() || undefined });
  }
}

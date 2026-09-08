import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';

export interface AssetDisposeDialogData {
  assetTag: string;
}

export interface AssetDisposeDialogResult {
  reason: string;
  disposalValue?: number;
  disposalDate?: string;
}

/** Reason + optional disposal value/date for retiring an asset — see the "Disposal slice"
 *  decision-log entry: disposing also writes off any remaining on-hand stock for the asset's
 *  product at its location, so this action needs a real reason on record, not a bare status flip. */
@Component({
  selector: 'app-asset-dispose-dialog',
  standalone: true,
  imports: [FormsModule, MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>Dispose Asset</h2>
    <mat-dialog-content class="ad-dispose-content">
      <p class="ad-dispose-tag">{{ data.assetTag }}</p>
      <p class="ad-dispose-warning">This moves the asset to DISPOSED and writes off any remaining on-hand stock for its product at its location. This cannot be undone from here.</p>
      <div class="field-group">
        <label class="field-label">Reason <span class="required-star">*</span></label>
        <textarea class="field-input" rows="2" [(ngModel)]="reason" placeholder="Why is this asset being disposed?" aria-label="Disposal reason"></textarea>
      </div>
      <div class="field-group">
        <label class="field-label">Disposal Value</label>
        <input type="number" step="0.01" min="0" class="field-input" [(ngModel)]="disposalValue" placeholder="Optional — salvage/sale proceeds" aria-label="Disposal value" />
      </div>
      <div class="field-group">
        <label class="field-label">Disposal Date</label>
        <input type="date" class="field-input" [(ngModel)]="disposalDate" aria-label="Disposal date" />
      </div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button (click)="dialogRef.close(null)">Cancel</button>
      <button mat-flat-button color="warn" [disabled]="!reason.trim()" (click)="confirm()">Dispose Asset</button>
    </mat-dialog-actions>
  `,
  styles: `
    .ad-dispose-content { display: flex; flex-direction: column; gap: 12px; min-width: 340px; }
    .ad-dispose-tag { margin: 0; font-weight: 600; }
    .ad-dispose-warning { margin: 0; font-size: 0.8125rem; color: var(--cms-text-muted); }
  `,
})
export class AssetDisposeDialogComponent {
  protected readonly dialogRef = inject(MatDialogRef<AssetDisposeDialogComponent, AssetDisposeDialogResult | null>);
  protected readonly data: AssetDisposeDialogData = inject(MAT_DIALOG_DATA);

  protected reason = '';
  protected disposalValue: number | null = null;
  protected disposalDate = new Date().toISOString().slice(0, 10);

  protected confirm(): void {
    if (!this.reason.trim()) return;
    this.dialogRef.close({
      reason: this.reason.trim(),
      disposalValue: this.disposalValue ?? undefined,
      disposalDate: this.disposalDate || undefined,
    });
  }
}

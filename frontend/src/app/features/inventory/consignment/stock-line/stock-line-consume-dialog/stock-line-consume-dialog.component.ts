import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { ConsignmentStockConsumeRequest } from '../stock-line.model';

export interface StockLineConsumeDialogData {
  productName: string;
  qtyOnHand: number;
}

/** Records that a portion of a consignment stock line's on-hand balance has been consumed —
 *  i.e. ownership should now transfer and the supplier should bill for it. A purely financial
 *  reconciliation action; does not itself move any physical stock (that already happened
 *  independently through whatever flow actually used the stock). See the "Consignment stock
 *  slice" decision-log entry. */
@Component({
  selector: 'app-stock-line-consume-dialog',
  standalone: true,
  imports: [FormsModule, MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>Record Consumption</h2>
    <mat-dialog-content class="cc-content">
      <p class="cc-product">{{ data.productName }}</p>
      <p class="cc-note">{{ data.qtyOnHand }} still on hand and unconsumed for this line.</p>
      <div class="field-group">
        <label class="field-label">Quantity Consumed <span class="required-star">*</span></label>
        <input type="number" step="0.001" min="0.001" [max]="data.qtyOnHand" class="field-input" [(ngModel)]="quantity" aria-label="Quantity consumed" />
      </div>
      <div class="field-group">
        <label class="field-label">Notes (optional)</label>
        <textarea class="field-input" rows="2" [(ngModel)]="notes" placeholder="Add any billing/reference context..." aria-label="Consumption notes"></textarea>
      </div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button (click)="dialogRef.close(null)">Cancel</button>
      <button mat-flat-button color="primary" [disabled]="!valid()" (click)="confirm()">Record Consumption</button>
    </mat-dialog-actions>
  `,
  styles: `
    .cc-content { display: flex; flex-direction: column; gap: 12px; min-width: 340px; }
    .cc-product { margin: 0; font-weight: 600; }
    .cc-note { margin: 0; font-size: 0.8125rem; color: var(--cms-text-muted); }
  `,
})
export class StockLineConsumeDialogComponent {
  protected readonly dialogRef = inject(MatDialogRef<StockLineConsumeDialogComponent, ConsignmentStockConsumeRequest | null>);
  protected readonly data: StockLineConsumeDialogData = inject(MAT_DIALOG_DATA);

  protected quantity: number | null = null;
  protected notes = '';

  protected valid(): boolean {
    return !!this.quantity && this.quantity > 0 && this.quantity <= this.data.qtyOnHand;
  }

  protected confirm(): void {
    if (!this.valid()) return;
    this.dialogRef.close({ quantity: this.quantity!, notes: this.notes.trim() || undefined });
  }
}

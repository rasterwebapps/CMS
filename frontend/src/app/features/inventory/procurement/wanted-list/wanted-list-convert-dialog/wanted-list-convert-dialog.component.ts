import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { WantedListItem } from '../wanted-list.model';

export interface WantedListConvertDialogData {
  items: WantedListItem[];
  locationName: string;
}

export interface WantedListConvertDialogResult {
  lines: { itemId: number; qty: number }[];
  requisitionDate?: string;
  notes?: string;
}

interface ConvertLine {
  itemId: number;
  productName: string;
  productCode: string;
  uomCode: string | null;
  qty: number;
}

/** Collectively converts the selected Wanted List lines (all for the same location, enforced by
 *  the caller) into one new Purchase Requisition — the standard MRP "collective conversion" of
 *  Planned Orders. Lets the planner adjust each line's quantity before it's firmed in. */
@Component({
  selector: 'app-wanted-list-convert-dialog',
  standalone: true,
  imports: [FormsModule, MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>Convert to Purchase Requisition</h2>
    <mat-dialog-content class="wl-convert-content">
      <p class="wl-convert-location">For <strong>{{ data.locationName }}</strong> — {{ lines.length }} product(s)</p>

      <table class="wl-convert-table">
        <thead>
          <tr><th>Product</th><th>UOM</th><th class="cell-align-right">Qty</th></tr>
        </thead>
        <tbody>
          @for (line of lines; track line.itemId) {
            <tr>
              <td>
                <div class="wl-convert-product-name">{{ line.productName }}</div>
                <div class="wl-convert-product-code">{{ line.productCode }}</div>
              </td>
              <td>{{ line.uomCode || '—' }}</td>
              <td class="cell-align-right">
                <input type="number" step="0.001" min="0.001" class="field-input wl-convert-qty-input"
                  [(ngModel)]="line.qty" [attr.aria-label]="'Quantity for ' + line.productName" />
              </td>
            </tr>
          }
        </tbody>
      </table>

      <div class="field-group">
        <label class="field-label">Requisition Date (optional)</label>
        <input type="date" class="field-input" [(ngModel)]="requisitionDate" aria-label="Requisition date" />
      </div>
      <div class="field-group">
        <label class="field-label">Notes (optional)</label>
        <textarea class="field-input" rows="2" [(ngModel)]="notes" placeholder="Auto-created from Wanted List" aria-label="Requisition notes"></textarea>
      </div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button (click)="dialogRef.close(null)">Cancel</button>
      <button mat-flat-button color="primary" [disabled]="!canConfirm()" (click)="confirm()">Create Requisition</button>
    </mat-dialog-actions>
  `,
  styles: `
    .wl-convert-content { display: flex; flex-direction: column; gap: 14px; min-width: 420px; }
    .wl-convert-location { margin: 0; color: var(--mat-sys-on-surface-variant); }
    .wl-convert-table { width: 100%; border-collapse: collapse; }
    .wl-convert-table th { text-align: left; font-size: 12px; font-weight: 600; color: var(--mat-sys-on-surface-variant); padding: 4px 8px; }
    .wl-convert-table td { padding: 4px 8px; border-top: 1px solid var(--cms-border-default); }
    .wl-convert-product-name { font-weight: 600; }
    .wl-convert-product-code { font-size: 12px; color: var(--mat-sys-on-surface-variant); }
    .wl-convert-qty-input { width: 100px; text-align: right; }
  `,
})
export class WantedListConvertDialogComponent {
  protected readonly dialogRef = inject(MatDialogRef<WantedListConvertDialogComponent, WantedListConvertDialogResult | null>);
  protected readonly data: WantedListConvertDialogData = inject(MAT_DIALOG_DATA);

  protected readonly lines: ConvertLine[] = this.data.items.map((item) => ({
    itemId: item.id,
    productName: item.productName,
    productCode: item.productCode,
    uomCode: item.uomCode,
    qty: item.suggestedQty,
  }));

  protected requisitionDate = '';
  protected notes = '';

  protected canConfirm(): boolean {
    return this.lines.every((l) => l.qty != null && l.qty > 0);
  }

  protected confirm(): void {
    this.dialogRef.close({
      lines: this.lines.map((l) => ({ itemId: l.itemId, qty: l.qty })),
      requisitionDate: this.requisitionDate || undefined,
      notes: this.notes.trim() || undefined,
    });
  }
}

import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { ConsignmentAgreementService } from '../../agreement/agreement.service';
import { ConsignmentAgreement } from '../../agreement/agreement.model';
import { CmsProductPickerComponent } from '../../../../../shared/product-picker/product-picker.component';
import { ConsignmentStockReceiveRequest } from '../stock-line.model';

export interface StockLineReceiveDialogData {
  agreementId?: number | null;
}

/** Receive vendor-owned stock against a Consignment Agreement — mirrors the approval bypass
 *  dialog's inline-template pattern. See the "Consignment stock slice" decision-log entry. */
@Component({
  selector: 'app-stock-line-receive-dialog',
  standalone: true,
  imports: [FormsModule, MatDialogModule, MatButtonModule, CmsProductPickerComponent],
  template: `
    <h2 mat-dialog-title>Receive Consignment Stock</h2>
    <mat-dialog-content class="rc-content">
      <div class="field-group">
        <label class="field-label">Agreement <span class="required-star">*</span></label>
        <select class="field-select" [(ngModel)]="agreementId" aria-label="Consignment agreement">
          <option [ngValue]="null" disabled>Select an agreement</option>
          @for (a of agreements(); track a.id) {
            <option [ngValue]="a.id">{{ a.agreementNumber }} — {{ a.supplierName }} ({{ a.locationVirtualName }})</option>
          }
        </select>
      </div>
      <div class="field-group">
        <cms-product-picker
          label="Product"
          [selectedProductId]="productId"
          (selectedProductIdChange)="productId = $event"
        />
      </div>
      <div class="rc-row">
        <div class="field-group">
          <label class="field-label">Quantity <span class="required-star">*</span></label>
          <input type="number" step="0.001" min="0.001" class="field-input" [(ngModel)]="quantity" aria-label="Quantity" />
        </div>
        <div class="field-group">
          <label class="field-label">Consignment Price / Unit <span class="required-star">*</span></label>
          <input type="number" step="0.01" min="0" class="field-input" [(ngModel)]="consignmentPrice" aria-label="Consignment price per unit" />
        </div>
      </div>
      <div class="field-group">
        <label class="field-label">Notes (optional)</label>
        <textarea class="field-input" rows="2" [(ngModel)]="notes" placeholder="Add any reference/context..." aria-label="Receive notes"></textarea>
      </div>
      <p class="rc-note">The received quantity posts to stock immediately as usable, but stays vendor-owned until consumption is recorded.</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button (click)="dialogRef.close(null)">Cancel</button>
      <button mat-flat-button color="primary" [disabled]="!valid()" (click)="confirm()">Receive Stock</button>
    </mat-dialog-actions>
  `,
  styles: `
    .rc-content { display: flex; flex-direction: column; gap: 12px; min-width: 380px; }
    .rc-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    .rc-note { margin: 0; font-size: 0.75rem; color: var(--cms-text-muted); }
  `,
})
export class StockLineReceiveDialogComponent implements OnInit {
  protected readonly dialogRef = inject(MatDialogRef<StockLineReceiveDialogComponent, ConsignmentStockReceiveRequest | null>);
  protected readonly data: StockLineReceiveDialogData = inject(MAT_DIALOG_DATA);
  private readonly agreementService = inject(ConsignmentAgreementService);

  protected readonly agreements = signal<ConsignmentAgreement[]>([]);

  protected agreementId: number | null = this.data?.agreementId ?? null;
  protected productId: number | null = null;
  protected quantity: number | null = null;
  protected consignmentPrice: number | null = null;
  protected notes = '';

  ngOnInit(): void {
    this.agreementService.getAllActive().subscribe({ next: (page) => this.agreements.set(page.content) });
  }

  protected valid(): boolean {
    return !!this.agreementId && !!this.productId && !!this.quantity && this.quantity > 0
      && this.consignmentPrice != null && this.consignmentPrice >= 0;
  }

  protected confirm(): void {
    if (!this.valid()) return;
    this.dialogRef.close({
      agreementId: this.agreementId!,
      productId: this.productId!,
      quantity: this.quantity!,
      consignmentPrice: this.consignmentPrice!,
      notes: this.notes.trim() || undefined,
    });
  }
}

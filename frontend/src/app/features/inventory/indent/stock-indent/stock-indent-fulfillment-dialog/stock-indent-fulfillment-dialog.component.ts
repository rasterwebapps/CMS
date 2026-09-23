import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { StockIndentService } from '../stock-indent.service';
import { StockIndentFulfillmentContext } from '../stock-indent.model';
import { ToastService } from '../../../../../core/toast/toast.service';

export interface StockIndentFulfillmentDialogData {
  requestId: number;
  lineId: number;
  productName: string;
}

type FulfillmentAction = 'fulfill' | 'transfer' | 'raise_po' | 'deny';

export type StockIndentFulfillmentDialogResult =
  | { action: 'fulfill'; notes?: string }
  | { action: 'transfer'; sourceLocationId: number; transferQty: number; notes?: string }
  | { action: 'raise_po'; qty: number; notes?: string }
  | { action: 'deny'; notes?: string };

/**
 * The store's own fulfillment decision on an APPROVED line (Phase D of the Stock Indent
 * auto-indent feature) — shows real context (own stock, other locations' surplus) so "transfer in
 * from a sub-location with abundant stock" is an informed choice, then lets the store pick exactly
 * one of the four outcomes. Manual decision only, no automatic action — see the "OC-206 reopened"
 * decision-log entry for why.
 */
@Component({
  selector: 'app-stock-indent-fulfillment-dialog',
  standalone: true,
  imports: [FormsModule, MatButtonModule, MatDialogModule, MatProgressSpinnerModule],
  templateUrl: './stock-indent-fulfillment-dialog.component.html',
  styleUrl: './stock-indent-fulfillment-dialog.component.scss',
})
export class StockIndentFulfillmentDialogComponent implements OnInit {
  protected readonly dialogRef = inject(MatDialogRef<StockIndentFulfillmentDialogComponent, StockIndentFulfillmentDialogResult | null>);
  protected readonly data: StockIndentFulfillmentDialogData = inject(MAT_DIALOG_DATA);
  private readonly requestService = inject(StockIndentService);
  private readonly toast = inject(ToastService);

  protected readonly loading = signal(true);
  protected readonly context = signal<StockIndentFulfillmentContext | null>(null);

  protected action: FulfillmentAction = 'fulfill';
  protected sourceLocationId: number | null = null;
  protected transferQty: number | null = null;
  protected poQty: number | null = null;
  protected notes = '';

  ngOnInit(): void {
    this.requestService.getFulfillmentContext(this.data.requestId, this.data.lineId).subscribe({
      next: (ctx) => {
        this.context.set(ctx);
        this.poQty = ctx.requestedQty;
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load fulfillment context'); this.loading.set(false); },
    });
  }

  protected get canConfirm(): boolean {
    if (this.action === 'transfer') return this.sourceLocationId != null && !!this.transferQty && this.transferQty > 0;
    if (this.action === 'raise_po') return !!this.poQty && this.poQty > 0;
    return true;
  }

  protected confirm(): void {
    if (!this.canConfirm) return;
    const notes = this.notes.trim() || undefined;
    if (this.action === 'fulfill') {
      this.dialogRef.close({ action: 'fulfill', notes });
    } else if (this.action === 'transfer') {
      this.dialogRef.close({ action: 'transfer', sourceLocationId: this.sourceLocationId!, transferQty: this.transferQty!, notes });
    } else if (this.action === 'raise_po') {
      this.dialogRef.close({ action: 'raise_po', qty: this.poQty!, notes });
    } else {
      this.dialogRef.close({ action: 'deny', notes });
    }
  }
}

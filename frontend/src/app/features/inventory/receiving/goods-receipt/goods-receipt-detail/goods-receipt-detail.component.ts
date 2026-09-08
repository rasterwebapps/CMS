import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { GoodsReceiptService } from '../goods-receipt.service';
import { GoodsReceipt, GoodsReceiptLine, ReceivablePurchaseOrderLine } from '../goods-receipt.model';
import { ConfirmDialogComponent } from '../../../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsStatusBadgeComponent } from '../../../../../shared/status-badge/status-badge.component';
import { ToastService } from '../../../../../core/toast/toast.service';

@Component({
  selector: 'app-goods-receipt-detail',
  standalone: true,
  imports: [
    RouterLink,
    FormsModule,
    DatePipe,
    DecimalPipe,
    MatDialogModule,
    MatProgressSpinnerModule,
    CmsStatusBadgeComponent,
  ],
  templateUrl: './goods-receipt-detail.component.html',
  styleUrl: './goods-receipt-detail.component.scss',
})
export class GoodsReceiptDetailComponent implements OnInit {
  private readonly route          = inject(ActivatedRoute);
  private readonly router         = inject(Router);
  private readonly receiptService = inject(GoodsReceiptService);
  private readonly dialog         = inject(MatDialog);
  private readonly toast          = inject(ToastService);

  protected readonly loading          = signal(false);
  protected readonly busy             = signal(false);
  protected readonly receipt          = signal<GoodsReceipt | null>(null);
  protected readonly receivableLines  = signal<ReceivablePurchaseOrderLine[]>([]);

  protected addPoItemId: number | null = null;
  protected addQty: number | null = null;
  protected addUnitCost: number | null = null;
  protected addBatchOrSerialNo = '';
  protected addExpiryDate = '';

  private receiptId!: number;

  ngOnInit(): void {
    this.receiptId = Number(this.route.snapshot.paramMap.get('id'));
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.receiptService.getById(this.receiptId).subscribe({
      next: (r) => {
        this.receipt.set(r);
        this.loading.set(false);
        if (r.status === 'DRAFT') this.loadReceivableLines(r.purchaseOrderId);
      },
      error: () => { this.toast.error('Failed to load goods receipt'); this.loading.set(false); },
    });
  }

  private loadReceivableLines(purchaseOrderId: number): void {
    this.receiptService.getReceivableLines(purchaseOrderId).subscribe({
      next: (lines) => this.receivableLines.set(lines),
      error: () => this.toast.error('Failed to load the order\'s open lines'),
    });
  }

  protected onPoItemChange(): void {
    const line = this.receivableLines().find((l) => l.id === this.addPoItemId);
    this.addQty = line ? line.openQty : null;
    this.addUnitCost = line ? line.unitPrice : null;
  }

  protected addLine(): void {
    if (this.addPoItemId == null || this.addQty == null || this.addQty <= 0) return;
    this.busy.set(true);
    this.receiptService.addLine(this.receiptId, {
      purchaseOrderItemId: this.addPoItemId,
      receivedQty: this.addQty,
      unitCost: this.addUnitCost ?? undefined,
      batchOrSerialNo: this.addBatchOrSerialNo.trim() || undefined,
      expiryDate: this.addExpiryDate || undefined,
    }).subscribe({
      next: () => {
        this.addPoItemId = null;
        this.addQty = null;
        this.addUnitCost = null;
        this.addBatchOrSerialNo = '';
        this.addExpiryDate = '';
        this.toast.success('Line added to the receipt');
        this.busy.set(false);
        this.load();
      },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to add line'); this.busy.set(false); },
    });
  }

  protected removeLine(line: GoodsReceiptLine): void {
    this.busy.set(true);
    this.receiptService.removeLine(this.receiptId, line.id).subscribe({
      next: () => { this.toast.success('Line removed'); this.busy.set(false); this.load(); },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to remove line'); this.busy.set(false); },
    });
  }

  protected confirmReceipt(): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Confirm Receipt',
        message: 'This posts each line to stock and cannot be undone from here. Continue?',
        confirmText: 'Confirm Receipt',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.busy.set(true);
      this.receiptService.confirm(this.receiptId).subscribe({
        next: () => { this.toast.success('Receipt confirmed and posted to stock'); this.busy.set(false); this.load(); },
        error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to confirm receipt'); this.busy.set(false); },
      });
    });
  }

  protected goBack(): void {
    void this.router.navigate(['/inventory/receiving/goods-receipts']);
  }
}

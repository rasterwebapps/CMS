import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { StockTransferService } from '../stock-transfer.service';
import { StockTransfer, StockTransferLine } from '../stock-transfer.model';
import { ConfirmDialogComponent } from '../../../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsStatusBadgeComponent } from '../../../../../shared/status-badge/status-badge.component';
import { CmsProductPickerComponent } from '../../../../../shared/product-picker/product-picker.component';
import { ToastService } from '../../../../../core/toast/toast.service';
import { ProductVariantService } from '../../../product/product-variant/product-variant.service';
import { ProductVariant } from '../../../product/product-variant/product-variant.model';
import { InventoryRackService } from '../../../rack/inventory-rack.service';
import { InventoryBin } from '../../../rack/inventory-rack.model';

@Component({
  selector: 'app-stock-transfer-detail',
  standalone: true,
  imports: [
    RouterLink,
    FormsModule,
    DatePipe,
    MatDialogModule,
    MatProgressSpinnerModule,
    CmsStatusBadgeComponent,
    CmsProductPickerComponent,
  ],
  templateUrl: './stock-transfer-detail.component.html',
  styleUrl: './stock-transfer-detail.component.scss',
})
export class StockTransferDetailComponent implements OnInit {
  private readonly route           = inject(ActivatedRoute);
  private readonly router          = inject(Router);
  private readonly transferService = inject(StockTransferService);
  private readonly dialog          = inject(MatDialog);
  private readonly toast           = inject(ToastService);
  private readonly variantService  = inject(ProductVariantService);
  private readonly rackService     = inject(InventoryRackService);

  protected readonly loading  = signal(false);
  protected readonly busy     = signal(false);
  protected readonly transfer = signal<StockTransfer | null>(null);
  protected readonly addProductId = signal<number | null>(null);
  protected readonly addVariantId = signal<number | null>(null);
  protected readonly addQty       = signal<number | null>(null);
  protected readonly variants     = signal<ProductVariant[]>([]);
  protected readonly addSourceBinId      = signal<number | null>(null);
  protected readonly addDestinationBinId = signal<number | null>(null);
  // Active bins in the transfer's source/destination locations — a line's source/destination bin
  // is always within the matching one of these two.
  protected readonly sourceBins      = signal<InventoryBin[]>([]);
  protected readonly destinationBins = signal<InventoryBin[]>([]);

  private transferId!: number;

  ngOnInit(): void {
    this.transferId = Number(this.route.snapshot.paramMap.get('id'));
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.transferService.getById(this.transferId).subscribe({
      next: (t) => {
        this.transfer.set(t);
        this.loading.set(false);
        if (t.status === 'DRAFT' && !this.sourceBins().length && !this.destinationBins().length) {
          this.rackService.getBins(undefined, t.sourceLocationId, true).subscribe({ next: (b) => this.sourceBins.set(b) });
          this.rackService.getBins(undefined, t.destinationLocationId, true).subscribe({ next: (b) => this.destinationBins.set(b) });
        }
      },
      error: () => { this.toast.error('Failed to load stock transfer'); this.loading.set(false); },
    });
  }

  protected onProductChange(productId: number | null): void {
    this.addProductId.set(productId);
    this.addVariantId.set(null);
    this.variants.set([]);
    if (productId == null) return;
    this.variantService.findByProduct(productId).subscribe({
      next: (variants) => this.variants.set(variants.filter(v => v.isActive)),
      error: () => { /* no variants for this product — not an error */ },
    });
  }

  protected addLine(): void {
    const productId = this.addProductId();
    const quantity = this.addQty();
    if (productId == null || quantity == null || quantity <= 0) return;
    if (this.variants().length > 0 && this.addVariantId() == null) {
      this.toast.error('This product has variants — select one before adding it');
      return;
    }
    this.busy.set(true);
    this.transferService.addLine(this.transferId, {
      productId,
      variantId: this.addVariantId() ?? undefined,
      quantity,
      sourceBinId: this.addSourceBinId() ?? undefined,
      destinationBinId: this.addDestinationBinId() ?? undefined,
    }).subscribe({
      next: () => {
        this.addProductId.set(null);
        this.addVariantId.set(null);
        this.addQty.set(null);
        this.addSourceBinId.set(null);
        this.addDestinationBinId.set(null);
        this.variants.set([]);
        this.toast.success('Product added to the transfer');
        this.busy.set(false);
        this.load();
      },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to add product'); this.busy.set(false); },
    });
  }

  protected removeLine(line: StockTransferLine): void {
    this.busy.set(true);
    this.transferService.removeLine(this.transferId, line.id).subscribe({
      next: () => { this.toast.success('Line removed'); this.busy.set(false); this.load(); },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to remove line'); this.busy.set(false); },
    });
  }

  protected completeTransfer(): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Complete Transfer',
        message: 'This posts stock out of the source location and into the destination. Continue?',
        confirmText: 'Complete Transfer',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.busy.set(true);
      this.transferService.complete(this.transferId).subscribe({
        next: () => { this.toast.success('Transfer completed and posted to stock'); this.busy.set(false); this.load(); },
        error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to complete transfer'); this.busy.set(false); },
      });
    });
  }

  protected cancelTransfer(): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Cancel Transfer',
        message: 'This abandons the transfer entirely. Continue?',
        confirmText: 'Cancel Transfer',
        cancelText: 'Keep Transfer',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.busy.set(true);
      this.transferService.cancel(this.transferId).subscribe({
        next: () => { this.toast.success('Transfer cancelled'); this.busy.set(false); this.load(); },
        error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to cancel transfer'); this.busy.set(false); },
      });
    });
  }

  protected goBack(): void {
    void this.router.navigate(['/inventory/stock/transfers']);
  }
}

import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PurchaseOrderService } from '../purchase-order.service';
import { AvailableRequisitionLine, PurchaseOrder, PurchaseOrderItem } from '../purchase-order.model';
import { TaxRuleService } from '../../tax-rule/tax-rule.service';
import { TaxRule } from '../../tax-rule/tax-rule.model';
import { ProductService } from '../../../product/product.service';
import { ProductUomLevel } from '../../../product/product.model';
import { ConfirmDialogComponent } from '../../../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsStatusBadgeComponent } from '../../../../../shared/status-badge/status-badge.component';
import { ToastService } from '../../../../../core/toast/toast.service';

@Component({
  selector: 'app-purchase-order-detail',
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
  templateUrl: './purchase-order-detail.component.html',
  styleUrl: './purchase-order-detail.component.scss',
})
export class PurchaseOrderDetailComponent implements OnInit {
  private readonly route         = inject(ActivatedRoute);
  private readonly router        = inject(Router);
  private readonly orderService  = inject(PurchaseOrderService);
  private readonly taxRuleService = inject(TaxRuleService);
  private readonly productService = inject(ProductService);
  private readonly dialog        = inject(MatDialog);
  private readonly toast         = inject(ToastService);

  protected readonly loading         = signal(false);
  protected readonly busy            = signal(false);
  protected readonly order           = signal<PurchaseOrder | null>(null);
  protected readonly availableLines  = signal<AvailableRequisitionLine[]>([]);
  protected readonly taxRules        = signal<TaxRule[]>([]);
  // Non-base levels of the selected line's product's active unit-of-measure chain — the unit
  // picker's pool. Empty means the product has no hierarchy configured; it can only be ordered
  // in its base unit.
  protected readonly uomLevels       = signal<ProductUomLevel[]>([]);

  protected addRequisitionItemId: number | null = null;
  protected addQty: number | null = null;
  protected addUomLevelId: number | null = null;
  protected addUnitPrice: number | null = null;
  protected addTaxRuleId: number | null = null;
  protected forceCloseReason = '';

  private orderId!: number;

  ngOnInit(): void {
    this.orderId = Number(this.route.snapshot.paramMap.get('id'));
    this.taxRuleService.getAll(true).subscribe({ next: (t) => this.taxRules.set(t) });
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.orderService.getById(this.orderId).subscribe({
      next: (o) => {
        this.order.set(o);
        this.loading.set(false);
        if (o.status === 'PENDING') this.loadAvailableLines(o.locationId);
      },
      error: () => { this.toast.error('Failed to load purchase order'); this.loading.set(false); },
    });
  }

  private loadAvailableLines(locationId: number): void {
    this.orderService.getAvailableRequisitionLines(locationId).subscribe({
      next: (lines) => this.availableLines.set(lines),
      error: () => this.toast.error('Failed to load approved requisition lines'),
    });
  }

  protected addLine(): void {
    if (this.addRequisitionItemId == null) return;
    this.busy.set(true);
    this.orderService.addLine(this.orderId, {
      purchaseRequisitionItemId: this.addRequisitionItemId,
      orderedQty: this.addQty ?? undefined,
      uomLevelId: this.addUomLevelId ?? undefined,
      unitPrice: this.addUnitPrice ?? undefined,
      taxRuleId: this.addTaxRuleId ?? undefined,
    }).subscribe({
      next: () => {
        this.addRequisitionItemId = null;
        this.addQty = null;
        this.addUomLevelId = null;
        this.addUnitPrice = null;
        this.addTaxRuleId = null;
        this.uomLevels.set([]);
        this.toast.success('Line added to the order');
        this.busy.set(false);
        this.load();
      },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to add line'); this.busy.set(false); },
    });
  }

  protected removeLine(line: PurchaseOrderItem): void {
    this.busy.set(true);
    this.orderService.removeLine(this.orderId, line.id).subscribe({
      next: () => { this.toast.success('Line removed'); this.busy.set(false); this.load(); },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to remove line'); this.busy.set(false); },
    });
  }

  protected sendOrder(): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Send Order',
        message: 'Sending locks in the line items. Continue?',
        confirmText: 'Send Order',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.busy.set(true);
      this.orderService.order(this.orderId).subscribe({
        next: () => { this.toast.success('Order sent'); this.busy.set(false); this.load(); },
        error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to send order'); this.busy.set(false); },
      });
    });
  }

  protected forceClose(): void {
    if (!this.forceCloseReason.trim()) {
      this.toast.error('A reason is required to force-close an order');
      return;
    }
    this.busy.set(true);
    this.orderService.forceClose(this.orderId, { reason: this.forceCloseReason.trim() }).subscribe({
      next: () => { this.toast.success('Order force-closed'); this.busy.set(false); this.forceCloseReason = ''; this.load(); },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to force-close order'); this.busy.set(false); },
    });
  }

  protected onRequisitionLineChange(): void {
    const line = this.availableLines().find((l) => l.id === this.addRequisitionItemId);
    this.addQty = line ? line.requestedQty : null;
    this.addUomLevelId = null;
    this.addTaxRuleId = null;
    this.uomLevels.set([]);
    if (!line) return;
    this.productService.getActiveUomChain(line.productId).subscribe({
      next: (chain) => {
        const levels = (chain?.levels ?? []).filter(l => l.levelRank > 0);
        this.uomLevels.set(levels);
        const defaultLevel = levels.find(l => l.isDefaultPurchase);
        if (defaultLevel) this.addUomLevelId = defaultLevel.id;
      },
      error: () => { /* no hierarchy configured for this product — base unit only, not an error */ },
    });
    // Pre-fill the tax from the product's own default — still freely overridable in the select
    // above before "Add Line" is clicked. See the "HSN/SAC + default TaxRule" decision-log entry.
    this.productService.getById(line.productId).subscribe({
      next: (product) => { if (product.defaultTaxRuleId != null) this.addTaxRuleId = product.defaultTaxRuleId; },
      error: () => { /* no default configured, or fetch failed — leave the tax rule unset */ },
    });
  }

  protected onUomLevelChange(): void {
    // Quantity was typed against the previous unit's scale — clearing it avoids silently
    // treating, say, "5 Cartons" as "5 Boxes" after switching the unit.
    this.addQty = null;
  }

  protected goBack(): void {
    void this.router.navigate(['/inventory/procurement/purchase-orders']);
  }
}

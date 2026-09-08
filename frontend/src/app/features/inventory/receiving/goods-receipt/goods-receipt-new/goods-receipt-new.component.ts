import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { forkJoin } from 'rxjs';
import { GoodsReceiptService } from '../goods-receipt.service';
import { GoodsReceiptCreateRequest } from '../goods-receipt.model';
import { PurchaseOrderService } from '../../../procurement/purchase-order/purchase-order.service';
import { PurchaseOrder } from '../../../procurement/purchase-order/purchase-order.model';
import { ToastService } from '../../../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../../../shared/utils/scroll-to-invalid';
import { cmsFieldError } from '../../../../../shared/validators/cms-validators';

const RECEIVABLE_STATUSES = ['ORDERED', 'IN_PROGRESS', 'PARTIALLY_COMPLETED'];

@Component({
  selector: 'app-goods-receipt-new',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './goods-receipt-new.component.html',
  styleUrl: './goods-receipt-new.component.scss',
})
export class GoodsReceiptNewComponent implements OnInit {
  private readonly fb            = inject(FormBuilder);
  private readonly router        = inject(Router);
  private readonly receiptService = inject(GoodsReceiptService);
  private readonly orderService  = inject(PurchaseOrderService);
  private readonly toast         = inject(ToastService);

  protected readonly saving = signal(false);
  protected readonly orders = signal<PurchaseOrder[]>([]);

  protected readonly form: FormGroup = this.fb.group({
    purchaseOrderId: [null as number | null, [Validators.required]],
    receiptDate:     [new Date().toISOString().slice(0, 10), [Validators.required]],
    notes:           ['', [Validators.maxLength(500)]],
  });

  ngOnInit(): void {
    forkJoin(RECEIVABLE_STATUSES.map((status) => this.orderService.getPage({ status, size: 100 })))
      .subscribe({
        next: (pages) => this.orders.set(pages.flatMap((p) => p.content)),
      });
  }

  protected getErrorMessage(fieldName: string): string {
    const labels: Record<string, string> = { purchaseOrderId: 'Purchase order', receiptDate: 'Receipt date' };
    return cmsFieldError(this.form.get(fieldName), labels[fieldName] ?? fieldName);
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }

    const v = this.form.value;
    const request: GoodsReceiptCreateRequest = {
      purchaseOrderId: v.purchaseOrderId,
      receiptDate: v.receiptDate,
      notes: v.notes?.trim() || undefined,
    };

    this.saving.set(true);
    this.receiptService.create(request).subscribe({
      next: (receipt) => {
        this.toast.success('Goods receipt started');
        this.saving.set(false);
        void this.router.navigate(['/inventory/receiving/goods-receipts', receipt.id]);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to start goods receipt');
        this.saving.set(false);
      },
    });
  }
}

import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { SupplierReturnService } from '../supplier-return.service';
import { SupplierReturnCreateRequest } from '../supplier-return.model';
import { GoodsReceiptService } from '../../goods-receipt/goods-receipt.service';
import { GoodsReceipt } from '../../goods-receipt/goods-receipt.model';
import { ToastService } from '../../../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../../../shared/utils/scroll-to-invalid';
import { cmsFieldError } from '../../../../../shared/validators/cms-validators';

@Component({
  selector: 'app-supplier-return-new',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './supplier-return-new.component.html',
  styleUrl: './supplier-return-new.component.scss',
})
export class SupplierReturnNewComponent implements OnInit {
  private readonly fb             = inject(FormBuilder);
  private readonly router         = inject(Router);
  private readonly returnService  = inject(SupplierReturnService);
  private readonly receiptService = inject(GoodsReceiptService);
  private readonly toast          = inject(ToastService);

  protected readonly saving   = signal(false);
  protected readonly receipts = signal<GoodsReceipt[]>([]);

  protected readonly reasons = [
    { value: 'DEFECTIVE', label: 'Defective' },
    { value: 'WRONG_ITEM', label: 'Wrong Item' },
    { value: 'DAMAGED_IN_TRANSIT', label: 'Damaged in Transit' },
    { value: 'QUALITY_ISSUE', label: 'Quality Issue' },
    { value: 'OTHER', label: 'Other' },
  ];

  protected readonly form: FormGroup = this.fb.group({
    goodsReceiptId: [null as number | null, [Validators.required]],
    returnDate:     [new Date().toISOString().slice(0, 10), [Validators.required]],
    reason:         [null as string | null],
    notes:          ['', [Validators.maxLength(500)]],
  });

  ngOnInit(): void {
    this.receiptService.getPage({ status: 'CONFIRMED', size: 100 }).subscribe({
      next: (page) => this.receipts.set(page.content),
    });
  }

  protected getErrorMessage(fieldName: string): string {
    const labels: Record<string, string> = { goodsReceiptId: 'Goods receipt', returnDate: 'Return date' };
    return cmsFieldError(this.form.get(fieldName), labels[fieldName] ?? fieldName);
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }

    const v = this.form.value;
    const request: SupplierReturnCreateRequest = {
      goodsReceiptId: v.goodsReceiptId,
      returnDate: v.returnDate,
      reason: v.reason || undefined,
      notes: v.notes?.trim() || undefined,
    };

    this.saving.set(true);
    this.returnService.create(request).subscribe({
      next: (ret) => {
        this.toast.success('Supplier return started');
        this.saving.set(false);
        void this.router.navigate(['/inventory/receiving/supplier-returns', ret.id]);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to start supplier return');
        this.saving.set(false);
      },
    });
  }
}

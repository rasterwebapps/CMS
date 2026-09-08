import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { StockService } from '../stock.service';
import { StockMovementRequest, StockTxnType } from '../stock.model';
import { ProductService } from '../../product/product.service';
import { Product } from '../../product/product.model';
import { InventoryLocationService } from '../../location/inventory-location.service';
import { InventoryLocation } from '../../location/inventory-location.model';
import { ToastService } from '../../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../../shared/utils/scroll-to-invalid';
import { cmsFieldError } from '../../../../shared/validators/cms-validators';

@Component({
  selector: 'app-stock-movement-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './stock-movement-form.component.html',
  styleUrl: './stock-movement-form.component.scss',
})
export class StockMovementFormComponent implements OnInit {
  private readonly fb              = inject(FormBuilder);
  private readonly router          = inject(Router);
  private readonly stockService    = inject(StockService);
  private readonly productService  = inject(ProductService);
  private readonly locationService = inject(InventoryLocationService);
  private readonly toast           = inject(ToastService);

  protected readonly saving    = signal(false);
  protected readonly products  = signal<Product[]>([]);
  protected readonly locations = signal<InventoryLocation[]>([]);

  protected readonly txnTypes: { value: StockTxnType; label: string; hint: string }[] = [
    { value: 'RECEIPT', label: 'Receipt', hint: 'New stock coming in (e.g. an opening balance, until Procurement/GRN exist)' },
    { value: 'ADJUSTMENT', label: 'Adjustment', hint: 'Correcting a count — found extra or found missing' },
    { value: 'DISPOSAL', label: 'Disposal', hint: 'Stock written off (expired, damaged, scrapped)' },
  ];

  protected readonly form: FormGroup = this.fb.group({
    productId:       [null as number | null, [Validators.required]],
    locationId:      [null as number | null, [Validators.required]],
    txnType:         ['RECEIPT' as StockTxnType, [Validators.required]],
    direction:       ['INCREASE' as 'INCREASE' | 'DECREASE'],
    quantity:        [null as number | null, [Validators.required, Validators.min(0.001)]],
    unitCost:        [null as number | null, [Validators.min(0)]],
    batchOrSerialNo: [''],
    expiryDate:      [''],
    notes:           ['', [Validators.maxLength(500)]],
  });

  ngOnInit(): void {
    this.productService.getPage({ page: 0, size: 500 }).subscribe({ next: (p) => this.products.set(p.content) });
    this.locationService.getAll(true).subscribe({ next: (l) => this.locations.set(l) });
  }

  protected getErrorMessage(fieldName: string): string {
    const labels: Record<string, string> = { productId: 'Product', locationId: 'Location', quantity: 'Quantity' };
    return cmsFieldError(this.form.get(fieldName), labels[fieldName] ?? fieldName);
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }

    const v = this.form.value;
    const request: StockMovementRequest = {
      productId: v.productId,
      locationId: v.locationId,
      txnType: v.txnType,
      direction: v.txnType === 'ADJUSTMENT' ? v.direction : undefined,
      quantity: v.quantity,
      unitCost: v.unitCost ?? undefined,
      batchOrSerialNo: v.batchOrSerialNo?.trim() || undefined,
      expiryDate: v.batchOrSerialNo?.trim() && v.expiryDate ? v.expiryDate : undefined,
      notes: v.notes?.trim() || undefined,
    };

    this.saving.set(true);
    this.stockService.recordMovement(request).subscribe({
      next: () => {
        this.toast.success('Stock movement recorded successfully');
        this.saving.set(false);
        void this.router.navigate(['/inventory/stock/balances']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to record stock movement');
        this.saving.set(false);
      },
    });
  }
}

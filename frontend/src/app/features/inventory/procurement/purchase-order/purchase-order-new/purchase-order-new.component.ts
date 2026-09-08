import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PurchaseOrderService } from '../purchase-order.service';
import { PurchaseOrderCreateRequest } from '../purchase-order.model';
import { SupplierService } from '../../supplier/supplier.service';
import { Supplier } from '../../supplier/supplier.model';
import { InventoryLocationService } from '../../../location/inventory-location.service';
import { InventoryLocation } from '../../../location/inventory-location.model';
import { ToastService } from '../../../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../../../shared/utils/scroll-to-invalid';
import { cmsFieldError } from '../../../../../shared/validators/cms-validators';

@Component({
  selector: 'app-purchase-order-new',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './purchase-order-new.component.html',
  styleUrl: './purchase-order-new.component.scss',
})
export class PurchaseOrderNewComponent implements OnInit {
  private readonly fb              = inject(FormBuilder);
  private readonly router          = inject(Router);
  private readonly orderService    = inject(PurchaseOrderService);
  private readonly supplierService = inject(SupplierService);
  private readonly locationService = inject(InventoryLocationService);
  private readonly toast           = inject(ToastService);

  protected readonly saving    = signal(false);
  protected readonly suppliers = signal<Supplier[]>([]);
  protected readonly locations = signal<InventoryLocation[]>([]);

  protected readonly form: FormGroup = this.fb.group({
    supplierId:            [null as number | null, [Validators.required]],
    locationId:             [null as number | null, [Validators.required]],
    poDate:                 [new Date().toISOString().slice(0, 10), [Validators.required]],
    expectedDeliveryDate:   [''],
    currencyCode:           ['INR', [Validators.maxLength(10)]],
    exchangeRate:           [null as number | null],
    notes:                  ['', [Validators.maxLength(500)]],
  });

  ngOnInit(): void {
    this.supplierService.getAll(true).subscribe({ next: (s) => this.suppliers.set(s) });
    this.locationService.getAll(true).subscribe({ next: (l) => this.locations.set(l) });
  }

  protected getErrorMessage(fieldName: string): string {
    const labels: Record<string, string> = { supplierId: 'Supplier', locationId: 'Location', poDate: 'Order date' };
    return cmsFieldError(this.form.get(fieldName), labels[fieldName] ?? fieldName);
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }

    const v = this.form.value;
    const request: PurchaseOrderCreateRequest = {
      supplierId: v.supplierId,
      locationId: v.locationId,
      poDate: v.poDate,
      expectedDeliveryDate: v.expectedDeliveryDate || undefined,
      currencyCode: v.currencyCode?.trim() || undefined,
      exchangeRate: v.exchangeRate ?? undefined,
      notes: v.notes?.trim() || undefined,
    };

    this.saving.set(true);
    this.orderService.create(request).subscribe({
      next: (order) => {
        this.toast.success('Purchase order started');
        this.saving.set(false);
        void this.router.navigate(['/inventory/procurement/purchase-orders', order.id]);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to start purchase order');
        this.saving.set(false);
      },
    });
  }
}

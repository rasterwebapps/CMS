import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { environment } from '../../../../../../environments';
import { VendorProductMappingService } from '../vendor-product-mapping.service';
import { VendorProductMappingRequest } from '../vendor-product-mapping.model';
import { SupplierService } from '../../supplier/supplier.service';
import { Supplier } from '../../supplier/supplier.model';
import { RateContractService } from '../../rate-contract/rate-contract.service';
import { RateContract } from '../../rate-contract/rate-contract.model';
import { UomService } from '../../../uom/uom.service';
import { Uom } from '../../../uom/uom.model';
import { ToastService } from '../../../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../../../shared/utils/scroll-to-invalid';
import { cmsFieldError } from '../../../../../shared/validators/cms-validators';
import { uniqueFieldValidator } from '../../../../../shared/validators/unique-field.validator';
import { CmsProductPickerComponent } from '../../../../../shared/product-picker/product-picker.component';

@Component({
  selector: 'app-vendor-product-mapping-form',
  standalone: true,
  imports: [
    RouterLink,
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    CmsProductPickerComponent,
  ],
  templateUrl: './vendor-product-mapping-form.component.html',
  styleUrl: './vendor-product-mapping-form.component.scss',
})
export class VendorProductMappingFormComponent implements OnInit {
  private readonly fb              = inject(FormBuilder);
  private readonly route           = inject(ActivatedRoute);
  private readonly router          = inject(Router);
  private readonly http            = inject(HttpClient);
  private readonly mappingService  = inject(VendorProductMappingService);
  private readonly supplierService = inject(SupplierService);
  private readonly rateContractService = inject(RateContractService);
  private readonly uomService      = inject(UomService);
  private readonly toast           = inject(ToastService);

  protected readonly loading    = signal(false);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly suppliers  = signal<Supplier[]>([]);
  protected readonly rateContracts = signal<RateContract[]>([]);
  protected readonly uoms       = signal<Uom[]>([]);

  private mappingId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    supplierId:     [null as number | null, [Validators.required]],
    productId:      [null as number | null, [Validators.required]],
    rateContractId: [null as number | null],
    vendorPartNumber:  ['', [Validators.maxLength(100)]],
    vendorProductName: ['', [Validators.maxLength(200)]],
    unitPrice:      [null as number | null, [Validators.required, Validators.min(0)]],
    currencyCode:   ['INR', [Validators.required, Validators.minLength(3), Validators.maxLength(3)]],
    uomId:          [null as number | null],
    minOrderQty:    [null as number | null, [Validators.min(0)]],
    leadTimeDays:   [null as number | null, [Validators.min(0)]],
    isPreferred:    [false],
  });

  ngOnInit(): void {
    this.supplierService.getAll(true).subscribe({ next: (s) => this.suppliers.set(s) });
    this.uomService.getAll(true).subscribe({ next: (u) => this.uoms.set(u) });

    this.form.get('supplierId')?.valueChanges.subscribe((supplierId) => {
      this.form.get('rateContractId')?.setValue(null);
      this.loadRateContracts(supplierId);
    });

    // A mapping's uniqueness is the (supplier, product) pair, not a single name/code field — the
    // shared uniqueFieldValidator is reused here with productId as the checked "value" and
    // supplierId as the scoping extra param, same pattern as Product's name-exists-within-category.
    this.form.get('productId')?.setAsyncValidators(
      uniqueFieldValidator(
        this.http,
        `${environment.apiUrl}/inventory/procurement/vendor-product-mappings/pair-exists`,
        () => this.mappingId,
        () => this.form.value.supplierId ? { supplierId: this.form.value.supplierId } : null
      )
    );

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.mappingId = Number(idParam);
      this.isEditMode.set(true);
      this.loadMapping();
    }
  }

  protected getErrorMessage(fieldName: string): string {
    const labels: Record<string, string> = {
      supplierId: 'Supplier', productId: 'Product', unitPrice: 'Unit price', currencyCode: 'Currency code',
    };
    if (fieldName === 'productId' && this.form.get('productId')?.hasError('duplicate')) {
      return 'This supplier already has an active rate for this product';
    }
    return cmsFieldError(this.form.get(fieldName), labels[fieldName] ?? fieldName);
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }

    const v = this.form.value;
    const request: VendorProductMappingRequest = {
      supplierId: v.supplierId,
      productId: v.productId,
      rateContractId: v.rateContractId ?? undefined,
      vendorPartNumber: v.vendorPartNumber?.trim() || undefined,
      vendorProductName: v.vendorProductName?.trim() || undefined,
      unitPrice: v.unitPrice,
      currencyCode: v.currencyCode?.toUpperCase(),
      uomId: v.uomId ?? undefined,
      minOrderQty: v.minOrderQty ?? undefined,
      leadTimeDays: v.leadTimeDays ?? undefined,
      isPreferred: v.isPreferred,
    };

    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.mappingService.update(this.mappingId!, request)
      : this.mappingService.create(request);

    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Vendor product rate updated successfully' : 'Vendor product rate created successfully');
        this.saving.set(false);
        void this.router.navigate(['/inventory/procurement/vendor-product-mappings']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? (this.isEditMode() ? 'Failed to update vendor product rate' : 'Failed to create vendor product rate'));
        this.saving.set(false);
      },
    });
  }

  private loadRateContracts(supplierId: number | null): void {
    if (supplierId == null) {
      this.rateContracts.set([]);
      return;
    }
    this.rateContractService.getPage({ supplierId, activeOnly: true, size: 100 }).subscribe({
      next: (page) => this.rateContracts.set(page.content),
      error: () => this.rateContracts.set([]),
    });
  }

  private loadMapping(): void {
    if (!this.mappingId) return;
    this.loading.set(true);
    this.mappingService.getById(this.mappingId).subscribe({
      next: (m) => {
        this.loadRateContracts(m.supplierId);
        this.form.patchValue({
          supplierId: m.supplierId,
          productId: m.productId,
          rateContractId: m.rateContractId,
          vendorPartNumber: m.vendorPartNumber ?? '',
          vendorProductName: m.vendorProductName ?? '',
          unitPrice: m.unitPrice,
          currencyCode: m.currencyCode,
          uomId: m.uomId,
          minOrderQty: m.minOrderQty,
          leadTimeDays: m.leadTimeDays,
          isPreferred: m.isPreferred,
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load vendor product rate');
        void this.router.navigate(['/inventory/procurement/vendor-product-mappings']);
      },
    });
  }
}

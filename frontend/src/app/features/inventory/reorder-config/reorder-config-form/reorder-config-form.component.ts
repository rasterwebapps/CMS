import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { environment } from '../../../../../environments';
import { ReorderConfigService } from '../reorder-config.service';
import { ProductLocationReorderConfigRequest } from '../reorder-config.model';
import { InventoryLocationService } from '../../location/inventory-location.service';
import { InventoryLocation } from '../../location/inventory-location.model';
import { ToastService } from '../../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../../shared/utils/scroll-to-invalid';
import { cmsFieldError } from '../../../../shared/validators/cms-validators';
import { uniqueFieldValidator } from '../../../../shared/validators/unique-field.validator';
import { CmsProductPickerComponent } from '../../../../shared/product-picker/product-picker.component';

@Component({
  selector: 'app-reorder-config-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    CmsProductPickerComponent,
  ],
  templateUrl: './reorder-config-form.component.html',
  styleUrl: './reorder-config-form.component.scss',
})
export class ReorderConfigFormComponent implements OnInit {
  private readonly fb              = inject(FormBuilder);
  private readonly route           = inject(ActivatedRoute);
  private readonly router          = inject(Router);
  private readonly http            = inject(HttpClient);
  private readonly configService   = inject(ReorderConfigService);
  private readonly locationService = inject(InventoryLocationService);
  private readonly toast           = inject(ToastService);

  protected readonly loading    = signal(false);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly locations  = signal<InventoryLocation[]>([]);

  private configId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    productId:          [null as number | null, [Validators.required]],
    locationId:         [null as number | null, [Validators.required]],
    reorderLevel:       [null as number | null, [Validators.required, Validators.min(0)]],
    reorderQty:         [null as number | null, [Validators.required, Validators.min(0.001)]],
    maxStockQty:        [null as number | null, [Validators.min(0)]],
    autoIndentEnabled:  [false],
  });

  /** The selected location's own default supplying store — read-only context shown next to the
   *  Auto-Indent checkbox so a missing one is obvious before the save-time gate rejects it. */
  protected readonly selectedLocationSupplyingStore = signal<string | null>(null);

  ngOnInit(): void {
    this.locationService.getAll(true).subscribe({
      next: (l) => this.locations.set(l.filter(loc => loc.locationRole === 'REQUESTING_POINT' || loc.locationRole === 'BOTH')),
    });

    this.form.get('locationId')?.valueChanges.subscribe((locationId: number | null) => {
      const loc = this.locations().find(l => l.id === locationId);
      this.selectedLocationSupplyingStore.set(loc?.defaultSupplyingLocationVirtualName ?? null);
    });

    // A config's uniqueness is the (product, location) pair, not a single name/code field — same
    // uniqueFieldValidator-as-pair-check pattern VendorProductMapping's own form uses.
    this.form.get('productId')?.setAsyncValidators(
      uniqueFieldValidator(
        this.http,
        `${environment.apiUrl}/inventory/stock/reorder-configs/pair-exists`,
        () => this.configId,
        () => this.form.value.locationId ? { locationId: this.form.value.locationId } : null
      )
    );

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.configId = Number(idParam);
      this.isEditMode.set(true);
      this.loadConfig();
    }
  }

  protected getErrorMessage(fieldName: string): string {
    const labels: Record<string, string> = {
      productId: 'Product', locationId: 'Location', reorderLevel: 'Reorder level', reorderQty: 'Reorder quantity', maxStockQty: 'Max stock quantity',
    };
    if (fieldName === 'productId' && this.form.get('productId')?.hasError('duplicate')) {
      return 'This location already has an active reorder configuration for this product';
    }
    return cmsFieldError(this.form.get(fieldName), labels[fieldName] ?? fieldName);
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }

    const v = this.form.value;
    const request: ProductLocationReorderConfigRequest = {
      productId: v.productId,
      locationId: v.locationId,
      reorderLevel: v.reorderLevel,
      reorderQty: v.reorderQty,
      maxStockQty: v.maxStockQty ?? undefined,
      autoIndentEnabled: v.autoIndentEnabled,
    };

    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.configService.update(this.configId!, request)
      : this.configService.create(request);

    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Reorder configuration updated successfully' : 'Reorder configuration created successfully');
        this.saving.set(false);
        void this.router.navigate(['/inventory/stock/reorder-configs']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? (this.isEditMode() ? 'Failed to update reorder configuration' : 'Failed to create reorder configuration'));
        this.saving.set(false);
      },
    });
  }

  private loadConfig(): void {
    if (!this.configId) return;
    this.loading.set(true);
    this.configService.getById(this.configId).subscribe({
      next: (c) => {
        this.selectedLocationSupplyingStore.set(c.defaultSupplyingLocationVirtualName);
        this.form.patchValue({
          productId: c.productId,
          locationId: c.locationId,
          reorderLevel: c.reorderLevel,
          reorderQty: c.reorderQty,
          maxStockQty: c.maxStockQty,
          autoIndentEnabled: c.autoIndentEnabled,
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load reorder configuration');
        void this.router.navigate(['/inventory/stock/reorder-configs']);
      },
    });
  }
}

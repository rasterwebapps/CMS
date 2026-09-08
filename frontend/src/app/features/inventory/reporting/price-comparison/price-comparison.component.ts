import { Component, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { VendorProductMappingService } from '../../procurement/vendor-product-mapping/vendor-product-mapping.service';
import { VendorProductMapping } from '../../procurement/vendor-product-mapping/vendor-product-mapping.model';
import { CmsProductPickerComponent } from '../../../../shared/product-picker/product-picker.component';
import { CmsEmptyStateComponent } from '../../../../shared/empty-state/empty-state.component';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ToastService } from '../../../../core/toast/toast.service';

/**
 * Phase 8's fifth Reporting slice — for one chosen Product, every active supplier's rate side
 * by side, cheapest first. Deliberately built with **no new backend code at all**: the existing
 * `VendorProductMappingController`'s `/page?productId=` endpoint already returns everything
 * needed, `effectivePrice`/`priceSource` included (the contract-override resolution
 * `VendorProductMappingService` already does for the Vendor Product Mapping list). This screen
 * is purely a different read-only presentation of data the module already fully computes. See
 * the "Price Comparison Report slice" decision-log entry.
 */
@Component({
  selector: 'app-price-comparison-report',
  standalone: true,
  imports: [
    DecimalPipe,
    MatProgressSpinnerModule,
    CmsProductPickerComponent,
    CmsEmptyStateComponent,
  ],
  templateUrl: './price-comparison.component.html',
  styleUrl: './price-comparison.component.scss',
})
export class PriceComparisonReportComponent {
  private readonly mappingService = inject(VendorProductMappingService);
  private readonly toast          = inject(ToastService);

  protected readonly loading    = signal(false);
  protected readonly selectedProductId = signal<number | null>(null);
  protected readonly rows       = signal<VendorProductMapping[]>([]);
  protected readonly hasSearched = signal(false);

  protected onProductChange(productId: number | null): void {
    this.selectedProductId.set(productId);
    if (productId == null) {
      this.rows.set([]);
      this.hasSearched.set(false);
      return;
    }
    this.hasSearched.set(true);
    this.loading.set(true);
    this.mappingService.getPage({ productId, activeOnly: true, size: 100 }).subscribe({
      next: (page) => {
        const sorted = [...page.content].sort((a, b) => a.effectivePrice - b.effectivePrice);
        this.rows.set(sorted);
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load supplier rates'); this.loading.set(false); },
    });
  }

  protected isCheapest(row: VendorProductMapping): boolean {
    return this.rows().length > 0 && this.rows()[0].id === row.id;
  }
}

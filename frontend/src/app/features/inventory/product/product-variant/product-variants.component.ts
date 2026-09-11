import { Component, Input, OnChanges, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ProductVariantService } from './product-variant.service';
import { ProductVariant } from './product-variant.model';
import { ProductBarcodePreviewDialogComponent, ProductBarcodePreviewDialogData } from '../product-barcode-preview-dialog/product-barcode-preview-dialog.component';
import { ConfirmDialogComponent } from '../../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsStatusBadgeComponent } from '../../../../shared/status-badge/status-badge.component';
import { CmsRowActionButtonComponent } from '../../../../shared/row-action-button/row-action-button.component';
import { CmsIconEditComponent, CmsIconToggleStatusComponent } from '../../../../shared/icons';
import { ToastService } from '../../../../core/toast/toast.service';

/**
 * A self-contained variant list widget for one Product — shown only in the Product form's edit
 * mode (a product must already exist before it can have variants), same posture as
 * ProductImagesComponent. See the 2026-09-11 "ProductVariant" decision-log entry.
 */
@Component({
  selector: 'app-product-variants',
  standalone: true,
  imports: [
    DecimalPipe, MatButtonModule, MatProgressSpinnerModule, MatDialogModule,
    CmsStatusBadgeComponent, CmsRowActionButtonComponent, CmsIconEditComponent, CmsIconToggleStatusComponent,
    ProductBarcodePreviewDialogComponent,
  ],
  templateUrl: './product-variants.component.html',
  styleUrl: './product-variants.component.scss',
})
export class ProductVariantsComponent implements OnChanges {
  @Input({ required: true }) productId!: number;

  private readonly variantService = inject(ProductVariantService);
  private readonly router         = inject(Router);
  private readonly dialog         = inject(MatDialog);
  private readonly toast          = inject(ToastService);

  protected readonly loading  = signal(false);
  protected readonly variants = signal<ProductVariant[]>([]);
  protected readonly barcodeTarget = signal<ProductBarcodePreviewDialogData | null>(null);

  ngOnChanges(): void {
    if (this.productId) this.load();
  }

  protected addVariant(): void {
    void this.router.navigate(['/inventory/products', this.productId, 'variants', 'new']);
  }

  protected editVariant(variant: ProductVariant): void {
    void this.router.navigate(['/inventory/products', this.productId, 'variants', variant.id, 'edit']);
  }

  protected printBarcode(variant: ProductVariant): void {
    const code = variant.barcode?.trim() || variant.variantCode;
    this.barcodeTarget.set({
      id: variant.id,
      productName: variant.variantName,
      code,
      fetchPng: (id) => this.variantService.getBarcodePng(id),
    });
  }

  protected toggleStatus(variant: ProductVariant): void {
    const nextAction = variant.isActive ? 'Deactivate' : 'Activate';
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: `${nextAction} Variant`,
        message: `${nextAction} "${variant.variantName}" (${variant.variantCode})?`,
        confirmText: nextAction,
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe(confirmed => {
      if (confirmed) this.performToggle(variant);
    });
  }

  protected deleteVariant(variant: ProductVariant): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Variant',
        message: `Permanently delete "${variant.variantName}" (${variant.variantCode})? This cannot be undone.`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.variantService.delete(this.productId, variant.id).subscribe({
        next: () => { this.toast.success('Variant deleted'); this.load(); },
        error: (err) => this.toast.error(err?.error?.message ?? 'Failed to delete variant'),
      });
    });
  }

  private performToggle(variant: ProductVariant): void {
    this.variantService.updateStatus(this.productId, variant.id, { isActive: !variant.isActive }).subscribe({
      next: () => {
        this.toast.success(`Variant ${variant.isActive ? 'deactivated' : 'activated'} successfully`);
        this.load();
      },
      error: (err) => this.toast.error(err?.error?.message ?? 'Failed to update variant status'),
    });
  }

  private load(): void {
    this.loading.set(true);
    this.variantService.findByProduct(this.productId).subscribe({
      next: (variants) => { this.variants.set(variants); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load variants'); this.loading.set(false); },
    });
  }
}

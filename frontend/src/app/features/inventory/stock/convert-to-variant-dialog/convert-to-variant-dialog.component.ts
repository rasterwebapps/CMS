import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { ProductVariantService } from '../../product/product-variant/product-variant.service';
import { ProductVariant } from '../../product/product-variant/product-variant.model';
import { ToastService } from '../../../../core/toast/toast.service';

export interface ConvertToVariantDialogData {
  productId: number;
  productName: string;
  qtyOnHand: number;
}

/**
 * Picks which active variant a stranded null-variant balance's *entire* remaining quantity moves
 * onto — see the 2026-09-15 "null-variant stock is stranded" specialist round. One variant per
 * conversion; splitting across several is still possible by converting the same row again against
 * its new (still-nonzero) remainder.
 */
@Component({
  selector: 'app-convert-to-variant-dialog',
  standalone: true,
  imports: [FormsModule, MatDialogModule, MatButtonModule, MatProgressSpinnerModule],
  template: `
    <h2 mat-dialog-title>Convert to Variant</h2>
    <mat-dialog-content>
      <p class="cvd-intro">
        "{{ data.productName }}" has active variants, so this unassigned balance of
        <strong>{{ data.qtyOnHand }}</strong> can no longer be adjusted, issued, or transferred
        until it's assigned to one. Choose which variant it belongs to — the entire quantity
        moves in one step.
      </p>
      @if (loading()) {
        <div class="cvd-loading"><mat-spinner diameter="28"></mat-spinner></div>
      } @else if (variants().length === 0) {
        <p class="cvd-empty">This product has no active variants to convert to.</p>
      } @else {
        <select class="field-select cvd-select" [(ngModel)]="selectedVariantId" aria-label="Select variant">
          <option [ngValue]="null">Select a variant…</option>
          @for (v of variants(); track v.id) {
            <option [ngValue]="v.id">{{ v.variantName }} ({{ v.variantCode }})</option>
          }
        </select>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button (click)="onCancel()">Cancel</button>
      <button mat-flat-button color="primary" [disabled]="!selectedVariantId" (click)="onConfirm()">Convert</button>
    </mat-dialog-actions>
  `,
  styles: `
    .cvd-intro { margin: 0 0 16px; color: var(--mat-sys-on-surface-variant); }
    .cvd-loading { display: flex; justify-content: center; padding: 16px 0; }
    .cvd-empty { margin: 0; color: var(--mat-sys-on-surface-variant); }
    .cvd-select { width: 100%; }
  `,
})
export class ConvertToVariantDialogComponent implements OnInit {
  protected readonly dialogRef = inject(MatDialogRef<ConvertToVariantDialogComponent>);
  protected readonly data: ConvertToVariantDialogData = inject(MAT_DIALOG_DATA);
  private readonly variantService = inject(ProductVariantService);
  private readonly toast = inject(ToastService);

  protected readonly loading = signal(true);
  protected readonly variants = signal<ProductVariant[]>([]);
  protected selectedVariantId: number | null = null;

  ngOnInit(): void {
    this.variantService.findByProduct(this.data.productId).subscribe({
      next: (variants) => {
        this.variants.set(variants.filter(v => v.isActive));
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load variants'); this.loading.set(false); },
    });
  }

  protected onCancel(): void {
    this.dialogRef.close(null);
  }

  protected onConfirm(): void {
    if (!this.selectedVariantId) return;
    this.dialogRef.close(this.selectedVariantId);
  }
}

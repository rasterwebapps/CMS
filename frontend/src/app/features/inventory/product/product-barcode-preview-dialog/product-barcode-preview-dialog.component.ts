import { Component, ElementRef, OnDestroy, OnInit, ViewChild, inject, input, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CmsFlyoutPanelComponent } from '../../../../shared/flyout-panel/flyout-panel.component';
import { ProductService } from '../product.service';
import { PrintService } from '../../../../core/print/print.service';
import { ToastService } from '../../../../core/toast/toast.service';

export interface ProductBarcodePreviewDialogData {
  id: number;
  productName: string;
  /** The code actually encoded — the captured barcode/GTIN if present, else productCode; kept
   *  here rather than re-derived so the caption always matches what the backend rendered. */
  code: string;
}

/**
 * Mirrors LibraryBarcodePreviewDialogComponent's shape (same fetch-blob/object-URL/PrintService
 * pattern) but deliberately without its printer-mode/ZPL-transport machinery — Inventory's first
 * barcode slice is browser-print only. See the 2026-09-11 "Barcode/GTIN" decision-log entry.
 */
@Component({
  selector: 'app-product-barcode-preview-dialog',
  standalone: true,
  imports: [MatButtonModule, MatProgressSpinnerModule, CmsFlyoutPanelComponent],
  templateUrl: './product-barcode-preview-dialog.component.html',
  styleUrl: './product-barcode-preview-dialog.component.scss',
})
export class ProductBarcodePreviewDialogComponent implements OnInit, OnDestroy {
  readonly target = input<ProductBarcodePreviewDialogData | null>(null);
  readonly closed = output<void>();

  private readonly productService = inject(ProductService);
  private readonly printService = inject(PrintService);
  private readonly toast = inject(ToastService);

  @ViewChild('barcodeImg', { read: ElementRef }) private barcodeImgRef?: ElementRef<HTMLElement>;

  protected readonly loading = signal(true);
  protected readonly imageUrl = signal<string | null>(null);

  ngOnInit(): void {
    const data = this.target()!;
    this.productService.getBarcodePng(data.id).subscribe({
      next: blob => {
        this.imageUrl.set(URL.createObjectURL(blob));
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to generate barcode');
        this.loading.set(false);
      },
    });
  }

  ngOnDestroy(): void {
    const url = this.imageUrl();
    if (url) URL.revokeObjectURL(url);
  }

  protected print(): void {
    if (!this.barcodeImgRef) return;
    this.printService.printElement(this.barcodeImgRef);
  }

  protected download(): void {
    const url = this.imageUrl();
    if (!url) return;
    const a = document.createElement('a');
    a.href = url;
    a.download = `barcode-${this.target()!.code}.png`;
    a.click();
  }

  protected close(): void {
    this.closed.emit();
  }
}

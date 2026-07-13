import { Component, ElementRef, OnDestroy, OnInit, ViewChild, inject, input, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CmsFlyoutPanelComponent } from '../../../shared/flyout-panel/flyout-panel.component';
import { LibraryService } from '../library.service';
import { LibraryPrintTransportService } from '../library-print-transport.service';
import { LibraryItemType } from '../library.model';
import { PrintService } from '../../../core/print/print.service';
import { ToastService } from '../../../core/toast/toast.service';

export interface LibraryBarcodePreviewDialogData {
  itemType: LibraryItemType;
  id: number;
  title: string;
  code: string;
}

@Component({
  selector: 'app-library-barcode-preview-dialog',
  standalone: true,
  imports: [MatButtonModule, MatProgressSpinnerModule, CmsFlyoutPanelComponent],
  templateUrl: './library-barcode-preview-dialog.component.html',
  styleUrl: './library-barcode-preview-dialog.component.scss',
})
export class LibraryBarcodePreviewDialogComponent implements OnInit, OnDestroy {
  readonly target = input<LibraryBarcodePreviewDialogData | null>(null);
  readonly closed = output<void>();

  private readonly libraryService = inject(LibraryService);
  private readonly printTransport = inject(LibraryPrintTransportService);
  private readonly printService = inject(PrintService);
  private readonly toast = inject(ToastService);

  @ViewChild('barcodeImg', { read: ElementRef }) private barcodeImgRef?: ElementRef<HTMLElement>;

  protected readonly loading = signal(true);
  protected readonly printing = signal(false);
  protected readonly imageUrl = signal<string | null>(null);

  ngOnInit(): void {
    const data = this.target()!;
    const fetch = data.itemType === 'BOOK'
      ? this.libraryService.getBookBarcodePng(data.id)
      : this.libraryService.getPeriodicalBarcodePng(data.id);

    fetch.subscribe({
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
    if (this.printing()) return;
    const data = this.target()!;

    this.printTransport.getPrinterMode().subscribe(mode => {
      if (mode === 'BROWSER') {
        if (this.barcodeImgRef) this.printService.printElement(this.barcodeImgRef);
        return;
      }

      this.printing.set(true);
      this.printTransport.sendSingle(data.itemType, data.id, mode).subscribe({
        next: result => {
          this.printing.set(false);
          if (result.success) this.toast.success('Sent to printer');
          else this.toast.error(result.message ?? 'Failed to send to printer');
        },
        error: () => {
          this.printing.set(false);
          this.toast.error('Failed to send to printer');
        },
      });
    });
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

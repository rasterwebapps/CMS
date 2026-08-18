import { Component, EventEmitter, Output, inject, signal } from '@angular/core';
import { CmsFlyoutPanelComponent } from '../../../shared/flyout-panel/flyout-panel.component';
import { ToastService } from '../../../core/toast/toast.service';
import { detectShapesFromCanvas } from './pdf-shape-detect.util';
import { DetectedShapeCandidate } from '../spatial.model';

const THUMBNAIL_SCALE = 0.35;
const EXPORT_SCALE = 2.5;
const MAX_FILE_SIZE = 30 * 1024 * 1024;

interface PageThumbnail {
  pageNumber: number;
  dataUrl: string;
}

export interface PdfImportResult {
  file: File;
  /** Candidate sub-components detected via OpenCV.js contour-finding + Tesseract.js OCR on the
   *  rasterized page (BR-60 Phase B) — approximate, unlike the DXF path's exact vector detection,
   *  but goes through the same staged review before anything is ever created. Empty when nothing
   *  was found, or when detection itself failed (a toast is shown; the import still succeeds with
   *  just the background image, same as before this feature existed). */
  detectedShapes: DetectedShapeCandidate[];
}

/**
 * Renders a PDF entirely in the browser via pdf.js and lets the admin pick one page to become
 * a Floor Plan — architect PDFs bundle a title slide, the top-down 2D plan, and often many 3D
 * render pages in one file (confirmed against real office plans). The chosen page is rasterized
 * to a PNG File and handed back to the caller exactly like a manually-picked image file, so the
 * rest of the create flow (and the backend) needs no changes at all.
 */
@Component({
  selector: 'app-pdf-import-flyout',
  standalone: true,
  imports: [CmsFlyoutPanelComponent],
  templateUrl: './pdf-import-flyout.component.html',
  styleUrl: './pdf-import-flyout.component.scss',
})
export class PdfImportFlyoutComponent {
  @Output() readonly closed = new EventEmitter<void>();
  @Output() readonly imported = new EventEmitter<PdfImportResult>();

  private readonly toast = inject(ToastService);

  protected readonly loading = signal(false);
  protected readonly exporting = signal(false);
  protected readonly detecting = signal(false);
  protected readonly pages = signal<PageThumbnail[]>([]);
  protected readonly selectedPage = signal<number | null>(null);
  protected readonly sourceFileName = signal('');

  private pdfDoc: import('pdfjs-dist').PDFDocumentProxy | null = null;

  protected async onFileSelected(event: Event): Promise<void> {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    if (!file) return;
    if (file.size > MAX_FILE_SIZE) {
      this.toast.error(`${file.name} exceeds the 30 MB limit`);
      (event.target as HTMLInputElement).value = '';
      return;
    }

    this.loading.set(true);
    this.pages.set([]);
    this.selectedPage.set(null);
    this.sourceFileName.set(file.name);

    try {
      const pdfjsLib = await import('pdfjs-dist');
      pdfjsLib.GlobalWorkerOptions.workerSrc = new URL('pdfjs-dist/build/pdf.worker.mjs', import.meta.url).toString();

      const buffer = await file.arrayBuffer();
      this.pdfDoc = await pdfjsLib.getDocument({ data: buffer }).promise;

      const thumbnails: PageThumbnail[] = [];
      for (let pageNumber = 1; pageNumber <= this.pdfDoc.numPages; pageNumber++) {
        const dataUrl = await this.renderPage(pageNumber, THUMBNAIL_SCALE);
        thumbnails.push({ pageNumber, dataUrl });
      }
      this.pages.set(thumbnails);
    } catch {
      this.toast.error('Failed to read this PDF — it may be corrupted or password-protected');
    } finally {
      this.loading.set(false);
    }
  }

  protected selectPage(pageNumber: number): void {
    this.selectedPage.set(pageNumber);
  }

  protected onClose(): void {
    this.closed.emit();
  }

  protected async onImport(): Promise<void> {
    const pageNumber = this.selectedPage();
    if (!pageNumber || !this.pdfDoc || this.exporting()) return;
    this.exporting.set(true);

    let file: File;
    let canvas: HTMLCanvasElement;
    try {
      canvas = await this.renderPageToCanvas(pageNumber, EXPORT_SCALE);
      const blob = await canvasToBlob(canvas);
      const baseName = this.sourceFileName().replace(/\.pdf$/i, '') || 'floor-plan';
      file = new File([blob], `${baseName}-page${pageNumber}.png`, { type: 'image/png' });
    } catch {
      this.toast.error('Failed to export the selected page');
      this.exporting.set(false);
      return;
    }
    this.exporting.set(false);

    this.detecting.set(true);
    const detectedShapes = await detectShapesFromCanvas(canvas).catch(() => {
      this.toast.error('Sub-component detection failed — the floor plan itself still imported fine, place markers manually');
      return [];
    });
    this.detecting.set(false);

    this.imported.emit({ file, detectedShapes });
  }

  private async renderPage(pageNumber: number, scale: number): Promise<string> {
    const canvas = await this.renderPageToCanvas(pageNumber, scale);
    return canvas.toDataURL('image/png');
  }

  private async renderPageToCanvas(pageNumber: number, scale: number): Promise<HTMLCanvasElement> {
    const page = await this.pdfDoc!.getPage(pageNumber);
    const viewport = page.getViewport({ scale });
    const canvas = document.createElement('canvas');
    canvas.width = viewport.width;
    canvas.height = viewport.height;
    const context = canvas.getContext('2d')!;
    await page.render({ canvas, canvasContext: context, viewport }).promise;
    return canvas;
  }
}

function canvasToBlob(canvas: HTMLCanvasElement): Promise<Blob> {
  return new Promise<Blob>((resolve, reject) => {
    canvas.toBlob((blob) => (blob ? resolve(blob) : reject(new Error('toBlob failed'))), 'image/png');
  });
}

import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
  inject,
  signal,
} from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

export interface PhotoCropDialogData   { file: File; }
export interface PhotoCropDialogResult { blob: Blob; }

const VIEWPORT_PX    = 320;  // size of the visible drag area in CSS px
const CROP_CIRCLE_PX = 280;  // diameter of the circular crop zone
const OUTPUT_PX      = 500;  // output canvas side length

@Component({
  selector: 'app-photo-crop-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule, MatIconModule, MatTooltipModule],
  templateUrl: './photo-crop-dialog.component.html',
  styleUrl:    './photo-crop-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PhotoCropDialogComponent implements OnInit, AfterViewInit, OnDestroy {

  // ── The preview IS a <canvas> — what you see is what gets cropped ──────────
  @ViewChild('previewCanvas') private canvasRef?: ElementRef<HTMLCanvasElement>;

  private readonly dialogRef =
    inject<MatDialogRef<PhotoCropDialogComponent, PhotoCropDialogResult>>(MatDialogRef);
  private readonly data = inject<PhotoCropDialogData>(MAT_DIALOG_DATA);

  // ── Loaded image ──────────────────────────────────────────────────────────
  private img: HTMLImageElement | null = null;
  private objectUrl = '';
  /** Natural pixel dimensions */
  private nw = 1;
  private nh = 1;
  /** Scale that makes the image cover the crop circle at zoom 1 */
  private fitScale = 1;
  /** Display dimensions (display-space pixels, used by canvas draw) */
  private dw = 0;
  private dh = 0;

  // ── User transform state (plain numbers, NOT signals, for perf) ────────────
  private _tx = 0;
  private _ty = 0;
  private _zoom = 1;
  private _fh = 1;   // 1 = normal, -1 = flip horizontal
  private _fv = 1;

  // ── Reactive signals for the template only ────────────────────────────────
  protected readonly imageReady = signal(false);
  protected readonly applying   = signal(false);

  // ── Constants exposed to template ─────────────────────────────────────────
  protected readonly VIEWPORT = VIEWPORT_PX;
  protected readonly CROP_R   = CROP_CIRCLE_PX / 2;

  // ── Drag state ─────────────────────────────────────────────────────────────
  private dragging = false;
  private lastX = 0;
  private lastY = 0;
  private readonly boundMove = this.onPointerMove.bind(this);
  private readonly boundUp   = this.onPointerUp.bind(this);

  private viewReady = false;

  // ══════════════════════════════════════════════════════════════════════════
  // Lifecycle
  // ══════════════════════════════════════════════════════════════════════════

  ngOnInit(): void {
    this.objectUrl = URL.createObjectURL(this.data.file);
    const img = new Image();
    img.onload = () => {
      this.img      = img;
      this.nw       = img.naturalWidth;
      this.nh       = img.naturalHeight;
      // Cover fit: neither dimension smaller than the crop circle
      this.fitScale = Math.max(CROP_CIRCLE_PX / this.nw, CROP_CIRCLE_PX / this.nh);
      this.dw       = this.nw * this.fitScale;
      this.dh       = this.nh * this.fitScale;
      // Reset transforms
      this._tx = 0; this._ty = 0; this._zoom = 1; this._fh = 1; this._fv = 1;
      this.imageReady.set(true);
      this.draw();
    };
    img.src = this.objectUrl;
  }

  ngAfterViewInit(): void {
    this.viewReady = true;
    this.draw(); // in case image loaded before view init
  }

  ngOnDestroy(): void {
    if (this.objectUrl) URL.revokeObjectURL(this.objectUrl);
    document.removeEventListener('pointermove', this.boundMove);
    document.removeEventListener('pointerup',   this.boundUp);
  }

  // ══════════════════════════════════════════════════════════════════════════
  // Canvas draw  — THE SINGLE SOURCE OF TRUTH
  //
  //   translate(VIEWPORT/2 + tx, VIEWPORT/2 + ty)
  //   scale(fh * zoom, fv * zoom)
  //   drawImage centred at (0, 0) with size (dw, dh)
  //
  // Because renderCrop() reads from this same canvas, what you see = what is saved.
  // ══════════════════════════════════════════════════════════════════════════
  private draw(): void {
    if (!this.viewReady || !this.img) return;
    const canvas = this.canvasRef?.nativeElement;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    ctx.clearRect(0, 0, VIEWPORT_PX, VIEWPORT_PX);
    ctx.save();
    ctx.translate(VIEWPORT_PX / 2 + this._tx, VIEWPORT_PX / 2 + this._ty);
    ctx.scale(this._fh * this._zoom, this._fv * this._zoom);
    ctx.drawImage(this.img, -this.dw / 2, -this.dh / 2, this.dw, this.dh);
    ctx.restore();
  }

  // ══════════════════════════════════════════════════════════════════════════
  // Controls
  // ══════════════════════════════════════════════════════════════════════════

  protected flipH(): void  { this._fh *= -1; this.draw(); }
  protected flipV(): void  { this._fv *= -1; this.draw(); }
  protected zoomIn(): void  { this._zoom = Math.min(this._zoom + 0.1, 4);   this.draw(); }
  protected zoomOut(): void { this._zoom = Math.max(this._zoom - 0.1, 0.5); this.draw(); }

  protected resetFit(): void {
    this._tx = 0; this._ty = 0; this._zoom = 1;
    this.draw();
  }

  // ══════════════════════════════════════════════════════════════════════════
  // Drag
  // ══════════════════════════════════════════════════════════════════════════

  protected onPointerDown(e: PointerEvent): void {
    e.preventDefault();
    this.dragging = true;
    this.lastX = e.clientX;
    this.lastY = e.clientY;
    document.addEventListener('pointermove', this.boundMove);
    document.addEventListener('pointerup',   this.boundUp);
  }

  private onPointerMove(e: PointerEvent): void {
    if (!this.dragging) return;
    this._tx += e.clientX - this.lastX;
    this._ty += e.clientY - this.lastY;
    this.lastX = e.clientX;
    this.lastY = e.clientY;
    this.draw();
  }

  private onPointerUp(): void {
    this.dragging = false;
    document.removeEventListener('pointermove', this.boundMove);
    document.removeEventListener('pointerup',   this.boundUp);
  }

  // ══════════════════════════════════════════════════════════════════════════
  // Crop — copies from the preview canvas (WYSIWYG)
  // ══════════════════════════════════════════════════════════════════════════

  protected cancel(): void { this.dialogRef.close(); }

  protected async apply(): Promise<void> {
    this.applying.set(true);
    try {
      const blob = await this.renderCrop();
      this.dialogRef.close({ blob });
    } finally {
      this.applying.set(false);
    }
  }

  /**
   * The preview canvas is exactly VIEWPORT_PX × VIEWPORT_PX and already contains
   * the transformed image. The crop circle occupies the square
   *   [cx, cy, cx + CROP_CIRCLE_PX, cy + CROP_CIRCLE_PX]
   * inside that canvas. We copy that region into the output canvas and clip to circle.
   */
  private renderCrop(): Promise<Blob> {
    return new Promise((resolve, reject) => {
      const preview = this.canvasRef?.nativeElement;
      if (!preview) { reject(new Error('No preview canvas')); return; }

      // Force a fresh draw so we always read the latest state.
      this.draw();

      const out = document.createElement('canvas');
      out.width  = OUTPUT_PX;
      out.height = OUTPUT_PX;
      const oc = out.getContext('2d');
      if (!oc) { reject(new Error('No output context')); return; }

      // Circular clip on output
      oc.beginPath();
      oc.arc(OUTPUT_PX / 2, OUTPUT_PX / 2, OUTPUT_PX / 2, 0, Math.PI * 2);
      oc.clip();

      // Source: the square that encloses the crop circle inside the preview canvas
      const src_x = (VIEWPORT_PX - CROP_CIRCLE_PX) / 2;
      const src_y = (VIEWPORT_PX - CROP_CIRCLE_PX) / 2;
      oc.drawImage(
        preview,
        src_x, src_y, CROP_CIRCLE_PX, CROP_CIRCLE_PX,  // source region
        0, 0, OUTPUT_PX, OUTPUT_PX,                      // full output
      );

      out.toBlob(
        blob => blob ? resolve(blob) : reject(new Error('toBlob failed')),
        'image/png',  // PNG format preserves transparency from transparent PNGs
      );
    });
  }
}


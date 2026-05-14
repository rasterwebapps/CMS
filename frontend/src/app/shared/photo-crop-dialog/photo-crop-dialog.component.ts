import {
  Component,
  ElementRef,
  ViewChild,
  signal,
  computed,
  OnInit,
  OnDestroy,
  inject,
  ChangeDetectionStrategy,
} from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

/** Data passed into the dialog. */
export interface PhotoCropDialogData {
  file: File;
}

/** Result returned by the dialog on confirm. */
export interface PhotoCropDialogResult {
  blob: Blob;
}

/** Size of the visible crop viewport in CSS pixels. */
const VIEWPORT_PX = 320;
/** Diameter of the circular crop area (centred in the viewport). */
const CROP_CIRCLE_PX = 280;
/** Output square canvas size (diameter of final image → circle). */
const OUTPUT_PX = 500;

@Component({
  selector: 'app-photo-crop-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule, MatIconModule, MatTooltipModule],
  templateUrl: './photo-crop-dialog.component.html',
  styleUrl: './photo-crop-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PhotoCropDialogComponent implements OnInit, OnDestroy {
  @ViewChild('imgEl') private imgEl!: ElementRef<HTMLImageElement>;

  private readonly dialogRef = inject<MatDialogRef<PhotoCropDialogComponent, PhotoCropDialogResult>>(MatDialogRef);
  private readonly data = inject<PhotoCropDialogData>(MAT_DIALOG_DATA);

  /** Object URL created from the incoming file — revoked on dialog close. */
  protected readonly imageSrc = signal('');

  // ── Transform state ────────────────────────────────────────────────────────
  /** Pan offset in viewport CSS pixels (relative to image centre). */
  protected readonly tx = signal(0);
  protected readonly ty = signal(0);
  /** Zoom multiplier (1 = fit-to-circle). */
  protected readonly zoom = signal(1);
  /** Flip direction multipliers. */
  protected readonly fh = signal(1);  // 1 = normal, -1 = flipped
  protected readonly fv = signal(1);

  /** CSS transform applied to <img>. Operates around the element's centre (= viewport centre). */
  protected readonly imgTransform = computed(() =>
    `translate(${this.tx()}px, ${this.ty()}px) scale(${this.zoom()}) scaleX(${this.fh()}) scaleY(${this.fv()})`,
  );

  /** Natural dimensions set once the image loads. */
  private nw = 1;
  private nh = 1;
  /** Scale applied to the image element to make it cover the crop circle at zoom=1. */
  private fitScale = 1;

  /**
   * CSS top/left of the <img> element so its centre sits at the viewport centre.
   * Recomputed whenever displayW/displayH change (i.e. after onImageLoad).
   * Together with transform-origin:center the element centre == viewport centre
   * regardless of image dimensions — which is the pivot point assumed by renderCrop.
   */
  protected readonly centerX = computed(() => (VIEWPORT_PX - this.displayW()) / 2);
  protected readonly centerY = computed(() => (VIEWPORT_PX - this.displayH()) / 2);

  /** Initial element display size so CSS width/height can be set. */
  protected readonly displayW = signal(0);
  protected readonly displayH = signal(0);

  // ── Drag state ─────────────────────────────────────────────────────────────
  private dragging = false;
  private lastX = 0;
  private lastY = 0;
  private readonly boundPointerMove = this.onPointerMove.bind(this);
  private readonly boundPointerUp = this.onPointerUp.bind(this);

  protected readonly applying = signal(false);

  /** Viewport and circle size exposed to the template. */
  protected readonly VIEWPORT = VIEWPORT_PX;
  protected readonly CROP_R = CROP_CIRCLE_PX / 2;

  ngOnInit(): void {
    const objectUrl = URL.createObjectURL(this.data.file);
    this.imageSrc.set(objectUrl);
  }

  ngOnDestroy(): void {
    const src = this.imageSrc();
    if (src) URL.revokeObjectURL(src);
    document.removeEventListener('pointermove', this.boundPointerMove);
    document.removeEventListener('pointerup', this.boundPointerUp);
  }

  /** Called once the <img> has loaded — compute natural dimensions and fit scale. */
  protected onImageLoad(): void {
    const img = this.imgEl.nativeElement;
    this.nw = img.naturalWidth;
    this.nh = img.naturalHeight;

    // Fit: the entire image should fit inside the crop circle at zoom=1.
    // Use "cover" semantics so neither dimension is smaller than the circle.
    this.fitScale = Math.max(CROP_CIRCLE_PX / this.nw, CROP_CIRCLE_PX / this.nh);
    this.displayW.set(Math.round(this.nw * this.fitScale));
    this.displayH.set(Math.round(this.nh * this.fitScale));
    // Centre the image
    this.tx.set(0);
    this.ty.set(0);
    this.zoom.set(1);
    this.fh.set(1);
    this.fv.set(1);
  }

  // ── Controls ───────────────────────────────────────────────────────────────
  protected flipH(): void { this.fh.update(v => v * -1); }
  protected flipV(): void { this.fv.update(v => v * -1); }

  /** Zoom in / out in 10% steps. */
  protected zoomIn():  void { this.zoom.update(v => Math.min(v + 0.1, 4)); }
  protected zoomOut(): void { this.zoom.update(v => Math.max(v - 0.1, 0.5)); }

  /** Reset pan and zoom to the initial cover-fit state. */
  protected resetFit(): void {
    this.tx.set(0);
    this.ty.set(0);
    this.zoom.set(1);
  }

  // ── Drag ───────────────────────────────────────────────────────────────────
  protected onPointerDown(e: PointerEvent): void {
    e.preventDefault();
    this.dragging = true;
    this.lastX = e.clientX;
    this.lastY = e.clientY;
    document.addEventListener('pointermove', this.boundPointerMove);
    document.addEventListener('pointerup', this.boundPointerUp);
  }

  private onPointerMove(e: PointerEvent): void {
    if (!this.dragging) return;
    this.tx.update(v => v + (e.clientX - this.lastX));
    this.ty.update(v => v + (e.clientY - this.lastY));
    this.lastX = e.clientX;
    this.lastY = e.clientY;
  }

  private onPointerUp(): void {
    this.dragging = false;
    document.removeEventListener('pointermove', this.boundPointerMove);
    document.removeEventListener('pointerup', this.boundPointerUp);
  }

  // ── Apply / cancel ─────────────────────────────────────────────────────────
  protected cancel(): void {
    this.dialogRef.close();
  }

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
   * Two-pass crop that is guaranteed to match the visual preview exactly.
   *
   * Pass 1 — replicate the CSS display:
   *   Draw the image onto a VIEWPORT_PX × VIEWPORT_PX scratch canvas exactly
   *   as the browser shows it: at displayW × displayH, centred at
   *   (VIEWPORT/2, VIEWPORT/2), then apply translate(tx,ty) + zoom + flip.
   *   This eliminates any natural-vs-display-size ambiguity in drawImage.
   *
   * Pass 2 — extract the crop circle:
   *   We know the crop circle centre = (VIEWPORT/2, VIEWPORT/2), radius = CROP_R.
   *   Copy that exact square region from the scratch canvas into the OUTPUT_PX
   *   output, clipped to a circle.
   */
  private renderCrop(): Promise<Blob> {
    return new Promise((resolve, reject) => {
      const img = this.imgEl.nativeElement;

      // ── Pass 1: paint the transformed image at display size ───────────────
      const scratch = document.createElement('canvas');
      scratch.width  = VIEWPORT_PX;
      scratch.height = VIEWPORT_PX;
      const sc = scratch.getContext('2d');
      if (!sc) { reject(new Error('No scratch 2D context')); return; }

      const dw = this.displayW();
      const dh = this.displayH();

      sc.save();
      // Move to crop-circle centre, apply user transforms, draw image centred
      sc.translate(VIEWPORT_PX / 2 + this.tx(), VIEWPORT_PX / 2 + this.ty());
      sc.scale(this.fh() * this.zoom(), this.fv() * this.zoom());
      sc.drawImage(img, -dw / 2, -dh / 2, dw, dh);
      sc.restore();

      // ── Pass 2: extract crop circle → output canvas ───────────────────────
      const out = document.createElement('canvas');
      out.width  = OUTPUT_PX;
      out.height = OUTPUT_PX;
      const oc = out.getContext('2d');
      if (!oc) { reject(new Error('No output 2D context')); return; }

      // Circular clip
      oc.beginPath();
      oc.arc(OUTPUT_PX / 2, OUTPUT_PX / 2, OUTPUT_PX / 2, 0, Math.PI * 2);
      oc.clip();

      // Source rect in scratch canvas: the square enclosing the crop circle
      const cx = VIEWPORT_PX / 2 - CROP_CIRCLE_PX / 2;
      const cy = VIEWPORT_PX / 2 - CROP_CIRCLE_PX / 2;
      oc.drawImage(scratch, cx, cy, CROP_CIRCLE_PX, CROP_CIRCLE_PX, 0, 0, OUTPUT_PX, OUTPUT_PX);

      out.toBlob(
        (blob) => {
          if (blob) resolve(blob);
          else reject(new Error('Failed to create blob'));
        },
        'image/jpeg',
        0.92,
      );
    });
  }
}


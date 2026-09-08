import { Component, Input, OnChanges, OnDestroy, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ProductImageService } from './product-image.service';
import { ProductImage } from './product-image.model';
import { ConfirmDialogComponent } from '../../../../shared/confirm-dialog/confirm-dialog.component';
import { ToastService } from '../../../../core/toast/toast.service';
import { PermissionService } from '../../../../core/permissions/permission.service';

interface GalleryImage extends ProductImage {
  objectUrl: string | null;
}

/**
 * A self-contained photo gallery/upload widget for one Product — shown only in the Product
 * form's edit mode (a product must already exist before it can have photos). Follows the
 * `FloorPlanCalibrationFlyoutComponent` precedent for rendering an authenticated download as an
 * `<img>` (fetch as a blob, bind an object URL — a plain `<img src>` can't carry the auth
 * header). See the "Product Image slice" decision-log entry.
 */
@Component({
  selector: 'app-product-images',
  standalone: true,
  imports: [MatButtonModule, MatProgressSpinnerModule, MatDialogModule],
  templateUrl: './product-images.component.html',
  styleUrl: './product-images.component.scss',
})
export class ProductImagesComponent implements OnChanges, OnDestroy {
  @Input({ required: true }) productId!: number;

  private readonly imageService      = inject(ProductImageService);
  private readonly dialog            = inject(MatDialog);
  private readonly toast             = inject(ToastService);
  private readonly permissionService = inject(PermissionService);

  protected readonly loading = signal(false);
  protected readonly uploading = signal(false);
  protected readonly images = signal<GalleryImage[]>([]);

  protected readonly canManage = computed(() => this.permissionService.has('INVENTORY_PRODUCT_IMAGE_MANAGE'));

  ngOnChanges(): void {
    if (this.productId) this.load();
  }

  ngOnDestroy(): void {
    this.revokeAll();
  }

  protected onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;

    this.uploading.set(true);
    this.imageService.upload(this.productId, file).subscribe({
      next: () => { this.toast.success('Photo uploaded'); this.uploading.set(false); this.load(); },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to upload photo'); this.uploading.set(false); },
    });
  }

  protected setPrimary(image: GalleryImage): void {
    this.imageService.setPrimary(this.productId, image.id).subscribe({
      next: () => { this.toast.success('Primary photo updated'); this.load(); },
      error: (err) => this.toast.error(err?.error?.message ?? 'Failed to set primary photo'),
    });
  }

  protected deleteImage(image: GalleryImage): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Delete Photo', message: 'This permanently deletes the photo. Continue?', confirmText: 'Delete', cancelText: 'Cancel' },
    }).afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.imageService.delete(this.productId, image.id).subscribe({
        next: () => { this.toast.success('Photo deleted'); this.load(); },
        error: (err) => this.toast.error(err?.error?.message ?? 'Failed to delete photo'),
      });
    });
  }

  private load(): void {
    this.loading.set(true);
    this.imageService.findByProduct(this.productId).subscribe({
      next: (images) => {
        this.revokeAll();
        this.images.set(images.map((i) => ({ ...i, objectUrl: null })));
        this.loading.set(false);
        for (const image of images) this.loadThumbnail(image.id);
      },
      error: () => { this.toast.error('Failed to load product photos'); this.loading.set(false); },
    });
  }

  private loadThumbnail(imageId: number): void {
    this.imageService.download(this.productId, imageId).subscribe({
      next: (response) => {
        if (!response.body) return;
        const url = URL.createObjectURL(response.body);
        this.images.update((list) => list.map((i) => (i.id === imageId ? { ...i, objectUrl: url } : i)));
      },
      error: () => { /* thumbnail best-effort — a broken one just shows the placeholder */ },
    });
  }

  private revokeAll(): void {
    for (const image of this.images()) {
      if (image.objectUrl) URL.revokeObjectURL(image.objectUrl);
    }
  }
}

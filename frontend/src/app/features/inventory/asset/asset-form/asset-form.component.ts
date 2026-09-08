import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AssetService } from '../asset.service';
import { AssetRequest } from '../asset.model';
import { InventoryLocationService } from '../../location/inventory-location.service';
import { InventoryLocation } from '../../location/inventory-location.model';
import { CmsProductPickerComponent } from '../../../../shared/product-picker/product-picker.component';
import { ToastService } from '../../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../../shared/utils/scroll-to-invalid';
import { noConsecutiveSpaces, trimmedMinLength, cmsFieldError } from '../../../../shared/validators/cms-validators';
import { environment } from '../../../../../environments';
import { uniqueFieldValidator } from '../../../../shared/validators/unique-field.validator';

@Component({
  selector: 'app-asset-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    CmsProductPickerComponent,
  ],
  templateUrl: './asset-form.component.html',
  styleUrl: './asset-form.component.scss',
})
export class AssetFormComponent implements OnInit {
  private readonly fb              = inject(FormBuilder);
  private readonly route           = inject(ActivatedRoute);
  private readonly router          = inject(Router);
  private readonly assetService    = inject(AssetService);
  private readonly locationService = inject(InventoryLocationService);
  private readonly toast           = inject(ToastService);
  private readonly destroyRef      = inject(DestroyRef);
  private readonly http            = inject(HttpClient);

  protected readonly loading    = signal(false);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle  = signal('Register Asset');
  protected readonly locations  = signal<InventoryLocation[]>([]);

  private assetId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    productId:         [null as number | null, [Validators.required]],
    locationId:        [null as number | null, [Validators.required]],
    assetTag:          ['', [Validators.required, trimmedMinLength(2), Validators.maxLength(50), noConsecutiveSpaces()]],
    serialNumber:      ['', [Validators.maxLength(100)]],
    purchaseValue:     [null as number | null, [Validators.min(0)]],
    purchaseDate:      [''],
    usefulLifeMonths:  [null as number | null, [Validators.min(1)]],
    salvageValue:      [null as number | null, [Validators.min(0)]],
    notes:             ['', [Validators.maxLength(500)]],
  });

  ngOnInit(): void {
    this.locationService.getAll(true).subscribe({ next: (l) => this.locations.set(l) });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.assetId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Asset');
      this.loadAsset();
    }

    const tagCtrl = this.form.get('assetTag');
    tagCtrl?.setAsyncValidators(
      uniqueFieldValidator(this.http, `${environment.apiUrl}/inventory/asset/assets/asset-tag-exists`, () => this.assetId),
    );
    tagCtrl?.updateValueAndValidity({ emitEvent: false });
  }

  protected getErrorMessage(fieldName: string): string {
    const labels: Record<string, string> = { productId: 'Product', locationId: 'Location', assetTag: 'Asset tag' };
    if (fieldName === 'assetTag' && this.form.get('assetTag')?.hasError('duplicate')) {
      return 'This asset tag is already in use';
    }
    return cmsFieldError(this.form.get(fieldName), labels[fieldName] ?? fieldName);
  }

  private loadAsset(): void {
    this.loading.set(true);
    this.assetService.getById(this.assetId!)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (a) => {
          this.form.patchValue({
            productId: a.productId,
            locationId: a.locationId,
            assetTag: a.assetTag,
            serialNumber: a.serialNumber ?? '',
            purchaseValue: a.purchaseValue,
            purchaseDate: a.purchaseDate ?? '',
            usefulLifeMonths: a.usefulLifeMonths,
            salvageValue: a.salvageValue,
            notes: a.notes ?? '',
          });
          this.loading.set(false);
        },
        error: () => { this.toast.error('Failed to load asset'); this.loading.set(false); },
      });
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }

    const v = this.form.value;
    const request: AssetRequest = {
      productId: v.productId,
      locationId: v.locationId,
      assetTag: v.assetTag.trim(),
      serialNumber: v.serialNumber?.trim() || undefined,
      purchaseValue: v.purchaseValue ?? undefined,
      purchaseDate: v.purchaseDate || undefined,
      usefulLifeMonths: v.usefulLifeMonths ?? undefined,
      salvageValue: v.salvageValue ?? undefined,
      notes: v.notes?.trim() || undefined,
    };

    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.assetService.update(this.assetId!, request)
      : this.assetService.create(request);

    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Asset updated successfully' : 'Asset registered successfully');
        this.saving.set(false);
        void this.router.navigate(['/inventory/asset/assets']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? (this.isEditMode() ? 'Failed to update asset' : 'Failed to register asset'));
        this.saving.set(false);
      },
    });
  }
}

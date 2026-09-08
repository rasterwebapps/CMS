import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { GatePassService } from '../gate-pass.service';
import { GatePassCreateRequest } from '../gate-pass.model';
import { InventoryLocationService } from '../../location/inventory-location.service';
import { InventoryLocation } from '../../location/inventory-location.model';
import { AssetService } from '../../asset/asset.service';
import { Asset } from '../../asset/asset.model';
import { CmsProductPickerComponent } from '../../../../shared/product-picker/product-picker.component';
import { ToastService } from '../../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../../shared/utils/scroll-to-invalid';
import { cmsFieldError } from '../../../../shared/validators/cms-validators';

@Component({
  selector: 'app-gate-pass-new',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    CmsProductPickerComponent,
  ],
  templateUrl: './gate-pass-new.component.html',
  styleUrl: './gate-pass-new.component.scss',
})
export class GatePassNewComponent implements OnInit {
  private readonly fb              = inject(FormBuilder);
  private readonly router          = inject(Router);
  private readonly gatePassService = inject(GatePassService);
  private readonly locationService = inject(InventoryLocationService);
  private readonly assetService    = inject(AssetService);
  private readonly toast           = inject(ToastService);

  protected readonly saving    = signal(false);
  protected readonly locations = signal<InventoryLocation[]>([]);
  protected readonly availableAssets = signal<Asset[]>([]);

  protected readonly form: FormGroup = this.fb.group({
    targetType:            ['PRODUCT', [Validators.required]],
    productId:             [null as number | null],
    assetId:                [null as number | null],
    direction:             ['OUTWARD', [Validators.required]],
    returnable:            [true],
    locationId:            [null as number | null, [Validators.required]],
    quantity:              [1, [Validators.required, Validators.min(0.001)]],
    reason:                ['', [Validators.required, Validators.maxLength(500)]],
    partyName:             ['', [Validators.required, Validators.maxLength(200)]],
    partyContact:          ['', [Validators.maxLength(100)]],
    linkedPurchaseOrderId: [null as number | null],
    passDate:              [new Date().toISOString().slice(0, 10), [Validators.required]],
    expectedReturnDate:    ['', [Validators.required]],
    notes:                 ['', [Validators.maxLength(500)]],
  });

  ngOnInit(): void {
    this.locationService.getAll(true).subscribe({ next: (l) => this.locations.set(l) });
    this.assetService.getPage({ status: 'AVAILABLE', size: 500 }).subscribe({ next: (page) => this.availableAssets.set(page.content) });

    this.form.get('targetType')?.valueChanges.subscribe((type: string) => {
      if (type === 'PRODUCT') {
        this.form.get('assetId')?.setValue(null);
      } else {
        this.form.get('productId')?.setValue(null);
      }
    });
    this.form.get('returnable')?.valueChanges.subscribe((returnable: boolean) => {
      const control = this.form.get('expectedReturnDate');
      if (returnable) {
        control?.setValidators([Validators.required]);
      } else {
        control?.clearValidators();
        control?.setValue('');
      }
      control?.updateValueAndValidity();
    });
  }

  protected getErrorMessage(fieldName: string): string {
    const labels: Record<string, string> = {
      productId: 'Product', assetId: 'Asset', locationId: 'Location', quantity: 'Quantity',
      reason: 'Reason', partyName: 'Party name', expectedReturnDate: 'Expected return date',
    };
    return cmsFieldError(this.form.get(fieldName), labels[fieldName] ?? fieldName);
  }

  protected onSubmit(): void {
    const targetControl = this.form.get(this.form.value.targetType === 'PRODUCT' ? 'productId' : 'assetId');
    targetControl?.markAsTouched();
    if (this.form.invalid || !targetControl?.value) {
      scrollToFirstInvalid(this.form);
      return;
    }

    const v = this.form.value;
    const request: GatePassCreateRequest = {
      direction: v.direction,
      returnable: !!v.returnable,
      productId: v.targetType === 'PRODUCT' ? v.productId : null,
      assetId: v.targetType === 'ASSET' ? v.assetId : null,
      locationId: v.locationId,
      quantity: v.quantity,
      reason: v.reason.trim(),
      partyName: v.partyName.trim(),
      partyContact: v.partyContact?.trim() || undefined,
      linkedPurchaseOrderId: v.linkedPurchaseOrderId || undefined,
      passDate: v.passDate,
      expectedReturnDate: v.returnable ? v.expectedReturnDate : undefined,
      notes: v.notes?.trim() || undefined,
    };

    this.saving.set(true);
    this.gatePassService.create(request).subscribe({
      next: (pass) => {
        this.toast.success('Gate pass created');
        this.saving.set(false);
        void this.router.navigate(['/inventory/gate-pass/gate-passes', pass.id]);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to create gate pass');
        this.saving.set(false);
      },
    });
  }
}

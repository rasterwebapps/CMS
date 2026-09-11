import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ProductVariantService } from '../product-variant.service';
import { ProductVariantRequest } from '../product-variant.model';
import { ProductService } from '../../product.service';
import { Product, ProductAttributeValueRequest } from '../../product.model';
import { CategoryAttributeService } from '../../../category/category-attribute.service';
import { CategoryAttribute } from '../../../category/category-attribute.model';
import { ToastService } from '../../../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../../../shared/utils/scroll-to-invalid';
import { cmsFieldError } from '../../../../../shared/validators/cms-validators';
import { environment } from '../../../../../../environments';
import { uniqueFieldValidator } from '../../../../../shared/validators/unique-field.validator';

/**
 * "Inheriting typed attributes, tracking mode, pricing ... from the phases above" is
 * copy-at-creation, not a live link: this form pre-fills a new variant's fields from its parent
 * Product's current values, then they're independently editable and never read from the parent
 * again — see the 2026-09-11 "ProductVariant" decision-log entry.
 */
@Component({
  selector: 'app-product-variant-form',
  standalone: true,
  imports: [RouterLink, ReactiveFormsModule, MatButtonModule, MatProgressSpinnerModule],
  templateUrl: './product-variant-form.component.html',
  styleUrl: './product-variant-form.component.scss',
})
export class ProductVariantFormComponent implements OnInit {
  private readonly fb              = inject(FormBuilder);
  private readonly route           = inject(ActivatedRoute);
  private readonly router          = inject(Router);
  private readonly variantService  = inject(ProductVariantService);
  private readonly productService  = inject(ProductService);
  private readonly attributeService = inject(CategoryAttributeService);
  private readonly toast           = inject(ToastService);
  private readonly http            = inject(HttpClient);

  protected readonly loading    = signal(false);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle  = signal('Add Variant');
  protected readonly parentProduct = signal<Product | null>(null);
  protected readonly categoryAttributes = signal<CategoryAttribute[]>([]);

  protected productId!: number;
  private variantId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    variantCode:  ['', [Validators.required, Validators.maxLength(50)]],
    variantName:  ['', [Validators.required, Validators.maxLength(200)]],
    barcode:      ['', [Validators.maxLength(64)]],
    trackingMode: ['NONE'],
    standardCost: [null as number | null, [Validators.min(0)]],
    listPrice:    [null as number | null, [Validators.min(0)]],
    isActive:     [true],
    attributeValues: this.fb.array([] as FormGroup[]),
  });

  get attributeValueRows(): FormArray {
    return this.form.get('attributeValues') as FormArray;
  }

  ngOnInit(): void {
    this.productId = Number(this.route.snapshot.paramMap.get('productId'));
    const idParam = this.route.snapshot.paramMap.get('id');

    this.productService.getById(this.productId).subscribe({
      next: (product) => {
        this.parentProduct.set(product);
        this.attributeService.findByCategory(product.categoryId).subscribe({
          next: (attrs) => {
            this.categoryAttributes.set(attrs);
            if (idParam) {
              this.variantId = Number(idParam);
              this.isEditMode.set(true);
              this.pageTitle.set('Edit Variant');
              this.loadVariant();
            } else {
              this.prefillFromParent(product);
            }
          },
          error: () => this.toast.error('Failed to load category attributes'),
        });
      },
      error: () => {
        this.toast.error('Failed to load product');
        void this.router.navigate(['/inventory/products']);
      },
    });

    this.setupUniquenessValidators();
  }

  private setupUniquenessValidators(): void {
    const codeCtrl = this.form.get('variantCode');
    codeCtrl?.setAsyncValidators(
      uniqueFieldValidator(this.http, `${environment.apiUrl}/inventory/products/${this.productId}/variants/code-exists`, () => this.variantId),
    );
    codeCtrl?.updateValueAndValidity({ emitEvent: false });

    const barcodeCtrl = this.form.get('barcode');
    barcodeCtrl?.setAsyncValidators(
      uniqueFieldValidator(this.http, `${environment.apiUrl}/inventory/products/${this.productId}/variants/barcode-exists`, () => this.variantId),
    );
    barcodeCtrl?.updateValueAndValidity({ emitEvent: false });
  }

  private prefillFromParent(product: Product): void {
    this.form.patchValue({
      trackingMode: product.trackingMode,
      standardCost: product.standardCost ?? null,
      listPrice: product.listPrice ?? null,
    });
    this.attributeValueRows.clear();
    for (const attr of this.categoryAttributes()) {
      const known = product.attributeValues.find(v => v.attributeId === attr.id)?.value ?? '';
      this.attributeValueRows.push(this.fb.group({ attributeId: [attr.id], value: [known] }));
    }
    this.loading.set(false);
  }

  protected attributeOptions(attr: CategoryAttribute): string[] {
    return (attr.enumOptions ?? '').split(',').map(s => s.trim()).filter(Boolean);
  }

  protected getErrorMessage(fieldName: string): string {
    const labels: Record<string, string> = { variantCode: 'Variant code', variantName: 'Variant name', barcode: 'Barcode' };
    return cmsFieldError(this.form.get(fieldName), labels[fieldName] ?? fieldName);
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }

    const v = this.form.value;
    const attributeValues: ProductAttributeValueRequest[] = (v.attributeValues ?? [])
      .filter((r: { value: string }) => (r.value ?? '').trim().length > 0)
      .map((r: { attributeId: number; value: string }) => ({ attributeId: r.attributeId, value: r.value.trim() }));

    const request: ProductVariantRequest = {
      variantCode: (v.variantCode ?? '').trim().toUpperCase(),
      variantName: (v.variantName ?? '').trim(),
      barcode: v.barcode?.trim() || undefined,
      trackingMode: v.trackingMode,
      standardCost: v.standardCost,
      listPrice: v.listPrice,
      isActive: v.isActive,
      attributeValues,
    };

    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.variantService.update(this.productId, this.variantId!, request)
      : this.variantService.create(this.productId, request);

    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Variant updated successfully' : 'Variant created successfully');
        this.saving.set(false);
        void this.router.navigate(['/inventory/products', this.productId, 'edit']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? (this.isEditMode() ? 'Failed to update variant' : 'Failed to create variant'));
        this.saving.set(false);
      },
    });
  }

  private loadVariant(): void {
    if (!this.variantId) return;
    this.loading.set(true);
    this.variantService.getById(this.productId, this.variantId).subscribe({
      next: (variant) => {
        this.form.patchValue({
          variantCode: variant.variantCode,
          variantName: variant.variantName,
          barcode: variant.barcode || '',
          trackingMode: variant.trackingMode,
          standardCost: variant.standardCost ?? null,
          listPrice: variant.listPrice ?? null,
          isActive: variant.isActive,
        });
        this.attributeValueRows.clear();
        for (const attr of this.categoryAttributes()) {
          const known = variant.attributeValues.find(v => v.attributeId === attr.id)?.value ?? '';
          this.attributeValueRows.push(this.fb.group({ attributeId: [attr.id], value: [known] }));
        }
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load variant');
        void this.router.navigate(['/inventory/products', this.productId, 'edit']);
      },
    });
  }
}

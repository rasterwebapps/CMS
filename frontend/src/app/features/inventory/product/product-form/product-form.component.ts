import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormArray, FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CmsPreviewCardComponent } from '../../../../shared/preview-card/preview-card.component';
import { ProductImagesComponent } from '../product-images/product-images.component';
import { ProductService } from '../product.service';
import { Product, ProductAttributeValueRequest, ProductRequest } from '../product.model';
import { CategoryService } from '../../category/category.service';
import { Category } from '../../category/category.model';
import { CategoryAttributeService } from '../../category/category-attribute.service';
import { CategoryAttribute } from '../../category/category-attribute.model';
import { UomService } from '../../uom/uom.service';
import { Uom } from '../../uom/uom.model';
import { ToastService } from '../../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../../shared/utils/scroll-to-invalid';
import { noConsecutiveSpaces, noInternalSpaces, trimmedMinLength, cmsFieldError, stripSpaces } from '../../../../shared/validators/cms-validators';
import { environment } from '../../../../../environments';
import { uniqueFieldValidator } from '../../../../shared/validators/unique-field.validator';

@Component({
  selector: 'app-product-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    CmsPreviewCardComponent,
    ProductImagesComponent,
  ],
  templateUrl: './product-form.component.html',
  styleUrl: './product-form.component.scss',
})
export class ProductFormComponent implements OnInit {
  private readonly fb               = inject(FormBuilder);
  private readonly route            = inject(ActivatedRoute);
  private readonly router           = inject(Router);
  private readonly productService   = inject(ProductService);
  private readonly categoryService  = inject(CategoryService);
  private readonly attributeService = inject(CategoryAttributeService);
  private readonly uomService       = inject(UomService);
  private readonly toast            = inject(ToastService);
  private readonly destroyRef       = inject(DestroyRef);
  private readonly http             = inject(HttpClient);

  protected readonly loading    = signal(false);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle  = signal('Add Product');
  protected readonly categories = signal<Category[]>([]);
  protected readonly uoms       = signal<Uom[]>([]);
  protected readonly categoryAttributes = signal<CategoryAttribute[]>([]);
  protected readonly attributesLoading  = signal(false);

  protected readonly previewCode = signal('');
  protected readonly previewName = signal('');
  protected readonly previewCategoryName = signal('');

  private productId: number | null = null;
  // Exposed only so the template can pass it to <app-product-images> in edit mode — a product
  // must already exist before it can have photos, so this stays null in create mode.
  protected readonly savedProductId = signal<number | null>(null);
  // Attribute values keyed by attributeId, carried across category switches so re-selecting the
  // original category (or loading an existing product) doesn't lose already-entered values.
  private knownAttributeValues = new Map<number, string>();

  protected readonly form: FormGroup = this.fb.group({
    productCode:  ['', [Validators.required, Validators.maxLength(50), noInternalSpaces()]],
    productName:  ['', [Validators.required, trimmedMinLength(2), Validators.maxLength(200), noConsecutiveSpaces()]],
    categoryId:   [null as number | null, [Validators.required]],
    baseUomId:    [null as number | null, [Validators.required]],
    reorderLevel: [null as number | null],
    reorderQty:   [null as number | null],
    isAsset:      [false],
    isConsumable: [true],
    isService:    [false],
    isLoanable:   [false],
    depreciationRate:      [null as number | null],
    warrantyPeriodMonths:  [null as number | null],
    description:  ['', [Validators.maxLength(1000)]],
    aliases:          this.fb.array([] as FormControl<string>[]),
    attributeValues:  this.fb.array([] as FormGroup[]),
  });

  get aliases(): FormArray {
    return this.form.get('aliases') as FormArray;
  }

  get attributeValueRows(): FormArray {
    return this.form.get('attributeValues') as FormArray;
  }

  constructor() {
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(v => {
        this.previewCode.set((v.productCode ?? '').toUpperCase().trim());
        this.previewName.set((v.productName ?? '').trim());
        const category = this.categories().find(c => c.id === v.categoryId);
        this.previewCategoryName.set(category?.name ?? '');
      });
  }

  ngOnInit(): void {
    this.categoryService.getAll(true).subscribe({ next: (c) => this.categories.set(c) });
    this.uomService.getAll(true).subscribe({ next: (u) => this.uoms.set(u) });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.productId = Number(idParam);
      this.savedProductId.set(this.productId);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Product');
      this.loadProduct();
    }
    this.setupUniquenessValidators();

    this.form.get('categoryId')?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((categoryId) => {
      this.rebuildAttributeRows(categoryId);
    });
  }

  private setupUniquenessValidators(): void {
    const codeCtrl = this.form.get('productCode');
    codeCtrl?.setAsyncValidators(
      uniqueFieldValidator(this.http, `${environment.apiUrl}/inventory/products/code-exists`, () => this.productId),
    );
    codeCtrl?.updateValueAndValidity({ emitEvent: false });

    const nameCtrl = this.form.get('productName');
    nameCtrl?.setAsyncValidators(
      uniqueFieldValidator(
        this.http,
        `${environment.apiUrl}/inventory/products/name-exists`,
        () => this.productId,
        (): Record<string, string | number> => {
          const categoryId = this.form.value.categoryId;
          const params: Record<string, string | number> = {};
          if (categoryId != null) params['categoryId'] = categoryId;
          return params;
        },
      ),
    );
    nameCtrl?.updateValueAndValidity({ emitEvent: false });

    this.form.get('categoryId')?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      nameCtrl?.updateValueAndValidity({ emitEvent: false });
    });
  }

  protected onCodeInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const start = input.selectionStart ?? 0;
    const end   = input.selectionEnd ?? 0;
    const cleaned = stripSpaces(input.value).toUpperCase();
    if (cleaned !== input.value) {
      this.form.get('productCode')?.setValue(cleaned, { emitEvent: true });
      setTimeout(() => input.setSelectionRange(start, end), 0);
    }
  }

  // ── Aliases ──────────────────────────────────────────────────────────────
  protected addAlias(): void {
    this.aliases.push(this.fb.control('', [Validators.required, Validators.maxLength(200)]));
  }

  protected removeAlias(index: number): void {
    this.aliases.removeAt(index);
  }

  // ── Attribute values (rebuilt whenever the category changes) ──────────────
  private rebuildAttributeRows(categoryId: number | null): void {
    // Snapshot whatever is currently on-screen before tearing the rows down, so re-selecting the
    // same category later restores what was there.
    this.captureCurrentAttributeValues();

    this.attributeValueRows.clear();
    this.categoryAttributes.set([]);
    if (categoryId == null) return;

    this.attributesLoading.set(true);
    this.attributeService.findByCategory(categoryId).subscribe({
      next: (attrs) => {
        this.categoryAttributes.set(attrs);
        for (const attr of attrs) {
          const known = this.knownAttributeValues.get(attr.id) ?? '';
          const validators = attr.isRequired ? [Validators.required] : [];
          this.attributeValueRows.push(this.fb.group({
            attributeId: [attr.id],
            value: [known, validators],
          }));
        }
        this.attributesLoading.set(false);
      },
      error: () => { this.toast.error('Failed to load category attributes'); this.attributesLoading.set(false); },
    });
  }

  private captureCurrentAttributeValues(): void {
    for (const group of this.attributeValueRows.controls) {
      const attributeId = group.get('attributeId')?.value;
      const value = group.get('value')?.value;
      if (attributeId != null && value) this.knownAttributeValues.set(attributeId, value);
    }
  }

  protected attributeOptions(attr: CategoryAttribute): string[] {
    return (attr.enumOptions ?? '').split(',').map(s => s.trim()).filter(Boolean);
  }

  protected onSubmit(): void {
    this.captureCurrentAttributeValues();
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }

    const v = this.form.value;
    const aliasValues: string[] = (v.aliases ?? []).map((a: string) => (a ?? '').trim()).filter((a: string) => a.length > 0);
    const attributeValues: ProductAttributeValueRequest[] = (v.attributeValues ?? [])
      .filter((r: { value: string }) => (r.value ?? '').trim().length > 0)
      .map((r: { attributeId: number; value: string }) => ({ attributeId: r.attributeId, value: r.value.trim() }));

    const request: ProductRequest = {
      productCode:  (v.productCode ?? '').trim().toUpperCase(),
      productName:  (v.productName ?? '').trim(),
      categoryId:   v.categoryId,
      baseUomId:    v.baseUomId,
      reorderLevel: v.reorderLevel,
      reorderQty:   v.reorderQty,
      isAsset:      !!v.isAsset,
      isConsumable: !!v.isConsumable,
      isService:    !!v.isService,
      isLoanable:   !!v.isLoanable,
      depreciationRate:     v.depreciationRate,
      warrantyPeriodMonths: v.warrantyPeriodMonths,
      description:  v.description?.trim() || undefined,
      aliases: aliasValues,
      attributeValues,
    };

    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.productService.update(this.productId!, request)
      : this.productService.create(request);

    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Product updated successfully' : 'Product created successfully');
        this.saving.set(false);
        void this.router.navigate(['/inventory/products']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? (this.isEditMode() ? 'Failed to update product' : 'Failed to create product'));
        this.saving.set(false);
      },
    });
  }

  private static readonly FIELD_LABELS: Record<string, string> = {
    productCode: 'Product code', productName: 'Product name', categoryId: 'Category', baseUomId: 'Base unit of measure',
    description: 'Description',
  };

  protected getErrorMessage(fieldName: string): string {
    return cmsFieldError(this.form.get(fieldName), ProductFormComponent.FIELD_LABELS[fieldName] ?? fieldName);
  }

  private loadProduct(): void {
    if (!this.productId) return;
    this.loading.set(true);
    this.productService.getById(this.productId).subscribe({
      next: (p: Product) => {
        for (const av of p.attributeValues) {
          if (av.value) this.knownAttributeValues.set(av.attributeId, av.value);
        }
        this.form.patchValue({
          productCode: p.productCode,
          productName: p.productName,
          categoryId: p.categoryId,
          baseUomId: p.baseUomId,
          reorderLevel: p.reorderLevel ?? null,
          reorderQty: p.reorderQty ?? null,
          isAsset: p.isAsset,
          isConsumable: p.isConsumable,
          isService: p.isService,
          isLoanable: p.isLoanable,
          depreciationRate: p.depreciationRate ?? null,
          warrantyPeriodMonths: p.warrantyPeriodMonths ?? null,
          description: p.description || '',
        });
        this.aliases.clear();
        for (const alias of p.aliases) {
          this.aliases.push(this.fb.control(alias, [Validators.required, Validators.maxLength(200)]));
        }
        // categoryId patch above already triggered rebuildAttributeRows via valueChanges, which
        // will now find the values captured into knownAttributeValues just above.
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load product');
        void this.router.navigate(['/inventory/products']);
      },
    });
  }
}

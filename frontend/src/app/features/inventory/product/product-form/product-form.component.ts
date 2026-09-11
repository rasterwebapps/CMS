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
import {
  Product,
  ProductAttributeValueRequest,
  ProductRequest,
  ProductUomChainVersion,
  ProductUomLevelRequest,
  StockTrackingMode,
} from '../product.model';
import { CategoryService } from '../../category/category.service';
import { Category } from '../../category/category.model';
import { CategoryAttributeService } from '../../category/category-attribute.service';
import { CategoryAttribute } from '../../category/category-attribute.model';
import { UomService } from '../../uom/uom.service';
import { Uom } from '../../uom/uom.model';
import { BrandService } from '../../brand/brand.service';
import { Brand } from '../../brand/brand.model';
import { TaxRuleService } from '../../procurement/tax-rule/tax-rule.service';
import { TaxRule } from '../../procurement/tax-rule/tax-rule.model';
import { UomConversionTemplateService } from '../../uom-conversion-template/uom-conversion-template.service';
import { UomConversionTemplate } from '../../uom-conversion-template/uom-conversion-template.model';
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
  private readonly brandService     = inject(BrandService);
  private readonly taxRuleService   = inject(TaxRuleService);
  private readonly uomTemplateService = inject(UomConversionTemplateService);
  private readonly toast            = inject(ToastService);
  private readonly destroyRef       = inject(DestroyRef);
  private readonly http             = inject(HttpClient);

  protected readonly loading    = signal(false);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle  = signal('Add Product');
  protected readonly categories = signal<Category[]>([]);
  protected readonly uoms       = signal<Uom[]>([]);
  protected readonly brands     = signal<Brand[]>([]);
  protected readonly taxRules   = signal<TaxRule[]>([]);
  // Templates whose own base unit matches the product's currently-selected base unit — the only
  // ones the "apply a template" picker in Unit Hierarchy can legitimately offer.
  protected readonly applicableTemplates = signal<UomConversionTemplate[]>([]);
  protected readonly categoryAttributes = signal<CategoryAttribute[]>([]);
  protected readonly attributesLoading  = signal(false);

  // ── Unit Hierarchy (only meaningful once the product has an id — see savedProductId) ─────
  protected readonly activeChain    = signal<ProductUomChainVersion | null>(null);
  protected readonly chainVersions  = signal<ProductUomChainVersion[]>([]);
  protected readonly chainLoading   = signal(false);
  protected readonly chainSaving    = signal(false);
  protected readonly showVersionHistory = signal(false);
  protected readonly uomLevels: FormArray = this.fb.array([] as FormGroup[]);

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
    barcode:      ['', [Validators.maxLength(64)]],
    categoryId:   [null as number | null, [Validators.required]],
    baseUomId:    [null as number | null, [Validators.required]],
    brandId:      [null as number | null],
    reorderLevel: [null as number | null],
    reorderQty:   [null as number | null],
    standardCost: [null as number | null, [Validators.min(0)]],
    listPrice:    [null as number | null, [Validators.min(0)]],
    hsnSacCode:      ['', [Validators.maxLength(20)]],
    defaultTaxRuleId: [null as number | null],
    isAsset:      [false],
    isConsumable: [true],
    isService:    [false],
    isLoanable:   [false],
    trackingMode: ['NONE' as StockTrackingMode],
    depreciationRate:      [null as number | null],
    warrantyPeriodMonths:  [null as number | null],
    lengthCm:  [null as number | null, [Validators.min(0)]],
    widthCm:   [null as number | null, [Validators.min(0)]],
    heightCm:  [null as number | null, [Validators.min(0)]],
    weightKg:  [null as number | null, [Validators.min(0)]],
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
    this.brandService.getAll(true).subscribe({ next: (b) => this.brands.set(b) });
    this.taxRuleService.getAll(true).subscribe({ next: (t) => this.taxRules.set(t) });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.productId = Number(idParam);
      this.savedProductId.set(this.productId);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Product');
      this.loadProduct();
      this.loadUomChain();
    }
    this.setupUniquenessValidators();

    this.form.get('baseUomId')?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((baseUomId) => {
      this.loadApplicableTemplates(baseUomId);
    });

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

    const barcodeCtrl = this.form.get('barcode');
    barcodeCtrl?.setAsyncValidators(
      uniqueFieldValidator(this.http, `${environment.apiUrl}/inventory/products/barcode-exists`, () => this.productId),
    );
    barcodeCtrl?.updateValueAndValidity({ emitEvent: false });

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

  // ── Unit Hierarchy ──────────────────────────────────────────────────────
  private newUomLevelGroup(uomId: number | null = null, factorToBase: number | null = null, isDefaultPurchase = false): FormGroup {
    return this.fb.group({
      uomId: [uomId as number | null, [Validators.required]],
      factorToBase: [factorToBase as number | null, [Validators.required, Validators.min(0.000001)]],
      isDefaultPurchase: [isDefaultPurchase],
    });
  }

  protected addUomLevel(): void {
    this.uomLevels.push(this.newUomLevelGroup());
  }

  protected removeUomLevel(index: number): void {
    this.uomLevels.removeAt(index);
  }

  private loadApplicableTemplates(baseUomId: number | null): void {
    if (baseUomId == null) { this.applicableTemplates.set([]); return; }
    this.uomTemplateService.getApplicableTo(baseUomId).subscribe({
      next: (templates) => this.applicableTemplates.set(templates),
      error: () => this.applicableTemplates.set([]),
    });
  }

  /** Pre-fills the levels-above-base array from a template's own levels — the levels are then
   * ordinary editable rows, only actually saved when "Save Unit Hierarchy" is clicked. No link is
   * kept to the template afterward. See the "Shared/global UOM conversion templates"
   * decision-log entry. */
  protected applyUomTemplate(templateIdStr: string): void {
    const templateId = Number(templateIdStr);
    const template = this.applicableTemplates().find(t => t.id === templateId);
    if (!template) return;
    this.uomLevels.clear();
    const aboveBase = (template.levels ?? []).filter(l => l.levelRank > 0).sort((a, b) => a.levelRank - b.levelRank);
    for (const level of aboveBase) {
      this.uomLevels.push(this.newUomLevelGroup(level.uomId, level.factorToBase, level.isDefaultPurchase));
    }
    this.toast.success(`Applied "${template.name}" — review and save when ready`);
  }

  /** A level above the base is only removable/reorderable through this form — level 0 (the
   * product's base unit) is always implied and never part of this editable array. */
  private loadUomChain(): void {
    if (!this.productId) return;
    this.chainLoading.set(true);
    this.productService.getActiveUomChain(this.productId).subscribe({
      next: (chain) => {
        this.activeChain.set(chain);
        this.uomLevels.clear();
        const aboveBase = (chain?.levels ?? []).filter(l => l.levelRank > 0).sort((a, b) => a.levelRank - b.levelRank);
        for (const level of aboveBase) {
          this.uomLevels.push(this.newUomLevelGroup(level.uomId, level.factorToBase, level.isDefaultPurchase));
        }
        this.chainLoading.set(false);
      },
      error: () => { this.toast.error('Failed to load unit hierarchy'); this.chainLoading.set(false); },
    });
  }

  protected loadUomChainVersions(): void {
    if (!this.productId) return;
    this.showVersionHistory.set(true);
    this.productService.getUomChainVersions(this.productId).subscribe({
      next: (versions) => this.chainVersions.set(versions),
      error: () => this.toast.error('Failed to load version history'),
    });
  }

  protected saveUomChain(): void {
    if (!this.productId) return;
    if (this.uomLevels.invalid) {
      this.uomLevels.markAllAsTouched();
      return;
    }
    const baseUomId = this.form.value.baseUomId;
    if (!baseUomId) {
      this.toast.error('Select a base unit of measure above before saving the unit hierarchy');
      return;
    }

    const levels: ProductUomLevelRequest[] = [
      { uomId: baseUomId, levelRank: 0, factorToBase: 1, isDefaultPurchase: false },
      ...this.uomLevels.controls.map((g, i) => ({
        uomId: g.value.uomId,
        levelRank: i + 1,
        factorToBase: g.value.factorToBase,
        isDefaultPurchase: !!g.value.isDefaultPurchase,
      })),
    ];

    this.chainSaving.set(true);
    this.productService.saveUomChainVersion(this.productId, { levels }).subscribe({
      next: (version) => {
        this.activeChain.set(version);
        this.chainSaving.set(false);
        this.toast.success('Unit hierarchy saved');
        if (this.showVersionHistory()) this.loadUomChainVersions();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to save unit hierarchy');
        this.chainSaving.set(false);
      },
    });
  }

  protected uomName(uomId: number): string {
    return this.uoms().find(u => u.id === uomId)?.name ?? '—';
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
      barcode:      v.barcode?.trim() || undefined,
      categoryId:   v.categoryId,
      baseUomId:    v.baseUomId,
      brandId:      v.brandId ?? undefined,
      reorderLevel: v.reorderLevel,
      reorderQty:   v.reorderQty,
      standardCost: v.standardCost,
      listPrice:    v.listPrice,
      hsnSacCode:      v.hsnSacCode?.trim() || undefined,
      defaultTaxRuleId: v.defaultTaxRuleId ?? undefined,
      isAsset:      !!v.isAsset,
      isConsumable: !!v.isConsumable,
      isService:    !!v.isService,
      isLoanable:   !!v.isLoanable,
      trackingMode: v.trackingMode,
      depreciationRate:     v.depreciationRate,
      warrantyPeriodMonths: v.warrantyPeriodMonths,
      lengthCm: v.lengthCm,
      widthCm:  v.widthCm,
      heightCm: v.heightCm,
      weightKg: v.weightKg,
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
    productCode: 'Product code', productName: 'Product name', barcode: 'Barcode', categoryId: 'Category', baseUomId: 'Base unit of measure',
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
          barcode: p.barcode || '',
          categoryId: p.categoryId,
          baseUomId: p.baseUomId,
          brandId: p.brandId ?? null,
          reorderLevel: p.reorderLevel ?? null,
          reorderQty: p.reorderQty ?? null,
          standardCost: p.standardCost ?? null,
          listPrice: p.listPrice ?? null,
          hsnSacCode: p.hsnSacCode || '',
          defaultTaxRuleId: p.defaultTaxRuleId ?? null,
          isAsset: p.isAsset,
          isConsumable: p.isConsumable,
          isService: p.isService,
          isLoanable: p.isLoanable,
          trackingMode: p.trackingMode ?? 'NONE',
          depreciationRate: p.depreciationRate ?? null,
          warrantyPeriodMonths: p.warrantyPeriodMonths ?? null,
          lengthCm: p.lengthCm ?? null,
          widthCm: p.widthCm ?? null,
          heightCm: p.heightCm ?? null,
          weightKg: p.weightKg ?? null,
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

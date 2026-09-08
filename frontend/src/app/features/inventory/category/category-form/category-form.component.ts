import { Component, DestroyRef, inject, OnInit, signal, computed } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { CmsPreviewCardComponent } from '../../../../shared/preview-card/preview-card.component';
import { CategoryService } from '../category.service';
import { Category, CategoryRequest } from '../category.model';
import { CategoryAttributeService } from '../category-attribute.service';
import { AttributeDataType, CategoryAttribute, CategoryAttributeRequest } from '../category-attribute.model';
import { ToastService } from '../../../../core/toast/toast.service';
import { ConfirmDialogComponent } from '../../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsRowActionButtonComponent } from '../../../../shared/row-action-button/row-action-button.component';
import { CmsIconEditComponent } from '../../../../shared/icons';
import { scrollToFirstInvalid } from '../../../../shared/utils/scroll-to-invalid';
import { noConsecutiveSpaces, trimmedMinLength, cmsFieldError } from '../../../../shared/validators/cms-validators';
import { environment } from '../../../../../environments';
import { uniqueFieldValidator } from '../../../../shared/validators/unique-field.validator';

@Component({
  selector: 'app-category-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    CmsPreviewCardComponent,
    CmsRowActionButtonComponent,
    CmsIconEditComponent,
  ],
  templateUrl: './category-form.component.html',
  styleUrl: './category-form.component.scss',
})
export class CategoryFormComponent implements OnInit {
  private readonly fb              = inject(FormBuilder);
  private readonly route           = inject(ActivatedRoute);
  private readonly router          = inject(Router);
  private readonly categoryService = inject(CategoryService);
  private readonly attributeService = inject(CategoryAttributeService);
  private readonly toast           = inject(ToastService);
  private readonly destroyRef      = inject(DestroyRef);
  private readonly http            = inject(HttpClient);
  private readonly dialog          = inject(MatDialog);

  protected readonly loading    = signal(false);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle  = signal('Add Category');
  protected readonly allCategories = signal<Category[]>([]);

  protected readonly previewName       = signal('');
  protected readonly previewParentName = signal('');

  private categoryId: number | null = null;

  // ── Attributes (only editable once the category itself exists) ────────────
  protected readonly attributes = signal<CategoryAttribute[]>([]);
  protected readonly attributesLoading = signal(false);
  protected readonly attributeSaving = signal(false);
  protected readonly editingAttributeId = signal<number | null>(null);
  protected readonly dataTypes: AttributeDataType[] = ['TEXT', 'NUMBER', 'DATE', 'BOOLEAN', 'ENUM'];

  protected readonly attributeForm: FormGroup = this.fb.group({
    name:         ['', [Validators.required, Validators.maxLength(100)]],
    dataType:     ['TEXT' as AttributeDataType, [Validators.required]],
    enumOptions:  ['', [Validators.maxLength(1000)]],
    isRequired:   [false],
    displayOrder: [0],
  });

  // Excludes self and any descendant of self, so a category can never be moved under its own subtree.
  protected readonly parentOptions = computed(() => {
    const all = this.allCategories();
    if (this.categoryId == null) return all;
    const excluded = new Set<number>([this.categoryId]);
    let changed = true;
    while (changed) {
      changed = false;
      for (const c of all) {
        if (c.parentCategoryId != null && excluded.has(c.parentCategoryId) && !excluded.has(c.id)) {
          excluded.add(c.id);
          changed = true;
        }
      }
    }
    return all.filter(c => !excluded.has(c.id));
  });

  protected readonly form: FormGroup = this.fb.group({
    name:              ['', [Validators.required, trimmedMinLength(2), Validators.maxLength(150), noConsecutiveSpaces()]],
    parentCategoryId:  [null as number | null],
    description:       ['', [Validators.maxLength(500)]],
  });

  constructor() {
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(v => {
        this.previewName.set((v.name ?? '').trim());
        const parent = this.allCategories().find(c => c.id === v.parentCategoryId);
        this.previewParentName.set(parent?.name ?? '');
      });
  }

  ngOnInit(): void {
    this.categoryService.getAll().subscribe({ next: (c) => this.allCategories.set(c) });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.categoryId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Category');
      this.loadCategory();
      this.loadAttributes();
    }
    this.setupUniquenessValidator();
  }

  private setupUniquenessValidator(): void {
    const nameCtrl = this.form.get('name');
    if (!nameCtrl) return;
    nameCtrl.setAsyncValidators(
      uniqueFieldValidator(
        this.http,
        `${environment.apiUrl}/inventory/categories/name-exists`,
        () => this.categoryId,
        (): Record<string, string | number> => {
          const parentId = this.form.value.parentCategoryId;
          const params: Record<string, string | number> = {};
          if (parentId != null) params['parentCategoryId'] = parentId;
          return params;
        },
      ),
    );
    nameCtrl.updateValueAndValidity({ emitEvent: false });

    // Re-validate uniqueness whenever the parent scope changes — the same name may be free
    // under the newly selected parent even if it was taken under the previous one.
    this.form.get('parentCategoryId')?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      nameCtrl.updateValueAndValidity({ emitEvent: false });
    });
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }

    const request: CategoryRequest = {
      name:              (this.form.value.name ?? '').trim(),
      parentCategoryId:  this.form.value.parentCategoryId,
      description:       this.form.value.description?.trim() || undefined,
    };

    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.categoryService.update(this.categoryId!, request)
      : this.categoryService.create(request);

    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Category updated successfully' : 'Category created successfully');
        this.saving.set(false);
        void this.router.navigate(['/inventory/categories']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? (this.isEditMode() ? 'Failed to update category' : 'Failed to create category'));
        this.saving.set(false);
      },
    });
  }

  private static readonly FIELD_LABELS: Record<string, string> = {
    name: 'Name', parentCategoryId: 'Parent category', description: 'Description',
  };

  protected getErrorMessage(fieldName: string): string {
    return cmsFieldError(this.form.get(fieldName), CategoryFormComponent.FIELD_LABELS[fieldName] ?? fieldName);
  }

  // ── Attribute management ────────────────────────────────────────────────
  protected loadAttributes(): void {
    if (!this.categoryId) return;
    this.attributesLoading.set(true);
    this.attributeService.findByCategory(this.categoryId).subscribe({
      next: (a) => { this.attributes.set(a); this.attributesLoading.set(false); },
      error: () => { this.toast.error('Failed to load attributes'); this.attributesLoading.set(false); },
    });
  }

  protected editAttribute(attr: CategoryAttribute): void {
    this.editingAttributeId.set(attr.id);
    this.attributeForm.patchValue({
      name: attr.name,
      dataType: attr.dataType,
      enumOptions: attr.enumOptions || '',
      isRequired: attr.isRequired,
      displayOrder: attr.displayOrder,
    });
  }

  protected cancelAttributeEdit(): void {
    this.editingAttributeId.set(null);
    this.attributeForm.reset({ name: '', dataType: 'TEXT', enumOptions: '', isRequired: false, displayOrder: 0 });
  }

  protected saveAttribute(): void {
    if (!this.categoryId || this.attributeForm.invalid) {
      scrollToFirstInvalid(this.attributeForm);
      return;
    }
    const v = this.attributeForm.value;
    const request: CategoryAttributeRequest = {
      name: (v.name ?? '').trim(),
      dataType: v.dataType,
      enumOptions: v.dataType === 'ENUM' ? (v.enumOptions?.trim() || undefined) : undefined,
      isRequired: !!v.isRequired,
      displayOrder: v.displayOrder ?? 0,
    };

    this.attributeSaving.set(true);
    const editingId = this.editingAttributeId();
    const op$ = editingId != null
      ? this.attributeService.update(this.categoryId, editingId, request)
      : this.attributeService.create(this.categoryId, request);

    op$.subscribe({
      next: () => {
        this.toast.success(editingId != null ? 'Attribute updated successfully' : 'Attribute added successfully');
        this.attributeSaving.set(false);
        this.cancelAttributeEdit();
        this.loadAttributes();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to save attribute');
        this.attributeSaving.set(false);
      },
    });
  }

  protected deleteAttribute(attr: CategoryAttribute): void {
    if (!this.categoryId) return;
    const categoryId = this.categoryId;
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Attribute',
        message: `Delete "${attr.name}"? This cannot be undone.`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.attributeService.delete(categoryId, attr.id).subscribe({
        next: () => { this.toast.success('Attribute removed'); this.loadAttributes(); },
        error: (err) => this.toast.error(err?.error?.message ?? 'Failed to remove attribute'),
      });
    });
  }

  private loadCategory(): void {
    if (!this.categoryId) return;
    this.loading.set(true);
    this.categoryService.getById(this.categoryId).subscribe({
      next: (c) => {
        this.form.patchValue({
          name: c.name,
          parentCategoryId: c.parentCategoryId,
          description: c.description || '',
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load category');
        void this.router.navigate(['/inventory/categories']);
      },
    });
  }
}

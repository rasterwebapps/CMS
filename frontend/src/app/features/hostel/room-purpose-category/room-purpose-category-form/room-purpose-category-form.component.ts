import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CmsPreviewCardComponent } from '../../../../shared/preview-card/preview-card.component';
import { RoomPurposeCategoryService } from '../room-purpose-category.service';
import {
  ROOM_PURPOSE_CATEGORY_CODE_LABELS,
  RoomPurposeCategoryCode,
  RoomPurposeCategoryRequest,
} from '../room-purpose-category.model';
import { ToastService } from '../../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../../shared/utils/scroll-to-invalid';
import { noConsecutiveSpaces, trimmedMinLength, cmsFieldError } from '../../../../shared/validators/cms-validators';
import { environment } from '../../../../../environments';
import { uniqueFieldValidator } from '../../../../shared/validators/unique-field.validator';

@Component({
  selector: 'app-room-purpose-category-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatCheckboxModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    CmsPreviewCardComponent,
  ],
  templateUrl: './room-purpose-category-form.component.html',
  styleUrl: './room-purpose-category-form.component.scss',
})
export class RoomPurposeCategoryFormComponent implements OnInit {
  private readonly fb              = inject(FormBuilder);
  private readonly route           = inject(ActivatedRoute);
  private readonly router          = inject(Router);
  private readonly categoryService = inject(RoomPurposeCategoryService);
  private readonly toast           = inject(ToastService);
  private readonly destroyRef      = inject(DestroyRef);
  private readonly http            = inject(HttpClient);

  protected readonly loading    = signal(false);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle  = signal('Add Room Purpose Category');

  protected readonly previewName          = signal('');
  protected readonly previewCode          = signal('');
  protected readonly previewIsResidential = signal(false);

  /** Every valid code, for the create-mode dropdown; filtered against usedCodes() in the
   *  template so an admin can only pick a code no existing category already holds. */
  protected readonly allCodeOptions: { value: RoomPurposeCategoryCode; label: string }[] =
    (Object.entries(ROOM_PURPOSE_CATEGORY_CODE_LABELS) as [RoomPurposeCategoryCode, string][])
      .map(([value, label]) => ({ value, label }));
  protected readonly usedCodes = signal<Set<RoomPurposeCategoryCode>>(new Set());
  protected readonly codeLabels = ROOM_PURPOSE_CATEGORY_CODE_LABELS;

  private categoryId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name:          ['', [Validators.required, trimmedMinLength(2), Validators.maxLength(100), noConsecutiveSpaces()]],
    code:          [null as RoomPurposeCategoryCode | null, [Validators.required]],
    isResidential: [false],
    description:   ['', [Validators.maxLength(500)]],
  });

  constructor() {
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(v => {
        this.previewName.set((v.name ?? '').trim());
        // .value directly, not the destructured v -- a disabled control (locked code, edit mode)
        // is excluded from the FormGroup's own aggregate value.
        this.previewCode.set(this.form.get('code')?.value ?? '');
        this.previewIsResidential.set(!!v.isResidential);
      });
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.categoryId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Room Purpose Category');
      this.loadCategory();
    } else {
      this.loadUsedCodes();
    }
    this.setupUniquenessValidators();
  }

  protected availableCodeOptions(): { value: RoomPurposeCategoryCode; label: string }[] {
    const used = this.usedCodes();
    return this.allCodeOptions.filter((o) => !used.has(o.value));
  }

  private loadUsedCodes(): void {
    this.categoryService.getAll(false).subscribe({
      next: (categories) => this.usedCodes.set(new Set(categories.map((c) => c.code))),
      error: () => this.toast.error('Failed to load existing category codes'),
    });
  }

  private setupUniquenessValidators(): void {
    const nameCtrl = this.form.get('name');
    if (nameCtrl) {
      nameCtrl.setAsyncValidators(
        uniqueFieldValidator(this.http, `${environment.apiUrl}/room-purpose-categories/name-exists`, () => this.categoryId)
      );
      nameCtrl.updateValueAndValidity({ emitEvent: false });
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }

    const request: RoomPurposeCategoryRequest = {
      name:          (this.form.value.name ?? '').trim(),
      code:          this.form.get('code')?.value,
      isResidential: !!this.form.value.isResidential,
      description:   this.form.value.description?.trim() || undefined,
    };

    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.categoryService.update(this.categoryId!, request)
      : this.categoryService.create(request);

    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Room purpose category updated successfully' : 'Room purpose category created successfully');
        this.saving.set(false);
        void this.router.navigate(['/room-purpose-categories']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? (this.isEditMode() ? 'Failed to update room purpose category' : 'Failed to create room purpose category'));
        this.saving.set(false);
      },
    });
  }

  private static readonly FIELD_LABELS: Record<string, string> = {
    name: 'Name', code: 'Code', description: 'Description',
  };

  protected getErrorMessage(fieldName: string): string {
    return cmsFieldError(this.form.get(fieldName), RoomPurposeCategoryFormComponent.FIELD_LABELS[fieldName] ?? fieldName);
  }

  private loadCategory(): void {
    if (!this.categoryId) return;
    this.loading.set(true);
    this.categoryService.getById(this.categoryId).subscribe({
      next: (c) => {
        this.form.patchValue({
          name: c.name,
          code: c.code,
          isResidential: c.isResidential,
          description: c.description || '',
        });
        // Code is the fixed identity everything downstream keys off -- locked after creation,
        // same rule enforced server-side in RoomPurposeCategoryService.update().
        this.form.get('code')?.disable();
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load room purpose category');
        void this.router.navigate(['/room-purpose-categories']);
      },
    });
  }
}

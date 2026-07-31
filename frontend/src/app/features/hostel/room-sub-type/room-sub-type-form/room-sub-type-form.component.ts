import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CmsPreviewCardComponent } from '../../../../shared/preview-card/preview-card.component';
import { RoomSubTypeService } from '../room-sub-type.service';
import { RoomSubTypeRequest } from '../room-sub-type.model';
import { RoomPurposeCategoryService } from '../../room-purpose-category/room-purpose-category.service';
import { RoomPurposeCategory } from '../../room-purpose-category/room-purpose-category.model';
import { ToastService } from '../../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../../shared/utils/scroll-to-invalid';
import { noConsecutiveSpaces, noInternalSpaces, trimmedMinLength, cmsFieldError, stripSpaces } from '../../../../shared/validators/cms-validators';
import { environment } from '../../../../../environments';
import { uniqueFieldValidator } from '../../../../shared/validators/unique-field.validator';

@Component({
  selector: 'app-room-sub-type-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    CmsPreviewCardComponent,
  ],
  templateUrl: './room-sub-type-form.component.html',
  styleUrl: './room-sub-type-form.component.scss',
})
export class RoomSubTypeFormComponent implements OnInit {
  private readonly fb              = inject(FormBuilder);
  private readonly route           = inject(ActivatedRoute);
  private readonly router          = inject(Router);
  private readonly subTypeService  = inject(RoomSubTypeService);
  private readonly categoryService = inject(RoomPurposeCategoryService);
  private readonly toast           = inject(ToastService);
  private readonly destroyRef      = inject(DestroyRef);
  private readonly http            = inject(HttpClient);

  protected readonly loading    = signal(false);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle  = signal('Add Room Sub-Type');
  protected readonly categories = signal<RoomPurposeCategory[]>([]);

  protected readonly previewName         = signal('');
  protected readonly previewCode         = signal('');
  protected readonly previewCategoryName = signal('');
  protected readonly codeCharCount       = signal(0);

  private subTypeId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    purposeCategoryId: [null as number | null, [Validators.required]],
    name:              ['', [Validators.required, trimmedMinLength(2), Validators.maxLength(150), noConsecutiveSpaces()]],
    code:              ['', [Validators.required, Validators.maxLength(50), noInternalSpaces()]],
    description:       ['', [Validators.maxLength(500)]],
  });

  constructor() {
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(v => {
        this.previewName.set((v.name ?? '').trim());
        const code = (v.code ?? '').toUpperCase().trim();
        this.previewCode.set(code);
        this.codeCharCount.set(code.length);
        const category = this.categories().find(c => c.id === v.purposeCategoryId);
        this.previewCategoryName.set(category?.name ?? '');
      });
  }

  ngOnInit(): void {
    this.categoryService.getAll(true).subscribe({ next: (c) => this.categories.set(c) });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.subTypeId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Room Sub-Type');
      this.loadSubType();
    }
    this.setupUniquenessValidators();
  }

  private setupUniquenessValidators(): void {
    const nameCtrl = this.form.get('name');
    if (nameCtrl) {
      nameCtrl.setAsyncValidators(
        uniqueFieldValidator(
          this.http,
          `${environment.apiUrl}/room-sub-types/name-exists`,
          () => this.subTypeId,
          () => (this.form.value.purposeCategoryId != null ? { purposeCategoryId: this.form.value.purposeCategoryId } : null),
        ),
      );
      nameCtrl.updateValueAndValidity({ emitEvent: false });
    }
    const codeCtrl = this.form.get('code');
    if (codeCtrl) {
      codeCtrl.setAsyncValidators(
        uniqueFieldValidator(
          this.http,
          `${environment.apiUrl}/room-sub-types/code-exists`,
          () => this.subTypeId,
          () => (this.form.value.purposeCategoryId != null ? { purposeCategoryId: this.form.value.purposeCategoryId } : null),
        ),
      );
      codeCtrl.updateValueAndValidity({ emitEvent: false });
    }

    // Re-validate uniqueness whenever the category scope changes — the same name/code may be
    // free in the newly selected category even if it was taken in the previous one.
    this.form.get('purposeCategoryId')?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      nameCtrl?.updateValueAndValidity({ emitEvent: false });
      codeCtrl?.updateValueAndValidity({ emitEvent: false });
    });
  }

  protected onCodeInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const start = input.selectionStart ?? 0;
    const end   = input.selectionEnd ?? 0;
    const cleaned = stripSpaces(input.value).toUpperCase();
    if (cleaned !== input.value) {
      this.form.get('code')?.setValue(cleaned, { emitEvent: true });
      setTimeout(() => input.setSelectionRange(start, end), 0);
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }

    const request: RoomSubTypeRequest = {
      purposeCategoryId: this.form.value.purposeCategoryId,
      name:               (this.form.value.name ?? '').trim(),
      code:               (this.form.value.code ?? '').trim().toUpperCase(),
      description:        this.form.value.description?.trim() || undefined,
    };

    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.subTypeService.update(this.subTypeId!, request)
      : this.subTypeService.create(request);

    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Room sub-type updated successfully' : 'Room sub-type created successfully');
        this.saving.set(false);
        void this.router.navigate(['/room-sub-types']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? (this.isEditMode() ? 'Failed to update room sub-type' : 'Failed to create room sub-type'));
        this.saving.set(false);
      },
    });
  }

  private static readonly FIELD_LABELS: Record<string, string> = {
    purposeCategoryId: 'Purpose category', name: 'Name', code: 'Code', description: 'Description',
  };

  protected getErrorMessage(fieldName: string): string {
    return cmsFieldError(this.form.get(fieldName), RoomSubTypeFormComponent.FIELD_LABELS[fieldName] ?? fieldName);
  }

  private loadSubType(): void {
    if (!this.subTypeId) return;
    this.loading.set(true);
    this.subTypeService.getById(this.subTypeId).subscribe({
      next: (s) => {
        this.form.patchValue({
          purposeCategoryId: s.purposeCategoryId,
          name: s.name,
          code: s.code,
          description: s.description || '',
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load room sub-type');
        void this.router.navigate(['/room-sub-types']);
      },
    });
  }
}

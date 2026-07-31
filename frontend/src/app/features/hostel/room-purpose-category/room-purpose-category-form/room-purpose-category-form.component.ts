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
import { RoomPurposeCategoryRequest } from '../room-purpose-category.model';
import { ToastService } from '../../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../../shared/utils/scroll-to-invalid';
import { noConsecutiveSpaces, noInternalSpaces, trimmedMinLength, cmsFieldError, stripSpaces } from '../../../../shared/validators/cms-validators';
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
  protected readonly codeCharCount        = signal(0);

  private categoryId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name:          ['', [Validators.required, trimmedMinLength(2), Validators.maxLength(100), noConsecutiveSpaces()]],
    code:          ['', [Validators.required, Validators.maxLength(50), noInternalSpaces()]],
    isResidential: [false],
    description:   ['', [Validators.maxLength(500)]],
  });

  constructor() {
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(v => {
        this.previewName.set((v.name ?? '').trim());
        const code = (v.code ?? '').toUpperCase().trim();
        this.previewCode.set(code);
        this.codeCharCount.set(code.length);
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
    }
    this.setupUniquenessValidators();
  }

  private setupUniquenessValidators(): void {
    const nameCtrl = this.form.get('name');
    if (nameCtrl) {
      nameCtrl.setAsyncValidators(
        uniqueFieldValidator(this.http, `${environment.apiUrl}/room-purpose-categories/name-exists`, () => this.categoryId)
      );
      nameCtrl.updateValueAndValidity({ emitEvent: false });
    }
    const codeCtrl = this.form.get('code');
    if (codeCtrl) {
      codeCtrl.setAsyncValidators(
        uniqueFieldValidator(this.http, `${environment.apiUrl}/room-purpose-categories/code-exists`, () => this.categoryId)
      );
      codeCtrl.updateValueAndValidity({ emitEvent: false });
    }
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

    const request: RoomPurposeCategoryRequest = {
      name:          (this.form.value.name ?? '').trim(),
      code:          (this.form.value.code ?? '').trim().toUpperCase(),
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
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load room purpose category');
        void this.router.navigate(['/room-purpose-categories']);
      },
    });
  }
}

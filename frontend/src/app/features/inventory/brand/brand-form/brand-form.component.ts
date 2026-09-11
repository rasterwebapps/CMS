import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CmsPreviewCardComponent } from '../../../../shared/preview-card/preview-card.component';
import { BrandService } from '../brand.service';
import { BrandRequest } from '../brand.model';
import { ToastService } from '../../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../../shared/utils/scroll-to-invalid';
import { noConsecutiveSpaces, trimmedMinLength, cmsFieldError } from '../../../../shared/validators/cms-validators';
import { environment } from '../../../../../environments';
import { uniqueFieldValidator } from '../../../../shared/validators/unique-field.validator';

@Component({
  selector: 'app-brand-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    CmsPreviewCardComponent,
  ],
  templateUrl: './brand-form.component.html',
  styleUrl: './brand-form.component.scss',
})
export class BrandFormComponent implements OnInit {
  private readonly fb          = inject(FormBuilder);
  private readonly route       = inject(ActivatedRoute);
  private readonly router      = inject(Router);
  private readonly brandService = inject(BrandService);
  private readonly toast       = inject(ToastService);
  private readonly destroyRef  = inject(DestroyRef);
  private readonly http        = inject(HttpClient);

  protected readonly loading    = signal(false);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle  = signal('Add Brand');

  protected readonly previewName = signal('');

  private brandId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name:        ['', [Validators.required, trimmedMinLength(2), Validators.maxLength(150), noConsecutiveSpaces()]],
    description: ['', [Validators.maxLength(500)]],
  });

  constructor() {
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(v => {
        this.previewName.set((v.name ?? '').trim());
      });
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.brandId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Brand');
      this.loadBrand();
    }
    this.setupUniquenessValidators();
  }

  private setupUniquenessValidators(): void {
    const nameCtrl = this.form.get('name');
    nameCtrl?.setAsyncValidators(
      uniqueFieldValidator(this.http, `${environment.apiUrl}/inventory/brands/name-exists`, () => this.brandId),
    );
    nameCtrl?.updateValueAndValidity({ emitEvent: false });
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }

    const request: BrandRequest = {
      name:        (this.form.value.name ?? '').trim(),
      description: this.form.value.description?.trim() || undefined,
    };

    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.brandService.update(this.brandId!, request)
      : this.brandService.create(request);

    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Brand updated successfully' : 'Brand created successfully');
        this.saving.set(false);
        void this.router.navigate(['/inventory/brands']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? (this.isEditMode() ? 'Failed to update brand' : 'Failed to create brand'));
        this.saving.set(false);
      },
    });
  }

  private static readonly FIELD_LABELS: Record<string, string> = {
    name: 'Name', description: 'Description',
  };

  protected getErrorMessage(fieldName: string): string {
    return cmsFieldError(this.form.get(fieldName), BrandFormComponent.FIELD_LABELS[fieldName] ?? fieldName);
  }

  private loadBrand(): void {
    if (!this.brandId) return;
    this.loading.set(true);
    this.brandService.getById(this.brandId).subscribe({
      next: (b) => {
        this.form.patchValue({
          name: b.name,
          description: b.description || '',
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load brand');
        void this.router.navigate(['/inventory/brands']);
      },
    });
  }
}

import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ServiceTicketCategoryService } from '../category.service';
import { ServiceTicketCategoryRequest } from '../category.model';
import { ToastService } from '../../../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../../../shared/utils/scroll-to-invalid';
import { noConsecutiveSpaces, trimmedMinLength, cmsFieldError } from '../../../../../shared/validators/cms-validators';
import { environment } from '../../../../../../environments';
import { uniqueFieldValidator } from '../../../../../shared/validators/unique-field.validator';

@Component({
  selector: 'app-service-ticket-category-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './category-form.component.html',
  styleUrl: './category-form.component.scss',
})
export class ServiceTicketCategoryFormComponent implements OnInit {
  private readonly fb              = inject(FormBuilder);
  private readonly route           = inject(ActivatedRoute);
  private readonly router          = inject(Router);
  private readonly categoryService = inject(ServiceTicketCategoryService);
  private readonly toast           = inject(ToastService);
  private readonly destroyRef      = inject(DestroyRef);
  private readonly http            = inject(HttpClient);

  protected readonly loading    = signal(false);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);

  private categoryId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name:        ['', [Validators.required, trimmedMinLength(2), Validators.maxLength(100), noConsecutiveSpaces()]],
    description: ['', [Validators.maxLength(500)]],
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.categoryId = Number(idParam);
      this.isEditMode.set(true);
      this.loadCategory();
    }
    this.setupUniquenessValidator();
  }

  private setupUniquenessValidator(): void {
    const nameCtrl = this.form.get('name');
    nameCtrl?.setAsyncValidators(
      uniqueFieldValidator(this.http, `${environment.apiUrl}/inventory/ticket/categories/name-exists`, () => this.categoryId),
    );
    nameCtrl?.updateValueAndValidity({ emitEvent: false });
  }

  protected getErrorMessage(fieldName: string): string {
    const labels: Record<string, string> = { name: 'Name', description: 'Description' };
    return cmsFieldError(this.form.get(fieldName), labels[fieldName] ?? fieldName);
  }

  private loadCategory(): void {
    this.loading.set(true);
    this.categoryService.getById(this.categoryId!).subscribe({
      next: (c) => {
        this.form.patchValue({ name: c.name, description: c.description ?? '' });
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load category'); this.loading.set(false); },
    });
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }
    const request: ServiceTicketCategoryRequest = {
      name: this.form.value.name.trim(),
      description: this.form.value.description?.trim() || undefined,
    };

    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.categoryService.update(this.categoryId!, request)
      : this.categoryService.create(request);

    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Category updated' : 'Category created');
        this.saving.set(false);
        void this.router.navigate(['/inventory/ticket/categories']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to save category');
        this.saving.set(false);
      },
    });
  }
}

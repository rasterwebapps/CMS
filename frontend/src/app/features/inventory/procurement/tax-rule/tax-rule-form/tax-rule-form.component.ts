import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TaxRuleService } from '../tax-rule.service';
import { TaxRuleRequest } from '../tax-rule.model';
import { ToastService } from '../../../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../../../shared/utils/scroll-to-invalid';
import { noConsecutiveSpaces, trimmedMinLength, cmsFieldError } from '../../../../../shared/validators/cms-validators';
import { environment } from '../../../../../../environments';
import { uniqueFieldValidator } from '../../../../../shared/validators/unique-field.validator';

@Component({
  selector: 'app-tax-rule-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './tax-rule-form.component.html',
  styleUrl: './tax-rule-form.component.scss',
})
export class TaxRuleFormComponent implements OnInit {
  private readonly fb           = inject(FormBuilder);
  private readonly route        = inject(ActivatedRoute);
  private readonly router       = inject(Router);
  private readonly taxRuleService = inject(TaxRuleService);
  private readonly toast        = inject(ToastService);
  private readonly destroyRef   = inject(DestroyRef);
  private readonly http         = inject(HttpClient);

  protected readonly loading    = signal(false);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle  = signal('Add Tax Rule');

  private taxRuleId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name:        ['', [Validators.required, trimmedMinLength(2), Validators.maxLength(100), noConsecutiveSpaces()]],
    ratePercent: [null as number | null, [Validators.required, Validators.min(0)]],
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.taxRuleId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Tax Rule');
      this.loadTaxRule();
    }
    const nameCtrl = this.form.get('name');
    nameCtrl?.setAsyncValidators(
      uniqueFieldValidator(this.http, `${environment.apiUrl}/inventory/procurement/tax-rules/name-exists`, () => this.taxRuleId),
    );
    nameCtrl?.updateValueAndValidity({ emitEvent: false });
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }

    const request: TaxRuleRequest = {
      name: (this.form.value.name ?? '').trim(),
      ratePercent: this.form.value.ratePercent,
    };

    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.taxRuleService.update(this.taxRuleId!, request)
      : this.taxRuleService.create(request);

    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Tax rule updated successfully' : 'Tax rule created successfully');
        this.saving.set(false);
        void this.router.navigate(['/inventory/procurement/tax-rules']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? (this.isEditMode() ? 'Failed to update tax rule' : 'Failed to create tax rule'));
        this.saving.set(false);
      },
    });
  }

  private static readonly FIELD_LABELS: Record<string, string> = {
    name: 'Name', ratePercent: 'Rate',
  };

  protected getErrorMessage(fieldName: string): string {
    return cmsFieldError(this.form.get(fieldName), TaxRuleFormComponent.FIELD_LABELS[fieldName] ?? fieldName);
  }

  private loadTaxRule(): void {
    if (!this.taxRuleId) return;
    this.loading.set(true);
    this.taxRuleService.getById(this.taxRuleId).subscribe({
      next: (t) => {
        this.form.patchValue({ name: t.name, ratePercent: t.ratePercent });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load tax rule');
        void this.router.navigate(['/inventory/procurement/tax-rules']);
      },
    });
  }
}

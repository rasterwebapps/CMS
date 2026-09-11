import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CurrencyExchangeRateService } from '../currency-exchange-rate.service';
import { CurrencyExchangeRateRequest } from '../currency-exchange-rate.model';
import { ToastService } from '../../../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../../../shared/utils/scroll-to-invalid';
import { cmsFieldError } from '../../../../../shared/validators/cms-validators';
import { environment } from '../../../../../../environments';
import { uniqueFieldValidator } from '../../../../../shared/validators/unique-field.validator';

@Component({
  selector: 'app-currency-exchange-rate-form',
  standalone: true,
  imports: [RouterLink, ReactiveFormsModule, MatButtonModule, MatProgressSpinnerModule],
  templateUrl: './currency-exchange-rate-form.component.html',
  styleUrl: './currency-exchange-rate-form.component.scss',
})
export class CurrencyExchangeRateFormComponent implements OnInit {
  private readonly fb          = inject(FormBuilder);
  private readonly route       = inject(ActivatedRoute);
  private readonly router      = inject(Router);
  private readonly rateService = inject(CurrencyExchangeRateService);
  private readonly toast       = inject(ToastService);
  private readonly http        = inject(HttpClient);
  private readonly destroyRef  = inject(DestroyRef);

  protected readonly loading    = signal(false);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);

  private rateId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    currencyCode:   ['', [Validators.required, Validators.minLength(3), Validators.maxLength(3)]],
    rateToBase:     [null as number | null, [Validators.required, Validators.min(0.000001)]],
    effectiveDate:  ['', [Validators.required]],
    isActive:       [true],
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.rateId = Number(idParam);
      this.isEditMode.set(true);
      this.loadRate();
    }
    this.setupUniquenessValidator();
  }

  private setupUniquenessValidator(): void {
    // Checked on effectiveDate (the field most likely typed/changed last), scoped by currencyCode
    // — matches the shared uniqueFieldValidator convention (see VendorProductMapping's pair form).
    const dateCtrl = this.form.get('effectiveDate');
    dateCtrl?.setAsyncValidators(
      uniqueFieldValidator(
        this.http,
        `${environment.apiUrl}/inventory/procurement/currency-exchange-rates/pair-exists`,
        () => this.rateId,
        () => {
          const code = (this.form.value.currencyCode ?? '').trim();
          return code.length === 3 ? { currencyCode: code.toUpperCase() } : null;
        },
      ),
    );
    dateCtrl?.updateValueAndValidity({ emitEvent: false });
    // Re-check the date once the currency code is typed/changed (the date may already hold a
    // value from before the code was known).
    this.form.get('currencyCode')?.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => dateCtrl?.updateValueAndValidity());
  }

  protected getErrorMessage(fieldName: string): string {
    const labels: Record<string, string> = { currencyCode: 'Currency code', rateToBase: 'Rate', effectiveDate: 'Effective date' };
    if (fieldName === 'effectiveDate' && this.form.get('effectiveDate')?.hasError('duplicate')) {
      return 'A rate for this currency on this date already exists';
    }
    return cmsFieldError(this.form.get(fieldName), labels[fieldName] ?? fieldName);
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }
    const request: CurrencyExchangeRateRequest = {
      currencyCode: (this.form.value.currencyCode ?? '').trim().toUpperCase(),
      rateToBase: this.form.value.rateToBase,
      effectiveDate: this.form.value.effectiveDate,
      isActive: this.form.value.isActive,
    };

    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.rateService.update(this.rateId!, request)
      : this.rateService.create(request);

    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Exchange rate updated successfully' : 'Exchange rate created successfully');
        this.saving.set(false);
        void this.router.navigate(['/inventory/procurement/currency-exchange-rates']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? (this.isEditMode() ? 'Failed to update exchange rate' : 'Failed to create exchange rate'));
        this.saving.set(false);
      },
    });
  }

  private loadRate(): void {
    if (!this.rateId) return;
    this.loading.set(true);
    this.rateService.getById(this.rateId).subscribe({
      next: (r) => {
        this.form.patchValue({
          currencyCode: r.currencyCode,
          rateToBase: r.rateToBase,
          effectiveDate: r.effectiveDate,
          isActive: r.isActive,
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load exchange rate');
        void this.router.navigate(['/inventory/procurement/currency-exchange-rates']);
      },
    });
  }
}

import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CurrencySettingsService } from './currency-settings.service';
import { InventoryCurrencySettingsRequest } from './currency-settings.model';
import { ToastService } from '../../../../core/toast/toast.service';
import { cmsFieldError } from '../../../../shared/validators/cms-validators';

/**
 * A singleton settings screen (no list, no "new"/"id" routes) — mirrors the backend's
 * single-row InventoryCurrencySetting. See the 2026-09-11 "Multi-currency FX" decision-log entry.
 */
@Component({
  selector: 'app-currency-settings',
  standalone: true,
  imports: [ReactiveFormsModule, MatButtonModule, MatProgressSpinnerModule, DatePipe],
  templateUrl: './currency-settings.component.html',
  styleUrl: './currency-settings.component.scss',
})
export class CurrencySettingsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly settingsService = inject(CurrencySettingsService);
  private readonly toast = inject(ToastService);

  protected readonly loading = signal(false);
  protected readonly saving  = signal(false);
  protected readonly lastUpdatedAt = signal<string | null>(null);
  protected readonly lastUpdatedBy = signal<string | null>(null);

  protected readonly form: FormGroup = this.fb.group({
    baseCurrencyCode: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(3)]],
  });

  ngOnInit(): void {
    this.loading.set(true);
    this.settingsService.find().subscribe({
      next: (settings) => {
        this.form.patchValue({ baseCurrencyCode: settings.baseCurrencyCode ?? '' });
        this.lastUpdatedAt.set(settings.updatedAt);
        this.lastUpdatedBy.set(settings.updatedBy);
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load currency settings'); this.loading.set(false); },
    });
  }

  protected getErrorMessage(fieldName: string): string {
    return cmsFieldError(this.form.get(fieldName), 'Base currency code');
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const request: InventoryCurrencySettingsRequest = {
      baseCurrencyCode: (this.form.value.baseCurrencyCode ?? '').trim().toUpperCase(),
    };
    this.saving.set(true);
    this.settingsService.save(request).subscribe({
      next: (settings) => {
        this.toast.success('Currency settings saved');
        this.form.patchValue({ baseCurrencyCode: settings.baseCurrencyCode ?? '' });
        this.lastUpdatedAt.set(settings.updatedAt);
        this.lastUpdatedBy.set(settings.updatedBy);
        this.saving.set(false);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to save currency settings');
        this.saving.set(false);
      },
    });
  }
}

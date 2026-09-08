import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { SupplierService } from '../supplier.service';
import { SupplierRequest } from '../supplier.model';
import { ToastService } from '../../../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../../../shared/utils/scroll-to-invalid';
import { noConsecutiveSpaces, noInternalSpaces, trimmedMinLength, cmsFieldError, stripSpaces } from '../../../../../shared/validators/cms-validators';
import { environment } from '../../../../../../environments';
import { uniqueFieldValidator } from '../../../../../shared/validators/unique-field.validator';

@Component({
  selector: 'app-supplier-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './supplier-form.component.html',
  styleUrl: './supplier-form.component.scss',
})
export class SupplierFormComponent implements OnInit {
  private readonly fb              = inject(FormBuilder);
  private readonly route           = inject(ActivatedRoute);
  private readonly router          = inject(Router);
  private readonly supplierService = inject(SupplierService);
  private readonly toast           = inject(ToastService);
  private readonly destroyRef      = inject(DestroyRef);
  private readonly http            = inject(HttpClient);

  protected readonly loading    = signal(false);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);

  private supplierId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    supplierCode:         ['', [Validators.required, Validators.maxLength(50), noInternalSpaces()]],
    supplierName:         ['', [Validators.required, trimmedMinLength(2), Validators.maxLength(200), noConsecutiveSpaces()]],
    contactPerson:        ['', [Validators.maxLength(150)]],
    email:                ['', [Validators.email, Validators.maxLength(150)]],
    phone:                ['', [Validators.maxLength(30)]],
    taxRegistrationId:    ['', [Validators.maxLength(50)]],
    legalRegistrationNo:  ['', [Validators.maxLength(50)]],
    bankAccountNumber:    ['', [Validators.maxLength(40)]],
    bankIfscCode:         ['', [Validators.maxLength(20)]],
    bankName:             ['', [Validators.maxLength(150)]],
    bankAccountHolder:    ['', [Validators.maxLength(150)]],
    portalAccessEnabled:  [false],
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.supplierId = Number(idParam);
      this.isEditMode.set(true);
      this.loadSupplier();
    }
    const codeCtrl = this.form.get('supplierCode');
    codeCtrl?.setAsyncValidators(
      uniqueFieldValidator(this.http, `${environment.apiUrl}/inventory/procurement/suppliers/code-exists`, () => this.supplierId),
    );
    codeCtrl?.updateValueAndValidity({ emitEvent: false });
  }

  protected onCodeInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const start = input.selectionStart ?? 0;
    const end   = input.selectionEnd ?? 0;
    const cleaned = stripSpaces(input.value).toUpperCase();
    if (cleaned !== input.value) {
      this.form.get('supplierCode')?.setValue(cleaned, { emitEvent: true });
      setTimeout(() => input.setSelectionRange(start, end), 0);
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }

    const v = this.form.value;
    const request: SupplierRequest = {
      supplierCode:        (v.supplierCode ?? '').trim().toUpperCase(),
      supplierName:        (v.supplierName ?? '').trim(),
      contactPerson:       v.contactPerson?.trim() || undefined,
      email:               v.email?.trim() || undefined,
      phone:               v.phone?.trim() || undefined,
      taxRegistrationId:   v.taxRegistrationId?.trim() || undefined,
      legalRegistrationNo: v.legalRegistrationNo?.trim() || undefined,
      bankAccountNumber:   v.bankAccountNumber?.trim() || undefined,
      bankIfscCode:        v.bankIfscCode?.trim() || undefined,
      bankName:            v.bankName?.trim() || undefined,
      bankAccountHolder:   v.bankAccountHolder?.trim() || undefined,
      portalAccessEnabled: v.portalAccessEnabled,
    };

    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.supplierService.update(this.supplierId!, request)
      : this.supplierService.create(request);

    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Supplier updated successfully' : 'Supplier created successfully');
        this.saving.set(false);
        void this.router.navigate(['/inventory/procurement/suppliers']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? (this.isEditMode() ? 'Failed to update supplier' : 'Failed to create supplier'));
        this.saving.set(false);
      },
    });
  }

  private static readonly FIELD_LABELS: Record<string, string> = {
    supplierCode: 'Supplier code', supplierName: 'Supplier name', email: 'Email',
  };

  protected getErrorMessage(fieldName: string): string {
    return cmsFieldError(this.form.get(fieldName), SupplierFormComponent.FIELD_LABELS[fieldName] ?? fieldName);
  }

  private loadSupplier(): void {
    if (!this.supplierId) return;
    this.loading.set(true);
    this.supplierService.getById(this.supplierId).subscribe({
      next: (s) => {
        this.form.patchValue({
          supplierCode: s.supplierCode,
          supplierName: s.supplierName,
          contactPerson: s.contactPerson || '',
          email: s.email || '',
          phone: s.phone || '',
          taxRegistrationId: s.taxRegistrationId || '',
          legalRegistrationNo: s.legalRegistrationNo || '',
          bankAccountNumber: s.bankAccountNumber || '',
          bankIfscCode: s.bankIfscCode || '',
          bankName: s.bankName || '',
          bankAccountHolder: s.bankAccountHolder || '',
          portalAccessEnabled: s.portalAccessEnabled,
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load supplier');
        void this.router.navigate(['/inventory/procurement/suppliers']);
      },
    });
  }
}

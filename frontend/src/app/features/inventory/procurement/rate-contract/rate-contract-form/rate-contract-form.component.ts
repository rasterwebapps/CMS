import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RateContractService } from '../rate-contract.service';
import { RateContractRequest } from '../rate-contract.model';
import { SupplierService } from '../../supplier/supplier.service';
import { Supplier } from '../../supplier/supplier.model';
import { ToastService } from '../../../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../../../shared/utils/scroll-to-invalid';
import { cmsFieldError } from '../../../../../shared/validators/cms-validators';

@Component({
  selector: 'app-rate-contract-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './rate-contract-form.component.html',
  styleUrl: './rate-contract-form.component.scss',
})
export class RateContractFormComponent implements OnInit {
  private readonly fb                  = inject(FormBuilder);
  private readonly route               = inject(ActivatedRoute);
  private readonly router              = inject(Router);
  private readonly rateContractService = inject(RateContractService);
  private readonly supplierService     = inject(SupplierService);
  private readonly toast               = inject(ToastService);

  protected readonly loading    = signal(false);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly suppliers  = signal<Supplier[]>([]);

  private contractId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    supplierId:           [null as number | null, [Validators.required]],
    startDate:            ['', [Validators.required]],
    endDate:              [''],
    contractValueCap:     [null as number | null, [Validators.min(0)]],
    termsText:            ['', [Validators.maxLength(2000)]],
    renewalReminderDate:  [''],
  });

  ngOnInit(): void {
    this.supplierService.getAll(true).subscribe({ next: (s) => this.suppliers.set(s) });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.contractId = Number(idParam);
      this.isEditMode.set(true);
      this.loadContract();
    }
  }

  protected getErrorMessage(fieldName: string): string {
    const labels: Record<string, string> = { supplierId: 'Supplier', startDate: 'Start date' };
    return cmsFieldError(this.form.get(fieldName), labels[fieldName] ?? fieldName);
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }

    const v = this.form.value;
    if (v.endDate && v.endDate < v.startDate) {
      this.toast.error('End date cannot be before the start date');
      return;
    }

    const request: RateContractRequest = {
      supplierId: v.supplierId,
      startDate: v.startDate,
      endDate: v.endDate || undefined,
      contractValueCap: v.contractValueCap ?? undefined,
      termsText: v.termsText?.trim() || undefined,
      renewalReminderDate: v.renewalReminderDate || undefined,
    };

    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.rateContractService.update(this.contractId!, request)
      : this.rateContractService.create(request);

    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Rate contract updated successfully' : 'Rate contract created successfully');
        this.saving.set(false);
        void this.router.navigate(['/inventory/procurement/rate-contracts']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? (this.isEditMode() ? 'Failed to update rate contract' : 'Failed to create rate contract'));
        this.saving.set(false);
      },
    });
  }

  private loadContract(): void {
    if (!this.contractId) return;
    this.loading.set(true);
    this.rateContractService.getById(this.contractId).subscribe({
      next: (c) => {
        this.form.patchValue({
          supplierId: c.supplierId,
          startDate: c.startDate,
          endDate: c.endDate || '',
          contractValueCap: c.contractValueCap,
          termsText: c.termsText || '',
          renewalReminderDate: c.renewalReminderDate || '',
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load rate contract');
        void this.router.navigate(['/inventory/procurement/rate-contracts']);
      },
    });
  }
}

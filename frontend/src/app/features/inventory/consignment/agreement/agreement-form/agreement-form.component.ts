import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ConsignmentAgreementService } from '../agreement.service';
import { ConsignmentAgreementRequest } from '../agreement.model';
import { InventoryLocationService } from '../../../location/inventory-location.service';
import { InventoryLocation } from '../../../location/inventory-location.model';
import { SupplierService } from '../../../procurement/supplier/supplier.service';
import { Supplier } from '../../../procurement/supplier/supplier.model';
import { ToastService } from '../../../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../../../shared/utils/scroll-to-invalid';
import { cmsFieldError } from '../../../../../shared/validators/cms-validators';

@Component({
  selector: 'app-consignment-agreement-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './agreement-form.component.html',
  styleUrl: './agreement-form.component.scss',
})
export class ConsignmentAgreementFormComponent implements OnInit {
  private readonly fb                = inject(FormBuilder);
  private readonly route             = inject(ActivatedRoute);
  private readonly router            = inject(Router);
  private readonly agreementService  = inject(ConsignmentAgreementService);
  private readonly locationService   = inject(InventoryLocationService);
  private readonly supplierService   = inject(SupplierService);
  private readonly toast             = inject(ToastService);

  protected readonly loading    = signal(false);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly locations  = signal<InventoryLocation[]>([]);
  protected readonly suppliers  = signal<Supplier[]>([]);

  private agreementId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    supplierId:        [null as number | null, [Validators.required]],
    locationId:        [null as number | null, [Validators.required]],
    agreementNumber:   ['', [Validators.required, Validators.maxLength(100)]],
    startDate:         [new Date().toISOString().slice(0, 10), [Validators.required]],
    endDate:           [''],
    billingCycleDays:  [30, [Validators.min(1)]],
    notes:             ['', [Validators.maxLength(500)]],
  });

  ngOnInit(): void {
    this.locationService.getAll(true).subscribe({ next: (l) => this.locations.set(l) });
    this.supplierService.getAll(true).subscribe({ next: (s) => this.suppliers.set(s) });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.agreementId = Number(idParam);
      this.isEditMode.set(true);
      this.loadAgreement();
    }
  }

  protected getErrorMessage(fieldName: string): string {
    const labels: Record<string, string> = {
      supplierId: 'Supplier', locationId: 'Location', agreementNumber: 'Agreement number', startDate: 'Start date',
    };
    return cmsFieldError(this.form.get(fieldName), labels[fieldName] ?? fieldName);
  }

  private loadAgreement(): void {
    this.loading.set(true);
    this.agreementService.getById(this.agreementId!).subscribe({
      next: (a) => {
        this.form.patchValue({
          supplierId: a.supplierId,
          locationId: a.locationId,
          agreementNumber: a.agreementNumber,
          startDate: a.startDate,
          endDate: a.endDate ?? '',
          billingCycleDays: a.billingCycleDays,
          notes: a.notes ?? '',
        });
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load consignment agreement'); this.loading.set(false); },
    });
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

    const request: ConsignmentAgreementRequest = {
      supplierId: v.supplierId,
      locationId: v.locationId,
      agreementNumber: v.agreementNumber.trim(),
      startDate: v.startDate,
      endDate: v.endDate || undefined,
      billingCycleDays: v.billingCycleDays || undefined,
      notes: v.notes?.trim() || undefined,
    };

    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.agreementService.update(this.agreementId!, request)
      : this.agreementService.create(request);

    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Agreement updated' : 'Agreement created');
        this.saving.set(false);
        void this.router.navigate(['/inventory/consignment/agreements']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to save agreement');
        this.saving.set(false);
      },
    });
  }
}

import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ServiceContractService } from '../service-contract.service';
import { AssetServiceContractRequest } from '../service-contract.model';
import { AssetService } from '../../asset.service';
import { Asset } from '../../asset.model';
import { SupplierService } from '../../../procurement/supplier/supplier.service';
import { Supplier } from '../../../procurement/supplier/supplier.model';
import { ToastService } from '../../../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../../../shared/utils/scroll-to-invalid';
import { cmsFieldError } from '../../../../../shared/validators/cms-validators';

@Component({
  selector: 'app-service-contract-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './service-contract-form.component.html',
  styleUrl: './service-contract-form.component.scss',
})
export class ServiceContractFormComponent implements OnInit {
  private readonly fb              = inject(FormBuilder);
  private readonly route           = inject(ActivatedRoute);
  private readonly router          = inject(Router);
  private readonly contractService = inject(ServiceContractService);
  private readonly assetService    = inject(AssetService);
  private readonly supplierService = inject(SupplierService);
  private readonly toast           = inject(ToastService);

  protected readonly loading    = signal(false);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly assets     = signal<Asset[]>([]);
  protected readonly suppliers  = signal<Supplier[]>([]);

  private contractId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    assetId:              [null as number | null, [Validators.required]],
    supplierId:           [null as number | null, [Validators.required]],
    contractNumber:       ['', [Validators.maxLength(100)]],
    startDate:            ['', [Validators.required]],
    endDate:              [''],
    renewalReminderDate:  [''],
    coverageDetails:      ['', [Validators.maxLength(1000)]],
  });

  ngOnInit(): void {
    this.assetService.getPage({ page: 0, size: 200 }).subscribe({ next: (page) => this.assets.set(page.content) });
    this.supplierService.getAll(true).subscribe({ next: (s) => this.suppliers.set(s) });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.contractId = Number(idParam);
      this.isEditMode.set(true);
      this.loadContract();
    }
  }

  protected getErrorMessage(fieldName: string): string {
    const labels: Record<string, string> = { assetId: 'Asset', supplierId: 'Supplier', startDate: 'Start date' };
    return cmsFieldError(this.form.get(fieldName), labels[fieldName] ?? fieldName);
  }

  private loadContract(): void {
    this.loading.set(true);
    this.contractService.getById(this.contractId!).subscribe({
      next: (c) => {
        this.form.patchValue({
          assetId: c.assetId,
          supplierId: c.supplierId,
          contractNumber: c.contractNumber ?? '',
          startDate: c.startDate,
          endDate: c.endDate ?? '',
          renewalReminderDate: c.renewalReminderDate ?? '',
          coverageDetails: c.coverageDetails ?? '',
        });
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load service contract'); this.loading.set(false); },
    });
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }

    const v = this.form.value;
    const request: AssetServiceContractRequest = {
      assetId: v.assetId,
      supplierId: v.supplierId,
      contractNumber: v.contractNumber?.trim() || undefined,
      startDate: v.startDate,
      endDate: v.endDate || undefined,
      renewalReminderDate: v.renewalReminderDate || undefined,
      coverageDetails: v.coverageDetails?.trim() || undefined,
    };

    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.contractService.update(this.contractId!, request)
      : this.contractService.create(request);

    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Contract updated' : 'Service contract created');
        this.saving.set(false);
        void this.router.navigate(['/inventory/asset/service-contracts']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to save service contract');
        this.saving.set(false);
      },
    });
  }
}

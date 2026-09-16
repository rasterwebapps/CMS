import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { QuotationRequestService } from '../quotation-request.service';
import { QuotationRequestCreateRequest } from '../quotation-request.model';
import { InventoryLocationService } from '../../../location/inventory-location.service';
import { InventoryLocation } from '../../../location/inventory-location.model';
import { ToastService } from '../../../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../../../shared/utils/scroll-to-invalid';
import { cmsFieldError } from '../../../../../shared/validators/cms-validators';

@Component({
  selector: 'app-quotation-request-new',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './quotation-request-new.component.html',
  styleUrl: './quotation-request-new.component.scss',
})
export class QuotationRequestNewComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly requestService = inject(QuotationRequestService);
  private readonly locationService = inject(InventoryLocationService);
  private readonly toast = inject(ToastService);

  protected readonly saving = signal(false);
  protected readonly locations = signal<InventoryLocation[]>([]);

  protected readonly form: FormGroup = this.fb.group({
    locationId: [null as number | null, [Validators.required]],
    requestDate: [new Date().toISOString().slice(0, 10), [Validators.required]],
    notes: ['', [Validators.maxLength(500)]],
  });

  ngOnInit(): void {
    this.locationService.getAll(true).subscribe({ next: (l) => this.locations.set(l) });
  }

  protected getErrorMessage(fieldName: string): string {
    const labels: Record<string, string> = { locationId: 'Location', requestDate: 'Date' };
    return cmsFieldError(this.form.get(fieldName), labels[fieldName] ?? fieldName);
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }

    const v = this.form.value;
    const request: QuotationRequestCreateRequest = {
      locationId: v.locationId,
      requestDate: v.requestDate,
      notes: v.notes?.trim() || undefined,
    };

    this.saving.set(true);
    this.requestService.create(request).subscribe({
      next: (quotationRequest) => {
        this.toast.success('Quotation request started');
        this.saving.set(false);
        void this.router.navigate(['/inventory/procurement/quotation-requests', quotationRequest.id]);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to start quotation request');
        this.saving.set(false);
      },
    });
  }
}

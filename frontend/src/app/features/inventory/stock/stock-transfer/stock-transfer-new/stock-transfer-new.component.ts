import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { StockTransferService } from '../stock-transfer.service';
import { StockTransferCreateRequest } from '../stock-transfer.model';
import { InventoryLocationService } from '../../../location/inventory-location.service';
import { InventoryLocation } from '../../../location/inventory-location.model';
import { ToastService } from '../../../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../../../shared/utils/scroll-to-invalid';
import { cmsFieldError } from '../../../../../shared/validators/cms-validators';

@Component({
  selector: 'app-stock-transfer-new',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './stock-transfer-new.component.html',
  styleUrl: './stock-transfer-new.component.scss',
})
export class StockTransferNewComponent implements OnInit {
  private readonly fb              = inject(FormBuilder);
  private readonly router          = inject(Router);
  private readonly transferService = inject(StockTransferService);
  private readonly locationService = inject(InventoryLocationService);
  private readonly toast           = inject(ToastService);

  protected readonly saving    = signal(false);
  protected readonly locations = signal<InventoryLocation[]>([]);

  protected readonly form: FormGroup = this.fb.group({
    sourceLocationId:       [null as number | null, [Validators.required]],
    destinationLocationId:  [null as number | null, [Validators.required]],
    transferDate:           [new Date().toISOString().slice(0, 10), [Validators.required]],
    notes:                  ['', [Validators.maxLength(500)]],
  });

  ngOnInit(): void {
    this.locationService.getAll(true).subscribe({ next: (l) => this.locations.set(l) });
  }

  protected getErrorMessage(fieldName: string): string {
    const labels: Record<string, string> = { sourceLocationId: 'Source location', destinationLocationId: 'Destination location', transferDate: 'Transfer date' };
    return cmsFieldError(this.form.get(fieldName), labels[fieldName] ?? fieldName);
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }
    if (this.form.value.sourceLocationId === this.form.value.destinationLocationId) {
      this.toast.error('Source and destination locations must be different');
      return;
    }

    const v = this.form.value;
    const request: StockTransferCreateRequest = {
      sourceLocationId: v.sourceLocationId,
      destinationLocationId: v.destinationLocationId,
      transferDate: v.transferDate,
      notes: v.notes?.trim() || undefined,
    };

    this.saving.set(true);
    this.transferService.create(request).subscribe({
      next: (transfer) => {
        this.toast.success('Stock transfer started');
        this.saving.set(false);
        void this.router.navigate(['/inventory/stock/transfers', transfer.id]);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to start stock transfer');
        this.saving.set(false);
      },
    });
  }
}

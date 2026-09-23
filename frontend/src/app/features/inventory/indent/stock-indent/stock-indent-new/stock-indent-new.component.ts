import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { StockIndentService } from '../stock-indent.service';
import { StockIndentCreateRequest } from '../stock-indent.model';
import { InventoryLocationService } from '../../../location/inventory-location.service';
import { InventoryLocation } from '../../../location/inventory-location.model';
import { ToastService } from '../../../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../../../shared/utils/scroll-to-invalid';
import { cmsFieldError } from '../../../../../shared/validators/cms-validators';

@Component({
  selector: 'app-stock-indent-new',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './stock-indent-new.component.html',
  styleUrl: './stock-indent-new.component.scss',
})
export class StockIndentNewComponent implements OnInit {
  private readonly fb              = inject(FormBuilder);
  private readonly router          = inject(Router);
  private readonly requestService  = inject(StockIndentService);
  private readonly locationService = inject(InventoryLocationService);
  private readonly toast           = inject(ToastService);

  protected readonly saving    = signal(false);
  protected readonly locations = signal<InventoryLocation[]>([]);

  /** Only a REQUESTING_POINT/BOTH location can be the requester — a STORE never requests. */
  protected readonly requestingLocations = computed(() =>
    this.locations().filter((l) => l.locationRole === 'REQUESTING_POINT' || l.locationRole === 'BOTH'));
  /** Only a STORE/BOTH location can issue — a REQUESTING_POINT never issues to another location. */
  protected readonly issuingLocations = computed(() =>
    this.locations().filter((l) => l.locationRole === 'STORE' || l.locationRole === 'BOTH'));

  protected readonly form: FormGroup = this.fb.group({
    requestingLocationId: [null as number | null, [Validators.required]],
    issuingLocationId:    [null as number | null, [Validators.required]],
    requestDate:          [new Date().toISOString().slice(0, 10), [Validators.required]],
    notes:                ['', [Validators.maxLength(500)]],
  });

  ngOnInit(): void {
    this.locationService.getAll(true).subscribe({ next: (l) => this.locations.set(l) });
  }

  protected getErrorMessage(fieldName: string): string {
    const labels: Record<string, string> = { requestingLocationId: 'Requesting location', issuingLocationId: 'Issuing location', requestDate: 'Date' };
    return cmsFieldError(this.form.get(fieldName), labels[fieldName] ?? fieldName);
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }
    if (this.form.value.requestingLocationId === this.form.value.issuingLocationId) {
      this.toast.error('Requesting and issuing locations must be different');
      return;
    }

    const v = this.form.value;
    const request: StockIndentCreateRequest = {
      requestingLocationId: v.requestingLocationId,
      issuingLocationId: v.issuingLocationId,
      requestDate: v.requestDate,
      notes: v.notes?.trim() || undefined,
    };

    this.saving.set(true);
    this.requestService.create(request).subscribe({
      next: (indent) => {
        this.toast.success('Stock indent started');
        this.saving.set(false);
        void this.router.navigate(['/inventory/indent/stock-indents', indent.id]);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to start stock indent');
        this.saving.set(false);
      },
    });
  }
}

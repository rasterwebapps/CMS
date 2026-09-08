import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CycleCountService } from '../cycle-count.service';
import { CycleCountCreateRequest, CycleCountScope } from '../cycle-count.model';
import { InventoryLocationService } from '../../location/inventory-location.service';
import { InventoryLocation } from '../../location/inventory-location.model';
import { ToastService } from '../../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../../shared/utils/scroll-to-invalid';
import { cmsFieldError } from '../../../../shared/validators/cms-validators';

@Component({
  selector: 'app-cycle-count-new',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './cycle-count-new.component.html',
  styleUrl: './cycle-count-new.component.scss',
})
export class CycleCountNewComponent implements OnInit {
  private readonly fb              = inject(FormBuilder);
  private readonly router          = inject(Router);
  private readonly cycleCountService = inject(CycleCountService);
  private readonly locationService = inject(InventoryLocationService);
  private readonly toast           = inject(ToastService);

  protected readonly saving    = signal(false);
  protected readonly locations = signal<InventoryLocation[]>([]);

  protected readonly scopes: { value: CycleCountScope; label: string; hint: string }[] = [
    { value: 'FULL_LOCATION', label: 'Full Location', hint: 'Auto-fills the count sheet with every product currently holding a balance at this location — products can still be added or removed before you start counting.' },
    { value: 'AD_HOC', label: 'Ad-hoc', hint: 'Starts with an empty sheet — add just the specific products you want to spot-check.' },
  ];

  protected readonly form: FormGroup = this.fb.group({
    locationId: [null as number | null, [Validators.required]],
    countDate:  [new Date().toISOString().slice(0, 10), [Validators.required]],
    scope:      ['FULL_LOCATION' as CycleCountScope, [Validators.required]],
    notes:      ['', [Validators.maxLength(500)]],
  });

  ngOnInit(): void {
    this.locationService.getAll(true).subscribe({ next: (l) => this.locations.set(l) });
  }

  protected getErrorMessage(fieldName: string): string {
    const labels: Record<string, string> = { locationId: 'Location', countDate: 'Count date' };
    return cmsFieldError(this.form.get(fieldName), labels[fieldName] ?? fieldName);
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }

    const v = this.form.value;
    const request: CycleCountCreateRequest = {
      locationId: v.locationId,
      countDate: v.countDate,
      scope: v.scope,
      notes: v.notes?.trim() || undefined,
    };

    this.saving.set(true);
    this.cycleCountService.create(request).subscribe({
      next: (count) => {
        this.toast.success('Cycle count started');
        this.saving.set(false);
        void this.router.navigate(['/inventory/stock/cycle-counts', count.id]);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to start cycle count');
        this.saving.set(false);
      },
    });
  }
}

import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { BudgetService } from '../budget.service';
import { BudgetRequest } from '../budget.model';
import { InventoryLocationService } from '../../location/inventory-location.service';
import { InventoryLocation } from '../../location/inventory-location.model';
import { ToastService } from '../../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../../shared/utils/scroll-to-invalid';
import { cmsFieldError } from '../../../../shared/validators/cms-validators';

@Component({
  selector: 'app-budget-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './budget-form.component.html',
  styleUrl: './budget-form.component.scss',
})
export class BudgetFormComponent implements OnInit {
  private readonly fb              = inject(FormBuilder);
  private readonly route           = inject(ActivatedRoute);
  private readonly router          = inject(Router);
  private readonly budgetService   = inject(BudgetService);
  private readonly locationService = inject(InventoryLocationService);
  private readonly toast           = inject(ToastService);

  protected readonly loading    = signal(false);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly locations  = signal<InventoryLocation[]>([]);

  private budgetId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    locationId:       [null as number | null, [Validators.required]],
    periodStartDate:  ['', [Validators.required]],
    periodEndDate:    ['', [Validators.required]],
    allocatedAmount:  [null as number | null, [Validators.required, Validators.min(0)]],
    notes:            ['', [Validators.maxLength(500)]],
  });

  ngOnInit(): void {
    this.locationService.getAll(true).subscribe({ next: (l) => this.locations.set(l) });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.budgetId = Number(idParam);
      this.isEditMode.set(true);
      this.loadBudget();
    }
  }

  protected getErrorMessage(fieldName: string): string {
    const labels: Record<string, string> = { locationId: 'Location', periodStartDate: 'Period start', periodEndDate: 'Period end', allocatedAmount: 'Allocated amount' };
    return cmsFieldError(this.form.get(fieldName), labels[fieldName] ?? fieldName);
  }

  private loadBudget(): void {
    this.loading.set(true);
    this.budgetService.getById(this.budgetId!).subscribe({
      next: (b) => {
        this.form.patchValue({
          locationId: b.locationId,
          periodStartDate: b.periodStartDate,
          periodEndDate: b.periodEndDate,
          allocatedAmount: b.allocatedAmount,
          notes: b.notes ?? '',
        });
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load budget'); this.loading.set(false); },
    });
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }
    if (this.form.value.periodEndDate < this.form.value.periodStartDate) {
      this.toast.error('Period end date cannot be before the start date');
      return;
    }

    const v = this.form.value;
    const request: BudgetRequest = {
      locationId: v.locationId,
      periodStartDate: v.periodStartDate,
      periodEndDate: v.periodEndDate,
      allocatedAmount: v.allocatedAmount,
      notes: v.notes?.trim() || undefined,
    };

    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.budgetService.update(this.budgetId!, request)
      : this.budgetService.create(request);

    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Budget updated' : 'Budget created');
        this.saving.set(false);
        void this.router.navigate(['/inventory/budget/budgets']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to save budget');
        this.saving.set(false);
      },
    });
  }
}

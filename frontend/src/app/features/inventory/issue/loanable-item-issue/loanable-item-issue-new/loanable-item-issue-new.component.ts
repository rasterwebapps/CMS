import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { LoanableItemIssueService } from '../loanable-item-issue.service';
import { LoanableItemIssueCreateRequest } from '../loanable-item-issue.model';
import { InventoryLocationService } from '../../../location/inventory-location.service';
import { InventoryLocation } from '../../../location/inventory-location.model';
import { CmsProductPickerComponent } from '../../../../../shared/product-picker/product-picker.component';
import { ToastService } from '../../../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../../../shared/utils/scroll-to-invalid';
import { cmsFieldError } from '../../../../../shared/validators/cms-validators';

@Component({
  selector: 'app-loanable-item-issue-new',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    CmsProductPickerComponent,
  ],
  templateUrl: './loanable-item-issue-new.component.html',
  styleUrl: './loanable-item-issue-new.component.scss',
})
export class LoanableItemIssueNewComponent implements OnInit {
  private readonly fb              = inject(FormBuilder);
  private readonly router          = inject(Router);
  private readonly issueService    = inject(LoanableItemIssueService);
  private readonly locationService = inject(InventoryLocationService);
  private readonly toast           = inject(ToastService);

  protected readonly saving    = signal(false);
  protected readonly locations = signal<InventoryLocation[]>([]);

  protected readonly form: FormGroup = this.fb.group({
    productId:           [null as number | null, [Validators.required]],
    locationId:           [null as number | null, [Validators.required]],
    borrowerName:         ['', [Validators.required, Validators.maxLength(200)]],
    borrowerContact:      ['', [Validators.maxLength(100)]],
    issueDate:            [new Date().toISOString().slice(0, 10), [Validators.required]],
    expectedReturnDate:   ['', [Validators.required]],
    conditionOnIssue:     ['', [Validators.maxLength(500)]],
    notes:                ['', [Validators.maxLength(500)]],
  });

  ngOnInit(): void {
    this.locationService.getAll(true).subscribe({ next: (l) => this.locations.set(l) });
  }

  protected getErrorMessage(fieldName: string): string {
    const labels: Record<string, string> = {
      productId: 'Product', locationId: 'Location', borrowerName: 'Borrower name', expectedReturnDate: 'Expected return date',
    };
    return cmsFieldError(this.form.get(fieldName), labels[fieldName] ?? fieldName);
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }

    const v = this.form.value;
    const request: LoanableItemIssueCreateRequest = {
      productId: v.productId,
      locationId: v.locationId,
      borrowerName: v.borrowerName.trim(),
      borrowerContact: v.borrowerContact?.trim() || undefined,
      issueDate: v.issueDate,
      expectedReturnDate: v.expectedReturnDate,
      conditionOnIssue: v.conditionOnIssue?.trim() || undefined,
      notes: v.notes?.trim() || undefined,
    };

    this.saving.set(true);
    this.issueService.create(request).subscribe({
      next: (issue) => {
        this.toast.success('Item issued');
        this.saving.set(false);
        void this.router.navigate(['/inventory/issue/loanable-item-issues', issue.id]);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to issue item');
        this.saving.set(false);
      },
    });
  }
}

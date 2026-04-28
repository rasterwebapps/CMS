import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FeeDemand, PaymentMode, TermFeePaymentRequest } from '../academic-year.model';

@Component({
  selector: 'app-fee-payment-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatButtonModule, MatIconModule, DatePipe, DecimalPipe],
  template: `
    <h2 mat-dialog-title>Record Fee Payment</h2>
    <mat-dialog-content>
      <div class="demand-info">
        <div class="demand-info__row">
          <span class="demand-info__label">Student</span>
          <span class="demand-info__value">{{ data.demand.studentName }}</span>
        </div>
        <div class="demand-info__row">
          <span class="demand-info__label">Total Amount</span>
          <span class="demand-info__value">₹{{ data.demand.totalAmount | number:'1.2-2' }}</span>
        </div>
        <div class="demand-info__row">
          <span class="demand-info__label">Paid Amount</span>
          <span class="demand-info__value">₹{{ data.demand.paidAmount | number:'1.2-2' }}</span>
        </div>
        <div class="demand-info__row">
          <span class="demand-info__label">Outstanding</span>
          <span class="demand-info__value outstanding">₹{{ data.demand.outstandingAmount | number:'1.2-2' }}</span>
        </div>
        <div class="demand-info__row">
          <span class="demand-info__label">Due Date</span>
          <span class="demand-info__value">{{ data.demand.dueDate | date:'mediumDate' }}</span>
        </div>
      </div>

      <form [formGroup]="form" class="payment-form">
        <div class="field-group">
          <label class="field-label">Amount Paid <span class="required-star">*</span></label>
          <input type="number" class="field-input" formControlName="amountPaid"
            min="0.01" step="0.01" placeholder="Enter amount" />
          @if (form.get('amountPaid')?.invalid && form.get('amountPaid')?.touched) {
            <span class="field-error">Valid amount required</span>
          }
        </div>

        <div class="field-group">
          <label class="field-label">Payment Date <span class="required-star">*</span></label>
          <input type="date" class="field-input" formControlName="paymentDate" />
          @if (form.get('paymentDate')?.invalid && form.get('paymentDate')?.touched) {
            <span class="field-error">Payment date is required</span>
          }
        </div>

        <div class="field-group">
          <label class="field-label">Payment Mode <span class="required-star">*</span></label>
          <select class="field-input" formControlName="paymentMode">
            <option value="">Select mode</option>
            @for (mode of paymentModes; track mode) {
              <option [value]="mode">{{ mode.replace('_', ' ') }}</option>
            }
          </select>
          @if (form.get('paymentMode')?.invalid && form.get('paymentMode')?.touched) {
            <span class="field-error">Payment mode is required</span>
          }
        </div>

        <div class="field-group">
          <label class="field-label">Remarks</label>
          <input type="text" class="field-input" formControlName="remarks"
            placeholder="Optional remarks" maxlength="500" />
        </div>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button mat-dialog-close>Cancel</button>
      <button mat-flat-button color="primary" (click)="submit()" [disabled]="form.invalid">
        Record Payment
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .demand-info {
      background: var(--cms-bg-surface-2, #f5f5f5);
      border-radius: 8px;
      padding: 12px 16px;
      margin-bottom: 16px;
    }
    .demand-info__row {
      display: flex;
      justify-content: space-between;
      padding: 4px 0;
      font-size: 14px;
    }
    .demand-info__label {
      color: var(--cms-line, #666);
    }
    .demand-info__value {
      font-weight: 500;
    }
    .outstanding {
      color: #d32f2f;
      font-weight: 600;
    }
    .payment-form {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    .field-group {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    .field-label {
      font-size: 13px;
      font-weight: 500;
    }
    .field-input {
      padding: 8px 12px;
      border: 1px solid #ccc;
      border-radius: 4px;
      font-size: 14px;
      width: 100%;
      box-sizing: border-box;
    }
    .field-error {
      font-size: 12px;
      color: #d32f2f;
    }
    .required-star { color: #d32f2f; }
  `],
})
export class FeePaymentDialogComponent {
  readonly data = inject<{ demand: FeeDemand }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<FeePaymentDialogComponent>);
  private readonly fb = inject(FormBuilder);

  readonly paymentModes: PaymentMode[] = [
    'CASH', 'CARD', 'UPI', 'NET_BANKING', 'BANK_TRANSFER', 'CHEQUE', 'DEMAND_DRAFT', 'SCHOLARSHIP',
  ];

  readonly form: FormGroup = this.fb.group({
    amountPaid: [null, [Validators.required, Validators.min(0.01)]],
    paymentDate: [new Date().toISOString().split('T')[0], Validators.required],
    paymentMode: ['', Validators.required],
    remarks: [''],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.value;
    const request: TermFeePaymentRequest = {
      feeDemandId: this.data.demand.id,
      paymentDate: v.paymentDate,
      amountPaid: Number(v.amountPaid),
      paymentMode: v.paymentMode as PaymentMode,
      remarks: v.remarks || undefined,
    };
    this.dialogRef.close(request);
  }
}

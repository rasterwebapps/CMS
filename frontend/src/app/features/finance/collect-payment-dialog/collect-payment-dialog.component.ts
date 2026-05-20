import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { FinanceService } from '../finance.service';
import { CollectPaymentRequest, CollectPaymentResponse } from '../finance.model';
import { ToastService } from '../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';
import { getPaymentModeLabel, PAYMENT_MODES } from '../../../shared/utils/payment-mode.utils';
import { PaymentModeLabelPipe } from '../../../shared/pipes/payment-mode-label.pipe';
import { CashDenominationComponent } from '../../../shared/cash-denomination/cash-denomination.component';
import { printFeeReceipt } from '../../../shared/utils/print-receipt.utils';

@Component({
  selector: 'app-collect-payment-dialog',
  standalone: true,
  imports: [
    InrPipe,
    PaymentModeLabelPipe,
    ReactiveFormsModule, MatDialogModule,
    MatButtonModule, MatProgressSpinnerModule, MatIconModule,
    CashDenominationComponent,
  ],
  templateUrl: './collect-payment-dialog.component.html',
  styleUrl: './collect-payment-dialog.component.scss',
})
export class CollectPaymentDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<CollectPaymentDialogComponent>);
  private readonly data: { studentId: number } = inject(MAT_DIALOG_DATA);
  private readonly financeService = inject(FinanceService);
  private readonly toast = inject(ToastService);

  protected saving = false;
  protected result: CollectPaymentResponse | null = null;
  protected denominationValid = false;

  protected readonly paymentModes = PAYMENT_MODES;
  protected readonly getPaymentModeLabel = getPaymentModeLabel;

  protected readonly form: FormGroup = this.fb.group({
    amount:               [null, [Validators.required, Validators.min(1)]],
    paymentDate:          ['',   Validators.required],
    paymentMode:          ['',   Validators.required],
    transactionReference: [''],
    remarks:              [''],
  });

  protected isCashMode(): boolean {
    return this.form.get('paymentMode')?.value === 'CASH';
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }
    if (this.isCashMode() && !this.denominationValid) return;
    const v = this.form.value;
    const request: CollectPaymentRequest = {
      amount:               v.amount,
      paymentDate:          v.paymentDate,
      paymentMode:          v.paymentMode,
      transactionReference: v.transactionReference?.trim() || undefined,
      remarks:              v.remarks?.trim() || undefined,
    };

    this.saving = true;
    this.financeService.collectPayment(this.data.studentId, request).subscribe({
      next: (r) => {
        this.saving = false;
        this.result = r;
      },
      error: () => {
        this.toast.error('Failed to collect payment');
        this.saving = false;
      },
    });
  }

  protected onDone(): void {
    this.dialogRef.close(this.result);
  }

  protected onCancel(): void {
    this.dialogRef.close();
  }

  protected printReceipt(): void {
    const r = this.result!;
    printFeeReceipt({
      receiptNumber: r.receiptNumber,
      payerName: r.studentName,
      payerIdentifier: r.rollNumber,
      amountPaid: r.amountPaid,
      paymentDate: r.paymentDate,
      paymentMode: r.paymentMode,
      transactionReference: r.transactionReference,
      feeCategory: r.feeCategory,
      installmentBreakdown: r.installmentBreakdown.map(s => ({
        installmentLabel: s.installmentLabel,
        amountApplied: s.amountApplied,
      })),
    });
  }

  protected isTransactionRefRequired(): boolean {
    const mode = this.form.get('paymentMode')?.value;
    return ['UPI', 'BANK_TRANSFER', 'CHEQUE', 'DEMAND_DRAFT'].includes(mode);
  }
}

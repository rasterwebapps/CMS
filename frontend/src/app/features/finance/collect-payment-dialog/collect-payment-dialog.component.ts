import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { FinanceService } from '../finance.service';
import { CollectPaymentRequest } from '../finance.model';

@Component({
  selector: 'app-collect-payment-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule, MatDialogModule,
    MatButtonModule, MatProgressSpinnerModule, MatSnackBarModule,
  ],
  templateUrl: './collect-payment-dialog.component.html',
  styleUrl: './collect-payment-dialog.component.scss',
})
export class CollectPaymentDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<CollectPaymentDialogComponent>);
  private readonly data: { studentId: number } = inject(MAT_DIALOG_DATA);
  private readonly financeService = inject(FinanceService);
  private readonly snackBar = inject(MatSnackBar);

  protected saving = false;

  protected readonly paymentModes = ['CASH', 'UPI', 'BANK_TRANSFER', 'CHEQUE', 'CARD'];

  protected readonly form: FormGroup = this.fb.group({
    amount: [null, [Validators.required, Validators.min(1)]],
    paymentDate: ['', Validators.required],
    paymentMode: ['', Validators.required],
    transactionReference: [''],
    remarks: [''],
  });

  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.value;
    const request: CollectPaymentRequest = {
      amount: v.amount,
      paymentDate: v.paymentDate,
      paymentMode: v.paymentMode,
      transactionReference: v.transactionReference?.trim() || undefined,
      remarks: v.remarks?.trim() || undefined,
    };

    this.saving = true;
    this.financeService.collectPayment(this.data.studentId, request).subscribe({
      next: (result) => this.dialogRef.close(result),
      error: () => {
        this.snackBar.open('Failed to collect payment', 'Close', { duration: 3000 });
        this.saving = false;
      },
    });
  }

  protected onCancel(): void {
    this.dialogRef.close();
  }
}

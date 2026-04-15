import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, FormArray, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { DecimalPipe } from '@angular/common';
import { FinanceService } from '../finance.service';
import { StudentFeeAllocationRequest } from '../finance.model';

@Component({
  selector: 'app-fee-finalization',
  standalone: true,
  imports: [
    DecimalPipe, RouterLink, ReactiveFormsModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatCardModule, MatProgressSpinnerModule, MatSnackBarModule,
  ],
  templateUrl: './fee-finalization.component.html',
  styleUrl: './fee-finalization.component.scss',
})
export class FeeFinalizationComponent {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly financeService = inject(FinanceService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly saving = signal(false);

  protected readonly form: FormGroup = this.fb.group({
    studentId: [null, [Validators.required, Validators.min(1)]],
    totalFee: [null, [Validators.required, Validators.min(0)]],
    discountAmount: [0],
    discountReason: [''],
    agentCommission: [0],
    yearFees: this.fb.array([]),
  });

  get yearFees(): FormArray {
    return this.form.get('yearFees') as FormArray;
  }

  get netFee(): number {
    const total = this.form.get('totalFee')?.value || 0;
    const discount = this.form.get('discountAmount')?.value || 0;
    return total - discount;
  }

  protected addYear(): void {
    this.yearFees.push(this.fb.group({
      yearNumber: [this.yearFees.length + 1, [Validators.required, Validators.min(1)]],
      amount: [null, [Validators.required, Validators.min(0)]],
      dueDate: ['', Validators.required],
    }));
  }

  protected removeYear(index: number): void {
    this.yearFees.removeAt(index);
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.yearFees.length === 0) {
      this.snackBar.open('Add at least one year fee entry', 'Close', { duration: 3000 });
      return;
    }

    const v = this.form.value;
    const request: StudentFeeAllocationRequest = {
      studentId: v.studentId,
      totalFee: v.totalFee,
      discountAmount: v.discountAmount || undefined,
      discountReason: v.discountReason?.trim() || undefined,
      agentCommission: v.agentCommission || undefined,
      yearFees: v.yearFees,
    };

    this.saving.set(true);
    this.financeService.finalizeStudentFee(request).subscribe({
      next: (result) => {
        this.snackBar.open('Fee finalized successfully', 'Close', { duration: 3000 });
        void this.router.navigate(['/student-fees', result.studentId]);
      },
      error: () => {
        this.snackBar.open('Failed to finalize fee', 'Close', { duration: 3000 });
        this.saving.set(false);
      },
    });
  }
}

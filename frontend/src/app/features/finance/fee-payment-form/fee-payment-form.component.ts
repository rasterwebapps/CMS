import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { FinanceService } from '../finance.service';
import { FeePaymentRequest } from '../finance.model';
import { environment } from '../../../../environments/environment';
import { LayoutService } from '../../../core/layout/layout.service';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { ToastService } from '../../../core/toast/toast.service';

@Component({
  selector: 'app-fee-payment-form',
  standalone: true,
  imports: [
    RouterLink, ReactiveFormsModule, MatProgressSpinnerModule, PageHeaderComponent],
  templateUrl: './fee-payment-form.component.html',
  styleUrl: './fee-payment-form.component.scss',
})
export class FeePaymentFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly financeService = inject(FinanceService);
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);
  protected readonly layoutService = inject(LayoutService);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly pageTitle = signal('Record Payment');
  protected readonly students = signal<{ id: number; name: string }[]>([]);
  protected readonly feeStructures = signal<{ id: number; name: string }[]>([]);

  protected readonly paymentMethods = ['CASH', 'CARD', 'ONLINE', 'CHEQUE'];
  protected readonly statuses = ['PENDING', 'COMPLETED', 'FAILED', 'REFUNDED'];

  protected readonly form: FormGroup = this.fb.group({
    studentId: [null, Validators.required],
    feeStructureId: [null, Validators.required],
    amountPaid: [null, [Validators.required, Validators.min(0)]],
    paymentDate: ['', Validators.required],
    paymentMethod: ['', Validators.required],
    transactionId: [''],
    status: ['COMPLETED', Validators.required],
  });

  ngOnInit(): void {
    this.http.get<{ id: number; name: string }[]>(`${environment.apiUrl}/students`).subscribe({
      next: (data) => this.students.set(data),
      error: () => this.toast.error('Failed to load students'),
    });
    this.http.get<{ id: number; name: string }[]>(`${environment.apiUrl}/fee-structures`).subscribe({
      next: (data) => this.feeStructures.set(data),
      error: () => this.toast.error('Failed to load fee structures'),
    });
  }

  protected onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.value;
    const request: FeePaymentRequest = { studentId: v.studentId, feeStructureId: v.feeStructureId, amountPaid: v.amountPaid, paymentDate: v.paymentDate, paymentMethod: v.paymentMethod, transactionId: v.transactionId || undefined, status: v.status };
    this.saving.set(true);
    this.financeService.createPayment(request).subscribe({
      next: () => { this.toast.success('Payment recorded'); void this.router.navigate(['/fee-payments']); },
      error: () => { this.toast.error('Failed to save'); this.saving.set(false); },
    });
  }
}

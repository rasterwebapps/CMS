import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CurrencyPipe } from '@angular/common';
import { EnquiryService } from '../../enquiry/enquiry.service';
import { Enquiry, EnquiryPaymentRequest } from '../../enquiry/enquiry.model';

interface YearFee {
  yearNumber: number;
  amount: number;
}

@Component({
  selector: 'app-enquiry-payment-collection',
  standalone: true,
  imports: [
    CurrencyPipe,
    RouterLink,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTableModule,
    MatTooltipModule,
  ],
  templateUrl: './enquiry-payment-collection.component.html',
  styleUrl: './enquiry-payment-collection.component.scss',
})
export class EnquiryPaymentCollectionComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly enquiryService = inject(EnquiryService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly selectedEnquiry = signal<Enquiry | null>(null);
  protected readonly yearFees = signal<YearFee[]>([]);

  protected readonly paymentModes = ['CASH', 'UPI', 'BANK_TRANSFER', 'CHEQUE', 'CARD'];

  protected readonly displayedColumns = [
    'name',
    'programName',
    'courseName',
    'finalizedNetFee',
    'finalizedAt',
    'actions',
  ];
  protected readonly dataSource = new MatTableDataSource<Enquiry>([]);

  protected readonly form: FormGroup = this.fb.group({
    amount: [null, [Validators.required, Validators.min(1)]],
    paymentDate: ['', Validators.required],
    paymentMode: ['', Validators.required],
    transactionReference: [''],
    remarks: [''],
  });

  ngOnInit(): void {
    this.loadFinalizedEnquiries();
  }

  private loadFinalizedEnquiries(): void {
    this.loading.set(true);
    this.enquiryService.getEnquiries().subscribe({
      next: (data) => {
        this.dataSource.data = data.filter(
          (e) => e.status === 'FEES_FINALIZED' || e.status === 'PARTIALLY_PAID',
        );
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Failed to load enquiries', 'Close', { duration: 3000 });
        this.loading.set(false);
      },
    });
  }

  protected selectEnquiry(enquiry: Enquiry): void {
    this.selectedEnquiry.set(enquiry);

    // Pre-fill payment amount with net fee
    this.form.patchValue({
      amount: enquiry.finalizedNetFee ?? enquiry.finalCalculatedFee ?? 0,
      paymentDate: new Date().toISOString().split('T')[0],
    });

    // Parse year-wise fees
    if (enquiry.yearWiseFees) {
      try {
        const parsed = JSON.parse(enquiry.yearWiseFees) as YearFee[];
        this.yearFees.set(parsed);
      } catch {
        this.yearFees.set([]);
      }
    } else {
      this.yearFees.set([]);
    }
  }

  protected backToList(): void {
    this.selectedEnquiry.set(null);
    this.yearFees.set([]);
    this.form.reset();
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const enquiry = this.selectedEnquiry();
    if (!enquiry) return;

    const v = this.form.value;
    const paymentRequest: EnquiryPaymentRequest = {
      amountPaid: v.amount,
      paymentDate: v.paymentDate,
      paymentMode: v.paymentMode,
      transactionReference: v.transactionReference || undefined,
      remarks: v.remarks || undefined,
    };

    this.saving.set(true);
    this.enquiryService.collectPayment(enquiry.id, paymentRequest).subscribe({
      next: (response) => {
        this.snackBar.open(
          `Payment collected — Receipt: ${response.receiptNumber}`,
          'Close',
          { duration: 5000 },
        );
        this.selectedEnquiry.set(null);
        this.loadFinalizedEnquiries();
        this.saving.set(false);
      },
      error: () => {
        this.snackBar.open('Failed to collect payment', 'Close', { duration: 3000 });
        this.saving.set(false);
      },
    });
  }
}

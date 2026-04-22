import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { EnquiryService } from '../../enquiry/enquiry.service';
import { Enquiry, EnquiryPaymentRequest, EnquiryPaymentResponse, EnquiryYearWiseFeeStatusResponse } from '../../enquiry/enquiry.model';
import { LayoutService } from '../../../core/layout/layout.service';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';

@Component({
  selector: 'app-enquiry-payment-collection',
  standalone: true,
  imports: [
    CurrencyPipe,
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTableModule,
    MatTooltipModule,
    PageHeaderComponent,
  ],
  templateUrl: './enquiry-payment-collection.component.html',
  styleUrl: './enquiry-payment-collection.component.scss',
})
export class EnquiryPaymentCollectionComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly enquiryService = inject(EnquiryService);
  private readonly snackBar = inject(MatSnackBar);
  protected readonly layoutService = inject(LayoutService);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly selectedEnquiry = signal<Enquiry | null>(null);
  protected readonly yearWiseFeeStatus = signal<EnquiryYearWiseFeeStatusResponse | null>(null);
  protected readonly lastPaymentResponse = signal<EnquiryPaymentResponse | null>(null);

  protected readonly paymentModes = ['CASH', 'UPI', 'BANK_TRANSFER', 'CHEQUE', 'CARD', 'NET_BANKING', 'DEMAND_DRAFT'];

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
    const enquiryId = this.route.snapshot.queryParamMap.get('enquiryId');
    if (enquiryId) {
      this.loading.set(true);
      this.enquiryService.getEnquiryById(Number(enquiryId)).subscribe({
        next: (enquiry) => {
          this.dataSource.data = [enquiry];
          this.selectEnquiry(enquiry);
          this.loading.set(false);
        },
        error: () => {
          this.snackBar.open('Failed to load enquiry', 'Close', { duration: 3000 });
          this.loadFinalizedEnquiries();
        },
      });
    } else {
      this.loadFinalizedEnquiries();
    }
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
    this.yearWiseFeeStatus.set(null);

    this.form.patchValue({
      paymentDate: new Date().toISOString().split('T')[0],
    });

    this.enquiryService.getYearWiseFeeStatus(enquiry.id).subscribe({
      next: (status) => {
        this.yearWiseFeeStatus.set(status);
        // Pre-fill with outstanding amount
        if (!this.form.get('amount')?.value) {
          this.form.patchValue({ amount: status.totalOutstanding > 0 ? status.totalOutstanding : null });
        }
      },
      error: () => {
        // Fall back to net fee if status endpoint fails
        this.form.patchValue({
          amount: enquiry.finalizedNetFee ?? enquiry.finalCalculatedFee ?? 0,
        });
      },
    });
  }

  protected backToList(): void {
    this.selectedEnquiry.set(null);
    this.yearWiseFeeStatus.set(null);
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
        this.saving.set(false);
        this.lastPaymentResponse.set(response);
      },
      error: () => {
        this.snackBar.open('Failed to collect payment', 'Close', { duration: 3000 });
        this.saving.set(false);
      },
    });
  }

  protected doneWithReceipt(): void {
    void this.router.navigate(['/enquiries']);
  }
}

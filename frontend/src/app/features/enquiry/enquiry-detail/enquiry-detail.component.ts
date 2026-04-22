import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { EnquiryService } from '../enquiry.service';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import {
  Enquiry,
  EnquiryDocument,
  EnquiryPaymentResponse,
  EnquiryStatusHistoryResponse,
} from '../enquiry.model';

@Component({
  selector: 'app-enquiry-detail',
  standalone: true,
  imports: [
    RouterLink,
    MatTabsModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    CurrencyPipe,
    DatePipe,
    PageHeaderComponent,
    CmsStatusBadgeComponent,
  ],
  templateUrl: './enquiry-detail.component.html',
  styleUrl: './enquiry-detail.component.scss',
})
export class EnquiryDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly enquiryService = inject(EnquiryService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly enquiry = signal<Enquiry | null>(null);
  protected readonly documents = signal<EnquiryDocument[]>([]);
  protected readonly payments = signal<EnquiryPaymentResponse[]>([]);
  protected readonly statusHistory = signal<EnquiryStatusHistoryResponse[]>([]);
  protected readonly loading = signal(true);
  protected readonly submitting = signal(false);

  protected readonly paymentColumns = ['receiptNumber', 'amountPaid', 'paymentDate', 'paymentMode', 'collectedBy'];
  protected readonly historyColumns = ['changedAt', 'fromStatus', 'toStatus', 'changedBy', 'remarks'];
  protected readonly docColumns = ['documentType', 'status', 'verifiedBy', 'verifiedAt'];

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) this.load(id);
  }

  private load(id: number): void {
    this.loading.set(true);
    this.enquiryService.getEnquiryById(id).subscribe({
      next: (e) => {
        this.enquiry.set(e);
        this.loading.set(false);
        this.enquiryService.getDocuments(id).subscribe({ next: (d) => this.documents.set(d) });
        this.enquiryService.getPayments(id).subscribe({ next: (p) => this.payments.set(p) });
        this.enquiryService.getStatusHistory(id).subscribe({ next: (h) => this.statusHistory.set(h) });
      },
      error: () => {
        this.snackBar.open('Failed to load enquiry', 'Close', { duration: 3000 });
        void this.router.navigate(['/enquiries']);
      },
    });
  }

  protected canSubmitDocuments(): boolean {
    const s = this.enquiry()?.status;
    return s === 'FEES_PAID' || s === 'PARTIALLY_PAID';
  }

  protected canConvert(): boolean {
    return this.enquiry()?.status === 'DOCUMENTS_SUBMITTED';
  }

  protected submitDocuments(): void {
    const id = this.enquiry()?.id;
    if (!id) return;
    this.submitting.set(true);
    this.enquiryService.submitDocuments(id).subscribe({
      next: () => {
        this.snackBar.open('Documents submitted successfully', 'Close', { duration: 3000 });
        this.load(id);
        this.submitting.set(false);
      },
      error: () => {
        this.snackBar.open('Failed to submit documents', 'Close', { duration: 3000 });
        this.submitting.set(false);
      },
    });
  }

  protected navigateToConvert(): void {
    const id = this.enquiry()?.id;
    if (id) void this.router.navigate(['/enquiries', id, 'convert']);
  }
}

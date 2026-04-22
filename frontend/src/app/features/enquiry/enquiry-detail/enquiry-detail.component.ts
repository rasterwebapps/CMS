import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { EnquiryService } from '../enquiry.service';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { CmsSkeletonComponent } from '../../../shared/skeleton/skeleton.component';
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
    MatTooltipModule,
    MatSnackBarModule,
    CurrencyPipe,
    DatePipe,
    PageHeaderComponent,
    CmsStatusBadgeComponent,
    CmsSkeletonComponent,
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

  protected readonly historyColumns = [
    'changedAt',
    'fromStatus',
    'toStatus',
    'changedBy',
    'remarks',
  ];

  // Quick-stat computed signals (Phase 3 §2.3 / §2.8)
  protected readonly daysActive = computed(() => {
    const e = this.enquiry();
    if (!e?.enquiryDate) return 0;
    const ms = Date.now() - Date.parse(e.enquiryDate);
    return Math.max(0, Math.floor(ms / 86_400_000));
  });
  protected readonly totalPaid = computed(() =>
    this.payments().reduce((sum, p) => sum + (p.amountPaid ?? 0), 0),
  );
  protected readonly docsVerified = computed(
    () => this.documents().filter((d) => d.status === 'VERIFIED').length,
  );
  protected readonly docsTotal = computed(() => this.documents().length);

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
        this.enquiryService
          .getStatusHistory(id)
          .subscribe({ next: (h) => this.statusHistory.set(h) });
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

  /** Maps payment mode → receipt-card border-left modifier class. */
  protected receiptModeClass(mode: string | null | undefined): string {
    switch ((mode || '').toUpperCase()) {
      case 'CASH':
        return 'receipt-card--cash';
      case 'CHEQUE':
      case 'DD':
        return 'receipt-card--cheque';
      case 'ONLINE':
      case 'UPI':
      case 'CARD':
      case 'NEFT':
      case 'RTGS':
      default:
        return 'receipt-card--online';
    }
  }

  /** Maps document status → doc-row icon modifier class. */
  protected docIconClass(status: string | null | undefined): string {
    switch ((status || '').toUpperCase()) {
      case 'VERIFIED':
        return 'doc-row__icon--verified';
      case 'REJECTED':
        return 'doc-row__icon--rejected';
      default:
        return 'doc-row__icon--pending';
    }
  }

  /** Opens the stored document binary in a new tab for inline viewing. */
  protected viewDocumentFile(d: EnquiryDocument): void {
    if (!d.hasFile) return;
    this.enquiryService.downloadDocumentFile(d.enquiryId, d.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const opened = window.open(url, '_blank');
        if (!opened) {
          this.triggerDownload(blob, d.fileName ?? d.documentType);
        }
        setTimeout(() => URL.revokeObjectURL(url), 10_000);
      },
      error: () => this.snackBar.open('Failed to load document', 'Close', { duration: 3000 }),
    });
  }

  /** Downloads the stored document binary to the user's device. */
  protected downloadDocumentFile(d: EnquiryDocument): void {
    if (!d.hasFile) return;
    this.enquiryService.downloadDocumentFile(d.enquiryId, d.id).subscribe({
      next: (blob) => this.triggerDownload(blob, d.fileName ?? d.documentType),
      error: () => this.snackBar.open('Failed to download document', 'Close', { duration: 3000 }),
    });
  }

  private triggerDownload(blob: Blob, fileName: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }
}

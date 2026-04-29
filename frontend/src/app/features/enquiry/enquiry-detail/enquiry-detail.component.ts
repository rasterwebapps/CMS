import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { EnquiryService } from '../enquiry.service';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import {
  Enquiry,
  EnquiryDocument,
  EnquiryPaymentResponse,
  EnquiryStatusHistoryResponse,
} from '../enquiry.model';
import { ToastService } from '../../../core/toast/toast.service';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { STATUS_LABELS } from '../enquiry-list/enquiry-list.component';
import { computeInitials } from '../../../shared/utils/initials';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { ENQUIRY_DETAIL_TOUR } from '../../../shared/tour/tours/enquiry.tours';

@Component({
  selector: 'app-enquiry-detail',
  standalone: true,
  imports: [
    AppDatePipe, InrPipe, RouterLink,
    MatTableModule, MatTooltipModule, MatProgressSpinnerModule,
    CmsEmptyStateComponent,
    CmsTourButtonComponent,
  ],
  templateUrl: './enquiry-detail.component.html',
  styleUrl: './enquiry-detail.component.scss',
})
export class EnquiryDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly enquiryService = inject(EnquiryService);
  private readonly toast = inject(ToastService);
  private readonly tourService = inject(TourService);

  protected readonly enquiry = signal<Enquiry | null>(null);
  protected readonly documents = signal<EnquiryDocument[]>([]);
  protected readonly payments = signal<EnquiryPaymentResponse[]>([]);
  protected readonly statusHistory = signal<EnquiryStatusHistoryResponse[]>([]);
  protected readonly loading = signal(true);
  protected readonly submitting = signal(false);

  protected readonly historyColumns = ['changedAt', 'fromStatus', 'toStatus', 'changedBy', 'remarks'];
  protected readonly activeTab = signal<'overview' | 'documents' | 'payments' | 'history'>('overview');
  protected readonly computeInitials = computeInitials;
  protected readonly STATUS_LABELS   = STATUS_LABELS;
  protected readonly Math            = Math;

  protected readonly initials = computed(() => computeInitials(this.enquiry()?.name));

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
    this.tourService.register('enquiry-detail', ENQUIRY_DETAIL_TOUR);
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
        this.toast.error('Failed to load enquiry');
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
        this.toast.success('Documents submitted successfully');
        this.load(id);
        this.submitting.set(false);
      },
      error: () => {
        this.toast.error('Failed to submit documents');
        this.submitting.set(false);
      },
    });
  }

  protected statusLabel(s: string | null | undefined): string {
    return STATUS_LABELS[s ?? ''] ?? (s ?? '');
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
      error: () => this.toast.error('Failed to load document'),
    });
  }

  /** Downloads the stored document binary to the user's device. */
  protected downloadDocumentFile(d: EnquiryDocument): void {
    if (!d.hasFile) return;
    this.enquiryService.downloadDocumentFile(d.enquiryId, d.id).subscribe({
      next: (blob) => this.triggerDownload(blob, d.fileName ?? d.documentType),
      error: () => this.toast.error('Failed to download document'),
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

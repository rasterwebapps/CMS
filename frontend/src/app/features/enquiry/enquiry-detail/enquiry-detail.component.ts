import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { EnquiryService } from '../enquiry.service';
import { PermissionService } from '../../../core/permissions/permission.service';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { CmsSkeletonComponent } from '../../../shared/skeleton/skeleton.component';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import {
  Enquiry,
  EnquiryDocument,
  EnquiryPaymentResponse,
  EnquiryStatusHistoryResponse,
  EnquiryCreditApplication,
} from '../enquiry.model';
import { ToastService } from '../../../core/toast/toast.service';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { PaymentModeLabelPipe } from '../../../shared/pipes/payment-mode-label.pipe';
import { STATUS_LABELS } from '../enquiry-list/enquiry-list.component';
import { computeInitials } from '../../../shared/utils/initials';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { ENQUIRY_DETAIL_TOUR } from '../../../shared/tour/tours/enquiry.tours';
import { FeeReceiptDialogComponent } from '../../../shared/fee-receipt-dialog/fee-receipt-dialog.component';
import { ReceiptDisplayData } from '../../finance/finance.model';
import { printFeeReceipt, printRefundVoucher, downloadRefundVoucher, RefundVoucherData } from '../../../shared/utils/print-receipt.utils';

@Component({
  selector: 'app-enquiry-detail',
  standalone: true,
  imports: [
    AppDatePipe,
    PaymentModeLabelPipe,
    InrPipe,
    RouterLink,
    MatTooltipModule,
    MatTabsModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    CmsEmptyStateComponent,
    CmsStatusBadgeComponent,
    CmsSkeletonComponent,
    CmsTourButtonComponent,
    CmsRowActionButtonComponent,
    FeeReceiptDialogComponent,
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
  private readonly permissionService = inject(PermissionService);
  private readonly dialog = inject(MatDialog);

  private forceReplaceDocument: EnquiryDocument | null = null;

  protected readonly enquiry              = signal<Enquiry | null>(null);
  protected readonly documents            = signal<EnquiryDocument[]>([]);
  protected readonly payments             = signal<EnquiryPaymentResponse[]>([]);
  protected readonly statusHistory        = signal<EnquiryStatusHistoryResponse[]>([]);
  protected readonly creditApplications   = signal<EnquiryCreditApplication[]>([]);
  protected readonly loading              = signal(true);
  protected readonly verifyingDocumentId  = signal<number | null>(null);
  protected readonly selectedReceipt      = signal<ReceiptDisplayData | null>(null);

  protected readonly selectedTabIndex = signal(0);

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
  protected readonly totalCreditApplied = computed(() =>
    this.creditApplications().reduce((sum, c) => sum + (c.amountApplied ?? 0), 0),
  );
  protected readonly outstandingAmount = computed(() => {
    const netFee = this.enquiry()?.finalizedNetFee;
    return netFee == null ? 0 : Math.max(0, netFee - this.totalPaid());
  });
  protected readonly docsVerified = computed(
    () => this.documents().filter((d) => d.status === 'VERIFIED').length,
  );
  protected readonly docsTotal = computed(() => this.documents().length);
  protected readonly paymentsWithNotes = computed(() =>
    this.payments().filter((p) => p.receiptType !== 'REFUND' && p.remarks),
  );

  protected readonly journeyRows = computed(() => {
    const history = this.statusHistory();
    if (history.length === 0) return [];
    type Node = { status: string; changedAt: string | null; changedBy: string | null; remarks: string | null; isLast: boolean };
    const nodes: Node[] = [
      { status: history[0].fromStatus ?? '', changedAt: null, changedBy: null, remarks: null, isLast: false },
    ];
    history.forEach((h, i) => nodes.push({
      status: h.toStatus,
      changedAt: h.changedAt,
      changedBy: h.changedBy,
      remarks: h.remarks,
      isLast: i === history.length - 1,
    }));
    const rows: Node[][] = [];
    for (let i = 0; i < nodes.length; i += 4) rows.push(nodes.slice(i, i + 4));
    return rows;
  });

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
        this.enquiryService.getCreditApplications(id).subscribe({ next: (c) => this.creditApplications.set(c) });
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
    return this.enquiry()?.status === 'DOCUMENTS_VERIFIED';
  }

  protected readonly canEdit = computed(() => this.enquiry()?.status !== 'ADMITTED');

  protected submitDocuments(): void {
    const id = this.enquiry()?.id;
    if (!id) return;
    void this.router.navigate(['/enquiries/document-submission', id]);
  }

  protected statusLabel(s: string | null | undefined): string {
    return STATUS_LABELS[s ?? ''] ?? (s ?? '');
  }

  protected navigateToConvert(): void {
    const id = this.enquiry()?.id;
    if (id) void this.router.navigate(['/enquiries', id, 'convert']);
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

  /** Whether the current user can force-replace an already-VERIFIED document (BR-26 override). */
  protected canForceReplace(): boolean {
    return this.permissionService.has('DOCUMENT_VERIFIED_OVERRIDE');
  }

  /** Whether the current user can verify documents (re-verification after a force-replace, etc.). */
  protected canVerifyDocuments(): boolean {
    return this.permissionService.has('DOCUMENT_VERIFICATION_MANAGE');
  }

  /**
   * Verifies an UPLOADED document directly from this screen. Needed because the
   * dedicated verification screen only loads enquiries in DOCUMENTS_SUBMITTED
   * status, so a document reset to UPLOADED by a force-replace on a later-stage
   * enquiry (e.g. ADMITTED) has nowhere else to be re-verified.
   */
  protected verifyDocumentFile(d: EnquiryDocument): void {
    if (!this.canVerifyDocuments() || d.status !== 'UPLOADED' || this.verifyingDocumentId() !== null) return;
    this.verifyingDocumentId.set(d.id);
    this.enquiryService.verifyDocument(d.enquiryId, d.id).subscribe({
      next: (saved) => {
        this.documents.update((docs) => docs.map((x) => (x.id === d.id ? saved : x)));
        this.toast.success(`${d.documentType} verified`);
        this.verifyingDocumentId.set(null);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? `Failed to verify ${d.documentType}`);
        this.verifyingDocumentId.set(null);
      },
    });
  }

  /** Opens the file picker to force-replace a VERIFIED document, resetting it to Uploaded. */
  protected forceReplaceDocumentFile(d: EnquiryDocument, input: HTMLInputElement): void {
    if (!this.canForceReplace() || d.status !== 'VERIFIED') return;
    this.dialog
      .open(ConfirmDialogComponent, {
        data: {
          title: 'Force Replace Document',
          message: `"${d.documentType}" is verified. Replacing it will reset its status to Uploaded and require re-verification. Continue?`,
          confirmText: 'Replace',
          cancelText: 'Cancel',
        },
      })
      .afterClosed()
      .subscribe((confirmed) => {
        if (!confirmed) return;
        this.forceReplaceDocument = d;
        input.value = '';
        input.click();
      });
  }

  protected onForceReplaceFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    const doc = this.forceReplaceDocument;
    this.forceReplaceDocument = null;
    input.value = '';
    if (!file || !doc) return;

    this.enquiryService.uploadDocumentFile(doc.enquiryId, doc.documentType, file, undefined, true).subscribe({
      next: (saved) => {
        this.documents.update((docs) => docs.map((x) => (x.id === doc.id ? saved : x)));
        this.toast.success(`${doc.documentType} replaced`);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? `Failed to replace ${doc.documentType}`);
      },
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

  // ─── Receipt actions ─────────────────────────────────────────────────────

  /** Opens the receipt slide-in panel for the given payment. */
  protected viewReceipt(p: EnquiryPaymentResponse): void {
    const e = this.enquiry();
    this.selectedReceipt.set({
      receiptNumber:        p.receiptNumber,
      payerType:            'ENQUIRY',
      payerName:            e?.name ?? p.enquiryName,
      payerIdentifier:      null,
      programName:          e?.programName ?? null,
      amountPaid:           p.amountPaid,
      paymentDate:          p.paymentDate,
      paymentMode:          p.paymentMode,
      transactionReference: p.transactionReference,
      remarks:              p.remarks,
      installmentsCovered:  '',
      installmentBreakdown: [],
      feeCategory:          p.feeCategory,
    });
  }

  /** Directly prints the receipt without opening the dialog. */
  protected printReceiptDirect(p: EnquiryPaymentResponse): void {
    const e = this.enquiry();
    printFeeReceipt({
      receiptNumber:        p.receiptNumber,
      payerName:            e?.name ?? p.enquiryName,
      payerIdentifier:      '',
      programName:          e?.programName ?? '',
      amountPaid:           p.amountPaid,
      paymentDate:          p.paymentDate,
      paymentMode:          p.paymentMode,
      transactionReference: p.transactionReference,
      feeCategory:          p.feeCategory,
      installmentBreakdown: [],
    });
  }

  /** Opens the refund voucher PDF in a new tab (view). */
  protected viewRefundVoucher(p: EnquiryPaymentResponse): void {
    void printRefundVoucher(this.toRefundVoucherData(p));
  }

  /** Downloads the refund voucher PDF. */
  protected downloadRefundVoucher(p: EnquiryPaymentResponse): void {
    void downloadRefundVoucher(this.toRefundVoucherData(p));
  }

  private toRefundVoucherData(p: EnquiryPaymentResponse): RefundVoucherData {
    const e = this.enquiry();
    return {
      refundNumber:          p.receiptNumber,
      originalReceiptNumber: p.originalReceiptNumber ?? '',
      payerName:             e?.name ?? p.enquiryName,
      payerIdentifier:       null,
      admissionNumber:       null,
      programName:           e?.programName ?? null,
      refundAmount:          p.amountPaid,
      refundDate:            p.paymentDate,
      reason:                p.remarks ?? '',
      paymentMode:           p.paymentMode,
      paymentDate:           p.paymentDate,
      transactionReference:  p.transactionReference,
    };
  }
}

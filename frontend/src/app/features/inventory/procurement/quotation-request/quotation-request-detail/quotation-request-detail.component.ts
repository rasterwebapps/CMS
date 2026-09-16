import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { QuotationRequestService } from '../quotation-request.service';
import { AvailableRequisitionLine, QuotationRequest, QuotationRequestLine, QuotationRequestSupplier } from '../quotation-request.model';
import { SupplierService } from '../../supplier/supplier.service';
import { Supplier } from '../../supplier/supplier.model';
import { ConfirmDialogComponent } from '../../../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsStatusBadgeComponent } from '../../../../../shared/status-badge/status-badge.component';
import { PermissionService } from '../../../../../core/permissions/permission.service';
import { ToastService } from '../../../../../core/toast/toast.service';

interface ResponseDraft {
  price: number | null;
  leadTimeDays: number | null;
}

@Component({
  selector: 'app-quotation-request-detail',
  standalone: true,
  imports: [
    RouterLink,
    FormsModule,
    DatePipe,
    MatDialogModule,
    MatProgressSpinnerModule,
    CmsStatusBadgeComponent,
  ],
  templateUrl: './quotation-request-detail.component.html',
  styleUrl: './quotation-request-detail.component.scss',
})
export class QuotationRequestDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly requestService = inject(QuotationRequestService);
  private readonly supplierService = inject(SupplierService);
  private readonly dialog = inject(MatDialog);
  private readonly permissionService = inject(PermissionService);
  private readonly toast = inject(ToastService);

  protected readonly loading = signal(false);
  protected readonly busy = signal(false);
  protected readonly quotationRequest = signal<QuotationRequest | null>(null);
  protected readonly availableLines = signal<AvailableRequisitionLine[]>([]);
  protected readonly activeSuppliers = signal<Supplier[]>([]);

  protected readonly canAward = computed(() => this.permissionService.has('INVENTORY_QUOTATION_AWARD'));
  protected readonly invitableSuppliers = computed(() => {
    const invitedIds = new Set((this.quotationRequest()?.suppliers ?? []).map((s) => s.supplierId));
    return this.activeSuppliers().filter((s) => !invitedIds.has(s.id));
  });
  protected readonly hasAwardedNotYetOrdered = computed(() =>
    (this.quotationRequest()?.lines ?? []).some((l) => l.status === 'AWARDED'));

  protected selectedRequisitionItemId: number | null = null;
  protected addQtyOverride: number | null = null;
  protected selectedSupplierId: number | null = null;
  protected readonly responseDrafts = signal<Record<string, ResponseDraft>>({});
  protected readonly awardChoice = signal<Record<number, number | null>>({});

  private requestId!: number;

  ngOnInit(): void {
    this.requestId = Number(this.route.snapshot.paramMap.get('id'));
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.requestService.getById(this.requestId).subscribe({
      next: (r) => {
        this.quotationRequest.set(r);
        this.loading.set(false);
        if (r.status === 'DRAFT') {
          this.requestService.getAvailableRequisitionLines(r.locationId).subscribe({ next: (l) => this.availableLines.set(l) });
          this.supplierService.getAll(true).subscribe({ next: (s) => this.activeSuppliers.set(s) });
        }
      },
      error: () => { this.toast.error('Failed to load quotation request'); this.loading.set(false); },
    });
  }

  // ── DRAFT: lines ─────────────────────────────────────────────────────────

  protected addLine(): void {
    const purchaseRequisitionItemId = this.selectedRequisitionItemId;
    if (purchaseRequisitionItemId == null) return;
    this.busy.set(true);
    this.requestService.addLine(this.requestId, {
      purchaseRequisitionItemId,
      requestedQty: this.addQtyOverride ?? undefined,
    }).subscribe({
      next: () => {
        this.selectedRequisitionItemId = null;
        this.addQtyOverride = null;
        this.toast.success('Line added to the request');
        this.busy.set(false);
        this.load();
      },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to add line'); this.busy.set(false); },
    });
  }

  protected removeLine(line: QuotationRequestLine): void {
    this.busy.set(true);
    this.requestService.removeLine(this.requestId, line.id).subscribe({
      next: () => { this.toast.success('Line removed'); this.busy.set(false); this.load(); },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to remove line'); this.busy.set(false); },
    });
  }

  // ── DRAFT: suppliers ─────────────────────────────────────────────────────

  protected addSupplier(): void {
    const supplierId = this.selectedSupplierId;
    if (supplierId == null) return;
    this.busy.set(true);
    this.requestService.addSupplier(this.requestId, { supplierId }).subscribe({
      next: () => {
        this.selectedSupplierId = null;
        this.toast.success('Supplier invited');
        this.busy.set(false);
        this.load();
      },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to invite supplier'); this.busy.set(false); },
    });
  }

  protected removeSupplier(supplier: QuotationRequestSupplier): void {
    this.busy.set(true);
    this.requestService.removeSupplier(this.requestId, supplier.supplierId).subscribe({
      next: () => { this.toast.success('Supplier removed'); this.busy.set(false); this.load(); },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to remove supplier'); this.busy.set(false); },
    });
  }

  // ── DRAFT -> SUBMITTED / cancel ──────────────────────────────────────────

  protected submitRequest(): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Submit Quotation Request',
        message: 'Submitting locks in the product list and invited suppliers, and opens the request for recording quotes. Continue?',
        confirmText: 'Submit',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.busy.set(true);
      this.requestService.submit(this.requestId).subscribe({
        next: () => { this.toast.success('Quotation request submitted'); this.busy.set(false); this.load(); },
        error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to submit request'); this.busy.set(false); },
      });
    });
  }

  protected cancelRequest(): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Cancel Quotation Request',
        message: 'This abandons the request entirely — the underlying requisition lines stay approved and available to order directly. Continue?',
        confirmText: 'Cancel Request',
        cancelText: 'Keep Request',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.busy.set(true);
      this.requestService.cancel(this.requestId).subscribe({
        next: () => { this.toast.success('Quotation request cancelled'); this.busy.set(false); this.load(); },
        error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to cancel request'); this.busy.set(false); },
      });
    });
  }

  // ── SUBMITTED: record quotes, award, reject ─────────────────────────────

  private draftKey(lineId: number, supplierId: number): string {
    return `${lineId}-${supplierId}`;
  }

  protected getResponseFor(line: QuotationRequestLine, supplierId: number) {
    return line.responses.find((r) => r.supplierId === supplierId) ?? null;
  }

  protected getDraft(lineId: number, supplierId: number): ResponseDraft {
    return this.responseDrafts()[this.draftKey(lineId, supplierId)] ?? { price: null, leadTimeDays: null };
  }

  protected setDraftPrice(lineId: number, supplierId: number, price: number | null): void {
    const key = this.draftKey(lineId, supplierId);
    this.responseDrafts.update((d) => ({ ...d, [key]: { ...(d[key] ?? { price: null, leadTimeDays: null }), price } }));
  }

  protected setDraftLeadTime(lineId: number, supplierId: number, leadTimeDays: number | null): void {
    const key = this.draftKey(lineId, supplierId);
    this.responseDrafts.update((d) => ({ ...d, [key]: { ...(d[key] ?? { price: null, leadTimeDays: null }), leadTimeDays } }));
  }

  protected recordResponse(line: QuotationRequestLine, supplierId: number): void {
    const draft = this.getDraft(line.id, supplierId);
    if (draft.price == null || draft.price < 0) return;
    this.busy.set(true);
    this.requestService.recordResponse(this.requestId, line.id, supplierId, {
      quotedUnitPrice: draft.price,
      quotedLeadTimeDays: draft.leadTimeDays ?? undefined,
    }).subscribe({
      next: () => { this.toast.success('Quote recorded'); this.busy.set(false); this.load(); },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to record quote'); this.busy.set(false); },
    });
  }

  protected setAwardChoice(lineId: number, responseLineId: number | null): void {
    this.awardChoice.update((c) => ({ ...c, [lineId]: responseLineId }));
  }

  protected awardLine(line: QuotationRequestLine): void {
    const responseLineId = this.awardChoice()[line.id];
    if (responseLineId == null) return;
    this.busy.set(true);
    this.requestService.award(this.requestId, line.id, { responseLineId }).subscribe({
      next: () => { this.toast.success('Line awarded'); this.busy.set(false); this.load(); },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to award line'); this.busy.set(false); },
    });
  }

  protected rejectLine(line: QuotationRequestLine): void {
    this.busy.set(true);
    this.requestService.rejectLine(this.requestId, line.id).subscribe({
      next: () => { this.toast.success('Line rejected'); this.busy.set(false); this.load(); },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to reject line'); this.busy.set(false); },
    });
  }

  protected convertAwardedLines(): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Create Purchase Order(s)',
        message: 'Creates one Purchase Order per winning supplier from every awarded line not yet ordered. Continue?',
        confirmText: 'Create Purchase Order(s)',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.busy.set(true);
      this.requestService.convertAwardedLines(this.requestId).subscribe({
        next: (orders) => {
          this.toast.success(`Created ${orders.length} Purchase Order${orders.length === 1 ? '' : 's'}`);
          this.busy.set(false);
          this.load();
        },
        error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to create Purchase Order(s)'); this.busy.set(false); },
      });
    });
  }

  protected goBack(): void {
    void this.router.navigate(['/inventory/procurement/quotation-requests']);
  }
}

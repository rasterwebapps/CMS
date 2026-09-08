import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { StockIssueRequestService } from '../stock-issue-request.service';
import { StockIssueRequest, StockIssueRequestItem } from '../stock-issue-request.model';
import { ConfirmDialogComponent } from '../../../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsStatusBadgeComponent } from '../../../../../shared/status-badge/status-badge.component';
import { CmsProductPickerComponent } from '../../../../../shared/product-picker/product-picker.component';
import { PermissionService } from '../../../../../core/permissions/permission.service';
import { ToastService } from '../../../../../core/toast/toast.service';

@Component({
  selector: 'app-stock-issue-request-detail',
  standalone: true,
  imports: [
    RouterLink,
    FormsModule,
    DatePipe,
    MatDialogModule,
    MatProgressSpinnerModule,
    CmsStatusBadgeComponent,
    CmsProductPickerComponent,
  ],
  templateUrl: './stock-issue-request-detail.component.html',
  styleUrl: './stock-issue-request-detail.component.scss',
})
export class StockIssueRequestDetailComponent implements OnInit {
  private readonly route             = inject(ActivatedRoute);
  private readonly router            = inject(Router);
  private readonly requestService    = inject(StockIssueRequestService);
  private readonly dialog            = inject(MatDialog);
  private readonly permissionService = inject(PermissionService);
  private readonly toast             = inject(ToastService);

  protected readonly loading      = signal(false);
  protected readonly busy         = signal(false);
  protected readonly issueRequest = signal<StockIssueRequest | null>(null);
  protected readonly addProductId = signal<number | null>(null);
  protected readonly addQty       = signal<number | null>(null);

  protected readonly canApprove = computed(() => this.permissionService.has('INVENTORY_ISSUE_REQUEST_APPROVE'));
  protected readonly canReturn  = computed(() => this.permissionService.has('INVENTORY_ISSUE_REQUEST_RETURN'));

  protected returnQtyByLine: Record<number, number | null> = {};

  private requestId!: number;

  ngOnInit(): void {
    this.requestId = Number(this.route.snapshot.paramMap.get('id'));
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.requestService.getById(this.requestId).subscribe({
      next: (r) => { this.issueRequest.set(r); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load stock issue request'); this.loading.set(false); },
    });
  }

  protected addLine(): void {
    const productId = this.addProductId();
    const requestedQty = this.addQty();
    if (productId == null || requestedQty == null || requestedQty <= 0) return;
    this.busy.set(true);
    this.requestService.addLine(this.requestId, { productId, requestedQty }).subscribe({
      next: () => {
        this.addProductId.set(null);
        this.addQty.set(null);
        this.toast.success('Product added to the request');
        this.busy.set(false);
        this.load();
      },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to add product'); this.busy.set(false); },
    });
  }

  protected removeLine(line: StockIssueRequestItem): void {
    this.busy.set(true);
    this.requestService.removeLine(this.requestId, line.id).subscribe({
      next: () => { this.toast.success('Line removed'); this.busy.set(false); this.load(); },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to remove line'); this.busy.set(false); },
    });
  }

  protected submitRequest(): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Submit Request',
        message: 'Submitting locks in the product list and sends every line for approval. Continue?',
        confirmText: 'Submit',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.busy.set(true);
      this.requestService.submit(this.requestId).subscribe({
        next: () => { this.toast.success('Request submitted'); this.busy.set(false); this.load(); },
        error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to submit request'); this.busy.set(false); },
      });
    });
  }

  protected cancelRequest(): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Cancel Request',
        message: 'This abandons the request entirely. Continue?',
        confirmText: 'Cancel Request',
        cancelText: 'Keep Request',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.busy.set(true);
      this.requestService.cancel(this.requestId).subscribe({
        next: () => { this.toast.success('Request cancelled'); this.busy.set(false); this.load(); },
        error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to cancel request'); this.busy.set(false); },
      });
    });
  }

  protected approveLine(line: StockIssueRequestItem): void {
    this.busy.set(true);
    this.requestService.approveLine(this.requestId, line.id, { notes: line.resolutionNotes ?? undefined }).subscribe({
      next: () => { this.toast.success('Line approved and issued'); this.busy.set(false); this.load(); },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to approve line'); this.busy.set(false); },
    });
  }

  protected rejectLine(line: StockIssueRequestItem): void {
    this.busy.set(true);
    this.requestService.rejectLine(this.requestId, line.id, { notes: line.resolutionNotes ?? undefined }).subscribe({
      next: () => { this.toast.success('Line rejected'); this.busy.set(false); this.load(); },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to reject line'); this.busy.set(false); },
    });
  }

  protected returnLine(line: StockIssueRequestItem): void {
    const qty = this.returnQtyByLine[line.id];
    if (qty == null || qty <= 0) return;
    this.busy.set(true);
    this.requestService.returnLine(this.requestId, line.id, { returnedQty: qty }).subscribe({
      next: () => {
        this.returnQtyByLine[line.id] = null;
        this.toast.success('Stock returned to the issuing location');
        this.busy.set(false);
        this.load();
      },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to return stock'); this.busy.set(false); },
    });
  }

  protected goBack(): void {
    void this.router.navigate(['/inventory/issue/stock-issue-requests']);
  }
}

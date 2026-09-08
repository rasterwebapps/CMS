import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PurchaseRequisitionService } from '../purchase-requisition.service';
import { PurchaseRequisition, PurchaseRequisitionItem } from '../purchase-requisition.model';
import { ConfirmDialogComponent } from '../../../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsStatusBadgeComponent } from '../../../../../shared/status-badge/status-badge.component';
import { CmsProductPickerComponent } from '../../../../../shared/product-picker/product-picker.component';
import { PermissionService } from '../../../../../core/permissions/permission.service';
import { ToastService } from '../../../../../core/toast/toast.service';

@Component({
  selector: 'app-purchase-requisition-detail',
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
  templateUrl: './purchase-requisition-detail.component.html',
  styleUrl: './purchase-requisition-detail.component.scss',
})
export class PurchaseRequisitionDetailComponent implements OnInit {
  private readonly route              = inject(ActivatedRoute);
  private readonly router             = inject(Router);
  private readonly requisitionService = inject(PurchaseRequisitionService);
  private readonly dialog             = inject(MatDialog);
  private readonly permissionService  = inject(PermissionService);
  private readonly toast              = inject(ToastService);

  protected readonly loading      = signal(false);
  protected readonly busy         = signal(false);
  protected readonly requisition  = signal<PurchaseRequisition | null>(null);
  protected readonly addProductId = signal<number | null>(null);
  protected readonly addQty       = signal<number | null>(null);

  protected readonly canApprove = computed(() => this.permissionService.has('INVENTORY_PURCHASE_REQUISITION_APPROVE'));

  private requisitionId!: number;

  ngOnInit(): void {
    this.requisitionId = Number(this.route.snapshot.paramMap.get('id'));
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.requisitionService.getById(this.requisitionId).subscribe({
      next: (r) => { this.requisition.set(r); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load purchase requisition'); this.loading.set(false); },
    });
  }

  protected addLine(): void {
    const productId = this.addProductId();
    const requestedQty = this.addQty();
    if (productId == null || requestedQty == null || requestedQty <= 0) return;
    this.busy.set(true);
    this.requisitionService.addLine(this.requisitionId, { productId, requestedQty }).subscribe({
      next: () => {
        this.addProductId.set(null);
        this.addQty.set(null);
        this.toast.success('Product added to the requisition');
        this.busy.set(false);
        this.load();
      },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to add product'); this.busy.set(false); },
    });
  }

  protected removeLine(line: PurchaseRequisitionItem): void {
    this.busy.set(true);
    this.requisitionService.removeLine(this.requisitionId, line.id).subscribe({
      next: () => { this.toast.success('Line removed'); this.busy.set(false); this.load(); },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to remove line'); this.busy.set(false); },
    });
  }

  protected submitRequisition(): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Submit Requisition',
        message: 'Submitting locks in the product list and sends every line for approval. Continue?',
        confirmText: 'Submit',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.busy.set(true);
      this.requisitionService.submit(this.requisitionId).subscribe({
        next: () => { this.toast.success('Requisition submitted'); this.busy.set(false); this.load(); },
        error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to submit requisition'); this.busy.set(false); },
      });
    });
  }

  protected cancelRequisition(): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Cancel Requisition',
        message: 'This abandons the requisition entirely. Continue?',
        confirmText: 'Cancel Requisition',
        cancelText: 'Keep Requisition',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.busy.set(true);
      this.requisitionService.cancel(this.requisitionId).subscribe({
        next: () => { this.toast.success('Requisition cancelled'); this.busy.set(false); this.load(); },
        error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to cancel requisition'); this.busy.set(false); },
      });
    });
  }

  protected approveLine(line: PurchaseRequisitionItem): void {
    this.busy.set(true);
    this.requisitionService.approveLine(this.requisitionId, line.id, { notes: line.resolutionNotes ?? undefined }).subscribe({
      next: () => { this.toast.success('Line approved'); this.busy.set(false); this.load(); },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to approve line'); this.busy.set(false); },
    });
  }

  protected rejectLine(line: PurchaseRequisitionItem): void {
    this.busy.set(true);
    this.requisitionService.rejectLine(this.requisitionId, line.id, { notes: line.resolutionNotes ?? undefined }).subscribe({
      next: () => { this.toast.success('Line rejected'); this.busy.set(false); this.load(); },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to reject line'); this.busy.set(false); },
    });
  }

  protected goBack(): void {
    void this.router.navigate(['/inventory/procurement/purchase-requisitions']);
  }
}

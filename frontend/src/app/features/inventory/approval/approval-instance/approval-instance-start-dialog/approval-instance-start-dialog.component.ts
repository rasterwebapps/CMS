import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { ApprovalInstanceService } from '../approval-instance.service';
import { ApprovalWorkflowService } from '../../approval-workflow/approval-workflow.service';
import { ApprovalWorkflow } from '../../approval-workflow/approval-workflow.model';
import { PurchaseRequisitionService } from '../../../procurement/purchase-requisition/purchase-requisition.service';
import { PurchaseRequisition } from '../../../procurement/purchase-requisition/purchase-requisition.model';
import { PurchaseOrderService } from '../../../procurement/purchase-order/purchase-order.service';
import { PurchaseOrder } from '../../../procurement/purchase-order/purchase-order.model';
import { ToastService } from '../../../../../core/toast/toast.service';

/** "Start Approval" — an explicit, optional action a user takes against an already-existing
 *  Purchase Requisition/Order; never auto-triggered. See the "Multi-level approval routing
 *  slice" decision-log entry for the scope boundary this deliberately stays within. */
@Component({
  selector: 'app-approval-instance-start-dialog',
  standalone: true,
  imports: [FormsModule, MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>Start Approval</h2>
    <mat-dialog-content class="ai-start-content">
      <div class="field-group">
        <label class="field-label">Document Type</label>
        <select class="field-select" [(ngModel)]="documentType" (ngModelChange)="onDocumentTypeChange()" aria-label="Document type">
          <option value="PURCHASE_REQUISITION">Purchase Requisition</option>
          <option value="PURCHASE_ORDER">Purchase Order</option>
        </select>
      </div>
      <div class="field-group">
        <label class="field-label">Document</label>
        <select class="field-select" [(ngModel)]="documentId" (ngModelChange)="onDocumentChange()" aria-label="Document">
          <option [ngValue]="null" disabled>Select a document</option>
          @if (documentType === 'PURCHASE_REQUISITION') {
            @for (r of requisitions; track r.id) {
              <option [ngValue]="r.id">#{{ r.id }} — {{ r.locationVirtualName }}</option>
            }
          } @else {
            @for (o of orders; track o.id) {
              <option [ngValue]="o.id">#{{ o.id }} — {{ o.supplierName }} ({{ o.currencyCode }} {{ o.totalAmount }})</option>
            }
          }
        </select>
      </div>
      <div class="field-group">
        <label class="field-label">Workflow</label>
        <select class="field-select" [(ngModel)]="workflowId" aria-label="Workflow">
          <option [ngValue]="null" disabled>Select a workflow</option>
          @for (w of eligibleWorkflows; track w.id) {
            <option [ngValue]="w.id">{{ w.name }} ({{ w.steps.length }} steps)</option>
          }
        </select>
        @if (documentId != null && !eligibleWorkflows.length) {
          <p class="field-hint">No active workflow applies to this document.</p>
        }
      </div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button (click)="dialogRef.close(null)">Cancel</button>
      <button mat-flat-button color="primary" [disabled]="!workflowId" (click)="confirm()">Start Approval</button>
    </mat-dialog-actions>
  `,
  styles: `
    .ai-start-content { display: flex; flex-direction: column; gap: 12px; min-width: 340px; }
  `,
})
export class ApprovalInstanceStartDialogComponent implements OnInit {
  protected readonly dialogRef        = inject(MatDialogRef<ApprovalInstanceStartDialogComponent, boolean | null>);
  private readonly instanceService    = inject(ApprovalInstanceService);
  private readonly workflowService    = inject(ApprovalWorkflowService);
  private readonly requisitionService = inject(PurchaseRequisitionService);
  private readonly orderService       = inject(PurchaseOrderService);
  private readonly toast              = inject(ToastService);

  protected documentType: 'PURCHASE_REQUISITION' | 'PURCHASE_ORDER' = 'PURCHASE_REQUISITION';
  protected documentId: number | null = null;
  protected workflowId: number | null = null;
  protected requisitions: PurchaseRequisition[] = [];
  protected orders: PurchaseOrder[] = [];
  protected eligibleWorkflows: ApprovalWorkflow[] = [];

  ngOnInit(): void {
    this.loadDocuments();
  }

  protected onDocumentTypeChange(): void {
    this.documentId = null;
    this.workflowId = null;
    this.eligibleWorkflows = [];
    this.loadDocuments();
  }

  private loadDocuments(): void {
    if (this.documentType === 'PURCHASE_REQUISITION') {
      this.requisitionService.getPage({ status: 'SUBMITTED', size: 100 }).subscribe({ next: (page) => this.requisitions = page.content });
    } else {
      this.orderService.getPage({ status: 'PENDING', size: 100 }).subscribe({ next: (page) => this.orders = page.content });
    }
  }

  protected onDocumentChange(): void {
    this.workflowId = null;
    this.eligibleWorkflows = [];
    if (this.documentId == null) return;

    if (this.documentType === 'PURCHASE_REQUISITION') {
      const requisition = this.requisitions.find((r) => r.id === this.documentId);
      if (!requisition) return;
      this.workflowService.getEligible('PURCHASE_REQUISITION', requisition.locationId).subscribe({
        next: (workflows) => this.eligibleWorkflows = workflows,
      });
    } else {
      const order = this.orders.find((o) => o.id === this.documentId);
      if (!order) return;
      this.workflowService.getEligible('PURCHASE_ORDER', order.locationId, order.totalAmount).subscribe({
        next: (workflows) => this.eligibleWorkflows = workflows,
      });
    }
  }

  protected confirm(): void {
    if (this.documentId == null || this.workflowId == null) return;
    this.instanceService.start({
      workflowId: this.workflowId,
      purchaseRequisitionId: this.documentType === 'PURCHASE_REQUISITION' ? this.documentId : undefined,
      purchaseOrderId: this.documentType === 'PURCHASE_ORDER' ? this.documentId : undefined,
    }).subscribe({
      next: () => { this.toast.success('Approval started'); this.dialogRef.close(true); },
      error: (err) => this.toast.error(err?.error?.message ?? 'Failed to start approval'),
    });
  }
}

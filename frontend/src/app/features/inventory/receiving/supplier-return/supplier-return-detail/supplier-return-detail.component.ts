import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { SupplierReturnService } from '../supplier-return.service';
import { ReturnableGoodsReceiptLine, SupplierReturn, SupplierReturnLine } from '../supplier-return.model';
import { ConfirmDialogComponent } from '../../../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsStatusBadgeComponent } from '../../../../../shared/status-badge/status-badge.component';
import { ToastService } from '../../../../../core/toast/toast.service';

@Component({
  selector: 'app-supplier-return-detail',
  standalone: true,
  imports: [
    RouterLink,
    FormsModule,
    DatePipe,
    MatDialogModule,
    MatProgressSpinnerModule,
    CmsStatusBadgeComponent,
  ],
  templateUrl: './supplier-return-detail.component.html',
  styleUrl: './supplier-return-detail.component.scss',
})
export class SupplierReturnDetailComponent implements OnInit {
  private readonly route         = inject(ActivatedRoute);
  private readonly router        = inject(Router);
  private readonly returnService = inject(SupplierReturnService);
  private readonly dialog        = inject(MatDialog);
  private readonly toast         = inject(ToastService);

  protected readonly loading         = signal(false);
  protected readonly busy            = signal(false);
  protected readonly ret             = signal<SupplierReturn | null>(null);
  protected readonly returnableLines = signal<ReturnableGoodsReceiptLine[]>([]);

  protected addReceiptLineId: number | null = null;
  protected addQty: number | null = null;

  private returnId!: number;

  ngOnInit(): void {
    this.returnId = Number(this.route.snapshot.paramMap.get('id'));
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.returnService.getById(this.returnId).subscribe({
      next: (r) => {
        this.ret.set(r);
        this.loading.set(false);
        if (r.status === 'DRAFT') this.loadReturnableLines(r.goodsReceiptId);
      },
      error: () => { this.toast.error('Failed to load supplier return'); this.loading.set(false); },
    });
  }

  private loadReturnableLines(goodsReceiptId: number): void {
    this.returnService.getReturnableLines(goodsReceiptId).subscribe({
      next: (lines) => this.returnableLines.set(lines),
      error: () => this.toast.error('Failed to load the receipt\'s returnable lines'),
    });
  }

  protected onReceiptLineChange(): void {
    const line = this.returnableLines().find((l) => l.id === this.addReceiptLineId);
    this.addQty = line ? line.openQty : null;
  }

  protected addLine(): void {
    if (this.addReceiptLineId == null || this.addQty == null || this.addQty <= 0) return;
    this.busy.set(true);
    this.returnService.addLine(this.returnId, {
      goodsReceiptLineId: this.addReceiptLineId,
      returnedQty: this.addQty,
    }).subscribe({
      next: () => {
        this.addReceiptLineId = null;
        this.addQty = null;
        this.toast.success('Line added to the return');
        this.busy.set(false);
        this.load();
      },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to add line'); this.busy.set(false); },
    });
  }

  protected removeLine(line: SupplierReturnLine): void {
    this.busy.set(true);
    this.returnService.removeLine(this.returnId, line.id).subscribe({
      next: () => { this.toast.success('Line removed'); this.busy.set(false); this.load(); },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to remove line'); this.busy.set(false); },
    });
  }

  protected completeReturn(): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Complete Return',
        message: 'This posts each line out of stock and cannot be undone from here. Continue?',
        confirmText: 'Complete Return',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.busy.set(true);
      this.returnService.complete(this.returnId).subscribe({
        next: () => { this.toast.success('Return completed and posted to stock'); this.busy.set(false); this.load(); },
        error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to complete return'); this.busy.set(false); },
      });
    });
  }

  protected cancelReturn(): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Cancel Return',
        message: 'This abandons the return entirely. Continue?',
        confirmText: 'Cancel Return',
        cancelText: 'Keep Return',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.busy.set(true);
      this.returnService.cancel(this.returnId).subscribe({
        next: () => { this.toast.success('Return cancelled'); this.busy.set(false); this.load(); },
        error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to cancel return'); this.busy.set(false); },
      });
    });
  }

  protected goBack(): void {
    void this.router.navigate(['/inventory/receiving/supplier-returns']);
  }
}

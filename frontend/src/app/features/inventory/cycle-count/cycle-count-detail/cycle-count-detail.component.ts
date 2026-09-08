import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CycleCountService } from '../cycle-count.service';
import { CycleCount, CycleCountLine } from '../cycle-count.model';
import { ProductService } from '../../product/product.service';
import { Product } from '../../product/product.model';
import { ConfirmDialogComponent } from '../../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsStatusBadgeComponent } from '../../../../shared/status-badge/status-badge.component';
import { PermissionService } from '../../../../core/permissions/permission.service';
import { ToastService } from '../../../../core/toast/toast.service';

@Component({
  selector: 'app-cycle-count-detail',
  standalone: true,
  imports: [
    RouterLink,
    FormsModule,
    DatePipe,
    MatDialogModule,
    MatProgressSpinnerModule,
    CmsStatusBadgeComponent,
  ],
  templateUrl: './cycle-count-detail.component.html',
  styleUrl: './cycle-count-detail.component.scss',
})
export class CycleCountDetailComponent implements OnInit {
  private readonly route             = inject(ActivatedRoute);
  private readonly router            = inject(Router);
  private readonly cycleCountService = inject(CycleCountService);
  private readonly productService    = inject(ProductService);
  private readonly dialog            = inject(MatDialog);
  private readonly permissionService = inject(PermissionService);
  private readonly toast             = inject(ToastService);

  protected readonly loading  = signal(false);
  protected readonly busy     = signal(false);
  protected readonly count    = signal<CycleCount | null>(null);
  protected readonly allProducts = signal<Product[]>([]);
  protected readonly addProductId = signal<number | null>(null);

  protected readonly canApprove = computed(() => this.permissionService.has('INVENTORY_CYCLE_COUNT_APPROVE'));

  protected readonly availableProducts = computed(() => {
    const c = this.count();
    const usedIds = new Set((c?.lines ?? []).map((l) => l.productId));
    return this.allProducts().filter((p) => !usedIds.has(p.id));
  });

  private countId!: number;

  ngOnInit(): void {
    this.countId = Number(this.route.snapshot.paramMap.get('id'));
    this.productService.getPage({ page: 0, size: 1000 }).subscribe({ next: (p) => this.allProducts.set(p.content) });
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.cycleCountService.getById(this.countId).subscribe({
      next: (c) => { this.count.set(c); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load cycle count'); this.loading.set(false); },
    });
  }

  protected addProduct(): void {
    const productId = this.addProductId();
    if (productId == null) return;
    this.busy.set(true);
    this.cycleCountService.addLine(this.countId, { productId }).subscribe({
      next: () => {
        this.addProductId.set(null);
        this.toast.success('Product added to the count sheet');
        this.busy.set(false);
        this.load();
      },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to add product'); this.busy.set(false); },
    });
  }

  protected removeLine(line: CycleCountLine): void {
    this.busy.set(true);
    this.cycleCountService.removeLine(this.countId, line.id).subscribe({
      next: () => { this.toast.success('Line removed'); this.busy.set(false); this.load(); },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to remove line'); this.busy.set(false); },
    });
  }

  protected saveCount(line: CycleCountLine): void {
    if (line.countedQty == null) return;
    this.cycleCountService.enterCount(this.countId, line.id, { countedQty: line.countedQty, notes: line.notes ?? undefined }).subscribe({
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to save the count'); this.load(); },
    });
  }

  protected submitCount(): void {
    const c = this.count();
    if (!c) return;
    const uncounted = (c.lines ?? []).filter((l) => l.countedQty == null).length;
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Submit Count',
        message: uncounted > 0
          ? `${uncounted} product(s) still have no counted quantity — submit anyway?`
          : 'Submitting locks in every counted quantity and reveals variances for review. Continue?',
        confirmText: 'Submit',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.busy.set(true);
      this.cycleCountService.submit(this.countId).subscribe({
        next: () => { this.toast.success('Count submitted'); this.busy.set(false); this.load(); },
        error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to submit count'); this.busy.set(false); },
      });
    });
  }

  protected cancelCount(): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Cancel Count',
        message: 'This abandons the count sheet entirely — no stock changes will be posted. Continue?',
        confirmText: 'Cancel Count',
        cancelText: 'Keep Count',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.busy.set(true);
      this.cycleCountService.cancel(this.countId).subscribe({
        next: () => { this.toast.success('Count cancelled'); this.busy.set(false); this.load(); },
        error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to cancel count'); this.busy.set(false); },
      });
    });
  }

  protected approveLine(line: CycleCountLine): void {
    this.busy.set(true);
    this.cycleCountService.approveLine(this.countId, line.id, { notes: line.resolutionNotes ?? undefined }).subscribe({
      next: () => { this.toast.success('Variance approved and posted to stock'); this.busy.set(false); this.load(); },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to approve variance'); this.busy.set(false); },
    });
  }

  protected rejectLine(line: CycleCountLine): void {
    this.busy.set(true);
    this.cycleCountService.rejectLine(this.countId, line.id, { notes: line.resolutionNotes ?? undefined }).subscribe({
      next: () => { this.toast.success('Variance rejected — no stock change'); this.busy.set(false); this.load(); },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to reject variance'); this.busy.set(false); },
    });
  }

  protected goBack(): void {
    void this.router.navigate(['/inventory/stock/cycle-counts']);
  }
}

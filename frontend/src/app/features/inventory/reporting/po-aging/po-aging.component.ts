import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { Router } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PurchaseOrderAgingReportService } from './po-aging.service';
import { PurchaseOrderAgingReport } from './po-aging.model';
import { CmsEmptyStateComponent } from '../../../../shared/empty-state/empty-state.component';
import { ToastService } from '../../../../core/toast/toast.service';

@Component({
  selector: 'app-po-aging-report',
  standalone: true,
  imports: [
    DatePipe,
    DecimalPipe,
    MatProgressSpinnerModule,
    CmsEmptyStateComponent,
  ],
  templateUrl: './po-aging.component.html',
  styleUrl: './po-aging.component.scss',
})
export class PurchaseOrderAgingReportComponent implements OnInit {
  private readonly reportService = inject(PurchaseOrderAgingReportService);
  private readonly router        = inject(Router);
  private readonly toast         = inject(ToastService);

  protected readonly loading = signal(false);
  protected readonly report  = signal<PurchaseOrderAgingReport | null>(null);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.reportService.get().subscribe({
      next: (r) => { this.report.set(r); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load the purchase order aging report'); this.loading.set(false); },
    });
  }

  protected isOverdueBucket(label: string): boolean {
    return label === '61–90 days' || label === '90+ days';
  }

  protected goToPurchaseOrders(): void {
    void this.router.navigate(['/inventory/procurement/purchase-orders']);
  }
}

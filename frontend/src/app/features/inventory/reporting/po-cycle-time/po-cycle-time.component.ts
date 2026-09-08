import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PurchaseOrderCycleTimeReportService } from './po-cycle-time.service';
import { PurchaseOrderCycleTimeReport } from './po-cycle-time.model';
import { CmsEmptyStateComponent } from '../../../../shared/empty-state/empty-state.component';
import { ToastService } from '../../../../core/toast/toast.service';

@Component({
  selector: 'app-po-cycle-time-report',
  standalone: true,
  imports: [
    DatePipe,
    DecimalPipe,
    MatProgressSpinnerModule,
    CmsEmptyStateComponent,
  ],
  templateUrl: './po-cycle-time.component.html',
  styleUrl: './po-cycle-time.component.scss',
})
export class PurchaseOrderCycleTimeReportComponent implements OnInit {
  private readonly reportService = inject(PurchaseOrderCycleTimeReportService);
  private readonly toast         = inject(ToastService);

  protected readonly loading = signal(false);
  protected readonly report  = signal<PurchaseOrderCycleTimeReport | null>(null);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.reportService.get().subscribe({
      next: (r) => { this.report.set(r); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load the purchase order cycle-time report'); this.loading.set(false); },
    });
  }
}

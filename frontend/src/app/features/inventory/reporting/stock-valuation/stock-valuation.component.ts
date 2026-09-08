import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { StockValuationReportService } from './stock-valuation.service';
import { StockValuationReport } from './stock-valuation.model';
import { InventoryLocationService } from '../../location/inventory-location.service';
import { InventoryLocation } from '../../location/inventory-location.model';
import { CmsEmptyStateComponent } from '../../../../shared/empty-state/empty-state.component';
import { ToastService } from '../../../../core/toast/toast.service';

@Component({
  selector: 'app-stock-valuation-report',
  standalone: true,
  imports: [
    FormsModule,
    DatePipe,
    DecimalPipe,
    MatProgressSpinnerModule,
    CmsEmptyStateComponent,
  ],
  templateUrl: './stock-valuation.component.html',
  styleUrl: './stock-valuation.component.scss',
})
export class StockValuationReportComponent implements OnInit {
  private readonly reportService   = inject(StockValuationReportService);
  private readonly locationService = inject(InventoryLocationService);
  private readonly toast           = inject(ToastService);

  protected readonly loading   = signal(false);
  protected readonly report    = signal<StockValuationReport | null>(null);
  protected readonly locations = signal<InventoryLocation[]>([]);

  protected locationFilter: number | null = null;

  ngOnInit(): void {
    this.locationService.getAll(true).subscribe({ next: (l) => this.locations.set(l) });
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.reportService.get(this.locationFilter).subscribe({
      next: (r) => { this.report.set(r); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load the stock valuation report'); this.loading.set(false); },
    });
  }
}

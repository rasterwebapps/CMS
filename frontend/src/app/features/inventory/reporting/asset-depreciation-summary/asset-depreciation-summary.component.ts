import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AssetDepreciationSummaryReportService } from './asset-depreciation-summary.service';
import { AssetDepreciationSummaryReport } from './asset-depreciation-summary.model';
import { CmsEmptyStateComponent } from '../../../../shared/empty-state/empty-state.component';
import { ToastService } from '../../../../core/toast/toast.service';

@Component({
  selector: 'app-asset-depreciation-summary-report',
  standalone: true,
  imports: [
    DatePipe,
    DecimalPipe,
    MatProgressSpinnerModule,
    CmsEmptyStateComponent,
  ],
  templateUrl: './asset-depreciation-summary.component.html',
  styleUrl: './asset-depreciation-summary.component.scss',
})
export class AssetDepreciationSummaryReportComponent implements OnInit {
  private readonly reportService = inject(AssetDepreciationSummaryReportService);
  private readonly toast         = inject(ToastService);

  protected readonly loading = signal(false);
  protected readonly report  = signal<AssetDepreciationSummaryReport | null>(null);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.reportService.get().subscribe({
      next: (r) => { this.report.set(r); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load the asset depreciation summary report'); this.loading.set(false); },
    });
  }
}

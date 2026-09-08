import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { BudgetVsActualReportService } from './budget-vs-actual.service';
import { BudgetVsActualReport } from './budget-vs-actual.model';
import { CmsEmptyStateComponent } from '../../../../shared/empty-state/empty-state.component';
import { ToastService } from '../../../../core/toast/toast.service';

@Component({
  selector: 'app-budget-vs-actual-report',
  standalone: true,
  imports: [
    DatePipe,
    DecimalPipe,
    MatProgressSpinnerModule,
    CmsEmptyStateComponent,
  ],
  templateUrl: './budget-vs-actual.component.html',
  styleUrl: './budget-vs-actual.component.scss',
})
export class BudgetVsActualReportComponent implements OnInit {
  private readonly reportService = inject(BudgetVsActualReportService);
  private readonly toast         = inject(ToastService);

  protected readonly loading = signal(false);
  protected readonly report  = signal<BudgetVsActualReport | null>(null);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.reportService.get().subscribe({
      next: (r) => { this.report.set(r); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load the budget vs. actual report'); this.loading.set(false); },
    });
  }
}

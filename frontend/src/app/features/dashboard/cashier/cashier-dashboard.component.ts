import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { formatCurrency } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/auth/auth.service';
import { environment } from '../../../../environments';
import { DashboardSummary } from '../dashboard.models';
import { DashboardKpiCardComponent, KpiTrend } from '../shared/kpi-card/dashboard-kpi-card.component';

interface CashierKpiCard {
  title: string;
  value: string;
  icon: string;
  color: 'sky' | 'amber' | 'teal' | 'emerald' | 'violet';
  subtitle: string;
  delay: string;
  trend?: KpiTrend;
}

/**
 * Cashier / Accountant dashboard.
 *
 * Displays finance-focused KPIs: fees collected this month, outstanding dues,
 * total payments recorded, and a quick-actions section for fee management tasks.
 */
@Component({
  selector: 'app-cashier-dashboard',
  standalone: true,
  imports: [RouterLink, MatIconModule, MatButtonModule, MatProgressSpinnerModule, DashboardKpiCardComponent],
  templateUrl: './cashier-dashboard.component.html',
  styleUrl: './cashier-dashboard.component.scss',
})
export class CashierDashboardComponent implements OnInit {
  protected readonly authService = inject(AuthService);
  private readonly http = inject(HttpClient);

  protected readonly loading = signal(true);
  protected readonly summary = signal<DashboardSummary | null>(null);

  protected readonly today = new Date().toLocaleDateString('en-IN', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });

  protected readonly kpiCards = computed((): CashierKpiCard[] => {
    const d = this.summary();
    const feeCollectedTrend = this.computeFeeCollectedTrend(d);
    const feeOutstandingTrend = this.computeFeeOutstandingTrend(d);
    return [
      {
        title: 'Fee Collected',
        value: d ? formatCurrency(d.feeCollectedThisMonth, 'en-IN', '₹', 'INR', '1.0-0') : '—',
        icon: 'payments',
        color: 'teal',
        subtitle: 'This month',
        delay: '0ms',
        trend: feeCollectedTrend,
      },
      {
        title: 'Fee Outstanding',
        value: d ? formatCurrency(d.feeOutstanding, 'en-IN', '₹', 'INR', '1.0-0') : '—',
        icon: 'account_balance',
        color: 'amber',
        subtitle: 'Pending amount',
        delay: '60ms',
        trend: feeOutstandingTrend,
      },
      {
        title: 'Total Payments',
        value: d ? String(d.totalFeePayments) : '—',
        icon: 'receipt_long',
        color: 'sky',
        subtitle: 'All time',
        delay: '120ms',
      },
      {
        title: 'Total Students',
        value: d ? String(d.totalStudents) : '—',
        icon: 'school',
        color: 'violet',
        subtitle: 'Active enrolled',
        delay: '180ms',
      },
    ];
  });

  ngOnInit(): void {
    this.http.get<DashboardSummary>(`${environment.apiUrl}/dashboard/summary`).subscribe({
      next: (data) => {
        this.summary.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  private computeFeeCollectedTrend(d: DashboardSummary | null): KpiTrend | undefined {
    if (!d || typeof d.feeCollectedDelta !== 'number') return undefined;
    const delta = d.feeCollectedDelta;
    if (Math.abs(delta) < 1) return { direction: 'neutral', label: 'No change vs last month' };
    return {
      direction: delta > 0 ? 'up' : 'down',
      label: (delta > 0 ? '+' : '') + delta.toFixed(0) + '% vs last month',
    };
  }

  private computeFeeOutstandingTrend(d: DashboardSummary | null): KpiTrend | undefined {
    if (!d || typeof d.feeOutstandingDelta !== 'number') return undefined;
    const delta = d.feeOutstandingDelta;
    if (Math.abs(delta) < 1) return { direction: 'neutral', label: 'No change vs last month' };
    // A decrease in outstanding is good (up direction = positive)
    return {
      direction: delta < 0 ? 'up' : 'down',
      label: Math.abs(delta).toFixed(0) + '% ' + (delta < 0 ? 'less' : 'more') + ' vs last month',
    };
  }
}


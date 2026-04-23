import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/auth/auth.service';
import { environment } from '../../../../environments';
import { FrontOfficeDashboard } from '../dashboard.models';
import { DashboardKpiCardComponent, KpiTrend } from '../shared/kpi-card/dashboard-kpi-card.component';

interface FoKpiCard {
  title: string;
  value: string;
  icon: string;
  color: 'sky' | 'amber' | 'teal' | 'emerald' | 'violet';
  subtitle: string;
  delay: string;
  trend?: KpiTrend;
}

@Component({
  selector: 'app-front-office-dashboard',
  standalone: true,
  imports: [
    RouterLink,
    DecimalPipe,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    DashboardKpiCardComponent,
  ],
  templateUrl: './front-office-dashboard.component.html',
  styleUrl: './front-office-dashboard.component.scss',
})
export class FrontOfficeDashboardComponent implements OnInit {
  protected readonly authService = inject(AuthService);
  private readonly http = inject(HttpClient);

  protected readonly loading = signal(true);
  protected readonly foData = signal<FrontOfficeDashboard | null>(null);

  protected readonly today = new Date().toLocaleDateString('en-IN', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });

  protected readonly kpiCards = computed((): FoKpiCard[] => {
    const d = this.foData();
    return [
      {
        title: "Today's Enquiries",
        value: d ? String(d.todayEnquiryCount) : '—',
        icon: 'contact_mail',
        color: 'sky',
        subtitle: 'Today',
        delay: '0ms',
      },
      {
        title: 'Pending Admissions',
        value: d ? String(d.pendingAdmissionsCount) : '—',
        icon: 'assignment_ind',
        color: 'amber',
        subtitle: 'Need attention',
        delay: '60ms',
      },
      {
        title: 'Fees Collected',
        value: d ? '₹' + d.feeCollectedToday.toLocaleString('en-IN') : '—',
        icon: 'payments',
        color: 'teal',
        subtitle: 'Today',
        delay: '120ms',
      },
      {
        title: 'Conversions',
        value: d ? String(d.conversionsThisWeek) : '—',
        icon: 'how_to_reg',
        color: 'emerald',
        subtitle: 'This week',
        delay: '180ms',
      },
      {
        title: 'Conversion Rate',
        value: d ? d.conversionRate.toFixed(1) + '%' : '—',
        icon: 'trending_up',
        color: 'violet',
        subtitle: 'Enquiry → Student',
        delay: '240ms',
        trend: this.conversionRateTrend(d),
      },
    ];
  });

  protected readonly funnelOrder = [
    'ENQUIRED',
    'INTERESTED',
    'FEES_FINALIZED',
    'FEES_PAID',
    'PARTIALLY_PAID',
    'DOCUMENTS_SUBMITTED',
    'CONVERTED',
    'ADMITTED',
  ];

  protected readonly funnelEntries = computed((): { status: string; count: number; pct: number }[] => {
    const d = this.foData();
    if (!d?.enquiryFunnel) return [];
    const funnel = d.enquiryFunnel;
    const max = Math.max(1, ...Object.values(funnel));
    return this.funnelOrder
      .filter((status) => funnel[status] !== undefined)
      .map((status) => ({
        status,
        count: funnel[status] ?? 0,
        pct: Math.round(((funnel[status] ?? 0) / max) * 100),
      }));
  });

  ngOnInit(): void {
    this.http.get<FrontOfficeDashboard>(`${environment.apiUrl}/dashboard/front-office`).subscribe({
      next: (data) => {
        this.foData.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  protected formatStatus(status: string): string {
    return status.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
  }

  protected statusColor(status: string): string {
    const map: Record<string, string> = {
      ENQUIRED: 'blue',
      INTERESTED: 'amber',
      FEES_FINALIZED: 'violet',
      FEES_PAID: 'teal',
      PARTIALLY_PAID: 'orange',
      DOCUMENTS_SUBMITTED: 'sky',
      CONVERTED: 'emerald',
      ADMITTED: 'green',
    };
    return map[status] ?? 'grey';
  }

  /**
   * Conversion-rate trend pill. Backend does not yet expose last week's rate,
   * so the helper returns `undefined` for now — the wiring is in place and the
   * pill will appear automatically once the field lands on `FrontOfficeDashboard`.
   */
  private conversionRateTrend(d: FrontOfficeDashboard | null): KpiTrend | undefined {
    if (!d) return undefined;
    const previous = d.conversionRateLastWeek;
    if (typeof previous !== 'number') return undefined;
    const delta = d.conversionRate - previous;
    if (Math.abs(delta) < 0.05) {
      return { direction: 'neutral', label: 'No change vs last week' };
    }
    return {
      direction: delta > 0 ? 'up' : 'down',
      label: (delta > 0 ? '+' : '') + delta.toFixed(1) + '% vs last week',
    };
  }
}

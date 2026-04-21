import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { DecimalPipe, KeyValuePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/auth/auth.service';
import { environment } from '../../../../environments';
import { DashboardSummary, DashboardTrends, DashboardTrendPoint } from '../dashboard.models';
import { DashboardKpiCardComponent } from '../shared/kpi-card/dashboard-kpi-card.component';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    RouterLink,
    DecimalPipe,
    KeyValuePipe,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    DashboardKpiCardComponent,
  ],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss',
})
export class AdminDashboardComponent implements OnInit {
  protected readonly authService = inject(AuthService);
  private readonly http = inject(HttpClient);

  protected readonly loading = signal(true);
  protected readonly trendsLoading = signal(true);
  protected readonly summary = signal<DashboardSummary | null>(null);
  protected readonly trends = signal<DashboardTrends | null>(null);

  protected readonly kpiCards = computed(() => {
    const s = this.summary();
    return [
      {
        title: 'Total Students',
        value: s ? String(s.totalStudents) : '—',
        icon: 'school',
        color: 'indigo' as const,
        subtitle: 'Enrolled',
        delay: '0ms',
      },
      {
        title: 'Active Faculty',
        value: s ? String(s.totalFaculty) : '—',
        icon: 'groups',
        color: 'emerald' as const,
        subtitle: 'Staff members',
        delay: '60ms',
      },
      {
        title: 'Labs',
        value: s ? String(s.totalLabs) : '—',
        icon: 'science',
        color: 'violet' as const,
        subtitle: 'Lab utilization',
        delay: '120ms',
      },
      {
        title: 'Fee Collected',
        value: s ? '₹' + s.feeCollectedThisMonth.toLocaleString('en-IN') : '—',
        icon: 'payments',
        color: 'teal' as const,
        subtitle: 'This month',
        delay: '180ms',
      },
      {
        title: 'Fee Outstanding',
        value: s ? '₹' + s.feeOutstanding.toLocaleString('en-IN') : '—',
        icon: 'account_balance_wallet',
        color: 'amber' as const,
        subtitle: 'Pending collection',
        delay: '240ms',
      },
    ];
  });

  protected readonly funnelOrder = [
    'ENQUIRED',
    'INTERESTED',
    'FEES_FINALIZED',
    'FEES_PAID',
    'DOCUMENTS_SUBMITTED',
    'CONVERTED',
    'ADMITTED',
  ];

  protected readonly funnelEntries = computed((): { status: string; count: number; pct: number }[] => {
    const s = this.summary();
    if (!s?.enquiryFunnel) return [];
    const funnel = s.enquiryFunnel;
    const max = Math.max(1, ...Object.values(funnel));
    return this.funnelOrder
      .filter((status) => funnel[status] !== undefined)
      .map((status) => ({
        status,
        count: funnel[status] ?? 0,
        pct: Math.round(((funnel[status] ?? 0) / max) * 100),
      }));
  });

  protected readonly pendingApprovals = computed(() => {
    const s = this.summary();
    if (!s) return [];
    const items: { message: string; severity: 'red' | 'amber' | 'blue'; link: string }[] = [];
    const pending = s.studentsByStatus?.['PENDING'] ?? 0;
    if (pending > 0) {
      items.push({ message: `${pending} student${pending > 1 ? 's' : ''} pending roll number`, severity: 'amber', link: '/students' });
    }
    const openMaint = s.maintenanceByStatus?.['OPEN'] ?? 0;
    if (openMaint > 0) {
      items.push({ message: `${openMaint} maintenance request${openMaint > 1 ? 's' : ''} open`, severity: 'red', link: '/maintenance' });
    }
    if (s.feeOutstanding > 0) {
      items.push({ message: `₹${s.feeOutstanding.toLocaleString('en-IN')} outstanding fee`, severity: 'amber', link: '/student-fees' });
    }
    return items;
  });

  protected readonly equipmentStatusEntries = computed(() => {
    const s = this.summary();
    if (!s?.equipmentByStatus) return [];
    const total = Math.max(1, Object.values(s.equipmentByStatus).reduce((a, b) => a + b, 0));
    return Object.entries(s.equipmentByStatus).map(([status, count]) => ({
      status,
      count,
      pct: Math.round((count / total) * 100),
    }));
  });

  // TODO: Replace with live academic calendar API
  protected readonly calendarPills = [
    { label: 'TODAY — Orientation Day', color: 'blue', date: 'Today' },
    { label: 'EXAM — Mid-Semester', color: 'amber', date: 'Next Mon' },
    { label: 'SAFETY — Fire Drill', color: 'red', date: 'Next Wed' },
    { label: 'FINANCE — Fee Deadline', color: 'emerald', date: 'Next Fri' },
    { label: 'EVENT — Annual Day', color: 'violet', date: 'Next Month' },
  ];

  ngOnInit(): void {
    this.http.get<DashboardSummary>(`${environment.apiUrl}/dashboard/summary`).subscribe({
      next: (data) => {
        this.summary.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });

    this.http.get<DashboardTrends>(`${environment.apiUrl}/dashboard/trends`).subscribe({
      next: (data) => {
        this.trends.set(data);
        this.trendsLoading.set(false);
      },
      error: () => this.trendsLoading.set(false),
    });
  }

  protected maxTrendValue(points: DashboardTrendPoint[]): number {
    return Math.max(1, ...points.map((p) => p.value));
  }

  protected trendBarHeight(value: number, max: number): number {
    return Math.round((value / max) * 100);
  }

  protected formatStatus(status: string): string {
    return status.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
  }

  protected equipmentStatusColor(status: string): string {
    const map: Record<string, string> = {
      OPERATIONAL: 'emerald',
      FAULTY: 'rose',
      IN_REPAIR: 'amber',
    };
    return map[status] ?? 'blue';
  }
}

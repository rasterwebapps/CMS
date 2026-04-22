import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { DecimalPipe, KeyValuePipe, DatePipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/auth/auth.service';
import { environment } from '../../../../environments';
import {
  DashboardSummary,
  DashboardTrends,
  DashboardTrendPoint,
  ActivityItem,
  CalendarEvent,
} from '../dashboard.models';
import {
  DashboardKpiCardComponent,
  KpiTrend,
} from '../shared/kpi-card/dashboard-kpi-card.component';
import { CmsSkeletonComponent } from '../../../shared/skeleton/skeleton.component';

interface AdminKpiCard {
  title: string;
  value: string;
  icon: string;
  color: 'indigo' | 'emerald' | 'amber' | 'teal' | 'violet';
  subtitle: string;
  delay: string;
  trend?: KpiTrend;
}

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    RouterLink,
    DecimalPipe,
    KeyValuePipe,
    DatePipe,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    DashboardKpiCardComponent,
    CmsSkeletonComponent,
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

  // ── Phase 4: Recent Activity feed ──────────────────────────────
  protected readonly activityLoading = signal(true);
  protected readonly activityFeed = signal<ActivityItem[]>([]);

  // ── Phase 4: Live Academic Calendar ────────────────────────────
  protected readonly calendarLoading = signal(true);
  protected readonly calendarEvents = signal<CalendarEvent[]>([]);

  protected readonly kpiCards = computed((): AdminKpiCard[] => {
    const s = this.summary();
    return [
      {
        title: 'Total Students',
        value: s ? String(s.totalStudents) : '—',
        icon: 'school',
        color: 'indigo',
        subtitle: 'Enrolled',
        delay: '0ms',
        trend: this.studentsTrend(s),
      },
      {
        title: 'Active Faculty',
        value: s ? String(s.totalFaculty) : '—',
        icon: 'groups',
        color: 'emerald',
        subtitle: 'Staff members',
        delay: '60ms',
      },
      {
        title: 'Labs',
        value: s ? String(s.totalLabs) : '—',
        icon: 'science',
        color: 'violet',
        subtitle: 'Lab utilization',
        delay: '120ms',
      },
      {
        title: 'Fee Collected',
        value: s ? '₹' + s.feeCollectedThisMonth.toLocaleString('en-IN') : '—',
        icon: 'payments',
        color: 'teal',
        subtitle: 'This month',
        delay: '180ms',
        trend: this.feeCollectedTrend(s),
      },
      {
        title: 'Fee Outstanding',
        value: s ? '₹' + s.feeOutstanding.toLocaleString('en-IN') : '—',
        icon: 'account_balance_wallet',
        color: 'amber',
        subtitle: 'Pending collection',
        delay: '240ms',
        trend: this.feeOutstandingTrend(s),
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

  // Live academic calendar replaces the previous hardcoded `calendarPills` array.
  // Data is loaded via the `/academic-years/current/events` endpoint in `ngOnInit`.

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

    this.loadActivity();
    this.loadCalendar();
  }

  /**
   * Loads (or reloads) the recent activity feed. Triggered on init and from the
   * widget's manual "Refresh" button. Failures are swallowed — the empty state
   * placeholder handles the missing-endpoint case gracefully.
   */
  protected refreshActivity(): void {
    this.loadActivity();
  }

  private loadActivity(): void {
    this.activityLoading.set(true);
    this.http
      .get<ActivityItem[]>(`${environment.apiUrl}/dashboard/activity`, { params: { limit: '10' } })
      .subscribe({
        next: (data) => {
          this.activityFeed.set(Array.isArray(data) ? data : []);
          this.activityLoading.set(false);
        },
        error: () => {
          this.activityFeed.set([]);
          this.activityLoading.set(false);
        },
      });
  }

  private loadCalendar(): void {
    this.calendarLoading.set(true);
    this.http
      .get<CalendarEvent[]>(`${environment.apiUrl}/academic-years/current/events`)
      .subscribe({
        next: (data) => {
          this.calendarEvents.set(Array.isArray(data) ? data : []);
          this.calendarLoading.set(false);
        },
        error: () => {
          this.calendarEvents.set([]);
          this.calendarLoading.set(false);
        },
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

  // ── KPI trend helpers ────────────────────────────────────────────
  // Backend currently does not yet expose month-over-month delta fields. When
  // those land on `DashboardSummary`, return a populated `KpiTrend`; until then
  // the helpers return `undefined` so the trend pill is hidden — wiring is in
  // place, but never lies about data we don't have.
  private studentsTrend(s: DashboardSummary | null): KpiTrend | undefined {
    const delta = (s as unknown as { studentsDelta?: number })?.studentsDelta;
    if (typeof delta !== 'number') return undefined;
    return {
      direction: delta > 0 ? 'up' : delta < 0 ? 'down' : 'neutral',
      label: (delta > 0 ? '+' : '') + delta + ' this month',
    };
  }

  private feeCollectedTrend(s: DashboardSummary | null): KpiTrend | undefined {
    const delta = (s as unknown as { feeCollectedDelta?: number })?.feeCollectedDelta;
    if (typeof delta !== 'number') return undefined;
    return {
      direction: delta >= 0 ? 'up' : 'down',
      label: (delta >= 0 ? '+' : '') + '₹' + Math.abs(delta).toLocaleString('en-IN') + ' vs last month',
    };
  }

  private feeOutstandingTrend(s: DashboardSummary | null): KpiTrend | undefined {
    const delta = (s as unknown as { feeOutstandingDelta?: number })?.feeOutstandingDelta;
    if (typeof delta !== 'number') return undefined;
    // Outstanding going DOWN is good — both directions render in green by design.
    return {
      direction: delta <= 0 ? 'down' : 'up',
      label: (delta <= 0 ? '−' : '+') + '₹' + Math.abs(delta).toLocaleString('en-IN') + ' vs last month',
    };
  }

  // ── Recent Activity helpers ──────────────────────────────────────
  /**
   * Returns a human-friendly relative time string for the activity feed.
   * Kept inline (no shared pipe) as this is the only screen that needs it —
   * extract to a `RelativeTimePipe` if/when a second consumer appears.
   */
  protected relativeTime(iso: string): string {
    const ts = Date.parse(iso);
    if (isNaN(ts)) return '';
    const diff = Date.now() - ts;
    if (diff < 60_000) return 'Just now';
    if (diff < 3_600_000) {
      const m = Math.floor(diff / 60_000);
      return `${m} minute${m === 1 ? '' : 's'} ago`;
    }
    if (diff < 86_400_000) {
      const h = Math.floor(diff / 3_600_000);
      return `${h} hour${h === 1 ? '' : 's'} ago`;
    }
    if (diff < 172_800_000) return 'Yesterday';
    return new Date(ts).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
  }

  protected activityIcon(entityType: ActivityItem['entityType']): string {
    const map: Record<ActivityItem['entityType'], string> = {
      ENQUIRY: 'contact_mail',
      ADMISSION: 'how_to_reg',
      STUDENT: 'school',
      PAYMENT: 'payments',
      DOCUMENT: 'description',
    };
    return map[entityType] ?? 'history';
  }

  // ── Calendar helpers ─────────────────────────────────────────────
  protected calendarColor(type: CalendarEvent['type']): string {
    const map: Record<CalendarEvent['type'], string> = {
      EXAM: 'amber',
      HOLIDAY: 'blue',
      DEADLINE: 'red',
      EVENT: 'violet',
      OTHER: 'gray',
    };
    return map[type] ?? 'gray';
  }

  /** True when the given ISO date (YYYY-MM-DD) is today in the user's timezone. */
  protected isToday(date: string): boolean {
    const d = new Date(date);
    if (isNaN(d.getTime())) return false;
    const now = new Date();
    return (
      d.getFullYear() === now.getFullYear() &&
      d.getMonth() === now.getMonth() &&
      d.getDate() === now.getDate()
    );
  }
}

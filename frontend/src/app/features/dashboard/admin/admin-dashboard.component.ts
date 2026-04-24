import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { AuthService } from '../../../core/auth/auth.service';
import { environment } from '../../../../environments';
import {
  DashboardSummary,
  DashboardTrendPoint,
  DashboardTrends,
} from '../dashboard.models';

/**
 * One row inside the "Pending Approvals" widget — title + subtitle + numeric amount.
 * Severities map directly to the bullet / amount accent colour in the design spec.
 */
interface ApprovalItem {
  title: string;
  subtitle: string;
  amount: string;
  severity: 'red' | 'amber' | 'accent';
}

/** Single bar in the Admission Trend chart with its `hi` / `lo` classification. */
interface TrendBar {
  label: string;
  height: number;
  emphasis: 'hi' | 'lo';
  value: number;
}

/** A single row in the Equipment Status widget. */
interface EquipmentRow {
  label: string;
  count: number;
  pct: number;
  color: 'green' | 'accent' | 'amber' | 'red';
}

/**
 * Admin dashboard.
 *
 * Layout follows `docs/college-management-v2.html` exactly: hero, 5-card stats grid,
 * quick-actions row, two 2-column rows (Trend + Approvals, Equipment + Fee).
 */
@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [RouterLink, DecimalPipe],
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

  // Academic year derived from today's date.
  // Indian academic years start in June (month index 5): June–December → year N,
  // January–May → year N-1.
  private readonly academicYearShort = computed(() => {
    const now = new Date();
    const year = now.getMonth() >= 5 ? now.getFullYear() : now.getFullYear() - 1;
    return `${year}–${String(year + 1).slice(-2)}`;
  });
  protected readonly academicYearLabel = computed(() => `Academic Year ${this.academicYearShort()}`);

  /** Optional "+N this month" pill rendered on the Total Students stat. */
  protected readonly studentsBadge = computed(() => {
    const delta = this.summary()?.studentsDelta;
    if (typeof delta !== 'number' || delta === 0) return null;
    return `${delta > 0 ? '+' : ''}${delta} this month`;
  });

  /**
   * Bars for the Admission Trend chart. A bar is rendered with the `hi` accent
   * style when its value is at or above the median of the series; everything
   * else is dimmed (`lo`). With no data we return an empty array — the empty
   * state is handled in the template.
   */
  protected readonly trendBars = computed((): TrendBar[] => {
    const points = this.trends()?.enrolmentTrend ?? [];
    if (points.length === 0) return [];
    const max = Math.max(1, ...points.map((p) => p.value));
    const sorted = [...points.map((p) => p.value)].sort((a, b) => a - b);
    const median = sorted[Math.floor(sorted.length / 2)];
    return points.map((p: DashboardTrendPoint) => ({
      label: p.month.split(' ')[0],
      height: Math.max(6, Math.round((p.value / max) * 100)),
      emphasis: p.value >= median ? 'hi' : 'lo',
      value: p.value,
    }));
  });

  /** Up to three rows for the Pending Approvals widget, derived from the summary payload. */
  protected readonly approvals = computed((): ApprovalItem[] => {
    const s = this.summary();
    if (!s) return [];
    const rows: ApprovalItem[] = [];

    if (s.feeOutstanding > 0) {
      const studentsWithDues = s.studentsByStatus?.['ACTIVE'] ?? s.totalStudents;
      rows.push({
        title: 'Outstanding Fee Balance',
        subtitle: `${studentsWithDues} student${studentsWithDues === 1 ? '' : 's'} · Immediate review required`,
        amount: '₹' + s.feeOutstanding.toLocaleString('en-IN'),
        severity: 'red',
      });
    }

    const openMaintenance = s.maintenanceByStatus?.['OPEN'] ?? 0;
    if (openMaintenance > 0) {
      rows.push({
        title: 'Maintenance Requests',
        subtitle: `${openMaintenance} open · Action required`,
        amount: String(openMaintenance),
        severity: 'amber',
      });
    }

    const newEnquiries = s.enquiryFunnel?.['ENQUIRED'] ?? 0;
    if (newEnquiries > 0) {
      rows.push({
        title: 'New Enrolment Requests',
        subtitle: `${newEnquiries} application${newEnquiries === 1 ? '' : 's'} · ${this.academicYearShort()} intake`,
        amount: String(newEnquiries),
        severity: 'accent',
      });
    }

    return rows.slice(0, 3);
  });

  /**
   * Equipment status rows used by the progress-bar widget. We re-map the raw
   * backend statuses to the three buckets shown in the design (Available /
   * In Use / Under Maintenance) so the colour assignment is deterministic.
   */
  protected readonly equipmentRows = computed((): EquipmentRow[] => {
    const s = this.summary();
    if (!s?.equipmentByStatus) return [];
    const buckets = s.equipmentByStatus;
    const total = Math.max(1, Object.values(buckets).reduce((a, b) => a + b, 0));
    const colorFor: Record<string, EquipmentRow['color']> = {
      OPERATIONAL: 'green',
      AVAILABLE: 'green',
      IN_USE: 'accent',
      ASSIGNED: 'accent',
      IN_REPAIR: 'amber',
      MAINTENANCE: 'amber',
      FAULTY: 'red',
      DECOMMISSIONED: 'red',
    };
    return Object.entries(buckets).map(([status, count]) => ({
      label: this.formatStatus(status),
      count,
      pct: Math.round((count / total) * 100),
      color: colorFor[status] ?? 'accent',
    }));
  });

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

  protected formatStatus(status: string): string {
    return status.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
  }
}

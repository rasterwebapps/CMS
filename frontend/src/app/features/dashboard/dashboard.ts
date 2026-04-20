import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DecimalPipe, KeyValuePipe } from '@angular/common';
import { AuthService } from '../../core/auth/auth.service';
import { environment } from '../../../environments';

export interface DashboardSummary {
  totalStudents: number;
  totalFaculty: number;
  totalDepartments: number;
  totalSubjects: number;
  totalPrograms: number;
  totalLabs: number;
  totalEquipment: number;
  totalExaminations: number;
  totalFeePayments: number;
  totalMaintenanceRequests: number;
  totalAttendanceRecords: number;
  equipmentByStatus: Record<string, number>;
  maintenanceByStatus: Record<string, number>;
  studentsByStatus: Record<string, number>;
  attendanceByStatus: Record<string, number>;
  enquiryFunnel: Record<string, number>;
  feeCollectedThisMonth: number;
  feeOutstanding: number;
}

export interface DashboardTrendPoint {
  month: string;
  value: number;
}

export interface DashboardTrends {
  enrolmentTrend: DashboardTrendPoint[];
  feeCollectionTrend: DashboardTrendPoint[];
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [MatCardModule, MatIconModule, MatProgressSpinnerModule, DecimalPipe, KeyValuePipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class DashboardComponent implements OnInit {
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
        color: 'indigo',
        sub: 'Enrolled',
      },
      {
        title: 'Total Faculty',
        value: s ? String(s.totalFaculty) : '—',
        icon: 'groups',
        color: 'emerald',
        sub: 'Active',
      },
      {
        title: 'Departments',
        value: s ? String(s.totalDepartments) : '—',
        icon: 'business',
        color: 'amber',
        sub: 'Active',
      },
      {
        title: 'Fee Collected',
        value: s ? '₹' + s.feeCollectedThisMonth.toLocaleString('en-IN') : '—',
        icon: 'payments',
        color: 'teal',
        sub: 'This month',
      },
    ];
  });

  /** Enquiry funnel statuses to display in order */
  protected readonly funnelOrder = [
    'ENQUIRED',
    'INTERESTED',
    'FEES_FINALIZED',
    'FEES_PAID',
    'DOCUMENTS_SUBMITTED',
    'CONVERTED',
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

  ngOnInit(): void {
    this.http.get<DashboardSummary>(`${environment.apiUrl}/dashboard/summary`).subscribe({
      next: (data) => {
        this.summary.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });

    this.http.get<DashboardTrends>(`${environment.apiUrl}/dashboard/trends`).subscribe({
      next: (data) => {
        this.trends.set(data);
        this.trendsLoading.set(false);
      },
      error: () => {
        this.trendsLoading.set(false);
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
}


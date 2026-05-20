import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { InrPipe } from '../../../../shared/pipes/inr.pipe';
import { environment } from '../../../../../environments';
import { INDIA_STATE_PATHS } from './india-state-paths';

type Severity = 'green' | 'amber' | 'red' | 'accent' | string;

interface GeoAdmissionBucket { label: string; state: string; district: string; count: number; pct: number; }
interface GeoStatePath { code: string; name: string; d: string; cx: number; cy: number; showLabel: boolean; count: number; pct: number; }
interface YoyAdmissionMonth { month: string; thisYear: number; lastYear: number; twoYearsAgo: number; }
interface MonthlyRatePoint { month: string; refundPct: number; cancellationPct: number; }
interface RefundCancellationData {
  totalPayments: number; refundedPayments: number; refundRatePct: number;
  totalStudents: number; withdrawnStudents: number; cancellationRatePct: number;
  trend: MonthlyRatePoint[];
}
interface PaymentModeSlice { mode: string; count: number; amount: number; sharePct: number; }
interface StudentFacultyRatioRow {
  departmentName: string; departmentCode: string; students: number; faculty: number; ratio: number; severity: Severity;
}
interface UtilizationHeatCell { day: string; slot: string; bookings: number; intensityPct: number; }
interface LabUtilizationData { days: string[]; slots: string[]; cells: UtilizationHeatCell[]; totalScheduledSessions: number; }
interface CohortRetentionTerm { termNumber: number; enrolled: number; active: number; retentionPct: number; }
interface CohortRetentionRow { cohortCode: string; cohortName: string; baseline: number; terms: CohortRetentionTerm[]; }
interface TopLineKpi { key: string; label: string; value: string; helper: string; severity: Severity; }

const ENDPOINT_BY_KEY: Record<string, string> = {
  'geographic-admissions': 'geographic-admissions',
  'yoy-admissions': 'yoy-admissions',
  'refund-cancellation-rate': 'refund-cancellation-rate',
  'payment-mode-breakdown': 'payment-mode-breakdown',
  'student-faculty-ratio': 'student-faculty-ratio',
  'lab-utilization-heatmap': 'lab-utilization-heatmap',
  'cohort-retention': 'cohort-retention',
  'top-line-kpis': 'top-line-kpis',
};

const PALETTE = ['#A78BFA', '#22C55E', '#38BDF8', '#F59E0B', '#EC4899', '#06B6D4', '#EF4444'];

const STATE_ALIASES: Record<string, string> = {
  'ANDAMAN & NICOBAR': 'ANDAMAN AND NICOBAR',
  'CHATTISGARH': 'CHHATTISGARH',
  'DADRA & NAGAR HAVELI': 'DADRA AND NAGAR HAVELI',
  'DAMAN AND DIU': 'DAMAN AND DIU',
  'JAMMU & KASHMIR': 'JAMMU AND KASHMIR',
  'NCT OF DELHI': 'DELHI',
  'ORISSA': 'ORISSA',
  'PONDICHERRY': 'PUDUCHERRY',
  'TAMILNADU': 'TAMIL NADU',
  'TELENGANA': 'TELANGANA',
  'UTTARANCHAL': 'UTTARANCHAL',
  'ODISHA': 'ORISSA',
  'UTTARAKHAND': 'UTTARANCHAL',
};

@Component({
  selector: 'dash-widget-tier3-strategic',
  standalone: true,
  imports: [MatIconModule, InrPipe],
  templateUrl: './tier3-strategic-widget.component.html',
  styleUrl: './tier3-strategic-widget.component.scss',
})
export class Tier3StrategicWidgetComponent implements OnInit {
  @Input() widgetKey?: string;
  @Input() widgetLabel?: string;
  @Input() widgetIcon?: string;

  protected readonly Math = Math;

  private readonly http = inject(HttpClient);

  protected readonly loading = signal(true);
  protected readonly error = signal(false);
  protected readonly data = signal<unknown>(null);

  protected readonly icon = computed(() => this.widgetIcon ?? 'analytics');
  protected readonly title = computed(() => this.widgetLabel ?? 'Strategic Insight');

  protected readonly geoRows = computed(() => this.asArray<GeoAdmissionBucket>());
  protected readonly topGeoRows = computed(() => this.geoRows().slice(0, 5));
  protected readonly geoStatePaths = computed((): GeoStatePath[] => {
    const byState = new Map<string, number>();
    for (const row of this.geoRows()) {
      const key = this.normaliseState(row.state);
      byState.set(key, (byState.get(key) ?? 0) + (row.count || 0));
    }
    const max = Math.max(1, ...Array.from(byState.values()));
    return INDIA_STATE_PATHS.map(s => {
      const count = byState.get(this.normaliseState(s.name)) ?? 0;
      return { ...s, count, pct: Math.round((count / max) * 100) };
    });
  });
  protected readonly yoyRows = computed(() => this.asArray<YoyAdmissionMonth>());
  protected readonly refundData = computed(() => this.data() as RefundCancellationData | null);
  protected readonly paymentSlices = computed(() => this.asArray<PaymentModeSlice>());
  protected readonly ratioRows = computed(() => this.asArray<StudentFacultyRatioRow>());
  protected readonly labData = computed(() => this.data() as LabUtilizationData | null);
  protected readonly cohortRows = computed(() => this.asArray<CohortRetentionRow>());
  protected readonly kpis = computed(() => this.asArray<TopLineKpi>());

  protected readonly yoyMax = computed(() => Math.max(1, ...this.yoyRows().flatMap(r => [r.thisYear, r.lastYear, r.twoYearsAgo])));
  protected readonly paymentTotal = computed(() => this.paymentSlices().reduce((s, x) => s + (x.amount || 0), 0));

  protected readonly paymentDonut = computed(() => {
    let cursor = 0;
    const stops = this.paymentSlices().map((slice, i) => {
      const start = cursor;
      const end = cursor + slice.sharePct;
      cursor = end;
      return `${PALETTE[i % PALETTE.length]} ${start}% ${end}%`;
    });
    if (cursor < 100) stops.push(`rgba(255,255,255,.06) ${cursor}% 100%`);
    return `conic-gradient(${stops.join(', ')})`;
  });

  protected colour(index: number): string { return PALETTE[index % PALETTE.length]; }

  protected barHeight(value: number): number {
    return Math.max(5, Math.round((value / this.yoyMax()) * 100));
  }

  protected heatCell(day: string, slot: string): UtilizationHeatCell | undefined {
    return this.labData()?.cells.find(c => c.day === day && c.slot === slot);
  }

  /** Returns fill-opacity (0–1) for a state based on admission percentage.
   *  Zero-admission states get 0.06 (subtle base), max gets ~0.85. */
  protected stateFillOpacity(pct: number): number {
    return pct === 0 ? 0.06 : +(0.12 + (pct / 100) * 0.73).toFixed(2);
  }

  ngOnInit(): void {
    const endpoint = this.widgetKey ? ENDPOINT_BY_KEY[this.widgetKey] : undefined;
    if (!endpoint) {
      this.error.set(true);
      this.loading.set(false);
      return;
    }

    this.http.get<unknown>(`${environment.apiUrl}/dashboard/data/${endpoint}`).subscribe({
      next: d => { this.data.set(d); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }

  private asArray<T>(): T[] {
    const value = this.data();
    return Array.isArray(value) ? (value as T[]) : [];
  }

  private normaliseState(value: string | null | undefined): string {
    const cleaned = (value ?? '')
      .trim()
      .replace(/\s+/g, ' ')
      .toUpperCase();
    return STATE_ALIASES[cleaned] ?? cleaned;
  }
}



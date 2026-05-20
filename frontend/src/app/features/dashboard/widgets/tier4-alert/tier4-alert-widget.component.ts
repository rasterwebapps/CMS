import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { InrPipe } from '../../../../shared/pipes/inr.pipe';
import { AppDatePipe } from '../../../../shared/pipes/app-date.pipe';
import { environment } from '../../../../../environments';

type Severity = 'green' | 'amber' | 'red' | 'accent' | string;

interface AnomalyBannerData {
  todayCollection: number;
  sameDayLastWeekCollection: number;
  deltaPct: number;
  direction: 'up' | 'down' | 'flat';
  message: string;
  severity: Severity;
}

interface CapacityAlertRow {
  programName: string;
  programCode: string;
  filled: number;
  capacity: number;
  occupancyPct: number;
  seatsLeft: number;
  severity: Severity;
}

interface ComplianceAlertRow {
  authority: string;
  documentName: string;
  referenceNumber: string | null;
  expiresOn: string;
  daysLeft: number;
  severity: Severity;
}

interface AuditMiniFeedItem {
  actor: string;
  action: string;
  entityType: string | null;
  entityId: string | null;
  detail: string | null;
  occurredAt: string;
  severity: Severity;
}

const ENDPOINT_BY_KEY: Record<string, string> = {
  'anomaly-banner': 'anomaly-banner',
  'capacity-alert': 'capacity-alert',
  'compliance-alerts': 'compliance-alerts',
  'audit-mini-feed': 'audit-mini-feed',
};

@Component({
  selector: 'dash-widget-tier4-alert',
  standalone: true,
  imports: [MatIconModule, InrPipe, AppDatePipe],
  templateUrl: './tier4-alert-widget.component.html',
  styleUrl: './tier4-alert-widget.component.scss',
})
export class Tier4AlertWidgetComponent implements OnInit {
  @Input() widgetKey?: string;
  @Input() widgetLabel?: string;
  @Input() widgetIcon?: string;

  private readonly http = inject(HttpClient);

  protected readonly loading = signal(true);
  protected readonly error = signal(false);
  protected readonly data = signal<unknown>(null);

  protected readonly title = computed(() => this.widgetLabel ?? 'Alert');
  protected readonly icon = computed(() => this.widgetIcon ?? 'notification_important');

  protected readonly anomaly = computed(() => this.data() as AnomalyBannerData | null);
  protected readonly capacityRows = computed(() => this.asArray<CapacityAlertRow>());
  protected readonly complianceRows = computed(() => this.asArray<ComplianceAlertRow>());
  protected readonly auditItems = computed(() => this.asArray<AuditMiniFeedItem>());

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
}


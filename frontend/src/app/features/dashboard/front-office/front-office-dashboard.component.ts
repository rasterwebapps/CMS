import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { AuthService } from '../../../core/auth/auth.service';
import { environment } from '../../../../environments';
import { FrontOfficeDashboard } from '../dashboard.models';

/**
 * Front Office dashboard.
 *
 * Layout follows `docs/college-management-v2.html` design language (same token
 * mapping and component patterns as the Admin dashboard) with FO-specific content:
 * shift banner → 4 KPI stat cards → 2-column grid (enquiry log + right column with
 * pending actions, admission funnel and quick-action tiles).
 */
@Component({
  selector: 'app-front-office-dashboard',
  standalone: true,
  imports: [RouterLink, DecimalPipe],
  templateUrl: './front-office-dashboard.component.html',
  styleUrl: './front-office-dashboard.component.scss',
})
export class FrontOfficeDashboardComponent implements OnInit {
  protected readonly authService = inject(AuthService);
  private readonly http = inject(HttpClient);

  protected readonly loading = signal(true);
  protected readonly foData = signal<FrontOfficeDashboard | null>(null);

  /** Human-readable date string shown in the shift banner. */
  protected readonly today = new Date().toLocaleDateString('en-IN', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });

  /** Derives a shift label from the current hour (Morning / Afternoon / Evening). */
  protected readonly shiftLabel = computed(() => {
    const h = new Date().getHours();
    if (h < 12) return 'Morning Shift';
    if (h < 17) return 'Afternoon Shift';
    return 'Evening Shift';
  });

  /** Number of pending action strings from the API payload. */
  protected readonly pendingCount = computed(
    () => this.foData()?.pendingActionItems?.length ?? 0,
  );

  private readonly funnelOrder = [
    'ENQUIRED',
    'INTERESTED',
    'FEES_FINALIZED',
    'FEES_PAID',
    'PARTIALLY_PAID',
    'DOCUMENTS_SUBMITTED',
    'CONVERTED',
    'ADMITTED',
  ];

  /** Admission funnel entries sorted by the canonical pipeline order. */
  protected readonly funnelEntries = computed(
    (): { status: string; count: number; pct: number }[] => {
      const d = this.foData();
      if (!d?.enquiryFunnel) return [];
      const funnel = d.enquiryFunnel;
      const max = Math.max(1, ...Object.values(funnel));
      return this.funnelOrder
        .filter((status) => (funnel[status] ?? 0) > 0)
        .map((status) => ({
          status,
          count: funnel[status] ?? 0,
          pct: Math.round(((funnel[status] ?? 0) / max) * 100),
        }));
    },
  );

  ngOnInit(): void {
    this.http
      .get<FrontOfficeDashboard>(`${environment.apiUrl}/dashboard/front-office`)
      .subscribe({
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
}

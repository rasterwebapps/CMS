import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { NgApexchartsModule } from 'ng-apexcharts';
import type { ApexNonAxisChartSeries, ApexChart, ApexTooltip,
              ApexLegend, ApexPlotOptions, ApexDataLabels } from 'ng-apexcharts';
import { environment } from '../../../../../environments';
import { InrPipe } from '../../../../shared/pipes/inr.pipe';

interface FeeOverviewData {
  collectedThisMonth: number;
  outstanding:        number;
  totalPayments:      number;
  enquiriesThisMonth: number;
  admissionsThisMonth:number;
}

@Component({
  selector: 'dash-widget-fee-overview',
  standalone: true,
  imports: [MatIconModule, RouterLink, InrPipe, NgApexchartsModule],
  templateUrl: './fee-overview-widget.component.html',
  styleUrl:    './fee-overview-widget.component.scss',
  host: { '[style.--ca]': '"#f59e0b"' },
})
export class FeeOverviewWidgetComponent implements OnInit {
  @Input() widgetKey?:   string;
  @Input() widgetLabel?: string;
  @Input() widgetIcon?:  string;

  private readonly http = inject(HttpClient);

  protected readonly loading = signal(true);
  protected readonly error   = signal(false);
  protected readonly data    = signal<FeeOverviewData | null>(null);

  private readonly isDark = signal(
    document.documentElement.classList.contains('dark-theme')
  );

  protected readonly chartSeries = computed((): ApexNonAxisChartSeries => {
    const d = this.data();
    if (!d) return [];
    return [d.collectedThisMonth, d.outstanding];
  });

  protected readonly apexChart: ApexChart = {
    type: 'donut',
    height: 160,
    toolbar: { show: false },
    animations: { enabled: true, speed: 700 },
    background: 'transparent',
  };

  protected readonly apexPlotOptions: ApexPlotOptions = {
    pie: {
      donut: {
        size: '72%',
        labels: {
          show: true,
          total: {
            show: true,
            label: 'Collected',
            fontSize: '10px',
            fontFamily: 'Inter, sans-serif',
            fontWeight: 600,
            formatter: (w) => {
              const collected = w.globals.seriesTotals[0] ?? 0;
              const total = w.globals.seriesTotals.reduce((a: number, b: number) => a + b, 0);
              if (!total) return '0%';
              return `${Math.round((collected / total) * 100)}%`;
            },
          },
          value: {
            fontSize: '16px',
            fontFamily: 'Inter, sans-serif',
            fontWeight: 700,
            offsetY: -2,
            color: '#f59e0b',
          },
        },
      },
    },
  };

  protected readonly apexDataLabels: ApexDataLabels = { enabled: false };

  protected readonly apexLegend: ApexLegend = { show: false };

  protected get apexTooltip(): ApexTooltip {
    return {
      theme: this.isDark() ? 'dark' : 'light',
      y: {
        formatter: (v: number) =>
          new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(v),
      },
    };
  }

  ngOnInit(): void {
    const obs = new MutationObserver(() =>
      this.isDark.set(document.documentElement.classList.contains('dark-theme'))
    );
    obs.observe(document.documentElement, { attributeFilter: ['class'] });

    this.http.get<FeeOverviewData>(`${environment.apiUrl}/dashboard/data/fee-overview`).subscribe({
      next:  d  => { this.data.set(d); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }
}

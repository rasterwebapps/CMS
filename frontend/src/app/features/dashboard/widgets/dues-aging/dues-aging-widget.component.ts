import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { InrPipe } from '../../../../shared/pipes/inr.pipe';
import { NgApexchartsModule } from 'ng-apexcharts';
import type { ApexAxisChartSeries, ApexChart, ApexXAxis, ApexYAxis,
              ApexTooltip, ApexFill, ApexGrid, ApexDataLabels,
              ApexPlotOptions } from 'ng-apexcharts';
import { environment } from '../../../../../environments';

interface DuesAgingBucket {
  label: string;
  demandCount: number;
  amount: number;
  severity: 'amber' | 'red';
}

@Component({
  selector: 'dash-widget-dues-aging',
  standalone: true,
  imports: [MatIconModule, InrPipe, NgApexchartsModule],
  templateUrl: './dues-aging-widget.component.html',
  styleUrl:    './dues-aging-widget.component.scss',
})
export class DuesAgingWidgetComponent implements OnInit {
  @Input() widgetKey?:   string;
  @Input() widgetLabel?: string;
  @Input() widgetIcon?:  string;

  private readonly http = inject(HttpClient);

  protected readonly loading = signal(true);
  protected readonly error   = signal(false);
  protected readonly buckets = signal<DuesAgingBucket[]>([]);

  private readonly isDark = signal(
    document.documentElement.classList.contains('dark-theme')
  );

  protected readonly totalOutstanding = computed(() =>
    this.buckets().reduce((s, b) => s + (b.amount || 0), 0)
  );

  protected readonly totalCount = computed(() =>
    this.buckets().reduce((s, b) => s + (b.demandCount || 0), 0)
  );

  protected readonly chartSeries = computed((): ApexAxisChartSeries => [{
    name: 'Amount (₹)',
    data: this.buckets().map(b => b.amount),
  }]);

  protected readonly chartCategories = computed(() =>
    this.buckets().map(b => b.label)
  );

  protected readonly chartColors = computed(() =>
    this.buckets().map(b => b.severity === 'red' ? '#EF4444' : '#F59E0B')
  );

  protected readonly apexChart: ApexChart = {
    type: 'bar',
    height: '100%',
    toolbar: { show: false },
    animations: { enabled: true, speed: 500 },
    background: 'transparent',
  };

  protected readonly apexPlotOptions: ApexPlotOptions = {
    bar: { horizontal: true, borderRadius: 4, borderRadiusApplication: 'end', distributed: true },
  };

  protected readonly apexDataLabels: ApexDataLabels = { enabled: false };

  protected readonly apexFill: ApexFill = { type: 'solid', opacity: 0.85 };

  protected readonly apexGrid: ApexGrid = {
    borderColor: 'rgba(255,255,255,0.05)',
    strokeDashArray: 3,
    xaxis: { lines: { show: true } },
    yaxis: { lines: { show: false } },
    padding: { left: 0, right: 8, top: 0, bottom: 0 },
  };

  protected get apexXAxis(): ApexXAxis {
    return {
      axisBorder: { show: false },
      axisTicks: { show: false },
      labels: {
        style: {
          fontSize: '10px',
          fontFamily: 'Inter, sans-serif',
          colors: this.isDark() ? 'rgba(255,255,255,0.35)' : 'rgba(0,0,0,0.35)',
        },
        formatter: (v: string) => {
          const n = Number(v);
          if (n >= 100000) return `₹${(n/100000).toFixed(1)}L`;
          if (n >= 1000) return `₹${(n/1000).toFixed(0)}K`;
          return `₹${n}`;
        },
      },
    };
  }

  protected get apexYAxis(): ApexYAxis {
    return {
      labels: {
        style: {
          fontSize: '11px',
          fontFamily: 'Inter, sans-serif',
          colors: this.isDark() ? 'rgba(255,255,255,0.65)' : 'rgba(0,0,0,0.65)',
          fontWeight: 600,
        },
      },
    };
  }

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

    this.http.get<DuesAgingBucket[]>(`${environment.apiUrl}/dashboard/data/dues-aging`).subscribe({
      next:  d  => { this.buckets.set(d ?? []); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }
}

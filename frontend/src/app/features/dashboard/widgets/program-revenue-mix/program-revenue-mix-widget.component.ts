import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { InrPipe } from '../../../../shared/pipes/inr.pipe';
import { NgApexchartsModule } from 'ng-apexcharts';
import type { ApexNonAxisChartSeries, ApexChart, ApexTooltip,
              ApexLegend, ApexPlotOptions, ApexDataLabels } from 'ng-apexcharts';
import { environment } from '../../../../../environments';

interface ProgramRevenueSlice {
  programName: string;
  programCode: string;
  netRevenue: number;
  sharePct: number;
}

const PALETTE = ['#A78BFA', '#22C55E', '#38BDF8', '#F59E0B', '#EC4899', '#06B6D4'];

@Component({
  selector: 'dash-widget-program-revenue-mix',
  standalone: true,
  imports: [MatIconModule, InrPipe, NgApexchartsModule],
  templateUrl: './program-revenue-mix-widget.component.html',
  styleUrl:    './program-revenue-mix-widget.component.scss',
})
export class ProgramRevenueMixWidgetComponent implements OnInit {
  @Input() widgetKey?:   string;
  @Input() widgetLabel?: string;
  @Input() widgetIcon?:  string;

  private readonly http = inject(HttpClient);

  protected readonly loading = signal(true);
  protected readonly error   = signal(false);
  protected readonly slices  = signal<ProgramRevenueSlice[]>([]);

  private readonly isDark = signal(
    document.documentElement.classList.contains('dark-theme')
  );

  protected readonly grandTotal = computed(() =>
    this.slices().reduce((s, x) => s + (x.netRevenue || 0), 0)
  );

  protected readonly chartSeries = computed((): ApexNonAxisChartSeries =>
    this.slices().map(s => s.netRevenue)
  );

  protected readonly chartLabels = computed(() =>
    this.slices().map(s => s.programCode)
  );

  protected readonly apexChart: ApexChart = {
    type: 'donut',
    height: 170,
    toolbar: { show: false },
    animations: { enabled: true, speed: 700 },
    background: 'transparent',
  };

  protected readonly apexPlotOptions: ApexPlotOptions = {
    pie: {
      donut: {
        size: '68%',
        labels: {
          show: true,
          total: {
            show: true,
            label: 'Net Revenue',
            fontSize: '10px',
            fontFamily: 'Inter, sans-serif',
            fontWeight: 600,
          },
          value: {
            fontSize: '14px',
            fontFamily: 'Inter, sans-serif',
            fontWeight: 700,
            offsetY: -2,
            formatter: (v: string) =>
              new Intl.NumberFormat('en-IN', {
                style: 'currency', currency: 'INR',
                notation: 'compact', maximumFractionDigits: 1,
              }).format(Number(v)),
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

  protected readonly legend = computed(() =>
    this.slices().map((sl, i) => ({ ...sl, colour: PALETTE[i % PALETTE.length] }))
  );

  ngOnInit(): void {
    const obs = new MutationObserver(() =>
      this.isDark.set(document.documentElement.classList.contains('dark-theme'))
    );
    obs.observe(document.documentElement, { attributeFilter: ['class'] });

    this.http.get<ProgramRevenueSlice[]>(`${environment.apiUrl}/dashboard/data/program-revenue-mix`).subscribe({
      next:  d  => { this.slices.set(d ?? []); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }
}

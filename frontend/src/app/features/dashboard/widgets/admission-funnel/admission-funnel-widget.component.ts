import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { NgApexchartsModule } from 'ng-apexcharts';
import type { ApexAxisChartSeries, ApexChart, ApexXAxis, ApexYAxis,
              ApexTooltip, ApexFill, ApexGrid, ApexDataLabels,
              ApexPlotOptions } from 'ng-apexcharts';
import { environment } from '../../../../../environments';

interface FunnelStage { key: string; label: string; count: number; conversionPct: number | null; }
interface AdmissionFunnelData { stages: FunnelStage[]; overallConversionPct: number; }

@Component({
  selector: 'dash-widget-admission-funnel',
  standalone: true,
  imports: [MatIconModule, NgApexchartsModule],
  templateUrl: './admission-funnel-widget.component.html',
  styleUrl:    './admission-funnel-widget.component.scss',
})
export class AdmissionFunnelWidgetComponent implements OnInit {
  @Input() widgetKey?:   string;
  @Input() widgetLabel?: string;
  @Input() widgetIcon?:  string;

  private readonly http = inject(HttpClient);

  protected readonly loading = signal(true);
  protected readonly error   = signal(false);
  protected readonly data    = signal<AdmissionFunnelData | null>(null);

  private readonly isDark = signal(
    document.documentElement.classList.contains('dark-theme')
  );

  protected readonly chartSeries = computed((): ApexAxisChartSeries => [{
    name: 'Count',
    data: (this.data()?.stages ?? []).map(s => s.count),
  }]);

  protected readonly chartCategories = computed(() =>
    (this.data()?.stages ?? []).map(s => s.label)
  );

  protected readonly apexChart: ApexChart = {
    type: 'bar',
    height: '100%',
    toolbar: { show: false },
    animations: { enabled: true, speed: 500 },
    background: 'transparent',
  };

  protected readonly apexPlotOptions: ApexPlotOptions = {
    bar: {
      horizontal: true,
      borderRadius: 4,
      borderRadiusApplication: 'end',
      dataLabels: { position: 'top' },
    },
  };

  protected readonly apexDataLabels: ApexDataLabels = {
    enabled: true,
    offsetX: -6,
    style: {
      fontSize: '10px',
      fontFamily: 'Inter, sans-serif',
      fontWeight: 600,
      colors: ['rgba(255,255,255,0.9)'],
    },
  };

  protected readonly apexFill: ApexFill = {
    type: 'gradient',
    gradient: {
      type: 'horizontal',
      shadeIntensity: 0.5,
      gradientToColors: ['var(--cms-primary)'],
      opacityFrom: 0.9,
      opacityTo: 0.6,
      stops: [0, 100],
    },
  };

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
      },
    };
  }

  protected get apexYAxis(): ApexYAxis {
    return {
      labels: {
        style: {
          fontSize: '10px',
          fontFamily: 'Inter, sans-serif',
          colors: this.isDark() ? 'rgba(255,255,255,0.55)' : 'rgba(0,0,0,0.55)',
          fontWeight: 500,
        },
        maxWidth: 100,
      },
    };
  }

  protected get apexTooltip(): ApexTooltip {
    return {
      theme: this.isDark() ? 'dark' : 'light',
      x: { show: false },
      y: { formatter: (v: number) => `${v} students` },
    };
  }

  ngOnInit(): void {
    const obs = new MutationObserver(() =>
      this.isDark.set(document.documentElement.classList.contains('dark-theme'))
    );
    obs.observe(document.documentElement, { attributeFilter: ['class'] });

    this.http.get<AdmissionFunnelData>(`${environment.apiUrl}/dashboard/data/admission-funnel`).subscribe({
      next:  d  => { this.data.set(d); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }
}

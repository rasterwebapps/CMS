import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { NgApexchartsModule } from 'ng-apexcharts';
import type { ApexAxisChartSeries, ApexChart, ApexXAxis, ApexYAxis,
              ApexTooltip, ApexStroke, ApexFill, ApexGrid, ApexDataLabels,
              ApexMarkers } from 'ng-apexcharts';
import { environment } from '../../../../../environments';

interface TrendPoint { month: string; value: number; }

@Component({
  selector: 'dash-widget-trend-chart',
  standalone: true,
  imports: [MatIconModule, RouterLink, NgApexchartsModule],
  templateUrl: './trend-chart-widget.component.html',
  styleUrl:    './trend-chart-widget.component.scss',
})
export class TrendChartWidgetComponent implements OnInit {
  @Input() widgetKey?:   string;
  @Input() widgetLabel?: string;
  @Input() widgetIcon?:  string;

  private readonly http = inject(HttpClient);

  protected readonly loading = signal(true);
  protected readonly error   = signal(false);
  protected readonly points  = signal<TrendPoint[]>([]);

  private readonly isDark = signal(
    document.documentElement.classList.contains('dark-theme')
  );

  protected readonly chartSeries = computed((): ApexAxisChartSeries => [{
    name: 'Admissions',
    data: this.points().map(p => p.value),
  }]);

  protected readonly chartCategories = computed(() =>
    this.points().map(p => p.month.split(' ')[0])
  );

  protected readonly apexChart: ApexChart = {
    type: 'area',
    height: '100%',
    toolbar: { show: false },
    zoom: { enabled: false },
    animations: {
      enabled: true,
      speed: 600,
      animateGradually: { enabled: true, delay: 80 },
      dynamicAnimation: { enabled: true, speed: 350 },
    },
    sparkline: { enabled: false },
    background: 'transparent',
  };

  protected readonly apexStroke: ApexStroke = {
    curve: 'smooth',
    width: 2.5,
  };

  protected readonly apexFill: ApexFill = {
    type: 'gradient',
    gradient: {
      shadeIntensity: 1,
      opacityFrom: 0.45,
      opacityTo: 0.02,
      stops: [0, 90, 100],
    },
  };

  protected readonly apexGrid: ApexGrid = {
    borderColor: 'rgba(255,255,255,0.06)',
    strokeDashArray: 4,
    xaxis: { lines: { show: false } },
    yaxis: { lines: { show: true } },
    padding: { left: 4, right: 4, top: 0, bottom: 0 },
  };

  protected readonly apexDataLabels: ApexDataLabels = { enabled: false };

  protected readonly apexMarkers: ApexMarkers = {
    size: 3,
    strokeWidth: 0,
    hover: { size: 5 },
  };

  protected get apexXAxis(): ApexXAxis {
    return {
      categories: this.chartCategories(),
      axisBorder: { show: false },
      axisTicks: { show: false },
      labels: {
        style: {
          fontSize: '10px',
          fontFamily: 'Inter, sans-serif',
          colors: this.isDark() ? 'rgba(255,255,255,0.4)' : 'rgba(0,0,0,0.4)',
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
          colors: this.isDark() ? 'rgba(255,255,255,0.4)' : 'rgba(0,0,0,0.4)',
        },
        formatter: (v: number) => String(Math.round(v)),
      },
      tickAmount: 3,
    };
  }

  protected get apexTooltip(): ApexTooltip {
    return {
      theme: this.isDark() ? 'dark' : 'light',
      x: { show: true },
      y: {
        formatter: (v: number) => `${v} admissions`,
        title: { formatter: () => '' },
      },
    };
  }

  ngOnInit(): void {
    const obs = new MutationObserver(() =>
      this.isDark.set(document.documentElement.classList.contains('dark-theme'))
    );
    obs.observe(document.documentElement, { attributeFilter: ['class'] });

    this.http.get<TrendPoint[]>(`${environment.apiUrl}/dashboard/data/trend-chart`).subscribe({
      next:  d  => { this.points.set(d); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }
}

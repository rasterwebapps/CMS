import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { NgApexchartsModule } from 'ng-apexcharts';
import type { ApexAxisChartSeries, ApexChart, ApexXAxis, ApexYAxis,
              ApexTooltip, ApexFill, ApexGrid, ApexDataLabels,
              ApexPlotOptions } from 'ng-apexcharts';
import { environment } from '../../../../../environments';

interface ProgramAdmissionRow {
  programName: string;
  programCode: string;
  admittedCount: number;
  pct: number;
}

@Component({
  selector: 'dash-widget-program-admissions',
  standalone: true,
  imports: [MatIconModule, NgApexchartsModule],
  templateUrl: './program-admissions-widget.component.html',
  styleUrl:    './program-admissions-widget.component.scss',
})
export class ProgramAdmissionsWidgetComponent implements OnInit {
  @Input() widgetKey?:   string;
  @Input() widgetLabel?: string;
  @Input() widgetIcon?:  string;

  private readonly http = inject(HttpClient);

  protected readonly loading = signal(true);
  protected readonly error   = signal(false);
  protected readonly rows    = signal<ProgramAdmissionRow[]>([]);

  private readonly isDark = signal(
    document.documentElement.classList.contains('dark-theme')
  );

  protected readonly total = computed(() =>
    this.rows().reduce((s, r) => s + (r.admittedCount || 0), 0)
  );

  protected readonly chartSeries = computed((): ApexAxisChartSeries => [{
    name: 'Admissions',
    data: this.rows().map(r => r.admittedCount),
  }]);

  protected readonly chartCategories = computed(() =>
    this.rows().map(r => r.programCode)
  );

  protected readonly apexChart: ApexChart = {
    type: 'bar',
    height: '100%',
    toolbar: { show: false },
    animations: { enabled: true, speed: 500 },
    background: 'transparent',
  };

  protected readonly apexPlotOptions: ApexPlotOptions = {
    bar: { horizontal: true, borderRadius: 4, borderRadiusApplication: 'end' },
  };

  protected readonly apexDataLabels: ApexDataLabels = {
    enabled: true,
    offsetX: -6,
    style: {
      fontSize: '11px',
      fontFamily: 'Inter, sans-serif',
      fontWeight: 700,
      colors: ['rgba(255,255,255,0.9)'],
    },
  };

  protected readonly apexFill: ApexFill = {
    type: 'gradient',
    gradient: {
      type: 'horizontal',
      gradientToColors: ['var(--cms-primary)'],
      opacityFrom: 0.8,
      opacityTo: 0.55,
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
          fontSize: '11px',
          fontFamily: 'Inter, sans-serif',
          colors: this.isDark() ? 'rgba(255,255,255,0.65)' : 'rgba(0,0,0,0.65)',
          fontWeight: 600,
        },
        maxWidth: 80,
      },
    };
  }

  protected get apexTooltip(): ApexTooltip {
    return {
      theme: this.isDark() ? 'dark' : 'light',
      y: { formatter: (v: number) => `${v} students` },
    };
  }

  ngOnInit(): void {
    const obs = new MutationObserver(() =>
      this.isDark.set(document.documentElement.classList.contains('dark-theme'))
    );
    obs.observe(document.documentElement, { attributeFilter: ['class'] });

    this.http.get<ProgramAdmissionRow[]>(`${environment.apiUrl}/dashboard/data/program-admissions`).subscribe({
      next:  d  => { this.rows.set(d ?? []); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }
}

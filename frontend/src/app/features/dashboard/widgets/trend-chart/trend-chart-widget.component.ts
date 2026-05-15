import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { environment } from '../../../../../environments';

interface TrendPoint { month: string; value: number; }
interface TrendBar   { label: string; height: number; emphasis: 'hi' | 'lo'; value: number; }

@Component({
  selector: 'dash-widget-trend-chart',
  standalone: true,
  imports: [MatIconModule, RouterLink],
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

  protected readonly bars = computed((): TrendBar[] => {
    const data = this.points();
    if (!data.length) return [];
    const max    = Math.max(1, ...data.map(p => p.value));
    const sorted = [...data.map(p => p.value)].sort((a, b) => a - b);
    const median = sorted[Math.floor(sorted.length / 2)];
    return data.map((p, i) => ({
      label:    p.month.split(' ')[0],
      height:   Math.max(6, Math.round((p.value / max) * 100)),
      emphasis: p.value >= median ? 'hi' : 'lo',
      value:    p.value,
    }));
  });

  ngOnInit(): void {
    this.http.get<TrendPoint[]>(`${environment.apiUrl}/dashboard/data/trend-chart`).subscribe({
      next:  d  => { this.points.set(d); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }
}

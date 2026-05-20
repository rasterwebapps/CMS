import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { environment } from '../../../../../environments';

interface FunnelStage {
  key: string;
  label: string;
  count: number;
  conversionPct: number | null;
}

interface AdmissionFunnelData {
  stages: FunnelStage[];
  overallConversionPct: number;
}

@Component({
  selector: 'dash-widget-admission-funnel',
  standalone: true,
  imports: [MatIconModule],
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

  /** Stages with bar widths normalised to the top-of-funnel count. */
  protected readonly rows = computed(() => {
    const d = this.data();
    if (!d || d.stages.length === 0) return [];
    const top = Math.max(1, d.stages[0].count);
    return d.stages.map((s, i) => ({
      ...s,
      widthPct: Math.max(8, Math.round((s.count / top) * 100)),
      index: i,
    }));
  });

  ngOnInit(): void {
    this.http.get<AdmissionFunnelData>(`${environment.apiUrl}/dashboard/data/admission-funnel`).subscribe({
      next:  d  => { this.data.set(d); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }
}


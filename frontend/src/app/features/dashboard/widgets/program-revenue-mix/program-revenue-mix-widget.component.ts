import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { InrPipe } from '../../../../shared/pipes/inr.pipe';
import { environment } from '../../../../../environments';

interface ProgramRevenueSlice {
  programName: string;
  programCode: string;
  netRevenue: number;
  sharePct: number;
}

// 6 palette slots — index by row order
const PALETTE = ['#A78BFA', '#22C55E', '#38BDF8', '#F59E0B', '#EC4899', '#06B6D4'];

@Component({
  selector: 'dash-widget-program-revenue-mix',
  standalone: true,
  imports: [MatIconModule, InrPipe],
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

  protected readonly grandTotal = computed(() =>
    this.slices().reduce((s, x) => s + (x.netRevenue || 0), 0)
  );

  /** Donut conic-gradient CSS string built from cumulative shares. */
  protected readonly donutGradient = computed(() => {
    const s = this.slices();
    if (s.length === 0) return 'transparent';
    let cursor = 0;
    const stops: string[] = [];
    s.forEach((sl, i) => {
      const colour = PALETTE[i % PALETTE.length];
      const start = cursor;
      const end   = cursor + sl.sharePct;
      stops.push(`${colour} ${start}% ${end}%`);
      cursor = end;
    });
    if (cursor < 100) stops.push(`rgba(255,255,255,.05) ${cursor}% 100%`);
    return `conic-gradient(${stops.join(', ')})`;
  });

  /** Each slice enriched with its palette colour. */
  protected readonly legend = computed(() =>
    this.slices().map((sl, i) => ({ ...sl, colour: PALETTE[i % PALETTE.length] }))
  );

  ngOnInit(): void {
    this.http.get<ProgramRevenueSlice[]>(`${environment.apiUrl}/dashboard/data/program-revenue-mix`).subscribe({
      next:  d  => { this.slices.set(d ?? []); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }
}


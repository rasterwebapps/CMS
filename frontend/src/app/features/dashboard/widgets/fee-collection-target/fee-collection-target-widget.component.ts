import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { InrPipe } from '../../../../shared/pipes/inr.pipe';
import { environment } from '../../../../../environments';

interface FeeCollectionTargetData {
  collected: number;
  target: number;
  achievedPct: number;
  lastMonthCollected: number;
  deltaPct: number;
}

@Component({
  selector: 'dash-widget-fee-collection-target',
  standalone: true,
  imports: [MatIconModule, InrPipe],
  templateUrl: './fee-collection-target-widget.component.html',
  styleUrl:    './fee-collection-target-widget.component.scss',
})
export class FeeCollectionTargetWidgetComponent implements OnInit {
  @Input() widgetKey?:   string;
  @Input() widgetLabel?: string;
  @Input() widgetIcon?:  string;

  private readonly http = inject(HttpClient);

  protected readonly loading = signal(true);
  protected readonly error   = signal(false);
  protected readonly data    = signal<FeeCollectionTargetData | null>(null);

  /** Clamp achieved% to 0–100 for the visual ring; keep the real value for the label. */
  protected readonly ringPct = computed(() => {
    const v = this.data()?.achievedPct ?? 0;
    return Math.max(0, Math.min(100, v));
  });

  /** Stroke dasharray offset for the SVG ring (circumference ≈ 2πr = 339.29 for r=54). */
  protected readonly dashOffset = computed(() => {
    const circ = 339.29;
    return circ - (circ * this.ringPct()) / 100;
  });

  protected readonly deltaIcon = computed(() => {
    const v = this.data()?.deltaPct ?? 0;
    return v > 0 ? 'arrow_upward' : v < 0 ? 'arrow_downward' : 'remove';
  });

  protected readonly deltaClass = computed(() => {
    const v = this.data()?.deltaPct ?? 0;
    return v > 0 ? 'pos' : v < 0 ? 'neg' : 'neu';
  });

  ngOnInit(): void {
    this.http.get<FeeCollectionTargetData>(`${environment.apiUrl}/dashboard/data/fee-collection-target`).subscribe({
      next:  d  => { this.data.set(d); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }
}


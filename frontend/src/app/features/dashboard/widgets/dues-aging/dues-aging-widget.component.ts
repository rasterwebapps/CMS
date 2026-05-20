import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { InrPipe } from '../../../../shared/pipes/inr.pipe';
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
  imports: [MatIconModule, InrPipe],
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

  protected readonly totalOutstanding = computed(() =>
    this.buckets().reduce((s, b) => s + (b.amount || 0), 0)
  );

  protected readonly totalCount = computed(() =>
    this.buckets().reduce((s, b) => s + (b.demandCount || 0), 0)
  );

  /** Buckets enriched with width % relative to total outstanding. */
  protected readonly rows = computed(() => {
    const total = Math.max(1, this.totalOutstanding());
    return this.buckets().map((b, i) => ({
      ...b,
      widthPct: Math.round((b.amount / total) * 100),
      index: i,
    }));
  });

  ngOnInit(): void {
    this.http.get<DuesAgingBucket[]>(`${environment.apiUrl}/dashboard/data/dues-aging`).subscribe({
      next:  d  => { this.buckets.set(d ?? []); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }
}


import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { environment } from '../../../../../environments';

interface QuickActionItem {
  label:       string;
  route:       string;
  icon:        string;
  description: string;
}

// Rotating accent palette for tile backgrounds
const TILE_COLORS = [
  { bg: 'linear-gradient(135deg,#6366f1,#818cf8)', glow: 'rgba(99,102,241,.35)' },
  { bg: 'linear-gradient(135deg,#0ea5e9,#38bdf8)', glow: 'rgba(14,165,233,.35)' },
  { bg: 'linear-gradient(135deg,#22c55e,#4ade80)', glow: 'rgba(34,197,94,.35)' },
  { bg: 'linear-gradient(135deg,#f59e0b,#fbbf24)', glow: 'rgba(245,158,11,.35)' },
  { bg: 'linear-gradient(135deg,#ec4899,#f472b6)', glow: 'rgba(236,72,153,.35)' },
  { bg: 'linear-gradient(135deg,#8b5cf6,#a78bfa)', glow: 'rgba(139,92,246,.35)' },
  { bg: 'linear-gradient(135deg,#ef4444,#f87171)', glow: 'rgba(239,68,68,.35)' },
  { bg: 'linear-gradient(135deg,#06b6d4,#67e8f9)', glow: 'rgba(6,182,212,.35)' },
];

@Component({
  selector: 'dash-widget-quick-actions',
  standalone: true,
  imports: [MatIconModule, RouterLink],
  templateUrl: './quick-actions-widget.component.html',
  styleUrl:    './quick-actions-widget.component.scss',
})
export class QuickActionsWidgetComponent implements OnInit {
  @Input() widgetKey?:   string;
  @Input() widgetLabel?: string;
  @Input() widgetIcon?:  string;

  private readonly http = inject(HttpClient);

  protected readonly loading = signal(true);
  protected readonly error   = signal(false);
  protected readonly actions = signal<QuickActionItem[]>([]);

  protected readonly coloredActions = computed(() =>
    this.actions().map((a, i) => ({
      ...a,
      color: TILE_COLORS[i % TILE_COLORS.length],
    }))
  );

  ngOnInit(): void {
    this.http.get<QuickActionItem[]>(`${environment.apiUrl}/dashboard/data/quick-actions`).subscribe({
      next:  d  => { this.actions.set(d); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }
}

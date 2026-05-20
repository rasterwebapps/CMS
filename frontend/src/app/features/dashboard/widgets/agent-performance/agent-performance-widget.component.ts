import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { environment } from '../../../../../environments';

interface AgentLeaderboardRow {
  agentId: number;
  agentName: string;
  leads: number;
  conversions: number;
  conversionPct: number;
}

@Component({
  selector: 'dash-widget-agent-performance',
  standalone: true,
  imports: [MatIconModule, RouterLink],
  templateUrl: './agent-performance-widget.component.html',
  styleUrl:    './agent-performance-widget.component.scss',
})
export class AgentPerformanceWidgetComponent implements OnInit {
  @Input() widgetKey?:   string;
  @Input() widgetLabel?: string;
  @Input() widgetIcon?:  string;

  private readonly http = inject(HttpClient);

  protected readonly loading = signal(true);
  protected readonly error   = signal(false);
  protected readonly rows    = signal<AgentLeaderboardRow[]>([]);

  protected readonly topName = computed(() => this.rows()[0]?.agentName ?? null);

  protected initials(name: string): string {
    return name.split(/\s+/).filter(Boolean).slice(0, 2).map(w => w[0]).join('').toUpperCase();
  }

  protected rankClass(i: number): string {
    return i === 0 ? 'gold' : i === 1 ? 'silver' : i === 2 ? 'bronze' : 'plain';
  }

  ngOnInit(): void {
    this.http.get<AgentLeaderboardRow[]>(`${environment.apiUrl}/dashboard/data/agent-performance`).subscribe({
      next:  d  => { this.rows.set(d ?? []); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }
}


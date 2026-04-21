import { Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

export type KpiColorVariant =
  | 'indigo'
  | 'emerald'
  | 'amber'
  | 'teal'
  | 'violet'
  | 'cyan'
  | 'orange'
  | 'rose'
  | 'sky'
  | 'pink';

export interface KpiTrend {
  direction: 'up' | 'down' | 'neutral';
  label: string;
}

@Component({
  selector: 'app-dashboard-kpi-card',
  standalone: true,
  imports: [MatIconModule],
  templateUrl: './dashboard-kpi-card.component.html',
  styleUrl: './dashboard-kpi-card.component.scss',
})
export class DashboardKpiCardComponent {
  readonly title = input.required<string>();
  readonly value = input.required<string>();
  readonly icon = input.required<string>();
  readonly colorVariant = input.required<KpiColorVariant>();
  readonly subtitle = input<string>();
  readonly trend = input<KpiTrend>();
  readonly danger = input<boolean>(false);
  readonly animationDelay = input<string>('0ms');
}

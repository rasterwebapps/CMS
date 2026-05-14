import { Component, Input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { AppDatePipe } from '../../../../shared/pipes/app-date.pipe';
import { SAMPLE_ACTIVITY } from '../widget.models';

interface ActivityRow {
  id: number;
  action: string;
  actor: string;
  timestamp: string;
}

/**
 * Recent activity timeline. Backed by `GET /api/dashboard/activity` (TODO);
 * until then the component falls back to a static sample feed so the widget
 * is visible during development.
 */
@Component({
  selector: 'cms-recent-activity',
  standalone: true,
  imports: [MatIconModule, AppDatePipe],
  templateUrl: './recent-activity.component.html',
  styleUrl: './recent-activity.component.scss',
})
export class RecentActivityComponent {
  @Input() items: ActivityRow[] = SAMPLE_ACTIVITY;

  protected iconFor(action: string): string {
    const a = action.toLowerCase();
    if (a.includes('verified')) return 'verified';
    if (a.includes('rejected')) return 'cancel';
    if (a.includes('uploaded')) return 'upload_file';
    if (a.includes('paid') || a.includes('payment')) return 'payments';
    if (a.includes('admitted') || a.includes('admission')) return 'how_to_reg';
    return 'history';
  }

  protected colorFor(action: string): string {
    const a = action.toLowerCase();
    if (a.includes('verified') || a.includes('paid')) return '#10b981';
    if (a.includes('rejected'))                       return '#ef4444';
    if (a.includes('uploaded'))                       return '#f59e0b';
    return 'var(--primary-theme, #6366f1)';
  }
}


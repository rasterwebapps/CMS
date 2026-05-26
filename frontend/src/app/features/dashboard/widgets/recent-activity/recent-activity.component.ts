import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { AppDatePipe } from '../../../../shared/pipes/app-date.pipe';
import { environment } from '../../../../../environments';

interface ActivityRow {
  id: number;
  action: string;
  actor: string;
  timestamp: string;
}

@Component({
  selector: 'cms-recent-activity',
  standalone: true,
  imports: [MatIconModule, AppDatePipe],
  templateUrl: './recent-activity.component.html',
  styleUrl: './recent-activity.component.scss',
})
export class RecentActivityComponent implements OnInit {
  @Input() widgetKey?:   string;
  @Input() widgetLabel?: string;
  @Input() widgetIcon?:  string;

  private readonly http = inject(HttpClient);

  protected readonly loading = signal(true);
  protected readonly error   = signal(false);
  protected readonly items   = signal<ActivityRow[]>([]);

  ngOnInit(): void {
    this.http.get<ActivityRow[]>(`${environment.apiUrl}/dashboard/data/activity`).subscribe({
      next:  d  => { this.items.set(d ?? []); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }

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
    return 'var(--cms-primary)';
  }
}

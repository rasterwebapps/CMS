import { Component, Input, OnInit, inject, signal } from '@angular/core';
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

  ngOnInit(): void {
    this.http.get<QuickActionItem[]>(`${environment.apiUrl}/dashboard/data/quick-actions`).subscribe({
      next:  d  => { this.actions.set(d); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }
}

import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { environment } from '../../../../../environments';

interface PendingApprovalItem {
  title:    string;
  subtitle: string;
  amount:   string;
  severity: 'red' | 'amber' | 'accent';
}

@Component({
  selector: 'dash-widget-pending-approvals',
  standalone: true,
  imports: [MatIconModule],
  templateUrl: './pending-approvals-widget.component.html',
  styleUrl:    './pending-approvals-widget.component.scss',
})
export class PendingApprovalsWidgetComponent implements OnInit {
  @Input() widgetKey?:   string;
  @Input() widgetLabel?: string;
  @Input() widgetIcon?:  string;

  private readonly http = inject(HttpClient);

  protected readonly loading = signal(true);
  protected readonly error   = signal(false);
  protected readonly items   = signal<PendingApprovalItem[]>([]);

  ngOnInit(): void {
    this.http.get<PendingApprovalItem[]>(`${environment.apiUrl}/dashboard/data/pending-approvals`).subscribe({
      next:  d  => { this.items.set(d); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }
}

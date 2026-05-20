import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { environment } from '../../../../../environments';

interface DocVerificationBacklogData {
  pendingCount: number;
  oldestAgeDays: number;
  rejectedCount: number;
  last24HoursVerified: number;
  cta: string;
}

@Component({
  selector: 'dash-widget-doc-verification-backlog',
  standalone: true,
  imports: [MatIconModule, RouterLink],
  templateUrl: './doc-verification-backlog-widget.component.html',
  styleUrl:    './doc-verification-backlog-widget.component.scss',
})
export class DocVerificationBacklogWidgetComponent implements OnInit {
  @Input() widgetKey?:   string;
  @Input() widgetLabel?: string;
  @Input() widgetIcon?:  string;

  private readonly http = inject(HttpClient);

  protected readonly loading = signal(true);
  protected readonly error   = signal(false);
  protected readonly data    = signal<DocVerificationBacklogData | null>(null);

  /** Severity drives the big-number color and the urgency icon. */
  protected readonly severity = computed<'ok' | 'warn' | 'critical'>(() => {
    const d = this.data();
    if (!d) return 'ok';
    if (d.pendingCount === 0) return 'ok';
    if (d.oldestAgeDays >= 5 || d.pendingCount >= 25) return 'critical';
    return 'warn';
  });

  ngOnInit(): void {
    this.http.get<DocVerificationBacklogData>(`${environment.apiUrl}/dashboard/data/doc-verification-backlog`).subscribe({
      next:  d  => { this.data.set(d); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }
}


import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { environment } from '../../../../../environments';

interface HealthCheck   { name: string; status: 'ok' | 'warn' | 'error'; detail: string; }
interface SystemHealthData { overall: 'ok' | 'warn' | 'error'; checks: HealthCheck[]; }

@Component({
  selector: 'dash-widget-system-health',
  standalone: true,
  imports: [MatIconModule],
  templateUrl: './system-health-widget.component.html',
  styleUrl:    './system-health-widget.component.scss',
  host: { '[style.--ca]': '"#10b981"' },
})
export class SystemHealthWidgetComponent implements OnInit {
  @Input() widgetKey?:   string;
  @Input() widgetLabel?: string;
  @Input() widgetIcon?:  string;

  private readonly http = inject(HttpClient);

  protected readonly loading = signal(true);
  protected readonly error   = signal(false);
  protected readonly data    = signal<SystemHealthData | null>(null);

  ngOnInit(): void {
    this.http.get<SystemHealthData>(`${environment.apiUrl}/dashboard/data/system-health`).subscribe({
      next:  d  => { this.data.set(d); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }
}

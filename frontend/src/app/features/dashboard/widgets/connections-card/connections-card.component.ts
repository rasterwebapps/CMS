import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { environment } from '../../../../../environments';

interface ConnectionItem {
  id: number;
  name: string;
  initials: string;
  role: string;
  online: boolean;
}

@Component({
  selector: 'cms-connections-card',
  standalone: true,
  imports: [MatIconModule, MatTooltipModule],
  templateUrl: './connections-card.component.html',
  styleUrl: './connections-card.component.scss',
  host: { '[style.--ca]': '"#10b981"' },
})
export class ConnectionsCardComponent implements OnInit {
  @Input() widgetKey?:   string;
  @Input() widgetLabel?: string;
  @Input() widgetIcon?:  string;
  @Input() title = 'Colleagues';

  private readonly http = inject(HttpClient);

  protected readonly loading = signal(true);
  protected readonly error   = signal(false);
  protected readonly people  = signal<ConnectionItem[]>([]);

  ngOnInit(): void {
    this.http.get<ConnectionItem[]>(`${environment.apiUrl}/dashboard/data/connections`).subscribe({
      next:  d  => { this.people.set(d ?? []); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }
}

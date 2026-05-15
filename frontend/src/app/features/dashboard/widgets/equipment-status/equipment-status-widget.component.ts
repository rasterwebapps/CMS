import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { environment } from '../../../../../environments';

interface EquipmentStatusRow {
  label: string;
  count: number;
  pct:   number;
  color: 'green' | 'accent' | 'amber' | 'red';
}

@Component({
  selector: 'dash-widget-equipment-status',
  standalone: true,
  imports: [MatIconModule],
  templateUrl: './equipment-status-widget.component.html',
  styleUrl:    './equipment-status-widget.component.scss',
})
export class EquipmentStatusWidgetComponent implements OnInit {
  @Input() widgetKey?:   string;
  @Input() widgetLabel?: string;
  @Input() widgetIcon?:  string;

  private readonly http = inject(HttpClient);

  protected readonly loading = signal(true);
  protected readonly error   = signal(false);
  protected readonly rows    = signal<EquipmentStatusRow[]>([]);

  ngOnInit(): void {
    this.http.get<EquipmentStatusRow[]>(`${environment.apiUrl}/dashboard/data/equipment-status`).subscribe({
      next:  d  => { this.rows.set(d); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }
}

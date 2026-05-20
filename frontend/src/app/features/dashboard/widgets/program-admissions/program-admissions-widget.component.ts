import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { environment } from '../../../../../environments';

interface ProgramAdmissionRow {
  programName: string;
  programCode: string;
  admittedCount: number;
  pct: number;
}

@Component({
  selector: 'dash-widget-program-admissions',
  standalone: true,
  imports: [MatIconModule],
  templateUrl: './program-admissions-widget.component.html',
  styleUrl:    './program-admissions-widget.component.scss',
})
export class ProgramAdmissionsWidgetComponent implements OnInit {
  @Input() widgetKey?:   string;
  @Input() widgetLabel?: string;
  @Input() widgetIcon?:  string;

  private readonly http = inject(HttpClient);

  protected readonly loading = signal(true);
  protected readonly error   = signal(false);
  protected readonly rows    = signal<ProgramAdmissionRow[]>([]);

  protected readonly total = computed(() =>
    this.rows().reduce((s, r) => s + (r.admittedCount || 0), 0)
  );

  ngOnInit(): void {
    this.http.get<ProgramAdmissionRow[]>(`${environment.apiUrl}/dashboard/data/program-admissions`).subscribe({
      next:  d  => { this.rows.set(d ?? []); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }
}


import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { environment } from '../../../../../environments';
import { CohortLapsedSummary } from '../../../academic-year/academic-year.model';

@Component({
  selector: 'dash-widget-govt-lapsed-seats',
  standalone: true,
  imports: [DecimalPipe, MatIconModule],
  templateUrl: './govt-lapsed-seats-widget.component.html',
  styleUrl:    './govt-lapsed-seats-widget.component.scss',
})
export class GovtLapsedSeatsWidgetComponent implements OnInit {
  @Input() widgetKey?:   string;
  @Input() widgetLabel?: string;
  @Input() widgetIcon?:  string;

  private readonly http = inject(HttpClient);

  protected readonly loading = signal(true);
  protected readonly error   = signal(false);
  protected readonly data    = signal<CohortLapsedSummary | null>(null);

  protected readonly hasLapsed = computed(() =>
    (this.data()?.totalLapsedSeats ?? 0) > 0
  );

  ngOnInit(): void {
    this.http.get<CohortLapsedSummary>(`${environment.apiUrl}/cohorts/lapsed-summary`).subscribe({
      next:  d  => { this.data.set(d); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }
}

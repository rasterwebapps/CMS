import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { environment } from '../../../../../environments';
import { InrPipe } from '../../../../shared/pipes/inr.pipe';

interface FeeOverviewData {
  collectedThisMonth: number;
  outstanding:        number;
  totalPayments:      number;
  enquiriesThisMonth: number;
  admissionsThisMonth:number;
}

@Component({
  selector: 'dash-widget-fee-overview',
  standalone: true,
  imports: [MatIconModule, RouterLink, InrPipe],
  templateUrl: './fee-overview-widget.component.html',
  styleUrl:    './fee-overview-widget.component.scss',
})
export class FeeOverviewWidgetComponent implements OnInit {
  @Input() widgetKey?:   string;
  @Input() widgetLabel?: string;
  @Input() widgetIcon?:  string;

  private readonly http = inject(HttpClient);

  protected readonly loading = signal(true);
  protected readonly error   = signal(false);
  protected readonly data    = signal<FeeOverviewData | null>(null);

  ngOnInit(): void {
    this.http.get<FeeOverviewData>(`${environment.apiUrl}/dashboard/data/fee-overview`).subscribe({
      next:  d  => { this.data.set(d); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }
}

import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { InrPipe } from '../../../../shared/pipes/inr.pipe';
import { environment } from '../../../../../environments';

interface ScholarshipBurnData {
  grossFee: number;
  totalDiscount: number;
  totalScholarship: number;
  netCollectable: number;
  discountPct: number;
  studentsImpacted: number;
}

@Component({
  selector: 'dash-widget-scholarship-burn',
  standalone: true,
  imports: [MatIconModule, InrPipe],
  templateUrl: './scholarship-burn-widget.component.html',
  styleUrl:    './scholarship-burn-widget.component.scss',
})
export class ScholarshipBurnWidgetComponent implements OnInit {
  @Input() widgetKey?:   string;
  @Input() widgetLabel?: string;
  @Input() widgetIcon?:  string;

  private readonly http = inject(HttpClient);

  protected readonly loading = signal(true);
  protected readonly error   = signal(false);
  protected readonly data    = signal<ScholarshipBurnData | null>(null);

  /** Bar segment widths as percentages of grossFee. */
  protected readonly segments = computed(() => {
    const d = this.data();
    if (!d || d.grossFee <= 0) return null;
    const dis = (d.totalDiscount    / d.grossFee) * 100;
    const sch = (d.totalScholarship / d.grossFee) * 100;
    const net = Math.max(0, 100 - dis - sch);
    return {
      discountPct:    Math.round(dis),
      scholarshipPct: Math.round(sch),
      netPct:         Math.round(net),
    };
  });

  ngOnInit(): void {
    this.http.get<ScholarshipBurnData>(`${environment.apiUrl}/dashboard/data/scholarship-burn`).subscribe({
      next:  d  => { this.data.set(d); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }
}


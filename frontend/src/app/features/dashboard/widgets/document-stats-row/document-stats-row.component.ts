import { Component, Input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { DocStats } from '../../services/document-slots.service';

/**
 * Compact 5-tile stat row showing document totals + verification status + completion %.
 * Used on Faculty and Student dashboards.
 */
@Component({
  selector: 'cms-document-stats-row',
  standalone: true,
  imports: [MatIconModule],
  templateUrl: './document-stats-row.component.html',
  styleUrl: './document-stats-row.component.scss',
})
export class DocumentStatsRowComponent {
  @Input({ required: true }) stats!: DocStats;
  @Input() progressPct = 0;

  /** SVG mini-ring offset for r=18 → C=2π·18 ≈ 113.1. */
  protected get miniRingOffset(): number {
    return 113.1 - (113.1 * this.progressPct) / 100;
  }
}


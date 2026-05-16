import { Component, Input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

/**
 * Big circular progress ring showing % of documents verified.
 * Used on Faculty and Student dashboards.
 */
@Component({
  selector: 'cms-completion-ring',
  standalone: true,
  imports: [MatIconModule],
  templateUrl: './completion-ring.component.html',
  styleUrl: './completion-ring.component.scss',
  host: { '[style.--ca]': '"#10b981"' },
})
export class CompletionRingComponent {
  @Input() progressPct = 0;

  /** SVG ring offset for r=60 → C=2π·60 ≈ 376.99. */
  protected get bigRingOffset(): number {
    return 376.99 - (376.99 * this.progressPct) / 100;
  }
}


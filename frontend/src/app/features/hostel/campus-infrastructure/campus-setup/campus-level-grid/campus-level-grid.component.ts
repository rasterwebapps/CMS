import { Component, input, output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { CmsStatusBadgeComponent } from '../../../../../shared/status-badge/status-badge.component';

export interface CampusLevelGridItem {
  id: number;
  title: string;
  subtitle: string;
  icon: string;
  isActive: boolean;
}

/**
 * Generic card grid for one level of the campus hierarchy (Branches, Blocks, Floors, Zones or
 * Rooms) — replaces the old bespoke building/floor-bar/zone-segment diagrams with a single
 * reusable "pick a card to drill down" view, matching every level's own drill-down + side-panel
 * add flow instead of nested inline popovers.
 */
@Component({
  selector: 'app-campus-level-grid',
  standalone: true,
  imports: [MatIconModule, CmsStatusBadgeComponent],
  templateUrl: './campus-level-grid.component.html',
  styleUrl: './campus-level-grid.component.scss',
})
export class CampusLevelGridComponent {
  readonly items = input.required<CampusLevelGridItem[]>();
  readonly loading = input(false);
  readonly emptyLabel = input('Nothing here yet.');
  readonly heading = input('');

  readonly select = output<number>();

  /** Fired from a card's edit pencil — the card's own `select` must NOT also fire, so the handler
   *  stops propagation before emitting (see the template's `(click)`). */
  readonly edit = output<number>();

  protected onEditClick(event: Event, id: number): void {
    event.stopPropagation();
    this.edit.emit(id);
  }
}

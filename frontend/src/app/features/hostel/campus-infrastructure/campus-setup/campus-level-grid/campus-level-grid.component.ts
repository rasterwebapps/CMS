import { Component, input, output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { CmsStatusBadgeComponent } from '../../../../../shared/status-badge/status-badge.component';

/** Small colored tag for a real, already-loaded fact (hostel status, gender restriction) — not
 *  decoration, only ever populated from a field that genuinely exists on the underlying entity. */
export interface CampusLevelGridBadge {
  label: string;
  tone: 'hostel' | 'boys' | 'girls';
}

export interface CampusLevelGridItem {
  id: number;
  title: string;
  subtitle: string;
  icon: string;
  isActive: boolean;
  /** A single highlighted fact, e.g. capacity — distinct from `subtitle` so it can read as a stat
   *  rather than blend into the descriptive line. */
  stat?: string;
  badges?: CampusLevelGridBadge[];
  /** A floor plan diagram already exists for this entity — renders a small filled indicator next
   *  to the "View Diagram" icon (BR-60 extension) so it's discoverable without opening the panel. */
  hasFloorPlan?: boolean;
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

  /** Only Zone and Room grids render the "View Diagram" icon (BR-60 extension) — Branch cards
   *  already have their own dedicated nav entry point into Branch Diagrams, so there's no second
   *  entry point needed there. Off by default so existing call sites (Branch, and any future
   *  level added without a diagram) are unaffected. */
  readonly showViewDiagram = input(false);

  readonly select = output<number>();

  /** Fired from a card's edit pencil — the card's own `select` must NOT also fire, so the handler
   *  stops propagation before emitting (see the template's `(click)`). */
  readonly edit = output<number>();

  /** Fired from a card's "View Diagram" icon, same stop-propagation pattern as `edit` above. */
  readonly viewDiagram = output<number>();

  protected onEditClick(event: Event, id: number): void {
    event.stopPropagation();
    this.edit.emit(id);
  }

  protected onViewDiagramClick(event: Event, id: number): void {
    event.stopPropagation();
    this.viewDiagram.emit(id);
  }
}

import {
  Component, Input, Output, EventEmitter,
  OnChanges, SimpleChanges, inject,
} from '@angular/core';
import {
  CdkDragDrop, CdkDrag, CdkDropList, CdkDragHandle,
  CdkDragPreview, CdkDragPlaceholder, moveItemInArray,
} from '@angular/cdk/drag-drop';
import { MatIconModule }             from '@angular/material/icon';
import { MatButtonModule }           from '@angular/material/button';
import { MatProgressSpinnerModule }  from '@angular/material/progress-spinner';
import { MatTooltipModule }          from '@angular/material/tooltip';

import { WidgetConfigDto, PermissionService }    from '../../core/permissions/permission.service';
import { WIDGET_REGISTRY, WidgetDef, widgetByKey } from '../../features/dashboard/widget-registry';

export interface PickerItem {
  key:     string;
  colSpan: 1 | 2 | 4;
  rowSpan: 1 | 2;
  def:     WidgetDef;
}

const CATEGORY_LABELS: Record<string, string> = {
  layout:      'Layout',
  stats:       'Statistics',
  charts:      'Charts & Trends',
  lists:       'Lists & Tables',
  operational: 'Operational',
};

const CATEGORY_COLORS: Record<string, string> = {
  layout:      'var(--cms-primary)',
  stats:       '#F59E0B',
  charts:      '#22C55E',
  lists:       '#A78BFA',
  operational: '#EF4444',
};

const CATEGORIES = ['layout', 'stats', 'charts', 'lists', 'operational'] as const;

@Component({
  selector: 'app-widget-picker',
  standalone: true,
  imports: [
    CdkDrag, CdkDropList, CdkDragHandle, CdkDragPreview, CdkDragPlaceholder,
    MatIconModule, MatButtonModule, MatProgressSpinnerModule, MatTooltipModule,
  ],
  templateUrl: './widget-picker.component.html',
  styleUrl:    './widget-picker.component.scss',
})
export class WidgetPickerComponent implements OnChanges {
  private readonly permService = inject(PermissionService);

  /** Pre-configured widgets to load into the active list on open. */
  @Input() initialWidgets: WidgetConfigDto[] = [];
  /** Whether a save is in progress (disables buttons). */
  @Input() saving = false;
  /** Show the Reset button in the footer (personal dashboard only). */
  @Input() showReset = false;
  /**
   * Restrict the palette (and drop already-saved entries) to widgets the current viewer holds
   * a required permission for. Correct for a user editing their own personal dashboard; set to
   * `false` when an admin is editing a ROLE's default widget set — that should offer the full
   * catalog regardless of the editing admin's own permissions, since it's the role's set, not theirs.
   */
  @Input() filterByViewerPermissions = true;

  /** Emits the final WidgetConfigDto[] when the user clicks Save. */
  @Output() saved    = new EventEmitter<WidgetConfigDto[]>();
  /** Emits when the user clicks Cancel or Reset. */
  @Output() cancelled = new EventEmitter<void>();
  /** Emits when the user clicks Reset (only when showReset = true). */
  @Output() reset    = new EventEmitter<void>();

  protected activeItems: PickerItem[] = [];
  protected readonly categories = CATEGORIES;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['initialWidgets']) {
      this.activeItems = (this.initialWidgets ?? [])
        .map(c => {
          const def = widgetByKey(c.key);
          if (!def || !this.isAccessible(def)) return null;
          return {
            key:     c.key,
            colSpan: this.coerceColSpan(c.colSpan),
            rowSpan: this.coerceRowSpan(c.rowSpan),
            def,
          } satisfies PickerItem;
        })
        .filter((x): x is PickerItem => x !== null);
    }
  }

  /** True if the widget should be offered — always true when filtering is off, otherwise
   *  true if it has no required permissions or the viewer holds at least one of them. */
  private isAccessible(def: WidgetDef): boolean {
    if (!this.filterByViewerPermissions) return true;
    return !def.requiredPermissions || this.permService.hasAny(...def.requiredPermissions);
  }

  // ── Palette ────────────────────────────────────────────────────────────────

  protected get paletteItems(): WidgetDef[] {
    const active = new Set(this.activeItems.map(i => i.key));
    return WIDGET_REGISTRY.filter(w => !active.has(w.key) && this.isAccessible(w));
  }

  protected paletteByCategory(cat: string): WidgetDef[] {
    return this.paletteItems.filter(w => w.category === cat);
  }

  protected categoryLabel(cat: string): string {
    return CATEGORY_LABELS[cat] ?? cat;
  }

  protected categoryColor(cat: string): string {
    return CATEGORY_COLORS[cat] ?? 'var(--cms-primary)';
  }

  // ── Drag-drop ──────────────────────────────────────────────────────────────

  protected drop(event: CdkDragDrop<PickerItem[]>): void {
    moveItemInArray(this.activeItems, event.previousIndex, event.currentIndex);
  }

  // ── Mutations ──────────────────────────────────────────────────────────────

  protected addWidget(def: WidgetDef): void {
    this.activeItems = [
      ...this.activeItems,
      {
        key:     def.key,
        colSpan: this.coerceColSpan(def.defaultColSpan),
        rowSpan: this.coerceRowSpan(def.defaultRowSpan),
        def,
      },
    ];
  }

  protected removeWidget(index: number): void {
    this.activeItems = this.activeItems.filter((_, i) => i !== index);
  }

  protected setColSpan(item: PickerItem, span: 1 | 2 | 4): void { item.colSpan = span; }
  protected setRowSpan(item: PickerItem, span: 1 | 2):     void { item.rowSpan = span; }

  // ── Actions ────────────────────────────────────────────────────────────────

  protected save(): void {
    const configs: WidgetConfigDto[] = this.activeItems.map((item, i) => ({
      key:        item.key,
      order:      i,
      colSpan:    item.colSpan,
      rowSpan:    item.rowSpan,
      configJson: null,
    }));
    this.saved.emit(configs);
  }

  protected cancel(): void { this.cancelled.emit(); }
  protected onReset(): void { this.reset.emit(); }

  // ── Helpers ────────────────────────────────────────────────────────────────

  private coerceColSpan(v: number): 1 | 2 | 4 {
    if (v >= 4) return 4;
    if (v >= 2) return 2;
    return 1;
  }

  private coerceRowSpan(v: number): 1 | 2 { return v >= 2 ? 2 : 1; }

  protected colSpanLabel(span: 1 | 2 | 4): string {
    return span === 1 ? 'Compact' : span === 2 ? 'Half' : 'Full';
  }

  protected rowSpanLabel(span: 1 | 2): string {
    return span === 1 ? 'Normal height' : 'Tall (2× height)';
  }

  protected readonly spanCells   = [0, 1, 2, 3];
  protected readonly heightCells = [0, 1];

  protected filledCells(span: 1 | 2 | 4): number {
    return span === 4 ? 4 : span === 2 ? 2 : 1;
  }
}

import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import {
  CdkDragDrop,
  CdkDrag,
  CdkDropList,
  CdkDragHandle,
  CdkDragPreview,
  CdkDragPlaceholder,
  moveItemInArray,
} from '@angular/cdk/drag-drop';
import { MatIconModule }   from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar }     from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { DashboardConfigService }                from '../services/dashboard-config.service';
import { PermissionService, WidgetConfigDto }    from '../../../core/permissions/permission.service';
import { WIDGET_REGISTRY, WidgetDef, widgetByKey } from '../widget-registry';

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
  selector:    'app-dashboard-configure',
  standalone:  true,
  imports: [
    CdkDrag, CdkDropList, CdkDragHandle, CdkDragPreview, CdkDragPlaceholder,
    MatIconModule, MatButtonModule, MatProgressSpinnerModule,
  ],
  templateUrl: './dashboard-configure.component.html',
  styleUrl:    './dashboard-configure.component.scss',
})
export class DashboardConfigureComponent implements OnInit {
  private readonly router        = inject(Router);
  private readonly configService = inject(DashboardConfigService);
  private readonly permService   = inject(PermissionService);
  private readonly snackBar      = inject(MatSnackBar);

  protected readonly loading = signal(true);
  protected readonly saving  = signal(false);

  protected activeItems: PickerItem[] = [];

  protected readonly categories = CATEGORIES;

  ngOnInit(): void {
    if (!this.permService.has('DASHBOARD_CUSTOMIZE')) {
      void this.router.navigate(['/dashboard']);
      return;
    }
    this.configService.getMyConfig().subscribe({
      next: configs => {
        this.activeItems = configs
          .map(c => {
            const def = widgetByKey(c.key);
            if (!def) return null;
            return {
              key:     c.key,
              colSpan: this.coerceColSpan(c.colSpan),
              rowSpan: this.coerceRowSpan(c.rowSpan),
              def,
            } satisfies PickerItem;
          })
          .filter((x): x is PickerItem => x !== null);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  // ── Palette ──────────────────────────────────────────────────────────────

  protected get paletteItems(): WidgetDef[] {
    const active = new Set(this.activeItems.map(i => i.key));
    return WIDGET_REGISTRY.filter(w => !active.has(w.key));
  }

  protected paletteByCategory(cat: string): WidgetDef[] {
    return this.paletteItems.filter(w => w.category === cat);
  }

  protected categoryLabel(cat: string): string {
    return CATEGORY_LABELS[cat] ?? cat;
  }

  // ── Drag-drop ────────────────────────────────────────────────────────────

  protected drop(event: CdkDragDrop<PickerItem[]>): void {
    moveItemInArray(this.activeItems, event.previousIndex, event.currentIndex);
  }

  // ── Active-item mutations ────────────────────────────────────────────────

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

  protected setColSpan(item: PickerItem, span: 1 | 2 | 4): void {
    item.colSpan = span;
  }

  // ── Actions ──────────────────────────────────────────────────────────────

  protected save(): void {
    this.saving.set(true);
    const configs: WidgetConfigDto[] = this.activeItems.map((item, i) => ({
      key:        item.key,
      order:      i,
      colSpan:    item.colSpan,
      rowSpan:    item.rowSpan,
      configJson: null,
    }));
    this.configService.saveConfig(configs).subscribe({
      next: () => {
        this.saving.set(false);
        this.snackBar.open('Dashboard saved', 'OK', { duration: 2500 });
        void this.router.navigate(['/dashboard']);
      },
      error: () => {
        this.saving.set(false);
        this.snackBar.open('Save failed — please try again', 'Dismiss', { duration: 3500 });
      },
    });
  }

  protected reset(): void {
    this.saving.set(true);
    this.configService.resetConfig().subscribe({
      next: () => {
        this.saving.set(false);
        this.snackBar.open('Reset to role default', 'OK', { duration: 2500 });
        void this.router.navigate(['/dashboard']);
      },
      error: () => {
        this.saving.set(false);
        this.snackBar.open('Reset failed — please try again', 'Dismiss', { duration: 3500 });
      },
    });
  }

  protected cancel(): void {
    void this.router.navigate(['/dashboard']);
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  private coerceColSpan(v: number): 1 | 2 | 4 {
    if (v >= 4) return 4;
    if (v >= 2) return 2;
    return 1;
  }

  private coerceRowSpan(v: number): 1 | 2 {
    return v >= 2 ? 2 : 1;
  }

  protected colSpanLabel(span: 1 | 2 | 4): string {
    return span === 1 ? 'Compact' : span === 2 ? 'Half' : 'Full';
  }

  /** CSS color value for the widget's category — used as --wc custom property. */
  protected categoryColor(cat: string): string {
    return CATEGORY_COLORS[cat] ?? 'var(--cms-primary)';
  }

  /** Returns [0,1,2,3] for the mini 4-cell grid in size chips. */
  protected readonly spanCells = [0, 1, 2, 3];

  /** How many cells are "filled" for each span option. */
  protected filledCells(span: 1 | 2 | 4): number {
    return span === 4 ? 4 : span === 2 ? 2 : 1;
  }
}

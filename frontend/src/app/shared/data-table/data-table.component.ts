import {
  AfterContentInit,
  Component,
  ContentChild,
  Input,
  OnChanges,
  TemplateRef,
  computed,
  inject,
  signal,
} from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatSortModule, Sort, SortDirection } from '@angular/material/sort';
import { MatIconModule } from '@angular/material/icon';
import { CmsSkeletonComponent } from '../skeleton/skeleton.component';
import { CmsEmptyStateComponent } from '../empty-state/empty-state.component';
import { ResponsiveService } from '../../core/layout/responsive.service';
import { CsvExporterService } from '../../core/export/csv-exporter.service';
import { ColumnDef } from './column-def.model';

/**
 * Generic data table wrapper around `mat-table`.
 *
 * Features:
 * - Shimmer skeleton rows while `loading` is `true`
 * - Built-in empty state when `data` is empty
 * - Sticky column headers
 * - Horizontally scrollable on small viewports
 * - Optional row-actions column via `<ng-template #rowActions let-row>`
 * - Mobile (`≤ 767px`) automatically renders rows as a vertical card list,
 *   either via the optional `<ng-template #mobileCard let-row>` or a default
 *   key/value layout that loops over `columns`.
 *
 * Usage:
 * ```html
 * <cms-data-table [columns]="cols" [data]="rows" [loading]="loading()">
 *   <ng-template #rowActions let-row>
 *     <button class="action-btn" (click)="edit(row)">Edit</button>
 *   </ng-template>
 * </cms-data-table>
 * ```
 */
@Component({
  selector: 'cms-data-table',
  standalone: true,
  imports: [
    MatTableModule,
    MatSortModule,
    MatIconModule,
    NgTemplateOutlet,
    CmsSkeletonComponent,
    CmsEmptyStateComponent,
  ],
  templateUrl: './data-table.component.html',
  styleUrl: './data-table.component.scss',
})
export class CmsDataTableComponent<T = Record<string, unknown>> implements OnChanges, AfterContentInit {
  private readonly csvExporter = inject(CsvExporterService);

  /** Column definitions — drives both header row and data cells. */
  @Input() columns: ColumnDef<T>[] = [];

  /** Row data to display. */
  @Input() data: T[] = [];

  /** When `true`, replaces table rows with shimmer skeleton placeholders. */
  @Input() loading = false;

  /** Number of skeleton rows to render while loading. */
  @Input() loadingRows = 5;

  /** Material icon name for the empty state. */
  @Input() emptyIcon = 'inbox';

  /** Primary message shown when `data` is empty. */
  @Input() emptyTitle = 'No data available';

  /** Optional secondary message shown when `data` is empty. */
  @Input() emptySubtitle = '';

  /** Label for the optional CTA button in the empty state. */
  @Input() emptyActionLabel = '';

  /** When `true`, renders an "Export CSV" button above the table. */
  @Input() exportable = false;

  /** Filename used for the CSV download (`.csv` is appended automatically). */
  @Input() exportFilename = 'export';

  /** Projected template used to render the actions cell for each row. */
  @ContentChild('rowActions') rowActionsTemplate?: TemplateRef<{ $implicit: T }>;

  /**
   * Optional template projected via `<ng-template #mobileCard let-row>` used
   * to render each row when the viewport is mobile. When omitted, a default
   * key/value card layout is rendered automatically.
   */
  @ContentChild('mobileCard') mobileCardTemplate?: TemplateRef<{ $implicit: T }>;

  private readonly responsive = inject(ResponsiveService);

  /** True when the viewport is in the `mobile` bucket. */
  protected readonly isMobile = this.responsive.isMobile;

  /** Internal signal tracking whether an actions column should be appended. */
  protected readonly hasActions = signal(false);

  /** Internal signal tracking whether a custom mobile-card template was provided. */
  protected readonly hasMobileCard = signal(false);

  /** Current sort state for the shared array-backed table. */
  protected readonly sortState = signal<Sort>({ active: '', direction: '' });

  /** Displayed column keys, appending `_actions` when a template is provided. */
  protected readonly displayedColumns = computed(() => {
    const keys = this.columns.map((c) => c.key);
    return this.hasActions() ? [...keys, '_actions'] : keys;
  });

  /** Data sorted according to the active Material sort header. */
  protected readonly sortedData = computed(() => {
    const sort = this.sortState();

    if (!sort.active || !sort.direction) {
      return this.data;
    }

    const column = this.columns.find((col) => col.key === sort.active);
    if (!column || column.sortable === false) {
      return this.data;
    }

    return [...this.data].sort((a, b) =>
      this.compareSortValues(this.sortValue(column, a), this.sortValue(column, b), sort.direction),
    );
  });

  /** Array used to drive @for skeleton rows. */
  protected skeletonRows: number[] = [];

  ngOnChanges(): void {
    this.skeletonRows = Array.from({ length: Math.max(1, this.loadingRows) }, (_, i) => i);
  }

  ngAfterContentInit(): void {
    this.hasActions.set(!!this.rowActionsTemplate);
    this.hasMobileCard.set(!!this.mobileCardTemplate);
  }

  /** Returns a safe string value for a cell, replacing nullish values with '—'. */
  protected cellValue(col: ColumnDef<T>, row: T): string {
    const val = col.cell(row);
    return val !== null && val !== undefined && val !== '' ? String(val) : '—';
  }

  /** Material sort arrow is placed before right-aligned numeric headers and after all others. */
  protected sortArrowPosition(col: ColumnDef<T>): 'before' | 'after' {
    return col.align === 'right' ? 'before' : 'after';
  }

  /** Updates the sort state from Material's `matSortChange` event. */
  protected updateSort(sort: Sort): void {
    this.sortState.set(sort);
  }

  private sortValue(col: ColumnDef<T>, row: T): string | number | boolean | Date | null | undefined {
    return col.sortAccessor ? col.sortAccessor(row) : col.cell(row);
  }

  private compareSortValues(
    a: string | number | boolean | Date | null | undefined,
    b: string | number | boolean | Date | null | undefined,
    direction: SortDirection,
  ): number {
    const multiplier = direction === 'asc' ? 1 : -1;
    const left = this.normalizeSortValue(a);
    const right = this.normalizeSortValue(b);

    if (left === right) return 0;
    if (left === null) return 1;
    if (right === null) return -1;

    return left < right ? -1 * multiplier : multiplier;
  }

  private normalizeSortValue(value: string | number | boolean | Date | null | undefined): string | number | null {
    if (value === null || value === undefined || value === '') return null;
    if (value instanceof Date) return value.getTime();
    if (typeof value === 'boolean') return value ? 1 : 0;
    if (typeof value === 'number') return value;

    const text = String(value);
    const numeric = Number(text.replace(/[^0-9.-]/g, ''));
    if (!Number.isNaN(numeric) && /\d/.test(text)) {
      return numeric;
    }

    return text.toLocaleLowerCase();
  }

  /**
   * Trigger a CSV download of the current `data` rows using the configured
   * `columns`. The cell formatter from each `ColumnDef` is reused so the
   * exported value matches what's shown on screen.
   */
  protected exportCsv(): void {
    const columns = this.columns.map((col) => ({
      key: col.key,
      header: col.header,
      format: (_value: unknown, row: T): string => {
        const cellVal = col.cell(row);
        return cellVal === null || cellVal === undefined ? '' : String(cellVal);
      },
    }));
    this.csvExporter.exportRows<T>(this.exportFilename, columns, this.data);
  }
}


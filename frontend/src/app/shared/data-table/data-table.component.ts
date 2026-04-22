import {
  AfterContentInit,
  Component,
  ContentChild,
  Input,
  OnChanges,
  TemplateRef,
  computed,
  signal,
} from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { CmsSkeletonComponent } from '../skeleton/skeleton.component';
import { CmsEmptyStateComponent } from '../empty-state/empty-state.component';
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
  imports: [MatTableModule, NgTemplateOutlet, CmsSkeletonComponent, CmsEmptyStateComponent],
  templateUrl: './data-table.component.html',
  styleUrl: './data-table.component.scss',
})
export class CmsDataTableComponent<T = Record<string, unknown>> implements OnChanges, AfterContentInit {
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

  /** Projected template used to render the actions cell for each row. */
  @ContentChild('rowActions') rowActionsTemplate?: TemplateRef<{ $implicit: T }>;

  /** Internal signal tracking whether an actions column should be appended. */
  protected readonly hasActions = signal(false);

  /** Displayed column keys, appending `_actions` when a template is provided. */
  protected readonly displayedColumns = computed(() => {
    const keys = this.columns.map((c) => c.key);
    return this.hasActions() ? [...keys, '_actions'] : keys;
  });

  /** Array used to drive @for skeleton rows. */
  protected skeletonRows: number[] = [];

  ngOnChanges(): void {
    this.skeletonRows = Array.from({ length: Math.max(1, this.loadingRows) }, (_, i) => i);
  }

  ngAfterContentInit(): void {
    this.hasActions.set(!!this.rowActionsTemplate);
  }

  /** Returns a safe string value for a cell, replacing nullish values with '—'. */
  protected cellValue(col: ColumnDef<T>, row: T): string {
    const val = col.cell(row);
    return val !== null && val !== undefined && val !== '' ? String(val) : '—';
  }
}

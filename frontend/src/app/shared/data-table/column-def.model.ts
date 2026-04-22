/**
 * Defines a single column in a CmsDataTable.
 *
 * @template T  The row data type.
 */
export interface ColumnDef<T = Record<string, unknown>> {
  /** Unique column identifier — used as `matColumnDef`. */
  key: string;

  /** Column header label. */
  header: string;

  /**
   * Returns the display value for a given row.
   * Return `null` or `undefined` to render an em-dash placeholder.
   */
  cell: (row: T) => string | number | boolean | null | undefined;

  /** Optional fixed column width (CSS value, e.g. `'120px'`). */
  width?: string;

  /** Text alignment for header and cells. Defaults to `'left'`. */
  align?: 'left' | 'center' | 'right';

  /** Additional CSS class applied to each `<td>` in this column. */
  cssClass?: string;
}

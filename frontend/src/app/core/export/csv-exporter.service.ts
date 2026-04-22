import { Injectable } from '@angular/core';

/**
 * Single-column metadata for {@link CsvExporterService.exportRows}.
 */
export interface CsvColumn<T> {
  /** Property key on the row, or any string when a `format` callback is supplied. */
  key: keyof T | string;
  /** Header text written as the first row of the CSV. */
  header: string;
  /**
   * Optional formatter — receives the raw cell value and the full row, and
   * returns the string written to the CSV. Useful for dates, currency, etc.
   */
  format?: (value: unknown, row: T) => string;
}

/**
 * Minimal RFC 4180 CSV exporter. Adds a UTF-8 BOM so Excel detects the
 * encoding correctly and triggers a download via a temporary `<a>` element.
 *
 * No third-party dependency — keeps Phase 6 in line with the plan's
 * "no new third-party UI libraries" non-goal.
 */
@Injectable({ providedIn: 'root' })
export class CsvExporterService {
  /**
   * Build a CSV from `rows` using `columns` and trigger a browser download.
   * An empty `rows` array still produces a valid (header-only) CSV.
   */
  exportRows<T>(filename: string, columns: ReadonlyArray<CsvColumn<T>>, rows: ReadonlyArray<T>): void {
    if (typeof document === 'undefined' || typeof URL === 'undefined') {
      return;
    }

    const csv = this.toCsv(columns, rows);
    const safeName = this.normaliseFilename(filename);

    // Prefix BOM so Excel/Numbers detect UTF-8 correctly.
    const blob = new Blob(['\uFEFF', csv], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);

    const link = document.createElement('a');
    link.href = url;
    link.download = safeName;
    link.style.display = 'none';
    document.body.appendChild(link);
    link.click();
    link.remove();

    // Defer revoke so Safari has time to start the download.
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }

  /** Returns the CSV body (no BOM) — exposed for unit tests / preview flows. */
  toCsv<T>(columns: ReadonlyArray<CsvColumn<T>>, rows: ReadonlyArray<T>): string {
    const headerLine = columns.map((c) => this.escape(c.header)).join(',');
    const rowLines = rows.map((row) =>
      columns
        .map((c) => {
          const raw = this.readKey(row, c.key);
          const formatted = c.format ? c.format(raw, row) : this.defaultFormat(raw);
          return this.escape(formatted);
        })
        .join(','),
    );
    return [headerLine, ...rowLines].join('\r\n');
  }

  private readKey<T>(row: T, key: keyof T | string): unknown {
    if (row === null || row === undefined) return '';
    return (row as Record<string, unknown>)[key as string];
  }

  private defaultFormat(value: unknown): string {
    if (value === null || value === undefined) return '';
    if (value instanceof Date) return value.toISOString();
    if (typeof value === 'object') {
      try {
        return JSON.stringify(value);
      } catch {
        return String(value);
      }
    }
    return String(value);
  }

  /** Quote per RFC 4180: wrap in `"…"` when the value contains `, " \r \n`. */
  private escape(value: string): string {
    const needsQuote = /[",\r\n]/.test(value);
    const escaped = value.replace(/"/g, '""');
    return needsQuote ? `"${escaped}"` : escaped;
  }

  private normaliseFilename(filename: string): string {
    const base = (filename || 'export').trim().replace(/[\\/:*?"<>|]/g, '_');
    return base.toLowerCase().endsWith('.csv') ? base : `${base}.csv`;
  }
}

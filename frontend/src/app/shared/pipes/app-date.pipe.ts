import { Pipe, PipeTransform } from '@angular/core';
import { DatePipe } from '@angular/common';
import { DATE_FORMATS, DateFormatType } from '../config/date-format.config';

/**
 * Application-wide date formatting pipe.
 *
 * All timestamps from the API are UTC ISO-8601 strings (e.g. "2026-04-28T08:30:00Z").
 * This pipe automatically converts them to the user's **browser local timezone** and
 * formats them according to the global date format configuration.
 *
 * Usage:
 *   {{ dateValue | appDate }}               → Standard date only:  "28-04-2026"
 *   {{ dateValue | appDate:'short' }}       → Compact date:        "28-04-26"
 *   {{ dateValue | appDate:'long' }}        → Long date:           "28-04-2026"
 *   {{ dateValue | appDate:'dateTime' }}    → Date + time (AM/PM): "28-04-2026 02:30 PM"
 *   {{ dateValue | appDate:'time' }}        → Time only (AM/PM):   "02:30 PM"
 *   {{ null | appDate }}                    → Returns '—' (en-dash for empty)
 *
 * Benefits:
 * - Single source of truth for date formats
 * - Automatic UTC → local timezone conversion (uses Intl API to detect browser TZ)
 * - AM/PM display for all time formats
 * - Consistent null/undefined handling
 * - Change format globally by editing date-format.config.ts
 * - Type-safe format selection
 *
 * @example
 * // In component
 * import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
 *
 * @Component({
 *   imports: [AppDatePipe],
 * })
 *
 * // In template
 * <td mat-cell *matCellDef="let row">{{ row.createdAt | appDate:'dateTime' }}</td>
 */
@Pipe({
  name: 'appDate',
  standalone: true,
  pure: true,
})
export class AppDatePipe implements PipeTransform {
  /**
   * IANA timezone string of the user's browser (e.g. "Asia/Kolkata", "America/New_York").
   * Detected once at pipe instantiation via the Intl API.
   */
  private readonly userTimezone: string = Intl.DateTimeFormat().resolvedOptions().timeZone;

  private readonly datePipe = new DatePipe('en-IN');

  /**
   * Transform a UTC date value to a locally-formatted string.
   *
   * @param value  - Date value (ISO-8601 UTC string, Date object, or timestamp number)
   * @param format - Format type: 'standard' | 'short' | 'long' | 'dateTime' | 'time'
   * @returns Formatted date string in the user's local timezone, or '—' for null/undefined
   */
  transform(
    value: Date | string | number | null | undefined,
    format: DateFormatType = 'standard',
  ): string {
    if (value == null || value === '') {
      return '—'; // en-dash for missing dates
    }
    const formatString = DATE_FORMATS[format];
    // Pass the user's browser IANA timezone so Angular converts UTC → local time.
    const formatted = this.datePipe.transform(value, formatString, this.userTimezone);
    return formatted ?? '—';
  }
}



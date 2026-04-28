import { Pipe, PipeTransform } from '@angular/core';
import { DatePipe } from '@angular/common';
import { DATE_FORMATS, DateFormatType } from '../config/date-format.config';
/**
 * Application-wide date formatting pipe
 * 
 * Uses the global date format configuration from date-format.config.ts
 * Ensures consistent date formatting across the entire application.
 * 
 * Usage:
 *   {{ dateValue | appDate }}               → Uses 'standard' format (dd-MM-yyyy)
 *   {{ dateValue | appDate:'short' }}       → Uses 'short' format (dd-MM-yy)
 *   {{ dateValue | appDate:'long' }}        → Uses 'long' format (dd-MM-yyyy)
 *   {{ dateValue | appDate:'dateTime' }}    → Uses 'dateTime' format (dd-MM-yyyy HH:mm)
 *   {{ null | appDate }}                    → Returns '—' (en-dash for empty)
 * 
 * Benefits:
 * - Single source of truth for date formats
 * - Change format globally by editing date-format.config.ts
 * - Consistent handling of null/undefined values
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
 * <td mat-cell *matCellDef="let row">{{ row.paymentDate | appDate }}</td>
 */
@Pipe({
  name: 'appDate',
  standalone: true,
  pure: true,
})
export class AppDatePipe implements PipeTransform {
  private readonly datePipe = new DatePipe('en-IN'); // Indian locale
  /**
   * Transform a date value to formatted string
   * 
   * @param value - Date value (Date object, ISO string, timestamp)
   * @param format - Format type: 'standard' | 'short' | 'long' | 'dateTime'
   * @returns Formatted date string or en-dash for null/undefined
   */
  transform(value: Date | string | number | null | undefined, format: DateFormatType = 'standard'): string {
    if (value == null || value === '') {
      return '—'; // en-dash for missing dates
    }
    const formatString = DATE_FORMATS[format];
    const formatted = this.datePipe.transform(value, formatString);
    return formatted ?? '—';
  }
}

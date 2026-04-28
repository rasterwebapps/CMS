/**
 * Global Date Format Configuration
 *
 * Single source of truth for all date formatting across the application.
 * Change these values to update date formats globally.
 *
 * Format reference (Angular DatePipe):
 * - dd: Day of month (01-31)
 * - MM: Month (01-12)
 * - MMM: Month short name (Jan, Feb, Mar)
 * - MMMM: Month full name (January, February)
 * - yy: 2-digit year (26)
 * - yyyy: 4-digit year (2026)
 * - EEE: Day of week short (Mon, Tue)
 * - EEEE: Day of week full (Monday, Tuesday)
 * - HH: Hour 24-hour (00-23)
 * - hh: Hour 12-hour (01-12)
 * - mm: Minute (00-59)
 * - ss: Second (00-59)
 * - a: AM/PM marker
 */

export const DATE_FORMATS = {
  /**
   * Standard date format for tables, forms, and displays
   * Default: 'dd-MM-yyyy' → "28-04-2026"
   * Alternative options:
   * - 'dd MMM yyyy' → "28 Apr 2026" (readable)
   * - 'dd/MM/yyyy' → "28/04/2026" (slash separator)
   * - 'yyyy-MM-dd' → "2026-04-28" (ISO format)
   */
  standard: 'dd-MM-yyyy',

  /**
   * Short date format for compact displays (tables with space constraints)
   * Default: 'dd-MM-yy' → "28-04-26"
   * Alternative: 'dd MMM yy' → "28 Apr 26"
   */
  short: 'dd-MM-yy',

  /**
   * Long date format for detailed views and reports
   * Default: 'dd-MM-yyyy' → "28-04-2026"
   * Alternative: 'dd MMMM yyyy' → "28 April 2026"
   */
  long: 'dd-MM-yyyy',

  /**
   * Date with time for timestamps (audit logs, payment records)
   * Default: 'dd-MM-yyyy HH:mm' → "28-04-2026 14:30"
   * Alternative: 'dd MMM yyyy, hh:mm a' → "28 Apr 2026, 02:30 PM"
   */
  dateTime: 'dd-MM-yyyy HH:mm',

  /**
   * Date range separator for displaying date ranges
   * Default: ' – ' (en-dash with spaces)
   */
  rangeSeparator: ' – ',
} as const;

/**
 * Type-safe accessor for date formats
 */
export type DateFormatType = keyof typeof DATE_FORMATS;

/**
 * Helper function to get a date format
 */
export function getDateFormat(type: DateFormatType = 'standard'): string {
  return DATE_FORMATS[type];
}


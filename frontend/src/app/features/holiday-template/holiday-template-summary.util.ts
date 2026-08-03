import { HolidayTemplate } from './holiday-template.model';

const MONTH_NAMES = [
  'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec',
];

const WEEK_OF_MONTH_LABELS: Record<string, string> = {
  FIRST: '1st', SECOND: '2nd', THIRD: '3rd', FOURTH: '4th', LAST: 'Last',
};

const DAY_OF_WEEK_LABELS: Record<string, string> = {
  MONDAY: 'Monday', TUESDAY: 'Tuesday', WEDNESDAY: 'Wednesday',
  THURSDAY: 'Thursday', FRIDAY: 'Friday', SATURDAY: 'Saturday',
};

/** Only the fields the summary actually reads -- lets the live-preview in the form pass a
 *  partially-filled-in draft without needing a full HolidayTemplate (id/timestamps/etc). */
type RecurrenceShape = Pick<
  HolidayTemplate,
  'recurrenceType' | 'month' | 'dayOfMonth' | 'weekOfMonth' | 'dayOfWeek' | 'durationDays'
>;

/** One-line human summary of a HolidayTemplate's recurrence rule, e.g. "26 Jan every year" or
 *  "2nd Saturday every month" -- used by the list screen's card/table views and the form's
 *  live preview. */
export function formatRecurrenceSummary(template: RecurrenceShape): string {
  if (template.recurrenceType === 'YEARLY') {
    const day = template.dayOfMonth ?? '?';
    const month = template.month ? MONTH_NAMES[template.month - 1] : '?';
    const duration = template.durationDays > 1 ? ` (${template.durationDays} days)` : '';
    return `${day} ${month} every year${duration}`;
  }
  const week = template.weekOfMonth ? WEEK_OF_MONTH_LABELS[template.weekOfMonth] : '?';
  const day = template.dayOfWeek ? DAY_OF_WEEK_LABELS[template.dayOfWeek] : '?';
  const duration = template.durationDays > 1 ? ` (${template.durationDays} days)` : '';
  return `${week} ${day} every month${duration}`;
}

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
  'recurrenceType' | 'month' | 'dayOfMonth' | 'weekOfMonth' | 'dayOfWeek' | 'durationDays' | 'intervalCount'
>;

/** "every 2 weeks" / "every week" -- interval of 1 collapses to the plain unit name, matching how
 *  iOS/Google Calendar phrase their own Repeat summaries. */
function everyUnit(intervalCount: number, unitSingular: string, unitPlural: string): string {
  return intervalCount > 1 ? `every ${intervalCount} ${unitPlural}` : `every ${unitSingular}`;
}

/** One-line human summary of a HolidayTemplate's recurrence rule, e.g. "26 Jan every year",
 *  "2nd Saturday every month", "every 2 weeks on Monday", "every 3 days" -- used by the list
 *  screen's card/table views and the form's live preview. */
export function formatRecurrenceSummary(template: RecurrenceShape): string {
  const interval = template.intervalCount || 1;
  const duration = template.durationDays > 1 ? ` (${template.durationDays} days)` : '';

  if (template.recurrenceType === 'YEARLY') {
    const day = template.dayOfMonth ?? '?';
    const month = template.month ? MONTH_NAMES[template.month - 1] : '?';
    return `${day} ${month} ${everyUnit(interval, 'year', 'years')}${duration}`;
  }
  if (template.recurrenceType === 'MONTHLY') {
    const pattern = template.dayOfMonth != null
      ? `day ${template.dayOfMonth}`
      : `${template.weekOfMonth ? WEEK_OF_MONTH_LABELS[template.weekOfMonth] : '?'} ${template.dayOfWeek ? DAY_OF_WEEK_LABELS[template.dayOfWeek] : '?'}`;
    return `${pattern} ${everyUnit(interval, 'month', 'months')}${duration}`;
  }
  if (template.recurrenceType === 'WEEKLY') {
    const day = template.dayOfWeek ? DAY_OF_WEEK_LABELS[template.dayOfWeek] : '?';
    return `${everyUnit(interval, 'week', 'weeks')} on ${day}${duration}`;
  }
  return `${everyUnit(interval, 'day', 'days')}${duration}`;
}

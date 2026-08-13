import { AppDayOfWeek, CalendarEventType } from '../academic-year.model';

/** Shared display constants for the Academic Calendar screen and its day-detail flyout.
 *  Kept in their own module (rather than defined in academic-calendar.component.ts and
 *  imported by the flyout) so the two components don't form a circular import -- the flyout
 *  is itself used inside academic-calendar.component.html. */

export const DAY_OF_WEEK_LABELS: Record<AppDayOfWeek, string> = {
  MONDAY: 'Monday',
  TUESDAY: 'Tuesday',
  WEDNESDAY: 'Wednesday',
  THURSDAY: 'Thursday',
  FRIDAY: 'Friday',
  SATURDAY: 'Saturday',
};

export const EVENT_TYPE_LABELS: Record<CalendarEventType, string> = {
  HOLIDAY: 'Holiday',
  EXAM: 'Exam',
  CULTURAL: 'Cultural',
  SPORTS: 'Sports',
  WORKSHOP: 'Workshop',
  OTHER: 'Other',
};

export const EVENT_TYPE_ICONS: Record<CalendarEventType, string> = {
  HOLIDAY: 'beach_access',
  EXAM: 'quiz',
  CULTURAL: 'theater_comedy',
  SPORTS: 'sports_soccer',
  WORKSHOP: 'handyman',
  OTHER: 'event',
};

export const EVENT_TYPE_BADGE_CLASS: Record<CalendarEventType, string> = {
  HOLIDAY: 'cms-badge--amber',
  EXAM: 'cms-badge--red',
  CULTURAL: 'cms-badge--violet',
  SPORTS: 'cms-badge--cyan',
  WORKSHOP: 'cms-badge--blue',
  OTHER: 'cms-badge--gray',
};

/** Day-mapping override marker on the month grid -- same cms-badge--* color system as event
 *  chips (not the separate CmsStatusBadge lifecycle-status system, since this marks an
 *  event/annotation type on a date, not a workflow state). 'green' is verified defined in
 *  styles.scss and not already claimed by any EVENT_TYPE_BADGE_CLASS value above. */
export const DAY_MAPPING_ICON = 'swap_horiz';
export const DAY_MAPPING_BADGE_CLASS = 'cms-badge--green';

import { CalendarEventType, HolidayCategory, AppDayOfWeek } from '../academic-year/academic-year.model';

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export type HolidayRecurrenceType = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY';
export type WeekOfMonth = 'FIRST' | 'SECOND' | 'THIRD' | 'FOURTH' | 'LAST';

export interface HolidayTemplate {
  id: number;
  name: string;
  recurrenceType: HolidayRecurrenceType;
  /** Which event type this template seeds -- originally always HOLIDAY (the master screen
   *  predates this field), now any type since a repeating event can be created inline from the
   *  Add Event form regardless of type. */
  eventType: CalendarEventType;
  /** Only meaningful when eventType === 'HOLIDAY'. */
  holidayCategory: HolidayCategory | null;
  description: string | null;
  durationDays: number;
  /** "Every N [recurrenceType units]". */
  intervalCount: number;
  /** Required whenever intervalCount > 1, and always for DAILY. */
  anchorDate: string | null;
  /** Null means repeats forever. */
  endDate: string | null;
  month: number | null;
  dayOfMonth: number | null;
  weekOfMonth: WeekOfMonth | null;
  dayOfWeek: AppDayOfWeek | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface HolidayTemplateRequest {
  name: string;
  recurrenceType: HolidayRecurrenceType;
  eventType?: CalendarEventType;
  holidayCategory?: HolidayCategory | null;
  description?: string;
  durationDays?: number;
  intervalCount?: number;
  anchorDate?: string | null;
  endDate?: string | null;
  month?: number | null;
  dayOfMonth?: number | null;
  weekOfMonth?: WeekOfMonth | null;
  dayOfWeek?: AppDayOfWeek | null;
  isActive?: boolean;
}

import { HolidayCategory, AppDayOfWeek } from '../academic-year/academic-year.model';

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export type HolidayRecurrenceType = 'YEARLY' | 'MONTHLY';
export type WeekOfMonth = 'FIRST' | 'SECOND' | 'THIRD' | 'FOURTH' | 'LAST';

export interface HolidayTemplate {
  id: number;
  name: string;
  recurrenceType: HolidayRecurrenceType;
  holidayCategory: HolidayCategory | null;
  description: string | null;
  durationDays: number;
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
  holidayCategory: HolidayCategory | null;
  description?: string;
  durationDays?: number;
  month?: number | null;
  dayOfMonth?: number | null;
  weekOfMonth?: WeekOfMonth | null;
  dayOfWeek?: AppDayOfWeek | null;
  isActive?: boolean;
}

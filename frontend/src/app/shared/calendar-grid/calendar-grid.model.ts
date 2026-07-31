export interface CalendarDayCell {
  date: Date;
  iso: string;
  dayNum: number;
  isCurrentMonth: boolean;
  isToday: boolean;
}

export interface CalendarMonthGrid {
  year: number;
  month: number;
  label: string;
  days: CalendarDayCell[];
}

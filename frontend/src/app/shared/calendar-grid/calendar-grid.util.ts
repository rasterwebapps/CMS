import { CalendarDayCell, CalendarMonthGrid } from './calendar-grid.model';

const MONTH_NAMES = Array.from({ length: 12 }, (_, i) =>
  new Intl.DateTimeFormat('en', { month: 'long' }).format(new Date(2000, i, 1)));

export function toIso(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

/** Pads a calendar month to full weeks (Sunday-aligned start, complete final week) -- the same
 *  algorithm academic-calendar.component.ts uses for the Academic Calendar's month view, ported
 *  here as a shared, event-agnostic util so the Timetable month view doesn't duplicate it while
 *  leaving that already-shipped screen untouched. */
export function buildMonthGrid(year: number, month: number): CalendarMonthGrid {
  const firstDay = new Date(year, month, 1);
  const lastDay = new Date(year, month + 1, 0);
  const startDow = firstDay.getDay();
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const days: CalendarDayCell[] = [];
  for (let pad = 0; pad < startDow; pad++) {
    days.push(buildDayCell(new Date(year, month, -startDow + pad + 1), false, today));
  }
  for (let d = 1; d <= lastDay.getDate(); d++) {
    days.push(buildDayCell(new Date(year, month, d), true, today));
  }
  while (days.length % 7 !== 0) {
    days.push(buildDayCell(new Date(year, month, days.length - startDow + 1), false, today));
  }

  return { year, month, label: `${MONTH_NAMES[month]} ${year}`, days };
}

function buildDayCell(date: Date, isCurrentMonth: boolean, today: Date): CalendarDayCell {
  return {
    date,
    iso: toIso(date),
    dayNum: date.getDate(),
    isCurrentMonth,
    isToday: date.getTime() === today.getTime(),
  };
}

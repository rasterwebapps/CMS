import { AcademicYear, CalendarEvent, DayMapping, TermInstance } from '../academic-year.model';

export interface MonthGrid {
  year: number;
  month: number; // 0-based
  label: string;
  days: DayCell[];
}

export interface DayCell {
  date: Date;
  dayNum: number;
  isCurrentMonth: boolean;
  termStatus: 'UPCOMING' | 'ONGOING' | 'COMPLETED' | null;
  termName: string | null;
  events: CalendarEvent[];
  dayMapping: DayMapping | null;
  isToday: boolean;
}

const MONTH_NAMES = Array.from({ length: 12 }, (_, i) =>
  new Intl.DateTimeFormat('en', { month: 'long' }).format(new Date(2000, i, 1)),
);

export function toIso(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

/** Shared by the main Academic Calendar page's full Month Grid view and the day-detail flyout's
 *  inline mini-calendar -- both need the exact same "which term/events land on this day" math,
 *  just rendered at different sizes. Extracted (mirroring blocked-period-summary.util.ts's own
 *  extraction) so neither copy can silently drift from the other. `getTermStatus`/`getTermLabel`
 *  are passed in rather than duplicated here since they're pure functions of `term` + `today`
 *  that the main page's stats/progress-bar code also needs to share. */
export function buildMonthGrids(
  ay: AcademicYear,
  termInstances: TermInstance[],
  events: CalendarEvent[],
  dayMappings: DayMapping[],
  getTermStatus: (term: TermInstance, today: Date) => 'UPCOMING' | 'ONGOING' | 'COMPLETED',
  getTermLabel: (term: TermInstance) => string,
): MonthGrid[] {
  const start = new Date(ay.startDate);
  const end = new Date(ay.endDate);
  const grids: MonthGrid[] = [];

  let cur = new Date(start.getFullYear(), start.getMonth(), 1);
  const endMonthStart = new Date(end.getFullYear(), end.getMonth(), 1);
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  while (cur <= endMonthStart) {
    const year = cur.getFullYear();
    const month = cur.getMonth();
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);

    const startDow = firstDay.getDay();
    const days: DayCell[] = [];

    for (let pad = 0; pad < startDow; pad++) {
      const d = new Date(year, month, -startDow + pad + 1);
      days.push(buildDayCell(d, false, termInstances, events, dayMappings, today, getTermStatus, getTermLabel));
    }
    for (let d = 1; d <= lastDay.getDate(); d++) {
      const date = new Date(year, month, d);
      days.push(buildDayCell(date, true, termInstances, events, dayMappings, today, getTermStatus, getTermLabel));
    }
    while (days.length % 7 !== 0) {
      const date = new Date(year, month + 1, days.length - lastDay.getDate() - startDow + 1);
      days.push(buildDayCell(date, false, termInstances, events, dayMappings, today, getTermStatus, getTermLabel));
    }

    grids.push({ year, month, label: `${MONTH_NAMES[month]} ${year}`, days });
    cur = new Date(year, month + 1, 1);
  }
  return grids;
}

function buildDayCell(
  date: Date,
  isCurrentMonth: boolean,
  termInstances: TermInstance[],
  events: CalendarEvent[],
  dayMappings: DayMapping[],
  today: Date,
  getTermStatus: (term: TermInstance, today: Date) => 'UPCOMING' | 'ONGOING' | 'COMPLETED',
  getTermLabel: (term: TermInstance) => string,
): DayCell {
  const iso = toIso(date);
  const term = termInstances.find((item) => item.startDate <= iso && item.endDate >= iso);
  const dayEvents = events.filter((e) => e.startDate <= iso && e.endDate >= iso);
  const dayMapping = dayMappings.find((m) => m.mappedDate === iso) ?? null;
  return {
    date,
    dayNum: date.getDate(),
    isCurrentMonth,
    termStatus: term ? getTermStatus(term, today) : null,
    termName: term ? getTermLabel(term) : null,
    events: dayEvents,
    dayMapping,
    isToday: date.getTime() === today.getTime(),
  };
}

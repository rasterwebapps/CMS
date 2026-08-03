import { Component, computed, input, output, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { CalendarEvent } from '../../../academic-year.model';
import { toIso } from '../../month-grid.util';

interface MiniDayCell {
  date: Date;
  iso: string;
  dayNum: number;
  isCurrentMonth: boolean;
  isToday: boolean;
  isSelected: boolean;
  isInRange: boolean;
  hasEvents: boolean;
}

/** Inline month-view calendar for the day-detail flyout -- sits above the native Start/End Date
 *  typed inputs (both remain, kept bidirectionally in sync; the calendar is a visual alternative,
 *  not a replacement). Serves two roles depending on what the flyout has open:
 *  - No Add/Edit Event form open: single-date mode (`selectedDateIso`) -- clicking a day is the
 *    same "change which date this panel is viewing" action the old bare native input drove.
 *  - Add/Edit Event form open: range mode (`rangeStart`/`rangeEnd`) -- clicking highlights the
 *    event's date span for a visual read of the range being picked in the typed fields below. */
@Component({
  selector: 'app-flyout-mini-calendar',
  standalone: true,
  imports: [MatIconModule, MatButtonModule],
  templateUrl: './flyout-mini-calendar.component.html',
  styleUrl: './flyout-mini-calendar.component.scss',
})
export class FlyoutMiniCalendarComponent {
  readonly events = input<CalendarEvent[]>([]);
  readonly selectedDateIso = input<string | null>(null);
  readonly rangeStart = input<string | null>(null);
  readonly rangeEnd = input<string | null>(null);
  readonly minDate = input<string | null>(null);
  readonly maxDate = input<string | null>(null);

  readonly dayClicked = output<string>();

  private static readonly MONTH_NAMES = Array.from({ length: 12 }, (_, i) =>
    new Intl.DateTimeFormat('en', { month: 'long' }).format(new Date(2000, i, 1)),
  );

  /** Which month is currently displayed -- initialized lazily from whichever anchor date is
   *  available the first time the grid is computed, then only moved by the prev/next buttons. */
  private readonly monthOverride = signal<{ year: number; month: number } | null>(null);

  protected readonly monthLabel = computed(() => {
    const { year, month } = this.displayedYearMonth();
    return `${FlyoutMiniCalendarComponent.MONTH_NAMES[month]} ${year}`;
  });

  protected readonly weeks = computed<MiniDayCell[][]>(() => {
    const { year, month } = this.displayedYearMonth();
    const cells = this.buildMonthCells(year, month);
    const weeks: MiniDayCell[][] = [];
    for (let i = 0; i < cells.length; i += 7) {
      weeks.push(cells.slice(i, i + 7));
    }
    return weeks;
  });

  protected onDayClick(cell: MiniDayCell): void {
    this.dayClicked.emit(cell.iso);
  }

  protected goToPreviousMonth(): void {
    const { year, month } = this.displayedYearMonth();
    this.monthOverride.set(month === 0 ? { year: year - 1, month: 11 } : { year, month: month - 1 });
  }

  protected goToNextMonth(): void {
    const { year, month } = this.displayedYearMonth();
    this.monthOverride.set(month === 11 ? { year: year + 1, month: 0 } : { year, month: month + 1 });
  }

  private displayedYearMonth(): { year: number; month: number } {
    const override = this.monthOverride();
    if (override) return override;
    const anchorIso = this.selectedDateIso() ?? this.rangeStart() ?? this.rangeEnd();
    const anchor = anchorIso ? new Date(`${anchorIso}T00:00:00`) : new Date();
    return { year: anchor.getFullYear(), month: anchor.getMonth() };
  }

  private buildMonthCells(year: number, month: number): MiniDayCell[] {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    const startDow = firstDay.getDay();

    const cells: MiniDayCell[] = [];
    for (let pad = 0; pad < startDow; pad++) {
      cells.push(this.buildCell(new Date(year, month, -startDow + pad + 1), false, today));
    }
    for (let d = 1; d <= lastDay.getDate(); d++) {
      cells.push(this.buildCell(new Date(year, month, d), true, today));
    }
    while (cells.length % 7 !== 0) {
      cells.push(this.buildCell(new Date(year, month + 1, cells.length - lastDay.getDate() - startDow + 1), false, today));
    }
    return cells;
  }

  private buildCell(date: Date, isCurrentMonth: boolean, today: Date): MiniDayCell {
    const iso = toIso(date);
    const rangeStart = this.rangeStart();
    const rangeEnd = this.rangeEnd();
    const isInRange = !!(rangeStart && rangeEnd && iso >= rangeStart && iso <= rangeEnd);
    return {
      date,
      iso,
      dayNum: date.getDate(),
      isCurrentMonth,
      isToday: date.getTime() === today.getTime(),
      isSelected: iso === this.selectedDateIso(),
      isInRange,
      hasEvents: this.events().some((e) => e.startDate <= iso && e.endDate >= iso),
    };
  }

  protected trackByIso(_index: number, cell: MiniDayCell): string {
    return cell.iso;
  }

  protected trackByWeek(index: number): number {
    return index;
  }
}

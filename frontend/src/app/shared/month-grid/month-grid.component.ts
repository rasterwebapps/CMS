import { Component, EventEmitter, Input, Output, computed, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ClassScheduleOccurrence } from '../../features/timetable/timetable.model';
import { buildMonthGrid } from '../calendar-grid/calendar-grid.util';
import { CalendarDayCell } from '../calendar-grid/calendar-grid.model';

const WEEKDAY_LABELS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

@Component({
  selector: 'cms-month-grid',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, MatTooltipModule],
  templateUrl: './month-grid.component.html',
  styleUrl: './month-grid.component.scss',
})
export class CmsMonthGridComponent {
  @Input({ required: true }) year!: number;
  @Input({ required: true }) month!: number;

  private readonly _occurrences = signal<ClassScheduleOccurrence[]>([]);
  @Input() set occurrences(value: ClassScheduleOccurrence[] | null | undefined) {
    this._occurrences.set(value ?? []);
  }

  @Output() monthChange = new EventEmitter<{ year: number; month: number }>();
  @Output() dayClick = new EventEmitter<string>();

  protected readonly weekdayLabels = WEEKDAY_LABELS;

  protected readonly grid = computed(() => buildMonthGrid(this.year, this.month));

  protected readonly occurrencesByDate = computed(() => {
    const map = new Map<string, ClassScheduleOccurrence[]>();
    for (const occ of this._occurrences()) {
      const list = map.get(occ.date) ?? [];
      list.push(occ);
      map.set(occ.date, list);
    }
    return map;
  });

  protected occurrencesFor(cell: CalendarDayCell): ClassScheduleOccurrence[] {
    return this.occurrencesByDate().get(cell.iso) ?? [];
  }

  protected previousMonth(): void {
    const m = this.month === 0 ? 11 : this.month - 1;
    const y = this.month === 0 ? this.year - 1 : this.year;
    this.monthChange.emit({ year: y, month: m });
  }

  protected nextMonth(): void {
    const m = this.month === 11 ? 0 : this.month + 1;
    const y = this.month === 11 ? this.year + 1 : this.year;
    this.monthChange.emit({ year: y, month: m });
  }

  protected today(): void {
    const now = new Date();
    this.monthChange.emit({ year: now.getFullYear(), month: now.getMonth() });
  }
}

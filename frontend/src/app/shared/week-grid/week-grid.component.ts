import { Component, EventEmitter, Input, Output, computed, signal } from '@angular/core';
import { CmsEmptyStateComponent } from '../empty-state/empty-state.component';
import {
  WeekGridSession,
  WeekGridMode,
  WEEK_GRID_DAYS,
  WEEK_GRID_DAY_LABELS,
} from './week-grid.model';

interface WeekGridRow {
  key: string;
  label: string;
  startTime: string;
  endTime: string;
}

/**
 * The first calendar/week-grid component in this codebase — a day-columns x period-rows grid
 * shared by the timetable draft-review screen, the admin browse screen, and personal "my
 * timetable" views. Rows are derived purely from the distinct time windows present in the
 * `sessions` input (no separate Period/LabSlot master fetch needed here), so it works for both
 * THEORY and LAB session types without knowing about either master specifically.
 */
@Component({
  selector: 'cms-week-grid',
  standalone: true,
  imports: [CmsEmptyStateComponent],
  templateUrl: './week-grid.component.html',
  styleUrl: './week-grid.component.scss',
})
export class CmsWeekGridComponent {
  private readonly _sessions = signal<WeekGridSession[]>([]);
  @Input() set sessions(value: WeekGridSession[] | null | undefined) {
    this._sessions.set(value ?? []);
  }

  @Input() mode: WeekGridMode = 'browse';

  /** Gates the review-mode Generate/Approve/Discard toolbar — the consuming screen owns the
   *  actual permission check and passes the result down, since this shared component has no
   *  knowledge of the app's permission model. */
  @Input() allowGenerate = false;
  @Input() allowManage = false;

  private readonly _holidayDayIndexes = signal<number[]>([]);
  @Input() set holidayDayIndexes(value: number[] | null | undefined) {
    this._holidayDayIndexes.set(value ?? []);
  }

  @Input() weekStart: string | null = null;
  @Input() generating = false;
  @Input() saving = false;

  @Output() sessionClick = new EventEmitter<WeekGridSession>();
  @Output() approveClick = new EventEmitter<void>();
  @Output() discardClick = new EventEmitter<void>();
  @Output() generateClick = new EventEmitter<void>();

  protected readonly days = WEEK_GRID_DAYS;
  protected readonly dayLabels = WEEK_GRID_DAY_LABELS;

  protected readonly isHoliday = computed(() => {
    const set = new Set(this._holidayDayIndexes());
    return (dayIndex: number) => set.has(dayIndex);
  });

  protected readonly rows = computed<WeekGridRow[]>(() => {
    const seen = new Map<string, WeekGridRow>();
    for (const s of this._sessions()) {
      const key = `${s.startTime}-${s.endTime}`;
      if (!seen.has(key)) {
        seen.set(key, { key, label: s.slotName || `${s.startTime}–${s.endTime}`, startTime: s.startTime, endTime: s.endTime });
      }
    }
    return Array.from(seen.values()).sort((a, b) => a.startTime.localeCompare(b.startTime));
  });

  protected readonly isEmpty = computed(() => this._sessions().length === 0);

  protected cell(day: string, row: WeekGridRow): WeekGridSession[] {
    return this._sessions().filter((s) => s.dayOfWeek === day && s.startTime === row.startTime && s.endTime === row.endTime);
  }

  protected onSessionClick(session: WeekGridSession): void {
    this.sessionClick.emit(session);
  }
}

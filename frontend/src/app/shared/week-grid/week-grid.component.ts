import { Component, EventEmitter, Input, Output, computed, signal } from '@angular/core';
import { CmsEmptyStateComponent } from '../empty-state/empty-state.component';
import {
  WeekGridSession,
  WeekGridMode,
  WeekGridCandidateCell,
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
 * timetable" views. Rows are derived from the distinct time windows present in the `sessions`
 * input, plus (while `swapMode` is on) any extra windows only present in `candidateCells` — a
 * period nobody currently has a session in still needs a visible row to be a swap target.
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

  /** Gates the review-mode Generate/Approve/Discard/Revert toolbar — the consuming screen owns
   *  the actual permission check and passes the result down, since this shared component has no
   *  knowledge of the app's permission model. */
  @Input() allowGenerate = false;
  @Input() allowManage = false;
  @Input() allowRevert = false;

  private readonly _holidayDayIndexes = signal<number[]>([]);
  @Input() set holidayDayIndexes(value: number[] | null | undefined) {
    this._holidayDayIndexes.set(value ?? []);
  }

  @Input() weekStart: string | null = null;
  @Input() generating = false;
  @Input() saving = false;

  /** Swap mode: the consuming screen has a session selected and is offering candidate target
   *  cells for it. Non-candidate cells dim out; candidate cells highlight and become clickable. */
  @Input() swapMode = false;
  @Input() swapSourceSessionId: number | null = null;

  private readonly _candidateCells = signal<WeekGridCandidateCell[]>([]);
  @Input() set candidateCells(value: WeekGridCandidateCell[] | null | undefined) {
    this._candidateCells.set(value ?? []);
  }

  @Output() sessionClick = new EventEmitter<WeekGridSession>();
  @Output() approveClick = new EventEmitter<void>();
  @Output() discardClick = new EventEmitter<void>();
  @Output() generateClick = new EventEmitter<void>();
  @Output() revertClick = new EventEmitter<void>();
  @Output() cellClick = new EventEmitter<WeekGridCandidateCell>();

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
    for (const c of this._candidateCells()) {
      const key = `${c.startTime}-${c.endTime}`;
      if (!seen.has(key)) {
        seen.set(key, { key, label: `${c.startTime}–${c.endTime}`, startTime: c.startTime, endTime: c.endTime });
      }
    }
    return Array.from(seen.values()).sort((a, b) => a.startTime.localeCompare(b.startTime));
  });

  protected readonly isEmpty = computed(() => this._sessions().length === 0);

  /** The review screen only ever loads one status at a time (DRAFT via /draft, PUBLISHED via the
   *  plain list endpoint), so the first session's status tells us which set is on screen — a live
   *  timetable gets the Revert-to-Draft action instead of Generate/Discard/Approve. */
  protected readonly isPublishedView = computed(() => {
    const sessions = this._sessions();
    return sessions.length > 0 && sessions[0].status === 'PUBLISHED';
  });

  protected cell(day: string, row: WeekGridRow): WeekGridSession[] {
    return this._sessions().filter((s) => s.dayOfWeek === day && s.startTime === row.startTime && s.endTime === row.endTime);
  }

  protected candidateFor(day: string, row: WeekGridRow): WeekGridCandidateCell | undefined {
    return this._candidateCells().find((c) => c.dayOfWeek === day && c.startTime === row.startTime && c.endTime === row.endTime);
  }

  protected isSourceCell(day: string, row: WeekGridRow): boolean {
    if (this.swapSourceSessionId == null) return false;
    return this.cell(day, row).some((s) => s.id === this.swapSourceSessionId);
  }

  protected onSessionClick(session: WeekGridSession, day: string, row: WeekGridRow, event: Event): void {
    if (this.swapMode) {
      event.stopPropagation();
      const candidate = this.candidateFor(day, row);
      if (candidate) this.cellClick.emit(candidate);
      return;
    }
    this.sessionClick.emit(session);
  }

  protected onCellClick(day: string, row: WeekGridRow): void {
    if (!this.swapMode) return;
    const candidate = this.candidateFor(day, row);
    if (candidate) this.cellClick.emit(candidate);
  }
}

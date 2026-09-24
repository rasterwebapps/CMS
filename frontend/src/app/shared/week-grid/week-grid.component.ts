import { Component, EventEmitter, Input, Output, computed, signal } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CmsEmptyStateComponent } from '../empty-state/empty-state.component';
import { colorForSessionType, SessionTypeForColor } from '../util/session-color.util';
import {
  WeekGridSession,
  WeekGridMode,
  WeekGridCandidateCell,
  WeekGridHolidayInfo,
  WeekGridPeriod,
  WEEK_GRID_DAYS,
  WEEK_GRID_DAY_LABELS,
  WEEK_GRID_HOLIDAY_CATEGORY_LABELS,
} from './week-grid.model';

interface WeekGridRow {
  key: string;
  label: string;
  startTime: string;
  endTime: string;
}

/** One day's row, left to right: a real Period column renders individually (`kind: 'period'`,
 *  unchanged); a run of consecutive Period columns covered by the same off-grid entry (e.g. a
 *  Clinical Shift duty window, {@link CmsWeekGridComponent.isSynthetic}) collapses into one
 *  `kind: 'shift'` segment spanning that many columns -- mirrors Timetable Builder's own
 *  `rowSegments`/`shiftWindowFor` (timetable-builder.component.ts), just driven by the time
 *  windows already present in `sessions` instead of a separately-fetched Period/ClinicalShiftWindow
 *  model, since this shared component has neither. */
type WeekGridSegment =
  | { kind: 'period'; key: string; row: WeekGridRow }
  | { kind: 'shift'; key: string; span: number; startTime: string; endTime: string; sessions: WeekGridSession[] };

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
  imports: [CmsEmptyStateComponent, MatTooltipModule, NgTemplateOutlet],
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

  /** Non-null disables the Publish button and shows this as its tooltip, independent of
   *  {@link allowManage}/{@link saving} — used by Draft Review's Conflict Inspector acknowledgment
   *  gate (OC-258) to block publishing without hiding the button/toolbar entirely. Null (default)
   *  leaves every other consumer of this component unaffected. */
  @Input() publishDisabledReason: string | null = null;

  private readonly _holidays = signal<WeekGridHolidayInfo[]>([]);
  @Input() set holidays(value: WeekGridHolidayInfo[] | null | undefined) {
    this._holidays.set(value ?? []);
  }

  protected readonly holidayCategoryLabels = WEEK_GRID_HOLIDAY_CATEGORY_LABELS;

  @Input() weekStart: string | null = null;
  @Input() generating = false;
  @Input() saving = false;

  /** The selected term's own real bounds, in the same YYYY-MM-DD shape as {@link weekStart} --
   *  both null (the default) for every consumer that doesn't pass them, which keeps the Generic/
   *  Review views (no {@link weekStart} either) and any caller that hasn't opted in unaffected. Lets
   *  {@link isOutOfTerm} flag a day column that falls outside the term (e.g. the Monday-Wednesday
   *  of a week whose Thursday is the term's actual start date) with the same dimmed-column +
   *  "Not in Term" badge treatment Holiday already uses, instead of that day silently rendering
   *  every period as empty with no explanation. */
  @Input() termStartDate: string | null = null;
  @Input() termEndDate: string | null = null;

  /** Swap mode: the consuming screen has a session selected and is offering candidate target
   *  cells for it. Non-candidate cells dim out; candidate cells highlight and become clickable. */
  @Input() swapMode = false;
  @Input() swapSourceSessionId: number | null = null;

  private readonly _candidateCells = signal<WeekGridCandidateCell[]>([]);
  @Input() set candidateCells(value: WeekGridCandidateCell[] | null | undefined) {
    this._candidateCells.set(value ?? []);
  }

  /** See {@link WeekGridPeriod}'s own doc comment. Empty (default) for every consumer that
   *  doesn't pass it, leaving {@link rows} exactly as it behaved before this input existed. */
  private readonly _allPeriods = signal<WeekGridPeriod[]>([]);
  @Input() set allPeriods(value: WeekGridPeriod[] | null | undefined) {
    this._allPeriods.set(value ?? []);
  }

  @Output() sessionClick = new EventEmitter<WeekGridSession>();
  @Output() approveClick = new EventEmitter<void>();
  @Output() discardClick = new EventEmitter<void>();
  @Output() generateClick = new EventEmitter<void>();
  @Output() revertClick = new EventEmitter<void>();
  @Output() cellClick = new EventEmitter<WeekGridCandidateCell>();

  /** The term's real working-Saturday count, from TermInstanceDto.workingSaturdayCount -- 0 means
   *  the term hasn't opted in to Saturday scheduling at all (see WorkingSaturdaysFlyoutComponent's
   *  doc comment: Mon-Fri only, hard-blocked). Null (default) keeps every existing consumer that
   *  doesn't pass it unaffected and always shows all 6 days, same as before this input existed. */
  @Input() workingSaturdayCount: number | null = null;

  protected readonly days = computed(() =>
    this.workingSaturdayCount === 0 ? WEEK_GRID_DAYS.filter((d) => d !== 'SATURDAY') : WEEK_GRID_DAYS);
  protected readonly dayLabels = WEEK_GRID_DAY_LABELS;

  protected readonly isHoliday = computed(() => {
    const set = new Set(this._holidays().map((h) => h.dayIndex));
    return (dayIndex: number) => set.has(dayIndex);
  });

  protected readonly holidayFor = computed(() => {
    const map = new Map(this._holidays().map((h) => [h.dayIndex, h]));
    return (dayIndex: number) => map.get(dayIndex);
  });

  /** Actual calendar date for a day column, when the consumer knows which week is being viewed
   *  (weekStart is a Monday date, dayIndex 0=Monday..5=Saturday per WEEK_GRID_DAYS) -- null for
   *  review/browse screens that never pass weekStart, since a DRAFT template has no real date. */
  protected dateLabelFor(dayIndex: number): string | null {
    if (!this.weekStart) return null;
    const date = new Date(`${this.weekStart}T00:00:00`);
    date.setDate(date.getDate() + dayIndex);
    return date.toLocaleDateString('en-GB', { day: 'numeric', month: 'short' });
  }

  /** True when day column `dayIndex`'s real calendar date falls outside [termStartDate,
   *  termEndDate] -- e.g. Monday/Tuesday/Wednesday of a week whose Thursday is the term's actual
   *  start date, still shown so the in-term Thu-Sat of that same week remain visible. Requires the
   *  same {@link weekStart} {@link dateLabelFor} does plus both term bounds; false whenever any of
   *  the three is missing, so Generic/Review consumers (no weekStart, no term bounds) are
   *  unaffected. */
  protected isOutOfTerm(dayIndex: number): boolean {
    if (!this.weekStart || !this.termStartDate || !this.termEndDate) return false;
    const date = new Date(`${this.weekStart}T00:00:00`);
    date.setDate(date.getDate() + dayIndex);
    const iso = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
    return iso < this.termStartDate || iso > this.termEndDate;
  }

  /** Columns come only from sessions/candidates tied to a real Period ({@code periodId != null}).
   *  An off-grid entry (periodId null -- a Clinical Shift duty block, see {@link isSynthetic}) must
   *  never mint its own column: its time window (e.g. a 06:00-14:10 bus-departure/return buffer)
   *  doesn't match any single Period's exact start/end, so before this filter it always produced a
   *  spurious extra column sitting off to the side instead of occupying the real Period columns it
   *  actually covers -- see {@link daySegments}, which renders it as a spanning block over those
   *  columns instead. */
  protected readonly rows = computed<WeekGridRow[]>(() => {
    const seen = new Map<string, WeekGridRow>();
    // Master Period rows go in first, when supplied — their name is authoritative, so a session
    // sharing one of these exact windows below must never overwrite it (see the `masterKeys` guard).
    const masterKeys = new Set<string>();
    for (const p of this._allPeriods()) {
      const key = `${p.startTime}-${p.endTime}`;
      seen.set(key, { key, label: p.name, startTime: p.startTime, endTime: p.endTime });
      masterKeys.add(key);
    }
    for (const s of this._sessions()) {
      if (s.periodId == null) continue;
      const key = `${s.startTime}-${s.endTime}`;
      if (masterKeys.has(key)) continue;
      const label = s.slotName || `${s.startTime}–${s.endTime}`;
      const existing = seen.get(key);
      if (!existing) {
        seen.set(key, { key, label, startTime: s.startTime, endTime: s.endTime });
      } else if (existing.label !== label) {
        // A real Period's slotName is identical for every session that shares its time window, so
        // a mismatch here would only happen for two distinct Periods that coincidentally share one
        // exact start/end -- keeping whichever session was seen first would mislabel every other
        // session's column with a name that doesn't describe it.
        existing.label = `${s.startTime}–${s.endTime}`;
      }
    }
    for (const c of this._candidateCells()) {
      if (c.periodId == null) continue;
      const key = `${c.startTime}-${c.endTime}`;
      if (!seen.has(key)) {
        seen.set(key, { key, label: `${c.startTime}–${c.endTime}`, startTime: c.startTime, endTime: c.endTime });
      }
    }
    return Array.from(seen.values()).sort((a, b) => a.startTime.localeCompare(b.startTime));
  });

  /** Off-grid entries (periodId null) for one day, grouped by their exact time window -- two
   *  entries sharing one window (e.g. a Clinical Shift group running in parallel at two venues,
   *  see backend {@code TimetableSkeletonService#findClinicalShiftGridEntries}) render as chips
   *  inside the same spanning block, the same way a normal Period cell already groups multiple
   *  sessions together. */
  private readonly shiftGroupsByDay = computed(() => {
    const byDay = new Map<string, Map<string, WeekGridSession[]>>();
    for (const s of this._sessions()) {
      if (s.periodId != null) continue;
      let byWindow = byDay.get(s.dayOfWeek);
      if (!byWindow) byDay.set(s.dayOfWeek, (byWindow = new Map()));
      const key = `${s.startTime}-${s.endTime}`;
      const bucket = byWindow.get(key);
      if (bucket) bucket.push(s); else byWindow.set(key, [s]);
    }
    return byDay;
  });

  /** Same overlap test Timetable Builder's {@link shiftWindowFor} uses (period.startTime <
   *  window.endTime && window.startTime < period.endTime), just against the columns already
   *  derived from real sessions instead of a fetched Period entity. */
  private shiftWindowFor(day: string, row: WeekGridRow): { key: string; startTime: string; endTime: string; sessions: WeekGridSession[] } | null {
    for (const [key, sessions] of this.shiftGroupsByDay().get(day) ?? []) {
      const [startTime, endTime] = key.split('-');
      if (row.startTime < endTime && startTime < row.endTime) {
        return { key, startTime, endTime, sessions };
      }
    }
    return null;
  }

  protected daySegments(day: string): WeekGridSegment[] {
    const rows = this.rows();
    const segments: WeekGridSegment[] = [];
    let i = 0;
    while (i < rows.length) {
      const window = this.shiftWindowFor(day, rows[i]);
      if (!window) {
        segments.push({ kind: 'period', key: rows[i].key, row: rows[i] });
        i++;
        continue;
      }
      let span = 1;
      while (i + span < rows.length && this.shiftWindowFor(day, rows[i + span])?.key === window.key) {
        span++;
      }
      segments.push({ kind: 'shift', key: `shift-${window.key}-${day}`, span, startTime: window.startTime, endTime: window.endTime, sessions: window.sessions });
      i += span;
    }
    return segments;
  }

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

  /** A negative id is a synthetic, off-grid entry (e.g. a Clinical Shift Group duty roster block
   *  -- see backend TimetableSkeletonService#findClinicalShiftGridEntries) with no real
   *  ClassSchedule row behind it, so it can't be swapped or clicked into a detail action; same
   *  "negative id = non-interactive placeholder" convention ResourceGridCellResponse already uses. */
  protected isSynthetic(session: WeekGridSession): boolean {
    return session.id < 0;
  }

  /** Same primary-color-tint accent Timetable Builder/Day Agenda use for every session type — see
   *  {@link colorForSessionType}. WeekGridSession carries no coCurricular flag, so this always
   *  colors by session type alone. */
  protected sessionColor(sessionType: SessionTypeForColor): string {
    return colorForSessionType(sessionType);
  }

  /** `row` is null for a chip inside a spanning shift segment ({@link daySegments}) -- always
   *  paired with a synthetic session (see {@link isSynthetic}), which returns below before `row`
   *  is ever dereferenced, so the null case never reaches the swap-mode/candidateFor logic. */
  protected onSessionClick(session: WeekGridSession, day: string, row: WeekGridRow | null, event: Event): void {
    if (this.isSynthetic(session) || row == null) {
      event.stopPropagation();
      return;
    }
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

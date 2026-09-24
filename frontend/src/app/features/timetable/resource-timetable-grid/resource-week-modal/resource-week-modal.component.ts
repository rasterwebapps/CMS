import { Component, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TimetableService } from '../../timetable.service';
import { ResourceGridType, RoomKind } from '../../timetable.model';
import { CmsWeekGridComponent } from '../../../../shared/week-grid/week-grid.component';
import { WeekGridPeriod, WeekGridSession } from '../../../../shared/week-grid/week-grid.model';
import { CmsWeekNavigatorComponent } from '../../../../shared/week-navigator/week-navigator.component';
import { ToastService } from '../../../../core/toast/toast.service';
import { PeriodService } from '../../../period/period.service';

export interface ResourceWeekModalData {
  resourceType: ResourceGridType;
  resourceId: number;
  resourceName: string;
  termInstanceId: number;
  /** Required for a CLASSROOM-type resource -- see {@link RoomKind}'s own doc comment for why a
   *  raw resourceId alone can't safely identify one specific room. Round-tripped straight from the
   *  row's own {@code roomKind} (see resource-timetable-grid.component.ts's openWeekView). */
  roomKind: RoomKind | null;
  /** The selected term's own real date bounds -- Date mode's week-navigator clamps to these (same
   *  as timetable-view.component.ts's own dayMin/dayMax), and the very first landing week defaults
   *  inside them too. Without this, Date mode defaulted to *today's* real-calendar week regardless
   *  of whether the term had even started yet -- the recurring PUBLISHED template is keyed by
   *  termInstanceId + dayOfWeek only, with no per-date bound of its own, so a faculty's Full Week
   *  drill-in opened before a term's start date still rendered every recurring session under those
   *  real (but not-yet-real) calendar dates, reading as though classes were already happening. */
  termStartDate: string | null;
  termEndDate: string | null;
}

function mondayOf(date: Date): string {
  const d = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  const day = d.getDay(); // 0=Sunday..6=Saturday
  const diff = day === 0 ? -6 : 1 - day;
  d.setDate(d.getDate() + diff);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${dd}`;
}

function addDays(iso: string, days: number): string {
  const [y, m, d] = iso.split('-').map(Number);
  const date = new Date(y, m - 1, d);
  date.setDate(date.getDate() + days);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

/** Mirrors timetable-view.component.ts's identically-named private method -- see its own doc
 *  comment for the full reasoning. Clamps `date` into [min, max] and, only when that clamp landed
 *  on a mid-week term.startDate, skips forward to the next Monday so the default landing week isn't
 *  a confusing partial Mon-Wed the term hadn't started for yet (that first partial week stays
 *  reachable via Previous). No equivalent skip at the endDate boundary -- that IS the term's real
 *  last week. */
function defaultWeekStartInTerm(date: string, min: string | null, max: string | null): string {
  const clamped = min && date < min ? min : max && date > max ? max : date;
  if (clamped === date) return clamped;
  const clampedMonday = mondayOf(new Date(`${clamped}T00:00:00`));
  return clamped === min && clampedMonday < min ? addDays(clampedMonday, 7) : clampedMonday;
}

/** Drilled into from one row of the Resource Timetable's daily grid — this resource's own full
 *  Mon-Sat week, across every cohort (not just whichever single cohort the main Timetable screen's
 *  own Faculty/Room filter happens to be scoped to). Reuses the same shared cms-week-grid the
 *  cohort Timetable screen renders its Week/Date-wise views with, fed a resource-scoped session
 *  list instead of a cohort-scoped one. */
@Component({
  selector: 'app-resource-week-modal',
  standalone: true,
  imports: [MatDialogModule, MatProgressSpinnerModule, CmsWeekGridComponent, CmsWeekNavigatorComponent],
  templateUrl: './resource-week-modal.component.html',
  styleUrl: './resource-week-modal.component.scss',
})
export class ResourceWeekModalComponent {
  protected readonly data: ResourceWeekModalData = inject(MAT_DIALOG_DATA);
  private readonly timetableService = inject(TimetableService);
  private readonly periodService = inject(PeriodService);
  private readonly toast = inject(ToastService);

  protected readonly viewMode = signal<'DATE' | 'WEEKDAY'>('WEEKDAY');
  /** Defaults inside the selected term's own real date range (see {@link ResourceWeekModalData}'s
   *  termStartDate/termEndDate doc comment) rather than *today's* real-calendar week -- the modal
   *  can be opened for a term that hasn't started yet or has already ended. */
  protected readonly weekStart = signal(
    defaultWeekStartInTerm(mondayOf(new Date()), this.data.termStartDate, this.data.termEndDate));
  protected readonly loading = signal(false);
  protected readonly sessions = signal<WeekGridSession[]>([]);

  /** Passed to cms-week-grid's `allPeriods` input so every active period shows its own column even
   *  when this resource has nothing scheduled in it that week -- see WeekGridPeriod's doc comment.
   *  Without this, e.g. a faculty member with sessions only in Periods 1, 2, 7, 8 across the whole
   *  week rendered just those 4 columns, reading as though Periods 3-6 didn't exist that day. */
  protected readonly periods = signal<WeekGridPeriod[]>([]);

  constructor() {
    this.periodService.getAll(true).subscribe({
      next: (periods) => this.periods.set(periods),
      error: () => { /* Non-critical -- the grid just falls back to session-derived columns. */ },
    });
    this.load();
  }

  protected setViewMode(mode: 'DATE' | 'WEEKDAY'): void {
    if (this.viewMode() === mode) return;
    this.viewMode.set(mode);
    this.load();
  }

  protected onWeekStartChange(weekStart: string): void {
    this.weekStart.set(weekStart);
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    const weekStart = this.viewMode() === 'DATE' ? this.weekStart() : undefined;
    this.timetableService.getResourceWeekGrid(this.data.resourceType, this.data.resourceId, this.data.termInstanceId, weekStart, this.data.roomKind)
      .subscribe({
        next: (cells) => {
          this.sessions.set(cells.map((c): WeekGridSession => ({
            id: c.sessionId,
            sessionType: c.sessionType,
            status: c.status,
            subjectName: c.subjectName,
            subjectCode: c.subjectCode,
            facultyName: c.facultyName,
            roomName: c.roomName,
            batchName: c.batchName,
            dayOfWeek: c.dayOfWeek,
            periodId: c.periodId,
            startTime: c.startTime,
            endTime: c.endTime,
            slotName: c.slotName,
            termNumber: c.termNumber,
          })));
          this.loading.set(false);
        },
        error: () => { this.toast.error('Failed to load this resource\'s week'); this.loading.set(false); },
      });
  }
}

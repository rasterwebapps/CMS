import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear, TermInstance } from '../../academic-year/academic-year.model';
import { TimetableService } from '../timetable.service';
import { ResourceGridCell, ResourceGridRow, ResourceGridType } from '../timetable.model';
import { WEEK_GRID_DAYS, WEEK_GRID_DAY_LABELS } from '../../../shared/week-grid/week-grid.model';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { TourService } from '../../../shared/tour/tour.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { RESOURCE_TIMETABLE_GRID_TOUR, RESOURCE_TIMETABLE_GRID_FLOW_MAP } from '../../../shared/tour/tours/resource-timetable-grid.tours';
import { CmsInfiniteSelectComponent } from '../../../shared/infinite-select/infinite-select.component';
import { InfiniteSelectValue } from '../../../shared/infinite-select/infinite-select.model';
import { staticOptionsFetchPage } from '../../../shared/infinite-select/infinite-select.utils';
import { colorForSessionType, SessionTypeForColor } from '../../../shared/util/session-color.util';
import { ResourceWeekModalComponent, ResourceWeekModalData } from './resource-week-modal/resource-week-modal.component';
import { PeriodService } from '../../period/period.service';
import { Period } from '../../period/period.model';

interface TimeColumn {
  key: string;
  label: string;
  startTime: string;
  endTime: string;
  periodId: number;
}

/** One row's cells, left to right: a real Period column renders individually (`kind: 'period'`,
 *  unchanged); a run of consecutive Period columns covered by the same off-grid entry (e.g. a
 *  Clinical Shift duty window, periodId null) collapses into one `kind: 'shift'` segment spanning
 *  that many columns — same technique as cms-week-grid's `daySegments`/`WeekGridSegment`, just
 *  applied per resource-row instead of per-day since this grid's columns are periods, not days. */
type RowSegment =
  | { kind: 'period'; key: string; column: TimeColumn }
  | { kind: 'shift'; key: string; span: number; cells: ResourceGridCell[] };

@Component({
  selector: 'app-resource-timetable-grid',
  standalone: true,
  imports: [FormsModule, NgTemplateOutlet, MatDialogModule, MatProgressSpinnerModule, CmsEmptyStateComponent, CmsTourButtonComponent, CmsInfiniteSelectComponent],
  templateUrl: './resource-timetable-grid.component.html',
  styleUrl: './resource-timetable-grid.component.scss',
})
export class ResourceTimetableGridComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly timetableService = inject(TimetableService);
  private readonly periodService = inject(PeriodService);
  private readonly permissionService = inject(PermissionService);
  private readonly toast = inject(ToastService);
  private readonly tourService = inject(TourService);
  private readonly dialog = inject(MatDialog);

  protected readonly canViewFaculty = computed(() => this.permissionService.has('TIMETABLE_FACULTY_GRID_VIEW'));
  protected readonly canViewClassroom = computed(() => this.permissionService.has('TIMETABLE_CLASSROOM_GRID_VIEW'));

  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly termInstances = signal<TermInstance[]>([]);
  protected readonly loading = signal(false);
  protected readonly termsLoading = signal(false);

  protected selectedAcademicYearId: number | null = null;
  protected selectedTermInstanceId: number | null = null;

  protected readonly selectedTerm = computed(() =>
    this.termInstances().find((t) => t.id === this.selectedTermInstanceId) ?? null);

  protected readonly resourceType = signal<ResourceGridType>('FACULTY');
  protected readonly dayOfWeek = signal<string>(WEEK_GRID_DAYS[0]);
  protected readonly days = WEEK_GRID_DAYS;
  protected readonly dayLabels = WEEK_GRID_DAY_LABELS;

  /** DATE (default) resolves through any DayMappingOverride for a real calendar date, e.g. a
   *  compensatory working day correctly shows the borrowed weekday's schedules. WEEKDAY is a
   *  pure planning-mode fallback with no day-mapping awareness (the recurring weekly template,
   *  not tied to any specific date). */
  protected readonly viewMode = signal<'DATE' | 'WEEKDAY'>('DATE');
  protected selectedDate: string = new Date().toISOString().slice(0, 10);

  /** Bound to the date input's min/max — see timetable-view.component.ts's identically-named
   *  getters. Without this, Date mode's `dayOfWeek` is resolved purely from the picked date's own
   *  weekday (ResourceGridService#resolveEffectiveDayOfWeek), with no check that the date actually
   *  falls inside the selected term at all — e.g. picking a term starting 01/10/2026 while today
   *  (24/09) is still selected happily resolved "Thursday" and returned every Thursday's PUBLISHED
   *  recurring session, reading as though the term had already started when it hadn't. */
  protected get dayMin(): string | null {
    return this.selectedTerm()?.startDate ?? null;
  }

  protected get dayMax(): string | null {
    return this.selectedTerm()?.endDate ?? null;
  }

  /** The full active Period master list, independent of the selected term/day — fetched once so
   *  every period shows its own column even when nothing is scheduled that period, the same reason
   *  timetable-view.component.ts's `dayPeriods` exists for Day Agenda. Without this, {@link
   *  timeColumns} derived columns purely from whatever sessions happened to be loaded, so a day
   *  with real PUBLISHED sessions only in its later periods (e.g. Monday's classroom-type sessions
   *  only starting at Period 6) silently dropped every earlier period's column entirely instead of
   *  showing it blank -- reading as though those periods didn't exist rather than just being free. */
  protected readonly periods = signal<Period[]>([]);

  protected readonly rows = signal<ResourceGridRow[]>([]);

  /** Narrows the grid to one faculty/room instead of the full active-resource comparison list --
   *  options come from `rows()` itself (already every active resource of the current type,
   *  regardless of the day, since the backend returns all of them whether or not they have a
   *  session that day) rather than a separate master-list fetch. Reset whenever the Faculty/
   *  Classroom toggle switches, since the previous selection belongs to the other resource type. */
  protected readonly selectedResourceId = signal<number | null>(null);

  protected readonly resourceFilterLabel = computed(() =>
    this.resourceType() === 'FACULTY' ? 'All Faculty' : 'All Classrooms / Labs / Clinical Venues');

  protected readonly resourceFetchPage = staticOptionsFetchPage(() =>
    this.rows().map((r) => ({ id: r.resourceId, name: r.resourceName })));

  protected readonly filteredRows = computed(() => {
    const id = this.selectedResourceId();
    return id == null ? this.rows() : this.rows().filter((r) => r.resourceId === id);
  });

  /** Columns come from the full active Period master list ({@link periods}), not from whatever
   *  sessions happen to be loaded — see {@link periods}' own doc comment for why (a day with real
   *  data only in its later periods was silently losing every earlier period's column instead of
   *  showing it blank). Sorted by periodOrder the same way cms-day-agenda's own periodRows are,
   *  falling back to startTime when periodOrder is unset. An off-grid entry (e.g. a Clinical
   *  Shift's 06:00-14:10 bus-departure/return buffer, no Period of its own) never mints a column
   *  here — see {@link rowSegments}, which renders it as a block spanning whichever real Period
   *  columns its time window overlaps instead. */
  protected readonly timeColumns = computed<TimeColumn[]>(() =>
    this.periods()
      .slice()
      .sort((a, b) => (a.periodOrder ?? 0) - (b.periodOrder ?? 0) || a.startTime.localeCompare(b.startTime))
      .map((p) => ({ key: `period-${p.id}`, label: p.name, startTime: p.startTime, endTime: p.endTime, periodId: p.id })));

  /** Off-grid cells (periodId null) for one row, grouped by their exact time window -- two
   *  entries sharing one window (e.g. a Clinical Shift group running in parallel at two venues)
   *  render as chips inside the same spanning block, the same way a normal Period cell already
   *  groups multiple sessions together. Mirrors cms-week-grid's `shiftGroupsByDay`. */
  private shiftGroupsForRow(row: ResourceGridRow): Map<string, ResourceGridCell[]> {
    const groups = new Map<string, ResourceGridCell[]>();
    for (const s of row.sessions) {
      if (s.periodId != null) continue;
      const key = `${s.startTime}-${s.endTime}`;
      const bucket = groups.get(key);
      if (bucket) bucket.push(s); else groups.set(key, [s]);
    }
    return groups;
  }

  /** Same overlap test cms-week-grid's `shiftWindowFor` uses (window.startTime < column.endTime &&
   *  column.startTime < window.endTime), just against this row's own off-grid groups. */
  private shiftWindowForColumn(groups: Map<string, ResourceGridCell[]>, column: TimeColumn): { key: string; cells: ResourceGridCell[] } | null {
    for (const [key, cells] of groups) {
      const [startTime, endTime] = key.split('-');
      if (startTime < column.endTime && column.startTime < endTime) {
        return { key, cells };
      }
    }
    return null;
  }

  /** Left-to-right rendering plan for one row: a real Period column renders individually, and a
   *  run of consecutive Period columns covered by the same off-grid window (e.g. a Clinical Shift
   *  duty block) collapses into one spanning segment — see {@link RowSegment}. */
  protected rowSegments(row: ResourceGridRow): RowSegment[] {
    const columns = this.timeColumns();
    const groups = this.shiftGroupsForRow(row);
    const segments: RowSegment[] = [];
    let i = 0;
    while (i < columns.length) {
      const window = this.shiftWindowForColumn(groups, columns[i]);
      if (!window) {
        segments.push({ kind: 'period', key: columns[i].key, column: columns[i] });
        i++;
        continue;
      }
      let span = 1;
      while (i + span < columns.length && this.shiftWindowForColumn(groups, columns[i + span])?.key === window.key) {
        span++;
      }
      segments.push({ kind: 'shift', key: `shift-${window.key}-${row.resourceId}`, span, cells: window.cells });
      i += span;
    }
    return segments;
  }

  protected readonly isEmpty = computed(() => this.filteredRows().every((r) => r.sessions.length === 0));

  ngOnInit(): void {
    this.tourService.register('resource-timetable-grid', RESOURCE_TIMETABLE_GRID_TOUR);
    this.tourService.registerFlowMap('resource-timetable-grid', RESOURCE_TIMETABLE_GRID_FLOW_MAP);

    this.resourceType.set(this.canViewFaculty() ? 'FACULTY' : 'CLASSROOM');

    this.periodService.getAll(true).subscribe({
      next: (periods) => this.periods.set(periods),
      error: () => this.toast.error('Failed to load periods'),
    });

    this.academicYearService.getAllAcademicYears().subscribe({
      next: (years) => {
        this.academicYears.set(years);
        const initialYearId = years.find((y) => y.isCurrent)?.id ?? years[0]?.id ?? null;
        if (initialYearId) {
          this.selectedAcademicYearId = initialYearId;
          this.loadTermInstances(initialYearId);
        }
      },
      error: () => { this.toast.error('Failed to load academic years'); },
    });
  }

  protected readonly academicYearFetchPage = staticOptionsFetchPage(() =>
    this.academicYears().map(ay => ({ id: ay.id, name: ay.name })));
  protected readonly termFetchPage = staticOptionsFetchPage(() =>
    this.termInstances().map(t => ({ id: t.id, name: `${t.termType} · ${t.status}` })));
  protected readonly dayFetchPage = staticOptionsFetchPage(() =>
    this.days.map(day => ({ id: day, name: this.dayLabels[day] })));

  protected onAcademicYearChange(value: InfiniteSelectValue | null): void {
    this.selectedAcademicYearId = value != null ? Number(value) : null;
    this.selectedTermInstanceId = null;
    this.rows.set([]);
    if (this.selectedAcademicYearId) this.loadTermInstances(this.selectedAcademicYearId);
  }

  protected onTermChange(value: InfiniteSelectValue | null): void {
    this.selectedTermInstanceId = value != null ? Number(value) : null;
    this.selectedDate = this.clampToTerm(this.selectedDate, this.selectedTerm());
    this.load();
  }

  protected setResourceType(type: ResourceGridType): void {
    this.resourceType.set(type);
    this.selectedResourceId.set(null);
    this.load();
  }

  protected onResourceFilterChange(value: InfiniteSelectValue | null): void {
    this.selectedResourceId.set(value != null ? Number(value) : null);
  }

  protected onDayChange(value: InfiniteSelectValue | null): void {
    if (value == null) return;
    this.dayOfWeek.set(String(value));
    this.load();
  }

  protected onDateChange(date: string): void {
    this.selectedDate = this.clampToTerm(date, this.selectedTerm());
    this.load();
  }

  private clampToTerm(date: string, term: TermInstance | null): string {
    if (!term) return date;
    if (date < term.startDate) return term.startDate;
    if (date > term.endDate) return term.endDate;
    return date;
  }

  protected setViewMode(mode: 'DATE' | 'WEEKDAY'): void {
    this.viewMode.set(mode);
    this.load();
  }

  /** Drills into one row — that resource's own full Mon-Sat week, across every cohort. A second
   *  mode alongside this screen's existing one-day, all-resources comparison view (not a
   *  replacement for it), reached by clicking any row rather than a separate resource picker. */
  protected openWeekView(row: ResourceGridRow): void {
    if (!this.selectedTermInstanceId) return;
    const term = this.selectedTerm();
    const data: ResourceWeekModalData = {
      resourceType: this.resourceType(),
      resourceId: row.resourceId,
      resourceName: row.resourceName,
      termInstanceId: this.selectedTermInstanceId,
      termStartDate: term?.startDate ?? null,
      termEndDate: term?.endDate ?? null,
    };
    this.dialog.open(ResourceWeekModalComponent, { data, width: '1200px', maxWidth: '95vw' });
  }

  /** Matches by periodId, not start/end time equality -- same reasoning as cms-day-agenda's own
   *  byPeriodId lookup: it's the direct FK relationship, not a string comparison that could drift
   *  from how a period's own time got serialized. */
  protected cellsFor(row: ResourceGridRow, column: TimeColumn) {
    return row.sessions.filter((s) => s.periodId === column.periodId);
  }

  /** Same primary-color-tint accent Timetable Builder/Week Grid/Day Agenda use for every session
   *  type — see {@link colorForSessionType}. ResourceGridCell carries no coCurricular flag, so
   *  this always colors by session type alone. */
  protected cellColor(sessionType: SessionTypeForColor): string {
    return colorForSessionType(sessionType);
  }

  private loadTermInstances(academicYearId: number): void {
    this.termsLoading.set(true);
    this.academicYearService.getTermInstancesByAcademicYear(academicYearId).subscribe({
      next: (terms) => {
        this.termInstances.set(terms);
        this.termsLoading.set(false);
        this.selectedTermInstanceId = terms[0]?.id ?? null;
        this.selectedDate = this.clampToTerm(this.selectedDate, this.selectedTerm());
        this.load();
      },
      error: () => { this.toast.error('Failed to load term instances'); this.termsLoading.set(false); },
    });
  }

  private load(): void {
    if (!this.selectedTermInstanceId) { this.rows.set([]); return; }
    this.loading.set(true);
    const opts = this.viewMode() === 'DATE' ? { date: this.selectedDate } : { dayOfWeek: this.dayOfWeek() };
    this.timetableService.getResourceGrid(this.resourceType(), this.selectedTermInstanceId, opts).subscribe({
      next: (data) => { this.rows.set(data); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load resource grid'); this.loading.set(false); },
    });
  }
}

import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear, CohortSummary, TermInstance } from '../../academic-year/academic-year.model';
import { TimetableService } from '../timetable.service';
import { ClassSchedule, ClassScheduleOccurrence } from '../timetable.model';
import { CmsWeekGridComponent } from '../../../shared/week-grid/week-grid.component';
import { WeekGridSession } from '../../../shared/week-grid/week-grid.model';
import { CmsWeekNavigatorComponent } from '../../../shared/week-navigator/week-navigator.component';
import { CmsDayAgendaComponent, DayAgendaPeriod } from '../../../shared/day-agenda/day-agenda.component';
import { PeriodService } from '../../period/period.service';
import { ToastService } from '../../../core/toast/toast.service';
import { PermissionService } from '../../../core/permissions/permission.service';
import { RoomRelocationModalComponent } from '../room-relocation/room-relocation-modal.component';
import { TourService } from '../../../shared/tour/tour.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TIMETABLE_VIEW_TOUR, TIMETABLE_VIEW_FLOW_MAP } from '../../../shared/tour/tours/timetable-view.tours';
import { CmsInfiniteSelectComponent } from '../../../shared/infinite-select/infinite-select.component';
import { InfiniteSelectValue } from '../../../shared/infinite-select/infinite-select.model';
import { staticOptionsFetchPage } from '../../../shared/infinite-select/infinite-select.utils';

export type TimetableViewMode = 'week' | 'dateWise' | 'day';

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

@Component({
  selector: 'app-timetable-view',
  standalone: true,
  imports: [FormsModule, MatProgressSpinnerModule, MatDialogModule, CmsWeekGridComponent, CmsWeekNavigatorComponent, CmsDayAgendaComponent, CmsTourButtonComponent, CmsInfiniteSelectComponent],
  templateUrl: './timetable-view.component.html',
  styleUrl: './timetable-view.component.scss',
})
export class TimetableViewComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly timetableService = inject(TimetableService);
  private readonly periodService = inject(PeriodService);
  private readonly toast = inject(ToastService);
  private readonly route = inject(ActivatedRoute);
  private readonly permissionService = inject(PermissionService);
  private readonly dialog = inject(MatDialog);
  private readonly tourService = inject(TourService);

  protected readonly canRelocateRoom = computed(() => this.permissionService.has('TIMETABLE_ROOM_RELOCATE'));

  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly termInstances = signal<TermInstance[]>([]);
  protected readonly sessions = signal<ClassSchedule[]>([]);
  protected readonly loading = signal(false);
  protected readonly termsLoading = signal(false);

  protected selectedAcademicYearId: number | null = null;
  protected selectedTermInstanceId: number | null = null;

  protected readonly selectedTerm = computed(() =>
    this.termInstances().find((t) => t.id === this.selectedTermInstanceId) ?? null);

  /** The grid has no cohort dimension of its own (`WeekGridSession` carries only a sub-batch
   *  `batchName`, never a cohort id) — without this filter, every published cohort's sessions for
   *  the term merge into the same day/period cells with nothing but that small-print batch name to
   *  tell them apart. Scoped one cohort at a time instead, matching Timetable Builder/Draft Review/
   *  Capacity Planner. Loaded from every cohort in the college (not just this term's), same source
   *  and reasoning as Timetable Builder's own cohort list -- a cohort stays valid across every term
   *  it's enrolled in, so this only needs to load once. */
  protected readonly cohorts = signal<CohortSummary[]>([]);
  protected readonly cohortsLoading = signal(false);
  protected selectedCohortId: number | null = null;

  /** The Day view's period-grid rows -- the full active Period master list, independent of the
   *  selected term (Period has no term/shift scoping in this data model), fetched once so every
   *  period shows a row even when nothing is scheduled that period. */
  protected readonly dayPeriods = signal<DayAgendaPeriod[]>([]);

  protected readonly viewMode = signal<TimetableViewMode>('week');
  protected readonly weekStart = signal(mondayOf(new Date()));
  protected readonly dayDate = signal(new Date().toISOString().slice(0, 10));
  protected readonly occurrences = signal<ClassScheduleOccurrence[]>([]);
  protected readonly occurrencesLoading = signal(false);

  protected readonly selectedFaculty = signal<string | null>(null);
  protected readonly selectedRoom = signal<string | null>(null);
  protected readonly selectedBatch = signal<string | null>(null);

  /** Filter option lists are derived from the term's own loaded sessions rather than fetched
   *  from the Faculty/Classroom/Lab/Batch masters — keeps the dropdowns scoped to only what's
   *  actually scheduled this term instead of every faculty/room in the college.
   *
   *  <p>Pulled from both {@link sessions} (Generic's recurring template) and {@link occurrences}
   *  (Date-wise/Day's real dated occurrences), not just the former: a SUBSTITUTED occurrence's
   *  stand-in faculty/room only ever appears in occurrences, never in the recurring template, so a
   *  dropdown built from sessions alone stayed frozen on Generic's own faculty/room/batch list even
   *  after switching to Date-wise/Day and loading a week with a real substitution -- reading as if
   *  the filter dropdowns weren't refreshing at all when toggling between views. */
  protected readonly facultyOptions = computed(() => {
    const names = new Set(this.sessions().map((s) => s.facultyName));
    for (const o of this.occurrences()) names.add(o.session.facultyName);
    return Array.from(names).sort();
  });

  protected readonly roomOptions = computed(() => {
    const names = new Set(this.sessions().map((s) => s.roomName));
    for (const o of this.occurrences()) names.add(o.session.roomName);
    return Array.from(names).sort();
  });

  protected readonly batchOptions = computed(() => {
    const names = new Set(this.sessions().flatMap((s) => s.batchName ? [s.batchName] : []));
    for (const o of this.occurrences()) if (o.session.batchName) names.add(o.session.batchName);
    return Array.from(names).sort();
  });

  protected readonly filteredSessions = computed(() => {
    const faculty = this.selectedFaculty();
    const room = this.selectedRoom();
    const batch = this.selectedBatch();
    return this.sessions().filter((s) =>
      (!faculty || s.facultyName === faculty) &&
      (!room || s.roomName === room) &&
      (!batch || s.batchName === batch));
  });

  /** Same Faculty/Room/Batch filters applied to the date-exploded occurrences behind
   *  Date-wise/Day view, so switching view modes doesn't silently drop an active filter. */
  protected readonly filteredOccurrences = computed(() => {
    const faculty = this.selectedFaculty();
    const room = this.selectedRoom();
    const batch = this.selectedBatch();
    return this.occurrences().filter((o) =>
      (!faculty || o.session.facultyName === faculty) &&
      (!room || o.session.roomName === room) &&
      (!batch || o.session.batchName === batch));
  });

  /** The Date-wise-weekly view reuses the same day-columns x period-rows grid the Generic Week
   *  view renders (CmsWeekGridComponent), just fed real dated occurrences instead of the recurring
   *  template -- carrying each date's actual HELD/SUBSTITUTED/CANCELLED outcome and real
   *  room/faculty, unlike Week's generic pattern. */
  protected readonly dateWiseSessions = computed<WeekGridSession[]>(() =>
    this.filteredOccurrences().map((o) => ({
      ...o.session,
      occurrenceStatus: o.occurrenceStatus,
      cancelReason: o.cancelReason,
    })));

  ngOnInit(): void {
    this.tourService.register('timetable-view', TIMETABLE_VIEW_TOUR);
    this.tourService.registerFlowMap('timetable-view', TIMETABLE_VIEW_FLOW_MAP);

    this.periodService.getAll(true).subscribe({
      next: (periods) => this.dayPeriods.set(periods
        .map((p) => ({ id: p.id, name: p.name, startTime: p.startTime, endTime: p.endTime, periodOrder: p.periodOrder ?? null }))),
      error: () => { /* Day view just falls back to time-only grouping without period rows. */ },
    });

    const qpAcademicYearId = Number(this.route.snapshot.queryParamMap.get('academicYearId')) || null;
    const qpTermInstanceId = Number(this.route.snapshot.queryParamMap.get('termInstanceId')) || null;
    const qpCohortId = Number(this.route.snapshot.queryParamMap.get('cohortId')) || null;

    this.academicYearService.getAllAcademicYears().subscribe({
      next: (years) => {
        this.academicYears.set(years);
        const initialYearId = qpAcademicYearId
          ?? years.find((y) => y.isCurrent)?.id
          ?? years[0]?.id
          ?? null;
        if (initialYearId) {
          this.selectedAcademicYearId = initialYearId;
          this.loadTermInstances(initialYearId, qpTermInstanceId ?? undefined);
        }
      },
      error: () => { this.toast.error('Failed to load academic years'); },
    });

    this.cohortsLoading.set(true);
    this.academicYearService.getAllCohorts().subscribe({
      next: (cohorts) => {
        this.cohorts.set(cohorts);
        this.cohortsLoading.set(false);
        const initialCohortId = qpCohortId && cohorts.some((c) => c.id === qpCohortId)
          ? qpCohortId
          : cohorts[0]?.id ?? null;
        if (this.selectedCohortId == null) this.selectedCohortId = initialCohortId;
        this.reloadCurrentViewData();
      },
      error: () => { this.toast.error('Failed to load cohorts'); this.cohortsLoading.set(false); },
    });
  }

  protected readonly academicYearFetchPage = staticOptionsFetchPage(() =>
    this.academicYears().map(ay => ({ id: ay.id, name: ay.name })));
  protected readonly termFetchPage = staticOptionsFetchPage(() =>
    this.termInstances().map(t => ({ id: t.id, name: `${t.termType} · ${t.status}` })));
  protected readonly cohortFetchPage = staticOptionsFetchPage(() =>
    this.cohorts().map(c => ({ id: c.id, name: c.displayName })));
  protected readonly facultyFetchPage = staticOptionsFetchPage(() =>
    this.facultyOptions().filter((name): name is string => name != null).map(name => ({ id: name, name })));
  protected readonly roomFetchPage = staticOptionsFetchPage(() =>
    this.roomOptions().filter((name): name is string => name != null).map(name => ({ id: name, name })));
  protected readonly batchFetchPage = staticOptionsFetchPage(() =>
    this.batchOptions().map(name => ({ id: name, name })));

  protected onFacultyFilterChange(value: InfiniteSelectValue | null): void {
    this.selectedFaculty.set(value != null ? String(value) : null);
  }

  protected onRoomFilterChange(value: InfiniteSelectValue | null): void {
    this.selectedRoom.set(value != null ? String(value) : null);
  }

  protected onBatchFilterChange(value: InfiniteSelectValue | null): void {
    this.selectedBatch.set(value != null ? String(value) : null);
  }

  protected onAcademicYearChange(value: InfiniteSelectValue | null): void {
    this.selectedAcademicYearId = value != null ? Number(value) : null;
    this.selectedTermInstanceId = null;
    this.sessions.set([]);
    if (this.selectedAcademicYearId) this.loadTermInstances(this.selectedAcademicYearId);
  }

  protected onCohortChange(value: InfiniteSelectValue | null): void {
    this.selectedCohortId = value != null ? Number(value) : null;
    this.reloadCurrentViewData();
  }

  /** Re-fetches whichever view mode is currently showing for the already-selected term, without
   *  resetting the Date-wise/Day navigator back to today -- used on a Cohort change, where the
   *  term (and so the valid date range) hasn't changed, only which cohort's sessions to show. */
  private reloadCurrentViewData(): void {
    if (!this.selectedTermInstanceId) return;
    this.loadPublished(this.selectedTermInstanceId);
    this.refreshCurrentViewMode();
  }

  protected onTermChange(value: InfiniteSelectValue | null): void {
    this.selectedTermInstanceId = value != null ? Number(value) : null;
    if (this.selectedTermInstanceId) {
      this.loadPublished(this.selectedTermInstanceId);
      this.resetDateWiseAndDayDefaults();
      this.refreshCurrentViewMode();
    } else {
      this.sessions.set([]);
    }
  }

  protected setViewMode(mode: TimetableViewMode): void {
    this.viewMode.set(mode);
    if (mode === 'dateWise') this.loadDateWiseOccurrences(this.weekStart());
    else if (mode === 'day') this.loadDayOccurrences(this.dayDate());
  }

  protected onWeekStartChange(iso: string): void {
    const clamped = this.alignWeekStartToTerm(iso, this.selectedTerm());
    this.weekStart.set(clamped);
    this.loadDateWiseOccurrences(clamped);
  }

  protected onDayDateChange(iso: string): void {
    const clamped = this.clampToTerm(iso, this.selectedTerm());
    this.dayDate.set(clamped);
    this.loadDayOccurrences(clamped);
  }

  protected get dayMin(): string | null {
    return this.selectedTerm()?.startDate ?? null;
  }

  protected get dayMax(): string | null {
    return this.selectedTerm()?.endDate ?? null;
  }

  private refreshCurrentViewMode(): void {
    const mode = this.viewMode();
    if (mode === 'dateWise') this.loadDateWiseOccurrences(this.weekStart());
    else if (mode === 'day') this.loadDayOccurrences(this.dayDate());
  }

  private resetDateWiseAndDayDefaults(): void {
    const term = this.selectedTerm();
    if (!term) return;
    const today = new Date().toISOString().slice(0, 10);
    this.dayDate.set(this.clampToTerm(today, term));
    this.weekStart.set(this.defaultWeekStartInTerm(mondayOf(new Date()), term));
  }

  private clampToTerm(date: string, term: TermInstance | null): string {
    if (!term) return date;
    if (date < term.startDate) return term.startDate;
    if (date > term.endDate) return term.endDate;
    return date;
  }

  /** For a week-navigator-emitted or manually-picked weekStart (interactive navigation) -- never
   *  second-guesses which week the user actually asked to see, unlike {@link
   *  defaultWeekStartInTerm} below. cms-week-navigator's own Prev/Next already refuse to emit a
   *  week with zero real days in the term (its canGoPrevious/canGoNext check the week's LAST day,
   *  not its Monday, against the term bounds), so any value reaching here was already a week the
   *  user could legitimately choose -- including the term's own partial first/last week. Only
   *  clamps a stray out-of-range date (e.g. a raw date-picker value) back onto the nearest
   *  in-range week, re-aligned to that week's own Monday. */
  private alignWeekStartToTerm(date: string, term: TermInstance | null): string {
    const clamped = this.clampToTerm(date, term);
    return clamped === date ? clamped : mondayOf(new Date(`${clamped}T00:00:00`));
  }

  /** Only for the very first landing week, before the user has navigated anywhere -- see {@link
   *  alignWeekStartToTerm} for why interactive Prev/Next/date-picker changes use the plain version
   *  instead. clampToTerm alone can snap a Monday-aligned weekStart to a term.startDate that falls
   *  mid-week (e.g. a term starting on a Thursday), defaulting the view onto that week's own
   *  confusing partial Mon-Wed (blank, since the term hadn't started) while Timetable Builder's
   *  date-agnostic recurring view still shows those days occupied every week -- reading as if the
   *  app had silently lost that cohort's sessions. Skipping forward a further week keeps the
   *  default view fully inside the term; the term's own partial first week stays reachable by
   *  paging back with Previous. There's no equivalent skip for the endDate case: that IS the
   *  term's real last week, partial or not, and there's no later week to skip forward to instead. */
  private defaultWeekStartInTerm(date: string, term: TermInstance | null): string {
    const clamped = this.clampToTerm(date, term);
    if (clamped === date) return clamped;
    const clampedMonday = mondayOf(new Date(`${clamped}T00:00:00`));
    return clamped === term!.startDate && clampedMonday < term!.startDate
      ? addDays(clampedMonday, 7)
      : clampedMonday;
  }

  /** Lazy-loads exactly one Mon-Sat week's real occurrences at a time as the user pages through
   *  cms-week-navigator, rather than fetching the whole term upfront -- a term can run 15-20+
   *  weeks, so this keeps each request small regardless of term length. */
  private loadDateWiseOccurrences(weekStartIso: string): void {
    if (!this.selectedTermInstanceId) return;
    const to = new Date(`${weekStartIso}T00:00:00`);
    to.setDate(to.getDate() + 5);
    const toIso = `${to.getFullYear()}-${String(to.getMonth() + 1).padStart(2, '0')}-${String(to.getDate()).padStart(2, '0')}`;
    this.occurrencesLoading.set(true);
    const requestedCohortId = this.selectedCohortId;
    this.timetableService.getOccurrences(this.selectedTermInstanceId, weekStartIso, toIso, 'browse', requestedCohortId).subscribe({
      // Same stale-response race loadPublished guards against -- discard if a newer cohort
      // selection has already superseded the one this response answers.
      next: (occs) => {
        if (requestedCohortId !== this.selectedCohortId) return;
        this.occurrences.set(occs);
        this.occurrencesLoading.set(false);
      },
      error: () => { this.toast.error('Failed to load date-wise view'); this.occurrencesLoading.set(false); },
    });
  }

  private loadDayOccurrences(iso: string): void {
    if (!this.selectedTermInstanceId) return;
    this.occurrencesLoading.set(true);
    const requestedCohortId = this.selectedCohortId;
    this.timetableService.getOccurrences(this.selectedTermInstanceId, iso, iso, 'browse', requestedCohortId).subscribe({
      next: (occs) => {
        if (requestedCohortId !== this.selectedCohortId) return;
        this.occurrences.set(occs);
        this.occurrencesLoading.set(false);
      },
      error: () => { this.toast.error('Failed to load day view'); this.occurrencesLoading.set(false); },
    });
  }

  /** Day view only -- Week view has no date concept (it renders the recurring weekly pattern),
   *  so a per-date relocation can't be triggered from there. */
  protected openRoomRelocation(occurrence: ClassScheduleOccurrence): void {
    this.dialog.open(RoomRelocationModalComponent, { data: { occurrence }, width: '440px' })
      .afterClosed().subscribe((changed) => {
        if (changed) this.loadDayOccurrences(this.dayDate());
      });
  }

  private loadTermInstances(academicYearId: number, preselectTermInstanceId?: number): void {
    this.termsLoading.set(true);
    this.academicYearService.getTermInstancesByAcademicYear(academicYearId).subscribe({
      next: (terms) => {
        this.termInstances.set(terms);
        this.termsLoading.set(false);
        const preselect = preselectTermInstanceId && terms.some((t) => t.id === preselectTermInstanceId)
          ? preselectTermInstanceId
          : terms[0]?.id ?? null;
        this.selectedTermInstanceId = preselect;
        if (preselect) {
          this.loadPublished(preselect);
          this.resetDateWiseAndDayDefaults();
          this.refreshCurrentViewMode();
        } else {
          this.sessions.set([]);
        }
      },
      error: () => { this.toast.error('Failed to load term instances'); this.termsLoading.set(false); },
    });
  }

  /** ngOnInit's academic-year/term-instance resolution and its separate all-cohorts-in-the-college
   *  resolution (see {@link cohorts}'s own doc comment) are two independent HTTP chains racing each
   *  other -- each can call this with a different {@link selectedCohortId} (the term chain calls it
   *  directly once a term is preselected; the cohort chain calls it via {@link reloadCurrentViewData}
   *  once a cohort is preselected), with no cancellation between them. The all-cohorts request is
   *  the heavier query, so it's entirely possible for the earlier, unfiltered ({@code cohortId} still
   *  null) request's response to arrive AFTER the later, correctly cohort-filtered one and silently
   *  overwrite {@link sessions} with every published cohort's sessions merged together -- including
   *  other cohorts' Clinical Shift entries alongside the selected cohort's own. The `requestedCohortId`
   *  capture-and-compare below discards a response once it's no longer the answer to "what's
   *  currently selected" -- the same guard {@link loadDateWiseOccurrences}/{@link loadDayOccurrences}
   *  use for the identical race. */
  private loadPublished(termInstanceId: number): void {
    this.loading.set(true);
    this.resetFilters();
    const requestedCohortId = this.selectedCohortId;
    this.timetableService.getPublished(termInstanceId, requestedCohortId).subscribe({
      next: (data) => {
        if (requestedCohortId !== this.selectedCohortId) return;
        this.sessions.set(data);
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load timetable'); this.loading.set(false); },
    });
  }

  private resetFilters(): void {
    this.selectedFaculty.set(null);
    this.selectedRoom.set(null);
    this.selectedBatch.set(null);
  }
}

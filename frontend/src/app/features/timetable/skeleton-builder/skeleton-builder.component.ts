import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';
import { from } from 'rxjs';
import { concatMap } from 'rxjs/operators';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear, CohortSummary, TermInstance } from '../../academic-year/academic-year.model';
import { PeriodService } from '../../period/period.service';
import { Period } from '../../period/period.model';
import { SkeletonBuilderService } from './skeleton-builder.service';
import { SkeletonBuilderResponse, SkeletonCell, SkeletonCellPlacementRequest, SkeletonSessionType, SkeletonSlotPreview, SkeletonSubject } from './skeleton-builder.model';
import { WEEK_GRID_DAYS, WEEK_GRID_DAY_LABELS } from '../../../shared/week-grid/week-grid.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';
import { RotationSetupFlyoutComponent } from '../rotation-setup/rotation-setup-flyout.component';
import { ElectiveSlotBlockFlyoutComponent } from './elective-slot-block-flyout.component';
import { GlobalAutoScheduleReportFlyoutComponent } from './global-auto-schedule-report-flyout.component';
import { WorkingSaturdaysFlyoutComponent } from './working-saturdays-flyout.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { colorForSubject } from './subject-color.util';
import { violationText } from '../../../shared/util/violation-text';
import { TourService } from '../../../shared/tour/tour.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { SKELETON_BUILDER_TOUR, SKELETON_BUILDER_FLOW_MAP } from '../../../shared/tour/tours/skeleton-builder.tours';

/** "HH:mm:ss" -> decimal hours between two times on the same day. */
function hoursBetween(startTime: string, endTime: string): number {
  const toMinutes = (t: string) => {
    const [h, m] = t.split(':').map(Number);
    return h * 60 + m;
  };
  return (toMinutes(endTime) - toMinutes(startTime)) / 60;
}

interface HoursBreakdown {
  total: number;
  assigned: number;
  unassigned: number;
}

interface HoursSummary {
  /** Raw weekly-grid capacity for a single section's timetable — every active period's own
   *  duration, summed across Monday-Friday plus this term's real working-Saturday count, over the
   *  whole term. This is the ceiling Theory (one exclusive session per slot) is actually bound by;
   *  Lab/Clinical can exceed it since multiple batches run the same slot in parallel rooms, so
   *  don't read "unassigned > available" as impossible for those two. */
  availableHours: number;
  /** Sum of theory+lab+clinical — "how many hours this term needs/has in total", independent of
   *  session type. */
  overall: HoursBreakdown;
  theory: HoursBreakdown;
  lab: HoursBreakdown;
  clinical: HoursBreakdown;
}

@Component({
  selector: 'app-skeleton-builder',
  standalone: true,
  imports: [FormsModule, DecimalPipe, RouterLink, MatDialogModule, MatProgressSpinnerModule, RotationSetupFlyoutComponent, ElectiveSlotBlockFlyoutComponent, GlobalAutoScheduleReportFlyoutComponent, WorkingSaturdaysFlyoutComponent, CmsEmptyStateComponent, DragDropModule, CmsTourButtonComponent],
  templateUrl: './skeleton-builder.component.html',
  styleUrl: './skeleton-builder.component.scss',
})
export class SkeletonBuilderComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly periodService = inject(PeriodService);
  private readonly skeletonService = inject(SkeletonBuilderService);
  private readonly permissionService = inject(PermissionService);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly tourService = inject(TourService);

  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly termInstances = signal<TermInstance[]>([]);
  protected readonly cohorts = signal<CohortSummary[]>([]);
  protected readonly periods = signal<Period[]>([]);
  protected readonly skeleton = signal<SkeletonBuilderResponse | null>(null);

  /** Populated for the duration of one drag gesture only (set on {@link onDragStarted}, cleared on
   *  {@link onDragEnded}) — keyed by {@link previewKey} so the template can look up a given cell's
   *  live legality in O(1) while rendering the grid. Null whenever nothing is being dragged, or the
   *  preview call hasn't returned yet (no highlight flicker on slow networks; the grid just stays
   *  unhighlighted a moment longer). */
  protected readonly dragPreview = signal<Map<string, SkeletonSlotPreview> | null>(null);

  protected readonly termsLoading = signal(false);
  protected readonly cohortsLoading = signal(false);
  protected readonly skeletonLoading = signal(false);

  protected selectedAcademicYearId: number | null = null;
  protected selectedTermInstanceId: number | null = null;
  protected selectedCohortId: number | null = null;

  /** Bound to the cohort `<select>` directly — mirrors {@link selectedCohortId} except it can also
   *  hold the `'ALL'` sentinel for the "All cohorts" option. Kept separate from {@link
   *  selectedCohortId} deliberately: that field flows unchanged into placeCell/moveCell/the
   *  elective flyout's `[cohortId]` input and must never hold a fake id. */
  protected cohortSelection: number | 'ALL' | null = null;
  protected readonly allCohortsSelected = signal(false);
  protected readonly showGlobalAutoSchedule = signal(false);

  protected readonly days = WEEK_GRID_DAYS;
  protected readonly dayLabels = WEEK_GRID_DAY_LABELS;

  protected readonly showRotationSetup = signal(false);
  protected readonly showElectiveBlock = signal(false);
  protected readonly showWorkingSaturdays = signal(false);
  protected readonly hasElectiveGroup = computed(() =>
    (this.skeleton()?.subjects ?? []).some((s) => s.electiveGroupId != null));
  /** Drives the single-cohort "no schedule yet, run automation?" CTA — the grid itself still
   *  renders unconditionally underneath, this only decides whether the CTA is prominent. */
  protected readonly hasNoCells = computed(() => (this.skeleton()?.cells.length ?? 0) === 0);

  /** 'ALL' (default) shows every section combined, matching pre-existing behavior. A cohort with
   *  more than one committed Theory section otherwise renders every section's cells stacked into
   *  the same slot, which reads as clutter rather than a real scheduling conflict — this lets an
   *  admin isolate one section's actual week at a time. Reset on every cohort/term reload so a
   *  stale section id from a previous cohort can never silently linger. */
  protected readonly selectedSectionId = signal<number | 'ALL'>('ALL');

  protected selectSection(sectionId: number | 'ALL'): void {
    this.selectedSectionId.set(sectionId);
  }

  /** Theory/Lab/Clinical total (curriculum-required) vs. assigned (actually placed) vs.
   *  unassigned hours for the whole term, scoped to whatever the section tabs are currently
   *  showing — a cell/budget row with no section at all (cohortSectionId null) always counts,
   *  matching {@link cellsFor}'s own "applies to every section" rule. {@link
   *  HoursSummary#overall} sums all three types together. Assigned hours use each cell's REAL
   *  occurrence rate, not a flat "sessions placed" count: a Mon-Fri cell recurs every one of the
   *  term's {@link SkeletonBuilderResponse#weeksInTerm} weeks, but a Saturday-placed one only
   *  recurs {@link SkeletonBuilderResponse#workingSaturdayCount} times — 0 whenever this term
   *  hasn't opted into a working-Saturday pattern, since such a cell could only exist as
   *  pre-existing legacy data from before that pattern was configured (today's placement/swap/
   *  move all hard-block it).
   *
   *  <p>Subjects are first grouped into "logical" units by {@link
   *  SkeletonSubject#electiveGroupId}: every member of an elective group is a parallel
   *  alternative offering competing for the SAME shared slot (Elective Slot Block places every
   *  member in lockstep, one period, different rooms/faculty per member) — a student only ever
   *  consumes ONE of them, so the group demands exactly one member's worth of curriculum hours,
   *  not the sum of all N alternatives (matching the Curriculum Map's own per-term hours total,
   *  which counts an elective group once). A non-elective subject is simply its own one-member
   *  group. Within a logical unit, THEORY/LAB/CLINICAL budget rows are then grouped by {@link
   *  SkeletonSubjectBudget#cohortSectionId} — a second committed section is a genuinely separate
   *  live class (own room, own occurrence), so two sections really do need 2x the hours, same for
   *  a second elective-group member's section row. Within one (unit, section) bucket, every row
   *  carries the identical curriculum-hours quota — for LAB/CLINICAL that's parallel
   *  room-capacity-driven batches of ONE requirement, for an elective group it's the parallel
   *  alternative offerings — so only that bucket's first row's totalHours is added; summing all
   *  of them would inflate the total by however many parallel rows exist (a 10-seat lab against a
   *  100-seat section producing 10 batches, or a "choose 1 of 9" elective group, would otherwise
   *  read as 10x/9x the real curriculum demand). Assigned hours are averaged the same way, per
   *  bucket, which preserves the exact assigned/total (% complete) ratio each row would have on
   *  its own while still summing correctly across sections/genuinely-distinct subjects. */
  protected readonly hoursSummary = computed<HoursSummary | null>(() => {
    const sk = this.skeleton();
    if (!sk) return null;
    const sectionFilter = this.selectedSectionId();
    const appliesToFilter = (cohortSectionId: number | null) =>
      sectionFilter === 'ALL' || cohortSectionId == null || cohortSectionId === sectionFilter;

    const total: Record<SkeletonSessionType, number> = { THEORY: 0, LAB: 0, CLINICAL: 0 };
    const assigned: Record<SkeletonSessionType, number> = { THEORY: 0, LAB: 0, CLINICAL: 0 };

    const occurrencesFor = (cell: SkeletonCell) =>
      cell.dayOfWeek === 'SATURDAY' ? sk.workingSaturdayCount : sk.weeksInTerm;

    const subjectGroups = new Map<string, SkeletonSubject[]>();
    for (const subject of sk.subjects) {
      const key = subject.electiveGroupId != null ? `elective:${subject.electiveGroupId}` : `subject:${subject.courseOfferingId}`;
      const bucket = subjectGroups.get(key);
      if (bucket) bucket.push(subject); else subjectGroups.set(key, [subject]);
    }

    for (const group of subjectGroups.values()) {
      const offeringIds = new Set(group.map((s) => s.courseOfferingId));

      for (const type of ['THEORY', 'LAB', 'CLINICAL'] as const) {
        const rows = group.flatMap((s) => s.budgets.filter((b) => b.sessionType === type && appliesToFilter(b.cohortSectionId)));
        if (rows.length === 0) continue;

        const bySection = new Map<number | null, typeof rows>();
        for (const row of rows) {
          const bucket = bySection.get(row.cohortSectionId);
          if (bucket) bucket.push(row); else bySection.set(row.cohortSectionId, [row]);
        }

        for (const [sectionId, sectionRows] of bySection) {
          total[type] += sectionRows[0].totalHours;
          const sumAssigned = sk.cells
            .filter((c) => offeringIds.has(c.courseOfferingId) && c.sessionType === type && c.cohortSectionId === sectionId)
            .reduce((sum, c) => sum + hoursBetween(c.startTime, c.endTime) * occurrencesFor(c), 0);
          assigned[type] += sumAssigned / sectionRows.length;
        }
      }
    }

    const breakdown = (type: SkeletonSessionType): HoursBreakdown => ({
      total: total[type],
      assigned: assigned[type],
      unassigned: Math.max(0, total[type] - assigned[type]),
    });
    const theory = breakdown('THEORY');
    const lab = breakdown('LAB');
    const clinical = breakdown('CLINICAL');
    const overall: HoursBreakdown = {
      total: theory.total + lab.total + clinical.total,
      assigned: theory.assigned + lab.assigned + clinical.assigned,
      unassigned: theory.unassigned + lab.unassigned + clinical.unassigned,
    };

    const dailyGridHours = this.periods().reduce((sum, p) => sum + p.durationMinutes / 60, 0);
    const availableHours = dailyGridHours * (5 * sk.weeksInTerm + sk.workingSaturdayCount);

    return { availableHours, overall, theory, lab, clinical };
  });

  protected canManage(): boolean {
    return this.permissionService.has('TIMETABLE_SKELETON_MANAGE');
  }

  protected canMove(): boolean {
    return this.permissionService.has('TIMETABLE_SKELETON_MOVE');
  }

  protected canPlaceElectiveGroup(): boolean {
    return this.permissionService.has('TIMETABLE_SKELETON_ELECTIVE_PLACE');
  }

  protected canGlobalAutoPlace(): boolean {
    return this.permissionService.has('TIMETABLE_SKELETON_GLOBAL_AUTO_PLACE');
  }

  protected openGlobalAutoSchedule(): void {
    this.showGlobalAutoSchedule.set(true);
  }

  protected onGlobalAutoScheduleClosed(): void {
    this.showGlobalAutoSchedule.set(false);
  }

  /** Only fires on an actual successful run (not just closing the panel) -- and deliberately does
   *  NOT hide the flyout: it fires the instant the run's HTTP call succeeds, the same tick the
   *  flyout flips to its 'success' step and renders the per-cohort unplaced-reasons report (why
   *  any hours are still unassigned, the capacity-gap hour count, "Add Faculty" links). Closing
   *  the panel here used to tear that report down before it could ever paint, so every run looked
   *  like it silently did nothing even when it correctly explained a real, unfillable gap -- the
   *  admin only ever saw the flyout flash and vanish. The panel now only closes via the flyout's
   *  own {@link onGlobalAutoScheduleClosed} (its Close button/backdrop/X), once the admin has
   *  actually read the result. A single-cohort run just reloads the cohort the admin was already
   *  on in the background, so the grid behind the still-open flyout is fresh by the time they
   *  close it. An all-cohorts run falls back to a normal single-cohort selection instead, reusing
   *  the existing per-cohort load path with no new logic needed. */
  protected onGlobalScheduleCompleted(): void {
    if (!this.allCohortsSelected()) {
      this.reloadSkeleton();
      return;
    }
    this.allCohortsSelected.set(false);
    const fallbackCohortId = this.cohorts()[0]?.id ?? null;
    this.cohortSelection = fallbackCohortId;
    this.selectedCohortId = fallbackCohortId;
    this.onCohortChange();
  }

  protected openElectiveBlock(): void {
    this.showElectiveBlock.set(true);
  }

  protected onElectiveBlockClosed(): void {
    this.showElectiveBlock.set(false);
  }

  protected onElectiveBlockSaved(): void {
    this.showElectiveBlock.set(false);
    this.reloadSkeleton();
  }

  protected canManageRotation(): boolean {
    return this.permissionService.has('TIMETABLE_ROTATION_MANAGE');
  }

  protected openRotationSetup(): void {
    this.showRotationSetup.set(true);
  }

  protected onRotationSetupClosed(): void {
    this.showRotationSetup.set(false);
  }

  protected onRotationSaved(): void {
    this.showRotationSetup.set(false);
    this.reloadSkeleton();
  }

  protected canManageWorkingSaturdays(): boolean {
    return this.permissionService.has('TIMETABLE_WORKING_SATURDAYS_MANAGE');
  }

  protected openWorkingSaturdays(): void {
    this.showWorkingSaturdays.set(true);
  }

  protected onWorkingSaturdaysClosed(): void {
    this.showWorkingSaturdays.set(false);
  }

  /** A new/changed pattern changes which Saturdays are usable, which changes real occurrence
   *  counts — reload so the hours summary and grid both reflect it immediately. */
  protected onWorkingSaturdaysSaved(): void {
    this.showWorkingSaturdays.set(false);
    this.reloadSkeleton();
  }

  ngOnInit(): void {
    this.tourService.register('skeleton-builder', SKELETON_BUILDER_TOUR);
    this.tourService.registerFlowMap('skeleton-builder', SKELETON_BUILDER_FLOW_MAP);

    this.periodService.getAll(true).subscribe({
      next: (data) => this.periods.set(data),
      error: () => this.toast.error('Failed to load periods'),
    });
    this.academicYearService.getAllAcademicYears().subscribe({
      next: (years) => {
        this.academicYears.set(years);
        const initialYearId = years.find((y) => y.isCurrent)?.id ?? years[0]?.id ?? null;
        if (initialYearId) {
          this.selectedAcademicYearId = initialYearId;
          this.loadTermInstances(initialYearId);
          this.loadCohorts();
        }
      },
      error: () => this.toast.error('Failed to load academic years'),
    });
  }

  protected onAcademicYearChange(): void {
    this.selectedTermInstanceId = null;
    this.skeleton.set(null);
    if (this.selectedAcademicYearId) this.loadTermInstances(this.selectedAcademicYearId);
  }

  protected onTermChange(): void {
    this.tryLoadSkeleton();
  }

  protected onCohortChange(): void {
    this.tryLoadSkeleton();
  }

  /** Fires on every cohort `<select>` change, including the "All cohorts" option — the only place
   *  {@link cohortSelection} ever gets read, keeping {@link selectedCohortId} a real numeric id (or
   *  null) everywhere else in this component. */
  protected onCohortSelectionChange(): void {
    if (this.cohortSelection === 'ALL') {
      this.allCohortsSelected.set(true);
      this.selectedCohortId = null;
      this.skeleton.set(null);
      return;
    }
    this.allCohortsSelected.set(false);
    this.selectedCohortId = this.cohortSelection;
    this.onCohortChange();
  }

  private loadTermInstances(academicYearId: number): void {
    this.termsLoading.set(true);
    this.academicYearService.getTermInstancesByAcademicYear(academicYearId).subscribe({
      next: (terms) => {
        this.termInstances.set(terms);
        this.termsLoading.set(false);
        this.selectedTermInstanceId = terms[0]?.id ?? null;
        this.tryLoadSkeleton();
      },
      error: () => { this.toast.error('Failed to load term instances'); this.termsLoading.set(false); },
    });
  }

  /** Cohorts aren't scoped to a single academic year (an active cohort keeps appearing across
   *  every term it's still enrolled in), so this only needs to run once, mirroring Capacity
   *  Planner's own cohort list. */
  private loadCohorts(): void {
    this.cohortsLoading.set(true);
    this.academicYearService.getAllCohorts().subscribe({
      next: (cohorts) => {
        this.cohorts.set(cohorts);
        this.cohortsLoading.set(false);
        this.selectedCohortId = cohorts[0]?.id ?? null;
        this.cohortSelection = this.selectedCohortId;
        this.tryLoadSkeleton();
      },
      error: () => { this.toast.error('Failed to load cohorts'); this.cohortsLoading.set(false); },
    });
  }

  /** Term instances and cohorts load independently (in parallel) — this only fires the skeleton
   *  fetch once both a term and a cohort are actually selected, regardless of which one resolves
   *  last. A genuine cohort/term switch resets the section filter — a section id from the
   *  previous cohort has no meaning for this one — but {@link reloadSkeleton} (post-edit refresh
   *  of the *same* cohort) must not, or every drag/remove would silently kick the admin back to
   *  the combined "All Sections" view they'd deliberately narrowed away from. */
  private tryLoadSkeleton(): void {
    if (this.selectedTermInstanceId && this.selectedCohortId) {
      this.selectedSectionId.set('ALL');
      this.loadSkeleton(this.selectedTermInstanceId, this.selectedCohortId);
    } else {
      this.skeleton.set(null);
    }
  }

  private reloadSkeleton(): void {
    if (this.selectedTermInstanceId && this.selectedCohortId) {
      this.loadSkeleton(this.selectedTermInstanceId, this.selectedCohortId);
    }
  }

  private loadSkeleton(termInstanceId: number, cohortId: number): void {
    this.skeletonLoading.set(true);
    this.skeletonService.getCohortSkeleton(termInstanceId, cohortId).subscribe({
      next: (data) => {
        this.skeleton.set(data);
        this.skeletonLoading.set(false);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to load skeleton');
        this.skeleton.set(null);
        this.skeletonLoading.set(false);
      },
    });
  }

  /** A (day, period) slot can hold more than one cell — e.g. two parallel Lab batches in
   *  different rooms, a Rotation Group's linked cells, or (now cohort-wide) another subject
   *  entirely — so this returns every cell sharing that slot across the whole cohort. */
  protected cellsFor(day: string, periodId: number): SkeletonCell[] {
    const sectionFilter = this.selectedSectionId();
    return this.skeleton()?.cells.filter((c) => c.dayOfWeek === day && c.periodId === periodId
      && (sectionFilter === 'ALL' || c.cohortSectionId == null || c.cohortSectionId === sectionFilter)) ?? [];
  }

  protected subjectColor(courseOfferingId: number): string {
    return colorForSubject(courseOfferingId);
  }

  /** Whether {@code cell} has a same-subject/type/occupant cell in the immediately adjacent period
   *  of the same day — used purely to decide the visual "joined" border treatment for a run of
   *  periods extended via {@link onResizeHandleMouseDown}. Every period in the run is still its
   *  own fully independent {@link SkeletonCell} (own id, individually movable/removable/staffable)
   *  — this never implies or requires a shared {@code sessionGroupId}, unlike the separate
   *  periodSpan mechanism. */
  protected hasMatchingAdjacent(cell: SkeletonCell, direction: 1 | -1): boolean {
    const periods = this.periods();
    const idx = periods.findIndex((p) => p.id === cell.periodId);
    const neighbor = periods[idx + direction];
    if (idx < 0 || !neighbor) return false;
    return this.cellsFor(cell.dayOfWeek, neighbor.id).some((c) => this.sameOccupant(c, cell));
  }

  private sameOccupant(a: SkeletonCell, b: SkeletonCell): boolean {
    return a.courseOfferingId === b.courseOfferingId && a.sessionType === b.sessionType
      && a.batchId === b.batchId && a.cohortSectionId === b.cohortSectionId;
  }

  /** Drag-resize handle on an unstaffed cell's trailing edge — extends it into the following
   *  period(s) of the same day by placing ordinary new single-period cells for the same subject/
   *  type/batch/section, one {@link SkeletonBuilderService#placeCell} call per period (never
   *  {@code spanPeriodIds}), so each stays independently editable afterward; {@link
   *  hasMatchingAdjacent} then draws them as one continuous-looking block. Tracks the pointer via
   *  plain DOM events (not CDK drag-drop, which is for moving between drop lists, not resizing)
   *  and resolves the hovered period from {@code elementFromPoint} against the `data-period-id`/
   *  `data-day` attributes stamped on every grid `<td>`. */
  protected onResizeHandleMouseDown(event: MouseEvent, cell: SkeletonCell): void {
    event.preventDefault();
    event.stopPropagation();
    let extraPeriods = 0;

    const onMouseMove = (moveEvent: MouseEvent) => {
      const target = document.elementFromPoint(moveEvent.clientX, moveEvent.clientY);
      const td = target?.closest('td[data-period-id]') as HTMLElement | null;
      if (!td || td.getAttribute('data-day') !== cell.dayOfWeek) return;
      const targetPeriodId = Number(td.getAttribute('data-period-id'));
      const periods = this.periods();
      const startIdx = periods.findIndex((p) => p.id === cell.periodId);
      const targetIdx = periods.findIndex((p) => p.id === targetPeriodId);
      extraPeriods = Math.max(0, targetIdx - startIdx);
    };
    const onMouseUp = () => {
      document.removeEventListener('mousemove', onMouseMove);
      document.removeEventListener('mouseup', onMouseUp);
      if (extraPeriods > 0) {
        this.extendCellForward(cell, extraPeriods);
      }
    };
    document.addEventListener('mousemove', onMouseMove);
    document.addEventListener('mouseup', onMouseUp);
  }

  /** Places one new independent cell per period in {@code cell}'s next {@code extraPeriods}
   *  periods (same day) — stops and reports the first period that's already occupied by something
   *  else, silently skipping one that already matches (e.g. automation already placed this same
   *  subject there). Sequenced with {@code concatMap} rather than fired in parallel so a mid-range
   *  conflict is reported against the exact period it failed at, not a jumble of concurrent
   *  responses; the skeleton is reloaded either way so whatever did succeed before a failure is
   *  never left invisible. */
  private extendCellForward(cell: SkeletonCell, extraPeriods: number): void {
    const cohortId = this.selectedCohortId;
    const periods = this.periods();
    const startIdx = periods.findIndex((p) => p.id === cell.periodId);
    if (!cohortId || startIdx < 0) return;

    const requests: SkeletonCellPlacementRequest[] = [];
    for (const period of periods.slice(startIdx + 1, startIdx + 1 + extraPeriods)) {
      const occupants = this.cellsFor(cell.dayOfWeek, period.id);
      if (occupants.some((c) => this.sameOccupant(c, cell))) continue;
      if (occupants.length > 0) {
        this.toast.error(`Can't extend into ${period.name} — already occupied by another session.`);
        break;
      }
      requests.push({
        courseOfferingId: cell.courseOfferingId,
        sessionType: cell.sessionType,
        dayOfWeek: cell.dayOfWeek,
        periodId: period.id,
        batchId: cell.batchId,
        cohortId,
        cohortSectionId: cell.cohortSectionId,
        spanPeriodIds: null,
      });
    }
    if (requests.length === 0) return;

    from(requests).pipe(concatMap((req) => this.skeletonService.placeCell(req))).subscribe({
      error: (err) => {
        this.toast.error(violationText(err) ?? 'Failed to extend session');
        this.reloadSkeleton();
      },
      complete: () => {
        this.toast.success('Extended');
        this.reloadSkeleton();
      },
    });
  }

  /** Lab/Clinical sessions from two different subjects sharing a slot aren't hard-blocked (batch
   *  rosters aren't tracked cross-subject, so real overlap can't be proven server-side) — flagged
   *  here as a visual advisory instead. A shared Rotation Group means the cells were deliberately
   *  set up to alternate together, so that pairing is excluded. */
  protected hasAdvisory(day: string, periodId: number): boolean {
    const cells = this.cellsFor(day, periodId).filter((c) => c.sessionType !== 'THEORY');
    for (let i = 0; i < cells.length; i++) {
      for (let j = i + 1; j < cells.length; j++) {
        const a = cells[i];
        const b = cells[j];
        if (a.courseOfferingId === b.courseOfferingId) continue;
        const sharedGroup = !!a.rotationGroupLabel && a.rotationGroupLabel === b.rotationGroupLabel;
        if (!sharedGroup) return true;
      }
    }
    return false;
  }

  protected onCellChipClick(cell: SkeletonCell): void {
    if (!this.canManage()) return;
    if (cell.isStaffed) {
      // Read-only once staffed via click (edit through the Class Schedule screen instead) — this
      // used to just silently do nothing, which is indistinguishable from a broken click on a grid
      // where every cell is staffed.
      this.toast.error('This session is already staffed — remove or reassign its faculty on the Class Schedule screen first.');
      return;
    }
    this.confirmRemove(cell);
  }

  private previewKey(day: string, periodId: number): string {
    return `${day}|${periodId}`;
  }

  /** Fired once per drag gesture (CDK's `cdkDragStarted`, bound per-cell in the template) — fetches
   *  every grid slot's live legality for moving THIS cell there and stashes it in {@link
   *  dragPreview} so every `.skeleton-cell-stack` in the grid can highlight itself while the drag is
   *  in progress. A cell with no move permission or mid-periodSpan never starts a drag in the first
   *  place ({@code cdkDragDisabled} on the template's `cdkDrag`), so there's nothing to guard here
   *  beyond the cohort actually being loaded. Silently no-ops on request failure — the grid just
   *  shows no highlight for that drag, falling back to today's drop-and-find-out behavior rather
   *  than blocking the gesture over a preview-only call. */
  protected onDragStarted(cell: SkeletonCell): void {
    const cohortId = this.selectedCohortId;
    if (!cohortId) return;
    this.skeletonService.previewMoveTargets(cell.id, cohortId).subscribe({
      next: (slots) => {
        const map = new Map<string, SkeletonSlotPreview>();
        for (const slot of slots) {
          map.set(this.previewKey(slot.dayOfWeek, slot.periodId), slot);
        }
        this.dragPreview.set(map);
      },
      error: () => this.dragPreview.set(null),
    });
  }

  protected onDragEnded(): void {
    this.dragPreview.set(null);
  }

  /** Template helper — 'valid'/'invalid' drives the drop-target highlight class on a grid slot's
   *  `.skeleton-cell-stack`, or null while nothing is being dragged (or for the dragged cell's own
   *  current slot, which the preview list never includes — see {@code previewMoveTargets}). */
  protected slotPreviewState(day: string, periodId: number): 'valid' | 'invalid' | null {
    const slot = this.dragPreview()?.get(this.previewKey(day, periodId));
    if (!slot) return null;
    return slot.valid ? 'valid' : 'invalid';
  }

  /** Tooltip text for a highlighted-invalid drop target — the backend's own violation message
   *  (already user-facing prose, same text {@link onCellDrop}'s error toast would show on a real
   *  rejected drop), so hovering explains why without having to attempt the drop first. */
  protected slotPreviewReason(day: string, periodId: number): string | null {
    return this.dragPreview()?.get(this.previewKey(day, periodId))?.reason ?? null;
  }

  /** Drops onto the same slot the cell was already in are a no-op — CDK still fires the event
   *  for a same-list drop, so this guards it before ever calling the backend. A target slot with
   *  exactly one existing cell triggers an atomic swap (exchange both cells' day/period) instead
   *  of a plain move — dropping onto an occupied slot used to just fail with a conflict violation,
   *  so this is the only way to actually exchange two sessions rather than remove-then-re-place
   *  twice. A slot with more than one occupant is ambiguous (which one is the swap partner?), so
   *  that's left as an error rather than guessing. Reloads the whole skeleton on success rather
   *  than patching cells locally, matching the reload-after-mutation pattern {@link doRemove}
   *  already uses. */
  protected onCellDrop(event: CdkDragDrop<unknown>, day: string, periodId: number): void {
    const cell = event.item.data as SkeletonCell;
    const cohortId = this.selectedCohortId;
    if (!cell || !cohortId || (cell.dayOfWeek === day && cell.periodId === periodId)) return;

    const occupants = this.cellsFor(day, periodId);
    if (occupants.length > 1) {
      this.toast.error('This slot already has more than one session — remove one first before moving here.');
      return;
    }
    if (occupants.length === 1) {
      this.skeletonService.swapCells(cell.id, { targetCellId: occupants[0].id, cohortId }).subscribe({
        next: () => {
          this.toast.success('Swapped');
          this.reloadSkeleton();
        },
        error: (err) => {
          this.toast.error(violationText(err) ?? 'Failed to swap sessions');
        },
      });
      return;
    }

    this.skeletonService.moveCell(cell.id, { dayOfWeek: day, periodId, cohortId }).subscribe({
      next: () => {
        this.toast.success('Moved');
        this.reloadSkeleton();
      },
      error: (err) => {
        this.toast.error(violationText(err) ?? 'Failed to move session');
      },
    });
  }

  private confirmRemove(cell: SkeletonCell): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Remove Placement',
        message: `Remove the ${cell.sessionType} session placed on ${this.dayLabels[cell.dayOfWeek]}, ${cell.slotName}?`,
        confirmText: 'Remove',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) this.doRemove(cell.id);
    });
  }

  private doRemove(cellId: number): void {
    this.skeletonService.removeCell(cellId).subscribe({
      next: () => { this.toast.success('Removed'); this.reloadSkeleton(); },
      error: (err) => this.toast.error(err?.error?.message ?? 'Failed to remove placement'),
    });
  }
}

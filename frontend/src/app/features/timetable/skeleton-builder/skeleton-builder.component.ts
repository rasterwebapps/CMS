import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatMenuModule } from '@angular/material/menu';
import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';
import { from } from 'rxjs';
import { concatMap } from 'rxjs/operators';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear, CohortSummary, TermInstance } from '../../academic-year/academic-year.model';
import { PeriodService } from '../../period/period.service';
import { Period } from '../../period/period.model';
import { SkeletonBuilderService } from './skeleton-builder.service';
import { ClinicalShiftWindow, DisplacedSubjectShortfall, DutyDayMovePreview, SkeletonBuilderResponse, SkeletonCell, SkeletonCellPlacementRequest, SkeletonRelocationPlan, SkeletonSessionType, SkeletonSubject } from './skeleton-builder.model';
import { SkeletonCellReplaceDialogComponent, SkeletonCellReplaceDialogData, SkeletonCellReplaceDialogResult } from './skeleton-cell-replace-dialog/skeleton-cell-replace-dialog.component';
import { SkeletonCellReassignFacultyDialogComponent, SkeletonCellReassignFacultyDialogData, SkeletonCellReassignFacultyDialogResult } from './skeleton-cell-reassign-faculty-dialog/skeleton-cell-reassign-faculty-dialog.component';
import { SkeletonCellSwapDialogComponent, SkeletonCellSwapDialogData, SkeletonCellSwapDialogResult } from './skeleton-cell-swap-dialog/skeleton-cell-swap-dialog.component';
import { StaffingService } from '../staffing/staffing.service';
import { WEEK_GRID_DAYS, WEEK_GRID_DAY_LABELS } from '../../../shared/week-grid/week-grid.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';
import { GlobalAutoScheduleReportFlyoutComponent } from './global-auto-schedule-report-flyout.component';
import { WorkingSaturdaysFlyoutComponent } from './working-saturdays-flyout.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { colorForSubject, LIBRARY_CELL_COLOR, SPORTS_CELL_COLOR } from './subject-color.util';
import { violationText } from '../../../shared/util/violation-text';
import { TourService } from '../../../shared/tour/tour.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { SKELETON_BUILDER_TOUR, SKELETON_BUILDER_FLOW_MAP } from '../../../shared/tour/tours/skeleton-builder.tours';

/** "HH:mm[:ss]" -> minutes since midnight. */
function minutesOfDay(time: string): number {
  const [h, m] = time.split(':').map(Number);
  return h * 60 + m;
}

/** "HH:mm:ss" -> decimal hours between two times on the same day. */
function hoursBetween(startTime: string, endTime: string): number {
  return (minutesOfDay(endTime) - minutesOfDay(startTime)) / 60;
}

/** Clock minutes covered by any of `intervals` ([start, end) in minutes) — overlaps count once. */
function unionMinutes(intervals: [number, number][]): number {
  const sorted = intervals.filter(([start, end]) => end > start).sort((a, b) => a[0] - b[0]);
  let total = 0;
  let runStart = -1;
  let runEnd = -1;
  for (const [start, end] of sorted) {
    if (start > runEnd) {
      total += runEnd - runStart;
      runStart = start;
      runEnd = end;
    } else if (end > runEnd) {
      runEnd = end;
    }
  }
  return total + (runEnd - runStart);
}

/** What a dragged Clinical duty banner carries, telling a drop apart from a dragged session. */
interface DutyDragData {
  dutyWindow: ClinicalShiftWindow;
}

/** One cell in a Skeleton Builder day-row — see {@link SkeletonBuilderComponent#rowSegments}. */
type SkeletonRowSegment =
  | { kind: 'period'; key: string; period: Period }
  | { kind: 'shift'; key: string; span: number; window: ClinicalShiftWindow };

/** {@code assigned} is curriculum hours placed, capped at each subject's own requirement, so it
 *  never exceeds {@code total}; {@code extra} is whatever a subject got beyond its curriculum (the
 *  extra-hours filler); {@code unassigned} is summed per subject, so one subject's extra hours can
 *  never mask another subject's real gap. */
interface HoursBreakdown {
  total: number;
  assigned: number;
  unassigned: number;
  extra: number;
}

interface HoursSummary {
  /** Every working day's real clock time for one section across the term: each period, plus the
   *  Clinical duty time that falls outside the periods (a 07:00 duty starts before Period 1),
   *  overlaps counted once. Monday-Friday recur every week; Saturday only on the term's working
   *  Saturdays. Every assigned hour — a period cell or duty time — sits inside this, so assigned
   *  can never exceed it. */
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
  imports: [FormsModule, DecimalPipe, RouterLink, MatDialogModule, MatMenuModule, MatProgressSpinnerModule, GlobalAutoScheduleReportFlyoutComponent, WorkingSaturdaysFlyoutComponent, CmsEmptyStateComponent, DragDropModule, CmsTourButtonComponent],
  templateUrl: './skeleton-builder.component.html',
  styleUrl: './skeleton-builder.component.scss',
})
export class SkeletonBuilderComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly periodService = inject(PeriodService);
  private readonly skeletonService = inject(SkeletonBuilderService);
  // Reassign Faculty reuses the Staffing screen's own endpoint rather than adding a parallel one —
  // `staffCell` already handles reassignment (it grandfathers the current holder) and applies a
  // multi-period session's rows atomically.
  private readonly staffingService = inject(StaffingService);
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
  protected readonly dragPreview = signal<Map<string, SkeletonRelocationPlan> | null>(null);
  /** Per-day legality of moving a dragged Clinical duty banner there (see {@link onDutyDragStarted}). */
  protected readonly dutyPreview = signal<Map<string, DutyDayMovePreview> | null>(null);
  /** What's being dragged right now. Highlights show only during a drag, while the preview data
   *  outlives the gesture — CDK fires the drop after `cdkDragEnded`, and the drop needs it. */
  protected readonly dragKind = signal<'cell' | 'duty' | null>(null);
  /** For a multi-period block, which of its periods was grabbed: the window starts that many
   *  periods before the drop, so the grabbed row lands exactly where it's dropped. */
  private dragOffset = 0;

  protected readonly termsLoading = signal(false);
  protected readonly cohortsLoading = signal(false);
  protected readonly skeletonLoading = signal(false);

  protected selectedAcademicYearId: number | null = null;
  protected selectedTermInstanceId: number | null = null;
  protected selectedCohortId: number | null = null;

  /** The currently-selected {@link TermInstance} row, or null before terms have loaded. A plain
   *  method rather than a `computed` because {@link selectedTermInstanceId} is a plain ngModel
   *  field, not a signal — a computed over it would never recompute on change. Used to hand the
   *  Working Saturdays flyout the term's real date range so it can show how many actual Saturdays
   *  a given week-of-month pattern yields. */
  protected selectedTerm(): TermInstance | null {
    return this.termInstances().find((t) => t.id === this.selectedTermInstanceId) ?? null;
  }

  /** Bound to the cohort `<select>` directly — mirrors {@link selectedCohortId} except it can also
   *  hold the `'ALL'` sentinel for the "All cohorts" option. Kept separate from {@link
   *  selectedCohortId} deliberately: that field flows unchanged into placeCell/moveCell and must
   *  never hold a fake id. */
  protected cohortSelection: number | 'ALL' | null = null;
  protected readonly allCohortsSelected = signal(false);
  protected readonly showGlobalAutoSchedule = signal(false);

  protected readonly days = WEEK_GRID_DAYS;
  protected readonly dayLabels = WEEK_GRID_DAY_LABELS;

  protected readonly showWorkingSaturdays = signal(false);
  /** Drives the single-cohort "no schedule yet, run automation?" CTA — the grid itself still
   *  renders unconditionally underneath, this only decides whether the CTA is prominent. */
  protected readonly hasNoCells = computed(() => (this.skeleton()?.cells.length ?? 0) === 0);

  /** True once this term's timetable has been approved/PUBLISHED on Draft Review — a term-wide
   *  fact ({@link SkeletonBuilderResponse#termTimetablePublished}, populated by the backend from
   *  {@code ClassScheduleStatus.PUBLISHED}), not a per-cohort one. Distinct from whether this
   *  cohort's Cohort Room Allocation is committed ({@link SkeletonBuilderResponse#sections}
   *  non-empty) — committing room allocation only unlocks placement, it isn't itself a "stop
   *  touching this" signal. Once the term is published, only manual period/staff edits (swap
   *  staff, swap sessions) are allowed — Run Automation must never run again, at any cost — so both
   *  trigger points below (the header button and the "no schedule yet" empty-state CTA) gate on
   *  this. Always false while "All cohorts…" is selected ({@link skeleton} is null then); that mode
   *  reports the same term-wide fact back per cohort on the backend instead (see {@code
   *  skippedPublishedCohorts} in the run result). */
  protected readonly termTimetablePublished = computed(() => this.skeleton()?.termTimetablePublished ?? false);

  /** This cohort's active Clinical Shift windows, grouped by day — empty for every cohort whose
   *  Program hasn't opted into Clinical Shift scheduling (see {@link SkeletonBuilderResponse#clinicalShiftWindows}). */
  protected readonly shiftWindowsByDay = computed<Map<string, ClinicalShiftWindow[]>>(() => {
    const map = new Map<string, ClinicalShiftWindow[]>();
    for (const w of this.skeleton()?.clinicalShiftWindows ?? []) {
      const bucket = map.get(w.dayOfWeek);
      if (bucket) bucket.push(w); else map.set(w.dayOfWeek, [w]);
    }
    return map;
  });

  /** The Clinical Shift window (if any) overlapping this exact (day, period) cell's clock time.
   *  Drives {@link rowSegments}: a run of consecutive periods blocked by the same shift renders as
   *  one merged, non-placeable cell spanning exactly those columns (never a placement target, no
   *  cdkDropList) instead of each period rendering individually — so the shift visibly occupies the
   *  real grid space it actually blocks (e.g. through Period 5) rather than sitting off to the side
   *  in a separate summary column. The actual placement block is already enforced server-side (see
   *  `checkClinicalShiftBlocked`), so a false negative here (e.g. a not-yet-configured offering)
   *  never lets an illegal placement through, it just doesn't render the merged block until the
   *  offering's shift duration/buffer is filled in. */
  protected shiftWindowFor(day: string, period: Period): ClinicalShiftWindow | null {
    return (this.shiftWindowsByDay().get(day) ?? [])
      .find((w) => w.busDepart != null && w.busReturn != null && period.startTime < w.busReturn && w.busDepart < period.endTime)
      ?? null;
  }

  /** One day's row, left to right: real Periods not blocked by a Clinical Shift render individually
   *  (`kind: 'period'`, unchanged placement behavior); a run of consecutive Periods blocked by the
   *  SAME shift window (by `shiftGroupId`) collapses into one `kind: 'shift'` segment spanning that
   *  many columns, so e.g. a shift blocking Periods 1–5 renders as a single merged cell with
   *  `span: 5` rather than five separately-shaded ones. */
  protected rowSegments(day: string): SkeletonRowSegment[] {
    const periods = this.periods();
    const segments: SkeletonRowSegment[] = [];
    let i = 0;
    while (i < periods.length) {
      const window = this.shiftWindowFor(day, periods[i]);
      if (!window) {
        segments.push({ kind: 'period', key: `period-${periods[i].id}`, period: periods[i] });
        i++;
        continue;
      }
      let span = 1;
      while (i + span < periods.length && this.shiftWindowFor(day, periods[i + span])?.shiftGroupId === window.shiftGroupId) {
        span++;
      }
      segments.push({ kind: 'shift', key: `shift-${window.shiftGroupId}-${day}`, span, window });
      i += span;
    }
    return segments;
  }

  /** Sessions that are still scheduled inside a Clinical Shift duty window.
   *
   *  <p>These would otherwise be completely invisible. {@link rowSegments} collapses every period
   *  a shift covers into ONE banner segment, so cells sitting in those periods are never rendered
   *  — not painted over, but absent from the DOM entirely. They stay active in the database and,
   *  until the matching Conflict Inspector check was added, also passed the scan that gates
   *  Publish, so they could reach a published timetable without anyone seeing them.
   *
   *  <p>Nothing revalidates already-placed rows when a shift group's start time, duration or
   *  travel buffer is edited, which is exactly how a week that was legal when built acquires them.
   *  Surfacing them in the banner is what makes them removable at all from this screen. */
  protected cellsInsideShift(day: string, window: ClinicalShiftWindow): SkeletonCell[] {
    if (!window.busDepart || !window.busReturn) return [];
    return this.visibleCells()
      .filter((c) => c.dayOfWeek === day
        && c.startTime < window.busReturn!
        && window.busDepart! < c.endTime);
  }

  /** Total clinical duty duration (clinicalStart–clinicalEnd only, excluding bus travel) as a
   *  compact label like "8h" or "8h 30m". Null when the offering's shift duration isn't configured
   *  yet (clinicalEnd unset) or resolves to zero/negative. */
  protected shiftDurationLabel(w: ClinicalShiftWindow): string | null {
    if (!w.clinicalEnd) return null;
    const hours = hoursBetween(w.clinicalStart, w.clinicalEnd);
    if (hours <= 0) return null;
    const wholeHours = Math.floor(hours);
    const mins = Math.round((hours - wholeHours) * 60);
    return mins === 0 ? `${wholeHours}h` : `${wholeHours}h ${mins}m`;
  }

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

    // Curriculum-hours-only: Library has no curriculum hours budget (see CurriculumHoursCalculator's
    // backend equivalent), so it deliberately never participates in this Theory/Lab/Clinical
    // required-vs-assigned summary card, unlike SkeletonCell.sessionType which genuinely does span
    // all four types since Library cells appear for real in the grid below.
    const total: Record<'THEORY' | 'LAB' | 'CLINICAL', number> = { THEORY: 0, LAB: 0, CLINICAL: 0 };
    const assigned: Record<'THEORY' | 'LAB' | 'CLINICAL', number> = { THEORY: 0, LAB: 0, CLINICAL: 0 };
    const unassigned: Record<'THEORY' | 'LAB' | 'CLINICAL', number> = { THEORY: 0, LAB: 0, CLINICAL: 0 };
    const extra: Record<'THEORY' | 'LAB' | 'CLINICAL', number> = { THEORY: 0, LAB: 0, CLINICAL: 0 };

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
      const isElectiveGroup = group[0].electiveGroupId != null;

      for (const type of ['THEORY', 'LAB', 'CLINICAL'] as const) {
        const rows = group.flatMap((s) => s.budgets.filter((b) => b.sessionType === type && appliesToFilter(b.cohortSectionId)));
        if (rows.length === 0) continue;

        const bySection = new Map<number | null, typeof rows>();
        for (const row of rows) {
          const bucket = bySection.get(row.cohortSectionId);
          if (bucket) bucket.push(row); else bySection.set(row.cohortSectionId, [row]);
        }

        let subjectTotal = 0;
        let subjectAssigned = 0;
        for (const [sectionId, sectionRows] of bySection) {
          subjectTotal += sectionRows[0].totalHours;
          // A cell with no section (e.g. an elective group's shared slot) is one class the whole
          // cohort attends, so it counts toward every section's bucket -- the same "applies to every
          // section" rule the grid uses. Matching only the exact section id read a placed elective
          // as 0h assigned.
          const sectionCells = sk.cells
            .filter((c) => c.courseOfferingId != null && offeringIds.has(c.courseOfferingId) && c.sessionType === type
              && (c.cohortSectionId === sectionId || c.cohortSectionId == null));
          // An elective group's hours are its distinct slots: student-choice options all run at one
          // shared slot, and a management-selected group runs only its chosen option (OC-227).
          // Summing every option and dividing by the option count under-counted the latter by
          // however many options management didn't pick.
          const countedCells = isElectiveGroup
            ? [...new Map(sectionCells.map((c) => [`${c.dayOfWeek}|${c.periodId}`, c] as const)).values()]
            : sectionCells;
          const cellAssigned = countedCells
            .reduce((sum, c) => sum + hoursBetween(c.startTime, c.endTime) * occurrencesFor(c), 0);
          // Clinical Shift Group hours (OC-177) never produce a grid cell — they're reported
          // separately, already converted to hours, and only ever apply to CLINICAL. Only a
          // shift group scoped to this exact section counts here; a cohort-wide one
          // (cohortSectionId null) is added once below instead of per-section.
          const shiftAssigned = type === 'CLINICAL' && sectionId != null
            ? sk.clinicalShiftHours
                .filter((h) => offeringIds.has(h.courseOfferingId) && h.cohortSectionId === sectionId)
                .reduce((sum, h) => sum + h.assignedHours, 0)
            : 0;
          subjectAssigned += isElectiveGroup ? cellAssigned + shiftAssigned : (cellAssigned + shiftAssigned) / sectionRows.length;
        }

        // Cohort-wide Clinical Shift Group hours (no cohortSectionId) aren't scoped to any one
        // section bucket above — add them once here instead of repeating/dividing them per section.
        if (type === 'CLINICAL') {
          subjectAssigned += sk.clinicalShiftHours
            .filter((h) => offeringIds.has(h.courseOfferingId) && h.cohortSectionId === null)
            .reduce((sum, h) => sum + h.assignedHours, 0);
        }

        // Capped per subject: its extra-hours filler shows as "extra", never as curriculum placed,
        // and never offsets a different subject's shortfall.
        total[type] += subjectTotal;
        assigned[type] += Math.min(subjectAssigned, subjectTotal);
        extra[type] += Math.max(0, subjectAssigned - subjectTotal);
        unassigned[type] += Math.max(0, subjectTotal - subjectAssigned);
      }
    }

    const breakdown = (type: 'THEORY' | 'LAB' | 'CLINICAL'): HoursBreakdown => ({
      total: total[type],
      assigned: assigned[type],
      unassigned: unassigned[type],
      extra: extra[type],
    });
    const theory = breakdown('THEORY');
    const lab = breakdown('LAB');
    const clinical = breakdown('CLINICAL');
    const overall: HoursBreakdown = {
      total: theory.total + lab.total + clinical.total,
      assigned: theory.assigned + lab.assigned + clinical.assigned,
      unassigned: theory.unassigned + lab.unassigned + clinical.unassigned,
      extra: theory.extra + lab.extra + clinical.extra,
    };

    // Every working day's real clock time: each period, plus Clinical duty time outside the periods
    // (a 07:00-13:10 duty starts two hours before Period 1), overlaps counted once. The bus travel
    // buffer isn't teaching time, so it's left out. Saturday counts only on working Saturdays.
    const periodIntervals = this.periods()
      .map((p) => [minutesOfDay(p.startTime), minutesOfDay(p.endTime)] as [number, number]);
    const availableHours = WEEK_GRID_DAYS.reduce((sum, day) => {
      const dutyIntervals = sk.clinicalShiftWindows
        .filter((w) => w.dayOfWeek === day && w.clinicalEnd != null)
        .map((w) => [minutesOfDay(w.clinicalStart), minutesOfDay(w.clinicalEnd!)] as [number, number]);
      const occurrences = day === 'SATURDAY' ? sk.workingSaturdayCount : sk.weeksInTerm;
      return sum + (unionMinutes([...periodIntervals, ...dutyIntervals]) / 60) * occurrences;
    }, 0);

    return { availableHours, overall, theory, lab, clinical };
  });

  protected canManage(): boolean {
    return this.permissionService.has('TIMETABLE_SKELETON_MANAGE');
  }

  protected canMove(): boolean {
    return this.permissionService.has('TIMETABLE_SKELETON_MOVE');
  }

  protected canPin(): boolean {
    return this.permissionService.has('TIMETABLE_SKELETON_PIN');
  }

  protected canMoveDutyDay(): boolean {
    return this.permissionService.has('TIMETABLE_SKELETON_DUTY_DAY_MOVE');
  }


  protected canGlobalAutoPlace(): boolean {
    return this.permissionService.has('TIMETABLE_SKELETON_GLOBAL_AUTO_PLACE');
  }

  protected canViewConflictInspector(): boolean {
    return this.permissionService.has('TIMETABLE_CONFLICT_INSPECTOR_VIEW');
  }

  protected openGlobalAutoSchedule(): void {
    this.showGlobalAutoSchedule.set(true);
  }

  /** Single entry point for the toolbar's one Run button, in both its shapes — "Run Automation"
   *  for a single cohort, "Run Global Auto-Schedule" for "All cohorts" (same handler, same slot,
   *  only the label swaps — see the template). A run's rebuild clears every non-pinned DRAFT cell
   *  in scope before placing anything fresh, so this confirms with the admin FIRST whenever that
   *  scope already has draft content — pinned or not, since either kind means the admin has already
   *  put real work into this grid — rather than opening the existing prerequisite checklist flyout
   *  straight away and letting the overwrite happen as a side effect of ticking through it. A single
   *  cohort already has its grid on screen, so {@link hasNoCells} answers this instantly with no
   *  extra call; "All cohorts" mode never loads a grid, so it asks the term-wide equivalent from the
   *  backend instead. */
  protected confirmAndOpenGlobalAutoSchedule(): void {
    if (this.allCohortsSelected()) {
      const termInstanceId = this.selectedTermInstanceId;
      if (!termInstanceId) return;
      this.skeletonService.hasExistingDraftContent(termInstanceId).subscribe({
        next: (hasExisting) => {
          if (hasExisting) {
            this.confirmOverwrite('This term already has draft sessions placed for one or more cohorts.');
          } else {
            this.openGlobalAutoSchedule();
          }
        },
        error: () => this.toast.error('Failed to check for existing draft sessions'),
      });
      return;
    }
    if (!this.hasNoCells()) {
      const cohortName = this.skeleton()?.cohortName ?? 'This cohort';
      this.confirmOverwrite(`${cohortName} already has draft sessions placed.`);
      return;
    }
    this.openGlobalAutoSchedule();
  }

  private confirmOverwrite(situation: string): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Overwrite Existing Draft?',
        message: `${situation} Running automation now will clear everything that isn't pinned and rebuild from scratch — pinned sessions stay exactly where they are. Continue?`,
        confirmText: 'Run Anyway',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) this.openGlobalAutoSchedule();
    });
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
    // A rebuild re-derives every subject's placed count from scratch, so any shortfall recorded
    // from an earlier replace is now stale — whatever it reported has either been re-placed by the
    // run or is reported afresh in the run's own unplaced list.
    this.displacedShortfalls.set([]);
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

  /** Fires alongside {@link onWorkingSaturdaysSaved} only when the admin picked "Run Automation
   *  now" on the flyout's post-save follow-up — routes through the same overwrite-confirmation gate
   *  as the toolbar button, so they don't have to close this one and go hunt for that button
   *  themselves. */
  protected onWorkingSaturdaysRunAutomation(): void {
    this.confirmAndOpenGlobalAutoSchedule();
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
   *  Planner's own cohort list. Defaults to "All cohorts…" for anyone who can actually run it —
   *  placing every cohort's whole term shortfall at once is the main job this screen exists for,
   *  not an afterthought reached by opening a dropdown. Falls back to the first individual cohort
   *  for anyone without {@code TIMETABLE_SKELETON_GLOBAL_AUTO_PLACE}, since the "All cohorts…"
   *  option itself never appears in their dropdown (see the template's cohort `<select>`). */
  private loadCohorts(): void {
    this.cohortsLoading.set(true);
    this.academicYearService.getAllCohorts().subscribe({
      next: (cohorts) => {
        this.cohorts.set(cohorts);
        this.cohortsLoading.set(false);
        if (this.canGlobalAutoPlace() && cohorts.length > 0) {
          this.allCohortsSelected.set(true);
          this.cohortSelection = 'ALL';
          this.selectedCohortId = null;
        } else {
          this.selectedCohortId = cohorts[0]?.id ?? null;
          this.cohortSelection = this.selectedCohortId;
        }
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
      // Displaced-subject figures are per-cohort and per-term, so they're meaningless once either
      // changes. Cleared here rather than in reloadSkeleton because a replace itself calls
      // reloadSkeleton — clearing there would wipe the banner the replace just raised.
      this.displacedShortfalls.set([]);
      this.loadSkeleton(this.selectedTermInstanceId, this.selectedCohortId);
    } else {
      this.skeleton.set(null);
      this.displacedShortfalls.set([]);
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
    return this.visibleCells().filter((c) => c.dayOfWeek === day && c.periodId === periodId);
  }

  /** Every loaded cell the current section filter admits. Extracted so the section filter has one
   *  definition — {@link cellsFor} and {@link cellsInsideShift} select by different criteria
   *  (period id vs. clock-time overlap) but must agree on which sections are in scope. */
  private visibleCells(): SkeletonCell[] {
    const sectionFilter = this.selectedSectionId();
    return this.skeleton()?.cells.filter((c) =>
      sectionFilter === 'ALL' || c.cohortSectionId == null || c.cohortSectionId === sectionFilter) ?? [];
  }

  /** A cell's accent: its own fixed Sports colour, otherwise its subject's (an elective group shares
   *  one), with Library's fixed slate for the other offering-less cell. */
  protected cellColor(cell: SkeletonCell): string {
    if (cell.sessionType === 'SPORTS') return SPORTS_CELL_COLOR;
    const colorKey = cell.electiveGroupId ?? cell.courseOfferingId;
    return colorKey == null ? LIBRARY_CELL_COLOR : colorForSubject(colorKey);
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
    // LIBRARY has no CourseOffering to extend (its resize handle is hidden in the template for the
    // same reason) -- guards the type narrowing below rather than relying on that alone.
    if (!cohortId || startIdx < 0 || cell.courseOfferingId == null) return;

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

  protected canReplace(): boolean {
    return this.permissionService.has('TIMETABLE_SKELETON_REPLACE');
  }

  /** Whether Replace is offered for this specific cell, mirroring every gate the backend's
   *  {@code replaceCellSubject} enforces so the menu never offers an action that is certain to be
   *  rejected. THEORY/LIBRARY/SPORTS only (a Lab/Clinical cell's audience is a batch tied to one
   *  offering, so changing its subject means changing the batch in Capacity Planner); DRAFT only (a
   *  published session is immutable); never an elective (the group shares one slot — use Place
   *  Elective Block). A Library/Sports source has no `courseOfferingId` to displace — that's fine,
   *  it converts into a staffed Theory session rather than swapping one Theory subject for another
   *  (see the backend method's javadoc for why that isn't hour-neutral the way Theory-to-Theory is). */
  protected canReplaceCell(cell: SkeletonCell): boolean {
    return this.canReplace()
      && (cell.sessionType === 'THEORY' || cell.sessionType === 'LIBRARY' || cell.sessionType === 'SPORTS')
      && cell.status === 'DRAFT'
      && (cell.electiveGroupId == null || cell.commonElective);
  }

  /** Why Replace is unavailable on this cell, for the disabled menu item's explanation. Returns
   *  null when it IS available — an unexplained disabled control is the thing this avoids. */
  protected replaceBlockedReason(cell: SkeletonCell): string | null {
    if (!this.canReplace()) return 'You don\'t have permission to replace a session.';
    if (cell.status !== 'DRAFT') return 'Published sessions can\'t be changed here.';
    if (cell.electiveGroupId != null && !cell.commonElective) return 'Student-choice electives share one slot — Run Automation places the whole group.';
    if (cell.sessionType !== 'THEORY' && cell.sessionType !== 'LIBRARY' && cell.sessionType !== 'SPORTS') {
      return 'Only Theory, Library, or Sports sessions can be replaced — change a Lab/Clinical batch in Capacity Planner.';
    }
    return null;
  }

  /** Subjects pushed below their weekly curriculum requirement by a replace this session.
   *
   *  <p>Held in a persistent banner rather than a toast because the whole point is that the
   *  displaced subject still needs placing somewhere else — a message that disappears after a few
   *  seconds is exactly the wrong surface for a task the user has to act on. Keyed by
   *  offering+section so replacing the same subject twice updates one row instead of stacking
   *  duplicates. Cleared on cohort/term change and on a successful Run Automation, both of which
   *  make the figures stale. */
  protected readonly displacedShortfalls = signal<DisplacedSubjectShortfall[]>([]);

  private shortfallKey(d: DisplacedSubjectShortfall): string {
    return `${d.courseOfferingId}|${d.cohortSectionId ?? 'none'}`;
  }

  protected dismissShortfall(displaced: DisplacedSubjectShortfall): void {
    const key = this.shortfallKey(displaced);
    this.displacedShortfalls.update((list) => list.filter((d) => this.shortfallKey(d) !== key));
  }

  protected dismissAllShortfalls(): void {
    this.displacedShortfalls.set([]);
  }

  /** Open the Replace picker, then apply the choice. The dialog gathers subject + faculty but
   *  never writes, so a constraint violation surfaces through the same `violationText` toast path
   *  every other placement error uses rather than a second error surface inside the dialog. */
  protected openReplaceDialog(cell: SkeletonCell): void {
    const sk = this.skeleton();
    if (!sk || !this.canReplaceCell(cell)) return;

    this.dialog.open(SkeletonCellReplaceDialogComponent, {
      width: '560px',
      maxWidth: '95vw',
      data: {
        cell,
        subjects: sk.subjects,
        cohortId: sk.cohortId,
      } satisfies SkeletonCellReplaceDialogData,
    }).afterClosed().subscribe((result: SkeletonCellReplaceDialogResult | undefined) => {
      if (!result) return;
      this.skeletonService.replaceCell(cell.id, result).subscribe({
        next: (response) => {
          this.toast.success(`Replaced with ${response.cell.subjectCode} — pinned, so Run Automation will keep it.`);
          if (response.displaced) this.recordDisplaced(response.displaced);
          this.reloadSkeleton();
        },
        error: (err) => this.toast.error(violationText(err) ?? 'Failed to replace session'),
      });
    });
  }

  protected canReassignFaculty(): boolean {
    return this.permissionService.has('TIMETABLE_STAFFING_MANAGE');
  }

  /** Whether Reassign Faculty is offered for this cell, mirroring what {@code staffCell} accepts.
   *  DRAFT only (a published session is immutable), and it must have a subject to be taught — a
   *  Library slot has no offering and no faculty, and Sports is staffed by Run Automation from the
   *  Sports subject's PE faculty. A student-choice elective's room is a free pick rather than
   *  resolved from the committed allocation, but the backend now reuses whatever room it's already
   *  staffed with when this dialog sends no classroomId (see {@code requireRequestedClassroom}), so
   *  it's offered here too as long as it's already been staffed once (by Run Automation or the
   *  Approve auto-staff pass) — a never-staffed elective has no room to fall back to yet. */
  protected canReassignFacultyCell(cell: SkeletonCell): boolean {
    return this.canReassignFaculty()
      && cell.status === 'DRAFT'
      && cell.courseOfferingId != null
      && (cell.electiveGroupId == null || cell.commonElective || cell.isStaffed);
  }

  /** Why Reassign Faculty is unavailable, for the disabled menu item. Null when it IS available. */
  protected reassignBlockedReason(cell: SkeletonCell): string | null {
    if (!this.canReassignFaculty()) return 'You don\'t have permission to change staffing.';
    if (cell.status !== 'DRAFT') return 'Published sessions can\'t be changed here.';
    if (cell.sessionType === 'SPORTS') return 'Sports is staffed by Run Automation from the Sports subject\'s PE faculty.';
    if (cell.courseOfferingId == null) return 'A Library slot has no faculty to reassign.';
    if (cell.electiveGroupId != null && !cell.commonElective && !cell.isStaffed) {
      return 'This elective has no room yet — approve the term (or re-run automation) to auto-staff it first.';
    }
    return null;
  }

  /** Change who teaches a session, keeping the subject and slot. Separate from Replace because it
   *  is the far commoner edit: a staffing correction rather than a curriculum one. */
  protected openReassignFacultyDialog(cell: SkeletonCell): void {
    const sk = this.skeleton();
    if (!sk || !this.canReassignFacultyCell(cell)) return;

    this.dialog.open(SkeletonCellReassignFacultyDialogComponent, {
      width: '520px',
      maxWidth: '95vw',
      data: { cell, cohortId: sk.cohortId } satisfies SkeletonCellReassignFacultyDialogData,
    }).afterClosed().subscribe((result: SkeletonCellReassignFacultyDialogResult | undefined) => {
      if (!result) return;
      // classroomId is null throughout: elective Theory is the only session type the backend reads
      // it for, and those never reach this dialog (see canReassignFacultyCell).
      this.staffingService.staffCell(cell.id, { facultyId: result.facultyId, classroomId: null }).subscribe({
        next: () => {
          this.toast.success('Faculty reassigned.');
          this.reloadSkeleton();
        },
        error: (err) => this.toast.error(violationText(err) ?? 'Failed to reassign faculty'),
      });
    });
  }

  /** Whether "Move or swap…" is offered for this cell, mirroring {@code relocate}: DRAFT, and not a
   *  student-choice elective. A multi-period block moves whole, with its parallel batches. Shares
   *  {@code TIMETABLE_SKELETON_MOVE} with dragging because it is literally the same operation
   *  reached a different way, not a distinct capability. */
  protected canSwapCell(cell: SkeletonCell): boolean {
    return this.canMove()
      && cell.status === 'DRAFT'
      && (cell.electiveGroupId == null || cell.commonElective);
  }

  /** Why Swap is unavailable, for the disabled menu item. Null when it IS available. */
  protected swapBlockedReason(cell: SkeletonCell): string | null {
    if (!this.canMove()) return 'You don\'t have permission to move sessions.';
    if (cell.status !== 'DRAFT') return 'Published sessions can\'t be moved here.';
    // Moving one option out of a student-choice group's shared slot splits the group, which the
    // backend refuses outright. An institution-decided elective runs alone, so it swaps freely.
    if (cell.electiveGroupId != null && !cell.commonElective) return 'Student-choice electives share one slot — Run Automation moves the whole group.';
    return null;
  }

  /** Lists every legal place this session can go, whole block included — a move into empty periods
   *  or a swap with the sessions there — and applies the chosen one. Dragging does the same and stays
   *  quicker when both places are visible; this covers far-apart slots and is keyboard-reachable. */
  protected openSwapDialog(cell: SkeletonCell): void {
    const cohortId = this.selectedCohortId;
    if (!cohortId || !this.canSwapCell(cell)) return;
    this.skeletonService.previewRelocation(cell.id, cohortId).subscribe({
      next: (plans) => {
        this.dialog.open(SkeletonCellSwapDialogComponent, {
          width: '560px',
          maxWidth: '95vw',
          data: {
            title: 'Move or swap session',
            subtitle: this.cellSubtitle(cell),
            options: plans,
            chosen: null,
            periodNames: this.periodNames(),
            confirmText: 'Apply',
          } satisfies SkeletonCellSwapDialogData,
        }).afterClosed().subscribe((result: SkeletonCellSwapDialogResult | undefined) => {
          if (result) this.applyRelocation(cell, result.plan, cohortId);
        });
      },
      error: (err) => this.toast.error(violationText(err) ?? 'Failed to load where this session can go'),
    });
  }

  private cellSubtitle(cell: SkeletonCell): string {
    const occupant = cell.cohortSectionLabel ?? cell.batchName;
    return `${cell.subjectCode} · ${cell.sessionType} — ${this.dayLabels[cell.dayOfWeek]}, ${cell.slotName}`
      + (occupant ? ` · ${occupant}` : '');
  }

  /** Period id → the name the grid shows, for the move/swap preview's before → after rows. */
  private periodNames(): Record<number, string> {
    return Object.fromEntries(this.periods().map((p) => [p.id, p.name]));
  }

  private recordDisplaced(displaced: DisplacedSubjectShortfall): void {
    const key = this.shortfallKey(displaced);
    this.displacedShortfalls.update((list) => [
      ...list.filter((d) => this.shortfallKey(d) !== key),
      displaced,
    ]);
  }

  /** Pin/unpin from the cell's own badge. Stops propagation so it never falls through to
   *  {@link onCellChipClick}, which would try to REMOVE the session instead — the pin control sits
   *  inside the cell button, so without this a pin click would delete the very cell being pinned. */
  protected onTogglePin(event: Event, cell: SkeletonCell): void {
    event.stopPropagation();
    if (!this.canPin()) return;
    const next = !cell.pinned;
    this.skeletonService.setCellPinned(cell.id, next).subscribe({
      next: () => {
        this.toast.success(next
          ? 'Pinned — Run Automation will keep this session and schedule around it.'
          : 'Unpinned — Run Automation may now move or replace this session.');
        this.reloadSkeleton();
      },
      error: (err) => this.toast.error(err?.error?.message ?? 'Failed to update pin'),
    });
  }

  private previewKey(day: string, periodId: number): string {
    return `${day}|${periodId}`;
  }

  /** Fired once per drag gesture — fetches the legality of every same-length window for moving THIS
   *  session there with its whole block (MOVE into empty periods, SWAP with the sessions there, or
   *  why not) and stashes it in {@link dragPreview} so the grid highlights itself while the drag is
   *  in progress. Silently no-ops on request failure: the grid shows no highlight, and a drop asks
   *  the user to try again rather than guessing. */
  protected onDragStarted(cell: SkeletonCell): void {
    const cohortId = this.selectedCohortId;
    if (!cohortId) return;
    this.dragKind.set('cell');
    this.dragOffset = this.blockOffset(cell);
    this.dragPreview.set(null);
    this.skeletonService.previewRelocation(cell.id, cohortId).subscribe({
      next: (plans) => this.dragPreview.set(new Map(plans.map((p) => [this.previewKey(p.dayOfWeek, p.startPeriodId), p]))),
      error: () => this.dragPreview.set(null),
    });
  }

  /** The same for a dragged Clinical duty banner: which other days its duty could move to. */
  protected onDutyDragStarted(window: ClinicalShiftWindow): void {
    const cohortId = this.selectedCohortId;
    if (!cohortId) return;
    this.dragKind.set('duty');
    this.dutyPreview.set(null);
    this.skeletonService.previewDutyDayMove(window.shiftGroupId, cohortId).subscribe({
      next: (days) => this.dutyPreview.set(new Map(days.map((d) => [d.dayOfWeek, d]))),
      error: () => this.dutyPreview.set(null),
    });
  }

  protected onDragEnded(): void {
    this.dragKind.set(null);
  }

  /** Position of the grabbed row within its multi-period block (0 for a single-period session). */
  private blockOffset(cell: SkeletonCell): number {
    if (!cell.sessionGroupId) return 0;
    const order = this.periods().map((p) => p.id);
    const first = Math.min(...(this.skeleton()?.cells ?? [])
      .filter((c) => c.sessionGroupId === cell.sessionGroupId)
      .map((c) => order.indexOf(c.periodId)));
    return Math.max(0, order.indexOf(cell.periodId) - first);
  }

  /** The plan for dropping the dragged session with its grabbed row at (day, periodId). */
  private planForDrop(day: string, periodId: number): SkeletonRelocationPlan | null {
    const periods = this.periods();
    const startIndex = periods.findIndex((p) => p.id === periodId) - this.dragOffset;
    if (startIndex < 0) return null;
    return this.dragPreview()?.get(this.previewKey(day, periods[startIndex].id)) ?? null;
  }

  /** Template helper — a grid slot's drop highlight while something is being dragged: 'move' (an
   *  empty window), 'swap' (the sessions there trade places), 'invalid', or null. A dragged duty
   *  banner lights up whole days. */
  protected slotPreviewState(day: string, periodId: number): 'move' | 'swap' | 'invalid' | null {
    const kind = this.dragKind();
    if (kind === 'duty') {
      const preview = this.dutyPreview()?.get(day);
      return preview ? (preview.valid ? 'move' : 'invalid') : null;
    }
    if (kind !== 'cell') return null;
    const plan = this.planForDrop(day, periodId);
    if (!plan) return null;
    return !plan.valid ? 'invalid' : plan.kind === 'SWAP' ? 'swap' : 'move';
  }

  /** Tooltip for a highlighted drop target — the backend's own reason when it's refused (so hovering
   *  explains why without attempting the drop), or what it would swap with. */
  protected slotPreviewReason(day: string, periodId: number): string | null {
    const kind = this.dragKind();
    if (kind === 'duty') return this.dutyPreview()?.get(day)?.reason ?? null;
    if (kind !== 'cell') return null;
    const plan = this.planForDrop(day, periodId);
    if (!plan) return null;
    if (!plan.valid) return plan.reason;
    return plan.kind === 'SWAP' ? 'Swap with ' + plan.moves.slice(1).map((m) => m.subjectCode).join(', ') : 'Move here';
  }

  /** A drop onto the grid. A dragged session goes, whole block included, to the window its grabbed
   *  row lands in — a move into empty periods or a swap with the sessions there — after a preview of
   *  exactly what will move. A dragged Clinical duty banner moves that duty to the dropped-on day.
   *  Reloads the whole skeleton on success, matching {@link doRemove}'s reload-after-mutation. */
  protected onCellDrop(event: CdkDragDrop<unknown>, day: string, periodId: number): void {
    const data = event.item.data as SkeletonCell | DutyDragData | undefined;
    const cohortId = this.selectedCohortId;
    if (!data || !cohortId) return;
    if ('dutyWindow' in data) {
      this.confirmDutyDayMove(data.dutyWindow, day, cohortId);
      return;
    }
    if (data.dayOfWeek === day && data.periodId === periodId) return;
    const plan = this.planForDrop(day, periodId);
    if (!plan) {
      this.toast.info('Still checking where this session can go — try the drop again in a moment.');
      return;
    }
    if (!plan.valid) {
      this.toast.error(plan.reason ?? 'This session can\'t go there.');
      return;
    }
    const swap = plan.kind === 'SWAP';
    this.dialog.open(SkeletonCellSwapDialogComponent, {
      width: '560px',
      maxWidth: '95vw',
      data: {
        title: swap ? 'Swap sessions' : 'Move session',
        subtitle: this.cellSubtitle(data),
        options: [],
        chosen: plan,
        periodNames: this.periodNames(),
        confirmText: swap ? 'Swap' : 'Move',
      } satisfies SkeletonCellSwapDialogData,
    }).afterClosed().subscribe((result: SkeletonCellSwapDialogResult | undefined) => {
      if (result) this.applyRelocation(data, result.plan, cohortId);
    });
  }

  private applyRelocation(cell: SkeletonCell, plan: SkeletonRelocationPlan, cohortId: number): void {
    this.skeletonService.relocate(cell.id, { dayOfWeek: plan.dayOfWeek, startPeriodId: plan.startPeriodId, cohortId }).subscribe({
      next: () => {
        this.toast.success(plan.kind === 'SWAP'
          ? 'Swapped — everything that moved is pinned, so Run Automation will keep it.'
          : 'Moved — pinned, so Run Automation will keep it here.');
        this.reloadSkeleton();
      },
      error: (err) => this.toast.error(violationText(err) ?? 'Failed to move the session'),
    });
  }

  /** Confirms and applies moving a Clinical duty to another day — the new day's sessions inside
   *  the duty window swap into the day it leaves, shown in the preview first. */
  private confirmDutyDayMove(window: ClinicalShiftWindow, day: string, cohortId: number): void {
    if (day === window.dayOfWeek) return;
    const preview = this.dutyPreview()?.get(day);
    if (!preview) {
      this.toast.info('Still checking which days this duty can move to — try the drop again in a moment.');
      return;
    }
    if (!preview.valid) {
      this.toast.error(preview.reason ?? 'The duty can\'t move to that day.');
      return;
    }
    const plan: SkeletonRelocationPlan = {
      dayOfWeek: day, startPeriodId: 0, periodIds: [], kind: preview.moves.length > 0 ? 'SWAP' : 'MOVE',
      valid: true, reason: null, moves: preview.moves,
    };
    this.dialog.open(SkeletonCellSwapDialogComponent, {
      width: '560px',
      maxWidth: '95vw',
      data: {
        title: 'Move clinical duty',
        subtitle: `${window.label}: ${this.dayLabels[window.dayOfWeek]} → ${this.dayLabels[day]}`,
        options: [],
        chosen: plan,
        periodNames: this.periodNames(),
        confirmText: 'Move duty',
      } satisfies SkeletonCellSwapDialogData,
    }).afterClosed().subscribe((result: SkeletonCellSwapDialogResult | undefined) => {
      if (!result) return;
      this.skeletonService.moveDutyDay(window.shiftGroupId, { dayOfWeek: day, cohortId }).subscribe({
        next: () => {
          this.toast.success(preview.moves.length > 0
            ? `Duty moved to ${this.dayLabels[day]} — the sessions it displaced now run on ${this.dayLabels[window.dayOfWeek]} and are pinned.`
            : `Duty moved to ${this.dayLabels[day]}.`);
          this.reloadSkeleton();
        },
        error: (err) => this.toast.error(violationText(err) ?? 'Failed to move the duty'),
      });
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

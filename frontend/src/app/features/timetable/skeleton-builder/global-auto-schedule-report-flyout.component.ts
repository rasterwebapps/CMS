import { Component, DestroyRef, HostListener, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { CmsFlyoutPanelComponent } from '../../../shared/flyout-panel/flyout-panel.component';
import { ToastService } from '../../../core/toast/toast.service';
import { PermissionService } from '../../../core/permissions/permission.service';
import { violationText } from '../../../shared/util/violation-text';
import { SkeletonBuilderService } from './skeleton-builder.service';
import { CohortRoomAllocationService } from '../capacity-planner/cohort-room-allocation.service';
import { CapacityPlannerService } from '../capacity-planner/capacity-planner.service';
import { FacultyWorkloadOverviewReport } from '../capacity-planner/capacity-planner.model';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { CourseOfferingEditDialogComponent } from '../../course-offering/course-offering-edit-dialog/course-offering-edit-dialog.component';
import { FacultyOverCapacity, FacultyTightCapacity, GlobalAutoSchedulePrerequisites, GlobalAutoScheduleResult, VenueCapacityGap, VenueOverCapacity, VenueTightCapacity } from './skeleton-builder.model';
import { WorkingSaturdaysFlyoutComponent } from './working-saturdays-flyout.component';
import { SpecialClassRequestFlyoutComponent } from '../special-classes/special-class-request-flyout/special-class-request-flyout.component';
import { SpecialClassSessionType } from '../special-classes/special-class.model';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { FacultyWorkloadRulesService } from '../faculty-workload-rules/faculty-workload-rules.service';
import { FacultyWorkloadRules } from '../faculty-workload-rules/faculty-workload-rules.model';
import { FacultyService } from '../../faculty/faculty.service';
import { VenueRebalancePanelComponent } from '../capacity-planner/venue-rebalance-panel/venue-rebalance-panel.component';

type Step = 'checking-prerequisites' | 'checklist' | 'running' | 'success' | 'run-failed';

interface CohortNeedingRoom {
  cohortId: number;
  cohortName: string;
}

interface SingleShortfallSubject {
  cohortId: number;
  courseOfferingId: number;
  subjectName: string;
  sessionType: SpecialClassSessionType;
}

/** One milestone on the pre-run checklist, mirroring TermAdvanceChecklistDialogComponent's own
 *  established pattern (tick every item + a final acknowledgment, not the checks themselves
 *  passing, is the actual gate) — with one deliberate divergence: {@code hardBlock} items can
 *  never be ticked through to a live Run, because {@code runGlobalAutoSchedule} itself defensively
 *  re-checks {@code precheckCapacity} server-side and throws regardless of what the admin
 *  acknowledged here. Term-advance has no equivalent server-side re-check, so ticking there is a
 *  real informed choice; here, pretending a hard blocker is bypassable by ticking would just be
 *  lying to the admin about what happens when they press Run. */
interface ChecklistItem {
  key: string;
  label: string;
  warn: boolean;
  checked: boolean;
  hardBlock: boolean;
  /** Whether this item's own detail has ever been opened — the checkbox stays disabled until this
   *  is true, so an item can't be ticked blind; opening it once is enough (collapsing it back
   *  afterward doesn't re-lock the checkbox, since the admin already saw the real numbers). */
  viewed: boolean;
  /** Whether this item's detail panel is currently expanded — independent of {@link viewed}, so
   *  the admin can collapse a reviewed item to declutter without losing their tick eligibility. */
  expanded: boolean;
}

/**
 * Owns the whole global-auto-schedule interaction end to end: a consolidated prerequisite check on
 * open (offerings without faculty, faculty over/near capacity, cohorts without a committed room
 * allocation — everything surfaced together as actionable links, not discovered one gate at a
 * time), a tick-every-milestone checklist (mirroring TermAdvanceChecklistDialogComponent) before
 * Run ever enables, and a best-effort success report (what got placed, what didn't, with a link
 * into Draft Review). Never auto-runs the write call — the admin explicitly confirms every item,
 * so nothing gets placed/staffed without them seeing and acknowledging the real numbers first.
 */
@Component({
  selector: 'app-global-auto-schedule-report-flyout',
  standalone: true,
  imports: [CmsFlyoutPanelComponent, DecimalPipe, RouterLink, FormsModule, MatProgressSpinnerModule, MatDialogModule, MatCheckboxModule, WorkingSaturdaysFlyoutComponent, SpecialClassRequestFlyoutComponent, VenueRebalancePanelComponent],
  templateUrl: './global-auto-schedule-report-flyout.component.html',
  styleUrl: './global-auto-schedule-report-flyout.component.scss',
})
export class GlobalAutoScheduleReportFlyoutComponent implements OnInit {
  private readonly skeletonService = inject(SkeletonBuilderService);
  private readonly cohortRoomAllocationService = inject(CohortRoomAllocationService);
  private readonly capacityPlannerService = inject(CapacityPlannerService);
  private readonly academicYearService = inject(AcademicYearService);
  private readonly facultyWorkloadRulesService = inject(FacultyWorkloadRulesService);
  private readonly facultyService = inject(FacultyService);
  private readonly permissionService = inject(PermissionService);
  private readonly dialog = inject(MatDialog);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly termInstanceId = input.required<number>();
  readonly academicYearId = input.required<number>();
  /** Null = every cohort in the term ("All cohorts…"); a specific id scopes the whole flow
   *  (prerequisites, room-commit check, and the run itself) to just that cohort. */
  readonly cohortId = input<number | null>(null);
  readonly cohortName = input<string | null>(null);

  readonly closed = output<void>();
  /** Emitted only once the write call actually succeeds, so the parent knows to reload. */
  readonly scheduled = output<void>();

  protected readonly step = signal<Step>('checking-prerequisites');
  protected readonly prerequisites = signal<GlobalAutoSchedulePrerequisites | null>(null);
  protected readonly cohortsNeedingRoom = signal<CohortNeedingRoom[]>([]);
  protected readonly result = signal<GlobalAutoScheduleResult | null>(null);
  protected readonly runError = signal<string | null>(null);
  /** Ticks once a second while {@link step} is 'running' — a real, multi-minute "All cohorts" run
   *  with only a static "please wait" message is indistinguishable from a genuinely hung one, which
   *  is exactly what led to an admin closing the browser mid-run (the run itself was still fine —
   *  {@code placeCell}/{@code staffCell} commit independently — but nothing on screen proved it). */
  protected readonly elapsedSeconds = signal(0);
  private elapsedTimer: ReturnType<typeof setInterval> | undefined;

  protected readonly overCapacityFaculty = computed<FacultyOverCapacity[]>(() => this.prerequisites()?.capacityPrecheck.overCapacityFaculty ?? []);
  protected readonly tightCapacityFaculty = computed<FacultyTightCapacity[]>(() => this.prerequisites()?.capacityPrecheck.tightCapacityFaculty ?? []);
  protected readonly headerLabel = computed(() => this.cohortId() != null ? `Global Auto-Schedule — ${this.cohortName() ?? ''}` : 'Global Auto-Schedule — All Cohorts');

  /** Term-wide "does the curriculum's total hour demand even fit in the faculty pool's total
   *  capacity" — distinct from {@link overCapacityFaculty}/{@link tightCapacityFaculty}, which only
   *  ever flag someone already carrying assigned load near/over their own cap. A term can be
   *  nowhere near any individual cap yet still be structurally short overall, if most offerings
   *  simply have no faculty bound yet — that's exactly what {@code totalCurriculumRequiredHours}
   *  (curriculum demand regardless of assignment) vs {@code totalFacultyCapacityHours} (the whole
   *  pool's ceiling) surfaces that the per-faculty checks can't. */
  protected readonly capacityOverview = signal<FacultyWorkloadOverviewReport | null>(null);
  protected readonly capacityGapHours = computed(() => {
    const o = this.capacityOverview();
    return o ? Math.max(0, o.totalCurriculumRequiredHours - o.totalFacultyCapacityHours) : 0;
  });
  protected readonly hasCapacityGap = computed(() => this.capacityGapHours() > 0.001);

  protected readonly canManageWorkloadRules = computed(() => this.permissionService.has('TIMETABLE_WORKLOAD_RULES_MANAGE'));
  protected readonly canManageWorkingSaturdays = computed(() => this.permissionService.has('TIMETABLE_WORKING_SATURDAYS_MANAGE'));
  protected readonly canManageFaculty = computed(() => this.permissionService.has('FACULTY_MANAGE'));
  protected readonly workloadRules = signal<FacultyWorkloadRules | null>(null);
  protected readonly editingWorkloadRules = signal(false);
  protected readonly draftMaxDailyHours = signal<number | null>(null);
  protected readonly draftMaxWeeklyHours = signal<number | null>(null);

  protected readonly checklistItems = signal<ChecklistItem[]>([]);
  protected readonly acknowledged = signal(false);

  /** True the moment any {@code hardBlock} item is still unresolved — Run stays disabled
   *  regardless of ticking, since the server enforces this same gate unconditionally (see {@link
   *  ChecklistItem}). */
  protected readonly hasUnresolvedHardBlock = computed(() => this.checklistItems().some((item) => item.hardBlock && item.warn));

  protected readonly canRun = computed(() =>
    !this.hasUnresolvedHardBlock() && this.acknowledged() && this.checklistItems().every((item) => item.checked));

  /** No-op on an unviewed item — the template already disables that checkbox, but this guards the
   *  same rule at the source in case anything else ever calls this directly. */
  protected toggleChecklistItem(key: string): void {
    this.checklistItems.update((items) => items.map((item) =>
      item.key === key && item.viewed ? { ...item, checked: !item.checked } : item));
  }

  protected toggleAcknowledged(): void {
    this.acknowledged.update((v) => !v);
  }

  /** True the moment ANY session went unplaced — the run's own shortfall alert (Saturday /
   *  Special Class options) shows whenever this is true, regardless of whether it narrows down to
   *  one single subject. */
  protected readonly hasShortfall = computed(() => {
    const r = this.result();
    if (!r) return false;
    return r.electiveUnplaced.length > 0 || r.cohortSummaries.some((c) => c.unplaced.length > 0);
  });

  /** True only when this run genuinely couldn't staff some periods even after trying every
   *  eligible faculty (see `TimetableGlobalAutoScheduleService#fillSelfStudyGaps`) — a real,
   *  this-run capacity ceiling, not a room/scheduling conflict that Saturday or a Special Class
   *  could still resolve. Drives a separate remedy branch in the shortfall panel. */
  protected readonly hasCapacityCausedGap = computed(() => (this.result()?.capacityCausedGapHours ?? 0) > 0.001);

  /** True when this run left LAB/CLINICAL hours unplaced specifically because a venue's own weekly
   *  window capacity was the ceiling — see `VenueCapacityGap` (backend). Purely informational: this
   *  never auto-changes a venue's capacity, since that's a real physical/supervisory fact about a
   *  training site, not a value safe to infer from scheduling pressure. */
  protected readonly hasVenueCapacityGap = computed(() => (this.result()?.venueCapacityGaps?.length ?? 0) > 0);

  /** Pre-run counterparts of {@link hasVenueCapacityGap} — from {@link prerequisites}'s {@code
   *  labClinicalVenueCapacity}, backing the checklist's `venue-over-capacity`/`venue-tight-capacity`
   *  items (see {@link finishPrerequisiteCheck}). Same underlying backend computation as the
   *  post-hoc {@link hasVenueCapacityGap} card, so the two can never disagree. */
  protected readonly overCapacityVenues = computed<VenueOverCapacity[]>(() => this.prerequisites()?.labClinicalVenueCapacity.overCapacityVenues ?? []);
  protected readonly tightCapacityVenues = computed<VenueTightCapacity[]>(() => this.prerequisites()?.labClinicalVenueCapacity.tightCapacityVenues ?? []);

  /** Gates the venue links below on the same permission the target route itself requires
   *  (`LAB_MANAGE`/`CLINICAL_VENUE_MANAGE`) — mirrors `canManageFaculty`/`canManageWorkloadRules`
   *  above, just per-venue-type since a gap can be either kind. Structurally typed (not `VenueCapacityGap`
   *  itself) so the same three helpers also serve the pre-run `VenueOverCapacity`/`VenueTightCapacity`
   *  checklist items below without duplicating them. */
  protected canManageVenue(venue: { venueType: 'LAB' | 'CLINICAL'; venueId: number }): boolean {
    return this.permissionService.has(venue.venueType === 'LAB' ? 'LAB_MANAGE' : 'CLINICAL_VENUE_MANAGE');
  }

  /** Route to raise this venue's own capacity — the admin decides the real number, this only
   *  navigates them to where they'd change it. */
  protected venueEditRoute(venue: { venueType: 'LAB' | 'CLINICAL'; venueId: number }): string {
    return venue.venueType === 'LAB' ? `/labs/${venue.venueId}/edit` : `/clinical-venues/${venue.venueId}/edit`;
  }

  /** Route to designate a second venue for the same subject instead of raising the first one's
   *  capacity — the other remedy `VenueCapacityGap`'s javadoc names. */
  protected venueNewRoute(venue: { venueType: 'LAB' | 'CLINICAL'; venueId: number }): string {
    return venue.venueType === 'LAB' ? '/labs/new' : '/clinical-venues/new';
  }

  /** Query params for {@link venueNewRoute} — carries the exact subjects stuck on the over/tight
   *  venue through to the new venue's create form so saving it there immediately makes it eligible
   *  for those subjects too (see `VenueOverCapacity.affectedSubjectIds`, `SubjectService
   *  .addEligibleVenue`). Degrades to no params (old behavior: a plain, unlinked venue) for a
   *  `VenueCapacityGap` object, which doesn't carry subject ids. */
  protected venueNewQueryParams(venue: { venueId: number; affectedSubjectIds?: number[] }): Record<string, string> {
    const ids = venue.affectedSubjectIds;
    return ids && ids.length > 0 ? { linkSubjectIds: ids.join(',') } : {};
  }

  /** Non-null only when EVERY other subject across the whole run is fully scheduled and exactly
   *  one remains short — the one case specific enough to deep-link a Special Class request
   *  straight to the right subject instead of just pointing at the general shortfall. Electives
   *  are excluded entirely (no single cohort/offering to point at), so any elective shortfall
   *  alongside an otherwise-clean run still counts as "not a single remaining subject". */
  protected readonly singleShortfallSubject = computed<SingleShortfallSubject | null>(() => {
    const r = this.result();
    if (!r || r.electiveUnplaced.length > 0) return null;
    const bySubject = new Map<string, SingleShortfallSubject>();
    for (const summary of r.cohortSummaries) {
      for (const item of summary.unplaced) {
        if (item.courseOfferingId == null) continue;
        bySubject.set(`${item.courseOfferingId}|${item.sessionType}`, {
          cohortId: summary.cohortId,
          courseOfferingId: item.courseOfferingId,
          subjectName: item.subjectName,
          sessionType: item.sessionType as SpecialClassSessionType,
        });
      }
    }
    return bySubject.size === 1 ? [...bySubject.values()][0] : null;
  });

  protected readonly showWorkingSaturdaysFlyout = signal(false);
  protected readonly specialClassPrefill = signal<SingleShortfallSubject | null>(null);

  /** Required signal inputs aren't guaranteed bound until ngOnInit — reading {@link termInstanceId}
   *  any earlier (e.g. the constructor) throws NG0950. */
  ngOnInit(): void {
    this.runCheckPrerequisites();
    this.destroyRef.onDestroy(() => this.stopElapsedTimer());
  }

  /** "Open Capacity Auto-Plan"/"View full workload" still open a *different* screen in a new tab
   *  — clicking one gives no direct signal here whether the change actually landed. Rather than
   *  leave that ambiguous, re-run the prerequisite check automatically the moment this tab regains
   *  focus (the user just came back from doing something elsewhere), so a resolved item visibly
   *  clears and an unresolved one visibly still shows the same numbers — confirmation either way,
   *  not a guess. Only while actually on the incomplete step; no point re-checking mid-run or
   *  after a run already finished. ("Assign faculty" no longer goes through this path at all — see
   *  {@link onAssignFaculty}, which opens the same dialog in place and refreshes immediately on
   *  close, no tab-switch/refocus needed.) */
  /** Only re-checks while a real hard blocker is still outstanding — re-checking unconditionally
   *  on every refocus would also wipe out ticks/acknowledgment the admin already made on an
   *  otherwise-clean checklist just because they briefly tabbed away for something unrelated. */
  @HostListener('window:focus')
  protected onWindowFocus(): void {
    if (this.step() === 'checklist' && this.hasUnresolvedHardBlock()) {
      this.runCheckPrerequisites();
    }
  }

  protected runCheckPrerequisites(): void {
    this.step.set('checking-prerequisites');
    forkJoin({
      prereq: this.skeletonService.checkGlobalAutoPlacePrerequisites(this.termInstanceId(), this.cohortId()),
      overview: this.capacityPlannerService.getFacultyWorkloadOverview(this.termInstanceId()),
      rules: this.facultyWorkloadRulesService.get(),
    }).subscribe({
      next: ({ prereq, overview, rules }) => {
        this.prerequisites.set(prereq);
        this.capacityOverview.set(overview);
        this.workloadRules.set(rules);
        this.checkRoomCommitStatus(prereq);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to check prerequisites');
        this.closed.emit();
      },
    });
  }

  /** Re-fetches only the term-wide capacity overview (not the whole prerequisite set) after a
   *  workload-cap raise or a working-Saturday change — mirrors {@link refreshFacultyPrerequisite}'s
   *  narrow-refresh pattern so the admin's ticks/acknowledgment on every other item survive. Resets
   *  the capacity-gap item's own tick, since a changed number needs a fresh look before it counts as
   *  reviewed again. */
  private refreshCapacityOverview(): void {
    this.capacityPlannerService.getFacultyWorkloadOverview(this.termInstanceId()).subscribe({
      next: (overview) => {
        this.capacityOverview.set(overview);
        this.checklistItems.update((items) => items.map((item) => item.key !== 'capacity-gap' ? item : {
          ...item,
          label: this.capacityGapChecklistLabel(overview),
          warn: overview.totalCurriculumRequiredHours - overview.totalFacultyCapacityHours > 0.001,
          checked: false,
        }));
      },
      error: () => this.toast.error('Failed to refresh capacity numbers'),
    });
  }

  private capacityGapChecklistLabel(overview: FacultyWorkloadOverviewReport): string {
    const gap = overview.totalCurriculumRequiredHours - overview.totalFacultyCapacityHours;
    if (gap <= 0.001) {
      return 'Total faculty capacity covers this term’s full curriculum demand';
    }
    const staffNote = overview.recommendedAdditionalFacultyCount > 0
      ? ` — roughly ${overview.recommendedAdditionalFacultyCount} more faculty at the standard cap`
      : '';
    return `This term needs ${gap.toFixed(0)}h more faculty capacity than currently exists${staffNote}`;
  }

  private checkRoomCommitStatus(prereq: GlobalAutoSchedulePrerequisites): void {
    const termInstanceId = this.termInstanceId();
    const cohortId = this.cohortId();
    if (cohortId != null) {
      this.cohortRoomAllocationService.getCurrent(cohortId, termInstanceId).subscribe({
        next: (allocation) => {
          this.cohortsNeedingRoom.set(allocation == null ? [{ cohortId, cohortName: this.cohortName() ?? '' }] : []);
          this.finishPrerequisiteCheck(prereq);
        },
        error: () => {
          this.toast.error('Failed to check room allocation');
          this.closed.emit();
        },
      });
      return;
    }
    this.capacityPlannerService.getTermOverview(termInstanceId, 'SANCTIONED').subscribe({
      next: (overview) => {
        const needing = overview.cohorts
          .filter((c) => !c.hasCommittedAllocation)
          .map((c) => ({ cohortId: c.cohortId, cohortName: c.cohortLabel }));
        this.cohortsNeedingRoom.set(needing);
        this.finishPrerequisiteCheck(prereq);
      },
      error: () => {
        this.toast.error('Failed to check room allocations');
        this.closed.emit();
      },
    });
  }

  /** Always lands on the checklist step now, clean or not — every milestone must be individually
   *  ticked (plus a final acknowledgment) before Run enables, not just "no problems found" —
   *  giving positive, conscious confirmation of each real number instead of a passive green light.
   *  Resets every tick and the acknowledgment: this only runs on a genuinely fresh check (first
   *  load, or a re-check triggered by an outstanding hard blocker), never as a side effect of
   *  something the admin already ticked on the same data. */
  private finishPrerequisiteCheck(prereq: GlobalAutoSchedulePrerequisites): void {
    const offeringsWithoutFaculty = prereq.offeringsWithoutFaculty;
    const overCapacity = prereq.capacityPrecheck.overCapacityFaculty;
    const tightCapacity = prereq.capacityPrecheck.tightCapacityFaculty;
    const overCapacityVenues = prereq.labClinicalVenueCapacity.overCapacityVenues;
    const tightCapacityVenues = prereq.labClinicalVenueCapacity.tightCapacityVenues;
    const needingRoom = this.cohortsNeedingRoom();

    this.checklistItems.set([
      {
        key: 'faculty',
        label: offeringsWithoutFaculty.length > 0
          ? `${offeringsWithoutFaculty.length} offering(s) have no faculty assigned`
          : 'Every offering has faculty assigned',
        warn: offeringsWithoutFaculty.length > 0,
        checked: false,
        hardBlock: true,
        viewed: false,
        expanded: false,
      },
      {
        key: 'over-capacity',
        label: overCapacity.length > 0
          ? `${overCapacity.length} faculty member(s) are over their term capacity`
          : 'No faculty is over their term capacity',
        warn: overCapacity.length > 0,
        checked: false,
        hardBlock: true,
        viewed: false,
        expanded: false,
      },
      {
        key: 'tight-capacity',
        label: tightCapacity.length > 0
          ? `${tightCapacity.length} faculty member(s) are at ~100% capacity — real placement isn't guaranteed`
          : 'No faculty is at zero-slack capacity',
        warn: tightCapacity.length > 0,
        checked: false,
        hardBlock: false,
        viewed: false,
        expanded: false,
      },
      {
        key: 'venue-over-capacity',
        label: overCapacityVenues.length > 0
          ? `${overCapacityVenues.length} Lab/Clinical venue(s) can't physically fit their weekly demand`
          : 'Every Lab/Clinical venue fits its weekly demand',
        warn: overCapacityVenues.length > 0,
        checked: false,
        hardBlock: true,
        viewed: false,
        expanded: false,
      },
      {
        key: 'venue-tight-capacity',
        label: tightCapacityVenues.length > 0
          ? `${tightCapacityVenues.length} Lab/Clinical venue(s) are at ~100% of their weekly window — real placement isn't guaranteed`
          : 'No Lab/Clinical venue is at zero-slack weekly capacity',
        warn: tightCapacityVenues.length > 0,
        checked: false,
        hardBlock: false,
        viewed: false,
        expanded: false,
      },
      {
        key: 'room',
        label: needingRoom.length > 0
          ? `${needingRoom.length} cohort(s) have no committed room allocation`
          : 'Every cohort has a committed room allocation',
        warn: needingRoom.length > 0,
        checked: false,
        hardBlock: true,
        viewed: false,
        expanded: false,
      },
      {
        key: 'capacity-gap',
        label: this.capacityOverview() != null
          ? this.capacityGapChecklistLabel(this.capacityOverview()!)
          : 'Checking term-wide capacity…',
        // Soft warn, like tight-capacity: a structural shortfall doesn't mean nothing should run —
        // this run will still place and staff whatever the faculty pool can actually cover, exactly
        // like every other partial run. It's a prompt to consider raising hours/Saturdays before
        // committing to "as much as fits today", not a hard stop.
        warn: this.hasCapacityGap(),
        checked: false,
        hardBlock: false,
        viewed: false,
        expanded: false,
      },
    ]);
    this.acknowledged.set(false);
    this.step.set('checklist');
  }

  /** Opens the same Assign Faculty dialog Faculty Detail/Capacity Planner/Assign Faculty List all
   *  already use, in place — no navigation, no new tab, nothing to "come back" from. Was previously
   *  a routerLink deep link to the standalone /assign-faculty screen (target=_blank, same pattern
   *  the room/workload links above still use) — that worked fine for one offering, but a real run
   *  can flag a dozen-plus offerings at once, and clicking through a new tab per offering just to
   *  assign each one doesn't scale. This lets the admin work through every offering on this list
   *  one after another without ever leaving the flyout. */
  protected onAssignFaculty(courseOfferingId: number): void {
    this.academicYearService.getCourseOfferingById(courseOfferingId).subscribe({
      next: (offering) => {
        this.dialog.open(CourseOfferingEditDialogComponent, {
          data: { offering, suggestedFacultyId: null },
          width: '640px',
        }).afterClosed().subscribe(() => this.refreshFacultyPrerequisite());
      },
      error: () => this.toast.error('Failed to load offering details'),
    });
  }

  /** Re-fetches only the faculty-assignment prerequisite after assigning one offering in place, so
   *  the list shrinks live and the panel stays exactly where the admin left it — expanded, mid-way
   *  through the rest. A full {@link runCheckPrerequisites} would also reset every other item's
   *  tick/acknowledgment and collapse this one's detail panel (right for "came back after tabbing
   *  away", see {@link onWindowFocus} — but wrong here, since nothing else on the checklist
   *  changed and the admin is actively working through this exact list). Resets this item's own
   *  `checked` flag regardless of the new count — a prior tick confirmed the OLD numbers, and a
   *  changed count needs a fresh look before it counts as reviewed again. */
  private refreshFacultyPrerequisite(): void {
    this.skeletonService.checkGlobalAutoPlacePrerequisites(this.termInstanceId(), this.cohortId()).subscribe({
      next: (prereq) => {
        this.prerequisites.set(prereq);
        const offeringsWithoutFaculty = prereq.offeringsWithoutFaculty;
        this.checklistItems.update((items) => items.map((item) => item.key !== 'faculty' ? item : {
          ...item,
          label: offeringsWithoutFaculty.length > 0
            ? `${offeringsWithoutFaculty.length} offering(s) have no faculty assigned`
            : 'Every offering has faculty assigned',
          warn: offeringsWithoutFaculty.length > 0,
          checked: false,
        }));
      },
      error: () => this.toast.error('Failed to refresh faculty assignment status'),
    });
  }

  /** The "open it" half of "open it, confirm it, then tick it" — expands (or collapses) that
   *  item's own detail panel and permanently marks it viewed the first time, which is what
   *  actually unlocks its checkbox in the template. Collapsing again afterward doesn't re-lock it:
   *  the admin already saw the real numbers, re-hiding them is just decluttering. */
  protected toggleChecklistDetail(key: string): void {
    this.checklistItems.update((items) => items.map((item) =>
      item.key === key ? { ...item, expanded: !item.expanded, viewed: true } : item));
  }

  /** Guarded against re-entry: the confirm button is disabled until {@link canRun} and disappears
   *  the instant this flips to 'running', but that guard alone isn't enough — a run can take a
   *  while (large "All cohorts" runs can run tens of seconds), and closing the panel mid-run used
   *  to leave the request orphaned (its subscription outlives the destroyed component with no
   *  unsubscribe), so reopening and running again started a second, fully independent run
   *  overlapping the first. {@link takeUntilDestroyed} now cancels the underlying HTTP call the
   *  moment this component is destroyed, and {@link onClose} refuses to close at all while running,
   *  so there's no path left to stack two runs on top of each other. */
  protected runGlobalSchedule(): void {
    if (this.step() === 'running' || !this.canRun()) return;
    this.step.set('running');
    this.runError.set(null);
    this.elapsedSeconds.set(0);
    this.elapsedTimer = setInterval(() => this.elapsedSeconds.update((s) => s + 1), 1000);
    this.skeletonService.globalAutoPlace(this.termInstanceId(), this.cohortId())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (result) => {
          this.stopElapsedTimer();
          this.result.set(result);
          this.step.set('success');
          this.scheduled.emit();
        },
        error: (err) => {
          this.stopElapsedTimer();
          this.runError.set(violationText(err) ?? 'Failed to run the global auto-scheduler');
          this.step.set('run-failed');
        },
      });
  }

  private stopElapsedTimer(): void {
    clearInterval(this.elapsedTimer);
    this.elapsedTimer = undefined;
  }

  protected tierLabel(tier: string): string {
    switch (tier) {
      case 'FACULTY_OVERRIDE': return 'this faculty’s own override';
      case 'DESIGNATION_DEFAULT': return 'their designation’s default';
      default: return 'the institution-wide default';
    }
  }

  protected sessionTypeLabel(sessionType: string | null): string {
    switch (sessionType) {
      case 'THEORY': return 'Theory';
      case 'LAB': return 'Lab';
      case 'CLINICAL': return 'Clinical';
      case 'LAB_CLINICAL': return 'Lab/Clinical';
      case 'LIBRARY': return 'Library';
      default: return '—';
    }
  }

  /** No-op while a run is in flight (backdrop click, the panel's own X button, and the footer
   *  Close button all route here) — closing mid-run is what let a second run stack on top of the
   *  first; the admin has to wait for 'success'/'run-failed' before this can dismiss. */
  protected onClose(): void {
    if (this.step() === 'running') return;
    this.closed.emit();
  }

  protected openWorkingSaturdays(): void {
    this.showWorkingSaturdaysFlyout.set(true);
  }

  protected onWorkingSaturdaysClosed(): void {
    this.showWorkingSaturdaysFlyout.set(false);
  }

  /** Doesn't re-run the automation itself — a new working-Saturday pattern only takes effect on the
   *  *next* automation pass, and re-triggering a fresh multi-cohort run as a side effect of closing
   *  a settings flyout would be surprising. It does refresh the capacity-gap numbers, since more
   *  working days directly raises {@code totalFacultyCapacityHours} — the admin should see the gap
   *  narrow immediately rather than take it on faith until the next run. */
  protected onWorkingSaturdaysSaved(): void {
    this.showWorkingSaturdaysFlyout.set(false);
    this.toast.success('Saved — run automation again to use the updated pattern');
    this.refreshCapacityOverview();
  }

  /** Opens the inline daily/weekly cap editor, pre-filled with the institution-wide values already
   *  fetched alongside the rest of the prerequisite check — no extra round-trip just to open it. */
  /** How many faculty currently riding their own personal daily-cap override (not the
   *  institution-wide default) would be completely unaffected by raising the institution-wide rule
   *  alone — a personal override always wins over the institution default regardless of which is
   *  higher (see {@link tierLabel}). If this is everyone (as it will be for a term where every
   *  faculty already got an individual "Raise Cap"), editing only the institution-wide number here
   *  is a no-op on the actual gap, which is exactly the trap {@link saveWorkloadRules} avoids by
   *  also bulk-raising these. */
  protected readonly overriddenFacultyCount = computed(() =>
    (this.capacityOverview()?.rows ?? []).filter((r) => r.dailyCapacityTier === 'FACULTY_OVERRIDE').length);

  protected readonly bulkRaiseOverrides = signal(true);

  protected openWorkloadRulesEditor(): void {
    const rules = this.workloadRules();
    this.draftMaxDailyHours.set(rules?.maxDailyHours ?? null);
    this.draftMaxWeeklyHours.set(rules?.maxWeeklyHours ?? null);
    this.bulkRaiseOverrides.set(this.overriddenFacultyCount() > 0);
    this.editingWorkloadRules.set(true);
  }

  protected cancelWorkloadRulesEdit(): void {
    this.editingWorkloadRules.set(false);
  }

  /** Updates the institution-wide default (matching what Faculty Workload Rules itself edits) AND,
   *  when {@link bulkRaiseOverrides} is checked, also raises every faculty member's own personal
   *  daily-cap override to at least the new daily value — a personal override always wins over the
   *  institution default regardless of which is higher, so for a term where every faculty already
   *  has one (like this one), skipping this step would leave the actual gap completely unmoved
   *  despite the institution-wide number changing. Only ever raises an existing override, never
   *  lowers one already above the new value, and never touches a faculty member with no override at
   *  all (they already inherit the just-updated institution/designation default). Weekly-hour
   *  personal overrides aren't touched — there's no lightweight single-field endpoint for those the
   *  way {@link FacultyService#updateDailyCap} exists for daily (see Faculty Detail's "Raise Cap"),
   *  only the full Faculty edit form; only the institution-wide weekly rule moves here. */
  protected saveWorkloadRules(): void {
    const current = this.workloadRules();
    const newDaily = this.draftMaxDailyHours();
    this.facultyWorkloadRulesService.update({
      maxDailyHours: newDaily,
      maxWeeklyHours: this.draftMaxWeeklyHours(),
      maxContinuousHours: current?.maxContinuousHours ?? null,
    }).subscribe({
      next: (rules) => {
        this.workloadRules.set(rules);
        const toRaise = this.bulkRaiseOverrides() && newDaily != null
          ? (this.capacityOverview()?.rows ?? []).filter((r) =>
              r.dailyCapacityTier === 'FACULTY_OVERRIDE' && (r.plannedDailyHoursOverride ?? 0) < newDaily)
          : [];
        if (toRaise.length === 0) {
          this.editingWorkloadRules.set(false);
          this.toast.success('Workload rules updated');
          this.refreshCapacityOverview();
          return;
        }
        forkJoin(toRaise.map((r) => this.facultyService.updateDailyCap(r.facultyId, newDaily))).subscribe({
          next: () => {
            this.editingWorkloadRules.set(false);
            this.toast.success(`Workload rules updated — raised ${toRaise.length} faculty member(s)' own override too`);
            this.refreshCapacityOverview();
          },
          error: (err) => this.toast.error(err?.error?.message ?? 'Institution rule saved, but raising individual overrides failed'),
        });
      },
      error: (err) => this.toast.error(err?.error?.message ?? 'Failed to update workload rules'),
    });
  }

  protected openSpecialClass(subject: SingleShortfallSubject): void {
    this.specialClassPrefill.set(subject);
  }

  protected onSpecialClassClosed(): void {
    this.specialClassPrefill.set(null);
  }

  protected onSpecialClassSaved(): void {
    this.specialClassPrefill.set(null);
  }
}

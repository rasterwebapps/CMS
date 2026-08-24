import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe, NgTemplateOutlet } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear, TermInstance } from '../../academic-year/academic-year.model';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';
import { CapacityPlannerService } from '../capacity-planner/capacity-planner.service';
import { CohortAutoPlanSummary, RoomInventoryRow, TermCapacityOverview } from '../capacity-planner/capacity-planner.model';
import { CohortRoomAllocationService } from '../capacity-planner/cohort-room-allocation.service';
import { AllocatedBatch, CohortRoomAllocation, CohortSection, CohortSectionRequest, VentureSplit } from '../capacity-planner/cohort-room-allocation.model';

/** One grouped section of the Room Inventory chip grid -- Classrooms / Labs / Clinical Venues,
 *  in that fixed display order regardless of how the backend orders the flat roomInventory list. */
interface RoomInventoryGroup {
  roomType: RoomInventoryRow['roomType'];
  label: string;
  rooms: RoomInventoryRow[];
}

/** One editable Theory section on a cohort's proposal card -- starts as a copy of the server's
 *  suggested section (see TimetableCapacityPlanningService.suggestSections), room/size then freely
 *  swappable in place before Confirm & Commit. Deliberately NOT the full add/remove-section editor
 *  Capacity Planner has -- that stays behind the "Adjust manually" link for real restructuring. */
interface DraftSectionCard {
  sectionLabel: string;
  classroomId: number | null;
  plannedSize: number | null;
}

/** One editable Lab/Clinical batch row on a cohort's proposal card, scoped to one DraftSectionCard
 *  by sectionLabel -- same swap-in-place scope as DraftSectionCard, no split/merge/add-subject. */
interface DraftBatchCard {
  courseOfferingId: number;
  subjectName: string;
  sessionType: 'LAB' | 'CLINICAL';
  sectionLabel: string;
  batchLabel: string | null;
  venueId: number | null;
  plannedSize: number | null;
  /** The subject's own configured-eligible venue IDs, carried from SuggestedBatch — used to sort
   *  eligible options first in venueOptionsFor without a second lookup. */
  eligibleVenueIds: number[];
}

interface CohortDraft {
  sections: DraftSectionCard[];
  batches: DraftBatchCard[];
}

/** One subject's batch rows within a section, grouped for display (see batchGroupsForSection) --
 *  all batches in a group share one subject/venue, split only by headcount across sequential turns. */
interface BatchGroup {
  courseOfferingId: number;
  subjectName: string;
  sessionType: 'LAB' | 'CLINICAL';
  batches: DraftBatchCard[];
}

/**
 * Term-wide "auto-plan every cohort at once" overview -- lists every cohort enrolled in the
 * selected term with its committed/not-planned status, a strict Theory-room sufficiency check, a
 * whole-term room inventory (all 3 types, with claimed status and suggested-booking counts), and
 * each cohort's suggested sections/batches expandable in place. Never commits or mutates anything
 * itself: it's a scan + navigate screen, mirroring Conflict Inspector's shape. Reviewing, editing,
 * swapping, and committing a cohort's suggestion still happens on the existing Capacity Planner
 * screen, deep-linked to via the row action below.
 */
@Component({
  selector: 'app-capacity-auto-plan',
  standalone: true,
  imports: [
    FormsModule, RouterLink, MatProgressSpinnerModule, MatDialogModule,
    CmsEmptyStateComponent, CmsStatusBadgeComponent, NgTemplateOutlet, DecimalPipe, DatePipe,
  ],
  templateUrl: './capacity-auto-plan.component.html',
  styleUrl: './capacity-auto-plan.component.scss',
})
export class CapacityAutoPlanComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly capacityPlannerService = inject(CapacityPlannerService);
  private readonly cohortRoomAllocationService = inject(CohortRoomAllocationService);
  private readonly permissionService = inject(PermissionService);
  private readonly dialog = inject(MatDialog);
  private readonly toast = inject(ToastService);

  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly termInstances = signal<TermInstance[]>([]);
  protected readonly overview = signal<TermCapacityOverview | null>(null);
  protected readonly loading = signal(false);
  protected readonly termsLoading = signal(false);
  /** One editable draft per not-yet-committed cohort with a real suggestion -- rebuilt from
   *  scratch every time the overview reloads (see runOverview), so a just-committed cohort's card
   *  and every sibling cohort's re-suggested rooms always reflect the latest server state rather
   *  than a stale local edit. */
  protected readonly drafts = signal<Map<number, CohortDraft>>(new Map());
  protected readonly committingCohortId = signal<number | null>(null);
  /** Which cohort's card is showing -- one cohort at a time in its own tab, instead of every
   *  cohort's full editable card rendered in a grid at once (got cluttered fast once each card
   *  grew a real section/batch editor). Preserved across a re-run of Auto-Plan All when the
   *  selected cohort still exists in the refreshed list; otherwise falls back to the first
   *  not-yet-planned cohort (or just the first cohort if everything's committed). */
  protected readonly selectedCohortId = signal<number | null>(null);

  /** The selected cohort's real committed allocation -- fetched lazily only when its tab shows a
   *  COMMITTED cohort (see loadAllocationIfCommitted), since a not-yet-planned cohort has none to
   *  fetch. Read-only in the template; the only way to change it is Revert -> a fresh draft ->
   *  Confirm & Commit again, same two-step pattern Capacity Planner already uses for this exact
   *  object -- there is no in-place update endpoint anywhere in this codebase. */
  protected readonly currentAllocation = signal<CohortRoomAllocation | null>(null);
  protected readonly loadingAllocation = signal(false);
  protected readonly revertingAllocation = signal(false);

  protected selectedAcademicYearId: number | null = null;
  protected selectedTermInstanceId: number | null = null;

  protected readonly notPlannedCount = computed(() => (this.overview()?.cohorts ?? []).filter((r) => !r.hasCommittedAllocation).length);
  protected readonly committedCount = computed(() => (this.overview()?.cohorts ?? []).filter((r) => r.hasCommittedAllocation).length);
  /** Committed (numerator) vs. total-across-the-term (denominator) -- a committed cohort
   *  contributes its real committedSectionsCount/committedBatchesCount to BOTH; a not-yet-planned
   *  cohort contributes 0 to the numerator and its suggestedSections/suggestedLabClinicalBatches
   *  length to the denominator. Reads as "16/16" once everything's committed instead of silently
   *  dropping to a bare "0" (suggestedSections goes empty for a committed cohort -- see
   *  CohortAutoPlanSummary docs), and as e.g. "1/4" partway through committing a term. */
  protected readonly theorySectionsProgress = computed(() => {
    const rows = this.overview()?.cohorts ?? [];
    return {
      planned: rows.reduce((sum, r) => sum + (r.hasCommittedAllocation ? r.committedSectionsCount : 0), 0),
      total: rows.reduce((sum, r) => sum + (r.hasCommittedAllocation ? r.committedSectionsCount : r.suggestedSections.length), 0),
    };
  });
  protected readonly labClinicalBatchesProgress = computed(() => {
    const rows = this.overview()?.cohorts ?? [];
    return {
      planned: rows.reduce((sum, r) => sum + (r.hasCommittedAllocation ? r.committedBatchesCount : 0), 0),
      total: rows.reduce((sum, r) => sum + (r.hasCommittedAllocation ? r.committedBatchesCount : r.suggestedLabClinicalBatches.length), 0),
    };
  });

  protected readonly selectedCohort = computed<CohortAutoPlanSummary | null>(() => {
    const id = this.selectedCohortId();
    return (this.overview()?.cohorts ?? []).find((r) => r.cohortId === id) ?? null;
  });

  protected readonly roomGroups = computed<RoomInventoryGroup[]>(() => {
    const rooms = this.overview()?.roomInventory ?? [];
    return [
      { roomType: 'CLASSROOM', label: 'Classrooms', rooms: rooms.filter((r) => r.roomType === 'CLASSROOM') },
      { roomType: 'LAB', label: 'Labs', rooms: rooms.filter((r) => r.roomType === 'LAB') },
      { roomType: 'CLINICAL', label: 'Clinical Venues', rooms: rooms.filter((r) => r.roomType === 'CLINICAL') },
    ];
  });

  ngOnInit(): void {
    this.academicYearService.getAllAcademicYears().subscribe({
      next: (years) => {
        this.academicYears.set(years);
        const initialYearId = years.find((y) => y.isCurrent)?.id ?? years[0]?.id ?? null;
        if (initialYearId) {
          this.selectedAcademicYearId = initialYearId;
          this.loadTermInstances(initialYearId);
        }
      },
      error: () => this.toast.error('Failed to load academic years'),
    });
  }

  protected onAcademicYearChange(): void {
    this.selectedTermInstanceId = null;
    this.overview.set(null);
    this.drafts.set(new Map());
    this.selectedCohortId.set(null);
    this.currentAllocation.set(null);
    if (this.selectedAcademicYearId) this.loadTermInstances(this.selectedAcademicYearId);
  }

  protected onTermChange(): void {
    if (this.selectedTermInstanceId) this.runOverview();
    else this.overview.set(null);
  }

  protected onAutoPlanAll(): void {
    if (this.selectedTermInstanceId) this.runOverview();
  }

  protected queryParamsFor(row: CohortAutoPlanSummary): Record<string, number> {
    return {
      academicYearId: this.selectedAcademicYearId!,
      termInstanceId: this.selectedTermInstanceId!,
      cohortId: row.cohortId,
    };
  }

  protected selectCohortTab(cohortId: number): void {
    this.selectedCohortId.set(cohortId);
    this.loadAllocationIfCommitted(cohortId);
  }

  protected canManageAllocation(): boolean {
    return this.permissionService.has('TIMETABLE_COHORT_ROOM_ALLOCATION_MANAGE');
  }

  protected canRevertAllocation(): boolean {
    return this.permissionService.has('TIMETABLE_COHORT_ROOM_ALLOCATION_REVERT');
  }

  /** Lab/Clinical batches belonging to one committed section, scoped to ONE session type so the
   *  template can render Lab/Clinical as separate labeled blocks -- same shape as the draft
   *  card's own batchGroupsForSection, just reading the server's real AllocatedBatch rows instead
   *  of an editable draft. cohortSectionLabel is only populated when the commit had 2+ sections
   *  (see VentureSplit docs) -- with exactly one section every batch belongs to it regardless. */
  protected allocBatchesFor(alloc: CohortRoomAllocation, section: CohortSection, sessionType: 'LAB' | 'CLINICAL'): AllocatedBatch[] {
    const scoped = alloc.sections.length <= 1 ? alloc.batches : alloc.batches.filter((b) => b.cohortSectionLabel === section.sectionLabel);
    return scoped.filter((b) => b.sessionType === sessionType);
  }

  /** Fetches the real committed allocation only for a cohort whose tab is actually showing a
   *  COMMITTED status -- a not-yet-planned cohort has nothing to fetch, so this clears the signal
   *  instead. Called on tab switch and after every overview refresh (a commit/revert elsewhere can
   *  flip the selected cohort's own committed status without the tab selection itself changing).
   *  Gated the same way Capacity Planner gates its own identical fetch (MANAGE or REVERT) -- a
   *  user with neither just sees the plain "already committed" hint, same as before this existed,
   *  instead of a failed-fetch toast from the backend's own VIEW-permission check. */
  private loadAllocationIfCommitted(cohortId: number | null): void {
    this.currentAllocation.set(null);
    if (cohortId == null || !this.selectedTermInstanceId) return;
    if (!this.canManageAllocation() && !this.canRevertAllocation()) return;
    const row = this.overview()?.cohorts.find((r) => r.cohortId === cohortId);
    if (!row?.hasCommittedAllocation) return;
    this.loadingAllocation.set(true);
    this.cohortRoomAllocationService.getCurrent(cohortId, this.selectedTermInstanceId).subscribe({
      next: (alloc) => {
        this.currentAllocation.set(alloc);
        this.loadingAllocation.set(false);
      },
      error: () => {
        this.toast.error('Failed to load the committed room allocation');
        this.loadingAllocation.set(false);
      },
    });
  }

  /** Edit is nothing but Revert -- there is no in-place update endpoint anywhere in this codebase
   *  (see Capacity Planner, which uses the same two-step pattern for this exact object). Reverting
   *  flips the allocation to REVERTED and frees its rooms; the subsequent runOverview() refetch
   *  then finds the cohort not-yet-committed again and rebuilds a fresh editable draft for it from
   *  real server suggestions, ready for Confirm & Commit. */
  protected revertAllocation(row: CohortAutoPlanSummary): void {
    const alloc = this.currentAllocation();
    if (!alloc) return;
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Revert Room Allocation',
        message: `Revert ${row.cohortLabel}'s committed room allocation for this term? This frees its rooms and rebuilds an editable draft you can adjust and re-commit.`,
        confirmText: 'Revert Allocation',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) this.doRevertAllocation(alloc.id);
    });
  }

  private doRevertAllocation(allocationId: number): void {
    this.revertingAllocation.set(true);
    this.cohortRoomAllocationService.revert(allocationId).subscribe({
      next: () => {
        this.toast.success('Room allocation reverted — rebuild and re-commit when ready');
        this.revertingAllocation.set(false);
        this.runOverview();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to revert room allocation');
        this.revertingAllocation.set(false);
      },
    });
  }

  protected draftFor(cohortId: number): CohortDraft | undefined {
    return this.drafts().get(cohortId);
  }

  protected batchesForSection(cohortId: number, sectionLabel: string): DraftBatchCard[] {
    return this.draftFor(cohortId)?.batches.filter((b) => b.sectionLabel === sectionLabel) ?? [];
  }

  /** Groups one section's batch rows -- scoped to ONE session type so the template can render Lab
   *  and Clinical as separate labeled blocks (Theory/Lab/Clinical must read as clearly distinct
   *  groups, never interleaved) -- by subject, so the subject name shows ONCE per group instead of
   *  repeating it on every sequential-turn batch row (see splitIntoSequentialBatches): a subject
   *  split into "Batch 1"/"Batch 2" reads as one subject with one shared venue picker and a small
   *  count box per batch underneath, not two full repeated rows each with their own venue picker. */
  protected batchGroupsForSection(cohortId: number, sectionLabel: string, sessionType: 'LAB' | 'CLINICAL'): BatchGroup[] {
    const groups = new Map<string, BatchGroup>();
    for (const batch of this.batchesForSection(cohortId, sectionLabel)) {
      if (batch.sessionType !== sessionType) continue;
      const key = `${batch.courseOfferingId}:${batch.sessionType}`;
      let group = groups.get(key);
      if (!group) {
        group = { courseOfferingId: batch.courseOfferingId, subjectName: batch.subjectName, sessionType: batch.sessionType, batches: [] };
        groups.set(key, group);
      }
      group.batches.push(batch);
    }
    return [...groups.values()];
  }

  /** The group's shared venue -- sequential batches of the same subject are the SAME physical
   *  location run at different times (see splitIntoSequentialBatches), so one picker governs every
   *  batch in the group rather than repeating a venue dropdown per box. Reads the first batch's
   *  venueId since #onGroupVenueChange keeps every batch in a group in lockstep. */
  protected groupVenueId(group: BatchGroup): number | null {
    return group.batches[0]?.venueId ?? null;
  }

  protected onGroupVenueChange(group: BatchGroup, newValue: number | null): void {
    group.batches.forEach((b) => { b.venueId = newValue; });
  }

  /** Venue options for the group's shared picker -- filtered against the LARGEST individual batch's
   *  planned size (the binding constraint, since batches run sequentially not simultaneously; the
   *  venue only ever needs to seat one turn at a time), reusing venueOptionsFor's existing
   *  eligible-first sort and never-drop-the-current-selection behavior. */
  protected venueOptionsForGroup(group: BatchGroup): RoomInventoryRow[] {
    const largestBatch = group.batches.reduce((a, b) => (b.plannedSize ?? 0) > (a.plannedSize ?? 0) ? b : a, group.batches[0]);
    return this.venueOptionsFor(group.sessionType, largestBatch);
  }

  /** Editing one section's planned size auto-redistributes the remainder of the cohort's real
   *  strength EQUALLY across every other section on this card (e.g. a 100-strong cohort's Section 1
   *  changed to 40 sends Section 2 to 60) -- same integer-only, never-a-fractional-split rule as the
   *  backend's own equal-split suggestion algorithm (see TimetableCapacityPlanningService
   *  .equalSplitSizes), so the sections always keep summing to the cohort's exact strength without
   *  the admin having to hand-adjust every other field themselves. Every affected section's
   *  Lab/Clinical batch rows are then recomputed against its NEW size too (see
   *  recomputeBatchesForSection) -- a stale batch total left over from the section's old size is
   *  exactly the confusing "batches don't add up to the section" bug this closes; the recomputed
   *  rows stay freely editable afterward like any other suggestion. */
  protected onSectionSizeChange(cohortId: number, section: DraftSectionCard, newValue: number | null): void {
    section.plannedSize = newValue;
    const draft = this.draftFor(cohortId);
    const row = this.overview()?.cohorts.find((r) => r.cohortId === cohortId);
    if (!draft || !row) return;
    const others = draft.sections.filter((s) => s !== section);
    if (others.length > 0) {
      const remaining = Math.max(0, row.cohortStrength - (newValue ?? 0));
      const base = Math.floor(remaining / others.length);
      const extra = remaining % others.length;
      others.forEach((s, i) => { s.plannedSize = base + (i < extra ? 1 : 0); });
    }

    this.recomputeBatchesForSection(cohortId, section.sectionLabel, newValue ?? 0);
    others.forEach((s) => this.recomputeBatchesForSection(cohortId, s.sectionLabel, s.plannedSize ?? 0));
  }

  /** Rebuilds every Lab/Clinical batch row scoped to one section against its (possibly just-edited)
   *  planned size -- same designated-venues-only sequential-batch split the backend runs (see
   *  splitIntoSequentialBatches), ported here so a section-size edit doesn't leave stale batch
   *  numbers that no longer sum to the section's real headcount. Regroups by subject/session-type
   *  (never inventing a group that didn't already exist -- a subject with no designated venue
   *  produces no batch rows to begin with, so there's nothing here to recompute for it), and MAY
   *  change the row count (e.g. a shrunk section that now fits one venue collapses back to a single
   *  unsplit row, or a grown one gains another sequential turn in the same venue) -- rows are
   *  otherwise freely editable afterward, same as any suggestion. */
  private recomputeBatchesForSection(cohortId: number, sectionLabel: string, newSize: number): void {
    const draft = this.draftFor(cohortId);
    if (!draft) return;
    const sectionBatches = draft.batches.filter((b) => b.sectionLabel === sectionLabel);
    if (sectionBatches.length === 0) return;

    const groupKeys = new Set(sectionBatches.map((b) => `${b.courseOfferingId}:${b.sessionType}`));
    const otherBatches = draft.batches.filter((b) => b.sectionLabel !== sectionLabel);
    const recomputed: DraftBatchCard[] = [];
    for (const key of groupKeys) {
      const sample = sectionBatches.find((b) => `${b.courseOfferingId}:${b.sessionType}` === key)!;
      const eligible = new Set(sample.eligibleVenueIds);
      const pool = (this.overview()?.roomInventory ?? [])
        .filter((r) => r.roomType === sample.sessionType && eligible.has(r.id))
        .sort((a, b) => (b.capacity ?? 0) - (a.capacity ?? 0));
      if (pool.length === 0) {
        recomputed.push(...sectionBatches.filter((b) => `${b.courseOfferingId}:${b.sessionType}` === key));
        continue;
      }
      const slots = this.splitIntoSequentialBatches(newSize, pool);
      slots.forEach((slot, i) => {
        recomputed.push({
          courseOfferingId: sample.courseOfferingId,
          subjectName: sample.subjectName,
          sessionType: sample.sessionType,
          sectionLabel,
          batchLabel: slots.length > 1 ? `Batch ${i + 1}` : null,
          venueId: slot.venue.id,
          plannedSize: slot.size,
          eligibleVenueIds: sample.eligibleVenueIds,
        });
      });
    }
    draft.batches = [...otherBatches, ...recomputed];
  }

  /** TypeScript port of TimetableCapacityPlanningService.splitIntoSequentialBatches -- unlike Theory
   *  sectioning (equalSplitSizes below, whose whole cohort attends at the SAME moment and needs
   *  distinct simultaneous rooms), two batches of the same subject are two SEPARATE scheduled
   *  sessions, so the same designated venue can be reused turn after turn. Tries the fewest-distinct
   *  -venues equal split first (spreads load when more than one designated venue exists); only when
   *  that can't cover it does it fall through to reusing the single largest designated venue across
   *  as many additional equal-sized turns as needed -- always fully covers. Kept in exact lockstep
   *  with the backend algorithm intentionally -- this is a client-side preview, not an independent one. */
  private splitIntoSequentialBatches(strength: number, venuesSortedDesc: RoomInventoryRow[]): { venue: RoomInventoryRow; size: number }[] {
    if (strength <= 0 || venuesSortedDesc.length === 0) return [];

    const distinctVenueSizes = this.equalSplitSizes(strength, venuesSortedDesc);
    const covered = distinctVenueSizes.reduce((sum, s) => sum + s, 0);
    if (covered >= strength) {
      return distinctVenueSizes.map((size, i) => ({ venue: venuesSortedDesc[i], size }));
    }

    const largest = venuesSortedDesc[0];
    const largestCapacity = largest.capacity ?? 0;
    if (largestCapacity <= 0) return [];
    const turns = Math.ceil(strength / largestCapacity);
    const base = Math.floor(strength / turns);
    const remainder = strength % turns;
    return Array.from({ length: turns }, (_, i) => ({ venue: largest, size: base + (i < remainder ? 1 : 0) }));
  }

  /** TypeScript port of TimetableCapacityPlanningService.equalSplitSizes -- fewest-rooms EQUAL split
   *  (never a fractional split, e.g. 95 across 2 rooms is 48+47) across the fewest of
   *  roomsSortedDesc whose capacity can each hold their equal share; falls back to greedy
   *  fill-to-capacity only when no N is feasible, legitimately under-covering rather than overfilling
   *  a room past capacity. Kept in exact lockstep with the backend algorithm intentionally -- this is
   *  a client-side preview of the same suggestion, not an independent one. */
  private equalSplitSizes(strength: number, roomsSortedDesc: RoomInventoryRow[]): number[] {
    if (strength <= 0 || roomsSortedDesc.length === 0) return [];

    let chosenN = -1;
    for (let n = 1; n <= roomsSortedDesc.length; n++) {
      const equalShare = Math.ceil(strength / n);
      const nthCapacity = roomsSortedDesc[n - 1].capacity;
      if (nthCapacity != null && nthCapacity >= equalShare) { chosenN = n; break; }
    }

    if (chosenN > 0) {
      const base = Math.floor(strength / chosenN);
      const remainder = strength % chosenN;
      return Array.from({ length: chosenN }, (_, i) => base + (i < remainder ? 1 : 0));
    }

    const sizes: number[] = [];
    let remaining = strength;
    for (const room of roomsSortedDesc) {
      if (remaining <= 0) break;
      const capacity = room.capacity ?? 0;
      if (capacity <= 0) continue;
      const size = Math.min(remaining, capacity);
      sizes.push(size);
      remaining -= size;
    }
    return sizes;
  }

  /** Candidate classrooms for swapping a section's suggested room -- every active Classroom not
   *  claimed by a genuinely COMMITTED cohort (a room another still-uncommitted cohort's card also
   *  proposes is left selectable; the backend's unique constraint is the real, atomic guard against
   *  two commits racing onto the same room, same as the existing Room Inventory "Free" chip is
   *  best-effort, not a lock), not already picked by a DIFFERENT section on this same card, and big
   *  enough to actually seat this section's own planned size -- a room smaller than that would only
   *  fail validation at Confirm & Commit anyway, so it's excluded from the picker itself. The
   *  section's CURRENT room always stays listed even if its size has since been edited past that
   *  room's capacity, so the picker never silently drops the admin's own in-progress selection. */
  protected classroomOptionsFor(cohortId: number, section: DraftSectionCard): RoomInventoryRow[] {
    const rooms = (this.overview()?.roomInventory ?? []).filter((r) => r.roomType === 'CLASSROOM');
    const pickedElsewhere = new Set(
      (this.draftFor(cohortId)?.sections ?? [])
        .filter((s) => s !== section && s.classroomId != null)
        .map((s) => s.classroomId));
    return rooms.filter((r) =>
      (!r.claimedByCohortLabel || r.id === section.classroomId)
      && !pickedElsewhere.has(r.id)
      && (r.id === section.classroomId || r.capacity == null || (section.plannedSize ?? 0) <= r.capacity));
  }

  /** Lab/Clinical venues are never exclusively locked per term (see RoomInventoryRow docs) and are
   *  capacity-filtered against THIS row's own planned size (a section split into several batches
   *  needs each row filtered against its own small planned size, not the section's full strength --
   *  an unsplit row's plannedSize already IS the section's full strength, so the same filter
   *  naturally covers that case too). Restricted to the subject's own designated venues (see
   *  eligibleVenueIds) -- never an unrelated one, matching the backend's own designated-only
   *  auto-suggestion. Falls back to the full active pool ONLY when the subject has no designated
   *  venue configured at all (empty eligibleVenueIds) -- that gap is already hard-blocked from
   *  committing elsewhere on this screen, so this just avoids leaving the dropdown completely empty
   *  while the admin goes to configure one. The row's CURRENTLY picked venue always stays listed,
   *  same reasoning as classroomOptionsFor above. */
  protected venueOptionsFor(sessionType: 'LAB' | 'CLINICAL', batch: DraftBatchCard): RoomInventoryRow[] {
    const options = (this.overview()?.roomInventory ?? []).filter((r) =>
      r.roomType === sessionType
      && (r.id === batch.venueId || r.capacity == null || (batch.plannedSize ?? 0) <= r.capacity));
    const eligible = new Set(batch.eligibleVenueIds);
    if (eligible.size === 0) return options;
    return options.filter((r) => eligible.has(r.id) || r.id === batch.venueId);
  }

  /** Section label(s) the room is currently picked for on the SELECTED cohort -- its open editable
   *  draft when not yet committed, or its real committed allocation when it IS committed (Room
   *  Inventory's "active" highlight either way, distinct from claimedByCohortLabel, which flags a
   *  room COMMITTED to a cohort). Without this second branch, a committed cohort's own rooms only
   *  ever rendered as generic red "claimed" -- indistinguishable from some OTHER cohort's rooms --
   *  since a committed cohort never has a draft (buildDrafts skips it entirely). --active is
   *  declared after --claimed in the SCSS, so on a room that's both (the committed cohort's own
   *  room, viewed on its own tab), --active's blue correctly wins over --claimed's red. */
  protected highlightedSectionsFor(room: RoomInventoryRow): string[] {
    const cohortId = this.selectedCohortId();
    if (cohortId == null) return [];

    const draft = this.draftFor(cohortId);
    if (draft) return this.highlightedSectionsInDraft(room, draft);

    const alloc = this.currentAllocation();
    if (alloc && alloc.cohortId === cohortId) return this.highlightedSectionsInAllocation(room, alloc);

    return [];
  }

  private highlightedSectionsInDraft(room: RoomInventoryRow, draft: CohortDraft): string[] {
    if (room.roomType === 'CLASSROOM') {
      return draft.sections.filter((s) => s.classroomId === room.id).map((s) => s.sectionLabel);
    }
    const labels = draft.batches
      .filter((b) => b.sessionType === room.roomType && b.venueId === room.id)
      .map((b) => b.sectionLabel);
    return [...new Set(labels)];
  }

  /** Same shape as highlightedSectionsInDraft, reading the real committed rows (alloc.sections'
   *  own classroomId, allocBatchesFor's Lab/Clinical venueId) instead of an editable draft's. */
  private highlightedSectionsInAllocation(room: RoomInventoryRow, alloc: CohortRoomAllocation): string[] {
    const labels: string[] = [];
    for (const section of alloc.sections) {
      if (room.roomType === 'CLASSROOM') {
        if (section.classroomId === room.id) labels.push(section.sectionLabel);
        continue;
      }
      if (this.allocBatchesFor(alloc, section, room.roomType).some((b) => b.venueId === room.id)) {
        labels.push(section.sectionLabel);
      }
    }
    return [...new Set(labels)];
  }

  protected roomCapacity(id: number | null, options: RoomInventoryRow[]): number | null {
    if (id == null) return null;
    return options.find((o) => o.id === id)?.capacity ?? null;
  }

  private batchName(batch: DraftBatchCard): string {
    const prefix = batch.sessionType === 'LAB' ? 'Lab' : 'Clinical';
    const base = `${prefix} - ${batch.sectionLabel}`;
    return batch.batchLabel ? `${base} - ${batch.batchLabel}` : base;
  }

  protected confirmAndCommit(row: CohortAutoPlanSummary): void {
    const draft = this.draftFor(row.cohortId);
    if (!draft || !this.selectedTermInstanceId) return;

    if (draft.sections.some((s) => !s.classroomId || !s.plannedSize)) {
      this.toast.error('Every section needs a classroom and a planned size');
      return;
    }
    const classroomIds = new Set(draft.sections.map((s) => s.classroomId));
    if (classroomIds.size !== draft.sections.length) {
      this.toast.error('Each section needs a different classroom');
      return;
    }
    for (const section of draft.sections) {
      const capacity = this.roomCapacity(section.classroomId, this.classroomOptionsFor(row.cohortId, section));
      if (capacity != null && (section.plannedSize ?? 0) > capacity) {
        this.toast.error(`${section.sectionLabel} plans more students than its classroom seats`);
        return;
      }
    }
    const sectionsTotal = draft.sections.reduce((sum, s) => sum + (s.plannedSize ?? 0), 0);
    if (sectionsTotal !== row.cohortStrength) {
      const diff = sectionsTotal - row.cohortStrength;
      this.toast.error(diff < 0
        ? `Sections cover only ${sectionsTotal} of ${row.cohortStrength} students`
        : `Sections cover ${sectionsTotal} students, ${diff} more than the ${row.cohortStrength} being planned for`);
      return;
    }
    if (draft.batches.some((b) => !b.venueId || !b.plannedSize)) {
      this.toast.error('Every Lab/Clinical batch needs a venue and planned size');
      return;
    }
    for (const batch of draft.batches) {
      const capacity = this.roomCapacity(batch.venueId, this.venueOptionsFor(batch.sessionType, batch));
      if (capacity != null && (batch.plannedSize ?? 0) > capacity) {
        this.toast.error('A Lab/Clinical batch plans more students than its venue seats');
        return;
      }
    }
    // Per (section, subject, session type) -- two subjects' Lab batches are independent partitions
    // of the same section, so each subject's own batches must sum to the section's planned size,
    // not the section's combined total across every subject. Mirrors the backend's own grouping
    // exactly (see CohortRoomAllocationService's VentureKey check) -- summing every row in the
    // section together (the old bug here) falsely rejected a perfectly valid split (e.g. two
    // Lab subjects each correctly split 25+25 across a 50-student section summed to 100, not 50).
    const ventureTotals = new Map<string, { sectionLabel: string; subjectName: string; sessionType: 'LAB' | 'CLINICAL'; total: number }>();
    for (const batch of draft.batches) {
      const key = `${batch.sectionLabel}|${batch.courseOfferingId}|${batch.sessionType}`;
      const existing = ventureTotals.get(key);
      if (existing) existing.total += batch.plannedSize ?? 0;
      else ventureTotals.set(key, { sectionLabel: batch.sectionLabel, subjectName: batch.subjectName, sessionType: batch.sessionType, total: batch.plannedSize ?? 0 });
    }
    for (const venture of ventureTotals.values()) {
      const sectionSize = draft.sections.find((s) => s.sectionLabel === venture.sectionLabel)?.plannedSize ?? 0;
      if (venture.total !== sectionSize) {
        this.toast.error(`${venture.subjectName}'s ${venture.sessionType} batches for ${venture.sectionLabel} plan ${venture.total} students, not exactly ${sectionSize}`);
        return;
      }
    }

    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Commit Room Allocation',
        message: `Commit ${row.cohortLabel}'s ${draft.sections.length} Theory section(s) and ${draft.batches.length} `
          + `Lab/Clinical batch(es) for this term? This becomes the physical-location basis for the timetable.`,
        confirmText: 'Confirm & Commit',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) this.doCommit(row, draft);
    });
  }

  private doCommit(row: CohortAutoPlanSummary, draft: CohortDraft): void {
    this.committingCohortId.set(row.cohortId);
    const sections: CohortSectionRequest[] = draft.sections.map((s) => ({
      sectionLabel: s.sectionLabel,
      classroomId: s.classroomId!,
      plannedSize: s.plannedSize!,
    }));
    const ventureSplits: VentureSplit[] = draft.batches.map((b) => ({
      courseOfferingId: b.courseOfferingId,
      sessionType: b.sessionType,
      venueId: b.venueId!,
      batchName: this.batchName(b),
      plannedSize: b.plannedSize!,
      cohortSectionLabel: b.sectionLabel,
    }));
    this.cohortRoomAllocationService.commit({
      cohortId: row.cohortId,
      termInstanceId: this.selectedTermInstanceId!,
      planningBasis: 'SANCTIONED',
      sections,
      ventureSplits,
    }).subscribe({
      next: () => {
        this.toast.success(`${row.cohortLabel}'s room allocation committed`);
        this.committingCohortId.set(null);
        // Refetch rather than patch locally -- committing this cohort can change what's free for
        // every OTHER not-yet-planned cohort's own suggestion (their cards need to reflect that).
        this.runOverview();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to commit room allocation');
        this.committingCohortId.set(null);
      },
    });
  }

  private loadTermInstances(academicYearId: number): void {
    this.termsLoading.set(true);
    this.academicYearService.getTermInstancesByAcademicYear(academicYearId).subscribe({
      next: (terms) => {
        this.termInstances.set(terms);
        this.termsLoading.set(false);
        this.selectedTermInstanceId = terms[0]?.id ?? null;
        if (this.selectedTermInstanceId) this.runOverview();
        else this.overview.set(null);
      },
      error: () => { this.toast.error('Failed to load term instances'); this.termsLoading.set(false); },
    });
  }

  private runOverview(): void {
    this.loading.set(true);
    this.capacityPlannerService.getTermOverview(this.selectedTermInstanceId!, 'SANCTIONED').subscribe({
      next: (response) => {
        this.overview.set(response);
        this.drafts.set(this.buildDrafts(response));
        this.selectDefaultCohortTab(response);
        this.loadAllocationIfCommitted(this.selectedCohortId());
        this.loading.set(false);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to auto-plan this term');
        this.loading.set(false);
      },
    });
  }

  /** Keeps the current tab selected if it still exists in the refreshed cohort list (so committing
   *  or editing one cohort and re-running Auto-Plan All doesn't yank the admin's focus elsewhere);
   *  otherwise defaults to the first not-yet-planned cohort, or just the first cohort if every one
   *  is already committed. */
  private selectDefaultCohortTab(response: TermCapacityOverview): void {
    const currentId = this.selectedCohortId();
    if (currentId != null && response.cohorts.some((r) => r.cohortId === currentId)) return;
    const firstNotPlanned = response.cohorts.find((r) => !r.hasCommittedAllocation);
    this.selectedCohortId.set((firstNotPlanned ?? response.cohorts[0])?.cohortId ?? null);
  }

  /** One editable draft per not-yet-committed cohort that actually has a suggestion to review --
   *  a cohort with zero suggestedSections (no classroom available at all) has nothing to build a
   *  card editor around, so it's left out of the map and the template falls back to its shortfall
   *  message instead. */
  private buildDrafts(response: TermCapacityOverview): Map<number, CohortDraft> {
    const map = new Map<number, CohortDraft>();
    for (const row of response.cohorts) {
      if (row.hasCommittedAllocation || row.suggestedSections.length === 0) continue;
      map.set(row.cohortId, {
        sections: row.suggestedSections.map((s) => ({
          sectionLabel: s.sectionLabel,
          classroomId: s.classroomId,
          plannedSize: s.plannedSize,
        })),
        batches: row.suggestedLabClinicalBatches.map((b) => ({
          courseOfferingId: b.courseOfferingId,
          subjectName: b.subjectName,
          sessionType: b.sessionType,
          sectionLabel: b.sectionLabel,
          batchLabel: b.batchLabel,
          venueId: b.venueId,
          plannedSize: b.plannedSize,
          eligibleVenueIds: b.eligibleVenueIds,
        })),
      });
    }
    return map;
  }
}

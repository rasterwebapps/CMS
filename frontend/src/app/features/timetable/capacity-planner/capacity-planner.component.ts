import { Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear, CohortSummary, CourseOffering, TermInstance } from '../../academic-year/academic-year.model';
import { CapacityPlannerService } from './capacity-planner.service';
import { CapacityPlan, FacultyWorkloadReport, PlanningBasis, VenueOption } from './capacity-planner.model';
import { CmsCapacityMeterComponent } from '../../../shared/capacity-meter/capacity-meter.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';
import { PortionBlueprintService } from '../portion-blueprint.service';
import { PortionShortfall } from '../portion-blueprint.model';
import { CohortRoomAllocationService } from './cohort-room-allocation.service';
import { AllocatedBatch, CohortRoomAllocation, CohortSection, CohortSectionRequest, VentureSplit } from './cohort-room-allocation.model';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { CAPACITY_PLANNER_TOUR } from '../../../shared/tour/tours/timetable.tours';

/** One physical batch row within a subject block, scoped to a single cohort section. Starts as
 *  the section's whole headcount; Split breaks it into 2+ sibling rows (same section, own venue/
 *  size each) when one venue can't seat the whole section. */
interface LabDraftRow {
  localId: number;
  sectionLabel: string;
  /** null until this row has been produced by a Split — drives both the "Batch N" display-label
   *  suffix and whether Remove collapses the group back down to a single unsplit row. */
  groupLabel: string | null;
  venueId: number | null;
  plannedSize: number | null;
}

/** A subject picked once for an entire Lab/Clinical session — its rows (one per finalized cohort
 *  section, pre-filled from the block's own default venue) are generated automatically the moment
 *  a subject is chosen, so the subject/venue never needs re-picking per row. One block per subject;
 *  add another block to plan a second lab-bearing subject this term. */
interface LabSessionBlock {
  localId: number;
  sessionType: 'LAB' | 'CLINICAL';
  courseOfferingId: number | null;
  /** Applied to every row that doesn't yet have its own venue override — mirrors how picking a
   *  venue here should feel "shared" even though each row can still be repointed individually. */
  defaultVenueId: number | null;
  rows: LabDraftRow[];
}

interface DraftSection {
  localId: number;
  sectionLabel: string;
  classroomId: number | null;
  plannedSize: number | null;
}

@Component({
  selector: 'app-capacity-planner',
  standalone: true,
  imports: [
    FormsModule, MatDialogModule, MatProgressSpinnerModule, MatIconModule,
    CmsCapacityMeterComponent, CmsEmptyStateComponent, CmsTourButtonComponent, DecimalPipe, DatePipe,
  ],
  templateUrl: './capacity-planner.component.html',
  styleUrl: './capacity-planner.component.scss',
})
export class CapacityPlannerComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly capacityPlannerService = inject(CapacityPlannerService);
  private readonly permissionService = inject(PermissionService);
  private readonly toast = inject(ToastService);
  private readonly portionBlueprintService = inject(PortionBlueprintService);
  private readonly cohortRoomAllocationService = inject(CohortRoomAllocationService);
  private readonly dialog = inject(MatDialog);
  private readonly tourService = inject(TourService);

  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly termInstances = signal<TermInstance[]>([]);
  protected readonly cohorts = signal<CohortSummary[]>([]);
  protected readonly offerings = signal<CourseOffering[]>([]);
  protected readonly plan = signal<CapacityPlan | null>(null);

  protected readonly loading = signal(false);

  // ─── Faculty Workload tab (advisory, term-wide, no cohort scoping) ───
  protected readonly activeTab = signal<'rooms' | 'faculty'>('rooms');
  protected readonly facultyWorkload = signal<FacultyWorkloadReport | null>(null);
  protected readonly loadingFacultyWorkload = signal(false);

  // ─── Cohort Room Allocation (physical Theory/Lab/Clinical room commit) ───
  // Theory/Lab/Clinical pickers below source real Classroom/Lab/ClinicalVenue entities from the
  // plan's own already capacity-filtered fittingClassrooms/fittingLabs/fittingClinicalVenues —
  // NOT raw physical Rooms. CohortRoomAllocation's sections[].classroomId (and each split's
  // venueId) are real Classroom/Lab/ClinicalVenue foreign keys, the same ones Skeleton
  // Builder/Staffing use downstream; picking a Room id here would silently commit to whatever
  // Classroom/Lab/venue happens to share that same numeric id, which is exactly the bug this
  // replaced.
  protected readonly currentAllocation = signal<CohortRoomAllocation | null>(null);
  protected readonly loadingAllocation = signal(false);
  protected readonly committingAllocation = signal(false);
  protected readonly revertingAllocation = signal(false);
  protected readonly draftSections = signal<DraftSection[]>([]);
  protected readonly sectionsFinalized = signal(false);
  protected readonly sessionBlocks = signal<LabSessionBlock[]>([]);
  private nextBlockLocalId = 1;
  private nextRowLocalId = 1;
  private nextSectionLocalId = 1;

  protected canManageAllocation(): boolean {
    return this.permissionService.has('TIMETABLE_COHORT_ROOM_ALLOCATION_MANAGE');
  }

  protected canRevertAllocation(): boolean {
    return this.permissionService.has('TIMETABLE_COHORT_ROOM_ALLOCATION_REVERT');
  }

  /** Which fitting-venue list a block/row's capacity meter/options should read from, based on its
   *  own session type. */
  protected venueOptionsFor(sessionType: 'LAB' | 'CLINICAL'): VenueOption[] {
    const plan = this.plan();
    if (!plan) return [];
    return sessionType === 'LAB' ? plan.fittingLabs : plan.fittingClinicalVenues;
  }

  protected venueCapacity(venueId: number | null, options: VenueOption[]): number | null {
    if (venueId == null) return null;
    return options.find((o) => o.id === venueId)?.capacity ?? null;
  }

  protected classroomCapacity(classroomId: number | null): number | null {
    if (classroomId == null) return null;
    return this.plan()?.classroomsForSectioning.find((c) => c.id === classroomId)?.capacity ?? null;
  }

  /** Share of this classroom's weekly slots already used by OTHER cohorts' real Theory sessions
   *  this term (from Venue Utilization) -- surfaced in the Theory Sections picker so committing
   *  to a heavily-booked room is a visible, informed choice rather than a surprise later in
   *  Skeleton Builder when there's no free slot left to place this cohort's own sessions. */
  protected classroomUtilizationPercent(classroomId: number | null): number | null {
    if (classroomId == null) return null;
    return this.plan()?.classroomUtilization.find((u) => u.id === classroomId)?.utilizationPercent ?? null;
  }

  /** Whether this classroom is part of the current draft (still being edited) or the already-
   *  committed allocation — drives the Venue Utilization side card's highlight so the admin can
   *  see, at a glance, how busy the room(s) they're about to use already are. */
  protected isClassroomHighlighted(id: number): boolean {
    if (this.draftSections().some((s) => s.classroomId === id)) return true;
    const alloc = this.currentAllocation();
    return !!alloc?.sections.some((s) => s.isActive && s.classroomId === id);
  }

  /** Lab/Clinical batches belonging to one Theory section, for the grouped committed-allocation
   *  view. cohortSectionLabel is only populated when the commit had 2+ sections (see VentureSplit
   *  above) -- with exactly one section every batch belongs to it regardless. */
  protected batchesForSection(section: CohortSection, alloc: CohortRoomAllocation): AllocatedBatch[] {
    if (alloc.sections.length <= 1) return alloc.batches;
    return alloc.batches.filter((b) => b.cohortSectionLabel === section.sectionLabel);
  }

  protected isVenueHighlighted(sessionType: 'LAB' | 'CLINICAL', id: number): boolean {
    if (this.sessionBlocks().some((b) => b.sessionType === sessionType && b.rows.some((r) => r.venueId === id))) return true;
    const alloc = this.currentAllocation();
    return !!alloc?.batches.some((b) => b.isActive && b.sessionType === sessionType && b.venueId === id);
  }

  protected isRowOverCapacity(block: LabSessionBlock, row: LabDraftRow): boolean {
    const capacity = this.venueCapacity(row.venueId, this.venueOptionsFor(block.sessionType));
    return capacity != null && row.plannedSize != null && row.plannedSize > capacity;
  }

  protected isSectionOverCapacity(section: DraftSection): boolean {
    const capacity = this.classroomCapacity(section.classroomId);
    return capacity != null && section.plannedSize != null && section.plannedSize > capacity;
  }

  protected labBlocks(): LabSessionBlock[] {
    return this.sessionBlocks().filter((b) => b.sessionType === 'LAB');
  }

  protected clinicalBlocks(): LabSessionBlock[] {
    return this.sessionBlocks().filter((b) => b.sessionType === 'CLINICAL');
  }

  /** Subjects with Lab/Clinical hours this term that aren't already claimed by another block of
   *  the same session type — a subject's Lab session is planned once, in one block; the block's
   *  own currently-picked subject stays in its own list so re-opening its dropdown doesn't hide it. */
  protected offeringsForBlock(block: LabSessionBlock): CourseOffering[] {
    const usedElsewhere = new Set(
      this.sessionBlocks()
        .filter((b) => b !== block && b.sessionType === block.sessionType && b.courseOfferingId != null)
        .map((b) => b.courseOfferingId),
    );
    return this.offerings().filter((o) => {
      const hasHours = block.sessionType === 'LAB' ? o.labHours > 0 : o.clinicalHours > 0;
      return hasHours && !usedElsewhere.has(o.id);
    });
  }

  /** Whether at least one more not-yet-used Lab/Clinical subject exists, to gate the "+ Add
   *  Subject" button once every lab-bearing subject already has its own block. */
  protected hasMoreOfferings(sessionType: 'LAB' | 'CLINICAL'): boolean {
    const used = new Set(this.sessionBlocks().filter((b) => b.sessionType === sessionType && b.courseOfferingId != null).map((b) => b.courseOfferingId));
    return this.offerings().some((o) => (sessionType === 'LAB' ? o.labHours > 0 : o.clinicalHours > 0) && !used.has(o.id));
  }

  protected addLabBlock(sessionType: 'LAB' | 'CLINICAL'): void {
    this.sessionBlocks.update((blocks) => [
      ...blocks,
      { localId: this.nextBlockLocalId++, sessionType, courseOfferingId: null, defaultVenueId: null, rows: [] },
    ]);
  }

  protected removeLabBlock(localId: number): void {
    this.sessionBlocks.update((blocks) => blocks.filter((b) => b.localId !== localId));
  }

  /** Picking the subject is what triggers row generation — one row per finalized cohort section,
   *  pre-sized to that section's full headcount and left to inherit whatever default venue gets
   *  picked next. Only fires once per block (guarded by an empty rows array) so re-picking the
   *  same subject never clobbers in-progress venue/split edits. */
  protected onBlockSubjectChange(block: LabSessionBlock, value: number | null): void {
    this.updateBlock(block, (b) => {
      if (value == null || b.rows.length > 0) return { ...b, courseOfferingId: value };
      const rows: LabDraftRow[] = this.draftSections().map((s) => ({
        localId: this.nextRowLocalId++,
        sectionLabel: s.sectionLabel,
        groupLabel: null,
        venueId: b.defaultVenueId,
        plannedSize: s.plannedSize,
      }));
      return { ...b, courseOfferingId: value, rows };
    });
  }

  /** Applies the block's venue to every one of its rows -- there's no per-row venue override, a
   *  subject's batches all share the one venue picked here, splitting is purely a headcount
   *  decision. Deliberately does NOT shrink a row's size to fit — an unsplit row always shows the
   *  section's real headcount, even when that's more than the venue seats; isRowOverCapacity()
   *  surfaces the shortfall and prompts the user to explicitly create batches rather than silently
   *  losing students off a number they never agreed to change. */
  protected onBlockVenueChange(block: LabSessionBlock, value: number | null): void {
    this.updateBlock(block, (b) => ({
      ...b,
      defaultVenueId: value,
      rows: b.rows.map((r) => ({ ...r, venueId: value })),
    }));
  }

  /** Every block/row mutation below goes through here: replaces the target block with a freshly
   *  computed one inside a brand new sessionBlocks array, so every change is a genuine signal
   *  write (never an in-place mutation of an object the signal already holds) and the template's
   *  bindings are guaranteed to observe it. */
  private updateBlock(block: LabSessionBlock, compute: (b: LabSessionBlock) => LabSessionBlock): void {
    this.sessionBlocks.update((blocks) => blocks.map((b) => (b === block ? compute(b) : b)));
  }

  /** Every row belonging to one cohort section, in creation order — a single row means that
   *  section hasn't been broken into batches yet; 2+ means it has. */
  protected rowsForSection(block: LabSessionBlock, sectionLabel: string): LabDraftRow[] {
    return block.rows.filter((r) => r.sectionLabel === sectionLabel);
  }

  /** Sum of a section's batch rows once in batch mode -- must land on EXACTLY the section's real
   *  headcount before committing (see isSectionGroupMismatched): under-covering silently leaves
   *  some students with no Lab/Clinical batch at all, which the backend rejects the same as
   *  over-committing. */
  protected sectionGroupTotal(block: LabSessionBlock, sectionLabel: string): number {
    return this.rowsForSection(block, sectionLabel).reduce((sum, r) => sum + (r.plannedSize ?? 0), 0);
  }

  protected isSectionGroupMismatched(block: LabSessionBlock, section: DraftSection): boolean {
    return this.sectionGroupTotal(block, section.sectionLabel) !== (section.plannedSize ?? 0);
  }

  /** Converts a section's single unsplit row into the first batch of an explicit, freely
   *  add-to-able list — defaults its size down to the venue's capacity when the section doesn't
   *  fit whole, leaving the remainder unplaced until "+ Add Batch" covers it. No automatic
   *  halving/splitting: every subsequent batch is a deliberate Add, not a derived fraction. */
  protected createBatchesForSection(block: LabSessionBlock, sectionLabel: string): void {
    const row = block.rows.find((r) => r.sectionLabel === sectionLabel);
    if (!row) return;
    const total = row.plannedSize ?? 0;
    const capacity = this.venueCapacity(row.venueId, this.venueOptionsFor(block.sessionType));
    const firstSize = capacity != null && capacity < total ? capacity : total;
    this.updateBlock(block, (b) => ({
      ...b,
      rows: b.rows.map((r) => (r !== row ? r : { ...r, groupLabel: 'Batch 1', plannedSize: firstSize })),
    }));
  }

  /** Appends one more batch row to a section already in batch mode, defaulted to whatever's still
   *  unplaced (capped to the venue's capacity) so the common case needs no manual retyping, but
   *  freely editable afterward like every other batch. */
  protected addBatchForSection(block: LabSessionBlock, sectionLabel: string): void {
    const existing = block.rows.filter((r) => r.sectionLabel === sectionLabel);
    const section = this.draftSections().find((s) => s.sectionLabel === sectionLabel);
    const placed = existing.reduce((sum, r) => sum + (r.plannedSize ?? 0), 0);
    const remaining = Math.max(0, (section?.plannedSize ?? 0) - placed);
    const capacity = this.venueCapacity(block.defaultVenueId, this.venueOptionsFor(block.sessionType));
    const size = capacity != null && remaining > capacity ? capacity : remaining;
    const newRow: LabDraftRow = {
      localId: this.nextRowLocalId++,
      sectionLabel,
      groupLabel: `Batch ${existing.length + 1}`,
      venueId: block.defaultVenueId,
      plannedSize: size > 0 ? size : null,
    };
    this.updateBlock(block, (b) => ({ ...b, rows: [...b.rows, newRow] }));
  }

  /** Removing a row collapses its section back to a single ungrouped row, reset to the section's
   *  full headcount, once only one is left. */
  protected removeRow(block: LabSessionBlock, row: LabDraftRow): void {
    this.updateBlock(block, (b) => {
      const rows = b.rows.filter((r) => r !== row);
      const remaining = rows.filter((r) => r.sectionLabel === row.sectionLabel);
      const section = this.draftSections().find((s) => s.sectionLabel === row.sectionLabel);
      if (remaining.length === 1) {
        const solo = remaining[0];
        return {
          ...b,
          rows: rows.map((r) => (r !== solo ? r : { ...r, groupLabel: null, plannedSize: section ? section.plannedSize : r.plannedSize })),
        };
      }
      // Removing the LAST batch of a section would otherwise leave it with zero rows -- neither
      // the plain unsplit line nor the batch list has anything to render, a dead end with no way
      // back. Regenerate the unsplit line instead, same as collapsing from 2 batches down to 1.
      if (remaining.length === 0 && section) {
        const freshRow: LabDraftRow = {
          localId: this.nextRowLocalId++,
          sectionLabel: row.sectionLabel,
          groupLabel: null,
          venueId: b.defaultVenueId,
          plannedSize: section.plannedSize,
        };
        return { ...b, rows: [...rows, freshRow] };
      }
      return { ...b, rows };
    });
  }

  /** Plain on-screen label ("Section 1", "Section 1 - Batch 2") -- kept distinct from the batch
   *  name actually sent to the backend (rowBatchName below), which needs a session-type prefix the
   *  user doesn't need to see. */
  protected rowDisplayLabel(row: LabDraftRow): string {
    return row.groupLabel ? `${row.sectionLabel} - ${row.groupLabel}` : row.sectionLabel;
  }

  /** batches has a DB-level UNIQUE(course_offering_id, name) -- a subject with BOTH Lab and
   *  Clinical hours (the normal case for a nursing clinical subject) gets a Lab block AND a
   *  Clinical block sharing the same course offering, so the session-type prefix is required, not
   *  cosmetic: without it, both blocks' "Section 1" row would collide on that constraint. Never
   *  shown on screen -- see rowDisplayLabel for that. */
  protected rowBatchName(block: LabSessionBlock, row: LabDraftRow): string {
    const prefix = block.sessionType === 'LAB' ? 'Lab' : 'Clinical';
    const base = `${prefix} - ${row.sectionLabel}`;
    return row.groupLabel ? `${base} - ${row.groupLabel}` : base;
  }

  // ─── Theory sections ───

  /** Greedy-fills every active, unclaimed classroom biggest-first until the planning-basis
   *  strength is covered — guarantees each default section fits its own room by construction.
   *  Pre-seeds 1 row when a single classroom already fits the whole cohort, 2+ rows when
   *  sectioning is forced. Only runs once per fresh plan (guarded by empty draftSections) so
   *  re-clicking Calculate never clobbers in-progress picks. */
  private autoGenerateDraftSections(plan: CapacityPlan): void {
    if (this.draftSections().length > 0) return;
    const pool = [...plan.classroomsForSectioning].sort((a, b) => (b.capacity ?? 0) - (a.capacity ?? 0));
    let remaining = plan.cohortStrength;
    const rows: DraftSection[] = [];
    for (const classroom of pool) {
      if (remaining <= 0) break;
      const capacity = classroom.capacity ?? 0;
      if (capacity <= 0) continue;
      const size = Math.min(remaining, capacity);
      rows.push({
        localId: this.nextSectionLocalId++,
        sectionLabel: `Section ${rows.length + 1}`,
        classroomId: classroom.id,
        plannedSize: size,
      });
      remaining -= size;
    }
    if (rows.length > 0) this.draftSections.set(rows);
  }

  /** New section starts pre-filled with whatever's still unplaced out of the cohort's real
   *  strength, not blank — so the admin sees at a glance how many seats are left to cover, and
   *  picking a classroom (onSectionClassroomChange) then clamps it down further if needed. */
  protected addDraftSection(): void {
    const plan = this.plan();
    const remaining = plan ? Math.max(0, plan.cohortStrength - this.sectionedTotal()) : null;
    this.draftSections.update((rows) => [
      ...rows,
      { localId: this.nextSectionLocalId++, sectionLabel: `Section ${rows.length + 1}`, classroomId: null, plannedSize: remaining },
    ]);
  }

  protected removeDraftSection(localId: number): void {
    this.draftSections.update((rows) => rows.filter((r) => r.localId !== localId));
  }

  /** Auto-fills this section's planned size the moment a classroom is picked -- capped to that
   *  room's own capacity, and to whatever's still unplaced across the OTHER sections, so it never
   *  silently proposes more seats than either the room or the cohort's remaining headcount allow.
   *  Still freely editable afterward. */
  protected onSectionClassroomChange(section: DraftSection, classroomId: number | null): void {
    section.classroomId = classroomId;
    const capacity = this.classroomCapacity(classroomId);
    if (capacity == null) return;
    const otherSectionsTotal = this.draftSections()
      .filter((s) => s !== section)
      .reduce((sum, s) => sum + (s.plannedSize ?? 0), 0);
    const plan = this.plan();
    const remaining = plan ? Math.max(0, plan.cohortStrength - otherSectionsTotal) : capacity;
    section.plannedSize = Math.min(capacity, remaining > 0 ? remaining : capacity);
  }

  protected sectionedTotal(): number {
    return this.draftSections().reduce((sum, s) => sum + (s.plannedSize ?? 0), 0);
  }

  protected finalizeSections(): void {
    const plan = this.plan();
    if (!plan) return;
    const sections = this.draftSections();
    if (sections.some((s) => !s.classroomId || !s.plannedSize)) {
      this.toast.error('Every section needs a classroom and a planned size');
      return;
    }
    if (sections.some((s) => this.isSectionOverCapacity(s))) {
      this.toast.error('A section plans more students than its classroom seats');
      return;
    }
    const classroomIds = new Set(sections.map((s) => s.classroomId));
    if (classroomIds.size !== sections.length) {
      this.toast.error('Each section needs a different classroom');
      return;
    }
    // Exact match, not "at least": the chosen basis (e.g. sanctioned intake, for a first-term
    // cohort whose live enrollment is still unsettled) is already the deliberate ceiling to plan
    // against, so covering more than it just double-books an extra room for seats that don't exist.
    if (this.sectionedTotal() !== plan.cohortStrength) {
      const diff = this.sectionedTotal() - plan.cohortStrength;
      this.toast.error(diff < 0
        ? `Sections cover only ${this.sectionedTotal()} of ${plan.cohortStrength} students`
        : `Sections cover ${this.sectionedTotal()} students, ${diff} more than the ${plan.cohortStrength} being planned for`);
      return;
    }
    this.sectionsFinalized.set(true);
  }

  protected changeSections(): void {
    this.sectionsFinalized.set(false);
  }

  protected commitAllocation(): void {
    const plan = this.plan();
    if (!plan) return;
    const sections = this.draftSections();
    const blocks = this.sessionBlocks();
    if (blocks.some((b) => !b.courseOfferingId)) {
      this.toast.error('Every Lab/Clinical block needs a subject selected');
      return;
    }
    const rows = blocks.flatMap((b) => b.rows);
    if (rows.some((r) => !r.venueId || !r.plannedSize)) {
      this.toast.error('Every Lab/Clinical row needs a venue and planned size');
      return;
    }
    for (const block of blocks) {
      if (block.rows.some((r) => this.isRowOverCapacity(block, r))) {
        this.toast.error('A Lab/Clinical batch plans more students than its venue seats');
        return;
      }
      for (const section of sections) {
        if (this.isSectionGroupMismatched(block, section)) {
          this.toast.error(`${section.sectionLabel}'s batches must add up to exactly ${section.plannedSize} students`);
          return;
        }
      }
    }

    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Commit Room Allocation',
        message: `Commit ${plan.cohortLabel}'s ${sections.length} Theory section(s) and ${rows.length} Lab/Clinical `
          + `batch(es) for ${plan.termLabel}? This becomes the physical-location basis for the timetable.`,
        confirmText: 'Commit',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) this.doCommitAllocation(plan, sections, blocks);
    });
  }

  private doCommitAllocation(plan: CapacityPlan, sections: DraftSection[], blocks: LabSessionBlock[]): void {
    this.committingAllocation.set(true);
    const sectionRequests: CohortSectionRequest[] = sections.map((s) => ({
      sectionLabel: s.sectionLabel,
      classroomId: s.classroomId!,
      plannedSize: s.plannedSize!,
    }));
    const ventureSplits: VentureSplit[] = blocks.flatMap((b) => b.rows.map((r) => ({
      courseOfferingId: b.courseOfferingId!,
      sessionType: b.sessionType,
      venueId: r.venueId!,
      batchName: this.rowBatchName(b, r),
      plannedSize: r.plannedSize!,
      cohortSectionLabel: r.sectionLabel,
    })));
    this.cohortRoomAllocationService.commit({
      cohortId: plan.cohortId,
      termInstanceId: plan.termInstanceId,
      planningBasis: this.planningBasis,
      sections: sectionRequests,
      ventureSplits,
    }).subscribe({
      next: (allocation) => {
        this.currentAllocation.set(allocation);
        this.draftSections.set([]);
        this.sessionBlocks.set([]);
        this.sectionsFinalized.set(false);
        this.toast.success('Room allocation committed');
        this.committingAllocation.set(false);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to commit room allocation');
        this.committingAllocation.set(false);
      },
    });
  }

  protected revertAllocation(): void {
    const allocation = this.currentAllocation();
    if (!allocation) return;
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Revert Room Allocation',
        message: `Revert this committed allocation for ${allocation.cohortLabel}? The sections/batches it created will be deactivated (not deleted) and the rooms freed for another cohort.`,
        confirmText: 'Revert',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) this.doRevertAllocation(allocation.id);
    });
  }

  private doRevertAllocation(allocationId: number): void {
    this.revertingAllocation.set(true);
    this.cohortRoomAllocationService.revert(allocationId).subscribe({
      next: () => {
        this.currentAllocation.set(null);
        this.toast.success('Room allocation reverted');
        this.revertingAllocation.set(false);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to revert room allocation');
        this.revertingAllocation.set(false);
      },
    });
  }

  /** Auto-generating a draft only makes sense when this cohort has no committed allocation yet
   *  (the draft-builder UI only ever renders in that case) -- gated here, on the actual result,
   *  rather than fired unconditionally alongside this call. Firing it unconditionally used to
   *  silently pick a real classroom (e.g. the largest free lecture hall) into an invisible draft
   *  even when the committed-allocation view was what actually rendered, which then lit that room
   *  up as "active" in Venue Utilization with no on-screen explanation for why. */
  private loadCurrentAllocation(cohortId: number, termInstanceId: number, plan: CapacityPlan): void {
    this.loadingAllocation.set(true);
    this.cohortRoomAllocationService.getCurrent(cohortId, termInstanceId).subscribe({
      next: (allocation) => {
        this.currentAllocation.set(allocation);
        this.loadingAllocation.set(false);
        if (!allocation && this.canManageAllocation()) this.autoGenerateDraftSections(plan);
      },
      error: () => {
        this.currentAllocation.set(null);
        this.loadingAllocation.set(false);
        if (this.canManageAllocation()) this.autoGenerateDraftSections(plan);
      },
    });
  }

  // ─── Portion-completion shortfall (reuses this screen's own bufferHours) ───
  protected readonly shortfall = signal<PortionShortfall | null>(null);

  protected canViewShortfall(): boolean {
    return this.permissionService.has('PROGRESS_REPORT_VIEW');
  }

  protected selectedAcademicYearId: number | null = null;
  protected selectedTermInstanceId: number | null = null;
  protected selectedCohortId: number | null = null;
  // Sanctioned intake is the safer default: a cohort's live enrolled headcount is often still
  // unsettled (especially in a first term, where students keep joining after classes start), so
  // planning against the university-sanctioned ceiling avoids undersizing rooms/batches early on.
  protected planningBasis: PlanningBasis = 'SANCTIONED';

  ngOnInit(): void {
    this.tourService.register('capacity-planner', CAPACITY_PLANNER_TOUR);
    this.academicYearService.getAllAcademicYears().subscribe({
      next: (years) => {
        this.academicYears.set(years);
        const initialYearId = years.find((y) => y.isCurrent)?.id ?? years[0]?.id ?? null;
        if (initialYearId) {
          this.selectedAcademicYearId = initialYearId;
          this.loadTermInstances(initialYearId);
          this.loadCohorts(initialYearId);
        }
      },
      error: () => this.toast.error('Failed to load academic years'),
    });
  }

  protected onAcademicYearChange(): void {
    this.selectedTermInstanceId = null;
    this.selectedCohortId = null;
    this.offerings.set([]);
    this.plan.set(null);
    this.shortfall.set(null);
    this.resetAllocationDraft();
    this.facultyWorkload.set(null);
    if (this.selectedAcademicYearId) {
      this.loadTermInstances(this.selectedAcademicYearId);
      this.loadCohorts(this.selectedAcademicYearId);
    }
  }

  protected onTermChange(): void {
    this.offerings.set([]);
    this.plan.set(null);
    this.shortfall.set(null);
    this.resetAllocationDraft();
    this.facultyWorkload.set(null);
    this.autoLoadPlanIfReady();
    if (this.activeTab() === 'faculty') this.loadFacultyWorkload();
  }

  /** Faculty Workload is term-wide (no cohort selector), so switching to it just needs a term
   *  already picked — unlike the Room/Batch tab it never depends on a cohort. Lazily loads on
   *  first switch to this tab per term, not eagerly on every academic-year/term change, since the
   *  admin may never open it in a given session. */
  protected setActiveTab(tab: 'rooms' | 'faculty'): void {
    this.activeTab.set(tab);
    if (tab === 'faculty' && this.selectedTermInstanceId && !this.facultyWorkload() && !this.loadingFacultyWorkload()) {
      this.loadFacultyWorkload();
    }
  }

  protected selectedTermLabel(): string {
    const term = this.termInstances().find((t) => t.id === this.selectedTermInstanceId);
    return term ? `${term.termType} · ${term.status}` : '';
  }

  protected loadFacultyWorkload(): void {
    if (!this.selectedTermInstanceId) return;
    this.loadingFacultyWorkload.set(true);
    this.capacityPlannerService.getFacultyWorkloadReport(this.selectedTermInstanceId).subscribe({
      next: (report) => {
        this.facultyWorkload.set(report);
        this.loadingFacultyWorkload.set(false);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to load faculty workload report');
        this.loadingFacultyWorkload.set(false);
      },
    });
  }

  protected onCohortChange(): void {
    this.offerings.set([]);
    this.plan.set(null);
    this.shortfall.set(null);
    this.resetAllocationDraft();
    this.autoLoadPlanIfReady();
  }

  protected onPlanningBasisChange(): void {
    this.autoLoadPlanIfReady();
  }

  /** Loads the plan as soon as both a term and cohort are selected, without waiting for an
   *  explicit Calculate click — covers the initial page load's default selections as well as
   *  every subsequent term/cohort/basis change. */
  private autoLoadPlanIfReady(): void {
    if (this.selectedTermInstanceId && this.selectedCohortId) this.loadPlan();
  }

  private resetAllocationDraft(): void {
    this.currentAllocation.set(null);
    this.draftSections.set([]);
    this.sessionBlocks.set([]);
    this.sectionsFinalized.set(false);
  }

  private loadTermInstances(academicYearId: number): void {
    this.academicYearService.getTermInstancesByAcademicYear(academicYearId).subscribe({
      next: (terms) => {
        this.termInstances.set(terms);
        this.selectedTermInstanceId = terms[0]?.id ?? null;
        this.autoLoadPlanIfReady();
      },
      error: () => this.toast.error('Failed to load term instances'),
    });
  }

  private loadCohorts(academicYearId: number): void {
    this.academicYearService.getAllCohorts().subscribe({
      next: (cohorts) => {
        this.cohorts.set(cohorts);
        this.selectedCohortId = cohorts[0]?.id ?? null;
        this.autoLoadPlanIfReady();
      },
      error: () => this.toast.error('Failed to load cohorts'),
    });
  }

  /** Scoped to this cohort specifically (not just termInstance+semesterNumber) — a shared
   *  TermInstance can concurrently host other cohorts/programs whose own offerings happen to land
   *  on the exact same semesterNumber, which used to leak into this dropdown (e.g. another
   *  regulation's Term 3 subjects appearing alongside this cohort's real Term 1 papers). Cohort
   *  scoping also pins curriculumVersion server-side, which semesterNumber alone can't do. */
  private loadOfferings(termInstanceId: number, cohortId: number): void {
    this.academicYearService.getCourseOfferingsByTermInstance(termInstanceId, undefined, cohortId).subscribe({
      next: (offerings) => {
        this.offerings.set(offerings.filter((o) => !o.isElective));
      },
      error: () => this.toast.error('Failed to load course offerings'),
    });
  }

  protected loadPlan(): void {
    if (!this.selectedTermInstanceId || !this.selectedCohortId) return;
    this.loading.set(true);
    this.shortfall.set(null);
    const termInstanceId = this.selectedTermInstanceId;
    const cohortId = this.selectedCohortId;
    this.capacityPlannerService.getPlan(termInstanceId, cohortId, this.planningBasis)
      .subscribe({
        next: (data) => {
          this.plan.set(data);
          this.loading.set(false);
          this.loadOfferings(data.termInstanceId, data.cohortId);
          if (this.canViewShortfall()) this.loadShortfall(termInstanceId, cohortId);
          if (this.canManageAllocation() || this.canRevertAllocation()) this.loadCurrentAllocation(cohortId, termInstanceId, data);
        },
        error: (err) => {
          this.toast.error(err?.error?.message ?? 'Failed to load capacity plan');
          this.plan.set(null);
          this.loading.set(false);
        },
      });
  }

  private loadShortfall(termInstanceId: number, cohortId: number): void {
    this.portionBlueprintService.checkShortfall(termInstanceId, cohortId).subscribe({
      next: (data) => this.shortfall.set(data),
      // Quiet failure -- the shortfall banner is a bonus signal, not core to this screen's purpose.
      error: () => this.shortfall.set(null),
    });
  }

}

import { Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear, CohortSummary, CourseOffering, TermInstance } from '../../academic-year/academic-year.model';
import { CapacityPlannerService } from './capacity-planner.service';
import { CapacityPlan, VenueOption } from './capacity-planner.model';
import { CmsCapacityMeterComponent } from '../../../shared/capacity-meter/capacity-meter.component';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';
import { PortionBlueprintService } from '../portion-blueprint.service';
import { PortionShortfall } from '../portion-blueprint.model';
import { CohortRoomAllocationService } from './cohort-room-allocation.service';
import { CohortRoomAllocation, VentureSplit } from './cohort-room-allocation.model';

interface DraftSplit {
  localId: number;
  /** Picked per batch, not shared across a session type — two Lab batches for the same cohort can
   *  be entirely different subjects (e.g. English batch vs Tamil batch, run in parallel). */
  courseOfferingId: number | null;
  sessionType: 'LAB' | 'CLINICAL';
  venueId: number | null;
  batchName: string;
  plannedSize: number | null;
}

@Component({
  selector: 'app-capacity-planner',
  standalone: true,
  imports: [
    FormsModule, MatDialogModule, MatProgressSpinnerModule, RouterLink,
    CmsCapacityMeterComponent, DecimalPipe, DatePipe,
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

  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly termInstances = signal<TermInstance[]>([]);
  protected readonly cohorts = signal<CohortSummary[]>([]);
  protected readonly offerings = signal<CourseOffering[]>([]);
  protected readonly plan = signal<CapacityPlan | null>(null);

  protected readonly loading = signal(false);

  // ─── Cohort Room Allocation (physical Theory/Lab/Clinical room commit) ───
  // Theory/Lab/Clinical pickers below source real Classroom/Lab/ClinicalVenue entities from the
  // plan's own already capacity-filtered fittingClassrooms/fittingLabs/fittingClinicalVenues —
  // NOT raw physical Rooms. CohortRoomAllocation.theoryClassroomId (and each split's venueId) are
  // real Classroom/Lab/ClinicalVenue foreign keys, the same ones Skeleton Builder/Staffing use
  // downstream; picking a Room id here would silently commit to whatever Classroom/Lab/venue
  // happens to share that same numeric id, which is exactly the bug this replaced.
  protected readonly currentAllocation = signal<CohortRoomAllocation | null>(null);
  protected readonly loadingAllocation = signal(false);
  protected readonly committingAllocation = signal(false);
  protected readonly revertingAllocation = signal(false);
  protected selectedTheoryClassroomId: number | null = null;
  protected readonly theoryRoomFinalized = signal(false);
  protected readonly draftSplits = signal<DraftSplit[]>([]);
  private nextDraftLocalId = 1;

  protected canManageAllocation(): boolean {
    return this.permissionService.has('TIMETABLE_COHORT_ROOM_ALLOCATION_MANAGE');
  }

  protected canRevertAllocation(): boolean {
    return this.permissionService.has('TIMETABLE_COHORT_ROOM_ALLOCATION_REVERT');
  }

  /** Which fitting-venue list a split's capacity meter/options should read from, based on its
   *  own session type. */
  protected splitVenueOptions(split: DraftSplit): VenueOption[] {
    const plan = this.plan();
    if (!plan) return [];
    return split.sessionType === 'LAB' ? plan.fittingLabs : plan.fittingClinicalVenues;
  }

  protected venueCapacity(venueId: number | null, options: VenueOption[]): number | null {
    if (venueId == null) return null;
    return options.find((o) => o.id === venueId)?.capacity ?? null;
  }

  protected selectedTheoryClassroomLabel(): string {
    const plan = this.plan();
    if (!plan) return '';
    const classroom = plan.fittingClassrooms.find((c) => c.id === this.selectedTheoryClassroomId);
    return classroom ? `${classroom.name} (cap ${classroom.capacity})` : '';
  }

  protected finalizeTheoryRoom(): void {
    if (!this.selectedTheoryClassroomId) return;
    this.theoryRoomFinalized.set(true);
  }

  protected changeTheoryRoom(): void {
    this.theoryRoomFinalized.set(false);
  }

  protected isSplitOverCapacity(split: DraftSplit): boolean {
    const capacity = this.venueCapacity(split.venueId, this.splitVenueOptions(split));
    return capacity != null && split.plannedSize != null && split.plannedSize > capacity;
  }

  protected labSplits(): DraftSplit[] {
    return this.draftSplits().filter((s) => s.sessionType === 'LAB');
  }

  protected clinicalSplits(): DraftSplit[] {
    return this.draftSplits().filter((s) => s.sessionType === 'CLINICAL');
  }

  /** Defaults the new row's subject AND venue to the last existing row of the same session type
   *  — the common case is splitting ONE subject's lab across batches by headcount, not picking a
   *  different subject/venue each time, so re-selecting both on every row was pure redundancy.
   *  Still freely changeable per row for a genuine multi-subject split (e.g. an English batch and
   *  a Tamil batch run in parallel, or setting up a Rotation Group afterward). */
  protected addDraftSplit(sessionType: 'LAB' | 'CLINICAL'): void {
    const offeringId = this.selectedOfferingId;
    this.draftSplits.update((rows) => {
      const sameType = rows.filter((r) => r.sessionType === sessionType);
      const lastOfType = sameType[sameType.length - 1];
      return [
        ...rows,
        {
          localId: this.nextDraftLocalId++,
          courseOfferingId: lastOfType?.courseOfferingId ?? offeringId,
          sessionType,
          venueId: lastOfType?.venueId ?? null,
          batchName: `Batch ${String.fromCharCode(65 + sameType.length)}`,
          plannedSize: this.plan()?.targetBatchSize ?? null,
        },
      ];
    });
  }

  protected removeDraftSplit(localId: number): void {
    this.draftSplits.update((rows) => rows.filter((r) => r.localId !== localId));
  }

  /** Propagates a subject/venue pick to sibling rows of the same session type that are still
   *  blank/unset (never overwrites a row someone already deliberately customized) — covers the
   *  bulk `autoGenerateDraftSplits` case, where every row starts with the same subject already
   *  but venue is blank on all of them until the first one is picked. */
  protected onSplitSubjectChange(split: DraftSplit, value: number | null): void {
    const previous = split.courseOfferingId;
    split.courseOfferingId = value;
    this.draftSplits().forEach((row) => {
      if (row !== split && row.sessionType === split.sessionType && row.courseOfferingId === previous) {
        row.courseOfferingId = value;
      }
    });
  }

  protected onSplitVenueChange(split: DraftSplit, value: number | null): void {
    split.venueId = value;
    this.draftSplits().forEach((row) => {
      if (row !== split && row.sessionType === split.sessionType && row.venueId == null) {
        row.venueId = value;
      }
    });
  }

  /** Pre-fills exactly the number of batch rows the plan already says are needed, so the admin
   *  only has to pick a subject and venue for each rather than build the list from scratch — still
   *  freely add/removable/resizable afterward. Each batch keeps its own subject: two Lab batches
   *  for the same cohort can be entirely different subjects run in parallel (e.g. an English batch
   *  in the English lab and a Tamil batch in the Tamil lab at the same time), not just a capacity
   *  split of one subject. Only runs once per fresh plan (guarded by empty draftSplits) so
   *  re-clicking Calculate never clobbers in-progress picks. Needs offerings already resolved so
   *  each row starts with a real subject, not null. */
  private autoGenerateDraftSplits(plan: CapacityPlan): void {
    if (this.draftSplits().length > 0) return;
    const offeringId = this.selectedOfferingId;
    const rows: DraftSplit[] = [];
    if (plan.fittingLabs.length > 0) {
      for (let i = 0; i < plan.labBatchesNeeded; i++) {
        rows.push({
          localId: this.nextDraftLocalId++,
          courseOfferingId: offeringId,
          sessionType: 'LAB',
          venueId: null,
          batchName: `Batch ${String.fromCharCode(65 + i)}`,
          plannedSize: plan.targetBatchSize,
        });
      }
    }
    if (plan.fittingClinicalVenues.length > 0) {
      for (let i = 0; i < plan.clinicalBatchesNeeded; i++) {
        rows.push({
          localId: this.nextDraftLocalId++,
          courseOfferingId: offeringId,
          sessionType: 'CLINICAL',
          venueId: null,
          batchName: `Batch ${String.fromCharCode(65 + i)}`,
          plannedSize: plan.targetBatchSize,
        });
      }
    }
    if (rows.length > 0) this.draftSplits.set(rows);
  }

  protected commitAllocation(): void {
    const plan = this.plan();
    if (!plan || !this.selectedTheoryClassroomId) return;
    const splits = this.draftSplits();
    if (splits.some((s) => !s.courseOfferingId || !s.venueId || !s.batchName.trim() || !s.plannedSize)) {
      this.toast.error('Every batch row needs a subject, venue, name, and planned size');
      return;
    }

    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Commit Room Allocation',
        message: `Commit ${plan.cohortLabel}'s Theory room and ${splits.length} Lab/Clinical batch(es) for ${plan.termLabel}? This becomes the physical-location basis for the timetable.`,
        confirmText: 'Commit',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) this.doCommitAllocation(plan, splits);
    });
  }

  private doCommitAllocation(plan: CapacityPlan, splits: DraftSplit[]): void {
    this.committingAllocation.set(true);
    const ventureSplits: VentureSplit[] = splits.map((s) => ({
      courseOfferingId: s.courseOfferingId!,
      sessionType: s.sessionType,
      venueId: s.venueId!,
      batchName: s.batchName.trim(),
      plannedSize: s.plannedSize!,
    }));
    this.cohortRoomAllocationService.commit({
      cohortId: plan.cohortId,
      termInstanceId: plan.termInstanceId,
      theoryClassroomId: this.selectedTheoryClassroomId!,
      ventureSplits,
    }).subscribe({
      next: (allocation) => {
        this.currentAllocation.set(allocation);
        this.draftSplits.set([]);
        this.selectedTheoryClassroomId = null;
        this.theoryRoomFinalized.set(false);
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
        message: `Revert this committed allocation for ${allocation.cohortLabel}? The batches it created will be deactivated (not deleted) and the room freed for another cohort.`,
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

  private loadCurrentAllocation(cohortId: number, termInstanceId: number): void {
    this.loadingAllocation.set(true);
    this.cohortRoomAllocationService.getCurrent(cohortId, termInstanceId).subscribe({
      next: (allocation) => {
        this.currentAllocation.set(allocation);
        this.loadingAllocation.set(false);
      },
      error: () => {
        this.currentAllocation.set(null);
        this.loadingAllocation.set(false);
      },
    });
  }

  // ─── Portion-completion shortfall (reuses this screen's own bufferHours) ───
  protected readonly shortfall = signal<PortionShortfall | null>(null);
  protected readonly generatingBlueprint = signal(false);

  protected canViewShortfall(): boolean {
    return this.permissionService.has('PROGRESS_REPORT_VIEW');
  }

  protected canManageBlueprint(): boolean {
    return this.permissionService.has('PORTION_BLUEPRINT_MANAGE');
  }

  protected selectedAcademicYearId: number | null = null;
  protected selectedTermInstanceId: number | null = null;
  protected selectedCohortId: number | null = null;
  protected targetBatchSize: number | null = null;
  protected selectedOfferingId: number | null = null;

  ngOnInit(): void {
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
    if (this.selectedAcademicYearId) {
      this.loadTermInstances(this.selectedAcademicYearId);
      this.loadCohorts(this.selectedAcademicYearId);
    }
  }

  protected onTermChange(): void {
    this.selectedOfferingId = null;
    this.offerings.set([]);
    this.plan.set(null);
    this.shortfall.set(null);
    this.resetAllocationDraft();
  }

  protected onCohortChange(): void {
    this.selectedOfferingId = null;
    this.offerings.set([]);
    this.plan.set(null);
    this.shortfall.set(null);
    this.resetAllocationDraft();
  }

  private resetAllocationDraft(): void {
    this.currentAllocation.set(null);
    this.draftSplits.set([]);
    this.selectedTheoryClassroomId = null;
    this.theoryRoomFinalized.set(false);
  }

  private loadTermInstances(academicYearId: number): void {
    this.academicYearService.getTermInstancesByAcademicYear(academicYearId).subscribe({
      next: (terms) => {
        this.termInstances.set(terms);
        this.selectedTermInstanceId = terms[0]?.id ?? null;
      },
      error: () => this.toast.error('Failed to load term instances'),
    });
  }

  private loadCohorts(academicYearId: number): void {
    this.academicYearService.getAllCohorts().subscribe({
      next: (cohorts) => {
        this.cohorts.set(cohorts);
        this.selectedCohortId = cohorts[0]?.id ?? null;
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
        this.selectedOfferingId = this.offerings()[0]?.id ?? null;
        const plan = this.plan();
        if (plan && this.canManageAllocation()) this.autoGenerateDraftSplits(plan);
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
    this.capacityPlannerService.getPlan(termInstanceId, cohortId, this.targetBatchSize)
      .subscribe({
        next: (data) => {
          this.plan.set(data);
          this.loading.set(false);
          this.loadOfferings(data.termInstanceId, data.cohortId);
          if (this.canViewShortfall()) this.loadShortfall(termInstanceId, cohortId);
          if (this.canManageAllocation() || this.canRevertAllocation()) this.loadCurrentAllocation(cohortId, termInstanceId);
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

  protected generateBlueprint(): void {
    if (!this.selectedOfferingId) return;
    const offeringId = this.selectedOfferingId;
    this.generatingBlueprint.set(true);
    this.portionBlueprintService.generateBlueprint(offeringId).subscribe({
      next: () => {
        this.toast.success('Blueprint generated');
        this.generatingBlueprint.set(false);
        if (this.selectedTermInstanceId && this.selectedCohortId) {
          this.loadShortfall(this.selectedTermInstanceId, this.selectedCohortId);
        }
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to generate blueprint');
        this.generatingBlueprint.set(false);
      },
    });
  }

}

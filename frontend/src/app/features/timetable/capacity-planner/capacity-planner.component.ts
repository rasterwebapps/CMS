import { Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear, CohortSummary, CourseOffering, TermInstance } from '../../academic-year/academic-year.model';
import { CapacityPlannerService } from './capacity-planner.service';
import { CapacityPlan } from './capacity-planner.model';
import { BatchService } from '../../batch/batch.service';
import { CmsCapacityMeterComponent } from '../../../shared/capacity-meter/capacity-meter.component';
import { CmsRoomPickerComponent } from '../../../shared/room-picker/room-picker.component';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';
import { PortionBlueprintService } from '../portion-blueprint.service';
import { PortionShortfall } from '../portion-blueprint.model';
import { RoomPurposeCategoryService } from '../../hostel/room-purpose-category/room-purpose-category.service';
import { CampusInfrastructureService } from '../../hostel/campus-infrastructure/campus-infrastructure.service';
import { Room } from '../../hostel/campus-infrastructure/campus-infrastructure.model';
import { CohortRoomAllocationService } from './cohort-room-allocation.service';
import { CohortRoomAllocation, VentureSplit } from './cohort-room-allocation.model';

interface DraftSplit {
  localId: number;
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
    CmsCapacityMeterComponent, CmsRoomPickerComponent, DecimalPipe, DatePipe,
  ],
  templateUrl: './capacity-planner.component.html',
  styleUrl: './capacity-planner.component.scss',
})
export class CapacityPlannerComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly capacityPlannerService = inject(CapacityPlannerService);
  private readonly batchService = inject(BatchService);
  private readonly permissionService = inject(PermissionService);
  private readonly toast = inject(ToastService);
  private readonly portionBlueprintService = inject(PortionBlueprintService);
  private readonly roomPurposeCategoryService = inject(RoomPurposeCategoryService);
  private readonly campusInfrastructureService = inject(CampusInfrastructureService);
  private readonly cohortRoomAllocationService = inject(CohortRoomAllocationService);
  private readonly dialog = inject(MatDialog);

  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly termInstances = signal<TermInstance[]>([]);
  protected readonly cohorts = signal<CohortSummary[]>([]);
  protected readonly offerings = signal<CourseOffering[]>([]);
  protected readonly plan = signal<CapacityPlan | null>(null);

  protected readonly loading = signal(false);
  protected readonly creatingBatches = signal(false);

  // ─── Cohort Room Allocation (physical Theory/Lab/Clinical room commit) ───
  protected readonly academicCategoryId = signal<number | null>(null);
  protected readonly academicRooms = signal<Room[]>([]);
  protected readonly currentAllocation = signal<CohortRoomAllocation | null>(null);
  protected readonly loadingAllocation = signal(false);
  protected readonly committingAllocation = signal(false);
  protected readonly revertingAllocation = signal(false);
  protected selectedTheoryRoomId: number | null = null;
  protected readonly draftSplits = signal<DraftSplit[]>([]);
  private nextDraftLocalId = 1;

  protected canManageAllocation(): boolean {
    return this.permissionService.has('TIMETABLE_COHORT_ROOM_ALLOCATION_MANAGE');
  }

  protected canRevertAllocation(): boolean {
    return this.permissionService.has('TIMETABLE_COHORT_ROOM_ALLOCATION_REVERT');
  }

  protected roomCapacity(roomId: number | null): number | null {
    if (roomId == null) return null;
    return this.academicRooms().find((r) => r.id === roomId)?.capacity ?? null;
  }

  protected isSplitOverCapacity(split: DraftSplit): boolean {
    const capacity = this.roomCapacity(split.venueId);
    return capacity != null && split.plannedSize != null && split.plannedSize > capacity;
  }

  protected addDraftSplit(): void {
    const offeringId = this.selectedOfferingId;
    this.draftSplits.update((rows) => [
      ...rows,
      {
        localId: this.nextDraftLocalId++,
        courseOfferingId: offeringId,
        sessionType: 'LAB',
        venueId: null,
        batchName: `Batch ${String.fromCharCode(65 + rows.length)}`,
        plannedSize: null,
      },
    ]);
  }

  protected removeDraftSplit(localId: number): void {
    this.draftSplits.update((rows) => rows.filter((r) => r.localId !== localId));
  }

  protected commitAllocation(): void {
    const plan = this.plan();
    if (!plan || !this.selectedTheoryRoomId) return;
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
      theoryClassroomId: this.selectedTheoryRoomId!,
      ventureSplits,
    }).subscribe({
      next: (allocation) => {
        this.currentAllocation.set(allocation);
        this.draftSplits.set([]);
        this.selectedTheoryRoomId = null;
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

  private loadAcademicRooms(): void {
    this.roomPurposeCategoryService.getAll(true).subscribe({
      next: (categories) => {
        const academic = categories.find((c) => c.code === 'ACADEMIC');
        this.academicCategoryId.set(academic?.id ?? null);
        if (academic) {
          this.campusInfrastructureService.getRoomsByPurpose(academic.id).subscribe({
            next: (rooms) => this.academicRooms.set(rooms),
            error: () => this.academicRooms.set([]),
          });
        }
      },
      error: () => this.toast.error('Failed to load room purpose categories'),
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

  protected canCreateBatches(): boolean {
    return this.permissionService.has('TIMETABLE_CAPACITY_PLANNER_BATCH_CREATE');
  }

  ngOnInit(): void {
    this.loadAcademicRooms();
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
    this.selectedTheoryRoomId = null;
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

  /** Scoped to the plan's own semester (a cohort's enrolled students all share one semester in a
   *  given term) — the shared TermInstance otherwise packs every concurrent year's subjects
   *  together, which would dump every year's offerings into one dropdown. Falls back to
   *  unfiltered only if the cohort has no enrollment yet to derive a semester from. */
  private loadOfferings(termInstanceId: number, semesterNumber: number | null): void {
    this.academicYearService.getCourseOfferingsByTermInstance(termInstanceId, semesterNumber ?? undefined).subscribe({
      next: (offerings) => {
        this.offerings.set(offerings.filter((o) => !o.isElective));
        this.selectedOfferingId = this.offerings()[0]?.id ?? null;
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
          this.loadOfferings(data.termInstanceId, data.semesterNumber);
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

  protected createSuggestedBatches(): void {
    const plan = this.plan();
    if (!plan || !this.selectedOfferingId || plan.labBatchesNeeded <= 0) return;
    this.creatingBatches.set(true);
    this.batchService.autoCreate({
      courseOfferingId: this.selectedOfferingId,
      count: plan.labBatchesNeeded,
      capacity: plan.targetBatchSize,
    }).subscribe({
      next: (created) => {
        this.toast.success(`Created ${created.length} batch(es)`);
        this.creatingBatches.set(false);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to create batches');
        this.creatingBatches.set(false);
      },
    });
  }
}

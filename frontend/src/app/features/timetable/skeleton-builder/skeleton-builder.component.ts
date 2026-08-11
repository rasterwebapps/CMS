import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear, CohortSummary, TermInstance } from '../../academic-year/academic-year.model';
import { PeriodService } from '../../period/period.service';
import { Period } from '../../period/period.model';
import { SkeletonBuilderService } from './skeleton-builder.service';
import { SkeletonBuilderResponse, SkeletonCell, SkeletonPlacementCandidate, SkeletonSectionOption, SkeletonSessionType, SkeletonSubject } from './skeleton-builder.model';
import { WEEK_GRID_DAYS, WEEK_GRID_DAY_LABELS } from '../../../shared/week-grid/week-grid.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';
import { RotationSetupFlyoutComponent } from '../rotation-setup/rotation-setup-flyout.component';
import { colorForSubject } from './subject-color.util';

@Component({
  selector: 'app-skeleton-builder',
  standalone: true,
  imports: [FormsModule, RouterLink, MatDialogModule, MatProgressSpinnerModule, RotationSetupFlyoutComponent],
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

  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly termInstances = signal<TermInstance[]>([]);
  protected readonly cohorts = signal<CohortSummary[]>([]);
  protected readonly periods = signal<Period[]>([]);
  protected readonly skeleton = signal<SkeletonBuilderResponse | null>(null);

  protected readonly termsLoading = signal(false);
  protected readonly cohortsLoading = signal(false);
  protected readonly skeletonLoading = signal(false);
  protected readonly placing = signal(false);
  protected readonly suggesting = signal(false);

  protected selectedAcademicYearId: number | null = null;
  protected selectedTermInstanceId: number | null = null;
  protected selectedCohortId: number | null = null;

  /** Which subject in the rail is currently "active" for placement/suggest — the grid itself
   *  always shows every subject's cells together, this only scopes the placement panel + Suggest
   *  affordance. */
  protected activeOfferingId: number | null = null;

  protected readonly days = WEEK_GRID_DAYS;
  protected readonly dayLabels = WEEK_GRID_DAY_LABELS;

  protected readonly selectedCell = signal<{ day: string; periodId: number } | null>(null);
  protected selectedSessionType: SkeletonSessionType = 'THEORY';
  protected selectedBatchId: number | null = null;
  protected selectedCohortSectionId: number | null = null;

  protected readonly candidates = signal<SkeletonPlacementCandidate[]>([]);

  protected readonly needsBatch = computed(() => this.selectedSessionType !== 'THEORY');
  protected readonly needsSection = computed(() => this.selectedSessionType === 'THEORY' && this.activeSections().length > 0);
  protected readonly selectedPeriod = computed(() => {
    const cell = this.selectedCell();
    if (!cell) return null;
    return this.periods().find((p) => p.id === cell.periodId) ?? null;
  });
  protected readonly activeSubject = computed<SkeletonSubject | null>(() => {
    const id = this.activeOfferingId;
    if (!id) return null;
    return this.skeleton()?.subjects.find((s) => s.courseOfferingId === id) ?? null;
  });
  protected readonly activeBatches = computed(() => {
    const id = this.activeOfferingId;
    if (!id) return [];
    return this.skeleton()?.batches.filter((b) => b.courseOfferingId === id) ?? [];
  });
  protected readonly activeSections = computed<SkeletonSectionOption[]>(() => this.skeleton()?.sections ?? []);

  protected readonly showRotationSetup = signal(false);

  protected canManage(): boolean {
    return this.permissionService.has('TIMETABLE_SKELETON_MANAGE');
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

  ngOnInit(): void {
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
    this.activeOfferingId = null;
    if (this.selectedAcademicYearId) this.loadTermInstances(this.selectedAcademicYearId);
  }

  protected onTermChange(): void {
    this.tryLoadSkeleton();
  }

  protected onCohortChange(): void {
    this.tryLoadSkeleton();
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
        this.tryLoadSkeleton();
      },
      error: () => { this.toast.error('Failed to load cohorts'); this.cohortsLoading.set(false); },
    });
  }

  /** Term instances and cohorts load independently (in parallel) — this only fires the skeleton
   *  fetch once both a term and a cohort are actually selected, regardless of which one resolves
   *  last. */
  private tryLoadSkeleton(): void {
    this.cancelPlacement();
    this.activeOfferingId = null;
    if (this.selectedTermInstanceId && this.selectedCohortId) {
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
    this.cancelPlacement();
    this.candidates.set([]);
    this.skeletonService.getCohortSkeleton(termInstanceId, cohortId).subscribe({
      next: (data) => {
        this.skeleton.set(data);
        this.skeletonLoading.set(false);
        const stillPresent = data.subjects.some((s) => s.courseOfferingId === this.activeOfferingId);
        if (!stillPresent) this.activeOfferingId = data.subjects[0]?.courseOfferingId ?? null;
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to load skeleton');
        this.skeleton.set(null);
        this.skeletonLoading.set(false);
      },
    });
  }

  protected onSubjectSelect(courseOfferingId: number): void {
    this.activeOfferingId = courseOfferingId;
    this.cancelPlacement();
    this.candidates.set([]);
  }

  /** A (day, period) slot can hold more than one cell — e.g. two parallel Lab batches in
   *  different rooms, a Rotation Group's linked cells, or (now cohort-wide) another subject
   *  entirely — so this returns every cell sharing that slot across the whole cohort. */
  protected cellsFor(day: string, periodId: number): SkeletonCell[] {
    return this.skeleton()?.cells.filter((c) => c.dayOfWeek === day && c.periodId === periodId) ?? [];
  }

  protected subjectColor(courseOfferingId: number): string {
    return colorForSubject(courseOfferingId);
  }

  protected isCandidate(day: string, periodId: number): boolean {
    return this.candidates().some((c) => c.dayOfWeek === day && c.periodId === periodId);
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
    if (cell.isStaffed) return; // read-only once staffed -- edit via Class Schedule screen
    this.confirmRemove(cell);
  }

  /** Opens the placement panel for this slot — always available, even when the slot already
   *  has one or more cells, so a second parallel batch (or another subject) can be added. */
  protected onAddClick(day: string, periodId: number): void {
    if (!this.canManage() || !this.activeOfferingId) return;
    this.selectedCell.set({ day, periodId });
    this.selectedSessionType = 'THEORY';
    this.selectedBatchId = null;
    this.autoSelectSoleSection();
  }

  /** Re-run the auto-select whenever the panel's session-type radio flips (back) to THEORY, so
   *  the trivial single-section case never forces an extra click regardless of which type was
   *  picked first. */
  protected onSessionTypeChange(): void {
    this.selectedBatchId = null;
    this.selectedCohortSectionId = null;
    this.autoSelectSoleSection();
  }

  private autoSelectSoleSection(): void {
    const sections = this.activeSections();
    this.selectedCohortSectionId = sections.length === 1 ? sections[0].id : null;
  }

  protected cancelPlacement(): void {
    this.selectedCell.set(null);
  }

  protected confirmPlacement(): void {
    const cell = this.selectedCell();
    const offeringId = this.activeOfferingId;
    const cohortId = this.selectedCohortId;
    if (!cell || !offeringId || !cohortId) return;
    if (this.needsBatch() && !this.selectedBatchId) {
      this.toast.error('A batch is required for a Lab or Clinical session');
      return;
    }
    if (this.needsSection() && !this.selectedCohortSectionId) {
      this.toast.error('A cohort section is required to place a Theory session for this cohort');
      return;
    }
    this.placing.set(true);
    this.skeletonService.placeCell({
      courseOfferingId: offeringId,
      sessionType: this.selectedSessionType,
      dayOfWeek: cell.day,
      periodId: cell.periodId,
      batchId: this.selectedBatchId,
      cohortId,
      cohortSectionId: this.selectedSessionType === 'THEORY' ? this.selectedCohortSectionId : null,
    }).subscribe({
      next: () => {
        this.toast.success('Placed');
        this.placing.set(false);
        this.cancelPlacement();
        this.candidates.set([]);
        this.reloadSkeleton();
      },
      error: (err) => {
        this.toast.error(this.violationText(err) ?? 'Failed to place session');
        this.placing.set(false);
      },
    });
  }

  /** A placement attempt can fail several independent checks at once (already placed, blocked
   *  period, cohort/elective slot clash, ...) — the backend now reports every one of them
   *  together instead of just the first, so this joins them into one multi-line toast instead of
   *  making the user fix-and-resubmit repeatedly to discover each problem in turn. */
  private violationText(err: any): string | undefined {
    const violations = err?.error?.violations as { message: string }[] | undefined;
    return violations?.length ? violations.map((v) => v.message).join('\n') : err?.error?.message;
  }

  protected onSuggestClick(courseOfferingId: number, sessionType: SkeletonSessionType, batchId: number | null, cohortSectionId: number | null, event: Event): void {
    event.stopPropagation();
    if (!this.canManage()) return;
    this.activeOfferingId = courseOfferingId;
    this.suggesting.set(true);
    this.skeletonService.suggestCandidates(courseOfferingId, sessionType, batchId, cohortSectionId).subscribe({
      next: (candidates) => {
        this.candidates.set(candidates);
        this.suggesting.set(false);
        if (candidates.length === 0) this.toast.error('No free slot could be found — try clearing a blocked period or check faculty/room load elsewhere');
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to load suggestions');
        this.suggesting.set(false);
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

import { Component, HostListener, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CmsFlyoutPanelComponent } from '../../../shared/flyout-panel/flyout-panel.component';
import { ToastService } from '../../../core/toast/toast.service';
import { violationText } from '../../../shared/util/violation-text';
import { SkeletonBuilderService } from './skeleton-builder.service';
import { CohortRoomAllocationService } from '../capacity-planner/cohort-room-allocation.service';
import { CapacityPlannerService } from '../capacity-planner/capacity-planner.service';
import { FacultyOverCapacity, GlobalAutoSchedulePrerequisites, GlobalAutoScheduleResult } from './skeleton-builder.model';

type Step = 'checking-prerequisites' | 'prerequisites-incomplete' | 'ready' | 'running' | 'success' | 'run-failed';

interface CohortNeedingRoom {
  cohortId: number;
  cohortName: string;
}

/**
 * Owns the whole global-auto-schedule interaction end to end: a consolidated prerequisite check on
 * open (offerings without faculty, faculty over capacity, cohorts without a committed room
 * allocation — everything surfaced together as actionable links, not discovered one gate at a
 * time), a confirm-then-run step once every prerequisite is clean, and a best-effort success report
 * (what got placed, what didn't, with a link into Draft Review). Never auto-runs the write call —
 * the admin explicitly confirms, so nothing gets placed/staffed without them seeing "ready" first.
 */
@Component({
  selector: 'app-global-auto-schedule-report-flyout',
  standalone: true,
  imports: [CmsFlyoutPanelComponent, DecimalPipe, RouterLink],
  templateUrl: './global-auto-schedule-report-flyout.component.html',
  styleUrl: './global-auto-schedule-report-flyout.component.scss',
})
export class GlobalAutoScheduleReportFlyoutComponent implements OnInit {
  private readonly skeletonService = inject(SkeletonBuilderService);
  private readonly cohortRoomAllocationService = inject(CohortRoomAllocationService);
  private readonly capacityPlannerService = inject(CapacityPlannerService);
  private readonly toast = inject(ToastService);

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

  protected readonly overCapacityFaculty = computed<FacultyOverCapacity[]>(() => this.prerequisites()?.capacityPrecheck.overCapacityFaculty ?? []);
  protected readonly headerLabel = computed(() => this.cohortId() != null ? `Global Auto-Schedule — ${this.cohortName() ?? ''}` : 'Global Auto-Schedule — All Cohorts');

  /** Required signal inputs aren't guaranteed bound until ngOnInit — reading {@link termInstanceId}
   *  any earlier (e.g. the constructor) throws NG0950. */
  ngOnInit(): void {
    this.runCheckPrerequisites();
  }

  /** "Assign faculty"/"Open Capacity Auto-Plan"/"View full workload" all open a *different* screen
   *  in a new tab — clicking one gives no direct signal here whether the change actually landed.
   *  Rather than leave that ambiguous, re-run the prerequisite check automatically the moment this
   *  tab regains focus (the user just came back from doing something elsewhere), so a resolved
   *  item visibly clears and an unresolved one visibly still shows the same numbers — confirmation
   *  either way, not a guess. Only while actually on the incomplete step; no point re-checking
   *  mid-run or after a run already finished. */
  @HostListener('window:focus')
  protected onWindowFocus(): void {
    if (this.step() === 'prerequisites-incomplete') {
      this.runCheckPrerequisites();
    }
  }

  protected runCheckPrerequisites(): void {
    this.step.set('checking-prerequisites');
    this.skeletonService.checkGlobalAutoPlacePrerequisites(this.termInstanceId(), this.cohortId()).subscribe({
      next: (prereq) => {
        this.prerequisites.set(prereq);
        this.checkRoomCommitStatus(prereq);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to check prerequisites');
        this.closed.emit();
      },
    });
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

  private finishPrerequisiteCheck(prereq: GlobalAutoSchedulePrerequisites): void {
    const ready = prereq.offeringsWithoutFaculty.length === 0
      && prereq.capacityPrecheck.overCapacityFaculty.length === 0
      && this.cohortsNeedingRoom().length === 0;
    this.step.set(ready ? 'ready' : 'prerequisites-incomplete');
  }

  protected runGlobalSchedule(): void {
    this.step.set('running');
    this.runError.set(null);
    this.skeletonService.globalAutoPlace(this.termInstanceId(), this.cohortId()).subscribe({
      next: (result) => {
        this.result.set(result);
        this.step.set('success');
        this.scheduled.emit();
      },
      error: (err) => {
        this.runError.set(violationText(err) ?? 'Failed to run the global auto-scheduler');
        this.step.set('run-failed');
      },
    });
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
      default: return '—';
    }
  }

  protected onClose(): void {
    this.closed.emit();
  }
}

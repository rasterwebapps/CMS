import { Component, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CmsFlyoutPanelComponent } from '../../../shared/flyout-panel/flyout-panel.component';
import { ToastService } from '../../../core/toast/toast.service';
import { violationText } from '../../../shared/util/violation-text';
import { SkeletonBuilderService } from './skeleton-builder.service';
import { FacultyOverCapacity, GlobalAutoScheduleResult } from './skeleton-builder.model';

type Step = 'loading' | 'over-capacity' | 'ready' | 'running' | 'success' | 'run-failed';

/**
 * Owns the whole global-auto-schedule interaction end to end: precheck on open, an over-capacity
 * report with both remediation suggestion types if it fails, a confirm-then-run step if it
 * passes, and a per-cohort success summary. Never auto-runs the write call on a clean precheck —
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
  private readonly toast = inject(ToastService);

  readonly termInstanceId = input.required<number>();
  readonly academicYearId = input.required<number>();

  readonly closed = output<void>();
  /** Emitted only once the write call actually succeeds, so the parent knows to reload. */
  readonly scheduled = output<void>();

  protected readonly step = signal<Step>('loading');
  protected readonly overCapacityFaculty = signal<FacultyOverCapacity[]>([]);
  protected readonly result = signal<GlobalAutoScheduleResult | null>(null);
  protected readonly runError = signal<string | null>(null);

  protected readonly hasOverCapacity = computed(() => this.overCapacityFaculty().length > 0);

  /** Required signal inputs aren't guaranteed bound until ngOnInit — reading {@link termInstanceId}
   *  any earlier (e.g. the constructor) throws NG0950. */
  ngOnInit(): void {
    this.runPrecheck();
  }

  protected runPrecheck(): void {
    this.step.set('loading');
    this.skeletonService.precheckGlobalAutoPlace(this.termInstanceId()).subscribe({
      next: (result) => {
        this.overCapacityFaculty.set(result.overCapacityFaculty);
        this.step.set(result.overCapacityFaculty.length > 0 ? 'over-capacity' : 'ready');
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to check capacity');
        this.closed.emit();
      },
    });
  }

  protected runGlobalSchedule(): void {
    this.step.set('running');
    this.runError.set(null);
    this.skeletonService.globalAutoPlace(this.termInstanceId()).subscribe({
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

  protected onClose(): void {
    this.closed.emit();
  }
}

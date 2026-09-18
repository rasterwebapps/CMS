import { Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear, TermInstance } from '../../academic-year/academic-year.model';
import { TimetableService } from '../timetable.service';
import { ClassSchedule, ClinicalShiftSummaryItem, CohortTermStatusSummary, SwapCandidate, TimetableCoverageGap } from '../timetable.model';
import { ConflictInspectorService } from '../conflict-inspector/conflict-inspector.service';
import { CmsWeekGridComponent } from '../../../shared/week-grid/week-grid.component';
import { WeekGridSession } from '../../../shared/week-grid/week-grid.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CoverageOverrideDialogComponent } from './coverage-override-dialog.component';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';
import { violationText } from '../../../shared/util/violation-text';
import { TourService } from '../../../shared/tour/tour.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TIMETABLE_DRAFT_REVIEW_TOUR, TIMETABLE_DRAFT_REVIEW_FLOW_MAP } from '../../../shared/tour/tours/timetable-draft-review.tours';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';

@Component({
  selector: 'app-timetable-draft-review',
  standalone: true,
  imports: [
    FormsModule, DecimalPipe, RouterLink, MatDialogModule, MatProgressSpinnerModule, CmsWeekGridComponent,
    CmsTourButtonComponent, CmsStatusBadgeComponent, CmsEmptyStateComponent,
  ],
  templateUrl: './timetable-draft-review.component.html',
  styleUrl: './timetable-draft-review.component.scss',
})
export class TimetableDraftReviewComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly timetableService = inject(TimetableService);
  private readonly conflictInspectorService = inject(ConflictInspectorService);
  private readonly permissionService = inject(PermissionService);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly route = inject(ActivatedRoute);
  private readonly tourService = inject(TourService);

  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly termInstances = signal<TermInstance[]>([]);
  protected readonly sessions = signal<ClassSchedule[]>([]);
  protected readonly clinicalShiftSummary = signal<ClinicalShiftSummaryItem[]>([]);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly termsLoading = signal(false);

  /** Draft Review lands on a per-cohort status summary for the selected term; clicking any
   *  cohort row switches to 'grid' -- the existing, unchanged term-wide review grid below (never
   *  cohort-scoped: a TermInstance is shared across every cohort in that academic year+term, so
   *  Publish/Revert/Discard already operate on the whole term instance, not one cohort). */
  protected readonly viewMode = signal<'summary' | 'grid'>('summary');
  protected readonly cohortSummary = signal<CohortTermStatusSummary[]>([]);
  protected readonly summaryLoading = signal(false);

  /** Null while unknown (not yet fetched, or between term switches) so the Publish button stays
   *  disabled by default rather than briefly enabled before the real gate status arrives. */
  protected readonly conflictAcknowledged = signal<boolean | null>(null);

  protected publishDisabledReason(): string | null {
    return this.conflictAcknowledged() === false
      ? 'Run Conflict Inspector and click "Proceed to Review" for this term before it can be published.'
      : null;
  }

  /** Mirrors week-grid's own private `!isEmpty()` check that gates whether its Publish button
   *  renders at all — the gate banner would be misleading shown on a term with nothing to publish
   *  (already published, or no draft placed yet). */
  protected hasUnpublishedDraft(): boolean {
    return this.sessions().some((s) => s.status === 'DRAFT');
  }

  protected readonly swapMode = signal(false);
  protected readonly swapSource = signal<ClassSchedule | null>(null);
  protected readonly candidateCells = signal<SwapCandidate[]>([]);
  protected readonly swapping = signal(false);

  protected selectedAcademicYearId: number | null = null;
  protected selectedTermInstanceId: number | null = null;

  protected canManage(): boolean {
    return this.permissionService.has('TIMETABLE_MANAGE');
  }

  protected canRevert(): boolean {
    return this.permissionService.has('TIMETABLE_DISCARD_PUBLISHED');
  }

  /** Plain methods (not `computed`) since `selectedAcademicYearId`/`selectedTermInstanceId` are
   *  ngModel-bound properties, not signals -- Angular's change detection re-runs these on every
   *  selection change the same way it does for any other template expression. */
  protected selectedYearLabel(): string {
    return this.academicYears().find((y) => y.id === this.selectedAcademicYearId)?.name ?? '';
  }

  protected selectedTermTypeLabel(): string {
    return this.termInstances().find((t) => t.id === this.selectedTermInstanceId)?.termType ?? '';
  }

  ngOnInit(): void {
    this.tourService.register('timetable-draft-review', TIMETABLE_DRAFT_REVIEW_TOUR);
    this.tourService.registerFlowMap('timetable-draft-review', TIMETABLE_DRAFT_REVIEW_FLOW_MAP);

    const qpAcademicYearId = Number(this.route.snapshot.queryParamMap.get('academicYearId')) || null;
    const qpTermInstanceId = Number(this.route.snapshot.queryParamMap.get('termInstanceId')) || null;

    this.academicYearService.getAllAcademicYears().subscribe({
      next: (years) => {
        this.academicYears.set(years);
        const initialYearId = qpAcademicYearId
          ?? years.find((y) => y.isCurrent)?.id
          ?? years[0]?.id
          ?? null;
        if (initialYearId) {
          this.selectedAcademicYearId = initialYearId;
          this.loadTermInstances(initialYearId, qpTermInstanceId ?? undefined);
        }
      },
      error: () => { this.toast.error('Failed to load academic years'); },
    });
  }

  protected onAcademicYearChange(): void {
    this.selectedTermInstanceId = null;
    this.sessions.set([]);
    this.clinicalShiftSummary.set([]);
    this.cohortSummary.set([]);
    this.viewMode.set('summary');
    if (this.selectedAcademicYearId) this.loadTermInstances(this.selectedAcademicYearId);
  }

  protected onTermChange(): void {
    this.viewMode.set('summary');
    if (this.selectedTermInstanceId) this.loadCohortSummary(this.selectedTermInstanceId);
    else this.cohortSummary.set([]);
  }

  /** Deliberately ignores which cohort's row was clicked -- the grid below is term-wide by
   *  design, not cohort-scoped (see the viewMode doc comment above). */
  protected onCohortRowClick(): void {
    if (!this.selectedTermInstanceId) return;
    this.viewMode.set('grid');
    this.loadDraft(this.selectedTermInstanceId);
  }

  protected onBackToSummary(): void {
    this.viewMode.set('summary');
  }

  protected canOverrideIncompleteCoverage(): boolean {
    return this.permissionService.has('TIMETABLE_APPROVE_INCOMPLETE_OVERRIDE');
  }

  protected onApprove(): void {
    if (!this.selectedTermInstanceId) return;
    this.doApprove(false, undefined);
  }

  /** First attempt is always a plain approve (no override) — a coverage gap only surfaces once the
   *  backend actually finds one (OC-256), rather than pre-emptively asking every time. Resubmits
   *  with `overrideIncompleteCoverage=true` only after {@link openCoverageOverrideDialog} returns a
   *  reason. */
  private doApprove(overrideIncompleteCoverage: boolean, overrideReason: string | undefined): void {
    this.saving.set(true);
    this.timetableService.approve(this.selectedTermInstanceId!, overrideIncompleteCoverage, overrideReason).subscribe({
      next: (response) => {
        this.toast.success(`Published ${response.affectedCount} session(s) — timetable is now live`);
        this.loadDraft(this.selectedTermInstanceId!);
        this.loadCohortSummary(this.selectedTermInstanceId!);
        this.saving.set(false);
      },
      error: (err) => {
        this.saving.set(false);
        const gaps = err?.error?.gaps as TimetableCoverageGap[] | undefined;
        if (gaps?.length) {
          if (this.canOverrideIncompleteCoverage()) {
            this.openCoverageOverrideDialog(gaps);
          } else {
            this.toast.error(`${gaps.length} cohort/subject-type combination(s) still have unscheduled curriculum hours — `
              + 'ask an admin with override permission to approve this term.');
          }
          return;
        }
        // Client-side publishDisabledReason() already blocks this in the common case — reachable
        // here only if the acknowledgment went stale between page load and clicking Publish (e.g.
        // someone edited the skeleton in another tab). Re-fetch so the banner/disabled state
        // catches up instead of leaving the button looking enabled after a failed attempt.
        if (err?.error?.code === 'TIMETABLE_CONFLICT_ACKNOWLEDGMENT_REQUIRED') {
          this.loadConflictAcknowledgment(this.selectedTermInstanceId!);
        }
        this.toast.error(violationText(err) ?? 'Failed to approve timetable');
      },
    });
  }

  private openCoverageOverrideDialog(gaps: TimetableCoverageGap[]): void {
    this.dialog.open(CoverageOverrideDialogComponent, { data: { gaps }, width: '520px' })
      .afterClosed().subscribe((reason: string | null) => {
        if (reason) this.doApprove(true, reason);
      });
  }

  protected onRevert(): void {
    if (!this.selectedTermInstanceId) return;
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Revert to Draft',
        message: 'This moves the live, published timetable back to Draft so it can be edited and re-approved. It is hidden from student/faculty timetable views until re-approved. Blocked if any lab attendance has already been recorded against these sessions.',
        confirmText: 'Revert',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) this.doRevert();
    });
  }

  private doRevert(): void {
    this.saving.set(true);
    this.timetableService.revertToDraft(this.selectedTermInstanceId!).subscribe({
      next: (response) => {
        this.toast.success(`Reverted ${response.affectedCount} session(s) to draft`);
        this.loadDraft(this.selectedTermInstanceId!);
        this.loadCohortSummary(this.selectedTermInstanceId!);
        this.saving.set(false);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to revert timetable to draft');
        this.saving.set(false);
      },
    });
  }

  protected onDiscard(): void {
    if (!this.selectedTermInstanceId) return;
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Discard Draft Timetable',
        message: 'This permanently deletes every draft session generated for this term. This cannot be undone.',
        confirmText: 'Discard',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) this.doDiscard();
    });
  }

  private doDiscard(): void {
    this.saving.set(true);
    this.timetableService.clear(this.selectedTermInstanceId!).subscribe({
      next: () => {
        this.toast.success('Draft discarded');
        this.sessions.set([]);
        this.loadCohortSummary(this.selectedTermInstanceId!);
        this.saving.set(false);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to discard draft');
        this.saving.set(false);
      },
    });
  }

  protected onSessionClick(session: WeekGridSession): void {
    if (!this.selectedTermInstanceId || session.status !== 'DRAFT' || !this.permissionService.has('TIMETABLE_SWAP')) {
      return;
    }
    this.swapping.set(true);
    this.timetableService.getSwapCandidates(this.selectedTermInstanceId, session.id).subscribe({
      next: (candidates) => {
        this.swapSource.set(session);
        this.candidateCells.set(candidates);
        this.swapMode.set(true);
        this.swapping.set(false);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to load swap candidates');
        this.swapping.set(false);
      },
    });
  }

  protected onCandidateCellClick(cell: SwapCandidate): void {
    const source = this.swapSource();
    if (!this.selectedTermInstanceId || !source) return;
    this.swapping.set(true);
    this.timetableService.swapSession(this.selectedTermInstanceId, source.id, {
      dayOfWeek: cell.dayOfWeek,
      periodId: cell.periodId,
    }).subscribe({
      next: () => {
        this.toast.success(cell.occupied
          ? `Swapped ${source.subjectName} with ${cell.occupyingSubjectName}`
          : `Moved ${source.subjectName} to ${cell.dayOfWeek}, ${cell.startTime}–${cell.endTime}`);
        this.cancelSwap();
        this.loadDraft(this.selectedTermInstanceId!);
      },
      error: (err) => {
        this.toast.error(violationText(err) ?? 'Failed to swap — this slot may no longer be available');
        this.swapping.set(false);
        // Refresh candidates in case the conflict is stale, rather than leaving a dead list up.
        this.timetableService.getSwapCandidates(this.selectedTermInstanceId!, source.id).subscribe({
          next: (candidates) => this.candidateCells.set(candidates),
        });
      },
    });
  }

  protected onCancelSwap(): void {
    this.cancelSwap();
  }

  private cancelSwap(): void {
    this.swapMode.set(false);
    this.swapSource.set(null);
    this.candidateCells.set([]);
  }

  private loadTermInstances(academicYearId: number, preselectTermInstanceId?: number): void {
    this.termsLoading.set(true);
    this.academicYearService.getTermInstancesByAcademicYear(academicYearId).subscribe({
      next: (terms) => {
        this.termInstances.set(terms);
        this.termsLoading.set(false);
        const preselect = preselectTermInstanceId && terms.some((t) => t.id === preselectTermInstanceId)
          ? preselectTermInstanceId
          : terms[0]?.id ?? null;
        this.selectedTermInstanceId = preselect;
        this.viewMode.set('summary');
        if (preselect) this.loadCohortSummary(preselect);
        else this.cohortSummary.set([]);
      },
      error: () => { this.toast.error('Failed to load term instances'); this.termsLoading.set(false); },
    });
  }

  /** No DRAFT rows can mean either "never generated" or "already approved" — falls back to the
   *  published timetable so an approved term shows its live sessions (with Revert to Draft)
   *  instead of reading as empty. Only called once a cohort row is clicked into the grid — the
   *  landing state is the cohort summary table instead (see {@link loadCohortSummary}). */
  private loadDraft(termInstanceId: number): void {
    this.loading.set(true);
    this.timetableService.getDraft(termInstanceId).subscribe({
      next: (draft) => {
        if (draft.length > 0) {
          this.sessions.set(draft);
          this.loading.set(false);
          return;
        }
        this.timetableService.getPublished(termInstanceId).subscribe({
          next: (published) => { this.sessions.set(published); this.loading.set(false); },
          error: () => { this.sessions.set(draft); this.loading.set(false); },
        });
      },
      error: () => { this.toast.error('Failed to load draft timetable'); this.loading.set(false); },
    });
    this.loadClinicalShiftSummary(termInstanceId);
    this.loadConflictAcknowledgment(termInstanceId);
  }

  /** Silently leaves the gate unknown (Publish stays disabled) on failure rather than surfacing a
   *  second error toast on top of the grid's own — the same tradeoff as
   *  {@link loadClinicalShiftSummary} just above it. */
  private loadConflictAcknowledgment(termInstanceId: number): void {
    this.conflictAcknowledged.set(null);
    this.conflictInspectorService.getAcknowledgmentStatus(termInstanceId).subscribe({
      next: (status) => this.conflictAcknowledged.set(status.acknowledged),
      error: () => this.conflictAcknowledged.set(null),
    });
  }

  // Clinical Shift Group (duty-roster) hours never produce a grid cell (see ClinicalShiftSummaryItem),
  // so this is loaded separately from the grid sessions above rather than derived from them. Silently
  // empty on failure — the banner is a helpful aside, not worth an error toast on top of the grid's own.
  private loadClinicalShiftSummary(termInstanceId: number): void {
    this.timetableService.getClinicalShiftSummary(termInstanceId).subscribe({
      next: (summary) => this.clinicalShiftSummary.set(summary),
      error: () => this.clinicalShiftSummary.set([]),
    });
  }

  private loadCohortSummary(termInstanceId: number): void {
    this.summaryLoading.set(true);
    this.timetableService.getCohortStatusSummary(termInstanceId).subscribe({
      next: (rows) => { this.cohortSummary.set(rows); this.summaryLoading.set(false); },
      error: () => { this.toast.error('Failed to load cohort status summary'); this.summaryLoading.set(false); },
    });
  }
}

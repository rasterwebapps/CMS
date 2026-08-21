import { Component, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Period } from '../../period/period.model';
import { CmsFlyoutPanelComponent } from '../../../shared/flyout-panel/flyout-panel.component';
import { ToastService } from '../../../core/toast/toast.service';
import { violationText } from '../../../shared/util/violation-text';
import { SkeletonBuilderService } from './skeleton-builder.service';
import { ElectiveGroupMemberPlacement, SkeletonBuilderResponse, SkeletonCell, SkeletonSessionType, SkeletonSubject } from './skeleton-builder.model';

/**
 * Places every not-yet-placed member of a term's one elective group at a single shared day/period
 * in one atomic call — the "visually bundle and place at once" affordance Skeleton Builder's
 * per-cell placement flow has no equivalent for. Follows RotationSetupFlyoutComponent's shape
 * (same cms-flyout-panel host, same parent-supplied termInstanceId/periods/days/dayLabels inputs)
 * but needs no extra fetch: everything else it needs (member subjects, already-placed cells,
 * batch/section options) is already sitting in the parent's loaded `skeleton` response.
 */
@Component({
  selector: 'app-elective-slot-block-flyout',
  standalone: true,
  imports: [FormsModule, MatProgressSpinnerModule, CmsFlyoutPanelComponent],
  templateUrl: './elective-slot-block-flyout.component.html',
  styleUrl: './elective-slot-block-flyout.component.scss',
})
export class ElectiveSlotBlockFlyoutComponent implements OnInit {
  private readonly skeletonBuilderService = inject(SkeletonBuilderService);
  private readonly toast = inject(ToastService);

  readonly termInstanceId = input.required<number>();
  readonly cohortId = input.required<number>();
  readonly periods = input.required<Period[]>();
  readonly days = input.required<readonly string[]>();
  readonly dayLabels = input.required<Record<string, string>>();
  readonly skeleton = input.required<SkeletonBuilderResponse>();

  readonly closed = output<void>();
  readonly saved = output<void>();

  protected readonly saving = signal(false);
  protected readonly selections = signal<Record<number, { batchId: number | null; cohortSectionId: number | null }>>({});

  protected selectedDayOfWeek: string | null = null;
  protected selectedPeriodId: number | null = null;

  protected readonly electiveSubjects = computed(() =>
    this.skeleton().subjects.filter((s) => s.electiveGroupId != null));

  protected readonly electiveGroupName = computed(() =>
    this.electiveSubjects()[0]?.electiveGroupName ?? 'Elective Group');

  protected readonly placedCourseOfferingIds = computed(() =>
    new Set(this.skeleton().cells.filter((c) => c.electiveGroupId != null).map((c) => c.courseOfferingId)));

  protected readonly unplacedSubjects = computed(() =>
    this.electiveSubjects().filter((s) => !this.placedCourseOfferingIds().has(s.courseOfferingId)));

  /** Any already-placed cell for this group locks the day/period every further placement must
   *  match — the same rule the backend enforces, surfaced here so the admin isn't offered a
   *  picker that will just bounce with a conflict. Must agree with the backend's own anchor pick
   *  ({@code TimetableSkeletonService.resolveGroupAnchor}, lowest-id/earliest-created) rather than
   *  taking whichever cell happens to sort first in this unordered response list — otherwise the
   *  flyout could lock to (and pre-fill) a slot that isn't actually the one the backend enforces. */
  protected readonly anchorCell = computed(() =>
    this.skeleton().cells
      .filter((c) => c.electiveGroupId != null)
      .reduce<SkeletonCell | null>((min, c) => (min == null || c.id < min.id ? c : min), null));

  protected readonly isLocked = computed(() => this.anchorCell() != null);

  /** Required signal inputs aren't guaranteed bound until ngOnInit — reading {@link skeleton} (via
   *  {@link anchorCell}) any earlier throws NG0950. */
  ngOnInit(): void {
    const anchor = this.anchorCell();
    if (anchor) {
      this.selectedDayOfWeek = anchor.dayOfWeek;
      this.selectedPeriodId = anchor.periodId;
    }
  }

  /** The next session this subject's budget still needs — a subject requiring 2+ sessions/week
   *  gets its first one placed here; any remaining ones go through the normal single-cell flow
   *  afterward (already slot-validated reactively against this group's now-set anchor). */
  protected nextBudgetSessionType(subject: SkeletonSubject): SkeletonSessionType | null {
    const budget = subject.budgets.find((b) => b.placedSessionsPerWeek < b.requiredSessionsPerWeek);
    return budget?.sessionType ?? subject.budgets[0]?.sessionType ?? null;
  }

  protected batchOptionsFor(subject: SkeletonSubject) {
    return this.skeleton().batches.filter((b) => b.courseOfferingId === subject.courseOfferingId);
  }

  protected needsBatch(subject: SkeletonSubject): boolean {
    return this.nextBudgetSessionType(subject) !== 'THEORY';
  }

  protected needsSection(subject: SkeletonSubject): boolean {
    return this.nextBudgetSessionType(subject) === 'THEORY' && this.skeleton().sections.length > 0;
  }

  protected updateSelection(courseOfferingId: number, field: 'batchId' | 'cohortSectionId', value: number | null): void {
    const next = { ...this.selections() };
    const current = next[courseOfferingId] ?? { batchId: null, cohortSectionId: null };
    next[courseOfferingId] = { ...current, [field]: value };
    this.selections.set(next);
  }

  protected canSubmit(): boolean {
    if (!this.selectedDayOfWeek || !this.selectedPeriodId || this.unplacedSubjects().length === 0) return false;
    for (const subject of this.unplacedSubjects()) {
      const selection = this.selections()[subject.courseOfferingId];
      if (this.needsBatch(subject) && !selection?.batchId) return false;
      if (this.needsSection(subject) && !selection?.cohortSectionId) return false;
    }
    return true;
  }

  protected submit(): void {
    if (!this.canSubmit()) return;
    const members: ElectiveGroupMemberPlacement[] = this.unplacedSubjects().map((subject) => {
      const selection = this.selections()[subject.courseOfferingId];
      return {
        courseOfferingId: subject.courseOfferingId,
        sessionType: this.nextBudgetSessionType(subject)!,
        batchId: this.needsBatch(subject) ? selection?.batchId ?? null : null,
        cohortSectionId: this.needsSection(subject) ? selection?.cohortSectionId ?? null : null,
      };
    });

    this.saving.set(true);
    this.skeletonBuilderService.placeElectiveGroup({
      electiveGroupId: this.electiveSubjects()[0].electiveGroupId!,
      termInstanceId: this.termInstanceId(),
      cohortId: this.cohortId(),
      dayOfWeek: this.selectedDayOfWeek!,
      periodId: this.selectedPeriodId!,
      members,
    }).subscribe({
      next: () => {
        this.toast.success('Elective group placed');
        this.saving.set(false);
        this.saved.emit();
      },
      error: (err) => {
        this.toast.error(violationText(err) ?? 'Failed to place elective group');
        this.saving.set(false);
      },
    });
  }

  protected onClose(): void {
    this.closed.emit();
  }
}

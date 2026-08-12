import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear, TermInstance } from '../../academic-year/academic-year.model';
import { TimetableService } from '../timetable.service';
import { ClassSchedule, SwapCandidate } from '../timetable.model';
import { CmsWeekGridComponent } from '../../../shared/week-grid/week-grid.component';
import { WeekGridSession } from '../../../shared/week-grid/week-grid.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';
import { violationText } from '../../../shared/util/violation-text';

@Component({
  selector: 'app-timetable-draft-review',
  standalone: true,
  imports: [FormsModule, MatDialogModule, MatProgressSpinnerModule, CmsWeekGridComponent],
  templateUrl: './timetable-draft-review.component.html',
  styleUrl: './timetable-draft-review.component.scss',
})
export class TimetableDraftReviewComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly timetableService = inject(TimetableService);
  private readonly permissionService = inject(PermissionService);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly route = inject(ActivatedRoute);

  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly termInstances = signal<TermInstance[]>([]);
  protected readonly sessions = signal<ClassSchedule[]>([]);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly termsLoading = signal(false);

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

  ngOnInit(): void {
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
    if (this.selectedAcademicYearId) this.loadTermInstances(this.selectedAcademicYearId);
  }

  protected onTermChange(): void {
    if (this.selectedTermInstanceId) this.loadDraft(this.selectedTermInstanceId);
    else this.sessions.set([]);
  }

  protected onApprove(): void {
    if (!this.selectedTermInstanceId) return;
    this.saving.set(true);
    this.timetableService.approve(this.selectedTermInstanceId).subscribe({
      next: (response) => {
        this.toast.success(`Approved ${response.affectedCount} session(s) — timetable is now live`);
        this.loadDraft(this.selectedTermInstanceId!);
        this.saving.set(false);
      },
      error: (err) => {
        this.toast.error(violationText(err) ?? 'Failed to approve timetable');
        this.saving.set(false);
      },
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
        this.toast.error(err?.error?.message ?? 'Failed to swap — this slot may no longer be available');
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
        if (preselect) this.loadDraft(preselect);
        else this.sessions.set([]);
      },
      error: () => { this.toast.error('Failed to load term instances'); this.termsLoading.set(false); },
    });
  }

  /** No DRAFT rows can mean either "never generated" or "already approved" — falls back to the
   *  published timetable so an approved term shows its live sessions (with Revert to Draft)
   *  instead of reading as empty. */
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
  }
}

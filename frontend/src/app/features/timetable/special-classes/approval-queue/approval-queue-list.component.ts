import { Component, OnDestroy, OnInit, ViewChild, inject, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { Observable, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { MatTableModule, MatTableDataSource, MatTable } from '@angular/material/table';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { CmsEmptyStateComponent } from '../../../../shared/empty-state/empty-state.component';
import { CmsRowActionButtonComponent } from '../../../../shared/row-action-button/row-action-button.component';
import { CmsFlyoutPanelComponent } from '../../../../shared/flyout-panel/flyout-panel.component';
import { CmsStatusBadgeComponent } from '../../../../shared/status-badge/status-badge.component';
import { CmsInfiniteSelectComponent } from '../../../../shared/infinite-select/infinite-select.component';
import { InfiniteSelectValue } from '../../../../shared/infinite-select/infinite-select.model';
import { staticOptionsFetchPage } from '../../../../shared/infinite-select/infinite-select.utils';
import { ConfirmDialogComponent } from '../../../../shared/confirm-dialog/confirm-dialog.component';
import { ToastService } from '../../../../core/toast/toast.service';
import { PermissionService } from '../../../../core/permissions/permission.service';
import { SpecialClassService, SpecialClassSearchFilter } from '../special-class.service';
import { SpecialClassApprovalStatus, SpecialClassOccurrence } from '../special-class.model';
import { AcademicYearService } from '../../../academic-year/academic-year.service';
import { CohortSummary } from '../../../academic-year/academic-year.model';
import { FacultyService } from '../../../faculty/faculty.service';
import { Faculty } from '../../../faculty/faculty.model';
import { SubjectService } from '../../../subject/subject.service';
import { Subject as SubjectModel } from '../../../subject/subject.model';
import { TourService } from '../../../../shared/tour/tour.service';
import { CmsTourButtonComponent } from '../../../../shared/tour/tour-button.component';
import { SPECIAL_CLASS_APPROVALS_TOUR, SPECIAL_CLASS_APPROVALS_FLOW_MAP } from '../../../../shared/tour/tours/special-class.tours';

function todayIso(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/** BR-55 — admin-facing Special Class Approvals screen. Originally a pending-only queue; now the
 *  full request history (pending/approved/rejected/cancelled), filterable by status/faculty/
 *  date-range/subject/cohort/free-text, defaulting to PENDING on first load so the original
 *  approval-queue behavior is unchanged until an admin widens the filter. Approve/Reject stay
 *  inline per row via the shared cms-flyout-panel for the reject reason, disabled once a row's
 *  date has passed (also hard-gated backend-side — see SpecialClassRequestService
 *  .requireNotPastDate) and hidden entirely from anyone without TIMETABLE_SPECIAL_CLASS_APPROVE.
 *  A DAY_REPEAT/RECURRING row's approve/reject acts on its whole request_batch_id, not just that
 *  one row. */
@Component({
  selector: 'app-approval-queue-list',
  standalone: true,
  imports: [
    FormsModule, DatePipe, MatTableModule, MatSortModule, MatPaginatorModule, MatProgressSpinnerModule, MatDialogModule,
    CmsEmptyStateComponent, CmsRowActionButtonComponent, CmsFlyoutPanelComponent, CmsStatusBadgeComponent,
    CmsInfiniteSelectComponent, CmsTourButtonComponent,
  ],
  templateUrl: './approval-queue-list.component.html',
  styleUrl: './approval-queue-list.component.scss',
})
export class ApprovalQueueListComponent implements OnInit, OnDestroy {
  private readonly specialClassService = inject(SpecialClassService);
  private readonly academicYearService = inject(AcademicYearService);
  private readonly facultyService = inject(FacultyService);
  private readonly subjectService = inject(SubjectService);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly tourService = inject(TourService);
  private readonly permissionService = inject(PermissionService);

  @ViewChild(MatTable) private _matTable?: MatTable<unknown>;
  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  protected readonly canApprove = computed(() => this.permissionService.has('TIMETABLE_SPECIAL_CLASS_APPROVE'));

  protected readonly displayedColumns = ['requestedAt', 'occurrenceDate', 'subjectName', 'periodName', 'venueName', 'requestedFacultyName', 'requestedByFacultyName', 'approvalStatus', 'actions'];
  protected readonly dataSource = new MatTableDataSource<SpecialClassOccurrence>([]);
  /** Mirrors dataSource.data as a signal -- MatTableDataSource.data isn't reactive, but isPastDate
   *  needs to look across every row of a batch, not just the clicked one (approveBatch/rejectBatch
   *  act on the whole request_batch_id at once, so a RECURRING_SPECIAL_CLASS batch spanning several
   *  weeks can have some rows still upcoming and one already past -- the button must be disabled for
   *  every row in that batch, not just the past one, since clicking any of them would still hit the
   *  backend's whole-batch past-date guard). */
  protected readonly rows = signal<SpecialClassOccurrence[]>([]);
  protected readonly loading = signal(false);
  protected readonly acting = signal(false);

  protected readonly rejectTarget = signal<SpecialClassOccurrence | null>(null);
  protected rejectionReason = '';

  // ── Filters — status defaults to PENDING so first load matches the screen's original
  // approval-queue behavior; an admin widens it to see approved/rejected/cancelled history, or
  // picks "All Statuses" to see everything at once (sent as no status param at all). ──
  protected readonly selectedStatus = signal<string>('PENDING');
  protected readonly selectedFacultyId = signal<number | null>(null);
  protected readonly selectedSubjectId = signal<number | null>(null);
  protected readonly selectedCohortId = signal<number | null>(null);
  protected readonly dateFrom = signal('');
  protected readonly dateTo = signal('');
  protected readonly searchValue = signal('');

  private readonly faculty = signal<Faculty[]>([]);
  private readonly subjects = signal<SubjectModel[]>([]);
  private readonly cohorts = signal<CohortSummary[]>([]);

  protected readonly statusFetchPage = staticOptionsFetchPage(() => ([
    { id: 'ALL', name: 'All Statuses' },
    { id: 'PENDING', name: 'Pending' },
    { id: 'APPROVED', name: 'Approved' },
    { id: 'REJECTED', name: 'Rejected' },
    { id: 'CANCELLED', name: 'Cancelled' },
  ]));
  protected readonly facultyFetchPage = staticOptionsFetchPage(() =>
    this.faculty().map(f => ({ id: f.id, name: f.fullName })));
  protected readonly subjectFetchPage = staticOptionsFetchPage(() =>
    this.subjects().map(s => ({ id: s.id, name: `${s.name} (${s.code})` })));
  protected readonly cohortFetchPage = staticOptionsFetchPage(() =>
    this.cohorts().map(c => ({ id: c.id, name: c.displayName })));

  protected readonly hasActiveFilters = computed(() =>
    this.selectedStatus() !== 'PENDING' || this.selectedFacultyId() != null || this.selectedSubjectId() != null
    || this.selectedCohortId() != null || !!this.dateFrom() || !!this.dateTo() || !!this.searchValue());

  private readonly destroy$ = new Subject<void>();
  private readonly searchSubject = new Subject<string>();

  ngOnInit(): void {
    this.tourService.register('special-class-approvals', SPECIAL_CLASS_APPROVALS_TOUR);
    this.tourService.registerFlowMap('special-class-approvals', SPECIAL_CLASS_APPROVALS_FLOW_MAP);

    this.searchSubject.pipe(
      debounceTime(400), distinctUntilChanged(), takeUntil(this.destroy$),
    ).subscribe(() => this.load());

    this.facultyService.getAll().subscribe({ next: (data) => this.faculty.set(data) });
    this.subjectService.getAll(true).subscribe({ next: (data) => this.subjects.set(data) });
    this.academicYearService.getAllCohorts().subscribe({ next: (data) => this.cohorts.set(data) });

    this.load();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private load(): void {
    this.loading.set(true);
    const filter: SpecialClassSearchFilter = {
      status: this.selectedStatus() === 'ALL' ? null : (this.selectedStatus() as SpecialClassApprovalStatus),
      facultyId: this.selectedFacultyId(),
      subjectId: this.selectedSubjectId(),
      cohortId: this.selectedCohortId(),
      dateFrom: this.dateFrom() || null,
      dateTo: this.dateTo() || null,
      search: this.searchValue().trim() || null,
    };
    this.specialClassService.approvalQueue(filter).subscribe({
      next: (data) => { this.rows.set(data); this.dataSource.data = data; this.loading.set(false); },
      error: () => { this.toast.error('Failed to load special class requests'); this.loading.set(false); },
    });
  }

  protected onSearch(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchValue.set(value);
    this.searchSubject.next(value);
  }

  protected clearSearch(): void {
    this.searchValue.set('');
    this.searchSubject.next('');
  }

  protected onStatusFilterChange(value: InfiniteSelectValue | null): void {
    this.selectedStatus.set(value != null ? String(value) : 'PENDING');
    this.load();
  }

  protected onFacultyFilterChange(value: InfiniteSelectValue | null): void {
    this.selectedFacultyId.set(value != null ? Number(value) : null);
    this.load();
  }

  protected onSubjectFilterChange(value: InfiniteSelectValue | null): void {
    this.selectedSubjectId.set(value != null ? Number(value) : null);
    this.load();
  }

  protected onCohortFilterChange(value: InfiniteSelectValue | null): void {
    this.selectedCohortId.set(value != null ? Number(value) : null);
    this.load();
  }

  protected onDateFromChange(value: string): void {
    this.dateFrom.set(value || '');
    this.load();
  }

  protected onDateToChange(value: string): void {
    this.dateTo.set(value || '');
    this.load();
  }

  protected clearFilters(): void {
    this.selectedStatus.set('PENDING');
    this.selectedFacultyId.set(null);
    this.selectedSubjectId.set(null);
    this.selectedCohortId.set(null);
    this.dateFrom.set('');
    this.dateTo.set('');
    this.searchValue.set('');
    this.load();
  }

  /** A request's date has already passed -- backend hard-gates this too (see
   *  SpecialClassRequestService.requireNotPastDate), this just keeps the buttons from ever
   *  offering an action that would be rejected anyway. Also blocks every other row sharing the
   *  same requestBatchId: approve/reject act on the whole batch at once, and the backend's guard
   *  checks every row in it, so a RECURRING_SPECIAL_CLASS batch with one already-past week blocks
   *  the whole batch, not just that one row. */
  protected isPastDate(row: SpecialClassOccurrence): boolean {
    const today = todayIso();
    if (row.occurrenceDate < today) return true;
    if (!row.requestBatchId) return false;
    return this.rows().some(r => r.requestBatchId === row.requestBatchId && r.occurrenceDate < today);
  }

  protected confirmApprove(row: SpecialClassOccurrence): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Approve Special Class',
        message: row.requestBatchId
          ? row.occurrenceSource === 'DAY_REPEAT'
            ? `Approve every session in this day-repeat batch (starting with ${row.subjectName} on ${row.occurrenceDate})?`
            : row.occurrenceSource === 'RECURRING_SPECIAL_CLASS'
              ? `Approve every weekly occurrence in this recurring request for ${row.subjectName} (starting ${row.occurrenceDate})?`
              : `Approve every period in this multi-period request for ${row.subjectName} on ${row.occurrenceDate}?`
          : `Approve the special class for ${row.subjectName} on ${row.occurrenceDate}?`,
        confirmText: 'Approve',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) this.approve(row);
    });
  }

  private approve(row: SpecialClassOccurrence): void {
    this.acting.set(true);
    const request$: Observable<unknown> = row.requestBatchId
      ? this.specialClassService.approveBatch(row.requestBatchId)
      : this.specialClassService.approve(row.id);
    request$.subscribe({
      next: () => { this.toast.success('Approved'); this.acting.set(false); this.load(); },
      error: (err: any) => { this.toast.error(err?.error?.message ?? 'Failed to approve'); this.acting.set(false); },
    });
  }

  protected startReject(row: SpecialClassOccurrence): void {
    this.rejectionReason = '';
    this.rejectTarget.set(row);
  }

  protected closeRejectPanel(): void {
    this.rejectTarget.set(null);
  }

  protected confirmReject(): void {
    const row = this.rejectTarget();
    if (!row || !this.rejectionReason.trim()) return;
    this.acting.set(true);
    const request$: Observable<unknown> = row.requestBatchId
      ? this.specialClassService.rejectBatch(row.requestBatchId, this.rejectionReason)
      : this.specialClassService.reject(row.id, this.rejectionReason);
    request$.subscribe({
      next: () => { this.toast.success('Rejected'); this.acting.set(false); this.rejectTarget.set(null); this.load(); },
      error: (err: any) => { this.toast.error(err?.error?.message ?? 'Failed to reject'); this.acting.set(false); },
    });
  }
}

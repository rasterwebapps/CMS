import { Component, OnDestroy, OnInit, ViewChild, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource, MatTable } from '@angular/material/table';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { FACULTY_ABSENCE_TOUR, FACULTY_ABSENCE_FLOW_MAP } from '../../../shared/tour/tours/faculty-absence.tours';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';
import { FacultyAbsenceService } from '../faculty-absence.service';
import { FacultyAbsenceListItem } from '../faculty-absence.model';
import { MarkAbsenceFlyoutComponent } from '../mark-absence-flyout/mark-absence-flyout.component';

const DEFAULT_PAGE_SIZE = 25;
const today = (): string => new Date().toISOString().slice(0, 10);

/** List-by-default replacement for what used to be the whole /faculty-absence screen (a bare
 *  mark-absent form) — "who's absent on date X" had no answer anywhere in the app before this.
 *  Marking a new absence, and re-opening an already-marked one to find/apply a substitute, both
 *  happen in MarkAbsenceFlyoutComponent over this list rather than as the route itself. */
@Component({
  selector: 'app-faculty-absence-list',
  standalone: true,
  imports: [
    FormsModule, MatTableModule, MatSortModule, MatPaginatorModule, MatProgressSpinnerModule,
    CmsEmptyStateComponent, CmsRowActionButtonComponent, CmsTourButtonComponent, MarkAbsenceFlyoutComponent,
  ],
  templateUrl: './faculty-absence-list.component.html',
  styleUrl: './faculty-absence-list.component.scss',
})
export class FacultyAbsenceListComponent implements OnInit, OnDestroy {
  private readonly absenceService = inject(FacultyAbsenceService);
  private readonly toast = inject(ToastService);
  private readonly tourService = inject(TourService);
  protected readonly permissions = inject(PermissionService);

  private readonly destroy$ = new Subject<void>();
  private readonly searchSubject = new Subject<string>();

  @ViewChild(MatTable) private _matTable?: MatTable<unknown>;
  private _paginator?: MatPaginator;
  private _paginatorSub?: Subscription;
  @ViewChild(MatPaginator) set paginatorRef(p: MatPaginator | undefined) {
    if (!p || p === this._paginator) return;
    this._paginatorSub?.unsubscribe();
    this._paginator = p;
    p.pageIndex = this.currentPage;
    p.pageSize = this.currentPageSize;
    this._paginatorSub = p.page.pipe(takeUntil(this.destroy$)).subscribe((e: PageEvent) => {
      this.currentPage = e.pageIndex;
      this.currentPageSize = e.pageSize;
      this.load();
    });
  }

  // Only real, directly-queryable FacultyAbsence columns get mat-sort-header (absenceDate,
  // reason, recordedBy) — facultyName is a derived getter and specialityName/coverage come from a
  // joined relation / computed-in-service value, none of which Spring Data can sort by directly.
  protected readonly displayedColumns =
    ['facultyName', 'specialityName', 'absenceDate', 'reason', 'recordedBy', 'coverage', 'actions'];
  protected readonly dataSource = new MatTableDataSource<FacultyAbsenceListItem>([]);
  protected readonly loading = signal(false);
  protected totalElements = 0;

  protected currentPage = 0;
  protected currentPageSize = DEFAULT_PAGE_SIZE;
  protected sortActive = 'absenceDate';
  protected sortDirection: 'asc' | 'desc' = 'desc';

  protected readonly searchValue = signal('');
  // Defaults to today→today so the list answers "who's absent today" on load, per the question
  // that motivated this screen — widen or clear either side to see other days.
  protected readonly fromDate = signal<string | null>(today());
  protected readonly toDate = signal<string | null>(today());
  protected readonly substituteFilter = signal<'' | 'true' | 'false'>('');

  protected readonly canMark = computed(() => this.permissions.has('FACULTY_ABSENCE_MARK'));

  protected readonly showFlyout = signal(false);
  protected readonly flyoutAbsenceId = signal<number | null>(null);

  ngOnInit(): void {
    this.tourService.register('faculty-absence', FACULTY_ABSENCE_TOUR);
    this.tourService.registerFlowMap('faculty-absence', FACULTY_ABSENCE_FLOW_MAP);

    this.searchSubject.pipe(debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => { this.currentPage = 0; this.load(); });

    this.load();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
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

  protected onFromDateChange(value: string): void {
    this.fromDate.set(value || null);
    this.currentPage = 0;
    this.load();
  }

  protected onToDateChange(value: string): void {
    this.toDate.set(value || null);
    this.currentPage = 0;
    this.load();
  }

  protected onSubstituteFilterChange(value: string): void {
    this.substituteFilter.set(value as '' | 'true' | 'false');
    this.currentPage = 0;
    this.load();
  }

  protected onSortChange(sort: Sort): void {
    this.sortActive = sort.active || 'absenceDate';
    this.sortDirection = (sort.direction || 'desc') as 'asc' | 'desc';
    this.currentPage = 0;
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    const raw = this.substituteFilter();
    const substituteApplied = raw === '' ? null : raw === 'true';
    this.absenceService.getPage(
      { fromDate: this.fromDate(), toDate: this.toDate(), facultyName: this.searchValue() || null, substituteApplied },
      this.currentPage, this.currentPageSize, this.sortActive, this.sortDirection,
    ).subscribe({
      next: (page) => {
        this.dataSource.data = page.content;
        this.totalElements = page.totalElements;
        if (this._paginator) { this._paginator.length = page.totalElements; this._paginator.pageIndex = page.number; }
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load faculty absences'); this.loading.set(false); },
    });
  }

  protected openAddFlyout(): void {
    this.flyoutAbsenceId.set(null);
    this.showFlyout.set(true);
  }

  protected openRow(row: FacultyAbsenceListItem): void {
    this.flyoutAbsenceId.set(row.id);
    this.showFlyout.set(true);
  }

  protected onFlyoutClosed(): void {
    this.showFlyout.set(false);
  }

  protected onFlyoutSaved(): void {
    this.load();
  }
}

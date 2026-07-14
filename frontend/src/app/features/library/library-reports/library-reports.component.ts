import { Component, computed, inject, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource, MatTable } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatIconModule } from '@angular/material/icon';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { CmsTypeBadgeComponent } from '../../../shared/type-badge/type-badge.component';
import { LibraryService } from '../library.service';
import { LibraryIssue, LibraryMemberType } from '../library.model';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ExportButtonComponent, ExportFormat } from '../../../shared/export-button';

@Component({
  selector: 'app-library-reports',
  standalone: true,
  imports: [
    DatePipe, FormsModule,
    MatTableModule, MatPaginatorModule, MatSortModule, MatIconModule,
    CmsTypeBadgeComponent, CmsEmptyStateComponent, ExportButtonComponent,
  ],
  templateUrl: './library-reports.component.html',
  styleUrl:    './library-reports.component.scss',
})
export class LibraryReportsComponent implements OnInit, OnDestroy {
  private readonly libraryService = inject(LibraryService);
  private readonly toast          = inject(ToastService);
  protected readonly permissions  = inject(PermissionService);

  private readonly destroy$      = new Subject<void>();
  private readonly searchSubject = new Subject<string>();
  private _paginator?: MatPaginator;
  private _paginatorSub?: Subscription;

  @ViewChild(MatTable) private _matTable?: MatTable<unknown>;
  @ViewChild(MatPaginator) set paginatorRef(p: MatPaginator | undefined) {
    if (!p || p === this._paginator) return;
    this._paginatorSub?.unsubscribe();
    this._paginator = p;
    p.pageIndex = this.currentPage;
    p.pageSize  = this.currentPageSize;
    this._paginatorSub = p.page.pipe(takeUntil(this.destroy$)).subscribe((e: PageEvent) => {
      this.currentPage = e.pageIndex;
      this.currentPageSize = e.pageSize;
      this.loadPage();
    });
  }

  protected readonly displayedColumns = [
    'accessionNumber', 'itemTitle', 'memberName', 'dueDate', 'overdueDays', 'estFine',
  ];
  protected readonly dataSource   = new MatTableDataSource<LibraryIssue>([]);
  protected readonly loading      = signal(false);
  protected readonly exporting    = signal(false);
  protected readonly searchValue  = signal('');
  protected readonly memberFilter = signal<LibraryMemberType | null>(null);
  protected readonly canExport    = computed(() => this.permissions.hasAny('LIBRARY_REPORT_EXPORT'));
  protected readonly hasActiveFilters = () =>
    this.memberFilter() !== null || this.searchValue().length > 0;

  protected totalElements   = 0;
  protected currentPage     = 0;
  protected currentPageSize = 25;
  protected sortActive      = 'dueDate';
  protected sortDirection: 'asc' | 'desc' = 'asc';

  ngOnInit(): void {
    this.searchSubject.pipe(debounceTime(400), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => { this.currentPage = 0; this.loadPage(); });
    this.loadPage();
  }

  ngOnDestroy(): void {
    this.destroy$.next(); this.destroy$.complete();
    this._paginatorSub?.unsubscribe();
  }

  protected applyFilter(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchValue.set(value);
    this.searchSubject.next(value);
  }

  protected onFilterChange(): void { this.currentPage = 0; this.loadPage(); }

  protected clearFilters(): void {
    this.searchValue.set(''); this.memberFilter.set(null);
    this.currentPage = 0; this.loadPage();
  }

  protected onSortChange(sort: Sort): void {
    this.sortActive = sort.active;
    this.sortDirection = sort.direction as 'asc' | 'desc';
    this.currentPage = 0; this.loadPage();
  }

  protected onExport(format: ExportFormat): void {
    if (this.exporting()) return;
    this.exporting.set(true);
    this.libraryService.exportOverdue(format, {
      search: this.searchValue() || undefined,
      memberType: this.memberFilter(),
      sort: this.sortActive,
      direction: this.sortDirection,
    }).subscribe({
      next: blob => {
        const ext = format === 'pdf' ? 'pdf' : 'xlsx';
        const filename = `overdue-books-${new Date().toISOString().slice(0, 10)}.${ext}`;
        const url = URL.createObjectURL(blob);
        const a = Object.assign(document.createElement('a'), { href: url, download: filename });
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        this.exporting.set(false);
      },
      error: () => {
        this.toast.error('Export failed. Please try again.');
        this.exporting.set(false);
      },
    });
  }

  protected memberName(issue: LibraryIssue): string {
    return issue.memberType === 'STUDENT'
      ? `${issue.studentName ?? '—'} (${issue.studentRollNumber ?? ''})`
      : `${issue.facultyName ?? '—'} (${issue.facultyEmployeeCode ?? ''})`;
  }

  private loadPage(): void {
    this.loading.set(true);
    this.libraryService.getOverduePage({
      search: this.searchValue() || undefined,
      memberType: this.memberFilter(),
      page: this.currentPage, size: this.currentPageSize,
      sort: this.sortActive, direction: this.sortDirection,
    }).subscribe({
      next: page => {
        this.dataSource.data = page.content;
        this.totalElements = page.totalElements;
        if (this._paginator) { this._paginator.length = page.totalElements; this._paginator.pageIndex = page.number; }
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load overdue books'); this.loading.set(false); },
    });
  }
}

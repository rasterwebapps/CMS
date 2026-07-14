import { Component, computed, inject, OnInit, OnDestroy, AfterViewInit, signal, ViewChild } from '@angular/core';
import { ExportFormat } from '../../../shared/export-button/export-button.component';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource, MatTable } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { ExportButtonComponent } from '../../../shared/export-button/export-button.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { ToastService } from '../../../core/toast/toast.service';
import { PermissionService } from '../../../core/permissions/permission.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { SCHOLARSHIP_TYPE_LIST_TOUR } from '../../../shared/tour/tours/scholarship.tours';
import { ScholarshipType } from '../scholarship.model';
import { ScholarshipService } from '../scholarship.service';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsIconEditComponent, CmsIconToggleStatusComponent } from '../../../shared/icons';
import { ColumnPickerState, CmsColumnPickerComponent } from '../../../shared/column-picker';

import { ColumnResizeDirective, CmsWrapTextToggleComponent } from '../../../shared/column-resize';

@Component({
  selector: 'app-scholarship-type-list',
  standalone: true,
  imports: [
    RouterLink,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatDialogModule,
    MatIconModule,
    InrPipe,
    CmsEmptyStateComponent,
    ExportButtonComponent,
    CmsStatusBadgeComponent,
    CmsTourButtonComponent,
    CmsRowActionButtonComponent,
    CmsIconEditComponent,
    CmsIconToggleStatusComponent,
    CmsColumnPickerComponent, ColumnResizeDirective, CmsWrapTextToggleComponent,
  ],
  templateUrl: './scholarship-type-list.component.html',
  styleUrl: './scholarship-type-list.component.scss',
})
export class ScholarshipTypeListComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly scholarshipService = inject(ScholarshipService);
  private readonly router             = inject(Router);
  private readonly route              = inject(ActivatedRoute);
  private readonly toast              = inject(ToastService);
  private readonly dialog             = inject(MatDialog);
  private readonly tourService        = inject(TourService);
  private readonly permissionService  = inject(PermissionService);

  private readonly destroy$ = new Subject<void>();
  private readonly searchSubject = new Subject<string>();
  private _paginator?: MatPaginator;
  private _paginatorSub?: Subscription;

  @ViewChild(MatTable) private _matTable?: MatTable<unknown>;
  @ViewChild(MatPaginator) set paginatorRef(p: MatPaginator | undefined) {
    if (!p || p === this._paginator) return;
    this._paginatorSub?.unsubscribe();
    this._paginator = p;
    p.pageIndex = this.currentPage;
    p.pageSize = this.currentPageSize;
    this._paginatorSub = p.page.pipe(takeUntil(this.destroy$)).subscribe((e: PageEvent) => {
      this.currentPage = e.pageIndex;
      this.currentPageSize = e.pageSize;
      this.loadPage();
    });
  }

  protected readonly colState = new ColumnPickerState({
    storageKey: 'scholarship-type-columns',
    columns: [
      { key: 'code',            label: 'Code',     mandatory: true },
      { key: 'name',            label: 'Name',     mandatory: true },
      { key: 'discountType',    label: 'Type' },
      { key: 'discountValue',   label: 'Value' },
      { key: 'renewalRequired', label: 'Renewal' },
      { key: 'active',          label: 'Status' },
      { key: 'actions',         label: 'Actions', mandatory: true, pinnable: false },
    ],
  });
  protected readonly displayedColumns = computed(() => this.colState.visibleColumns());
  protected readonly dataSource  = new MatTableDataSource<ScholarshipType>([]);
  protected readonly loading     = signal(false);
  protected readonly exporting   = signal(false);
  protected readonly canExport   = computed(() => this.permissionService.has('SCHOLARSHIP_EXPORT'));
  protected readonly searchValue = signal('');

  protected totalElements = 0;
  protected currentPage = 0;
  protected currentPageSize = 25;
  protected sortActive = 'name';
  protected sortDirection: 'asc' | 'desc' = 'asc';
  private readonly sortMap: Record<string, string> = {
    name: 'name', code: 'code', active: 'active',
  };

  protected onPinChange(): void { this._matTable?.updateStickyColumnStyles(); }
  ngOnInit(): void {
    this.tourService.register('scholarship-type-list', SCHOLARSHIP_TYPE_LIST_TOUR);
    const snap = this.route.snapshot.queryParams;
    if (snap['sortField']) this.sortActive    = snap['sortField'];
    if (snap['sortDir'])   this.sortDirection = snap['sortDir'] as 'asc' | 'desc';
    this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      takeUntil(this.destroy$),
    ).subscribe(() => {
      this.currentPage = 0;
      this.loadPage();
    });
    this.loadPage();
  }

  ngAfterViewInit(): void {}

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this._paginatorSub?.unsubscribe();
  }

  protected applySearch(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchValue.set(value);
    this.searchSubject.next(value);
  }

  protected clearSearch(): void {
    this.searchValue.set('');
    this.searchSubject.next('');
  }

  protected onSort(sort: Sort): void {
    this.sortActive    = sort.active;
    this.sortDirection = (sort.direction || 'asc') as 'asc' | 'desc';
    this.currentPage   = 0;
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { sortField: sort.active, sortDir: this.sortDirection },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
    this.loadPage();
  }

  protected edit(row: ScholarshipType): void {
    void this.router.navigate(['/scholarships', row.id, 'edit']);
  }

  protected handleEmptyAction(): void {
    if (this.searchValue()) {
      this.clearSearch();
    } else {
      void this.router.navigate(['/scholarships/new']);
    }
  }

  protected toggleStatus(row: ScholarshipType): void {
    const nextAction = row.active ? 'Deactivate' : 'Activate';
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: `${nextAction} Scholarship`,
        message: `${nextAction} "${row.name}"?`,
        confirmText: nextAction,
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      const request$ = row.active
        ? this.scholarshipService.deactivateScholarshipType(row.id)
        : this.scholarshipService.reactivateScholarshipType(row.id);
      request$.subscribe({
        next: () => {
          this.toast.success(`Scholarship ${row.active ? 'deactivated' : 'activated'}`);
          this.loadPage();
        },
        error: (err) => this.toast.error(
          err?.error?.message ?? `Failed to ${row.active ? 'deactivate' : 'activate'} scholarship`,
        ),
      });
    });
  }

  protected onExport(format: ExportFormat): void {
    if (this.exporting()) return;
    if (this.totalElements === 0) {
      this.toast.error('No data available to export.');
      return;
    }
    this.exporting.set(true);
    this.scholarshipService.exportScholarshipTypes(format, {
      search: this.searchValue().trim() || null,
      sort: this.sortMap[this.sortActive] ?? this.sortActive,
      direction: this.sortDirection,
    }).subscribe({
      next: (blob) => {
        const ext = format === 'pdf' ? 'pdf' : 'xlsx';
        const url = URL.createObjectURL(blob);
        const a   = document.createElement('a');
        a.href     = url;
        a.download = `scholarship-types.${ext}`;
        a.click();
        URL.revokeObjectURL(url);
        this.exporting.set(false);
      },
      error: () => {
        this.toast.error('Export failed. Please try again.');
        this.exporting.set(false);
      },
    });
  }

  private loadPage(): void {
    this.loading.set(true);
    const search = this.searchValue().trim() || undefined;
    this.scholarshipService.getScholarshipTypesPage({ search, page: this.currentPage, size: this.currentPageSize, sort: this.sortMap[this.sortActive] ?? this.sortActive, direction: this.sortDirection }).subscribe({
      next: (page) => {
        this.dataSource.data = page.content;
        this.totalElements = page.totalElements;
        if (this._paginator) {
          this._paginator.length = page.totalElements;
          this._paginator.pageIndex = page.number;
        }
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load scholarships'); this.loading.set(false); },
    });
  }
}

import { Component, inject, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { CurriculumVersionService } from '../curriculum-version.service';
import { CurriculumVersion } from '../curriculum-version.model';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsViewToggleComponent } from '../../../shared/view-toggle/view-toggle.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { ToastService } from '../../../core/toast/toast.service';
import { environment } from '../../../../environments';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { CURRICULUM_VERSION_LIST_TOUR } from '../../../shared/tour/tours/curriculum-version.tours';
import { CmsIconEditComponent, CmsIconDeleteComponent, CmsIconViewComponent } from '../../../shared/icons';
import { CurriculumVersionCloneDialogComponent, CurriculumVersionCloneDialogData } from '../curriculum-version-clone-dialog/curriculum-version-clone-dialog.component';

@Component({
  selector: 'app-curriculum-version-list',
  standalone: true,
  imports: [
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatDialogModule,
    MatTooltipModule,
    CmsEmptyStateComponent,
    CmsViewToggleComponent,
    CmsStatusBadgeComponent,
    CmsRowActionButtonComponent,
    CmsTourButtonComponent,
    CmsIconEditComponent,
    CmsIconDeleteComponent,
    CmsIconViewComponent,
    CurriculumVersionCloneDialogComponent,
  ],
  templateUrl: './curriculum-version-list.component.html',
  styleUrl: './curriculum-version-list.component.scss',
})
export class CurriculumVersionListComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly curriculumVersionService = inject(CurriculumVersionService);
  private readonly toast = inject(ToastService);
  private readonly http = inject(HttpClient);
  private readonly dialog = inject(MatDialog);
  private readonly tourService = inject(TourService);

  private readonly VIEW_MODE_KEY = 'curriculum-version-view-mode';
  private readonly destroy$ = new Subject<void>();
  private readonly searchSubject = new Subject<string>();
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
      this.loadPage();
    });
  }

  protected readonly displayedColumns = ['versionName', 'program', 'effectiveFromAcademicYearName', 'content', 'status', 'actions'];
  protected readonly dataSource = new MatTableDataSource<CurriculumVersion>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly selectedProgramId = signal<number | null>(null);
  protected readonly statusFilter = signal<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL');
  protected readonly programs = signal<{ id: number; name: string; code: string }[]>([]);
  protected readonly viewMode = signal<'card' | 'table'>(this.loadViewMode());
  protected readonly cloneTarget = signal<CurriculumVersionCloneDialogData | null>(null);

  protected totalElements = 0;
  protected currentPage = 0;
  protected currentPageSize = 25;
  protected sortActive = 'versionName';
  protected sortDirection: 'asc' | 'desc' = 'asc';

  private readonly sortMap: Record<string, string> = {
    versionName: 'versionName',
    program: 'program.name',
    effectiveFromAcademicYearName: 'effectiveFromAcademicYear.name',
    status: 'isActive',
  };

  ngOnInit(): void {
    this.tourService.register('curriculum-version-list', CURRICULUM_VERSION_LIST_TOUR);

    const programIdParam = this.route.snapshot.queryParamMap.get('programId');
    if (programIdParam) {
      this.selectedProgramId.set(Number(programIdParam));
    }

    this.http.get<{ id: number; name: string; code: string }[]>(`${environment.apiUrl}/programs`)
      .subscribe({ next: (data) => { this.programs.set(data); } });

    this.searchSubject.pipe(debounceTime(400), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => { this.currentPage = 0; this.loadPage(); });
    this.loadPage();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this._paginatorSub?.unsubscribe();
  }

  protected setViewMode(mode: 'card' | 'table'): void {
    this.viewMode.set(mode);
    localStorage.setItem(this.VIEW_MODE_KEY, mode);
  }

  protected applyFilter(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchValue.set(value);
    this.searchSubject.next(value);
  }

  protected clearFilter(): void {
    this.searchValue.set('');
    this.searchSubject.next('');
  }

  protected onProgramFilterChange(programIdStr: string): void {
    this.selectedProgramId.set(programIdStr ? +programIdStr : null);
    this.currentPage = 0;
    this.loadPage();
  }

  protected onStatusFilterChange(value: string): void {
    this.statusFilter.set(value as 'ALL' | 'ACTIVE' | 'INACTIVE');
    this.currentPage = 0;
    this.loadPage();
  }

  protected onSortChange(sort: Sort): void {
    this.sortActive = sort.active;
    this.sortDirection = sort.direction as 'asc' | 'desc';
    this.currentPage = 0;
    this.loadPage();
  }

  private loadPage(): void {
    this.loading.set(true);
    const search = this.searchValue().trim() || undefined;
    const isActive = this.statusFilter() === 'ALL' ? null : this.statusFilter() === 'ACTIVE';
    this.curriculumVersionService.getPage({
      search,
      programId: this.selectedProgramId(),
      isActive,
      page: this.currentPage,
      size: this.currentPageSize,
      sort: this.sortMap[this.sortActive] ?? this.sortActive,
      direction: this.sortDirection,
    }).subscribe({
      next: (page) => {
        this.dataSource.data = page.content;
        this.totalElements = page.totalElements;
        if (this._paginator) {
          this._paginator.length = page.totalElements;
          this._paginator.pageIndex = page.number;
        }
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load curriculum versions');
        this.loading.set(false);
      },
    });
  }

  protected navigateToNew(): void {
    const params = this.selectedProgramId() ? { queryParams: { programId: this.selectedProgramId() } } : {};
    void this.router.navigate(['/curriculum-versions/new'], params);
  }

  protected editVersion(version: CurriculumVersion): void {
    void this.router.navigate(['/curriculum-versions', version.id, 'edit']);
  }

  protected viewCurriculum(version: CurriculumVersion): void {
    void this.router.navigate(['/curriculum-map', version.id]);
  }

  protected openCloneDialog(version: CurriculumVersion): void {
    this.cloneTarget.set({ source: version });
  }

  protected onCloneClosed(cloned: CurriculumVersion | undefined): void {
    this.cloneTarget.set(null);
    if (cloned) {
      this.loadPage();
    }
  }

  protected deleteVersion(version: CurriculumVersion): void {
    if (!version.deletable) return;
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Curriculum Version',
        message: `Are you sure you want to delete "${version.versionName}"? This also removes its term/subject mapping.`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) this.performDelete(version);
    });
  }

  protected deleteTooltip(version: CurriculumVersion): string {
    return version.deletable
      ? 'Delete'
      : 'Cannot delete — subjects are mapped into this version or course offerings reference it';
  }

  protected handleEmptyAction(): void {
    if (this.searchValue()) {
      this.clearFilter();
    } else {
      this.navigateToNew();
    }
  }

  private loadViewMode(): 'card' | 'table' {
    const stored = localStorage.getItem(this.VIEW_MODE_KEY);
    return stored === 'table' ? 'table' : 'card';
  }

  private performDelete(version: CurriculumVersion): void {
    this.loading.set(true);
    this.curriculumVersionService.delete(version.id).subscribe({
      next: () => {
        this.toast.success('Curriculum version deleted');
        this.loadPage();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to delete curriculum version');
        this.loading.set(false);
      },
    });
  }
}

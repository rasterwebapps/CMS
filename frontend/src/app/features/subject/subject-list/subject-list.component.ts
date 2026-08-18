import { Component, inject, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject as RxSubject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { SubjectService } from '../subject.service';
import { Subject } from '../subject.model';
import { CourseService } from '../../course/course.service';
import { Course } from '../../course/course.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsViewToggleComponent } from '../../../shared/view-toggle/view-toggle.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsIconDeleteComponent, CmsIconEditComponent, CmsIconToggleStatusComponent } from '../../../shared/icons';
import { TourService } from '../../../shared/tour/tour.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { SUBJECT_LIST_TOUR, SUBJECT_LIST_FLOW_MAP } from '../../../shared/tour/tours/preferences-remainder.tours';

@Component({
  selector: 'app-subject-list',
  standalone: true,
  imports: [
    RouterLink,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatDialogModule,
    MatTooltipModule,
    CmsEmptyStateComponent,
    CmsViewToggleComponent,
    CmsStatusBadgeComponent,
    CmsRowActionButtonComponent,
    CmsIconDeleteComponent,
    CmsIconEditComponent,
    CmsIconToggleStatusComponent,
    CmsTourButtonComponent,
  ],
  templateUrl: './subject-list.component.html',
  styleUrl: './subject-list.component.scss',
})
export class SubjectListComponent implements OnInit, OnDestroy {
  private readonly subjectService = inject(SubjectService);
  private readonly courseService = inject(CourseService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly tourService = inject(TourService);

  private readonly VIEW_MODE_KEY = 'subject-view-mode';
  private readonly destroy$ = new RxSubject<void>();
  private readonly searchSubject = new RxSubject<string>();
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

  protected readonly displayedColumns = ['code', 'name', 'credits', 'term', 'status', 'actions'];
  protected readonly dataSource = new MatTableDataSource<Subject>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly selectedCourseId = signal<number | null>(null);
  protected readonly courses = signal<Course[]>([]);
  protected readonly viewMode = signal<'card' | 'table'>(this.loadViewMode());

  protected totalElements = 0;
  protected currentPage = 0;
  protected currentPageSize = 25;
  protected sortActive = 'name';
  protected sortDirection: 'asc' | 'desc' = 'asc';

  private readonly sortMap: Record<string, string> = {
    code: 'code',
    name: 'name',
    credits: 'credits',
    term: 'termNumber',
    status: 'isActive',
  };

  ngOnInit(): void {
    this.tourService.register('subject-list', SUBJECT_LIST_TOUR);
    this.tourService.registerFlowMap('subject-list', SUBJECT_LIST_FLOW_MAP);

    this.loadCourses();
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

  protected onCourseFilterChange(courseIdStr: string): void {
    this.selectedCourseId.set(courseIdStr ? +courseIdStr : null);
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
    this.subjectService.getPage({
      search,
      courseId: this.selectedCourseId(),
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
        this.toast.error('Failed to load subjects');
        this.loading.set(false);
      },
    });
  }

  protected editSubject(subject: Subject): void {
    void this.router.navigate(['/subjects', subject.id, 'edit']);
  }

  protected deleteSubject(subject: Subject): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Subject',
        message: `Are you sure you want to delete "${subject.name}"?`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) this.performDelete(subject);
    });
  }

  protected toggleSubjectStatus(subject: Subject): void {
    const nextIsActive = !subject.isActive;
    const action = nextIsActive ? 'Activate' : 'Deactivate';
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: `${action} Subject`,
        message: `Are you sure you want to ${action.toLowerCase()} "${subject.name}"?`,
        confirmText: action,
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.loading.set(true);
      this.subjectService.updateStatus(subject.id, {
        isActive: nextIsActive,
        reason: `Manual ${action.toLowerCase()} from list`,
      }).subscribe({
        next: () => {
          this.toast.success(`Subject ${action.toLowerCase()}d successfully`);
          this.loadPage();
        },
        error: (err) => {
          this.toast.error(err?.error?.message ?? `Failed to ${action.toLowerCase()} subject`);
          this.loading.set(false);
        },
      });
    });
  }

  protected handleEmptyAction(): void {
    if (this.searchValue()) {
      this.clearFilter();
    } else {
      void this.router.navigate(['/subjects/new']);
    }
  }

  private loadViewMode(): 'card' | 'table' {
    const stored = localStorage.getItem(this.VIEW_MODE_KEY);
    return stored === 'table' ? 'table' : 'card';
  }

  private performDelete(subject: Subject): void {
    this.loading.set(true);
    this.subjectService.delete(subject.id).subscribe({
      next: () => {
        this.toast.success('Subject deleted successfully');
        this.loadPage();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to delete subject');
        this.loading.set(false);
      },
    });
  }

  private loadCourses(): void {
    this.courseService.getAll().subscribe({
      next: (courses) => this.courses.set(courses),
      error: () => this.toast.error('Failed to load courses'),
    });
  }
}

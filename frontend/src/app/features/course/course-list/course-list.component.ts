import { Component, inject, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { CourseService } from '../course.service';
import { Course } from '../course.model';
import { ProgramService } from '../../program/program.service';
import { Program } from '../../program/program.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsViewToggleComponent } from '../../../shared/view-toggle/view-toggle.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { COURSE_LIST_TOUR, COURSE_LIST_FLOW_MAP } from '../../../shared/tour/tours/course.tours';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsIconDeleteComponent, CmsIconEditComponent, CmsIconToggleStatusComponent } from '../../../shared/icons';

@Component({
  selector: 'app-course-list',
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
    CmsTourButtonComponent,
    CmsRowActionButtonComponent,
    CmsIconDeleteComponent,
    CmsIconEditComponent,
    CmsIconToggleStatusComponent,
  ],
  templateUrl: './course-list.component.html',
  styleUrl: './course-list.component.scss',
})
export class CourseListComponent implements OnInit, OnDestroy {
  private readonly courseService = inject(CourseService);
  private readonly programService = inject(ProgramService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly tourService = inject(TourService);

  private readonly VIEW_MODE_KEY = 'course-view-mode';
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

  protected readonly displayedColumns = ['code', 'name', 'specialization', 'program', 'status', 'actions'];
  protected readonly dataSource = new MatTableDataSource<Course>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly selectedProgramId = signal<number | null>(null);
  protected readonly programs = signal<Program[]>([]);
  protected readonly viewMode = signal<'card' | 'table'>(this.loadViewMode());

  protected totalElements = 0;
  protected currentPage = 0;
  protected currentPageSize = 25;
  protected sortActive = 'name';
  protected sortDirection: 'asc' | 'desc' = 'asc';

  private readonly sortMap: Record<string, string> = {
    code: 'code',
    name: 'name',
    specialization: 'specialization',
    program: 'program.name',
    status: 'isActive',
  };

  ngOnInit(): void {
    this.tourService.register('course-list', COURSE_LIST_TOUR);
    this.tourService.registerFlowMap('course-list', COURSE_LIST_FLOW_MAP);
    this.loadPrograms();
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

  protected onSortChange(sort: Sort): void {
    this.sortActive = sort.active;
    this.sortDirection = sort.direction as 'asc' | 'desc';
    this.currentPage = 0;
    this.loadPage();
  }

  private loadPage(): void {
    this.loading.set(true);
    const search = this.searchValue().trim() || undefined;
    this.courseService.getPage({
      search,
      programId: this.selectedProgramId(),
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
        this.toast.error('Failed to load courses');
        this.loading.set(false);
      },
    });
  }

  protected editCourse(course: Course): void {
    void this.router.navigate(['/courses', course.id, 'edit']);
  }

  protected deleteCourse(course: Course): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Course',
        message: `Are you sure you want to delete "${course.name}"?`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) this.performDelete(course);
    });
  }

  protected toggleCourseStatus(course: Course): void {
    const nextIsActive = !course.isActive;
    const action = nextIsActive ? 'Activate' : 'Deactivate';
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: `${action} Course`,
        message: `Are you sure you want to ${action.toLowerCase()} "${course.name}"?`,
        confirmText: action,
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.loading.set(true);
      this.courseService.updateStatus(course.id, {
        isActive: nextIsActive,
        reason: `Manual ${action.toLowerCase()} from list`,
      }).subscribe({
        next: () => {
          this.toast.success(`Course ${action.toLowerCase()}d successfully`);
          this.loadPage();
        },
        error: (err) => {
          this.toast.error(err?.error?.message ?? `Failed to ${action.toLowerCase()} course`);
          this.loading.set(false);
        },
      });
    });
  }

  protected handleEmptyAction(): void {
    if (this.searchValue()) {
      this.clearFilter();
    } else {
      void this.router.navigate(['/courses/new']);
    }
  }

  private loadViewMode(): 'card' | 'table' {
    const stored = localStorage.getItem(this.VIEW_MODE_KEY);
    return stored === 'table' ? 'table' : 'card';
  }

  private performDelete(course: Course): void {
    this.loading.set(true);
    this.courseService.delete(course.id).subscribe({
      next: () => {
        this.toast.success('Course deleted successfully');
        this.loadPage();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to delete course');
        this.loading.set(false);
      },
    });
  }

  private loadPrograms(): void {
    this.programService.getAll().subscribe({
      next: (programs) => this.programs.set(programs),
      error: () => this.toast.error('Failed to load programs'),
    });
  }
}

import { Component, inject, OnInit, OnDestroy, AfterViewInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { ProgramService } from '../program.service';
import { Program, ProgramStatus } from '../program.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsViewToggleComponent } from '../../../shared/view-toggle/view-toggle.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { PROGRAM_LIST_TOUR } from '../../../shared/tour/tours/program.tours';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsIconDeleteComponent, CmsIconEditComponent, CmsIconToggleStatusComponent } from '../../../shared/icons';

@Component({
  selector: 'app-program-list',
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
  templateUrl: './program-list.component.html',
  styleUrl: './program-list.component.scss',
})
export class ProgramListComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly programService = inject(ProgramService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly tourService = inject(TourService);

  private readonly VIEW_MODE_KEY = 'program-view-mode';
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

  protected readonly displayedColumns = ['code', 'name', 'durationYears', 'totalTerms', 'status', 'actions'];
  protected readonly dataSource = new MatTableDataSource<Program>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly viewMode = signal<'card' | 'table'>(this.loadViewMode());

  protected totalElements = 0;
  protected currentPage = 0;
  protected currentPageSize = 25;
  protected sortActive = 'name';
  protected sortDirection: 'asc' | 'desc' = 'asc';
  private readonly sortMap: Record<string, string> = {
    name: 'name', code: 'code', status: 'status',
  };

  ngOnInit(): void {
    this.tourService.register('program-list', PROGRAM_LIST_TOUR);
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

  protected onSortChange(sort: Sort): void {
    this.sortActive = sort.active;
    this.sortDirection = sort.direction as 'asc' | 'desc';
    this.currentPage = 0;
    this.loadPage();
  }

  protected handleEmptyAction(): void {
    if (this.searchValue()) {
      this.clearFilter();
    } else {
      void this.router.navigate(['/programs/new']);
    }
  }

  protected termCountLabel(program: Program): string {
    const n = program.totalTerms;
    if (program.assessmentPattern === 'YEARLY') {
      return `${n} ${n === 1 ? 'Year' : 'Years'}`;
    }
    return `${n} ${n === 1 ? 'Term' : 'Terms'}`;
  }

  protected editProgram(program: Program): void {
    void this.router.navigate(['/programs', program.id, 'edit']);
  }

  protected deleteProgram(program: Program): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Program',
        message: `Are you sure you want to delete "${program.name}"?`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) this.performDelete(program);
    });
  }

  protected toggleProgramStatus(program: Program): void {
    const nextStatus: ProgramStatus = program.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    const action = nextStatus === 'ACTIVE' ? 'Activate' : 'Deactivate';
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: `${action} Program`,
        message: `Are you sure you want to ${action.toLowerCase()} "${program.name}"?`,
        confirmText: action,
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.loading.set(true);
      this.programService.updateStatus(program.id, {
        status: nextStatus,
        reason: `Manual ${action.toLowerCase()} from list`,
      }).subscribe({
        next: () => {
          this.toast.success(`Program ${action.toLowerCase()}d successfully`);
          this.loadPage();
        },
        error: (err) => {
          this.toast.error(err?.error?.message ?? `Failed to ${action.toLowerCase()} program`);
          this.loading.set(false);
        },
      });
    });
  }

  private loadViewMode(): 'card' | 'table' {
    return localStorage.getItem(this.VIEW_MODE_KEY) === 'table' ? 'table' : 'card';
  }

  private performDelete(program: Program): void {
    this.loading.set(true);
    this.programService.delete(program.id).subscribe({
      next: () => {
        this.toast.success('Program deleted successfully');
        this.loadPage();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to delete program');
        this.loading.set(false);
      },
    });
  }

  private loadPage(): void {
    this.loading.set(true);
    const search = this.searchValue().trim() || undefined;
    this.programService.getPage({ search, page: this.currentPage, size: this.currentPageSize, sort: this.sortMap[this.sortActive] ?? this.sortActive, direction: this.sortDirection }).subscribe({
      next: (page) => {
        this.dataSource.data = page.content;
        this.totalElements = page.totalElements;
        if (this._paginator) {
          this._paginator.length = page.totalElements;
          this._paginator.pageIndex = page.number;
        }
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load programs'); this.loading.set(false); },
    });
  }
}

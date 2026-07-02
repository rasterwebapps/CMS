import { Component, inject, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { LabService } from '../lab.service';
import { Lab, LabType, LabStatus, LAB_TYPES, LAB_STATUSES } from '../lab.model';
import { SpecialityService } from '../../speciality/speciality.service';
import { Speciality } from '../../speciality/speciality.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsViewToggleComponent } from '../../../shared/view-toggle/view-toggle.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { CmsTypeBadgeComponent } from '../../../shared/type-badge/type-badge.component';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsIconDeleteComponent, CmsIconEditComponent, CmsIconViewComponent } from '../../../shared/icons';

@Component({
  selector: 'app-lab-list',
  standalone: true,
  imports: [
    CmsEmptyStateComponent,
    CmsViewToggleComponent,
    CmsStatusBadgeComponent,
    CmsTypeBadgeComponent,
    RouterLink, CmsTourButtonComponent,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatDialogModule,
    MatTooltipModule,
    CmsRowActionButtonComponent,
    CmsIconDeleteComponent,
    CmsIconEditComponent,
    CmsIconViewComponent,
  ],
  templateUrl: './lab-list.component.html',
  styleUrl: './lab-list.component.scss',
})
export class LabListComponent implements OnInit, OnDestroy {
  private readonly labService = inject(LabService);
  private readonly specialityService = inject(SpecialityService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);

  private readonly VIEW_MODE_KEY = 'lab-view-mode';
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

  protected readonly displayedColumns = ['name', 'labType', 'location', 'capacity', 'status', 'speciality', 'actions'];
  protected readonly dataSource = new MatTableDataSource<Lab>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly viewMode = signal<'card' | 'table'>(this.loadViewMode());

  protected readonly specialities = signal<Speciality[]>([]);
  protected readonly labTypes = LAB_TYPES;
  protected readonly labStatuses = LAB_STATUSES;

  protected readonly selectedSpeciality = signal<number | null>(null);
  protected readonly selectedType = signal<LabType | null>(null);
  protected readonly selectedStatus = signal<LabStatus | null>(null);

  protected totalElements = 0;
  protected currentPage = 0;
  protected currentPageSize = 25;
  protected sortActive = 'name';
  protected sortDirection: 'asc' | 'desc' = 'asc';

  private readonly sortMap: Record<string, string> = {
    name: 'name',
    labType: 'labType',
    location: 'building',
    capacity: 'capacity',
    status: 'status',
    speciality: 'speciality.name',
  };

  ngOnInit(): void {
    this.loadSpecialities();
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

  protected onSpecialityChange(specialityId: number | null): void {
    this.selectedSpeciality.set(specialityId);
    this.currentPage = 0;
    this.loadPage();
  }

  protected onTypeChange(type: LabType | null): void {
    this.selectedType.set(type);
    this.currentPage = 0;
    this.loadPage();
  }

  protected onStatusChange(status: LabStatus | null): void {
    this.selectedStatus.set(status);
    this.currentPage = 0;
    this.loadPage();
  }

  protected clearAllFilters(): void {
    this.searchValue.set('');
    this.selectedSpeciality.set(null);
    this.selectedType.set(null);
    this.selectedStatus.set(null);
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
    this.labService.getPage({
      search,
      specialityId: this.selectedSpeciality(),
      labType: this.selectedType(),
      status: this.selectedStatus(),
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
        this.toast.error('Failed to load labs');
        this.loading.set(false);
      },
    });
  }

  protected viewLab(lab: Lab): void {
    void this.router.navigate(['/labs', lab.id]);
  }

  protected editLab(lab: Lab): void {
    void this.router.navigate(['/labs', lab.id, 'edit']);
  }

  protected deleteLab(lab: Lab): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Lab',
        message: `Are you sure you want to delete "${lab.name}"?`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) this.performDelete(lab);
    });
  }

  protected getTypeLabel(type: LabType): string {
    return this.labTypes.find(t => t.value === type)?.label ?? type;
  }

  protected getLocation(lab: Lab): string {
    const parts = [lab.building, lab.roomNumber].filter(Boolean);
    return parts.length > 0 ? parts.join(' – ') : '—';
  }

  protected handleEmptyAction(): void {
    void this.router.navigate(['/labs/new']);
  }

  private loadViewMode(): 'card' | 'table' {
    try {
      return localStorage.getItem(this.VIEW_MODE_KEY) === 'table' ? 'table' : 'card';
    } catch {
      return 'card';
    }
  }

  private performDelete(lab: Lab): void {
    this.loading.set(true);
    this.labService.delete(lab.id).subscribe({
      next: () => {
        this.toast.success('Lab deleted successfully');
        this.loadPage();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to delete lab');
        this.loading.set(false);
      },
    });
  }

  private loadSpecialities(): void {
    this.specialityService.getAll().subscribe({
      next: (specialities) => this.specialities.set(specialities),
      error: () => this.toast.error('Failed to load specialities'),
    });
  }
}

import { Component, inject, OnInit, OnDestroy, AfterViewInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule } from '@angular/material/sort';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { EquipmentService } from '../equipment.service';
import { Equipment } from '../equipment.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { CmsTypeBadgeComponent } from '../../../shared/type-badge/type-badge.component';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsViewToggleComponent } from '../../../shared/view-toggle/view-toggle.component';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { EQUIPMENT_LIST_TOUR } from '../../../shared/tour/tours/equipment.tours';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsIconDeleteComponent, CmsIconEditComponent } from '../../../shared/icons';

@Component({
  selector: 'app-equipment-list',
  standalone: true,
  imports: [
    CmsEmptyStateComponent,
    CmsStatusBadgeComponent,
    CmsTypeBadgeComponent,
    CmsViewToggleComponent,
    RouterLink, MatTableModule, MatPaginatorModule, MatSortModule,
    MatDialogModule, MatTooltipModule,
    CmsTourButtonComponent,
    CmsRowActionButtonComponent,
    CmsIconDeleteComponent,
    CmsIconEditComponent,
  ],
  templateUrl: './equipment-list.component.html',
  styleUrl: './equipment-list.component.scss',
})
export class EquipmentListComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly equipmentService = inject(EquipmentService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly tourService = inject(TourService);

  private readonly VIEW_MODE_KEY = 'equipment-view-mode';
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

  protected readonly displayedColumns: readonly string[] = ['name', 'model', 'labName', 'category', 'status', 'purchaseDate', 'actions'];
  protected readonly dataSource = new MatTableDataSource<Equipment>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly viewMode = signal<'card' | 'table'>(this.loadViewMode());

  protected totalElements = 0;
  protected currentPage = 0;
  protected currentPageSize = 25;

  ngOnInit(): void {
    this.tourService.register('equipment-list', EQUIPMENT_LIST_TOUR);
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

  protected applyFilter(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchValue.set(value);
    this.searchSubject.next(value);
  }

  protected clearFilter(): void {
    this.searchValue.set('');
    this.searchSubject.next('');
  }

  protected setViewMode(mode: 'card' | 'table'): void {
    this.viewMode.set(mode);
    localStorage.setItem(this.VIEW_MODE_KEY, mode);
  }

  private loadViewMode(): 'card' | 'table' {
    return localStorage.getItem(this.VIEW_MODE_KEY) === 'table' ? 'table' : 'card';
  }

  protected handleEmptyAction(): void {
    if (this.searchValue()) {
      this.clearFilter();
    } else {
      void this.router.navigate(['/equipment/new']);
    }
  }

  protected edit(item: Equipment): void {
    void this.router.navigate(['/equipment', item.id, 'edit']);
  }

  protected delete(item: Equipment): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Delete Equipment', message: `Delete "${item.name}"?`, confirmText: 'Delete', cancelText: 'Cancel' },
    }).afterClosed().subscribe((confirmed) => { if (confirmed) this.doDelete(item); });
  }

  private doDelete(item: Equipment): void {
    this.loading.set(true);
    this.equipmentService.delete(item.id).subscribe({
      next: () => { this.toast.success('Deleted successfully'); this.loadPage(); },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to delete'); this.loading.set(false); },
    });
  }

  private loadPage(): void {
    this.loading.set(true);
    const search = this.searchValue().trim() || undefined;
    this.equipmentService.getPage({ search, page: this.currentPage, size: this.currentPageSize }).subscribe({
      next: (page) => {
        this.dataSource.data = page.content;
        this.totalElements = page.totalElements;
        if (this._paginator) {
          this._paginator.length = page.totalElements;
          this._paginator.pageIndex = page.number;
        }
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load equipment'); this.loading.set(false); },
    });
  }
}

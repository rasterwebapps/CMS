import { Component, inject, OnInit, OnDestroy, signal, ViewChild } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatIconModule } from '@angular/material/icon';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';

import { RoomPreferenceService } from '../room-preference.service';
import { RoomPreference, RoomPreferenceStatus } from '../room-preference.model';
import { ConfirmDialogComponent } from '../../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../../shared/empty-state/empty-state.component';
import { CmsStatusBadgeComponent } from '../../../../shared/status-badge/status-badge.component';
import { CmsRowActionButtonComponent } from '../../../../shared/row-action-button/row-action-button.component';
import { ToastService } from '../../../../core/toast/toast.service';

@Component({
  selector: 'app-room-preference-list',
  standalone: true,
  imports: [
    DatePipe,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatDialogModule,
    MatTooltipModule,
    MatIconModule,
    CmsEmptyStateComponent,
    CmsStatusBadgeComponent,
    CmsRowActionButtonComponent,
  ],
  templateUrl: './room-preference-list.component.html',
  styleUrl: './room-preference-list.component.scss',
})
export class RoomPreferenceListComponent implements OnInit, OnDestroy {
  private readonly roomPreferenceService = inject(RoomPreferenceService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);

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

  protected readonly displayedColumns = ['requester', 'preferredRoomType', 'preferredZone', 'status', 'createdAt', 'actions'];
  protected readonly dataSource = new MatTableDataSource<RoomPreference>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly statusFilter = signal<RoomPreferenceStatus | ''>('');

  protected totalElements = 0;
  protected currentPage = 0;
  protected currentPageSize = 25;
  protected sortActive = 'createdAt';
  protected sortDirection: 'asc' | 'desc' = 'desc';

  ngOnInit(): void {
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

  protected onStatusFilterChange(value: RoomPreferenceStatus | ''): void {
    this.statusFilter.set(value);
    this.currentPage = 0;
    this.loadPage();
  }

  protected onSortChange(sort: Sort): void {
    this.sortActive = sort.active;
    this.sortDirection = (sort.direction || 'desc') as 'asc' | 'desc';
    this.currentPage = 0;
    this.loadPage();
  }

  protected requesterName(item: RoomPreference): string {
    return item.studentName ?? item.enquiryName ?? '—';
  }

  protected markFulfilled(item: RoomPreference): void {
    this.roomPreferenceService.update(item.id, {
      enquiryId: item.enquiryId,
      studentId: item.studentId,
      preferredRoomTypeId: item.preferredRoomTypeId,
      preferredZoneId: item.preferredZoneId,
      status: 'FULFILLED',
    }).subscribe({
      next: () => { this.toast.success('Marked as fulfilled'); this.loadPage(); },
      error: () => this.toast.error('Failed to update preference'),
    });
  }

  protected cancelPreference(item: RoomPreference): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Cancel Room Preference',
        message: `Cancel ${this.requesterName(item)}'s preference for ${item.preferredRoomTypeName}?`,
        confirmText: 'Cancel Preference',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.roomPreferenceService.update(item.id, {
        enquiryId: item.enquiryId,
        studentId: item.studentId,
        preferredRoomTypeId: item.preferredRoomTypeId,
        preferredZoneId: item.preferredZoneId,
        status: 'CANCELLED',
      }).subscribe({
        next: () => { this.toast.success('Preference cancelled'); this.loadPage(); },
        error: () => this.toast.error('Failed to cancel preference'),
      });
    });
  }

  protected goToAllocations(item: RoomPreference): void {
    void this.router.navigate(['/room-allocations'], { queryParams: { studentId: item.studentId ?? undefined } });
  }

  private loadPage(): void {
    this.loading.set(true);
    const search = this.searchValue().trim() || undefined;
    const status = this.statusFilter() || undefined;
    this.roomPreferenceService.getPage({
      search, status, page: this.currentPage, size: this.currentPageSize,
      sort: this.sortActive, direction: this.sortDirection,
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
      error: () => { this.toast.error('Failed to load room preferences'); this.loading.set(false); },
    });
  }
}

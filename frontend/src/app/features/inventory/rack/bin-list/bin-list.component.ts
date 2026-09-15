import { Component, computed, inject, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { InventoryRackService } from '../inventory-rack.service';
import { InventoryBin, InventoryRack } from '../inventory-rack.model';
import { ConfirmDialogComponent } from '../../../../shared/confirm-dialog/confirm-dialog.component';
import { ToastService } from '../../../../core/toast/toast.service';
import { CmsEmptyStateComponent } from '../../../../shared/empty-state/empty-state.component';
import { CmsRowActionButtonComponent } from '../../../../shared/row-action-button/row-action-button.component';
import { CmsStatusBadgeComponent } from '../../../../shared/status-badge/status-badge.component';
import { PermissionService } from '../../../../core/permissions/permission.service';

@Component({
  selector: 'app-inventory-bin-list',
  standalone: true,
  imports: [
    RouterLink, FormsModule,
    MatTableModule, MatPaginatorModule, MatSortModule,
    MatDialogModule, MatIconModule, MatTooltipModule,
    CmsEmptyStateComponent, CmsRowActionButtonComponent, CmsStatusBadgeComponent,
  ],
  templateUrl: './bin-list.component.html',
  styleUrl: './bin-list.component.scss',
})
export class BinListComponent implements OnInit, OnDestroy {
  private readonly rackService = inject(InventoryRackService);
  private readonly router      = inject(Router);
  private readonly route       = inject(ActivatedRoute);
  private readonly toast       = inject(ToastService);
  private readonly dialog      = inject(MatDialog);
  protected readonly permissions = inject(PermissionService);

  private readonly destroy$      = new Subject<void>();
  private readonly searchSubject = new Subject<string>();
  private _paginator?: MatPaginator;
  private _paginatorSub?: Subscription;

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

  protected readonly displayedColumns = ['name', 'code', 'description', 'isActive', 'actions'];
  protected readonly dataSource   = new MatTableDataSource<InventoryBin>([]);
  protected readonly loading      = signal(false);
  protected readonly rackLoading  = signal(true);
  protected readonly searchValue  = signal('');
  protected readonly rack         = signal<InventoryRack | null>(null);
  protected readonly canManage    = computed(() => this.permissions.hasAny('INVENTORY_BIN_MANAGE'));

  protected totalElements   = 0;
  protected currentPage     = 0;
  protected currentPageSize = 25;
  protected sortActive      = 'name';
  protected sortDirection: 'asc' | 'desc' = 'asc';

  protected rackId!: number;

  ngOnInit(): void {
    const rackIdParam = this.route.snapshot.paramMap.get('rackId');
    if (!rackIdParam) {
      void this.router.navigate(['/inventory/racks']);
      return;
    }
    this.rackId = Number(rackIdParam);

    this.rackService.getRackById(this.rackId).subscribe({
      next: (rack) => { this.rack.set(rack); this.rackLoading.set(false); },
      error: () => {
        this.toast.error('Failed to load rack');
        void this.router.navigate(['/inventory/racks']);
      },
    });

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

  protected clearFilter(): void {
    this.searchValue.set('');
    this.searchSubject.next('');
  }

  protected onSortChange(sort: Sort): void {
    this.sortActive    = sort.active;
    this.sortDirection = (sort.direction || 'asc') as 'asc' | 'desc';
    this.currentPage   = 0;
    this.loadPage();
  }

  protected editBin(bin: InventoryBin): void {
    void this.router.navigate(['/inventory/racks', this.rackId, 'bins', bin.id, 'edit']);
  }

  protected toggleBinStatus(bin: InventoryBin): void {
    const nextAction = bin.isActive ? 'Deactivate' : 'Activate';
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: `${nextAction} Bin`,
        message: `${nextAction} "${bin.name}" (${bin.code})?`,
        confirmText: nextAction,
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe(confirmed => { if (confirmed) this.performToggle(bin); });
  }

  protected handleEmptyAction(): void {
    if (this.searchValue()) {
      this.clearFilter();
    } else if (this.canManage()) {
      void this.router.navigate(['/inventory/racks', this.rackId, 'bins', 'new']);
    }
  }

  private performToggle(bin: InventoryBin): void {
    this.rackService.updateBinStatus(bin.id, !bin.isActive).subscribe({
      next: () => {
        this.toast.success(`Bin ${bin.isActive ? 'deactivated' : 'activated'} successfully`);
        this.loadPage();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? `Failed to ${bin.isActive ? 'deactivate' : 'activate'} bin`);
      },
    });
  }

  private loadPage(): void {
    this.loading.set(true);
    this.rackService.getBinsPage({
      search: this.searchValue().trim() || undefined,
      rackId: this.rackId,
      page: this.currentPage, size: this.currentPageSize,
      sort: this.sortActive, direction: this.sortDirection,
    }).subscribe({
      next: (page) => {
        this.dataSource.data = page.content;
        this.totalElements = page.totalElements;
        if (this._paginator) { this._paginator.length = page.totalElements; this._paginator.pageIndex = page.number; }
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load bins'); this.loading.set(false); },
    });
  }
}

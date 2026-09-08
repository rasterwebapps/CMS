import { Component, inject, OnInit, OnDestroy, ViewChild, computed, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Subject, Subscription } from 'rxjs';
import { WantedListService } from '../wanted-list.service';
import { WantedListItem } from '../wanted-list.model';
import { InventoryLocationService } from '../../../location/inventory-location.service';
import { InventoryLocation } from '../../../location/inventory-location.model';
import { CmsEmptyStateComponent } from '../../../../../shared/empty-state/empty-state.component';
import { CmsStatusBadgeComponent } from '../../../../../shared/status-badge/status-badge.component';
import { CmsRowActionButtonComponent } from '../../../../../shared/row-action-button/row-action-button.component';
import { PermissionService } from '../../../../../core/permissions/permission.service';
import { ToastService } from '../../../../../core/toast/toast.service';
import { WantedListRejectDialogComponent, WantedListRejectDialogResult } from '../wanted-list-reject-dialog/wanted-list-reject-dialog.component';
import { WantedListConvertDialogComponent, WantedListConvertDialogResult } from '../wanted-list-convert-dialog/wanted-list-convert-dialog.component';

@Component({
  selector: 'app-wanted-list-list',
  standalone: true,
  imports: [
    FormsModule,
    DatePipe,
    MatTableModule,
    MatPaginatorModule,
    MatDialogModule,
    CmsEmptyStateComponent,
    CmsStatusBadgeComponent,
    CmsRowActionButtonComponent,
  ],
  templateUrl: './wanted-list-list.component.html',
  styleUrl: './wanted-list-list.component.scss',
})
export class WantedListListComponent implements OnInit, OnDestroy {
  private readonly wantedListService = inject(WantedListService);
  private readonly locationService   = inject(InventoryLocationService);
  private readonly dialog            = inject(MatDialog);
  private readonly permissionService = inject(PermissionService);
  private readonly router            = inject(Router);
  private readonly toast             = inject(ToastService);

  private readonly destroy$ = new Subject<void>();
  private _paginator?: MatPaginator;
  private _paginatorSub?: Subscription;

  @ViewChild(MatPaginator) set paginatorRef(p: MatPaginator | undefined) {
    if (!p || p === this._paginator) return;
    this._paginatorSub?.unsubscribe();
    this._paginator = p;
    p.pageIndex = this.currentPage;
    p.pageSize = this.currentPageSize;
    this._paginatorSub = p.page.pipe().subscribe((e: PageEvent) => {
      this.currentPage = e.pageIndex;
      this.currentPageSize = e.pageSize;
      this.loadPage();
    });
  }

  protected readonly displayedColumns =
    ['select', 'productName', 'locationVirtualName', 'qtyOnHandSnapshot', 'qtyOnOrderSnapshot', 'reorderLevelSnapshot', 'suggestedQty', 'status', 'generatedAt', 'actions'];
  protected readonly dataSource = new MatTableDataSource<WantedListItem>([]);
  protected readonly loading = signal(false);
  protected readonly busy = signal(false);
  protected readonly locations = signal<InventoryLocation[]>([]);
  protected readonly selectedIds = signal<Set<number>>(new Set());

  protected locationFilter: number | null = null;
  protected statusFilter: string | null = null;
  protected totalElements = 0;
  protected currentPage = 0;
  protected currentPageSize = 25;

  protected readonly canManage  = computed(() => this.permissionService.has('INVENTORY_WANTED_LIST_MANAGE'));
  protected readonly canConvert = computed(() => this.permissionService.has('INVENTORY_WANTED_LIST_CONVERT'));
  protected readonly canRun     = computed(() => this.permissionService.has('INVENTORY_WANTED_LIST_RUN'));

  protected readonly selectedItems = computed(() => {
    const ids = this.selectedIds();
    return this.dataSource.data.filter((row) => ids.has(row.id));
  });

  ngOnInit(): void {
    this.locationService.getAll(true).subscribe({ next: (l) => this.locations.set(l) });
    this.loadPage();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this._paginatorSub?.unsubscribe();
  }

  protected onFilterChange(): void {
    this.currentPage = 0;
    this.loadPage();
  }

  protected isSelectable(row: WantedListItem): boolean {
    return row.status === 'PENDING' || row.status === 'DEFERRED';
  }

  protected isSelected(row: WantedListItem): boolean {
    return this.selectedIds().has(row.id);
  }

  protected toggleSelection(row: WantedListItem): void {
    if (!this.isSelectable(row)) return;
    const current = this.selectedItems();
    if (!this.isSelected(row) && current.length > 0 && current[0].locationId !== row.locationId) {
      this.toast.error('Select lines from a single location to convert together');
      return;
    }
    const next = new Set(this.selectedIds());
    if (next.has(row.id)) next.delete(row.id); else next.add(row.id);
    this.selectedIds.set(next);
  }

  protected clearSelection(): void {
    this.selectedIds.set(new Set());
  }

  protected runNow(): void {
    this.busy.set(true);
    this.wantedListService.run().subscribe({
      next: (res) => {
        this.busy.set(false);
        this.toast.success(res.created > 0 ? `${res.created} new shortage line(s) found` : 'No new shortages found');
        this.loadPage();
      },
      error: (err) => { this.busy.set(false); this.toast.error(err?.error?.message ?? 'Failed to run the shortage check'); },
    });
  }

  protected defer(row: WantedListItem): void {
    this.busy.set(true);
    this.wantedListService.defer(row.id, {}).subscribe({
      next: () => { this.busy.set(false); this.toast.success('Line deferred'); this.loadPage(); },
      error: (err) => { this.busy.set(false); this.toast.error(err?.error?.message ?? 'Failed to defer line'); },
    });
  }

  protected reopen(row: WantedListItem): void {
    this.busy.set(true);
    this.wantedListService.reopen(row.id).subscribe({
      next: () => { this.busy.set(false); this.toast.success('Line reopened'); this.loadPage(); },
      error: (err) => { this.busy.set(false); this.toast.error(err?.error?.message ?? 'Failed to reopen line'); },
    });
  }

  protected reject(row: WantedListItem): void {
    this.dialog.open(WantedListRejectDialogComponent, { data: { productName: row.productName } })
      .afterClosed().subscribe((result: WantedListRejectDialogResult | null) => {
        if (!result) return;
        this.busy.set(true);
        this.wantedListService.reject(row.id, result).subscribe({
          next: () => { this.busy.set(false); this.toast.success('Line rejected'); this.loadPage(); this.deselect(row.id); },
          error: (err) => { this.busy.set(false); this.toast.error(err?.error?.message ?? 'Failed to reject line'); },
        });
      });
  }

  protected convertSelected(): void {
    const items = this.selectedItems();
    if (items.length === 0) return;
    this.dialog.open(WantedListConvertDialogComponent, {
      data: { items, locationName: items[0].locationVirtualName },
    }).afterClosed().subscribe((result: WantedListConvertDialogResult | null) => {
      if (!result) return;
      this.busy.set(true);
      this.wantedListService.convert(result).subscribe({
        next: (requisition) => {
          this.busy.set(false);
          this.toast.success('Purchase requisition created');
          this.clearSelection();
          this.loadPage();
          void this.router.navigate(['/inventory/procurement/purchase-requisitions', requisition.id]);
        },
        error: (err) => { this.busy.set(false); this.toast.error(err?.error?.message ?? 'Failed to convert lines'); },
      });
    });
  }

  protected openRequisition(id: number): void {
    void this.router.navigate(['/inventory/procurement/purchase-requisitions', id]);
  }

  private deselect(id: number): void {
    const next = new Set(this.selectedIds());
    next.delete(id);
    this.selectedIds.set(next);
  }

  private loadPage(): void {
    this.loading.set(true);
    this.wantedListService.getPage({
      locationId: this.locationFilter,
      status: this.statusFilter,
      page: this.currentPage,
      size: this.currentPageSize,
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
      error: () => { this.toast.error('Failed to load the wanted list'); this.loading.set(false); },
    });
  }
}

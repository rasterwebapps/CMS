import { Component, inject, OnInit, OnDestroy, ViewChild, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Subject, Subscription } from 'rxjs';
import { ReorderConfigService } from '../reorder-config.service';
import { ProductLocationReorderConfig } from '../reorder-config.model';
import { InventoryLocationService } from '../../location/inventory-location.service';
import { InventoryLocation } from '../../location/inventory-location.model';
import { ConfirmDialogComponent } from '../../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../../shared/empty-state/empty-state.component';
import { CmsStatusBadgeComponent } from '../../../../shared/status-badge/status-badge.component';
import { CmsRowActionButtonComponent } from '../../../../shared/row-action-button/row-action-button.component';
import { CmsIconEditComponent, CmsIconToggleStatusComponent } from '../../../../shared/icons';
import { ToastService } from '../../../../core/toast/toast.service';

@Component({
  selector: 'app-reorder-config-list',
  standalone: true,
  imports: [
    FormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatDialogModule,
    CmsEmptyStateComponent,
    CmsStatusBadgeComponent,
    CmsRowActionButtonComponent,
    CmsIconEditComponent,
    CmsIconToggleStatusComponent,
  ],
  templateUrl: './reorder-config-list.component.html',
  styleUrl: './reorder-config-list.component.scss',
})
export class ReorderConfigListComponent implements OnInit, OnDestroy {
  private readonly configService   = inject(ReorderConfigService);
  private readonly locationService = inject(InventoryLocationService);
  private readonly router          = inject(Router);
  private readonly toast           = inject(ToastService);
  private readonly dialog          = inject(MatDialog);

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

  protected readonly displayedColumns = ['productName', 'locationVirtualName', 'reorderLevel', 'reorderQty', 'maxStockQty', 'autoIndentEnabled', 'isActive', 'actions'];
  protected readonly dataSource = new MatTableDataSource<ProductLocationReorderConfig>([]);
  protected readonly loading = signal(false);
  protected readonly locations = signal<InventoryLocation[]>([]);

  protected locationFilter: number | null = null;
  protected totalElements = 0;
  protected currentPage = 0;
  protected currentPageSize = 25;

  ngOnInit(): void {
    this.locationService.getAll(true).subscribe({
      next: (l) => this.locations.set(l.filter(loc => loc.locationRole === 'REQUESTING_POINT' || loc.locationRole === 'BOTH')),
    });
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

  protected goToNew(): void {
    void this.router.navigate(['/inventory/stock/reorder-configs/new']);
  }

  protected editConfig(item: ProductLocationReorderConfig): void {
    void this.router.navigate(['/inventory/stock/reorder-configs', item.id, 'edit']);
  }

  protected toggleStatus(item: ProductLocationReorderConfig): void {
    const nextAction = item.isActive ? 'Deactivate' : 'Activate';
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: `${nextAction} Reorder Configuration`,
        message: `${nextAction} the reorder configuration for "${item.productName}" at "${item.locationVirtualName}"?`,
        confirmText: nextAction,
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe(confirmed => {
      if (confirmed) this.performToggle(item);
    });
  }

  protected handleEmptyAction(): void {
    this.goToNew();
  }

  private performToggle(item: ProductLocationReorderConfig): void {
    this.loading.set(true);
    this.configService.updateStatus(item.id, { isActive: !item.isActive }).subscribe({
      next: () => {
        this.toast.success(`Reorder configuration ${item.isActive ? 'deactivated' : 'activated'} successfully`);
        this.loadPage();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? `Failed to ${item.isActive ? 'deactivate' : 'activate'} reorder configuration`);
        this.loading.set(false);
      },
    });
  }

  private loadPage(): void {
    this.loading.set(true);
    this.configService.getPage({
      locationId: this.locationFilter,
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
      error: () => { this.toast.error('Failed to load reorder configurations'); this.loading.set(false); },
    });
  }
}

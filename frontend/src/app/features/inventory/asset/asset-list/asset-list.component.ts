import { Component, inject, OnInit, OnDestroy, ViewChild, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { Subject, Subscription } from 'rxjs';
import { AssetService } from '../asset.service';
import { Asset, AssetStatus } from '../asset.model';
import { InventoryLocationService } from '../../location/inventory-location.service';
import { InventoryLocation } from '../../location/inventory-location.model';
import { CmsEmptyStateComponent } from '../../../../shared/empty-state/empty-state.component';
import { CmsRowActionButtonComponent } from '../../../../shared/row-action-button/row-action-button.component';
import { CmsIconEditComponent } from '../../../../shared/icons';
import { ToastService } from '../../../../core/toast/toast.service';

const STATUS_OPTIONS: AssetStatus[] = ['AVAILABLE', 'IN_USE', 'UNDER_MAINTENANCE', 'RETIRED', 'DISPOSED'];

@Component({
  selector: 'app-asset-list',
  standalone: true,
  imports: [
    FormsModule,
    DatePipe,
    DecimalPipe,
    MatTableModule,
    MatPaginatorModule,
    CmsEmptyStateComponent,
    CmsRowActionButtonComponent,
    CmsIconEditComponent,
  ],
  templateUrl: './asset-list.component.html',
  styleUrl: './asset-list.component.scss',
})
export class AssetListComponent implements OnInit, OnDestroy {
  private readonly assetService    = inject(AssetService);
  private readonly locationService = inject(InventoryLocationService);
  private readonly router          = inject(Router);
  private readonly toast           = inject(ToastService);

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

  protected readonly statusOptions = STATUS_OPTIONS;
  protected readonly displayedColumns = ['assetTag', 'productName', 'locationVirtualName', 'status', 'purchaseDate', 'currentBookValue', 'actions'];
  protected readonly dataSource = new MatTableDataSource<Asset>([]);
  protected readonly loading = signal(false);
  protected readonly locations = signal<InventoryLocation[]>([]);

  protected locationFilter: number | null = null;
  protected statusFilter: string | null = null;
  protected search = '';
  protected totalElements = 0;
  protected currentPage = 0;
  protected currentPageSize = 25;

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

  protected goToNew(): void {
    void this.router.navigate(['/inventory/asset/assets/new']);
  }

  protected editAsset(item: Asset): void {
    void this.router.navigate(['/inventory/asset/assets', item.id, 'edit']);
  }

  protected onStatusChange(item: Asset, status: string): void {
    this.assetService.updateStatus(item.id, { status: status as AssetStatus }).subscribe({
      next: (updated) => {
        item.status = updated.status;
        this.toast.success('Asset status updated');
      },
      error: (err) => this.toast.error(err?.error?.message ?? 'Failed to update asset status'),
    });
  }

  private loadPage(): void {
    this.loading.set(true);
    this.assetService.getPage({
      locationId: this.locationFilter,
      status: this.statusFilter,
      search: this.search || null,
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
      error: () => { this.toast.error('Failed to load assets'); this.loading.set(false); },
    });
  }
}

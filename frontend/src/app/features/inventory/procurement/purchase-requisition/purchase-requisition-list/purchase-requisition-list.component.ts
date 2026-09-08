import { Component, inject, OnInit, OnDestroy, ViewChild, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { Subject, Subscription } from 'rxjs';
import { PurchaseRequisitionService } from '../purchase-requisition.service';
import { PurchaseRequisition } from '../purchase-requisition.model';
import { InventoryLocationService } from '../../../location/inventory-location.service';
import { InventoryLocation } from '../../../location/inventory-location.model';
import { CmsEmptyStateComponent } from '../../../../../shared/empty-state/empty-state.component';
import { CmsStatusBadgeComponent } from '../../../../../shared/status-badge/status-badge.component';
import { CmsRowActionButtonComponent } from '../../../../../shared/row-action-button/row-action-button.component';
import { CmsIconViewComponent } from '../../../../../shared/icons';
import { ToastService } from '../../../../../core/toast/toast.service';

@Component({
  selector: 'app-purchase-requisition-list',
  standalone: true,
  imports: [
    FormsModule,
    DatePipe,
    MatTableModule,
    MatPaginatorModule,
    CmsEmptyStateComponent,
    CmsStatusBadgeComponent,
    CmsRowActionButtonComponent,
    CmsIconViewComponent,
  ],
  templateUrl: './purchase-requisition-list.component.html',
  styleUrl: './purchase-requisition-list.component.scss',
})
export class PurchaseRequisitionListComponent implements OnInit, OnDestroy {
  private readonly requisitionService = inject(PurchaseRequisitionService);
  private readonly locationService    = inject(InventoryLocationService);
  private readonly router             = inject(Router);
  private readonly toast              = inject(ToastService);

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

  protected readonly displayedColumns = ['requisitionDate', 'locationVirtualName', 'status', 'lineCount', 'pendingCount', 'actions'];
  protected readonly dataSource = new MatTableDataSource<PurchaseRequisition>([]);
  protected readonly loading = signal(false);
  protected readonly locations = signal<InventoryLocation[]>([]);

  protected locationFilter: number | null = null;
  protected statusFilter: string | null = null;
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
    void this.router.navigate(['/inventory/procurement/purchase-requisitions/new']);
  }

  protected openRequisition(item: PurchaseRequisition): void {
    void this.router.navigate(['/inventory/procurement/purchase-requisitions', item.id]);
  }

  private loadPage(): void {
    this.loading.set(true);
    this.requisitionService.getPage({
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
      error: () => { this.toast.error('Failed to load purchase requisitions'); this.loading.set(false); },
    });
  }
}

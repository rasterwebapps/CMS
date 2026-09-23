import { Component, inject, OnInit, OnDestroy, ViewChild, signal, computed } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { PurchaseOrderService } from '../purchase-order.service';
import { PurchaseOrder } from '../purchase-order.model';
import { SupplierService } from '../../supplier/supplier.service';
import { Supplier } from '../../supplier/supplier.model';
import { CmsEmptyStateComponent } from '../../../../../shared/empty-state/empty-state.component';
import { CmsStatusBadgeComponent } from '../../../../../shared/status-badge/status-badge.component';
import { CmsRowActionButtonComponent } from '../../../../../shared/row-action-button/row-action-button.component';
import { CmsIconViewComponent } from '../../../../../shared/icons';
import { ConfirmDialogComponent } from '../../../../../shared/confirm-dialog/confirm-dialog.component';
import { ToastService } from '../../../../../core/toast/toast.service';
import { PermissionService } from '../../../../../core/permissions/permission.service';

@Component({
  selector: 'app-purchase-order-list',
  standalone: true,
  imports: [
    FormsModule,
    DatePipe,
    DecimalPipe,
    MatTableModule,
    MatPaginatorModule,
    MatDialogModule,
    CmsEmptyStateComponent,
    CmsStatusBadgeComponent,
    CmsRowActionButtonComponent,
    CmsIconViewComponent,
  ],
  templateUrl: './purchase-order-list.component.html',
  styleUrl: './purchase-order-list.component.scss',
})
export class PurchaseOrderListComponent implements OnInit, OnDestroy {
  private readonly orderService      = inject(PurchaseOrderService);
  private readonly supplierService   = inject(SupplierService);
  private readonly router            = inject(Router);
  private readonly toast             = inject(ToastService);
  private readonly dialog            = inject(MatDialog);
  private readonly permissionService = inject(PermissionService);

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
    this._paginatorSub = p.page.pipe().subscribe((e: PageEvent) => {
      this.currentPage = e.pageIndex;
      this.currentPageSize = e.pageSize;
      this.loadPage();
    });
  }

  protected readonly displayedColumns = ['poNumber', 'poDate', 'supplierName', 'locationVirtualName', 'status', 'lineCount', 'totalAmount', 'actions'];
  protected readonly dataSource = new MatTableDataSource<PurchaseOrder>([]);
  protected readonly loading = signal(false);
  protected readonly suppliers = signal<Supplier[]>([]);
  protected readonly searchValue = signal('');
  protected readonly canRegenerateNumbers = computed(() => this.permissionService.has('INVENTORY_PURCHASE_ORDER_REGENERATE_NUMBERS'));
  protected readonly regeneratingNumbers = signal(false);

  protected supplierFilter: number | null = null;
  protected statusFilter: string | null = null;
  protected totalElements = 0;
  protected currentPage = 0;
  protected currentPageSize = 25;

  ngOnInit(): void {
    this.supplierService.getAll(true).subscribe({ next: (s) => this.suppliers.set(s) });
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

  protected onFilterChange(): void {
    this.currentPage = 0;
    this.loadPage();
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

  /** Bulk-reassigns every purchase order's number — previews the full before/after mapping first,
   *  then asks for confirmation before writing anything (this rewrites production data). */
  protected regenerateNumbers(): void {
    this.regeneratingNumbers.set(true);
    this.orderService.previewRegenerateNumbers().subscribe({
      next: (preview) => {
        this.regeneratingNumbers.set(false);
        if (preview.totalChanged === 0) {
          this.toast.success('Every purchase order number already matches the current sequence — nothing to change');
          return;
        }
        this.dialog.open(ConfirmDialogComponent, {
          data: {
            title: 'Regenerate Purchase Order Numbers?',
            message: `This will reassign numbers for ${preview.totalChanged} purchase order(s). Old numbers stop working immediately — `
              + 'make sure a backup was taken first. This cannot be undone.',
            confirmText: 'Regenerate',
            cancelText: 'Cancel',
          },
        }).afterClosed().subscribe((confirmed) => {
          if (!confirmed) return;
          this.regeneratingNumbers.set(true);
          this.orderService.regenerateNumbers().subscribe({
            next: (result) => {
              this.toast.success(`Regenerated ${result.totalChanged} purchase order number(s)`);
              this.regeneratingNumbers.set(false);
              this.loadPage();
            },
            error: (err) => {
              this.regeneratingNumbers.set(false);
              this.toast.error(err?.error?.message ?? 'Failed to regenerate purchase order numbers');
            },
          });
        });
      },
      error: (err) => {
        this.regeneratingNumbers.set(false);
        this.toast.error(err?.error?.message ?? 'Failed to preview purchase order number regeneration');
      },
    });
  }

  protected goToNew(): void {
    void this.router.navigate(['/inventory/procurement/purchase-orders/new']);
  }

  protected openOrder(item: PurchaseOrder): void {
    void this.router.navigate(['/inventory/procurement/purchase-orders', item.id]);
  }

  private loadPage(): void {
    this.loading.set(true);
    this.orderService.getPage({
      supplierId: this.supplierFilter,
      status: this.statusFilter,
      search: this.searchValue().trim() || undefined,
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
      error: () => { this.toast.error('Failed to load purchase orders'); this.loading.set(false); },
    });
  }
}

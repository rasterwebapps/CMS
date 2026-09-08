import { Component, inject, OnInit, OnDestroy, ViewChild, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { Subject, Subscription } from 'rxjs';
import { StockService } from '../stock.service';
import { StockBalance } from '../stock.model';
import { ProductService } from '../../product/product.service';
import { Product } from '../../product/product.model';
import { InventoryLocationService } from '../../location/inventory-location.service';
import { InventoryLocation } from '../../location/inventory-location.model';
import { CmsEmptyStateComponent } from '../../../../shared/empty-state/empty-state.component';
import { ToastService } from '../../../../core/toast/toast.service';

@Component({
  selector: 'app-stock-balance-list',
  standalone: true,
  imports: [
    FormsModule,
    DatePipe,
    DecimalPipe,
    MatTableModule,
    MatPaginatorModule,
    CmsEmptyStateComponent,
  ],
  templateUrl: './stock-balance-list.component.html',
  styleUrl: './stock-balance-list.component.scss',
})
export class StockBalanceListComponent implements OnInit, OnDestroy {
  private readonly stockService    = inject(StockService);
  private readonly productService  = inject(ProductService);
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

  protected readonly displayedColumns = ['productCode', 'productName', 'locationVirtualName', 'batchOrSerialNo', 'expiryDate', 'qtyOnHand', 'valueOnHand', 'lastUpdated'];
  protected readonly dataSource = new MatTableDataSource<StockBalance>([]);
  protected readonly loading = signal(false);
  protected readonly products = signal<Product[]>([]);
  protected readonly locations = signal<InventoryLocation[]>([]);

  protected productFilter: number | null = null;
  protected locationFilter: number | null = null;
  protected totalElements = 0;
  protected currentPage = 0;
  protected currentPageSize = 25;

  ngOnInit(): void {
    this.productService.getPage({ page: 0, size: 500 }).subscribe({ next: (p) => this.products.set(p.content) });
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

  protected goToRecordMovement(): void {
    void this.router.navigate(['/inventory/stock/movements/new']);
  }

  private loadPage(): void {
    this.loading.set(true);
    this.stockService.getBalancePage({
      productId: this.productFilter,
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
      error: () => { this.toast.error('Failed to load stock balances'); this.loading.set(false); },
    });
  }
}

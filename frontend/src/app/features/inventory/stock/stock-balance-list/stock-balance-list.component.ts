import { Component, inject, OnInit, OnDestroy, ViewChild, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Subject, Subscription } from 'rxjs';
import { StockService } from '../stock.service';
import { StockBalance, StockBalanceBinBreakdown } from '../stock.model';
import { ProductService } from '../../product/product.service';
import { Product } from '../../product/product.model';
import { InventoryLocationService } from '../../location/inventory-location.service';
import { InventoryLocation } from '../../location/inventory-location.model';
import { ConvertToVariantDialogComponent, ConvertToVariantDialogData } from '../convert-to-variant-dialog/convert-to-variant-dialog.component';
import { CmsEmptyStateComponent } from '../../../../shared/empty-state/empty-state.component';
import { CmsRowActionButtonComponent } from '../../../../shared/row-action-button/row-action-button.component';
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
    MatDialogModule,
    CmsEmptyStateComponent,
    CmsRowActionButtonComponent,
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

  protected readonly displayedColumns = ['productCode', 'productName', 'locationVirtualName', 'batchOrSerialNo', 'expiryDate', 'qtyOnHand', 'valueOnHand', 'lastUpdated', 'actions', 'expand'];
  protected readonly dataSource = new MatTableDataSource<StockBalance>([]);
  protected readonly loading = signal(false);
  protected readonly products = signal<Product[]>([]);
  protected readonly locations = signal<InventoryLocation[]>([]);

  // ── Bin breakdown (expand row) ──────────────────────────────────────────
  protected readonly expandedRow = signal<StockBalance | null>(null);
  protected readonly binBreakdowns = signal<Record<number, StockBalanceBinBreakdown>>({});
  protected readonly loadingBinBreakdown = signal<number | null>(null);

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

  /** A stranded balance: predates its product's first active variant, so it can no longer be
   *  adjusted/issued/transferred through the normal stock-movement API until converted — see the
   *  2026-09-15 "null-variant stock is stranded" decision-log entry. Zero-qty rows are excluded:
   *  the backend always rejects converting an empty balance, so there's nothing left to action. */
  protected isStranded(row: StockBalance): boolean {
    return row.variantId == null && row.productHasActiveVariants && row.qtyOnHand > 0;
  }

  protected isExpanded(row: StockBalance): boolean {
    return this.expandedRow() === row;
  }

  protected toggleRow(row: StockBalance): void {
    if (this.expandedRow() === row) {
      this.expandedRow.set(null);
      return;
    }
    this.expandedRow.set(row);
    if (this.binBreakdowns()[row.id]) return;
    this.loadingBinBreakdown.set(row.id);
    this.stockService.getBinAllocations(row.id).subscribe({
      next: (breakdown) => {
        this.binBreakdowns.update((m) => ({ ...m, [row.id]: breakdown }));
        this.loadingBinBreakdown.set(null);
      },
      error: () => {
        this.toast.error('Failed to load bin breakdown');
        this.loadingBinBreakdown.set(null);
      },
    });
  }

  protected convertToVariant(row: StockBalance): void {
    const data: ConvertToVariantDialogData = { productId: row.productId, productName: row.productName, qtyOnHand: row.qtyOnHand };
    this.dialog.open(ConvertToVariantDialogComponent, { data, width: '420px' }).afterClosed().subscribe((variantId: number | null) => {
      if (!variantId) return;
      this.stockService.convertToVariant(row.id, { variantId }).subscribe({
        next: () => { this.toast.success('Balance converted to variant'); this.loadPage(); },
        error: (err) => this.toast.error(err?.error?.message ?? 'Failed to convert balance'),
      });
    });
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

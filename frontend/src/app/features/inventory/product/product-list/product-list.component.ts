import { Component, inject, OnInit, OnDestroy, ViewChild, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { ProductService } from '../product.service';
import { Product } from '../product.model';
import { CategoryService } from '../../category/category.service';
import { Category } from '../../category/category.model';
import { ConfirmDialogComponent } from '../../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../../shared/empty-state/empty-state.component';
import { ToastService } from '../../../../core/toast/toast.service';
import { CmsViewToggleComponent } from '../../../../shared/view-toggle/view-toggle.component';
import { CmsStatusBadgeComponent } from '../../../../shared/status-badge/status-badge.component';
import { CmsRowActionButtonComponent } from '../../../../shared/row-action-button/row-action-button.component';
import { CmsIconEditComponent, CmsIconToggleStatusComponent } from '../../../../shared/icons';
import { ProductBarcodePreviewDialogComponent, ProductBarcodePreviewDialogData } from '../product-barcode-preview-dialog/product-barcode-preview-dialog.component';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [
    RouterLink,
    FormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatDialogModule,
    MatTooltipModule,
    CmsEmptyStateComponent,
    CmsViewToggleComponent,
    CmsStatusBadgeComponent,
    CmsRowActionButtonComponent,
    CmsIconEditComponent,
    CmsIconToggleStatusComponent,
    ProductBarcodePreviewDialogComponent,
  ],
  templateUrl: './product-list.component.html',
  styleUrl: './product-list.component.scss',
})
export class ProductListComponent implements OnInit, OnDestroy {
  private readonly productService  = inject(ProductService);
  private readonly categoryService = inject(CategoryService);
  private readonly router          = inject(Router);
  private readonly route           = inject(ActivatedRoute);
  private readonly toast           = inject(ToastService);
  private readonly dialog          = inject(MatDialog);

  private readonly VIEW_MODE_KEY = 'product-view-mode';
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

  protected readonly displayedColumns = ['productCode', 'productName', 'categoryName', 'baseUomCode', 'isActive', 'actions'];
  protected readonly dataSource = new MatTableDataSource<Product>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly viewMode = signal<'card' | 'table'>(this.loadViewMode());
  protected readonly categories = signal<Category[]>([]);

  protected readonly barcodeTarget = signal<ProductBarcodePreviewDialogData | null>(null);

  protected categoryFilter: number | null = null;
  protected totalElements = 0;
  protected currentPage = 0;
  protected currentPageSize = 25;
  protected sortActive = 'productName';
  protected sortDirection: 'asc' | 'desc' = 'asc';

  ngOnInit(): void {
    this.categoryService.getAll(true).subscribe({ next: (c) => this.categories.set(c) });

    const snap = this.route.snapshot.queryParams;
    if (snap['sortField']) this.sortActive    = snap['sortField'];
    if (snap['sortDir'])   this.sortDirection = snap['sortDir'] as 'asc' | 'desc';
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

  protected setViewMode(mode: 'card' | 'table'): void {
    this.viewMode.set(mode);
    localStorage.setItem(this.VIEW_MODE_KEY, mode);
  }

  protected onCategoryFilterChange(): void {
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

  protected onSortChange(sort: Sort): void {
    this.sortActive    = sort.active;
    this.sortDirection = (sort.direction || 'asc') as 'asc' | 'desc';
    this.currentPage   = 0;
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { sortField: sort.active, sortDir: this.sortDirection },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
    this.loadPage();
  }

  protected editProduct(item: Product): void {
    void this.router.navigate(['/inventory/products', item.id, 'edit']);
  }

  protected printBarcode(item: Product): void {
    const code = item.barcode?.trim() || item.productCode;
    this.barcodeTarget.set({ id: item.id, productName: item.productName, code });
  }

  protected toggleProductStatus(item: Product): void {
    const nextAction = item.isActive ? 'Deactivate' : 'Activate';
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: `${nextAction} Product`,
        message: `${nextAction} "${item.productName}" (${item.productCode})?`,
        confirmText: nextAction,
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe(confirmed => {
      if (confirmed) this.performToggle(item);
    });
  }

  protected handleEmptyAction(): void {
    if (this.searchValue()) {
      this.clearFilter();
    } else {
      void this.router.navigate(['/inventory/products/new']);
    }
  }

  private loadViewMode(): 'card' | 'table' {
    return localStorage.getItem(this.VIEW_MODE_KEY) === 'table' ? 'table' : 'card';
  }

  private performToggle(item: Product): void {
    this.loading.set(true);
    this.productService.updateStatus(item.id, { isActive: !item.isActive }).subscribe({
      next: () => {
        this.toast.success(`Product ${item.isActive ? 'deactivated' : 'activated'} successfully`);
        this.loadPage();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? `Failed to ${item.isActive ? 'deactivate' : 'activate'} product`);
        this.loading.set(false);
      },
    });
  }

  private loadPage(): void {
    this.loading.set(true);
    const search = this.searchValue().trim() || undefined;
    this.productService.getPage({
      search,
      categoryId: this.categoryFilter,
      page: this.currentPage,
      size: this.currentPageSize,
      sort: this.sortActive,
      direction: this.sortDirection,
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
      error: () => { this.toast.error('Failed to load products'); this.loading.set(false); },
    });
  }
}

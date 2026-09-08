import { Component, inject, OnInit, OnDestroy, ViewChild, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { Subject, Subscription } from 'rxjs';
import { SupplierReturnService } from '../supplier-return.service';
import { SupplierReturn } from '../supplier-return.model';
import { CmsEmptyStateComponent } from '../../../../../shared/empty-state/empty-state.component';
import { CmsStatusBadgeComponent } from '../../../../../shared/status-badge/status-badge.component';
import { CmsRowActionButtonComponent } from '../../../../../shared/row-action-button/row-action-button.component';
import { CmsIconViewComponent } from '../../../../../shared/icons';
import { ToastService } from '../../../../../core/toast/toast.service';

@Component({
  selector: 'app-supplier-return-list',
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
  templateUrl: './supplier-return-list.component.html',
  styleUrl: './supplier-return-list.component.scss',
})
export class SupplierReturnListComponent implements OnInit, OnDestroy {
  private readonly returnService = inject(SupplierReturnService);
  private readonly router        = inject(Router);
  private readonly toast         = inject(ToastService);

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

  protected readonly displayedColumns = ['returnDate', 'goodsReceiptId', 'supplierName', 'locationVirtualName', 'status', 'lineCount', 'actions'];
  protected readonly dataSource = new MatTableDataSource<SupplierReturn>([]);
  protected readonly loading = signal(false);

  protected statusFilter: string | null = null;
  protected totalElements = 0;
  protected currentPage = 0;
  protected currentPageSize = 25;

  ngOnInit(): void {
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
    void this.router.navigate(['/inventory/receiving/supplier-returns/new']);
  }

  protected openReturn(item: SupplierReturn): void {
    void this.router.navigate(['/inventory/receiving/supplier-returns', item.id]);
  }

  private loadPage(): void {
    this.loading.set(true);
    this.returnService.getPage({
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
      error: () => { this.toast.error('Failed to load supplier returns'); this.loading.set(false); },
    });
  }
}

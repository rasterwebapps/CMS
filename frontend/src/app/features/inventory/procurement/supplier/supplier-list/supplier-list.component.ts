import { Component, inject, OnInit, OnDestroy, ViewChild, signal, computed } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { SupplierService } from '../supplier.service';
import { Supplier } from '../supplier.model';
import { ConfirmDialogComponent } from '../../../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../../../shared/empty-state/empty-state.component';
import { CmsStatusBadgeComponent } from '../../../../../shared/status-badge/status-badge.component';
import { CmsRowActionButtonComponent } from '../../../../../shared/row-action-button/row-action-button.component';
import { CmsIconEditComponent, CmsIconToggleStatusComponent } from '../../../../../shared/icons';
import { PermissionService } from '../../../../../core/permissions/permission.service';
import { ToastService } from '../../../../../core/toast/toast.service';

@Component({
  selector: 'app-supplier-list',
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
  templateUrl: './supplier-list.component.html',
  styleUrl: './supplier-list.component.scss',
})
export class SupplierListComponent implements OnInit, OnDestroy {
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
    this._paginatorSub = p.page.pipe(takeUntil(this.destroy$)).subscribe((e: PageEvent) => {
      this.currentPage = e.pageIndex;
      this.currentPageSize = e.pageSize;
      this.loadPage();
    });
  }

  protected readonly displayedColumns = ['supplierCode', 'supplierName', 'contactPerson', 'isApproved', 'isActive', 'actions'];
  protected readonly dataSource = new MatTableDataSource<Supplier>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly canApprove = computed(() => this.permissionService.has('INVENTORY_SUPPLIER_APPROVE'));

  protected totalElements = 0;
  protected currentPage = 0;
  protected currentPageSize = 25;

  ngOnInit(): void {
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

  protected applyFilter(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchValue.set(value);
    this.searchSubject.next(value);
  }

  protected clearFilter(): void {
    this.searchValue.set('');
    this.searchSubject.next('');
  }

  protected goToNew(): void {
    void this.router.navigate(['/inventory/procurement/suppliers/new']);
  }

  protected editSupplier(item: Supplier): void {
    void this.router.navigate(['/inventory/procurement/suppliers', item.id, 'edit']);
  }

  protected toggleStatus(item: Supplier): void {
    const nextAction = item.isActive ? 'Deactivate' : 'Activate';
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: `${nextAction} Supplier`,
        message: `${nextAction} "${item.supplierName}"?`,
        confirmText: nextAction,
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe(confirmed => {
      if (confirmed) this.performToggle(item);
    });
  }

  protected approveSupplier(item: Supplier): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Approve Supplier',
        message: `Approve "${item.supplierName}" for use in purchasing?`,
        confirmText: 'Approve',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.loading.set(true);
      this.supplierService.approve(item.id).subscribe({
        next: () => { this.toast.success('Supplier approved'); this.loadPage(); },
        error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to approve supplier'); this.loading.set(false); },
      });
    });
  }

  protected handleEmptyAction(): void {
    if (this.searchValue()) {
      this.clearFilter();
    } else {
      this.goToNew();
    }
  }

  private performToggle(item: Supplier): void {
    this.loading.set(true);
    this.supplierService.updateStatus(item.id, { isActive: !item.isActive }).subscribe({
      next: () => {
        this.toast.success(`Supplier ${item.isActive ? 'deactivated' : 'activated'} successfully`);
        this.loadPage();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? `Failed to ${item.isActive ? 'deactivate' : 'activate'} supplier`);
        this.loading.set(false);
      },
    });
  }

  private loadPage(): void {
    this.loading.set(true);
    const search = this.searchValue().trim() || undefined;
    this.supplierService.getPage({ search, page: this.currentPage, size: this.currentPageSize }).subscribe({
      next: (page) => {
        this.dataSource.data = page.content;
        this.totalElements = page.totalElements;
        if (this._paginator) {
          this._paginator.length = page.totalElements;
          this._paginator.pageIndex = page.number;
        }
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load suppliers'); this.loading.set(false); },
    });
  }
}

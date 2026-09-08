import { Component, inject, OnInit, OnDestroy, ViewChild, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Subject, Subscription } from 'rxjs';
import { RateContractService } from '../rate-contract.service';
import { RateContract } from '../rate-contract.model';
import { SupplierService } from '../../supplier/supplier.service';
import { Supplier } from '../../supplier/supplier.model';
import { ConfirmDialogComponent } from '../../../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../../../shared/empty-state/empty-state.component';
import { CmsStatusBadgeComponent } from '../../../../../shared/status-badge/status-badge.component';
import { CmsRowActionButtonComponent } from '../../../../../shared/row-action-button/row-action-button.component';
import { CmsIconEditComponent, CmsIconToggleStatusComponent } from '../../../../../shared/icons';
import { ToastService } from '../../../../../core/toast/toast.service';

@Component({
  selector: 'app-rate-contract-list',
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
    CmsIconEditComponent,
    CmsIconToggleStatusComponent,
  ],
  templateUrl: './rate-contract-list.component.html',
  styleUrl: './rate-contract-list.component.scss',
})
export class RateContractListComponent implements OnInit, OnDestroy {
  private readonly rateContractService = inject(RateContractService);
  private readonly supplierService     = inject(SupplierService);
  private readonly router              = inject(Router);
  private readonly toast                = inject(ToastService);
  private readonly dialog               = inject(MatDialog);

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

  protected readonly displayedColumns = ['supplierName', 'startDate', 'endDate', 'contractValueCap', 'isActive', 'actions'];
  protected readonly dataSource = new MatTableDataSource<RateContract>([]);
  protected readonly loading = signal(false);
  protected readonly suppliers = signal<Supplier[]>([]);

  protected supplierFilter: number | null = null;
  protected totalElements = 0;
  protected currentPage = 0;
  protected currentPageSize = 25;

  ngOnInit(): void {
    this.supplierService.getAll(true).subscribe({ next: (s) => this.suppliers.set(s) });
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
    void this.router.navigate(['/inventory/procurement/rate-contracts/new']);
  }

  protected editContract(item: RateContract): void {
    void this.router.navigate(['/inventory/procurement/rate-contracts', item.id, 'edit']);
  }

  protected toggleStatus(item: RateContract): void {
    const nextAction = item.isActive ? 'Deactivate' : 'Activate';
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: `${nextAction} Rate Contract`,
        message: `${nextAction} the rate contract with "${item.supplierName}"?`,
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

  private performToggle(item: RateContract): void {
    this.loading.set(true);
    this.rateContractService.updateStatus(item.id, { isActive: !item.isActive }).subscribe({
      next: () => {
        this.toast.success(`Rate contract ${item.isActive ? 'deactivated' : 'activated'} successfully`);
        this.loadPage();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? `Failed to ${item.isActive ? 'deactivate' : 'activate'} rate contract`);
        this.loading.set(false);
      },
    });
  }

  private loadPage(): void {
    this.loading.set(true);
    this.rateContractService.getPage({
      supplierId: this.supplierFilter,
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
      error: () => { this.toast.error('Failed to load rate contracts'); this.loading.set(false); },
    });
  }
}

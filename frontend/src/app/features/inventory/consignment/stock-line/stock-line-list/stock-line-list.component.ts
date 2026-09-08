import { Component, inject, OnInit, OnDestroy, ViewChild, computed, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { Subject, Subscription } from 'rxjs';
import { ConsignmentStockLineService } from '../stock-line.service';
import { ConsignmentStockLine } from '../stock-line.model';
import { ConsignmentAgreementService } from '../../agreement/agreement.service';
import { ConsignmentAgreement } from '../../agreement/agreement.model';
import { StockLineReceiveDialogComponent } from '../stock-line-receive-dialog/stock-line-receive-dialog.component';
import { StockLineConsumeDialogComponent } from '../stock-line-consume-dialog/stock-line-consume-dialog.component';
import { CmsEmptyStateComponent } from '../../../../../shared/empty-state/empty-state.component';
import { CmsRowActionButtonComponent } from '../../../../../shared/row-action-button/row-action-button.component';
import { CmsIconViewComponent } from '../../../../../shared/icons';
import { PermissionService } from '../../../../../core/permissions/permission.service';
import { ToastService } from '../../../../../core/toast/toast.service';

@Component({
  selector: 'app-consignment-stock-line-list',
  standalone: true,
  imports: [
    FormsModule,
    DatePipe,
    DecimalPipe,
    MatDialogModule,
    MatTableModule,
    MatPaginatorModule,
    CmsEmptyStateComponent,
    CmsRowActionButtonComponent,
    CmsIconViewComponent,
  ],
  templateUrl: './stock-line-list.component.html',
  styleUrl: './stock-line-list.component.scss',
})
export class ConsignmentStockLineListComponent implements OnInit, OnDestroy {
  private readonly lineService      = inject(ConsignmentStockLineService);
  private readonly agreementService = inject(ConsignmentAgreementService);
  private readonly dialog           = inject(MatDialog);
  private readonly permissionService = inject(PermissionService);
  private readonly toast            = inject(ToastService);

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

  protected readonly displayedColumns = ['productName', 'agreementNumber', 'consignmentPrice', 'receivedQty', 'consumedQty', 'qtyOnHand', 'actions'];
  protected readonly dataSource = new MatTableDataSource<ConsignmentStockLine>([]);
  protected readonly loading = signal(false);
  protected readonly busy = signal(false);
  protected readonly agreements = signal<ConsignmentAgreement[]>([]);

  protected readonly canManage = computed(() => this.permissionService.has('INVENTORY_CONSIGNMENT_MANAGE'));
  protected readonly canConvert = computed(() => this.permissionService.has('INVENTORY_CONSIGNMENT_CONVERT'));

  protected agreementFilter: number | null = null;
  protected totalElements = 0;
  protected currentPage = 0;
  protected currentPageSize = 25;

  ngOnInit(): void {
    this.agreementService.getAllActive().subscribe({ next: (page) => this.agreements.set(page.content) });
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

  protected openReceiveDialog(): void {
    this.dialog.open(StockLineReceiveDialogComponent, { data: { agreementId: this.agreementFilter } })
      .afterClosed().subscribe((result) => {
        if (!result) return;
        this.busy.set(true);
        this.lineService.receive(result).subscribe({
          next: () => { this.toast.success('Consignment stock received'); this.busy.set(false); this.loadPage(); },
          error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to receive consignment stock'); this.busy.set(false); },
        });
      });
  }

  protected openConsumeDialog(line: ConsignmentStockLine): void {
    this.dialog.open(StockLineConsumeDialogComponent, { data: { productName: line.productName, qtyOnHand: line.qtyOnHand } })
      .afterClosed().subscribe((result) => {
        if (!result) return;
        this.busy.set(true);
        this.lineService.consume(line.id, result).subscribe({
          next: () => { this.toast.success('Consumption recorded'); this.busy.set(false); this.loadPage(); },
          error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to record consumption'); this.busy.set(false); },
        });
      });
  }

  private loadPage(): void {
    this.loading.set(true);
    this.lineService.getPage({
      agreementId: this.agreementFilter,
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
      error: () => { this.toast.error('Failed to load consignment stock'); this.loading.set(false); },
    });
  }
}

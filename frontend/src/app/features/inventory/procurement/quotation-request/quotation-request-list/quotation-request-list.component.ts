import { Component, inject, OnInit, OnDestroy, ViewChild, signal, computed } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { QuotationRequestService } from '../quotation-request.service';
import { QuotationRequest } from '../quotation-request.model';
import { InventoryLocationService } from '../../../location/inventory-location.service';
import { InventoryLocation } from '../../../location/inventory-location.model';
import { CmsEmptyStateComponent } from '../../../../../shared/empty-state/empty-state.component';
import { CmsStatusBadgeComponent } from '../../../../../shared/status-badge/status-badge.component';
import { CmsRowActionButtonComponent } from '../../../../../shared/row-action-button/row-action-button.component';
import { CmsIconViewComponent } from '../../../../../shared/icons';
import { ConfirmDialogComponent } from '../../../../../shared/confirm-dialog/confirm-dialog.component';
import { ToastService } from '../../../../../core/toast/toast.service';
import { PermissionService } from '../../../../../core/permissions/permission.service';

@Component({
  selector: 'app-quotation-request-list',
  standalone: true,
  imports: [
    FormsModule,
    DatePipe,
    MatTableModule,
    MatPaginatorModule,
    MatDialogModule,
    CmsEmptyStateComponent,
    CmsStatusBadgeComponent,
    CmsRowActionButtonComponent,
    CmsIconViewComponent,
  ],
  templateUrl: './quotation-request-list.component.html',
  styleUrl: './quotation-request-list.component.scss',
})
export class QuotationRequestListComponent implements OnInit, OnDestroy {
  private readonly requestService    = inject(QuotationRequestService);
  private readonly locationService   = inject(InventoryLocationService);
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

  protected readonly displayedColumns = ['quotationNumber', 'requestDate', 'locationVirtualName', 'status', 'lineCount', 'pendingCount', 'actions'];
  protected readonly dataSource = new MatTableDataSource<QuotationRequest>([]);
  protected readonly loading = signal(false);
  protected readonly locations = signal<InventoryLocation[]>([]);
  protected readonly searchValue = signal('');
  protected readonly canRegenerateNumbers = computed(() => this.permissionService.has('INVENTORY_QUOTATION_REGENERATE_NUMBERS'));
  protected readonly regeneratingNumbers = signal(false);

  protected locationFilter: number | null = null;
  protected statusFilter: string | null = null;
  protected totalElements = 0;
  protected currentPage = 0;
  protected currentPageSize = 25;

  ngOnInit(): void {
    this.locationService.getAll(true).subscribe({ next: (l) => this.locations.set(l) });
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

  protected regenerateNumbers(): void {
    this.regeneratingNumbers.set(true);
    this.requestService.previewRegenerateNumbers().subscribe({
      next: (preview) => {
        this.regeneratingNumbers.set(false);
        if (preview.totalChanged === 0) {
          this.toast.success('Every quotation number already matches the current sequence — nothing to change');
          return;
        }
        this.dialog.open(ConfirmDialogComponent, {
          data: {
            title: 'Regenerate Quotation Numbers?',
            message: `This will reassign numbers for ${preview.totalChanged} quotation request(s). Old numbers stop working immediately — `
              + 'make sure a backup was taken first. This cannot be undone.',
            confirmText: 'Regenerate',
            cancelText: 'Cancel',
          },
        }).afterClosed().subscribe((confirmed) => {
          if (!confirmed) return;
          this.regeneratingNumbers.set(true);
          this.requestService.regenerateNumbers().subscribe({
            next: (result) => {
              this.toast.success(`Regenerated ${result.totalChanged} quotation number(s)`);
              this.regeneratingNumbers.set(false);
              this.loadPage();
            },
            error: (err) => {
              this.regeneratingNumbers.set(false);
              this.toast.error(err?.error?.message ?? 'Failed to regenerate quotation numbers');
            },
          });
        });
      },
      error: (err) => {
        this.regeneratingNumbers.set(false);
        this.toast.error(err?.error?.message ?? 'Failed to preview quotation number regeneration');
      },
    });
  }

  protected goToNew(): void {
    void this.router.navigate(['/inventory/procurement/quotation-requests/new']);
  }

  protected openRequest(item: QuotationRequest): void {
    void this.router.navigate(['/inventory/procurement/quotation-requests', item.id]);
  }

  private loadPage(): void {
    this.loading.set(true);
    this.requestService.getPage({
      locationId: this.locationFilter,
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
      error: () => { this.toast.error('Failed to load quotation requests'); this.loading.set(false); },
    });
  }
}

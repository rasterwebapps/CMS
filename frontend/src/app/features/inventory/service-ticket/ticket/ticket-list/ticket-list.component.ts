import { Component, inject, OnInit, OnDestroy, ViewChild, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { Subject, Subscription } from 'rxjs';
import { ServiceTicketService } from '../ticket.service';
import { ServiceTicket } from '../ticket.model';
import { ServiceTicketCategoryService } from '../../category/category.service';
import { ServiceTicketCategory } from '../../category/category.model';
import { InventoryLocationService } from '../../../location/inventory-location.service';
import { InventoryLocation } from '../../../location/inventory-location.model';
import { CmsEmptyStateComponent } from '../../../../../shared/empty-state/empty-state.component';
import { CmsStatusBadgeComponent } from '../../../../../shared/status-badge/status-badge.component';
import { CmsRowActionButtonComponent } from '../../../../../shared/row-action-button/row-action-button.component';
import { CmsIconViewComponent } from '../../../../../shared/icons';
import { ToastService } from '../../../../../core/toast/toast.service';

@Component({
  selector: 'app-service-ticket-list',
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
  templateUrl: './ticket-list.component.html',
  styleUrl: './ticket-list.component.scss',
})
export class ServiceTicketListComponent implements OnInit, OnDestroy {
  private readonly ticketService   = inject(ServiceTicketService);
  private readonly categoryService = inject(ServiceTicketCategoryService);
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

  protected readonly displayedColumns = ['description', 'categoryName', 'locationVirtualName', 'priority', 'status', 'actions'];
  protected readonly dataSource = new MatTableDataSource<ServiceTicket>([]);
  protected readonly loading = signal(false);
  protected readonly locations = signal<InventoryLocation[]>([]);
  protected readonly categories = signal<ServiceTicketCategory[]>([]);

  protected locationFilter: number | null = null;
  protected categoryFilter: number | null = null;
  protected statusFilter: string | null = null;
  protected priorityFilter: string | null = null;
  protected totalElements = 0;
  protected currentPage = 0;
  protected currentPageSize = 25;

  ngOnInit(): void {
    this.locationService.getAll(true).subscribe({ next: (l) => this.locations.set(l) });
    this.categoryService.getAll(true).subscribe({ next: (c) => this.categories.set(c) });
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
    void this.router.navigate(['/inventory/ticket/tickets/new']);
  }

  protected openTicket(ticket: ServiceTicket): void {
    void this.router.navigate(['/inventory/ticket/tickets', ticket.id]);
  }

  private loadPage(): void {
    this.loading.set(true);
    this.ticketService.getPage({
      locationId: this.locationFilter,
      categoryId: this.categoryFilter,
      status: this.statusFilter,
      priority: this.priorityFilter,
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
      error: () => { this.toast.error('Failed to load service tickets'); this.loading.set(false); },
    });
  }
}

import { Component, inject, OnInit, OnDestroy, ViewChild, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { Subject, Subscription } from 'rxjs';
import { MaintenanceScheduleService } from '../maintenance-schedule.service';
import { AssetMaintenanceSchedule } from '../maintenance-schedule.model';
import { CmsEmptyStateComponent } from '../../../../../shared/empty-state/empty-state.component';
import { CmsRowActionButtonComponent } from '../../../../../shared/row-action-button/row-action-button.component';
import { CmsIconEditComponent } from '../../../../../shared/icons';
import { ToastService } from '../../../../../core/toast/toast.service';

@Component({
  selector: 'app-maintenance-schedule-list',
  standalone: true,
  imports: [
    FormsModule,
    DatePipe,
    MatTableModule,
    MatPaginatorModule,
    CmsEmptyStateComponent,
    CmsRowActionButtonComponent,
    CmsIconEditComponent,
  ],
  templateUrl: './maintenance-schedule-list.component.html',
  styleUrl: './maintenance-schedule-list.component.scss',
})
export class MaintenanceScheduleListComponent implements OnInit, OnDestroy {
  private readonly scheduleService = inject(MaintenanceScheduleService);
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

  protected readonly displayedColumns = ['assetTag', 'scheduleType', 'nextDueDate', 'lastPerformedDate', 'actions'];
  protected readonly dataSource = new MatTableDataSource<AssetMaintenanceSchedule>([]);
  protected readonly loading = signal(false);

  protected overdueOnly = false;
  protected activeOnly = true;
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
    void this.router.navigate(['/inventory/asset/maintenance-schedules/new']);
  }

  protected editSchedule(item: AssetMaintenanceSchedule): void {
    void this.router.navigate(['/inventory/asset/maintenance-schedules', item.id, 'edit']);
  }

  protected markPerformed(item: AssetMaintenanceSchedule, event: Event): void {
    event.stopPropagation();
    this.scheduleService.markPerformed(item.id, {}).subscribe({
      next: () => { this.toast.success('Maintenance marked performed'); this.loadPage(); },
      error: (err) => this.toast.error(err?.error?.message ?? 'Failed to mark maintenance performed'),
    });
  }

  private loadPage(): void {
    this.loading.set(true);
    this.scheduleService.getPage({
      overdueOnly: this.overdueOnly,
      activeOnly: this.activeOnly,
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
      error: () => { this.toast.error('Failed to load maintenance schedules'); this.loading.set(false); },
    });
  }
}

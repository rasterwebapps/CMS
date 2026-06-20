import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { LibraryService } from '../library.service';
import {
  LibraryPeriodical,
  JournalType,
  SubscriptionStatus,
  JOURNAL_TYPE_OPTIONS,
  SUBSCRIPTION_STATUS_OPTIONS,
} from '../library.model';
import { ToastService } from '../../../core/toast/toast.service';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsTypeBadgeComponent } from '../../../shared/type-badge/type-badge.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { PermissionService } from '../../../core/permissions/permission.service';

@Component({
  selector: 'app-library-periodical-list',
  standalone: true,
  imports: [
    RouterLink,
    DatePipe,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    CmsEmptyStateComponent,
    CmsRowActionButtonComponent,
    CmsTypeBadgeComponent,
    CmsStatusBadgeComponent,
  ],
  templateUrl: './library-periodical-list.component.html',
  styleUrl:    './library-periodical-list.component.scss',
})
export class LibraryPeriodicalListComponent implements OnInit {
  private readonly libraryService = inject(LibraryService);
  private readonly router         = inject(Router);
  private readonly toast          = inject(ToastService);
  private readonly dialog         = inject(MatDialog);
  protected readonly permissions  = inject(PermissionService);

  @ViewChild(MatPaginator) set paginator(v: MatPaginator) { if (v) this.dataSource.paginator = v; }
  @ViewChild(MatSort)      set sort(v: MatSort)           { if (v) this.dataSource.sort = v; }

  protected readonly displayedColumns = [
    'journalName', 'journalType', 'volumeIssue', 'year',
    'copiesCount', 'subscriptionStatus', 'receivedDate', 'actions',
  ];
  protected readonly dataSource   = new MatTableDataSource<LibraryPeriodical>([]);
  protected readonly loading      = signal(false);
  protected readonly searchValue  = signal('');
  protected readonly typeFilter   = signal<JournalType | null>(null);
  protected readonly statusFilter = signal<SubscriptionStatus | null>(null);

  private readonly allItems        = signal<LibraryPeriodical[]>([]);
  protected readonly typeOptions   = JOURNAL_TYPE_OPTIONS;
  protected readonly statusOptions = SUBSCRIPTION_STATUS_OPTIONS;

  protected readonly filteredItems = computed(() => {
    const q      = this.searchValue().toLowerCase().trim();
    const type   = this.typeFilter();
    const status = this.statusFilter();
    return this.allItems().filter(p => {
      if (type   && p.journalType         !== type)   return false;
      if (status && p.subscriptionStatus  !== status) return false;
      if (q && !(
        p.journalName.toLowerCase().includes(q) ||
        (p.organization ?? '').toLowerCase().includes(q) ||
        (p.volumeNumber ?? '').toLowerCase().includes(q)
      )) return false;
      return true;
    });
  });

  protected readonly totalCount  = computed(() => this.allItems().length);
  protected readonly activeCount = computed(() => this.allItems().filter(p => p.subscriptionStatus === 'ACTIVE').length);

  protected readonly hasActiveFilters = computed(() =>
    this.typeFilter() !== null || this.statusFilter() !== null || this.searchValue().length > 0,
  );

  protected readonly canManage = computed(() =>
    this.permissions.hasAny('LIBRARY_PERIODICAL_MANAGE'),
  );

  ngOnInit(): void {
    this.dataSource.sortingDataAccessor = (item, prop) =>
      String((item as unknown as Record<string, unknown>)[prop] ?? '').toLowerCase();
    this.loadPeriodicals();
  }

  protected applyFilter(event: Event): void {
    this.searchValue.set((event.target as HTMLInputElement).value);
    this.syncTable();
  }

  protected clearFilters(): void {
    this.searchValue.set('');
    this.typeFilter.set(null);
    this.statusFilter.set(null);
    this.syncTable();
  }

  protected editItem(p: LibraryPeriodical): void {
    void this.router.navigate(['/library/periodicals', p.id, 'edit']);
  }

  protected deleteItem(p: LibraryPeriodical): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Periodical',
        message: `Delete "${p.journalName}"${p.volumeNumber ? ' Vol. ' + p.volumeNumber : ''}? This cannot be undone.`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe(confirmed => {
      if (confirmed) this.performDelete(p);
    });
  }

  protected handleEmptyAction(): void {
    if (this.hasActiveFilters()) this.clearFilters();
    else void this.router.navigate(['/library/periodicals/new']);
  }

  protected syncTable(): void {
    this.dataSource.data = this.filteredItems();
    this.dataSource.paginator?.firstPage();
  }

  private loadPeriodicals(): void {
    this.loading.set(true);
    this.libraryService.getPeriodicals().subscribe({
      next: items => {
        this.allItems.set(items);
        this.syncTable();
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load periodicals');
        this.loading.set(false);
      },
    });
  }

  private performDelete(p: LibraryPeriodical): void {
    this.libraryService.deletePeriodical(p.id).subscribe({
      next: () => {
        this.toast.success('Periodical entry deleted');
        this.loadPeriodicals();
      },
      error: err => this.toast.error(err?.error?.message ?? 'Failed to delete'),
    });
  }
}

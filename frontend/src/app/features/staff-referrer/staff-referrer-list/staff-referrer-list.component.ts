import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { StaffReferrerService } from '../staff-referrer.service';
import { StaffReferrer } from '../staff-referrer.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsViewToggleComponent } from '../../../shared/view-toggle/view-toggle.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsIconEditComponent, CmsIconToggleStatusComponent } from '../../../shared/icons';

@Component({
  selector: 'app-staff-referrer-list',
  standalone: true,
  imports: [
    RouterLink,
    InrPipe,
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
  ],
  templateUrl: './staff-referrer-list.component.html',
  styleUrl: './staff-referrer-list.component.scss',
})
export class StaffReferrerListComponent implements OnInit {
  private readonly service = inject(StaffReferrerService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  private readonly VIEW_MODE_KEY = 'staff-referrer-view-mode';

  protected readonly displayedColumns = ['name', 'phone', 'institution', 'commissionAmount', 'isActive', 'actions'];
  protected readonly dataSource = new MatTableDataSource<StaffReferrer>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly viewMode = signal<'card' | 'table'>(this.loadViewMode());

  private readonly allItems = signal<StaffReferrer[]>([]);

  protected readonly totalCount = computed(() => this.allItems().length);
  protected readonly activeCount = computed(() => this.allItems().filter(s => s.isActive).length);

  protected readonly filteredItems = computed(() => {
    const q = this.searchValue().trim().toLowerCase();
    if (!q) return this.allItems();
    return this.allItems().filter(s =>
      s.name.toLowerCase().includes(q) ||
      (s.phone?.toLowerCase().includes(q) ?? false) ||
      (s.institution?.toLowerCase().includes(q) ?? false),
    );
  });

  ngOnInit(): void {
    this.load();
  }

  protected setViewMode(mode: 'card' | 'table'): void {
    this.viewMode.set(mode);
    localStorage.setItem(this.VIEW_MODE_KEY, mode);
  }

  protected applyFilter(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchValue.set(value);
    this.dataSource.filter = value.trim().toLowerCase();
    if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
  }

  protected clearFilter(): void {
    this.searchValue.set('');
    this.dataSource.filter = '';
  }

  protected edit(item: StaffReferrer): void {
    void this.router.navigate(['/staff-referrers', item.id, 'edit']);
  }

  protected toggleStatus(item: StaffReferrer): void {
    const nextAction = item.isActive ? 'Deactivate' : 'Activate';
    this.dialog
      .open(ConfirmDialogComponent, {
        data: {
          title: `${nextAction} Staff Referrer`,
          message: `${nextAction} "${item.name}"?`,
          confirmText: nextAction,
          cancelText: 'Cancel',
        },
      })
      .afterClosed()
      .subscribe((confirmed) => { if (confirmed) this.doToggle(item); });
  }

  protected handleEmptyAction(): void {
    if (this.searchValue()) {
      this.clearFilter();
    } else {
      void this.router.navigate(['/staff-referrers/new']);
    }
  }

  private loadViewMode(): 'card' | 'table' {
    const stored = localStorage.getItem(this.VIEW_MODE_KEY);
    return stored === 'table' ? 'table' : 'card';
  }

  private doToggle(item: StaffReferrer): void {
    this.loading.set(true);
    const request$ = item.isActive
      ? this.service.deactivate(item.id)
      : this.service.reactivate(item.id);
    request$.subscribe({
      next: () => {
        this.toast.success(`Staff referrer ${item.isActive ? 'deactivated' : 'activated'} successfully`);
        this.load();
      },
      error: (err) => {
        this.toast.error(
          err?.error?.message ?? `Failed to ${item.isActive ? 'deactivate' : 'activate'} staff referrer`,
        );
        this.loading.set(false);
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.service.getAll().subscribe({
      next: (data) => {
        this.allItems.set(data);
        this.dataSource.data = data;
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load'); this.loading.set(false); },
    });
  }
}

import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { StaffReferrerService } from '../staff-referrer.service';
import { StaffReferrer } from '../staff-referrer.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { ToastService } from '../../../core/toast/toast.service';

@Component({
  selector: 'app-staff-referrer-list',
  standalone: true,
  imports: [
    RouterLink,
    DecimalPipe,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatDialogModule,
    MatTooltipModule,
    CmsEmptyStateComponent,
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

  protected delete(item: StaffReferrer): void {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: { title: 'Delete Staff Referrer', message: `Delete "${item.name}"?`, confirmText: 'Delete', cancelText: 'Cancel' },
      })
      .afterClosed()
      .subscribe((confirmed) => { if (confirmed) this.doDelete(item); });
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

  private doDelete(item: StaffReferrer): void {
    this.loading.set(true);
    this.service.delete(item.id).subscribe({
      next: () => { this.toast.success('Deleted successfully'); this.load(); },
      error: () => { this.toast.error('Failed to delete'); this.loading.set(false); },
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

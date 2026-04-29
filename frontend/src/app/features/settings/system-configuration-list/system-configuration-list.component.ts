import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { SettingsService } from '../settings.service';
import { SystemConfiguration } from '../settings.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';

@Component({
  selector: 'app-system-configuration-list',
  standalone: true,
  imports: [
    CmsEmptyStateComponent,
    RouterLink, MatTableModule, MatPaginatorModule, MatSortModule, CmsTourButtonComponent,
    MatDialogModule, MatTooltipModule,
  ],
  templateUrl: './system-configuration-list.component.html',
  styleUrl: './system-configuration-list.component.scss',
})
export class SystemConfigurationListComponent implements OnInit {
  private readonly settingsService = inject(SettingsService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);

  private readonly VIEW_MODE_KEY = 'settings-view-mode';

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  protected readonly displayedColumns: readonly string[] = ['configKey', 'configValue', 'category', 'dataType', 'isEditable', 'actions'];
  protected readonly dataSource = new MatTableDataSource<SystemConfiguration>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly viewMode = signal<'card' | 'table'>(this.loadViewMode());

  private readonly allSettings = signal<SystemConfiguration[]>([]);

  protected readonly filteredSettings = computed(() => {
    const q = this.searchValue().trim().toLowerCase();
    return this.allSettings().filter(item =>
      !q ||
      item.configKey.toLowerCase().includes(q) ||
      item.configValue.toLowerCase().includes(q) ||
      item.category.toLowerCase().includes(q),
    );
  });

  protected readonly totalCount = computed(() => this.allSettings().length);
  protected readonly editableCount = computed(() => this.allSettings().filter(s => s.isEditable).length);

  ngOnInit(): void { this.load(); }

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

  protected setViewMode(mode: 'card' | 'table'): void {
    this.viewMode.set(mode);
    localStorage.setItem(this.VIEW_MODE_KEY, mode);
  }

  private loadViewMode(): 'card' | 'table' {
    return localStorage.getItem(this.VIEW_MODE_KEY) === 'table' ? 'table' : 'card';
  }

  protected handleEmptyAction(): void {
    if (this.searchValue()) {
      this.clearFilter();
    } else {
      void this.router.navigate(['/settings/new']);
    }
  }

  protected edit(item: SystemConfiguration): void {
    void this.router.navigate(['/settings', item.id, 'edit']);
  }

  protected delete(item: SystemConfiguration): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Delete Configuration', message: `Delete "${item.configKey}"?`, confirmText: 'Delete', cancelText: 'Cancel' },
    }).afterClosed().subscribe((confirmed) => { if (confirmed) this.doDelete(item); });
  }

  private doDelete(item: SystemConfiguration): void {
    this.loading.set(true);
    this.settingsService.delete(item.id).subscribe({
      next: () => { this.toast.success('Deleted successfully'); this.load(); },
      error: () => { this.toast.error('Failed to delete'); this.loading.set(false); },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.settingsService.getAll().subscribe({
      next: (data) => {
        this.allSettings.set(data);
        this.dataSource.data = data;
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load'); this.loading.set(false); },
    });
  }
}

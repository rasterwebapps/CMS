import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource, MatTable } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MaintenanceService } from '../maintenance.service';
import { MaintenanceRequest } from '../maintenance.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsTypeBadgeComponent } from '../../../shared/type-badge/type-badge.component';
import { CmsIconDeleteComponent, CmsIconEditComponent } from '../../../shared/icons';
import { ColumnPickerState, CmsColumnPickerComponent } from '../../../shared/column-picker';

import { ColumnResizeDirective, CmsWrapTextToggleComponent } from '../../../shared/column-resize';
import { TourService } from '../../../shared/tour/tour.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { MAINTENANCE_LIST_TOUR, MAINTENANCE_LIST_FLOW_MAP } from '../../../shared/tour/tours/inventory.tours';

@Component({
  selector: 'app-maintenance-list',
  standalone: true,
  imports: [
    CmsEmptyStateComponent,
    CmsStatusBadgeComponent,
    CmsRowActionButtonComponent,
    CmsTypeBadgeComponent,
    RouterLink, FormsModule, MatTableModule, MatPaginatorModule, MatSortModule,
    MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, MatDialogModule, MatTooltipModule,
    CmsIconDeleteComponent,
    CmsIconEditComponent,
    CmsColumnPickerComponent, ColumnResizeDirective, CmsWrapTextToggleComponent, CmsTourButtonComponent,
  ],
  templateUrl: './maintenance-list.component.html',
  styleUrl: './maintenance-list.component.scss',
})
export class MaintenanceListComponent implements OnInit {
  private readonly maintenanceService = inject(MaintenanceService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly tourService = inject(TourService);
  private readonly dialog = inject(MatDialog);

  @ViewChild(MatTable) private _matTable?: MatTable<unknown>;
  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  protected readonly colState = new ColumnPickerState({
    storageKey: 'maintenance-list-cols',
    columns: [
      { key: 'equipmentName', label: 'Equipment', mandatory: true },
      { key: 'requestedBy', label: 'Req. By' },
      { key: 'priority', label: 'Priority' },
      { key: 'status', label: 'Status' },
      { key: 'assignedTechnician', label: 'Technician' },
      { key: 'createdAt', label: 'Created' },
      { key: 'actions', label: 'Actions', mandatory: true, pinnable: false },
    ],
  });
  protected readonly displayedColumns = computed(() => this.colState.visibleColumns());
  protected readonly dataSource = new MatTableDataSource<MaintenanceRequest>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');

  protected onPinChange(): void { this._matTable?.updateStickyColumnStyles(); }

  ngOnInit(): void {
    this.tourService.register('maintenance-list', MAINTENANCE_LIST_TOUR);
    this.tourService.registerFlowMap('maintenance-list', MAINTENANCE_LIST_FLOW_MAP);
    this.load();
  }

  protected applyFilter(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchValue.set(value);
    this.dataSource.filter = value.trim().toLowerCase();
    if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
  }

  protected clearFilter(): void { this.searchValue.set(''); this.dataSource.filter = ''; }

  protected edit(item: MaintenanceRequest): void { void this.router.navigate(['/maintenance', item.id, 'edit']); }

  protected delete(item: MaintenanceRequest): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Delete Maintenance Request', message: `Delete maintenance request for "${item.equipmentName}"?`, confirmText: 'Delete', cancelText: 'Cancel' },
    }).afterClosed().subscribe((confirmed) => { if (confirmed) this.doDelete(item); });
  }




  private doDelete(item: MaintenanceRequest): void {
    this.loading.set(true);
    this.maintenanceService.delete(item.id).subscribe({
      next: () => { this.toast.success('Deleted successfully'); this.load(); },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to delete'); this.loading.set(false); },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.maintenanceService.getAll().subscribe({
      next: (data) => {
        this.dataSource.data = data;
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load'); this.loading.set(false); },
    });
  }
}

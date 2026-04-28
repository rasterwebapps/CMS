import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { EquipmentService } from '../equipment.service';
import { Equipment } from '../equipment.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { EQUIPMENT_LIST_TOUR } from '../../../shared/tour/tours/equipment.tours';

@Component({
  selector: 'app-equipment-list',
  standalone: true,
  imports: [
    CmsEmptyStateComponent,
    CmsStatusBadgeComponent,
    RouterLink, MatTableModule, MatPaginatorModule, MatSortModule,
    MatDialogModule, MatTooltipModule,
    CmsTourButtonComponent,
  ],
  templateUrl: './equipment-list.component.html',
  styleUrl: './equipment-list.component.scss',
})
export class EquipmentListComponent implements OnInit {
  private readonly equipmentService = inject(EquipmentService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly tourService = inject(TourService);

  private readonly VIEW_MODE_KEY = 'equipment-view-mode';

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  protected readonly displayedColumns: readonly string[] = ['name', 'model', 'labName', 'category', 'status', 'purchaseDate', 'actions'];
  protected readonly dataSource = new MatTableDataSource<Equipment>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly viewMode = signal<'card' | 'table'>(this.loadViewMode());

  private readonly allEquipment = signal<Equipment[]>([]);

  protected readonly filteredEquipment = computed(() => {
    const q = this.searchValue().trim().toLowerCase();
    return this.allEquipment().filter(item =>
      !q ||
      item.name.toLowerCase().includes(q) ||
      (item.model ?? '').toLowerCase().includes(q) ||
      item.labName.toLowerCase().includes(q) ||
      item.category.toLowerCase().includes(q),
    );
  });

  protected readonly totalCount = computed(() => this.allEquipment().length);

  ngOnInit(): void {
    this.tourService.register('equipment-list', EQUIPMENT_LIST_TOUR);
    this.load();
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
      void this.router.navigate(['/equipment/new']);
    }
  }

  protected edit(item: Equipment): void {
    void this.router.navigate(['/equipment', item.id, 'edit']);
  }

  protected delete(item: Equipment): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Delete Equipment', message: `Delete "${item.name}"?`, confirmText: 'Delete', cancelText: 'Cancel' },
    }).afterClosed().subscribe((confirmed) => { if (confirmed) this.doDelete(item); });
  }

  private doDelete(item: Equipment): void {
    this.loading.set(true);
    this.equipmentService.delete(item.id).subscribe({
      next: () => { this.toast.success('Deleted successfully'); this.load(); },
      error: () => { this.toast.error('Failed to delete'); this.loading.set(false); },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.equipmentService.getAll().subscribe({
      next: (data) => {
        this.allEquipment.set(data);
        this.dataSource.data = data;
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load'); this.loading.set(false); },
    });
  }
}

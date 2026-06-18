import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { BloodGroupService } from '../blood-group.service';
import { BloodGroup } from '../blood-group.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsViewToggleComponent } from '../../../shared/view-toggle/view-toggle.component';
import { ToastService } from '../../../core/toast/toast.service';
import { PermissionService } from '../../../core/permissions/permission.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { BLOOD_GROUP_LIST_TOUR } from '../../../shared/tour/tours/blood-group.tours';

@Component({
  selector: 'app-blood-group-list',
  standalone: true,
  imports: [
    RouterLink,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatDialogModule,
    MatTooltipModule,
    CmsEmptyStateComponent,
    CmsViewToggleComponent,
    CmsTourButtonComponent,
  ],
  templateUrl: './blood-group-list.component.html',
  styleUrl: './blood-group-list.component.scss',
})
export class BloodGroupListComponent implements OnInit {
  private readonly bloodGroupService = inject(BloodGroupService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly permissionService = inject(PermissionService);
  private readonly tourService = inject(TourService);

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  protected readonly canManage = computed(() => this.permissionService.has('BLOOD_GROUP_MANAGE'));
  protected readonly displayedColumns = computed(() =>
    this.canManage() ? ['name', 'code', 'isActive', 'actions'] : ['name', 'code', 'isActive'],
  );
  protected readonly dataSource = new MatTableDataSource<BloodGroup>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly viewMode = signal<'card' | 'table'>('table');

  private readonly allItems = signal<BloodGroup[]>([]);

  protected readonly totalCount = computed(() => this.allItems().length);
  protected readonly activeCount = computed(() => this.allItems().filter(b => b.isActive).length);

  protected readonly filteredItems = computed(() => {
    const q = this.searchValue().trim().toLowerCase();
    if (!q) return this.allItems();
    return this.allItems().filter(
      b => b.name.toLowerCase().includes(q) || b.code.toLowerCase().includes(q),
    );
  });

  ngOnInit(): void {
    this.tourService.register('blood-group-list', BLOOD_GROUP_LIST_TOUR);
    this.load();
  }

  protected setViewMode(mode: 'card' | 'table'): void {
    this.viewMode.set(mode);
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

  protected edit(item: BloodGroup): void {
    if (!this.canManage()) {
      this.toast.error('You do not have permission to edit blood groups');
      return;
    }
    void this.router.navigate(['/blood-groups', item.id, 'edit']);
  }

  protected delete(item: BloodGroup): void {
    if (!this.canManage()) {
      this.toast.error('You do not have permission to delete blood groups');
      return;
    }
    this.dialog
      .open(ConfirmDialogComponent, {
        data: {
          title: 'Delete Blood Group',
          message: `Delete "${item.name}" (${item.code})?`,
          confirmText: 'Delete',
          cancelText: 'Cancel',
        },
      })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) this.doDelete(item);
      });
  }

  protected handleEmptyAction(): void {
    if (this.searchValue()) {
      this.clearFilter();
    } else {
      if (!this.canManage()) {
        this.toast.error('You do not have permission to add blood groups');
        return;
      }
      void this.router.navigate(['/blood-groups/new']);
    }
  }

  private doDelete(item: BloodGroup): void {
    this.loading.set(true);
    this.bloodGroupService.deleteBloodGroup(item.id).subscribe({
      next: () => {
        this.toast.success('Blood group deleted successfully');
        this.load();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to delete blood group');
        this.loading.set(false);
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.bloodGroupService.getBloodGroups().subscribe({
      next: (data) => {
        this.allItems.set(data);
        this.dataSource.data = data;
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load blood groups');
        this.loading.set(false);
      },
    });
  }
}


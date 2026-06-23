import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { InstitutionService } from '../institution.service';
import { Institution } from '../institution.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsViewToggleComponent } from '../../../shared/view-toggle/view-toggle.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { ToastService } from '../../../core/toast/toast.service';
import { PermissionService } from '../../../core/permissions/permission.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { INSTITUTION_LIST_TOUR } from '../../../shared/tour/tours/institution.tours';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsIconEditComponent, CmsIconToggleStatusComponent } from '../../../shared/icons';

@Component({
  selector: 'app-institution-list',
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
    CmsStatusBadgeComponent,
    CmsTourButtonComponent,
    CmsRowActionButtonComponent,
    CmsIconEditComponent,
    CmsIconToggleStatusComponent,
  ],
  templateUrl: './institution-list.component.html',
  styleUrl: './institution-list.component.scss',
})
export class InstitutionListComponent implements OnInit {
  private readonly institutionService = inject(InstitutionService);
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

  protected readonly canManage = computed(() => this.permissionService.has('INSTITUTION_MANAGE'));
  protected readonly displayedColumns = computed(() =>
    this.canManage() ? ['name', 'code', 'isActive', 'actions'] : ['name', 'code', 'isActive'],
  );
  protected readonly dataSource = new MatTableDataSource<Institution>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly viewMode = signal<'card' | 'table'>('table');

  private readonly allItems = signal<Institution[]>([]);

  protected readonly totalCount = computed(() => this.allItems().length);

  protected readonly filteredItems = computed(() => {
    const q = this.searchValue().trim().toLowerCase();
    if (!q) return this.allItems();
    return this.allItems().filter(
      i => i.name.toLowerCase().includes(q) || i.code.toLowerCase().includes(q),
    );
  });

  ngOnInit(): void {
    this.tourService.register('institution-list', INSTITUTION_LIST_TOUR);
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

  protected edit(item: Institution): void {
    if (!this.canManage()) {
      this.toast.error('You do not have permission to edit institutions');
      return;
    }
    void this.router.navigate(['/institutions', item.id, 'edit']);
  }

  protected toggleStatus(item: Institution): void {
    if (!this.canManage()) {
      this.toast.error('You do not have permission to update institution status');
      return;
    }
    const nextAction = item.isActive ? 'Deactivate' : 'Activate';
    this.dialog
      .open(ConfirmDialogComponent, {
        data: {
          title: `${nextAction} Institution`,
          message: `${nextAction} "${item.name}" (${item.code})?`,
          confirmText: nextAction,
          cancelText: 'Cancel',
        },
      })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) this.doToggle(item);
      });
  }

  protected handleEmptyAction(): void {
    if (this.searchValue()) {
      this.clearFilter();
    } else {
      if (!this.canManage()) {
        this.toast.error('You do not have permission to add institutions');
        return;
      }
      void this.router.navigate(['/institutions/new']);
    }
  }

  private doToggle(item: Institution): void {
    this.loading.set(true);
    this.institutionService.updateStatus(item.id, { isActive: !item.isActive }).subscribe({
      next: () => {
        this.toast.success(`Institution ${item.isActive ? 'deactivated' : 'activated'} successfully`);
        this.load();
      },
      error: (err) => {
        this.toast.error(
          err?.error?.message ?? `Failed to ${item.isActive ? 'deactivate' : 'activate'} institution`,
        );
        this.loading.set(false);
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.institutionService.getAll().subscribe({
      next: (data) => {
        this.allItems.set(data);
        this.dataSource.data = data;
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load institutions');
        this.loading.set(false);
      },
    });
  }
}

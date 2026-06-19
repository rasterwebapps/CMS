import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CommunityService } from '../community.service';
import { Community } from '../community.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsViewToggleComponent } from '../../../shared/view-toggle/view-toggle.component';
import { ToastService } from '../../../core/toast/toast.service';
import { PermissionService } from '../../../core/permissions/permission.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { COMMUNITY_LIST_TOUR } from '../../../shared/tour/tours/community.tours';

@Component({
  selector: 'app-community-list',
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
  templateUrl: './community-list.component.html',
  styleUrl: './community-list.component.scss',
})
export class CommunityListComponent implements OnInit {
  private readonly communityService = inject(CommunityService);
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

  protected readonly canManage = computed(() => this.permissionService.has('COMMUNITY_MANAGE'));
  protected readonly displayedColumns = computed(() =>
    this.canManage()
      ? ['name', 'code', 'description', 'isActive', 'actions']
      : ['name', 'code', 'description', 'isActive'],
  );
  protected readonly dataSource = new MatTableDataSource<Community>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly viewMode = signal<'card' | 'table'>('table');

  private readonly allItems = signal<Community[]>([]);

  protected readonly totalCount = computed(() => this.allItems().length);
  protected readonly activeCount = computed(() => this.allItems().filter(c => c.isActive).length);

  protected readonly filteredItems = computed(() => {
    const q = this.searchValue().trim().toLowerCase();
    if (!q) return this.allItems();
    return this.allItems().filter(
      c => c.name.toLowerCase().includes(q) || c.code.toLowerCase().includes(q),
    );
  });

  ngOnInit(): void {
    this.tourService.register('community-list', COMMUNITY_LIST_TOUR);
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

  protected edit(item: Community): void {
    if (!this.canManage()) {
      this.toast.error('You do not have permission to edit communities');
      return;
    }
    void this.router.navigate(['/communities', item.id, 'edit']);
  }

  protected toggleStatus(item: Community): void {
    if (!this.canManage()) {
      this.toast.error('You do not have permission to update community status');
      return;
    }
    const nextAction = item.isActive ? 'Deactivate' : 'Activate';
    this.dialog
      .open(ConfirmDialogComponent, {
        data: {
          title: `${nextAction} Community`,
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
        this.toast.error('You do not have permission to add communities');
        return;
      }
      void this.router.navigate(['/communities/new']);
    }
  }

  private doToggle(item: Community): void {
    this.loading.set(true);
    const request$ = item.isActive
      ? this.communityService.deactivateCommunity(item.id)
      : this.communityService.reactivateCommunity(item.id);
    request$.subscribe({
      next: () => {
        this.toast.success(`Community ${item.isActive ? 'deactivated' : 'activated'} successfully`);
        this.load();
      },
      error: (err) => {
        this.toast.error(
          err?.error?.message ?? `Failed to ${item.isActive ? 'deactivate' : 'activate'} community`,
        );
        this.loading.set(false);
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.communityService.getCommunities().subscribe({
      next: (data) => {
        this.allItems.set(data);
        this.dataSource.data = data;
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load communities');
        this.loading.set(false);
      },
    });
  }
}

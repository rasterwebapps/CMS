import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { ReferralTypeService } from '../referral-type.service';
import { ReferralType } from '../referral-type.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsViewToggleComponent } from '../../../shared/view-toggle/view-toggle.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { REFERRAL_TYPE_LIST_TOUR } from '../../../shared/tour/tours/referral-type.tours';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';

@Component({
  selector: 'app-referral-type-list',
  standalone: true,
  imports: [
    InrPipe,
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
  ],
  templateUrl: './referral-type-list.component.html',
  styleUrl: './referral-type-list.component.scss',
})
export class ReferralTypeListComponent implements OnInit {
  private readonly referralTypeService = inject(ReferralTypeService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly tourService = inject(TourService);

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  private readonly VIEW_MODE_KEY = 'referral-type-view-mode';

  protected readonly displayedColumns = ['name', 'code', 'hasCommission', 'commissionAmount', 'isActive', 'actions'];
  protected readonly dataSource = new MatTableDataSource<ReferralType>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly viewMode = signal<'card' | 'table'>(this.loadViewMode());

  private readonly allItems = signal<ReferralType[]>([]);

  protected readonly totalCount = computed(() => this.allItems().length);
  protected readonly activeCount = computed(() => this.allItems().filter(r => r.isActive).length);

  protected readonly filteredReferralTypes = computed(() => {
    const q = this.searchValue().trim().toLowerCase();
    if (!q) return this.allItems();
    return this.allItems().filter(
      r =>
        r.name.toLowerCase().includes(q) ||
        r.code.toLowerCase().includes(q),
    );
  });

  ngOnInit(): void {
    this.tourService.register('referral-type-list', REFERRAL_TYPE_LIST_TOUR);
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

  protected edit(item: ReferralType): void {
    void this.router.navigate(['/referral-types', item.id, 'edit']);
  }

  protected toggleStatus(item: ReferralType): void {
    const nextAction = item.isActive ? 'Deactivate' : 'Activate';
    this.dialog
      .open(ConfirmDialogComponent, {
        data: {
          title: `${nextAction} Referral Type`,
          message: `${nextAction} "${item.name}"?`,
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
      void this.router.navigate(['/referral-types/new']);
    }
  }

  private loadViewMode(): 'card' | 'table' {
    const stored = localStorage.getItem(this.VIEW_MODE_KEY);
    return stored === 'table' ? 'table' : 'card';
  }

  private doToggle(item: ReferralType): void {
    this.loading.set(true);
    const request$ = item.isActive
      ? this.referralTypeService.deactivateReferralType(item.id)
      : this.referralTypeService.reactivateReferralType(item.id);
    request$.subscribe({
      next: () => {
        this.toast.success(`Referral type ${item.isActive ? 'deactivated' : 'activated'} successfully`);
        this.load();
      },
      error: (err) => {
        this.toast.error(
          err?.error?.message ?? `Failed to ${item.isActive ? 'deactivate' : 'activate'} referral type`,
        );
        this.loading.set(false);
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.referralTypeService.getReferralTypes().subscribe({
      next: (data) => {
        this.allItems.set(data);
        this.dataSource.data = data;
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load');
        this.loading.set(false);
      },
    });
  }
}

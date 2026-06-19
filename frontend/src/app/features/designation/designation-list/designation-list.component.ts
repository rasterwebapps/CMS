import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { SlicePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ViewChild } from '@angular/core';
import { DesignationService } from '../designation.service';
import { DesignationMaster } from '../designation.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsViewToggleComponent } from '../../../shared/view-toggle/view-toggle.component';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { DESIGNATION_LIST_TOUR } from '../../../shared/tour/tours/designation.tours';

@Component({
  selector: 'app-designation-list',
  standalone: true,
  imports: [
    RouterLink,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatTooltipModule,
    CmsEmptyStateComponent,
    CmsViewToggleComponent,
    CmsTourButtonComponent,
    SlicePipe,
  ],
  templateUrl: './designation-list.component.html',
  styleUrl: './designation-list.component.scss',
})
export class DesignationListComponent implements OnInit {
  private readonly designationService = inject(DesignationService);
  private readonly router             = inject(Router);
  private readonly toast              = inject(ToastService);
  private readonly dialog             = inject(MatDialog);
  private readonly tourService        = inject(TourService);

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  protected readonly displayedColumns = ['code', 'name', 'description', 'isActive', 'actions'];
  protected readonly dataSource = new MatTableDataSource<DesignationMaster>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');

  private readonly VIEW_MODE_KEY = 'designation-view-mode';
  protected readonly viewMode = signal<'card' | 'table'>(this.loadViewMode());

  private readonly allDesignations = signal<DesignationMaster[]>([]);
  protected readonly totalCount = computed(() => this.allDesignations().length);

  protected readonly filteredItems = computed(() => {
    const q = this.searchValue().trim().toLowerCase();
    if (!q) return this.allDesignations();
    return this.allDesignations().filter(
      d => d.name.toLowerCase().includes(q) || d.code.toLowerCase().includes(q),
    );
  });

  ngOnInit(): void {
    this.tourService.register('designation-list', DESIGNATION_LIST_TOUR);
    this.loadDesignations();
  }

  protected setViewMode(mode: 'card' | 'table'): void {
    this.viewMode.set(mode);
    localStorage.setItem(this.VIEW_MODE_KEY, mode);
  }

  protected applyFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value;
    this.searchValue.set(filterValue);
    this.dataSource.filter = filterValue.trim().toLowerCase();
    if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
  }

  protected clearFilter(): void {
    this.searchValue.set('');
    this.dataSource.filter = '';
  }

  protected editDesignation(item: DesignationMaster): void {
    void this.router.navigate(['/designations', item.id, 'edit']);
  }

  protected toggleDesignationStatus(item: DesignationMaster): void {
    const nextAction = item.isActive ? 'Deactivate' : 'Activate';
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: `${nextAction} Designation`,
        message: `${nextAction} "${item.name}" (${item.code})?`,
        confirmText: nextAction,
        cancelText: 'Cancel',
      },
    });
    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) this.performToggle(item);
    });
  }

  protected handleEmptyAction(): void {
    if (this.searchValue()) {
      this.clearFilter();
    } else {
      void this.router.navigate(['/designations/new']);
    }
  }

  private loadViewMode(): 'card' | 'table' {
    const stored = localStorage.getItem(this.VIEW_MODE_KEY);
    return stored === 'table' ? 'table' : 'card';
  }

  private performToggle(item: DesignationMaster): void {
    this.loading.set(true);
    this.designationService.updateStatus(item.id, { isActive: !item.isActive }).subscribe({
      next: () => {
        this.toast.success(`Designation ${item.isActive ? 'deactivated' : 'activated'} successfully`);
        this.loadDesignations();
      },
      error: (err) => {
        this.toast.error(
          err?.error?.message ?? `Failed to ${item.isActive ? 'deactivate' : 'activate'} designation`,
        );
        this.loading.set(false);
      },
    });
  }

  private loadDesignations(): void {
    this.loading.set(true);
    this.designationService.getAll().subscribe({
      next: (items) => {
        this.allDesignations.set(items);
        this.dataSource.data = items;
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load designations');
        this.loading.set(false);
      },
    });
  }
}

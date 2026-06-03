import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { SpecialityService } from '../speciality.service';
import { Speciality } from '../speciality.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsViewMode, CmsViewToggleComponent } from '../../../shared/view-toggle/view-toggle.component';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { DEPT_LIST_TOUR } from '../../../shared/tour/tours/department.tours';
import { computeInitials } from '../../../shared/utils/initials';

@Component({
  selector: 'app-speciality-list',
  standalone: true,
  imports: [
    RouterLink,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatTooltipModule,
    CmsEmptyStateComponent, CmsViewToggleComponent, CmsTourButtonComponent,
  ],
  templateUrl: './speciality-list.component.html',
  styleUrl: './speciality-list.component.scss',
})
export class SpecialityListComponent implements OnInit {
  private readonly specialityService = inject(SpecialityService);
  private readonly router            = inject(Router);
  private readonly tourService       = inject(TourService);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  protected readonly displayedColumns = ['code', 'name', 'hodName', 'actions'];
  protected readonly dataSource = new MatTableDataSource<Speciality>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  private readonly _specialities = signal<Speciality[]>([]);

  protected readonly totalCount = computed(() => this._specialities().length);
  protected readonly hodAssignedCount = computed(() =>
    this._specialities().filter(d => d.hodName).length
  );

  protected readonly visibleRows = computed<Speciality[]>(() => {
    this.searchValue();
    return this.dataSource.filteredData;
  });

  protected onViewModeChange(mode: 'card' | 'table'): void {
    this.viewMode.set(mode);
  }

  protected initials(name?: string | null): string {
    return computeInitials(name) || '—';
  }

  private readonly allSpecialities = signal<Speciality[]>([]);

  private readonly VIEW_MODE_KEY = 'speciality-view-mode';
  protected readonly viewMode = signal<'card' | 'table'>(this.loadViewMode());

  protected readonly deptCount = computed(() => this.allSpecialities().length);
  protected readonly headsAssigned = computed(() =>
    this.allSpecialities().filter(d => !!(d.hodName?.trim())).length,
  );

  protected readonly filteredDepts = computed(() => {
    const q = this.searchValue().trim().toLowerCase();
    if (!q) return this.allSpecialities();
    return this.allSpecialities().filter(
      d =>
        d.name.toLowerCase().includes(q) ||
        d.code.toLowerCase().includes(q) ||
        (d.hodName?.toLowerCase().includes(q) ?? false),
    );
  });

  protected readonly computeInitials = computeInitials;

  ngOnInit(): void {
    this.tourService.register('dept-list', DEPT_LIST_TOUR);
    this.loadSpecialities();
  }

  protected setViewMode(mode: 'card' | 'table'): void {
    this.viewMode.set(mode);
    localStorage.setItem(this.VIEW_MODE_KEY, mode);
  }

  protected applyFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value;
    this.searchValue.set(filterValue);
    this.dataSource.filter = filterValue.trim().toLowerCase();
    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  protected clearFilter(): void {
    this.searchValue.set('');
    this.dataSource.filter = '';
  }

  protected editSpeciality(speciality: Speciality): void {
    void this.router.navigate(['/specialities', speciality.id, 'edit']);
  }

  protected deleteSpeciality(speciality: Speciality): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Speciality',
        message: `Are you sure you want to delete "${speciality.name}"?`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    });
    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) this.performDelete(speciality);
    });
  }

  protected handleEmptyAction(): void {
    if (this.searchValue()) {
      this.clearFilter();
    } else {
      void this.router.navigate(['/specialities/new']);
    }
  }

  private loadViewMode(): 'card' | 'table' {
    const stored = localStorage.getItem(this.VIEW_MODE_KEY);
    return stored === 'table' ? 'table' : 'card';
  }

  private performDelete(speciality: Speciality): void {
    this.loading.set(true);
    this.specialityService.delete(speciality.id).subscribe({
      next: () => {
        this.toast.success('Speciality deleted successfully');
        this.loadSpecialities();
      },
      error: () => {
        this.toast.error('Failed to delete speciality');
        this.loading.set(false);
      },
    });
  }

  private loadSpecialities(): void {
    this.loading.set(true);
    this.specialityService.getAll().subscribe({
      next: (specialities) => {
        this.allSpecialities.set(specialities);
        this.dataSource.data = specialities;
        this._specialities.set(specialities);
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load specialities');
        this.loading.set(false);
      },
    });
  }
}

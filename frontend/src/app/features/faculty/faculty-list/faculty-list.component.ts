import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { TitleCasePipe } from '@angular/common';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FacultyService } from '../faculty.service';
import { Faculty, FacultyStatus, FACULTY_STATUS_OPTIONS } from '../faculty.model';
import { DepartmentService } from '../../department/department.service';
import { Department } from '../../department/department.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { FACULTY_LIST_TOUR } from '../../../shared/tour/tours/faculty.tours';

@Component({
  selector: 'app-faculty-list',
  standalone: true,
  imports: [
    CmsEmptyStateComponent,
    CmsTourButtonComponent,
    RouterLink,
    TitleCasePipe,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatDialogModule,
    MatTooltipModule,
  ],
  templateUrl: './faculty-list.component.html',
  styleUrl: './faculty-list.component.scss',
})
export class FacultyListComponent implements OnInit {
  private readonly facultyService = inject(FacultyService);
  private readonly departmentService = inject(DepartmentService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly tourService = inject(TourService);

  private readonly VIEW_MODE_KEY = 'faculty-view-mode';

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  protected readonly displayedColumns: readonly string[] = ['employeeCode', 'fullName', 'phone', 'email', 'departmentName', 'designation', 'status', 'actions'];
  protected readonly dataSource = new MatTableDataSource<Faculty>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly viewMode = signal<'card' | 'table'>(this.loadViewMode());

  private readonly allFaculty = signal<Faculty[]>([]);
  protected readonly departments = signal<Department[]>([]);
  protected readonly selectedDepartmentId = signal<number | null>(null);
  protected readonly selectedStatus = signal<FacultyStatus | null>(null);
  protected readonly statusOptions = FACULTY_STATUS_OPTIONS;

  protected readonly filteredFaculty = computed(() => {
    const q = this.searchValue().trim().toLowerCase();
    const deptId = this.selectedDepartmentId();
    const status = this.selectedStatus();
    const deptName = deptId
      ? (this.departments().find(d => d.id === deptId)?.name ?? '')
      : '';

    return this.allFaculty().filter(item => {
      if (deptName && !item.departmentName.toLowerCase().includes(deptName.toLowerCase())) return false;
      if (status && item.status !== status) return false;
      if (q && !(
        item.fullName.toLowerCase().includes(q) ||
        item.employeeCode.toLowerCase().includes(q) ||
        (item.email ?? '').toLowerCase().includes(q) ||
        item.departmentName.toLowerCase().includes(q)
      )) return false;
      return true;
    });
  });

  protected readonly totalCount = computed(() => this.allFaculty().length);
  protected readonly activeCount = computed(() => this.allFaculty().filter(f => f.status === 'ACTIVE').length);

  protected readonly hasActiveFilters = computed(() =>
    this.selectedDepartmentId() !== null || this.selectedStatus() !== null || this.searchValue().length > 0,
  );

  ngOnInit(): void {
    this.tourService.register('faculty-list', FACULTY_LIST_TOUR);
    this.loadDepartments();
    this.loadFaculty();
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

  protected onDepartmentChange(departmentId: number | null): void {
    this.selectedDepartmentId.set(departmentId);
  }

  protected onStatusChange(status: FacultyStatus | null): void {
    this.selectedStatus.set(status);
  }

  protected clearFilters(): void {
    this.selectedDepartmentId.set(null);
    this.selectedStatus.set(null);
    this.clearFilter();
  }

  protected setViewMode(mode: 'card' | 'table'): void {
    this.viewMode.set(mode);
    localStorage.setItem(this.VIEW_MODE_KEY, mode);
  }

  private loadViewMode(): 'card' | 'table' {
    return localStorage.getItem(this.VIEW_MODE_KEY) === 'table' ? 'table' : 'card';
  }

  protected handleEmptyAction(): void {
    if (this.hasActiveFilters()) {
      this.clearFilters();
    } else {
      void this.router.navigate(['/faculty/new']);
    }
  }

  protected viewFaculty(faculty: Faculty): void {
    void this.router.navigate(['/faculty', faculty.id]);
  }

  protected editFaculty(faculty: Faculty): void {
    void this.router.navigate(['/faculty', faculty.id, 'edit']);
  }

  protected deleteFaculty(faculty: Faculty): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Faculty',
        message: `Are you sure you want to delete "${faculty.fullName}"?`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) this.performDelete(faculty);
    });
  }

  protected getStatusClass(status: FacultyStatus): string {
    if (status === 'ACTIVE') return 'fac-status--active';
    if (status === 'ON_LEAVE' || status === 'SABBATICAL') return 'fac-status--leave';
    return 'fac-status--inactive';
  }

  private performDelete(faculty: Faculty): void {
    this.loading.set(true);
    this.facultyService.delete(faculty.id).subscribe({
      next: () => {
        this.toast.success('Faculty deleted successfully');
        this.loadFaculty();
      },
      error: () => {
        this.toast.error('Failed to delete faculty');
        this.loading.set(false);
      },
    });
  }

  private loadDepartments(): void {
    this.departmentService.getAll().subscribe({
      next: (departments) => { this.departments.set(departments); },
      error: () => { this.toast.error('Failed to load departments'); },
    });
  }

  private loadFaculty(): void {
    this.loading.set(true);
    this.facultyService.getAll().subscribe({
      next: (facultyList) => {
        this.allFaculty.set(facultyList);
        this.dataSource.data = facultyList;
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load faculty');
        this.loading.set(false);
      },
    });
  }
}

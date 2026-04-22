import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TitleCasePipe } from '@angular/common';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { FacultyService } from '../faculty.service';
import { Faculty, FacultyStatus, FACULTY_STATUS_OPTIONS } from '../faculty.model';
import { DepartmentService } from '../../department/department.service';
import { Department } from '../../department/department.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { ToastService } from '../../../core/toast/toast.service';

@Component({
  selector: 'app-faculty-list',
  standalone: true,
  imports: [
    PageHeaderComponent,
    RouterLink,
    FormsModule,
    TitleCasePipe,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatTooltipModule,
    MatChipsModule],
  templateUrl: './faculty-list.component.html',
  styleUrl: './faculty-list.component.scss',
})
export class FacultyListComponent implements OnInit {
  private readonly facultyService = inject(FacultyService);
  private readonly departmentService = inject(DepartmentService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  // ── Column visibility ────────────────────────────────────────────────────
  protected readonly ALL_COLS = ['employeeCode', 'fullName', 'phone', 'email', 'departmentName', 'designation', 'status', 'actions'];
  protected readonly COLUMN_LABELS: Record<string, string> = {
    employeeCode: 'Code',
    fullName: 'Name',
    phone: 'Phone',
    email: 'Email',
    departmentName: 'Department',
    designation: 'Designation',
    status: 'Status',
    actions: 'Actions',
  };
  private readonly COLS_KEY = 'faculty-list-cols';
  private readonly _visibleCols = signal<Set<string>>(this._loadColPrefs());
  protected readonly displayedColumns = computed(() => this.ALL_COLS.filter(c => this._visibleCols().has(c)));
  protected readonly dataSource = new MatTableDataSource<Faculty>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly departments = signal<Department[]>([]);
  protected readonly selectedDepartmentId = signal<number | null>(null);
  protected readonly selectedStatus = signal<FacultyStatus | null>(null);
  protected readonly statusOptions = FACULTY_STATUS_OPTIONS;

  ngOnInit(): void {
    this.loadDepartments();
    this.loadFaculty();
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

  protected onDepartmentChange(departmentId: number | null): void {
    this.selectedDepartmentId.set(departmentId);
    this.loadFaculty();
  }

  protected onStatusChange(status: FacultyStatus | null): void {
    this.selectedStatus.set(status);
    this.loadFaculty();
  }

  protected clearFilters(): void {
    this.selectedDepartmentId.set(null);
    this.selectedStatus.set(null);
    this.clearFilter();
    this.loadFaculty();
  }

  protected viewFaculty(faculty: Faculty): void {
    void this.router.navigate(['/faculty', faculty.id]);
  }

  protected editFaculty(faculty: Faculty): void {
    void this.router.navigate(['/faculty', faculty.id, 'edit']);
  }

  protected deleteFaculty(faculty: Faculty): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Faculty',
        message: `Are you sure you want to delete "${faculty.fullName}"?`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.performDelete(faculty);
      }
    });
  }

  protected getStatusColor(status: FacultyStatus): string {
    switch (status) {
      case 'ACTIVE':
        return 'primary';
      case 'ON_LEAVE':
        return 'accent';
      case 'SABBATICAL':
        return 'accent';
      default:
        return 'warn';
    }
  }

  private _loadColPrefs(): Set<string> {
    try {
      const s = localStorage.getItem(this.COLS_KEY);
      if (s) return new Set<string>(JSON.parse(s) as string[]);
    } catch { /* empty */ }
    return new Set<string>(this.ALL_COLS);
  }

  protected toggleColumn(col: string): void {
    this._visibleCols.update(s => {
      const next = new Set(s);
      if (next.size > 1 && next.has(col)) { next.delete(col); } else { next.add(col); }
      localStorage.setItem(this.COLS_KEY, JSON.stringify([...next]));
      return next;
    });
  }

  protected isColumnVisible(col: string): boolean { return this._visibleCols().has(col); }

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
      next: (departments) => {
        this.departments.set(departments);
      },
      error: () => {
        this.toast.error('Failed to load departments');
      },
    });
  }

  private loadFaculty(): void {
    this.loading.set(true);

    const departmentId = this.selectedDepartmentId();
    const status = this.selectedStatus();

    let observable;
    if (departmentId) {
      observable = this.facultyService.getByDepartmentId(departmentId);
    } else if (status) {
      observable = this.facultyService.getByStatus(status);
    } else {
      observable = this.facultyService.getAll();
    }

    observable.subscribe({
      next: (facultyList) => {
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

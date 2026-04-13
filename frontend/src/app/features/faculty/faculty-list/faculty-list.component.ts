import { Component, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TitleCasePipe } from '@angular/common';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { FacultyService } from '../faculty.service';
import { Faculty, FacultyStatus, FACULTY_STATUS_OPTIONS } from '../faculty.model';
import { DepartmentService } from '../../department/department.service';
import { Department } from '../../department/department.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-faculty-list',
  standalone: true,
  imports: [
    RouterLink,
    FormsModule,
    TitleCasePipe,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatInputModule,
    MatFormFieldModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDialogModule,
    MatTooltipModule,
    MatSelectModule,
    MatChipsModule,
  ],
  templateUrl: './faculty-list.component.html',
  styleUrl: './faculty-list.component.scss',
})
export class FacultyListComponent implements OnInit {
  private readonly facultyService = inject(FacultyService);
  private readonly departmentService = inject(DepartmentService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  protected readonly displayedColumns = [
    'employeeCode',
    'fullName',
    'departmentName',
    'designation',
    'status',
    'actions',
  ];
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

  private performDelete(faculty: Faculty): void {
    this.loading.set(true);
    this.facultyService.delete(faculty.id).subscribe({
      next: () => {
        this.snackBar.open('Faculty deleted successfully', 'Close', {
          duration: 3000,
        });
        this.loadFaculty();
      },
      error: () => {
        this.snackBar.open('Failed to delete faculty', 'Close', {
          duration: 3000,
        });
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
        this.snackBar.open('Failed to load departments', 'Close', {
          duration: 3000,
        });
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
        this.dataSource.paginator = this.paginator;
        this.dataSource.sort = this.sort;
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Failed to load faculty', 'Close', {
          duration: 3000,
        });
        this.loading.set(false);
      },
    });
  }
}

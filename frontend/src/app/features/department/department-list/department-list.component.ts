import { Component, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DepartmentService } from '../department.service';
import { Department } from '../department.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';

@Component({
  selector: 'app-department-list',
  standalone: true,
  imports: [
    PageHeaderComponent,
    RouterLink,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDialogModule,
    MatTooltipModule,
  ],
  templateUrl: './department-list.component.html',
  styleUrl: './department-list.component.scss',
})
export class DepartmentListComponent implements OnInit {
  private readonly departmentService = inject(DepartmentService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  protected readonly displayedColumns = ['code', 'name', 'hodName', 'actions'];
  protected readonly dataSource = new MatTableDataSource<Department>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');

  ngOnInit(): void {
    this.loadDepartments();
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

  protected editDepartment(department: Department): void {
    void this.router.navigate(['/departments', department.id, 'edit']);
  }

  protected deleteDepartment(department: Department): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Department',
        message: `Are you sure you want to delete "${department.name}"?`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.performDelete(department);
      }
    });
  }

  private performDelete(department: Department): void {
    this.loading.set(true);
    this.departmentService.delete(department.id).subscribe({
      next: () => {
        this.snackBar.open('Department deleted successfully', 'Close', {
          duration: 3000,
        });
        this.loadDepartments();
      },
      error: () => {
        this.snackBar.open('Failed to delete department', 'Close', {
          duration: 3000,
        });
        this.loading.set(false);
      },
    });
  }

  private loadDepartments(): void {
    this.loading.set(true);
    this.departmentService.getAll().subscribe({
      next: (departments) => {
        this.dataSource.data = departments;
        this.dataSource.paginator = this.paginator;
        this.dataSource.sort = this.sort;
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Failed to load departments', 'Close', {
          duration: 3000,
        });
        this.loading.set(false);
      },
    });
  }
}

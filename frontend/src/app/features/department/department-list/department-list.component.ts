import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
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

@Component({
  selector: 'app-department-list',
  standalone: true,
  imports: [
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

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  // ── View mode (card / table) ─────────────────────────────────────────────
  protected readonly viewMode = signal<'card' | 'table'>('card');

  // ── Column definitions ───────────────────────────────────────────────────
  protected readonly ALL_COLS = ['code', 'name', 'hodName', 'actions'];
  protected readonly displayedColumns = computed(() => this.ALL_COLS);
  protected readonly dataSource = new MatTableDataSource<Department>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');

  // ── Stat counts ──────────────────────────────────────────────────────────
  private readonly _departments = signal<Department[]>([]);
  protected readonly totalCount = computed(() => this._departments().length);
  protected readonly hodAssignedCount = computed(() => this._departments().filter(d => d.hodName).length);

  ngOnInit(): void {
    this.loadDepartments();
  }

  protected setViewMode(mode: 'card' | 'table'): void {
    this.viewMode.set(mode);
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

  protected getInitials(name: string): string {
    if (!name) return '?';
    const parts = name.trim().split(' ').filter(Boolean);
    return parts.length >= 2
      ? (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
      : parts[0][0].toUpperCase();
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
        this._departments.set(departments);
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

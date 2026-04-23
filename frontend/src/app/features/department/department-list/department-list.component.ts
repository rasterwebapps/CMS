import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DepartmentService } from '../department.service';
import { Department } from '../department.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { computeInitials } from '../../../shared/utils/initials';

@Component({
  selector: 'app-department-list',
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
  ],
  templateUrl: './department-list.component.html',
  styleUrl: './department-list.component.scss',
})
export class DepartmentListComponent implements OnInit {
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

  protected readonly displayedColumns = ['code', 'name', 'hodName', 'actions'];
  protected readonly dataSource = new MatTableDataSource<Department>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  private readonly _departments = signal<Department[]>([]);

  protected readonly totalCount = computed(() => this._departments().length);
  protected readonly hodAssignedCount = computed(() =>
    this._departments().filter(d => d.hodName).length
  );

  protected readonly visibleRows = computed<Department[]>(() => {
    this.searchValue();
    return this.dataSource.filteredData;
  });

  protected onViewModeChange(mode: CmsViewMode): void {
    this.viewMode.set(mode);
  }

  protected initials(name?: string | null): string {
    return computeInitials(name) || '—';
  }

  // Single source of truth for all departments (card view reads from here)
  private readonly allDepts = signal<Department[]>([]);

  // View mode — persisted to localStorage
  private readonly VIEW_MODE_KEY = 'dept-view-mode';
  protected readonly viewMode = signal<'card' | 'table'>(this.loadViewMode());

  // Stat chips
  protected readonly deptCount = computed(() => this.allDepts().length);
  protected readonly headsAssigned = computed(() =>
    this.allDepts().filter(d => !!(d.hodName?.trim())).length,
  );

  // Filtered list for the card grid
  protected readonly filteredDepts = computed(() => {
    const q = this.searchValue().trim().toLowerCase();
    if (!q) return this.allDepts();
    return this.allDepts().filter(
      d =>
        d.name.toLowerCase().includes(q) ||
        d.code.toLowerCase().includes(q) ||
        (d.hodName?.toLowerCase().includes(q) ?? false),
    );
  });

  protected readonly computeInitials = computeInitials;

  ngOnInit(): void {
    this.loadDepartments();
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
      if (confirmed) this.performDelete(department);
    });
  }

  protected handleEmptyAction(): void {
    if (this.searchValue()) {
      this.clearFilter();
    } else {
      void this.router.navigate(['/departments/new']);
    }
  }

  private loadViewMode(): 'card' | 'table' {
    const stored = localStorage.getItem(this.VIEW_MODE_KEY);
    return stored === 'table' ? 'table' : 'card';
  }

  private performDelete(department: Department): void {
    this.loading.set(true);
    this.departmentService.delete(department.id).subscribe({
      next: () => {
        this.snackBar.open('Department deleted successfully', 'Close', { duration: 3000 });
        this.loadDepartments();
      },
      error: () => {
        this.snackBar.open('Failed to delete department', 'Close', { duration: 3000 });
        this.loading.set(false);
      },
    });
  }

  private loadDepartments(): void {
    this.loading.set(true);
    this.departmentService.getAll().subscribe({
      next: (departments) => {
        this.allDepts.set(departments);
        this.dataSource.data = departments;
        this._departments.set(departments);
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Failed to load departments', 'Close', { duration: 3000 });
        this.loading.set(false);
      },
    });
  }
}

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
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { CmsViewToggleComponent, CmsViewMode } from '../../../shared/view-toggle/view-toggle.component';
import { computeInitials } from '../../../shared/utils/initials';

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
    CmsViewToggleComponent,
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

  // ── Column visibility ────────────────────────────────────────────────────
  protected readonly ALL_COLS = ['code', 'name', 'hodName', 'actions'];
  protected readonly COLUMN_LABELS: Record<string, string> = {
    code: 'Code',
    name: 'Name',
    hodName: 'HOD',
    actions: 'Actions',
  };
  private readonly COLS_KEY = 'department-list-cols';
  private readonly _visibleCols = signal<Set<string>>(this._loadColPrefs());
  protected readonly displayedColumns = computed(() => this.ALL_COLS.filter(c => this._visibleCols().has(c)));
  protected readonly dataSource = new MatTableDataSource<Department>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');

  // ── View mode (card / table) ─────────────────────────────────────────────
  protected readonly viewMode = signal<CmsViewMode>('card');
  protected readonly VIEW_KEY = 'department-list-view';

  protected onViewModeChange(mode: CmsViewMode): void {
    this.viewMode.set(mode);
  }

  /** Filtered rows for the card grid (mirrors MatTableDataSource filter). */
  protected readonly visibleRows = computed<Department[]>(() => {
    // Re-evaluate when search changes
    this.searchValue();
    return this.dataSource.filteredData;
  });

  protected initials(name?: string | null): string {
    return computeInitials(name) || '—';
  }

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

  private _loadColPrefs(): Set<string> {
    try {
      const s = localStorage.getItem(this.COLS_KEY);
      if (s) return new Set<string>(JSON.parse(s) as string[]);
    } catch { /* empty */ }
    return new Set<string>(this.ALL_COLS);
  }

  protected toggleColumn(col: string): void {
    const next = new Set(this._visibleCols());
    if (next.size > 1 && next.has(col)) {
      next.delete(col);
    } else {
      next.add(col);
    }
    this._visibleCols.set(next);
    this._persistColPrefs(next);
  }

  private _persistColPrefs(cols: Set<string>): void {
    try {
      localStorage.setItem(this.COLS_KEY, JSON.stringify([...cols]));
    } catch {
      /* localStorage may be unavailable — ignore */
    }
  }

  protected isColumnVisible(col: string): boolean { return this._visibleCols().has(col); }

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

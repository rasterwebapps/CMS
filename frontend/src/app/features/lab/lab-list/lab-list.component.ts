import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { LabService } from '../lab.service';
import { Lab, LabType, LabStatus, LAB_TYPES, LAB_STATUSES } from '../lab.model';
import { DepartmentService } from '../../department/department.service';
import { Department } from '../../department/department.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { ToastService } from '../../../core/toast/toast.service';

@Component({
  selector: 'app-lab-list',
  standalone: true,
  imports: [
    PageHeaderComponent,
    CommonModule,
    RouterLink,
    FormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatTooltipModule],
  templateUrl: './lab-list.component.html',
  styleUrl: './lab-list.component.scss',
})
export class LabListComponent implements OnInit {
  private readonly labService = inject(LabService);
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
  protected readonly ALL_COLS = ['name', 'labType', 'location', 'capacity', 'status', 'department', 'actions'];
  protected readonly COLUMN_LABELS: Record<string, string> = {
    name: 'Name',
    labType: 'Type',
    location: 'Location',
    capacity: 'Capacity',
    status: 'Status',
    department: 'Department',
    actions: 'Actions',
  };
  private readonly COLS_KEY = 'lab-list-cols';
  private readonly _visibleCols = signal<Set<string>>(this._loadColPrefs());
  protected readonly displayedColumns = computed(() => this.ALL_COLS.filter(c => this._visibleCols().has(c)));
  protected readonly dataSource = new MatTableDataSource<Lab>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');

  protected readonly departments = signal<Department[]>([]);
  protected readonly labTypes = LAB_TYPES;
  protected readonly labStatuses = LAB_STATUSES;

  protected readonly selectedDepartment = signal<number | null>(null);
  protected readonly selectedType = signal<LabType | null>(null);
  protected readonly selectedStatus = signal<LabStatus | null>(null);

  private allLabs: Lab[] = [];

  ngOnInit(): void {
    this.loadDepartments();
    this.loadLabs();
  }

  protected applyFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value;
    this.searchValue.set(filterValue);
    this.applyFilters();
  }

  protected clearFilter(): void {
    this.searchValue.set('');
    this.applyFilters();
  }

  protected onDepartmentChange(departmentId: number | null): void {
    this.selectedDepartment.set(departmentId);
    this.applyFilters();
  }

  protected onTypeChange(type: LabType | null): void {
    this.selectedType.set(type);
    this.applyFilters();
  }

  protected onStatusChange(status: LabStatus | null): void {
    this.selectedStatus.set(status);
    this.applyFilters();
  }

  protected clearAllFilters(): void {
    this.searchValue.set('');
    this.selectedDepartment.set(null);
    this.selectedType.set(null);
    this.selectedStatus.set(null);
    this.applyFilters();
  }

  protected viewLab(lab: Lab): void {
    void this.router.navigate(['/labs', lab.id]);
  }

  protected editLab(lab: Lab): void {
    void this.router.navigate(['/labs', lab.id, 'edit']);
  }

  protected deleteLab(lab: Lab): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Lab',
        message: `Are you sure you want to delete "${lab.name}"?`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.performDelete(lab);
      }
    });
  }

  protected getStatusColor(status: LabStatus): string {
    switch (status) {
      case 'ACTIVE':
        return 'status-active';
      case 'INACTIVE':
        return 'status-inactive';
      case 'UNDER_MAINTENANCE':
        return 'status-maintenance';
      default:
        return '';
    }
  }

  protected getStatusLabel(status: LabStatus): string {
    const statusOption = this.labStatuses.find((s) => s.value === status);
    return statusOption?.label || status;
  }

  protected getTypeLabel(type: LabType): string {
    const typeOption = this.labTypes.find((t) => t.value === type);
    return typeOption?.label || type;
  }

  protected getLocation(lab: Lab): string {
    const parts = [lab.building, lab.roomNumber].filter(Boolean);
    return parts.length > 0 ? parts.join(' - ') : '—';
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

  private performDelete(lab: Lab): void {
    this.loading.set(true);
    this.labService.delete(lab.id).subscribe({
      next: () => {
        this.toast.success('Lab deleted successfully');
        this.loadLabs();
      },
      error: () => {
        this.toast.error('Failed to delete lab');
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

  private loadLabs(): void {
    this.loading.set(true);
    this.labService.getAll().subscribe({
      next: (labs) => {
        this.allLabs = labs;
        this.applyFilters();
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load labs');
        this.loading.set(false);
      },
    });
  }

  private applyFilters(): void {
    let filteredLabs = [...this.allLabs];

    // Filter by department
    const deptId = this.selectedDepartment();
    if (deptId !== null) {
      filteredLabs = filteredLabs.filter((lab) => lab.department.id === deptId);
    }

    // Filter by type
    const type = this.selectedType();
    if (type !== null) {
      filteredLabs = filteredLabs.filter((lab) => lab.labType === type);
    }

    // Filter by status
    const status = this.selectedStatus();
    if (status !== null) {
      filteredLabs = filteredLabs.filter((lab) => lab.status === status);
    }

    this.dataSource.data = filteredLabs;

    // Apply text search filter
    const searchVal = this.searchValue().trim().toLowerCase();
    this.dataSource.filter = searchVal;

    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }
}

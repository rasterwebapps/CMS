import { Component, computed, effect, inject, OnInit, signal, ViewChild } from '@angular/core';
import { NgClass } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { LabService } from '../lab.service';
import { Lab, LabType, LabStatus, LAB_TYPES, LAB_STATUSES } from '../lab.model';
import { DepartmentService } from '../../department/department.service';
import { Department } from '../../department/department.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { ToastService } from '../../../core/toast/toast.service';

@Component({
  selector: 'app-lab-list',
  standalone: true,
  imports: [
    CmsEmptyStateComponent,
    NgClass,
    RouterLink, CmsTourButtonComponent,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatDialogModule,
    MatTooltipModule,
  ],
  templateUrl: './lab-list.component.html',
  styleUrl: './lab-list.component.scss',
})
export class LabListComponent implements OnInit {
  private readonly labService = inject(LabService);
  private readonly departmentService = inject(DepartmentService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);

  private readonly VIEW_MODE_KEY = 'lab-view-mode';

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  protected readonly displayedColumns = ['name', 'labType', 'location', 'capacity', 'status', 'department', 'actions'];
  protected readonly dataSource = new MatTableDataSource<Lab>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly viewMode = signal<'card' | 'table'>(this.loadViewMode());

  protected readonly departments = signal<Department[]>([]);
  protected readonly labTypes = LAB_TYPES;
  protected readonly labStatuses = LAB_STATUSES;

  protected readonly selectedDepartment = signal<number | null>(null);
  protected readonly selectedType = signal<LabType | null>(null);
  protected readonly selectedStatus = signal<LabStatus | null>(null);

  private readonly allLabs = signal<Lab[]>([]);

  protected readonly filteredLabs = computed(() => {
    const deptId = this.selectedDepartment();
    const type = this.selectedType();
    const status = this.selectedStatus();
    const search = this.searchValue().trim().toLowerCase();

    return this.allLabs().filter(lab => {
      if (deptId !== null && lab.department.id !== deptId) return false;
      if (type !== null && lab.labType !== type) return false;
      if (status !== null && lab.status !== status) return false;
      if (search) {
        const haystack = [lab.name, lab.building, lab.roomNumber, lab.department.name]
          .filter(Boolean).join(' ').toLowerCase();
        if (!haystack.includes(search)) return false;
      }
      return true;
    });
  });

  protected readonly totalLabs = computed(() => this.allLabs().length);
  protected readonly activeLabs = computed(() => this.allLabs().filter(l => l.status === 'ACTIVE').length);

  constructor() {
    effect(() => {
      this.dataSource.data = this.filteredLabs();
      if (this.dataSource.paginator) {
        this.dataSource.paginator.firstPage();
      }
    });
  }

  ngOnInit(): void {
    this.loadDepartments();
    this.loadLabs();
  }

  protected setViewMode(mode: 'card' | 'table'): void {
    this.viewMode.set(mode);
    localStorage.setItem(this.VIEW_MODE_KEY, mode);
  }

  protected applyFilter(event: Event): void {
    this.searchValue.set((event.target as HTMLInputElement).value);
  }

  protected clearFilter(): void {
    this.searchValue.set('');
  }

  protected onDepartmentChange(departmentId: number | null): void {
    this.selectedDepartment.set(departmentId);
  }

  protected onTypeChange(type: LabType | null): void {
    this.selectedType.set(type);
  }

  protected onStatusChange(status: LabStatus | null): void {
    this.selectedStatus.set(status);
  }

  protected clearAllFilters(): void {
    this.searchValue.set('');
    this.selectedDepartment.set(null);
    this.selectedType.set(null);
    this.selectedStatus.set(null);
  }

  protected viewLab(lab: Lab): void {
    void this.router.navigate(['/labs', lab.id]);
  }

  protected editLab(lab: Lab): void {
    void this.router.navigate(['/labs', lab.id, 'edit']);
  }

  protected deleteLab(lab: Lab): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Lab',
        message: `Are you sure you want to delete "${lab.name}"?`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) this.performDelete(lab);
    });
  }

  protected getStatusColor(status: LabStatus): string {
    switch (status) {
      case 'ACTIVE': return 'status-active';
      case 'INACTIVE': return 'status-inactive';
      case 'UNDER_MAINTENANCE': return 'status-maintenance';
      default: return '';
    }
  }

  protected getStatusLabel(status: LabStatus): string {
    return this.labStatuses.find(s => s.value === status)?.label ?? status;
  }

  protected getTypeLabel(type: LabType): string {
    return this.labTypes.find(t => t.value === type)?.label ?? type;
  }

  protected getLocation(lab: Lab): string {
    const parts = [lab.building, lab.roomNumber].filter(Boolean);
    return parts.length > 0 ? parts.join(' – ') : '—';
  }

  protected handleEmptyAction(): void {
    void this.router.navigate(['/labs/new']);
  }

  private loadViewMode(): 'card' | 'table' {
    try {
      return localStorage.getItem(this.VIEW_MODE_KEY) === 'table' ? 'table' : 'card';
    } catch {
      return 'card';
    }
  }

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
      next: (departments) => this.departments.set(departments),
      error: () => this.toast.error('Failed to load departments'),
    });
  }

  private loadLabs(): void {
    this.loading.set(true);
    this.labService.getAll().subscribe({
      next: (labs) => {
        this.allLabs.set(labs);
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load labs');
        this.loading.set(false);
      },
    });
  }
}

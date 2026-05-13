import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { FacultyService } from '../faculty.service';
import { FacultyPendingDocumentsSummary, DESIGNATION_OPTIONS } from '../faculty.model';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';

@Component({
  selector: 'app-faculty-doc-verification',
  standalone: true,
  imports: [
    RouterLink,
    FormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    CmsEmptyStateComponent,
  ],
  templateUrl: './faculty-doc-verification.component.html',
  styleUrl: './faculty-doc-verification.component.scss',
})
export class FacultyDocVerificationComponent implements OnInit {
  private readonly facultyService = inject(FacultyService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  @ViewChild(MatPaginator) set paginator(v: MatPaginator) { if (v) this.dataSource.paginator = v; }
  @ViewChild(MatSort) set sort(v: MatSort) { if (v) this.dataSource.sort = v; }

  protected readonly loading = signal(false);
  protected readonly searchQuery = signal('');
  protected readonly filterDepartment = signal('ALL');

  protected readonly allData = signal<FacultyPendingDocumentsSummary[]>([]);
  protected readonly dataSource = new MatTableDataSource<FacultyPendingDocumentsSummary>([]);

  protected readonly displayedColumns = [
    'faculty',
    'department',
    'designation',
    'pendingCount',
    'actions',
  ];

  protected readonly totalPending = computed(() =>
    this.allData().reduce((sum, r) => sum + r.pendingCount, 0),
  );

  protected readonly departments = computed(() =>
    [...new Set(this.allData().map((r) => r.departmentName))].sort(),
  );

  protected readonly DESIGNATION_OPTIONS = DESIGNATION_OPTIONS;

  ngOnInit(): void {
    this.dataSource.filterPredicate = (row, _filter) => {
      const dept = this.filterDepartment();
      const q = this.searchQuery().toLowerCase().trim();
      if (dept !== 'ALL' && row.departmentName !== dept) return false;
      if (!q) return true;
      return (
        row.fullName.toLowerCase().includes(q) ||
        row.employeeCode.toLowerCase().includes(q) ||
        row.departmentName.toLowerCase().includes(q)
      );
    };
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.facultyService.getPendingDocumentsSummary().subscribe({
      next: (data) => {
        this.allData.set(data);
        this.dataSource.data = data;
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load pending documents');
        this.loading.set(false);
      },
    });
  }

  protected onSearch(event: Event): void {
    this.searchQuery.set((event.target as HTMLInputElement).value);
    this.triggerFilter();
  }

  protected clearSearch(): void {
    this.searchQuery.set('');
    this.triggerFilter();
  }

  protected onDeptChange(val: string): void {
    this.filterDepartment.set(val);
    this.triggerFilter();
  }

  private triggerFilter(): void {
    this.dataSource.filter = this.searchQuery() + '|' + this.filterDepartment();
    this.dataSource.paginator?.firstPage();
  }

  protected hasActiveFilters(): boolean {
    return this.filterDepartment() !== 'ALL' || this.searchQuery() !== '';
  }

  protected clearFilters(): void {
    this.filterDepartment.set('ALL');
    this.searchQuery.set('');
    this.triggerFilter();
  }

  protected openDocuments(row: FacultyPendingDocumentsSummary): void {
    void this.router.navigate(['/faculty', row.facultyId], {
      fragment: 'documents',
    });
  }

  protected designationLabel(value: string): string {
    return DESIGNATION_OPTIONS.find((o) => o.value === value)?.label ?? value;
  }
}

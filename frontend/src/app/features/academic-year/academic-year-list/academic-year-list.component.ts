import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AcademicYearService } from '../academic-year.service';
import { AcademicYear } from '../academic-year.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsViewToggleComponent } from '../../../shared/view-toggle/view-toggle.component';
import { ToastService } from '../../../core/toast/toast.service';

@Component({
  selector: 'app-academic-year-list',
  standalone: true,
  imports: [
    CmsEmptyStateComponent,
    CmsViewToggleComponent,
    RouterLink,
    DatePipe,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatDialogModule,
    MatTooltipModule,
  ],
  templateUrl: './academic-year-list.component.html',
  styleUrl: './academic-year-list.component.scss',
})
export class AcademicYearListComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  private readonly VIEW_MODE_KEY = 'academic-year-list-view-mode';

  private readonly allAcademicYears = signal<AcademicYear[]>([]);
  protected readonly dataSource = new MatTableDataSource<AcademicYear>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly viewMode = signal<'card' | 'table'>(this.loadViewMode());

  protected readonly displayedColumns: readonly string[] = ['name', 'startDate', 'endDate', 'isCurrent', 'actions'];

  protected readonly filteredAcademicYears = computed(() => {
    const q = this.searchValue().toLowerCase().trim();
    if (!q) return this.allAcademicYears();
    return this.allAcademicYears().filter(ay => ay.name.toLowerCase().includes(q));
  });

  protected readonly totalCount = computed(() => this.allAcademicYears().length);
  protected readonly currentCount = computed(() => this.allAcademicYears().filter(ay => ay.isCurrent).length);

  ngOnInit(): void {
    this.loadAcademicYears();
  }

  protected setViewMode(mode: 'card' | 'table'): void {
    this.viewMode.set(mode);
    localStorage.setItem(this.VIEW_MODE_KEY, mode);
  }

  private loadViewMode(): 'card' | 'table' {
    const stored = localStorage.getItem(this.VIEW_MODE_KEY);
    return stored === 'table' ? 'table' : 'card';
  }

  protected navigateToNew(): void {
    void this.router.navigate(['/academic-years/new']);
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

  protected editAcademicYear(academicYear: AcademicYear): void {
    void this.router.navigate(['/academic-years', academicYear.id, 'edit']);
  }

  protected deleteAcademicYear(academicYear: AcademicYear): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Academic Year',
        message: `Are you sure you want to delete "${academicYear.name}"?`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.performDelete(academicYear);
      }
    });
  }

  private performDelete(academicYear: AcademicYear): void {
    this.loading.set(true);
    this.academicYearService.deleteAcademicYear(academicYear.id).subscribe({
      next: () => {
        this.toast.success('Academic year deleted successfully');
        this.loadAcademicYears();
      },
      error: () => {
        this.toast.error('Failed to delete academic year');
        this.loading.set(false);
      },
    });
  }

  private loadAcademicYears(): void {
    this.loading.set(true);
    this.academicYearService.getAllAcademicYears().subscribe({
      next: (academicYears) => {
        this.allAcademicYears.set(academicYears);
        this.dataSource.data = academicYears;
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load academic years');
        this.loading.set(false);
      },
    });
  }
}

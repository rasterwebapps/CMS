import { DecimalPipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { HttpClient } from '@angular/common/http';
import { FinanceService } from '../finance.service';
import { GroupedFeeStructure } from '../finance.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { environment } from '../../../../environments';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';

interface Program { id: number; name: string; }
interface Course { id: number; name: string; }
interface AcademicYear { id: number; name: string; }

@Component({
  selector: 'app-fee-structure-list',
  standalone: true,
  imports: [
    PageHeaderComponent,
    DecimalPipe, RouterLink, ReactiveFormsModule,
    MatTableModule, MatPaginatorModule, MatSortModule,
    MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, MatSnackBarModule, MatDialogModule, MatTooltipModule,
  ],
  templateUrl: './fee-structure-list.component.html',
  styleUrl: './fee-structure-list.component.scss',
})
export class FeeStructureListComponent implements OnInit {
  private readonly financeService = inject(FinanceService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);
  private readonly http = inject(HttpClient);
  private readonly fb = inject(FormBuilder);

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  // ── Column visibility ────────────────────────────────────────────────────
  protected readonly ALL_COLS = ['programName', 'courseName', 'academicYearName', 'feeCount', 'totalAmount', 'actions'];
  protected readonly COLUMN_LABELS: Record<string, string> = {
    programName: 'Program',
    courseName: 'Course',
    academicYearName: 'Academic Year',
    feeCount: 'Fees',
    totalAmount: 'Total Amount',
    actions: 'Actions',
  };
  private readonly COLS_KEY = 'fee-structure-list-cols';
  private readonly _visibleCols = signal<Set<string>>(this._loadColPrefs());
  protected readonly displayedColumns = computed(() => this.ALL_COLS.filter(c => this._visibleCols().has(c)));
  protected readonly dataSource = new MatTableDataSource<GroupedFeeStructure>([]);
  protected readonly loading = signal(false);

  protected readonly programs = signal<Program[]>([]);
  protected readonly courses = signal<Course[]>([]);
  protected readonly academicYears = signal<AcademicYear[]>([]);

  protected readonly filterForm: FormGroup = this.fb.group({
    academicYearId: [null as number | null],
    programId: [null as number | null],
    courseId: [null as number | null],
  });

  ngOnInit(): void {
    this.http.get<Program[]>(`${environment.apiUrl}/programs`).subscribe({ next: (d) => this.programs.set(d) });
    this.http.get<AcademicYear[]>(`${environment.apiUrl}/academic-years`).subscribe({ next: (d) => this.academicYears.set(d) });
    this.load();
  }

  protected onProgramFilterChange(programId: number | null): void {
    this.filterForm.patchValue({ courseId: null });
    this.courses.set([]);
    if (programId) {
      this.http.get<Course[]>(`${environment.apiUrl}/courses/program/${programId}`).subscribe({
        next: (d) => this.courses.set(d),
      });
    }
    this.applyFilters();
  }

  protected applyFilters(): void {
    const v = this.filterForm.value;
    this.load({
      programId: v.programId ?? undefined,
      academicYearId: v.academicYearId ?? undefined,
      courseId: v.courseId ?? undefined,
    });
  }

  protected clearFilters(): void {
    this.filterForm.reset();
    this.courses.set([]);
    this.load();
  }

  protected edit(item: GroupedFeeStructure): void {
    const params: Record<string, string> = {
      programId: item.programId.toString(),
      academicYearId: item.academicYearId.toString(),
    };
    if (item.courseId) params['courseId'] = item.courseId.toString();
    void this.router.navigate(['/fee-structures/edit'], { queryParams: params });
  }

  protected delete(item: GroupedFeeStructure): void {
    const label = `${item.programName} / ${item.academicYearName}${item.courseName ? ' / ' + item.courseName : ''}`;
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Fee Structure Group',
        message: `Delete all fee structures for "${label}"?`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => { if (confirmed) this.doDelete(item); });
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

  private doDelete(item: GroupedFeeStructure): void {
    this.loading.set(true);
    this.financeService.deleteGroupedFeeStructures(
      item.programId, item.academicYearId, item.courseId ?? undefined
    ).subscribe({
      next: () => { this.snackBar.open('Deleted successfully', 'Close', { duration: 3000 }); this.load(); },
      error: () => { this.snackBar.open('Failed to delete', 'Close', { duration: 3000 }); this.loading.set(false); },
    });
  }

  private load(params?: { programId?: number; academicYearId?: number; courseId?: number }): void {
    this.loading.set(true);
    this.financeService.getGroupedFeeStructures(params).subscribe({
      next: (data) => {
        this.dataSource.data = data;
        this.loading.set(false);
      },
      error: () => { this.snackBar.open('Failed to load', 'Close', { duration: 3000 }); this.loading.set(false); },
    });
  }
}


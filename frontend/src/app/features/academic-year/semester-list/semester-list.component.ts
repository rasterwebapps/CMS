import { Component, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
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
import { AcademicYearService } from '../academic-year.service';
import { AcademicYear, Semester } from '../academic-year.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';

@Component({
  selector: 'app-semester-list',
  standalone: true,
  imports: [
    PageHeaderComponent,
    RouterLink,
    DatePipe,
    FormsModule,
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
  ],
  templateUrl: './semester-list.component.html',
  styleUrl: './semester-list.component.scss',
})
export class SemesterListComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  protected readonly displayedColumns = [
    'name',
    'semesterNumber',
    'startDate',
    'endDate',
    'academicYear',
    'actions',
  ];
  protected readonly dataSource = new MatTableDataSource<Semester>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly selectedAcademicYearId = signal<number | null>(null);

  ngOnInit(): void {
    this.loadAcademicYears();
    this.loadSemesters();
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

  protected onAcademicYearChange(academicYearId: number | null): void {
    this.selectedAcademicYearId.set(academicYearId);
    this.loadSemesters();
  }

  protected editSemester(semester: Semester): void {
    void this.router.navigate(['/semesters', semester.id, 'edit']);
  }

  protected deleteSemester(semester: Semester): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Semester',
        message: `Are you sure you want to delete "${semester.name}"?`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.performDelete(semester);
      }
    });
  }

  private performDelete(semester: Semester): void {
    this.loading.set(true);
    this.academicYearService.deleteSemester(semester.id).subscribe({
      next: () => {
        this.snackBar.open('Semester deleted successfully', 'Close', {
          duration: 3000,
        });
        this.loadSemesters();
      },
      error: () => {
        this.snackBar.open('Failed to delete semester', 'Close', {
          duration: 3000,
        });
        this.loading.set(false);
      },
    });
  }

  private loadAcademicYears(): void {
    this.academicYearService.getAllAcademicYears().subscribe({
      next: (academicYears) => {
        this.academicYears.set(academicYears);
      },
      error: () => {
        this.snackBar.open('Failed to load academic years', 'Close', {
          duration: 3000,
        });
      },
    });
  }

  private loadSemesters(): void {
    this.loading.set(true);

    const academicYearId = this.selectedAcademicYearId();
    const request$ = academicYearId
      ? this.academicYearService.getSemestersByAcademicYear(academicYearId)
      : this.academicYearService.getAllSemesters();

    request$.subscribe({
      next: (semesters) => {
        this.dataSource.data = semesters;
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Failed to load semesters', 'Close', {
          duration: 3000,
        });
        this.loading.set(false);
      },
    });
  }
}

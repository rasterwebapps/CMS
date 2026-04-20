import { Component, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CourseService } from '../course.service';
import { Course } from '../course.model';
import { ProgramService } from '../../program/program.service';
import { Program } from '../../program/program.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';

@Component({
  selector: 'app-course-list',
  standalone: true,
  imports: [
    PageHeaderComponent,
    RouterLink,
    FormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatFormFieldModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDialogModule,
    MatTooltipModule,
  ],
  templateUrl: './course-list.component.html',
  styleUrl: './course-list.component.scss',
})
export class CourseListComponent implements OnInit {
  private readonly courseService = inject(CourseService);
  private readonly programService = inject(ProgramService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  protected readonly displayedColumns = [
    'code',
    'name',
    'specialization',
    'program',
    'actions',
  ];
  protected readonly dataSource = new MatTableDataSource<Course>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly selectedProgramId = signal<number | null>(null);
  protected readonly programs = signal<Program[]>([]);

  ngOnInit(): void {
    this.loadPrograms();
    this.loadCourses();
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

  protected onProgramFilterChange(programId: number | null): void {
    this.selectedProgramId.set(programId);
    this.loadCourses();
  }

  protected editCourse(course: Course): void {
    void this.router.navigate(['/courses', course.id, 'edit']);
  }

  protected deleteCourse(course: Course): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Course',
        message: `Are you sure you want to delete "${course.name}"?`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.performDelete(course);
      }
    });
  }

  private performDelete(course: Course): void {
    this.loading.set(true);
    this.courseService.delete(course.id).subscribe({
      next: () => {
        this.snackBar.open('Course deleted successfully', 'Close', {
          duration: 3000,
        });
        this.loadCourses();
      },
      error: () => {
        this.snackBar.open('Failed to delete course', 'Close', {
          duration: 3000,
        });
        this.loading.set(false);
      },
    });
  }

  private loadPrograms(): void {
    this.programService.getAll().subscribe({
      next: (programs) => {
        this.programs.set(programs);
      },
      error: () => {
        this.snackBar.open('Failed to load programs', 'Close', {
          duration: 3000,
        });
      },
    });
  }

  private loadCourses(): void {
    this.loading.set(true);
    const programId = this.selectedProgramId();

    const courses$ = programId
      ? this.courseService.getByProgram(programId)
      : this.courseService.getAll();

    courses$.subscribe({
      next: (courses) => {
        this.dataSource.data = courses;
        this.dataSource.paginator = this.paginator;
        this.dataSource.sort = this.sort;
        this.applyFilters();
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Failed to load courses', 'Close', {
          duration: 3000,
        });
        this.loading.set(false);
      },
    });
  }

  private applyFilters(): void {
    this.dataSource.filter = this.searchValue().trim().toLowerCase();

    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }
}

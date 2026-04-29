import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { NgClass } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { StudentService } from '../student.service';
import { Student } from '../student.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { STUDENT_LIST_TOUR } from '../../../shared/tour/tours/student.tours';
import { ToastService } from '../../../core/toast/toast.service';

@Component({
  selector: 'app-student-list',
  standalone: true,
  imports: [
    PageHeaderComponent,
    CmsTourButtonComponent,
    NgClass,
    RouterLink,
    FormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatTooltipModule,
    MatChipsModule],
  templateUrl: './student-list.component.html',
  styleUrl: './student-list.component.scss',
})
export class StudentListComponent implements OnInit {
  private readonly studentService = inject(StudentService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly tourService = inject(TourService);

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  // ── Column visibility ────────────────────────────────────────────────────
  protected readonly ALL_COLS = ['rollNumber', 'fullName', 'programName', 'semester', 'labBatch', 'status', 'actions'];
  protected readonly COLUMN_LABELS: Record<string, string> = {
    rollNumber: 'Roll No.',
    fullName: 'Name',
    programName: 'Program',
    semester: 'Semester',
    labBatch: 'Lab Batch',
    status: 'Status',
    actions: 'Actions',
  };
  private readonly COLS_KEY = 'student-list-cols';
  private readonly _visibleCols = signal<Set<string>>(this._loadColPrefs());
  protected readonly displayedColumns = computed(() => this.ALL_COLS.filter(c => this._visibleCols().has(c)));
  protected readonly dataSource = new MatTableDataSource<Student>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');

  ngOnInit(): void {
    this.tourService.register('student-list', STUDENT_LIST_TOUR);
    this.loadStudents();
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

  protected viewStudent(student: Student): void {
    void this.router.navigate(['/students', student.id]);
  }

  protected editStudent(student: Student): void {
    void this.router.navigate(['/students', student.id, 'edit']);
  }

  protected deleteStudent(student: Student): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Student',
        message: `Are you sure you want to delete "${student.fullName}"?`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.performDelete(student);
      }
    });
  }

  protected getStatusClass(status: string): string {
    switch (status?.toUpperCase()) {
      case 'ACTIVE':
        return 'status-active';
      case 'INACTIVE':
        return 'status-inactive';
      case 'GRADUATED':
        return 'status-graduated';
      case 'DROPPED':
        return 'status-dropped';
      default:
        return '';
    }
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

  private performDelete(student: Student): void {
    this.loading.set(true);
    this.studentService.delete(student.id).subscribe({
      next: () => {
        this.toast.success('Student deleted successfully');
        this.loadStudents();
      },
      error: () => {
        this.toast.error('Failed to delete student');
        this.loading.set(false);
      },
    });
  }

  private loadStudents(): void {
    this.loading.set(true);
    this.studentService.getAll().subscribe({
      next: (students) => {
        this.dataSource.data = students;
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load students');
        this.loading.set(false);
      },
    });
  }
}

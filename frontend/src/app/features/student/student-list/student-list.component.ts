import { Component, inject, OnInit, signal, ViewChild } from '@angular/core';
import { NgClass } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
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
import { MatChipsModule } from '@angular/material/chips';
import { StudentService } from '../student.service';
import { Student } from '../student.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';

@Component({
  selector: 'app-student-list',
  standalone: true,
  imports: [
    PageHeaderComponent,
    NgClass,
    RouterLink,
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
    MatChipsModule,
  ],
  templateUrl: './student-list.component.html',
  styleUrl: './student-list.component.scss',
})
export class StudentListComponent implements OnInit {
  private readonly studentService = inject(StudentService);
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
    'rollNumber',
    'fullName',
    'programName',
    'semester',
    'labBatch',
    'status',
    'actions',
  ];
  protected readonly dataSource = new MatTableDataSource<Student>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');

  ngOnInit(): void {
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

  private performDelete(student: Student): void {
    this.loading.set(true);
    this.studentService.delete(student.id).subscribe({
      next: () => {
        this.snackBar.open('Student deleted successfully', 'Close', { duration: 3000 });
        this.loadStudents();
      },
      error: () => {
        this.snackBar.open('Failed to delete student', 'Close', { duration: 3000 });
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
        this.snackBar.open('Failed to load students', 'Close', { duration: 3000 });
        this.loading.set(false);
      },
    });
  }
}

import { Component, inject, OnInit, signal, ViewChild } from '@angular/core';
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
import { CurriculumService } from '../curriculum.service';
import { Syllabus } from '../curriculum.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-syllabus-list',
  standalone: true,
  imports: [
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
  ],
  templateUrl: './syllabus-list.component.html',
  styleUrl: './syllabus-list.component.scss',
})
export class SyllabusListComponent implements OnInit {
  private readonly curriculumService = inject(CurriculumService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  protected readonly displayedColumns = [
    'courseName',
    'courseCode',
    'version',
    'theoryHours',
    'labHours',
    'isActive',
    'actions',
  ];
  protected readonly dataSource = new MatTableDataSource<Syllabus>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');

  ngOnInit(): void {
    this.load();
  }

  protected applyFilter(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchValue.set(value);
    this.dataSource.filter = value.trim().toLowerCase();
    if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
  }

  protected clearFilter(): void {
    this.searchValue.set('');
    this.dataSource.filter = '';
  }

  protected edit(item: Syllabus): void {
    void this.router.navigate(['/syllabi', item.id, 'edit']);
  }

  protected delete(item: Syllabus): void {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: {
          title: 'Delete Syllabus',
          message: `Delete syllabus for "${item.courseName}" v${item.version}?`,
          confirmText: 'Delete',
          cancelText: 'Cancel',
        },
      })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) this.doDelete(item);
      });
  }

  private doDelete(item: Syllabus): void {
    this.loading.set(true);
    this.curriculumService.deleteSyllabus(item.id).subscribe({
      next: () => {
        this.snackBar.open('Deleted successfully', 'Close', { duration: 3000 });
        this.load();
      },
      error: () => {
        this.snackBar.open('Failed to delete', 'Close', { duration: 3000 });
        this.loading.set(false);
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.curriculumService.getAllSyllabi().subscribe({
      next: (data) => {
        this.dataSource.data = data;
        this.dataSource.paginator = this.paginator;
        this.dataSource.sort = this.sort;
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Failed to load', 'Close', { duration: 3000 });
        this.loading.set(false);
      },
    });
  }
}

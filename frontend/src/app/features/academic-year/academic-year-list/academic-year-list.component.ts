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
import { MatChipsModule } from '@angular/material/chips';
import { AcademicYearService } from '../academic-year.service';
import { AcademicYear } from '../academic-year.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';

@Component({
  selector: 'app-academic-year-list',
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
    MatChipsModule,
  ],
  templateUrl: './academic-year-list.component.html',
  styleUrl: './academic-year-list.component.scss',
})
export class AcademicYearListComponent implements OnInit {
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

  protected readonly displayedColumns = ['name', 'startDate', 'endDate', 'isCurrent', 'actions'];
  protected readonly dataSource = new MatTableDataSource<AcademicYear>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');

  ngOnInit(): void {
    this.loadAcademicYears();
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
        this.snackBar.open('Academic year deleted successfully', 'Close', {
          duration: 3000,
        });
        this.loadAcademicYears();
      },
      error: () => {
        this.snackBar.open('Failed to delete academic year', 'Close', {
          duration: 3000,
        });
        this.loading.set(false);
      },
    });
  }

  private loadAcademicYears(): void {
    this.loading.set(true);
    this.academicYearService.getAllAcademicYears().subscribe({
      next: (academicYears) => {
        this.dataSource.data = academicYears;
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Failed to load academic years', 'Close', {
          duration: 3000,
        });
        this.loading.set(false);
      },
    });
  }
}

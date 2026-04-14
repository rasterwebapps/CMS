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
import { MatTooltipModule } from '@angular/material/tooltip';
import { DecimalPipe } from '@angular/common';
import { FinanceService } from '../finance.service';
import { StudentFeeSummary } from '../finance.model';

@Component({
  selector: 'app-fee-explorer',
  standalone: true,
  imports: [
    DecimalPipe, RouterLink, FormsModule, MatTableModule, MatPaginatorModule, MatSortModule,
    MatInputModule, MatFormFieldModule, MatButtonModule, MatIconModule, MatCardModule,
    MatProgressSpinnerModule, MatSnackBarModule, MatTooltipModule,
  ],
  templateUrl: './fee-explorer.component.html',
  styleUrl: './fee-explorer.component.scss',
})
export class FeeExplorerComponent implements OnInit {
  private readonly financeService = inject(FinanceService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  protected readonly displayedColumns = [
    'rollNumber', 'studentName', 'programName', 'totalFee',
    'totalPaid', 'totalPending', 'totalPenalty', 'allocationStatus', 'actions',
  ];
  protected readonly dataSource = new MatTableDataSource<StudentFeeSummary>([]);
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

  protected searchFromApi(): void {
    this.load(this.searchValue());
  }

  protected viewDetails(student: StudentFeeSummary): void {
    void this.router.navigate(['/student-fees', student.studentId]);
  }

  private load(search?: string): void {
    this.loading.set(true);
    this.financeService.searchStudentFees(search).subscribe({
      next: (result) => {
        this.dataSource.data = result.students;
        this.dataSource.paginator = this.paginator;
        this.dataSource.sort = this.sort;
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Failed to load student fees', 'Close', { duration: 3000 });
        this.loading.set(false);
      },
    });
  }
}

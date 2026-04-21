import { Component, inject, OnInit, signal, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ExaminationService } from '../examination.service';
import { ExamResult, Examination } from '../examination.model';

@Component({
  selector: 'app-exam-result-list',
  standalone: true,
  imports: [
    FormsModule, MatTableModule, MatPaginatorModule, MatSortModule,
    MatInputModule, MatFormFieldModule, MatSelectModule, MatButtonModule, MatIconModule,
    MatCardModule, MatProgressSpinnerModule, MatSnackBarModule,
  ],
  templateUrl: './exam-result-list.component.html',
  styleUrl: './exam-result-list.component.scss',
})
export class ExamResultListComponent implements OnInit {
  private readonly examinationService = inject(ExaminationService);
  private readonly snackBar = inject(MatSnackBar);

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  protected readonly displayedColumns = ['studentRollNumber', 'studentName', 'marksObtained', 'grade', 'status'];
  protected readonly dataSource = new MatTableDataSource<ExamResult>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly examinations = signal<Examination[]>([]);
  protected readonly selectedExamId = signal<number | null>(null);

  ngOnInit(): void {
    this.loadExaminations();
  }

  protected onExaminationChange(examId: number): void {
    this.selectedExamId.set(examId);
    this.loadResults(examId);
  }

  protected applyFilter(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchValue.set(value);
    this.dataSource.filter = value.trim().toLowerCase();
    if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
  }

  protected clearFilter(): void { this.searchValue.set(''); this.dataSource.filter = ''; }

  private loadExaminations(): void {
    this.examinationService.getAll().subscribe({
      next: (data) => this.examinations.set(data),
      error: () => this.snackBar.open('Failed to load examinations', 'Close', { duration: 3000 }),
    });
  }

  private loadResults(examId: number): void {
    this.loading.set(true);
    this.examinationService.getResults(examId).subscribe({
      next: (data) => {
        this.dataSource.data = data;
        this.loading.set(false);
      },
      error: () => { this.snackBar.open('Failed to load results', 'Close', { duration: 3000 }); this.loading.set(false); },
    });
  }
}

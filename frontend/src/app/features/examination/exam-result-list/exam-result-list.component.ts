import { Component, inject, OnInit, signal, ViewChild } from '@angular/core';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ExaminationService } from '../examination.service';
import { ExamResult, Examination } from '../examination.model';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';

@Component({
  selector: 'app-exam-result-list',
  standalone: true,
  imports: [
    MatTableModule, MatPaginatorModule, MatSortModule,
    MatIconModule, MatProgressSpinnerModule, MatSnackBarModule,
    CmsStatusBadgeComponent,
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

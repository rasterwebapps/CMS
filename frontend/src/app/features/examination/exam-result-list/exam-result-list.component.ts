import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ExaminationService } from '../examination.service';
import { ExamResult, Examination } from '../examination.model';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { ToastService } from '../../../core/toast/toast.service';

@Component({
  selector: 'app-exam-result-list',
  standalone: true,
  imports: [
    MatTableModule, MatPaginatorModule, MatSortModule,
    MatIconModule, MatProgressSpinnerModule, CmsStatusBadgeComponent],
  templateUrl: './exam-result-list.component.html',
  styleUrl: './exam-result-list.component.scss',
})
export class ExamResultListComponent implements OnInit {
  private readonly examinationService = inject(ExaminationService);
  private readonly toast = inject(ToastService);

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  // ── Column visibility ────────────────────────────────────────────────────
  protected readonly ALL_COLS = ['studentRollNumber', 'studentName', 'marksObtained', 'grade', 'status'];
  protected readonly COLUMN_LABELS: Record<string, string> = {
    studentRollNumber: 'Roll No.',
    studentName: 'Student',
    marksObtained: 'Marks',
    grade: 'Grade',
    status: 'Status',
  };
  private readonly COLS_KEY = 'exam-result-list-cols';
  private readonly _visibleCols = signal<Set<string>>(this._loadColPrefs());
  protected readonly displayedColumns = computed(() => this.ALL_COLS.filter(c => this._visibleCols().has(c)));
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

  private loadExaminations(): void {
    this.examinationService.getAll().subscribe({
      next: (data) => this.examinations.set(data),
      error: () => this.toast.error('Failed to load examinations'),
    });
  }

  private loadResults(examId: number): void {
    this.loading.set(true);
    this.examinationService.getResults(examId).subscribe({
      next: (data) => {
        this.dataSource.data = data;
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load results'); this.loading.set(false); },
    });
  }
}

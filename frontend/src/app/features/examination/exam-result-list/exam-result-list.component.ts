import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { MatTableModule, MatTableDataSource, MatTable } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ExaminationService } from '../examination.service';
import { ExamResult, Examination } from '../examination.model';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { ToastService } from '../../../core/toast/toast.service';
import { ColumnPickerState, CmsColumnPickerComponent } from '../../../shared/column-picker';

import { ColumnResizeDirective, CmsWrapTextToggleComponent } from '../../../shared/column-resize';
import { TourService } from '../../../shared/tour/tour.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { EXAM_RESULT_LIST_TOUR, EXAM_RESULT_LIST_FLOW_MAP } from '../../../shared/tour/tours/examination.tours';

@Component({
  selector: 'app-exam-result-list',
  standalone: true,
  imports: [
    MatTableModule, MatPaginatorModule, MatSortModule,
    MatIconModule, MatProgressSpinnerModule, CmsStatusBadgeComponent, CmsEmptyStateComponent,
    CmsColumnPickerComponent, ColumnResizeDirective, CmsWrapTextToggleComponent, CmsTourButtonComponent
  ],
  templateUrl: './exam-result-list.component.html',
  styleUrl: './exam-result-list.component.scss',
})
export class ExamResultListComponent implements OnInit {
  private readonly examinationService = inject(ExaminationService);
  private readonly toast = inject(ToastService);
  private readonly tourService = inject(TourService);

  @ViewChild(MatTable) private _matTable?: MatTable<unknown>;
  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  protected readonly colState = new ColumnPickerState({
    storageKey: 'exam-result-list-cols',
    columns: [
      { key: 'studentRollNumber', label: 'Roll No.', mandatory: true },
      { key: 'studentName', label: 'Student' },
      { key: 'marksObtained', label: 'Marks' },
      { key: 'grade', label: 'Grade' },
      { key: 'status', label: 'Status' },
    ],
  });
  protected readonly displayedColumns = computed(() => this.colState.visibleColumns());
  protected readonly dataSource = new MatTableDataSource<ExamResult>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly examinations = signal<Examination[]>([]);
  protected readonly selectedExamId = signal<number | null>(null);

  protected onPinChange(): void { this._matTable?.updateStickyColumnStyles(); }

  ngOnInit(): void {
    this.tourService.register('exam-result-list', EXAM_RESULT_LIST_TOUR);
    this.tourService.registerFlowMap('exam-result-list', EXAM_RESULT_LIST_FLOW_MAP);
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

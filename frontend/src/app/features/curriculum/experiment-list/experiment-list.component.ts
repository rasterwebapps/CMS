import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource, MatTable } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CurriculumService } from '../curriculum.service';
import { Experiment } from '../curriculum.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { ToastService } from '../../../core/toast/toast.service';
import { TourService } from '../../../shared/tour/tour.service';
import { EXPERIMENT_LIST_TOUR } from '../../../shared/tour/tours/experiment.tours';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsIconDeleteComponent, CmsIconEditComponent } from '../../../shared/icons';
import { ColumnPickerState, CmsColumnPickerComponent } from '../../../shared/column-picker';

@Component({
  selector: 'app-experiment-list',
  standalone: true,
  imports: [
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
    CmsEmptyStateComponent,
    CmsRowActionButtonComponent,
    CmsIconDeleteComponent,
    CmsIconEditComponent,
],
  templateUrl: './experiment-list.component.html',
  styleUrl: './experiment-list.component.scss',
})
export class ExperimentListComponent implements OnInit {
  private readonly curriculumService = inject(CurriculumService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly tourService = inject(TourService);

  @ViewChild(MatTable) private _matTable?: MatTable<unknown>;
  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  protected readonly colState = new ColumnPickerState({
    storageKey: 'experiment-list-cols',
    columns: [
      { key: 'experimentNumber', label: '#', mandatory: true },
      { key: 'name', label: 'Name' },
      { key: 'courseName', label: 'Course' },
      { key: 'courseCode', label: 'Code' },
      { key: 'estimatedDurationMinutes', label: 'Duration' },
      { key: 'isActive', label: 'Status' },
      { key: 'actions', label: 'Actions', mandatory: true, pinnable: false },
    ],
  });
  protected readonly displayedColumns = computed(() => this.colState.visibleColumns());
  protected readonly dataSource = new MatTableDataSource<Experiment>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');

  protected onPinChange(): void { this._matTable?.updateStickyColumnStyles(); }

  ngOnInit(): void {
    this.tourService.register('experiment-list', EXPERIMENT_LIST_TOUR);
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

  protected edit(item: Experiment): void {
    void this.router.navigate(['/experiments', item.id, 'edit']);
  }

  protected delete(item: Experiment): void {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: {
          title: 'Delete Experiment',
          message: `Delete "${item.name}"?`,
          confirmText: 'Delete',
          cancelText: 'Cancel',
        },
      })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) this.doDelete(item);
      });
  }




  private doDelete(item: Experiment): void {
    this.loading.set(true);
    this.curriculumService.deleteExperiment(item.id).subscribe({
      next: () => {
        this.toast.success('Deleted successfully');
        this.load();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to delete');
        this.loading.set(false);
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.curriculumService.getAllExperiments().subscribe({
      next: (data) => {
        this.dataSource.data = data;
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load');
        this.loading.set(false);
      },
    });
  }
}

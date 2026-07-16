import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
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
import { Syllabus } from '../curriculum.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { ToastService } from '../../../core/toast/toast.service';
import { TourService } from '../../../shared/tour/tour.service';
import { SYLLABUS_LIST_TOUR } from '../../../shared/tour/tours/syllabus.tours';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsIconToggleStatusComponent } from '../../../shared/icons';
import { ColumnPickerState, CmsColumnPickerComponent } from '../../../shared/column-picker';

import { ColumnResizeDirective, CmsWrapTextToggleComponent } from '../../../shared/column-resize';

@Component({
  selector: 'app-syllabus-list',
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
    CmsIconToggleStatusComponent,
    CmsColumnPickerComponent, ColumnResizeDirective, CmsWrapTextToggleComponent,
],
  templateUrl: './syllabus-list.component.html',
  styleUrl: './syllabus-list.component.scss',
})
export class SyllabusListComponent implements OnInit {
  private readonly curriculumService = inject(CurriculumService);
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
    storageKey: 'syllabus-list-cols',
    columns: [
      { key: 'subjectName', label: 'Subject', mandatory: true },
      { key: 'subjectCode', label: 'Code' },
      { key: 'curriculumVersionName', label: 'Curriculum Version' },
      { key: 'termNumber', label: 'Term' },
      { key: 'version', label: 'Version' },
      { key: 'theoryHours', label: 'Theory Hrs' },
      { key: 'labHours', label: 'Lab Hrs' },
      { key: 'clinicalHours', label: 'Clinical Hrs' },
      { key: 'isActive', label: 'Status' },
      { key: 'actions', label: 'Actions', mandatory: true, pinnable: false },
    ],
  });
  protected readonly displayedColumns = computed(() => this.colState.visibleColumns());
  protected readonly dataSource = new MatTableDataSource<Syllabus>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');

  protected onPinChange(): void { this._matTable?.updateStickyColumnStyles(); }

  ngOnInit(): void {
    this.tourService.register('syllabus-list', SYLLABUS_LIST_TOUR);
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

  /** A syllabus version is immutable once created — this is the only permitted change.
   *  Activating clears every other active version for the same subject+term mapping. */
  protected toggleStatus(item: Syllabus): void {
    const nextAction = item.isActive ? 'Deactivate' : 'Activate';
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: `${nextAction} Syllabus`,
        message: `${nextAction} "${item.subjectName}" v${item.version}?`
          + (item.isActive ? '' : ' This will deactivate any other active version for this subject and term.'),
        confirmText: nextAction,
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) this.doToggle(item);
    });
  }

  private doToggle(item: Syllabus): void {
    this.loading.set(true);
    this.curriculumService.setSyllabusActive(item.id, { isActive: !item.isActive }).subscribe({
      next: () => {
        this.toast.success(`Syllabus ${item.isActive ? 'deactivated' : 'activated'} successfully`);
        this.load();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? `Failed to ${item.isActive ? 'deactivate' : 'activate'} syllabus`);
        this.loading.set(false);
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.curriculumService.getAllSyllabi().subscribe({
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

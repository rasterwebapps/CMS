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
import { LabCurriculumMapping } from '../curriculum.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsTypeBadgeComponent } from '../../../shared/type-badge/type-badge.component';
import { CmsIconDeleteComponent, CmsIconEditComponent } from '../../../shared/icons';
import { ColumnPickerState, CmsColumnPickerComponent } from '../../../shared/column-picker';

import { ColumnResizeDirective, CmsWrapTextToggleComponent } from '../../../shared/column-resize';

@Component({
  selector: 'app-co-po-mapping',
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
    CmsTypeBadgeComponent,
    CmsRowActionButtonComponent,
    CmsIconDeleteComponent,
    CmsIconEditComponent,
    CmsColumnPickerComponent, ColumnResizeDirective, CmsWrapTextToggleComponent,
],
  templateUrl: './co-po-mapping.component.html',
  styleUrl: './co-po-mapping.component.scss',
})
export class CoPoMappingComponent implements OnInit {
  private readonly curriculumService = inject(CurriculumService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);

  @ViewChild(MatTable) private _matTable?: MatTable<unknown>;
  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  protected readonly colState = new ColumnPickerState({
    storageKey: 'co-po-mapping-columns',
    columns: [
      { key: 'experimentName', label: 'Experiment', mandatory: true },
      { key: 'experimentNumber', label: '#' },
      { key: 'subjectName', label: 'Subject' },
      { key: 'outcomeType', label: 'Type' },
      { key: 'outcomeCode', label: 'Code' },
      { key: 'outcomeDescription', label: 'Outcome' },
      { key: 'mappingLevel', label: 'Level' },
      { key: 'actions', label: 'Actions', mandatory: true, pinnable: false },
    ],
  });
  protected readonly displayedColumns = computed(() => this.colState.visibleColumns());
  protected readonly dataSource = new MatTableDataSource<LabCurriculumMapping>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');

  protected onPinChange(): void { this._matTable?.updateStickyColumnStyles(); }

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

  protected edit(item: LabCurriculumMapping): void {
    void this.router.navigate(['/curriculum-mappings', item.id, 'edit']);
  }

  protected delete(item: LabCurriculumMapping): void {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: {
          title: 'Delete Mapping',
          message: `Delete mapping "${item.outcomeCode}" for "${item.experimentName}"?`,
          confirmText: 'Delete',
          cancelText: 'Cancel',
        },
      })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) this.doDelete(item);
      });
  }

  private doDelete(item: LabCurriculumMapping): void {
    this.loading.set(true);
    this.curriculumService.deleteMapping(item.id).subscribe({
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
    this.curriculumService.getAllMappings().subscribe({
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

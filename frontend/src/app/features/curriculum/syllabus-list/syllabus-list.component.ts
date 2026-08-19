import { Component, computed, effect, inject, OnInit, signal, ViewChild } from '@angular/core';
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
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { SYLLABUS_LIST_TOUR, SYLLABUS_LIST_FLOW_MAP } from '../../../shared/tour/tours/syllabus.tours';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsIconToggleStatusComponent } from '../../../shared/icons';
import { ColumnPickerState, CmsColumnPickerComponent } from '../../../shared/column-picker';
import { PermissionService } from '../../../core/permissions/permission.service';

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
    CmsTourButtonComponent,
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
  private readonly permissionService = inject(PermissionService);

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

  private readonly allSyllabi = signal<Syllabus[]>([]);
  protected readonly selectedCurriculumVersionId = signal<number | null>(null);
  protected readonly selectedTermNumber = signal<number | null>(null);

  /** Distinct curriculum versions actually present in the loaded syllabi, not every
   *  curriculum version in the system — no point offering one with zero syllabi to filter by. */
  protected readonly curriculumVersionOptions = computed(() => {
    const map = new Map<number, string>();
    for (const s of this.allSyllabi()) {
      if (!map.has(s.curriculumVersionId)) map.set(s.curriculumVersionId, s.curriculumVersionName);
    }
    return Array.from(map, ([id, name]) => ({ id, name })).sort((a, b) => a.name.localeCompare(b.name));
  });

  /** Term options scope to the selected curriculum version (terms differ per curriculum) —
   *  mirrors the cascading curriculum-version -> term picker used on the syllabus form. */
  protected readonly termNumberOptions = computed(() => {
    const cvId = this.selectedCurriculumVersionId();
    const terms = new Set(
      this.allSyllabi()
        .filter((s) => cvId === null || s.curriculumVersionId === cvId)
        .map((s) => s.termNumber)
    );
    return Array.from(terms).sort((a, b) => a - b);
  });

  protected readonly filteredSyllabi = computed(() => {
    const cvId = this.selectedCurriculumVersionId();
    const term = this.selectedTermNumber();
    const search = this.searchValue().trim().toLowerCase();
    return this.allSyllabi().filter((s) => {
      if (cvId !== null && s.curriculumVersionId !== cvId) return false;
      if (term !== null && s.termNumber !== term) return false;
      if (search) {
        const haystack = [s.subjectName, s.subjectCode, s.curriculumVersionName].join(' ').toLowerCase();
        if (!haystack.includes(search)) return false;
      }
      return true;
    });
  });

  protected readonly totalSyllabi = computed(() => this.allSyllabi().length);

  protected readonly hasActiveFilters = computed(() =>
    !!this.searchValue() || this.selectedCurriculumVersionId() !== null || this.selectedTermNumber() !== null
  );

  constructor() {
    effect(() => {
      this.dataSource.data = this.filteredSyllabi();
      if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
    });
  }

  protected onPinChange(): void { this._matTable?.updateStickyColumnStyles(); }

  protected canManage(): boolean {
    return this.permissionService.has('SYLLABUS_MANAGE');
  }

  ngOnInit(): void {
    this.tourService.register('syllabus-list', SYLLABUS_LIST_TOUR);
    this.tourService.registerFlowMap('syllabus-list', SYLLABUS_LIST_FLOW_MAP);
    this.load();
  }

  protected applyFilter(event: Event): void {
    this.searchValue.set((event.target as HTMLInputElement).value);
  }

  protected clearFilter(): void {
    this.searchValue.set('');
  }

  protected onCurriculumVersionChange(id: number | null): void {
    this.selectedCurriculumVersionId.set(id);
    this.selectedTermNumber.set(null);
  }

  protected onTermNumberChange(term: number | null): void {
    this.selectedTermNumber.set(term);
  }

  protected clearFilters(): void {
    this.searchValue.set('');
    this.selectedCurriculumVersionId.set(null);
    this.selectedTermNumber.set(null);
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
        this.allSyllabi.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load');
        this.loading.set(false);
      },
    });
  }
}

import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource, MatTable } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { forkJoin, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';

import { TimetableService } from '../../timetable/timetable.service';
import { ClassScheduleOccurrence } from '../../timetable/timetable.model';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { TermInstance } from '../../academic-year/academic-year.model';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { LAB_SCHEDULE_LIST_TOUR, LAB_SCHEDULE_LIST_FLOW_MAP } from '../../../shared/tour/tours/lab-schedule.tours';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsTypeBadgeComponent } from '../../../shared/type-badge/type-badge.component';
import { ColumnPickerState, CmsColumnPickerComponent } from '../../../shared/column-picker';
import { ColumnResizeDirective, CmsWrapTextToggleComponent } from '../../../shared/column-resize';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ExportButtonComponent, ExportFormat } from '../../../shared/export-button/export-button.component';
import { CmsInfiniteSelectComponent } from '../../../shared/infinite-select/infinite-select.component';
import { InfiniteSelectValue } from '../../../shared/infinite-select/infinite-select.model';
import { staticOptionsFetchPage } from '../../../shared/infinite-select/infinite-select.utils';
import { RescheduleModalComponent } from '../../timetable/reschedule/reschedule-modal.component';
import { StaffSwapModalComponent } from '../../timetable/staff-swap-modal/staff-swap-modal.component';

function todayIso(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

@Component({
  selector: 'app-lab-schedule-list',
  standalone: true,
  imports: [
    CmsTourButtonComponent,
    CmsEmptyStateComponent,
    CmsRowActionButtonComponent,
    CmsTypeBadgeComponent,
    ExportButtonComponent,
    CmsInfiniteSelectComponent,
    FormsModule, MatTableModule, MatPaginatorModule,
    MatProgressSpinnerModule, MatDialogModule, MatTooltipModule,
    CmsColumnPickerComponent, ColumnResizeDirective, CmsWrapTextToggleComponent,
  ],
  templateUrl: './lab-schedule-list.component.html',
  styleUrl: './lab-schedule-list.component.scss',
})
export class LabScheduleListComponent implements OnInit {
  private readonly timetableService = inject(TimetableService);
  private readonly academicYearService = inject(AcademicYearService);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly permissionService = inject(PermissionService);
  private readonly tourService = inject(TourService);

  @ViewChild(MatTable) private _matTable?: MatTable<unknown>;
  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }

  protected readonly colState = new ColumnPickerState({
    storageKey: 'lab-schedule-list-cols',
    columns: [
      { key: 'time', label: 'Time', mandatory: true },
      { key: 'sessionType', label: 'Type' },
      { key: 'roomName', label: 'Room' },
      { key: 'subjectName', label: 'Subject' },
      { key: 'subjectCode', label: 'Code' },
      { key: 'facultyName', label: 'Faculty' },
      { key: 'batchName', label: 'Batch' },
      { key: 'occurrenceStatus', label: 'Status' },
      { key: 'actions', label: 'Actions', mandatory: true, pinnable: false },
    ],
  });
  protected readonly displayedColumns = computed(() => this.colState.visibleColumns());
  protected readonly dataSource = new MatTableDataSource<ClassScheduleOccurrence>([]);
  protected readonly loading = signal(false);
  protected readonly exporting = signal(false);
  protected readonly canExport = computed(() => this.permissionService.has('TIMETABLE_OCCURRENCE_EXPORT'));
  protected readonly canSwap = computed(() => this.permissionService.has('TIMETABLE_STAFF_SWAP'));
  protected readonly canReschedule = computed(() => this.permissionService.has('TIMETABLE_OCCURRENCE_RESCHEDULE'));

  protected readonly selectedDate = signal(todayIso());
  private readonly termInstance = signal<TermInstance | null>(null);
  protected readonly termLabel = computed(() => {
    const t = this.termInstance();
    return t ? `${t.academicYearName} ${t.termType}` : null;
  });
  protected readonly noTermForDate = signal(false);

  private readonly allOccurrences = signal<ClassScheduleOccurrence[]>([]);
  protected readonly searchValue = signal('');
  protected readonly selectedType = signal('');
  protected readonly selectedRoom = signal('');
  protected readonly selectedFaculty = signal('');
  protected readonly selectedStatus = signal('');

  protected readonly typeFetchPage = staticOptionsFetchPage(() =>
    Array.from(new Set(this.allOccurrences().map(o => o.session.sessionType)))
      .map(t => ({ id: t, name: t.charAt(0) + t.slice(1).toLowerCase() })));
  protected readonly roomFetchPage = staticOptionsFetchPage(() =>
    Array.from(new Set(this.allOccurrences().map(o => o.session.roomName).filter((n): n is string => !!n)))
      .sort().map(n => ({ id: n, name: n })));
  protected readonly facultyFetchPage = staticOptionsFetchPage(() =>
    Array.from(new Set(this.allOccurrences().map(o => o.session.facultyName).filter((n): n is string => !!n)))
      .sort().map(n => ({ id: n, name: n })));
  protected readonly statusFetchPage = staticOptionsFetchPage(() => [
    { id: 'HELD', name: 'Held' },
    { id: 'SUBSTITUTED', name: 'Substituted' },
    { id: 'RESCHEDULED', name: 'Rescheduled' },
    { id: 'CANCELLED', name: 'Cancelled' },
  ]);

  protected readonly hasActiveFilters = computed(() =>
    !!this.searchValue() || !!this.selectedType() || !!this.selectedRoom()
    || !!this.selectedFaculty() || !!this.selectedStatus());

  protected readonly filteredCount = computed(() => this.dataSource.filteredData.length);
  protected readonly totalCount = computed(() => this.allOccurrences().length);

  protected onPinChange(): void { this._matTable?.updateStickyColumnStyles(); }

  ngOnInit(): void {
    this.tourService.register('lab-schedule-list', LAB_SCHEDULE_LIST_TOUR);
    this.tourService.registerFlowMap('lab-schedule-list', LAB_SCHEDULE_LIST_FLOW_MAP);
    this.dataSource.filterPredicate = (row, filter) => {
      const f = JSON.parse(filter) as {
        search: string; type: string; room: string; faculty: string; status: string;
      };
      if (f.type && row.session.sessionType !== f.type) return false;
      if (f.room && row.session.roomName !== f.room) return false;
      if (f.faculty && row.session.facultyName !== f.faculty) return false;
      if (f.status && row.occurrenceStatus !== f.status) return false;
      if (f.search) {
        const haystack = [
          row.session.subjectName, row.session.subjectCode, row.session.facultyName,
          row.session.roomName, row.session.batchName,
        ].filter(Boolean).join(' ').toLowerCase();
        if (!haystack.includes(f.search)) return false;
      }
      return true;
    };
    this.load();
  }

  private applyFilterPredicate(): void {
    this.dataSource.filter = JSON.stringify({
      search: this.searchValue().trim().toLowerCase(),
      type: this.selectedType(),
      room: this.selectedRoom(),
      faculty: this.selectedFaculty(),
      status: this.selectedStatus(),
    });
  }

  protected onDateChanged(): void {
    this.load();
  }

  protected onSearch(event: Event): void {
    this.searchValue.set((event.target as HTMLInputElement).value);
    this.applyFilterPredicate();
  }

  protected clearSearch(): void {
    this.searchValue.set('');
    this.applyFilterPredicate();
  }

  protected onTypeFilterChange(value: InfiniteSelectValue | null): void {
    this.selectedType.set(value != null ? String(value) : '');
    this.applyFilterPredicate();
  }

  protected onRoomFilterChange(value: InfiniteSelectValue | null): void {
    this.selectedRoom.set(value != null ? String(value) : '');
    this.applyFilterPredicate();
  }

  protected onFacultyFilterChange(value: InfiniteSelectValue | null): void {
    this.selectedFaculty.set(value != null ? String(value) : '');
    this.applyFilterPredicate();
  }

  protected onStatusFilterChange(value: InfiniteSelectValue | null): void {
    this.selectedStatus.set(value != null ? String(value) : '');
    this.applyFilterPredicate();
  }

  protected clearFilters(): void {
    this.searchValue.set('');
    this.selectedType.set('');
    this.selectedRoom.set('');
    this.selectedFaculty.set('');
    this.selectedStatus.set('');
    this.applyFilterPredicate();
  }

  protected canActOn(occ: ClassScheduleOccurrence): boolean {
    return occ.occurrenceStatus === 'HELD' || occ.occurrenceStatus === 'SUBSTITUTED';
  }

  protected openSwap(occ: ClassScheduleOccurrence): void {
    this.dialog.open(StaffSwapModalComponent, { data: { occurrence: occ }, width: '460px' })
      .afterClosed().subscribe((changed) => { if (changed) this.load(); });
  }

  protected openReschedule(occ: ClassScheduleOccurrence): void {
    this.dialog.open(RescheduleModalComponent, { data: { occurrence: occ }, width: '460px' })
      .afterClosed().subscribe((changed) => { if (changed) this.load(); });
  }

  private load(): void {
    this.loading.set(true);
    this.noTermForDate.set(false);
    const date = this.selectedDate();

    // GET /term-instances requires academicYearId -- no unfiltered "all terms" endpoint, so fetch
    // every academic year and flatten their terms, matching the pattern used elsewhere in this app
    // (lab-schedule-form.component.ts, capacity-auto-plan.component.ts), then find the one whose
    // date range actually contains the picked date.
    this.academicYearService.getAllAcademicYears().pipe(
      switchMap((years) => years.length
        ? forkJoin(years.map((y) => this.academicYearService.getTermInstancesByAcademicYear(y.id)))
        : of([])),
      map((perYear) => perYear.flat()),
    ).subscribe({
      next: (terms) => {
        const term = terms.find((t) => date >= t.startDate && date <= t.endDate) ?? null;
        this.termInstance.set(term);
        if (!term) {
          this.noTermForDate.set(true);
          this.allOccurrences.set([]);
          this.dataSource.data = [];
          this.loading.set(false);
          return;
        }
        this.timetableService.getOccurrences(term.id, date, date, 'browse').subscribe({
          next: (occurrences) => {
            this.allOccurrences.set(occurrences);
            this.dataSource.data = occurrences;
            this.applyFilterPredicate();
            this.loading.set(false);
          },
          error: () => { this.toast.error('Failed to load schedules for this date'); this.loading.set(false); },
        });
      },
      error: () => { this.toast.error('Failed to resolve the term for this date'); this.loading.set(false); },
    });
  }

  protected onExport(format: ExportFormat): void {
    const term = this.termInstance();
    if (this.exporting() || !term) return;
    if (this.totalCount() === 0) {
      this.toast.error('No data available to export.');
      return;
    }
    this.exporting.set(true);
    this.timetableService.exportOccurrences(format, term.id, this.selectedDate()).subscribe({
      next: (blob) => {
        const ext = format === 'pdf' ? 'pdf' : 'xlsx';
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `class-schedules-${this.selectedDate()}.${ext}`;
        a.click();
        URL.revokeObjectURL(url);
        this.exporting.set(false);
      },
      error: () => {
        this.toast.error('Export failed. Please try again.');
        this.exporting.set(false);
      },
    });
  }
}

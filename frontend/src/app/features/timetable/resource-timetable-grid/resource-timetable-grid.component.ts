import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear, TermInstance } from '../../academic-year/academic-year.model';
import { TimetableService } from '../timetable.service';
import { ResourceGridRow, ResourceGridType } from '../timetable.model';
import { WEEK_GRID_DAYS, WEEK_GRID_DAY_LABELS } from '../../../shared/week-grid/week-grid.model';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { TourService } from '../../../shared/tour/tour.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { RESOURCE_TIMETABLE_GRID_TOUR, RESOURCE_TIMETABLE_GRID_FLOW_MAP } from '../../../shared/tour/tours/resource-timetable-grid.tours';

interface TimeColumn {
  key: string;
  label: string;
  startTime: string;
  endTime: string;
}

@Component({
  selector: 'app-resource-timetable-grid',
  standalone: true,
  imports: [FormsModule, MatProgressSpinnerModule, CmsEmptyStateComponent, CmsTourButtonComponent],
  templateUrl: './resource-timetable-grid.component.html',
  styleUrl: './resource-timetable-grid.component.scss',
})
export class ResourceTimetableGridComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly timetableService = inject(TimetableService);
  private readonly permissionService = inject(PermissionService);
  private readonly toast = inject(ToastService);
  private readonly tourService = inject(TourService);

  protected readonly canViewFaculty = computed(() => this.permissionService.has('TIMETABLE_FACULTY_GRID_VIEW'));
  protected readonly canViewClassroom = computed(() => this.permissionService.has('TIMETABLE_CLASSROOM_GRID_VIEW'));

  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly termInstances = signal<TermInstance[]>([]);
  protected readonly loading = signal(false);
  protected readonly termsLoading = signal(false);

  protected selectedAcademicYearId: number | null = null;
  protected selectedTermInstanceId: number | null = null;

  protected readonly resourceType = signal<ResourceGridType>('FACULTY');
  protected readonly dayOfWeek = signal<string>(WEEK_GRID_DAYS[0]);
  protected readonly days = WEEK_GRID_DAYS;
  protected readonly dayLabels = WEEK_GRID_DAY_LABELS;

  /** DATE (default) resolves through any DayMappingOverride for a real calendar date, e.g. a
   *  compensatory working day correctly shows the borrowed weekday's schedules. WEEKDAY is a
   *  pure planning-mode fallback with no day-mapping awareness (the recurring weekly template,
   *  not tied to any specific date). */
  protected readonly viewMode = signal<'DATE' | 'WEEKDAY'>('DATE');
  protected selectedDate: string = new Date().toISOString().slice(0, 10);

  protected readonly rows = signal<ResourceGridRow[]>([]);

  protected readonly timeColumns = computed<TimeColumn[]>(() => {
    const seen = new Map<string, TimeColumn>();
    for (const row of this.rows()) {
      for (const s of row.sessions) {
        const key = `${s.startTime}-${s.endTime}`;
        if (!seen.has(key)) {
          seen.set(key, { key, label: s.slotName || `${s.startTime}–${s.endTime}`, startTime: s.startTime, endTime: s.endTime });
        }
      }
    }
    return Array.from(seen.values()).sort((a, b) => a.startTime.localeCompare(b.startTime));
  });

  protected readonly isEmpty = computed(() => this.rows().every((r) => r.sessions.length === 0));

  ngOnInit(): void {
    this.tourService.register('resource-timetable-grid', RESOURCE_TIMETABLE_GRID_TOUR);
    this.tourService.registerFlowMap('resource-timetable-grid', RESOURCE_TIMETABLE_GRID_FLOW_MAP);

    this.resourceType.set(this.canViewFaculty() ? 'FACULTY' : 'CLASSROOM');

    this.academicYearService.getAllAcademicYears().subscribe({
      next: (years) => {
        this.academicYears.set(years);
        const initialYearId = years.find((y) => y.isCurrent)?.id ?? years[0]?.id ?? null;
        if (initialYearId) {
          this.selectedAcademicYearId = initialYearId;
          this.loadTermInstances(initialYearId);
        }
      },
      error: () => { this.toast.error('Failed to load academic years'); },
    });
  }

  protected onAcademicYearChange(): void {
    this.selectedTermInstanceId = null;
    this.rows.set([]);
    if (this.selectedAcademicYearId) this.loadTermInstances(this.selectedAcademicYearId);
  }

  protected onTermChange(): void {
    this.load();
  }

  protected setResourceType(type: ResourceGridType): void {
    this.resourceType.set(type);
    this.load();
  }

  protected onDayChange(day: string): void {
    this.dayOfWeek.set(day);
    this.load();
  }

  protected onDateChange(date: string): void {
    this.selectedDate = date;
    this.load();
  }

  protected setViewMode(mode: 'DATE' | 'WEEKDAY'): void {
    this.viewMode.set(mode);
    this.load();
  }

  protected cellsFor(row: ResourceGridRow, column: TimeColumn) {
    return row.sessions.filter((s) => s.startTime === column.startTime && s.endTime === column.endTime);
  }

  private loadTermInstances(academicYearId: number): void {
    this.termsLoading.set(true);
    this.academicYearService.getTermInstancesByAcademicYear(academicYearId).subscribe({
      next: (terms) => {
        this.termInstances.set(terms);
        this.termsLoading.set(false);
        this.selectedTermInstanceId = terms[0]?.id ?? null;
        this.load();
      },
      error: () => { this.toast.error('Failed to load term instances'); this.termsLoading.set(false); },
    });
  }

  private load(): void {
    if (!this.selectedTermInstanceId) { this.rows.set([]); return; }
    this.loading.set(true);
    const opts = this.viewMode() === 'DATE' ? { date: this.selectedDate } : { dayOfWeek: this.dayOfWeek() };
    this.timetableService.getResourceGrid(this.resourceType(), this.selectedTermInstanceId, opts).subscribe({
      next: (data) => { this.rows.set(data); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load resource grid'); this.loading.set(false); },
    });
  }
}

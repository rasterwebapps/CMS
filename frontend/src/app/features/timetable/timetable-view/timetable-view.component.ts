import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear, TermInstance } from '../../academic-year/academic-year.model';
import { TimetableService } from '../timetable.service';
import { ClassSchedule, ClassScheduleOccurrence } from '../timetable.model';
import { CmsWeekGridComponent } from '../../../shared/week-grid/week-grid.component';
import { CmsMonthGridComponent } from '../../../shared/month-grid/month-grid.component';
import { CmsDayAgendaComponent } from '../../../shared/day-agenda/day-agenda.component';
import { ToastService } from '../../../core/toast/toast.service';
import { PermissionService } from '../../../core/permissions/permission.service';
import { RoomRelocationModalComponent } from '../room-relocation/room-relocation-modal.component';
import { TourService } from '../../../shared/tour/tour.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TIMETABLE_VIEW_TOUR, TIMETABLE_VIEW_FLOW_MAP } from '../../../shared/tour/tours/timetable-view.tours';

export type TimetableViewMode = 'week' | 'month' | 'day';

@Component({
  selector: 'app-timetable-view',
  standalone: true,
  imports: [FormsModule, MatProgressSpinnerModule, MatDialogModule, CmsWeekGridComponent, CmsMonthGridComponent, CmsDayAgendaComponent, CmsTourButtonComponent],
  templateUrl: './timetable-view.component.html',
  styleUrl: './timetable-view.component.scss',
})
export class TimetableViewComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly timetableService = inject(TimetableService);
  private readonly toast = inject(ToastService);
  private readonly route = inject(ActivatedRoute);
  private readonly permissionService = inject(PermissionService);
  private readonly dialog = inject(MatDialog);
  private readonly tourService = inject(TourService);

  protected readonly canRelocateRoom = computed(() => this.permissionService.has('TIMETABLE_ROOM_RELOCATE'));

  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly termInstances = signal<TermInstance[]>([]);
  protected readonly sessions = signal<ClassSchedule[]>([]);
  protected readonly loading = signal(false);
  protected readonly termsLoading = signal(false);

  protected selectedAcademicYearId: number | null = null;
  protected selectedTermInstanceId: number | null = null;

  protected readonly selectedTerm = computed(() =>
    this.termInstances().find((t) => t.id === this.selectedTermInstanceId) ?? null);

  protected readonly viewMode = signal<TimetableViewMode>('week');
  protected readonly monthYear = signal(new Date().getFullYear());
  protected readonly monthMonth = signal(new Date().getMonth());
  protected readonly dayDate = signal(new Date().toISOString().slice(0, 10));
  protected readonly occurrences = signal<ClassScheduleOccurrence[]>([]);
  protected readonly occurrencesLoading = signal(false);

  protected readonly selectedFaculty = signal<string | null>(null);
  protected readonly selectedRoom = signal<string | null>(null);
  protected readonly selectedBatch = signal<string | null>(null);

  /** Filter option lists are derived from the term's own loaded sessions rather than fetched
   *  from the Faculty/Classroom/Lab/Batch masters — keeps the dropdowns scoped to only what's
   *  actually scheduled this term instead of every faculty/room in the college. */
  protected readonly facultyOptions = computed(() =>
    Array.from(new Set(this.sessions().map((s) => s.facultyName))).sort());

  protected readonly roomOptions = computed(() =>
    Array.from(new Set(this.sessions().map((s) => s.roomName))).sort());

  protected readonly batchOptions = computed(() =>
    Array.from(new Set(this.sessions().flatMap((s) => s.batchName ? [s.batchName] : []))).sort());

  protected readonly filteredSessions = computed(() => {
    const faculty = this.selectedFaculty();
    const room = this.selectedRoom();
    const batch = this.selectedBatch();
    return this.sessions().filter((s) =>
      (!faculty || s.facultyName === faculty) &&
      (!room || s.roomName === room) &&
      (!batch || s.batchName === batch));
  });

  /** Same Faculty/Room/Batch filters applied to the date-exploded occurrences behind Month/Day
   *  view, so switching view modes doesn't silently drop an active filter. */
  protected readonly filteredOccurrences = computed(() => {
    const faculty = this.selectedFaculty();
    const room = this.selectedRoom();
    const batch = this.selectedBatch();
    return this.occurrences().filter((o) =>
      (!faculty || o.session.facultyName === faculty) &&
      (!room || o.session.roomName === room) &&
      (!batch || o.session.batchName === batch));
  });

  ngOnInit(): void {
    this.tourService.register('timetable-view', TIMETABLE_VIEW_TOUR);
    this.tourService.registerFlowMap('timetable-view', TIMETABLE_VIEW_FLOW_MAP);

    const qpAcademicYearId = Number(this.route.snapshot.queryParamMap.get('academicYearId')) || null;
    const qpTermInstanceId = Number(this.route.snapshot.queryParamMap.get('termInstanceId')) || null;

    this.academicYearService.getAllAcademicYears().subscribe({
      next: (years) => {
        this.academicYears.set(years);
        const initialYearId = qpAcademicYearId
          ?? years.find((y) => y.isCurrent)?.id
          ?? years[0]?.id
          ?? null;
        if (initialYearId) {
          this.selectedAcademicYearId = initialYearId;
          this.loadTermInstances(initialYearId, qpTermInstanceId ?? undefined);
        }
      },
      error: () => { this.toast.error('Failed to load academic years'); },
    });
  }

  protected onAcademicYearChange(): void {
    this.selectedTermInstanceId = null;
    this.sessions.set([]);
    if (this.selectedAcademicYearId) this.loadTermInstances(this.selectedAcademicYearId);
  }

  protected onTermChange(): void {
    if (this.selectedTermInstanceId) {
      this.loadPublished(this.selectedTermInstanceId);
      this.resetMonthDayDefaults();
      this.refreshCurrentViewMode();
    } else {
      this.sessions.set([]);
    }
  }

  protected setViewMode(mode: TimetableViewMode): void {
    this.viewMode.set(mode);
    if (mode === 'month') this.loadMonthOccurrences(this.monthYear(), this.monthMonth());
    else if (mode === 'day') this.loadDayOccurrences(this.dayDate());
  }

  protected onMonthChange(change: { year: number; month: number }): void {
    this.monthYear.set(change.year);
    this.monthMonth.set(change.month);
    this.loadMonthOccurrences(change.year, change.month);
  }

  protected onMonthDayClick(iso: string): void {
    this.dayDate.set(iso);
    this.viewMode.set('day');
    this.loadDayOccurrences(iso);
  }

  protected onDayDateChange(iso: string): void {
    const clamped = this.clampToTerm(iso, this.selectedTerm());
    this.dayDate.set(clamped);
    this.loadDayOccurrences(clamped);
  }

  protected get dayMin(): string | null {
    return this.selectedTerm()?.startDate ?? null;
  }

  protected get dayMax(): string | null {
    return this.selectedTerm()?.endDate ?? null;
  }

  private refreshCurrentViewMode(): void {
    const mode = this.viewMode();
    if (mode === 'month') this.loadMonthOccurrences(this.monthYear(), this.monthMonth());
    else if (mode === 'day') this.loadDayOccurrences(this.dayDate());
  }

  private resetMonthDayDefaults(): void {
    const term = this.selectedTerm();
    if (!term) return;
    const today = new Date().toISOString().slice(0, 10);
    const clamped = this.clampToTerm(today, term);
    this.dayDate.set(clamped);
    const [y, m] = clamped.split('-').map(Number);
    this.monthYear.set(y);
    this.monthMonth.set(m - 1);
  }

  private clampToTerm(date: string, term: TermInstance | null): string {
    if (!term) return date;
    if (date < term.startDate) return term.startDate;
    if (date > term.endDate) return term.endDate;
    return date;
  }

  private loadMonthOccurrences(year: number, month: number): void {
    if (!this.selectedTermInstanceId) return;
    const from = new Date(year, month, 1).toISOString().slice(0, 10);
    const to = new Date(year, month + 1, 0).toISOString().slice(0, 10);
    this.occurrencesLoading.set(true);
    this.timetableService.getOccurrences(this.selectedTermInstanceId, from, to, 'browse').subscribe({
      next: (occs) => { this.occurrences.set(occs); this.occurrencesLoading.set(false); },
      error: () => { this.toast.error('Failed to load month view'); this.occurrencesLoading.set(false); },
    });
  }

  private loadDayOccurrences(iso: string): void {
    if (!this.selectedTermInstanceId) return;
    this.occurrencesLoading.set(true);
    this.timetableService.getOccurrences(this.selectedTermInstanceId, iso, iso, 'browse').subscribe({
      next: (occs) => { this.occurrences.set(occs); this.occurrencesLoading.set(false); },
      error: () => { this.toast.error('Failed to load day view'); this.occurrencesLoading.set(false); },
    });
  }

  /** Day view only -- Week view has no date concept (it renders the recurring weekly pattern),
   *  so a per-date relocation can't be triggered from there. */
  protected openRoomRelocation(occurrence: ClassScheduleOccurrence): void {
    this.dialog.open(RoomRelocationModalComponent, { data: { occurrence }, width: '440px' })
      .afterClosed().subscribe((changed) => {
        if (changed) this.loadDayOccurrences(this.dayDate());
      });
  }

  private loadTermInstances(academicYearId: number, preselectTermInstanceId?: number): void {
    this.termsLoading.set(true);
    this.academicYearService.getTermInstancesByAcademicYear(academicYearId).subscribe({
      next: (terms) => {
        this.termInstances.set(terms);
        this.termsLoading.set(false);
        const preselect = preselectTermInstanceId && terms.some((t) => t.id === preselectTermInstanceId)
          ? preselectTermInstanceId
          : terms[0]?.id ?? null;
        this.selectedTermInstanceId = preselect;
        if (preselect) {
          this.loadPublished(preselect);
          this.resetMonthDayDefaults();
          this.refreshCurrentViewMode();
        } else {
          this.sessions.set([]);
        }
      },
      error: () => { this.toast.error('Failed to load term instances'); this.termsLoading.set(false); },
    });
  }

  private loadPublished(termInstanceId: number): void {
    this.loading.set(true);
    this.resetFilters();
    this.timetableService.getPublished(termInstanceId).subscribe({
      next: (data) => { this.sessions.set(data); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load timetable'); this.loading.set(false); },
    });
  }

  private resetFilters(): void {
    this.selectedFaculty.set(null);
    this.selectedRoom.set(null);
    this.selectedBatch.set(null);
  }
}

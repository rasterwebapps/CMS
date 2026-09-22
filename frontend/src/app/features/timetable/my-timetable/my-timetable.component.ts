import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear, TermInstance } from '../../academic-year/academic-year.model';
import { TimetableService } from '../timetable.service';
import { ClassSchedule } from '../timetable.model';
import { CmsWeekGridComponent } from '../../../shared/week-grid/week-grid.component';
import { WeekGridHolidayInfo, WeekGridSession } from '../../../shared/week-grid/week-grid.model';
import { CmsWeekNavigatorComponent } from '../../../shared/week-navigator/week-navigator.component';
import { CmsDayAgendaComponent } from '../../../shared/day-agenda/day-agenda.component';
import { ClassScheduleOccurrence } from '../timetable.model';
import { ToastService } from '../../../core/toast/toast.service';
import { PermissionService } from '../../../core/permissions/permission.service';
import { LogProgressDialogComponent } from '../log-progress-dialog/log-progress-dialog.component';
import { TourService } from '../../../shared/tour/tour.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { MY_TIMETABLE_TOUR, MY_TIMETABLE_FLOW_MAP } from '../../../shared/tour/tours/my-timetable.tours';
import { CmsInfiniteSelectComponent } from '../../../shared/infinite-select/infinite-select.component';
import { InfiniteSelectValue } from '../../../shared/infinite-select/infinite-select.model';
import { staticOptionsFetchPage } from '../../../shared/infinite-select/infinite-select.utils';

export type TimetableViewMode = 'week' | 'dateWise' | 'day';
export type MyTimetableAudience = 'STUDENT' | 'STAFF';

/** "HH:mm:ss" -> hours between two times, rounded to 2 decimals -- the "Log Progress" dialog's
 *  default hours-covered suggestion for a fresh log (never enforced, just a starting point). */
function periodHoursBetween(startTime: string, endTime: string): number {
  const [sh, sm] = startTime.split(':').map(Number);
  const [eh, em] = endTime.split(':').map(Number);
  const minutes = (eh * 60 + em) - (sh * 60 + sm);
  return Math.round((minutes / 60) * 100) / 100;
}

// Local-time-only date math -- .toISOString() converts to UTC, which silently shifts the date
// backward a day for any positive UTC offset (e.g. IST) when starting from local midnight.
function mondayOf(date: Date): string {
  const d = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  const day = d.getDay(); // 0=Sunday..6=Saturday
  const diff = day === 0 ? -6 : 1 - day;
  d.setDate(d.getDate() + diff);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${dd}`;
}

/**
 * Shared by two routes -- /my-timetable/student (MY_TIMETABLE_VIEW_STUDENT) and
 * /my-timetable/staff (MY_TIMETABLE_VIEW_STAFF) -- distinguished only by the `audience` route
 * data each one sets, per the specialist-agreed split: one screen with role-based restrictions
 * (Log Progress is staff-only), not two duplicated components. Angular re-creates this component
 * on navigation between the two routes (different paths), so ngOnInit re-reads `audience` fresh
 * each time.
 */
@Component({
  selector: 'app-my-timetable',
  standalone: true,
  imports: [FormsModule, MatDialogModule, MatProgressSpinnerModule, CmsWeekGridComponent, CmsWeekNavigatorComponent, CmsDayAgendaComponent, CmsTourButtonComponent, CmsInfiniteSelectComponent],
  templateUrl: './my-timetable.component.html',
  styleUrl: './my-timetable.component.scss',
})
export class MyTimetableComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly academicYearService = inject(AcademicYearService);
  private readonly timetableService = inject(TimetableService);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly permissionService = inject(PermissionService);
  private readonly tourService = inject(TourService);

  protected audience: MyTimetableAudience = 'STUDENT';

  /** Log Progress is a staff action -- gated on audience as well as the permission itself, so a
   *  student can never see it even in the unlikely case their role also holds PROGRESS_LOG_CREATE. */
  protected readonly canLogProgress = computed(() =>
    this.audience === 'STAFF' && this.permissionService.has('PROGRESS_LOG_CREATE'));

  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly termInstances = signal<TermInstance[]>([]);
  protected readonly sessions = signal<ClassSchedule[]>([]);
  protected readonly holidays = signal<WeekGridHolidayInfo[]>([]);
  protected readonly loading = signal(false);
  protected readonly termsLoading = signal(false);

  protected selectedAcademicYearId: number | null = null;
  protected selectedTermInstanceId: number | null = null;
  protected weekStart: string = mondayOf(new Date());

  protected readonly viewMode = signal<TimetableViewMode>('week');
  protected readonly dayDate = signal(new Date().toISOString().slice(0, 10));
  protected readonly occurrences = signal<ClassScheduleOccurrence[]>([]);
  protected readonly occurrencesLoading = signal(false);

  /** The "Week of" picker (and the Date-wise-weekly navigator, which shares the same weekStart)
   *  must stay inside the selected term's own date range -- ClassSchedule has no calendar date
   *  (it's a weekly recurring template, see PersonalTimetableService), so nothing stops the
   *  backend from happily returning that template for a week outside the term; only the picker's
   *  own bounds prevent showing a week that never actually occurred. */
  protected readonly selectedTerm = computed(() =>
    this.termInstances().find((t) => t.id === this.selectedTermInstanceId) ?? null);
  protected readonly weekMin = computed(() => this.selectedTerm()?.startDate ?? null);
  protected readonly weekMax = computed(() => this.selectedTerm()?.endDate ?? null);

  /** The Date-wise-weekly view reuses the same day-columns x period-rows grid the Generic Week
   *  view renders (CmsWeekGridComponent), just fed real dated occurrences instead of the recurring
   *  template -- carrying each date's actual HELD/SUBSTITUTED/CANCELLED outcome and real
   *  room/faculty, unlike Week's generic pattern. */
  protected readonly dateWiseSessions = computed<WeekGridSession[]>(() =>
    this.occurrences().map((o) => ({
      ...o.session,
      occurrenceStatus: o.occurrenceStatus,
      cancelReason: o.cancelReason,
    })));

  ngOnInit(): void {
    this.audience = (this.route.snapshot.data['audience'] as MyTimetableAudience | undefined) ?? 'STUDENT';

    this.tourService.register('my-timetable', MY_TIMETABLE_TOUR);
    this.tourService.registerFlowMap('my-timetable', MY_TIMETABLE_FLOW_MAP);

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

  protected readonly academicYearFetchPage = staticOptionsFetchPage(() =>
    this.academicYears().map(ay => ({ id: ay.id, name: ay.name })));
  protected readonly termFetchPage = staticOptionsFetchPage(() =>
    this.termInstances().map(t => ({ id: t.id, name: `${t.termType} · ${t.status}` })));

  protected onAcademicYearChange(value: InfiniteSelectValue | null): void {
    this.selectedAcademicYearId = value != null ? Number(value) : null;
    this.selectedTermInstanceId = null;
    this.sessions.set([]);
    if (this.selectedAcademicYearId) this.loadTermInstances(this.selectedAcademicYearId);
  }

  protected onTermChange(value: InfiniteSelectValue | null): void {
    this.selectedTermInstanceId = value != null ? Number(value) : null;
    const term = this.selectedTerm();
    if (term) this.weekStart = this.defaultWeekStartFor(term);
    this.load();
    this.refreshCurrentViewMode();
  }

  protected onWeekStartChange(): void {
    this.weekStart = this.clampToTerm(this.weekStart, this.selectedTerm());
    this.load();
  }

  protected onDateWiseWeekChange(iso: string): void {
    this.weekStart = this.clampToTerm(iso, this.selectedTerm());
    this.loadDateWiseOccurrences(this.weekStart);
  }

  private refreshCurrentViewMode(): void {
    const mode = this.viewMode();
    if (mode === 'dateWise') this.loadDateWiseOccurrences(this.weekStart);
    else if (mode === 'day') this.loadDayOccurrences(this.dayDate());
  }

  /** Today's Monday if it falls inside the term, otherwise the nearest term boundary --
   *  never defaults to a week the term hasn't reached yet or has already finished. */
  private defaultWeekStartFor(term: TermInstance): string {
    return this.clampToTerm(mondayOf(new Date()), term);
  }

  private clampToTerm(date: string, term: TermInstance | null): string {
    if (!term) return date;
    if (date < term.startDate) return term.startDate;
    if (date > term.endDate) return term.endDate;
    return date;
  }

  protected setViewMode(mode: TimetableViewMode): void {
    this.viewMode.set(mode);
    if (mode === 'dateWise') this.loadDateWiseOccurrences(this.weekStart);
    else if (mode === 'day') this.loadDayOccurrences(this.dayDate());
  }

  protected onDayDateChange(iso: string): void {
    this.dayDate.set(iso);
    this.loadDayOccurrences(iso);
  }

  /** Lazy-loads exactly one Mon-Sat week's real occurrences at a time as the user pages through
   *  cms-week-navigator, rather than fetching the whole term upfront -- a term can run 15-20+
   *  weeks, so this keeps each request small regardless of term length. */
  private loadDateWiseOccurrences(weekStartIso: string): void {
    if (!this.selectedTermInstanceId) return;
    const to = new Date(`${weekStartIso}T00:00:00`);
    to.setDate(to.getDate() + 5);
    const toIso = `${to.getFullYear()}-${String(to.getMonth() + 1).padStart(2, '0')}-${String(to.getDate()).padStart(2, '0')}`;
    this.occurrencesLoading.set(true);
    this.timetableService.getOccurrences(this.selectedTermInstanceId, weekStartIso, toIso, 'personal').subscribe({
      next: (occs) => { this.occurrences.set(occs); this.occurrencesLoading.set(false); },
      error: () => { this.toast.error('Failed to load date-wise view'); this.occurrencesLoading.set(false); },
    });
  }

  private loadDayOccurrences(iso: string): void {
    if (!this.selectedTermInstanceId) return;
    this.occurrencesLoading.set(true);
    this.timetableService.getOccurrences(this.selectedTermInstanceId, iso, iso, 'personal').subscribe({
      next: (occs) => { this.occurrences.set(occs); this.occurrencesLoading.set(false); },
      error: () => { this.toast.error('Failed to load day view'); this.occurrencesLoading.set(false); },
    });
  }

  protected openLogProgress(session: WeekGridSession): void {
    if (!this.canLogProgress()) return;
    const term = this.termInstances().find((t) => t.id === this.selectedTermInstanceId);
    if (!term) return;
    this.dialog.open(LogProgressDialogComponent, {
      width: '640px',
      data: {
        classScheduleId: session.id,
        subjectName: session.subjectName,
        subjectCode: session.subjectCode,
        termStartDate: term.startDate,
        periodHours: periodHoursBetween(session.startTime, session.endTime),
      },
    });
  }

  private loadTermInstances(academicYearId: number): void {
    this.termsLoading.set(true);
    this.academicYearService.getTermInstancesByAcademicYear(academicYearId).subscribe({
      next: (terms) => {
        this.termInstances.set(terms);
        this.termsLoading.set(false);
        this.selectedTermInstanceId = terms[0]?.id ?? null;
        if (terms[0]) this.weekStart = this.defaultWeekStartFor(terms[0]);
        this.load();
      },
      error: () => { this.toast.error('Failed to load term instances'); this.termsLoading.set(false); },
    });
  }

  private load(): void {
    if (!this.selectedTermInstanceId) { this.sessions.set([]); return; }
    this.loading.set(true);
    this.timetableService.getMyTimetable(this.selectedTermInstanceId, this.weekStart || undefined).subscribe({
      next: (response) => {
        this.sessions.set(response.sessions);
        this.holidays.set(response.holidays);
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load your timetable'); this.loading.set(false); },
    });
  }
}

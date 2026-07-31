import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear, TermInstance } from '../../academic-year/academic-year.model';
import { TimetableService } from '../timetable.service';
import { ClassSchedule } from '../timetable.model';
import { CmsWeekGridComponent } from '../../../shared/week-grid/week-grid.component';
import { WeekGridHolidayInfo, WeekGridSession } from '../../../shared/week-grid/week-grid.model';
import { CmsMonthGridComponent } from '../../../shared/month-grid/month-grid.component';
import { CmsDayAgendaComponent } from '../../../shared/day-agenda/day-agenda.component';
import { ClassScheduleOccurrence } from '../timetable.model';
import { ToastService } from '../../../core/toast/toast.service';
import { PermissionService } from '../../../core/permissions/permission.service';
import { LogProgressDialogComponent } from '../log-progress-dialog/log-progress-dialog.component';

export type TimetableViewMode = 'week' | 'month' | 'day';

/** "HH:mm:ss" -> hours between two times, rounded to 2 decimals -- the "Log Progress" dialog's
 *  default hours-covered suggestion for a fresh log (never enforced, just a starting point). */
function periodHoursBetween(startTime: string, endTime: string): number {
  const [sh, sm] = startTime.split(':').map(Number);
  const [eh, em] = endTime.split(':').map(Number);
  const minutes = (eh * 60 + em) - (sh * 60 + sm);
  return Math.round((minutes / 60) * 100) / 100;
}

function mondayOf(date: Date): string {
  const d = new Date(date);
  const day = d.getDay(); // 0=Sunday..6=Saturday
  const diff = day === 0 ? -6 : 1 - day;
  d.setDate(d.getDate() + diff);
  return d.toISOString().slice(0, 10);
}

@Component({
  selector: 'app-my-timetable',
  standalone: true,
  imports: [FormsModule, MatDialogModule, MatProgressSpinnerModule, CmsWeekGridComponent, CmsMonthGridComponent, CmsDayAgendaComponent],
  templateUrl: './my-timetable.component.html',
  styleUrl: './my-timetable.component.scss',
})
export class MyTimetableComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly timetableService = inject(TimetableService);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly permissionService = inject(PermissionService);

  protected readonly canLogProgress = computed(() => this.permissionService.has('PROGRESS_LOG_CREATE'));

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
  protected readonly monthYear = signal(new Date().getFullYear());
  protected readonly monthMonth = signal(new Date().getMonth());
  protected readonly dayDate = signal(new Date().toISOString().slice(0, 10));
  protected readonly occurrences = signal<ClassScheduleOccurrence[]>([]);
  protected readonly occurrencesLoading = signal(false);

  /** The "Week of" picker must stay inside the selected term's own date range -- ClassSchedule
   *  has no calendar date (it's a weekly recurring template, see PersonalTimetableService), so
   *  nothing stops the backend from happily returning that template for a week outside the term;
   *  only the picker's own bounds prevent showing a week that never actually occurred. */
  protected readonly selectedTerm = computed(() =>
    this.termInstances().find((t) => t.id === this.selectedTermInstanceId) ?? null);
  protected readonly weekMin = computed(() => this.selectedTerm()?.startDate ?? null);
  protected readonly weekMax = computed(() => this.selectedTerm()?.endDate ?? null);

  ngOnInit(): void {
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
    this.sessions.set([]);
    if (this.selectedAcademicYearId) this.loadTermInstances(this.selectedAcademicYearId);
  }

  protected onTermChange(): void {
    const term = this.selectedTerm();
    if (term) this.weekStart = this.defaultWeekStartFor(term);
    this.load();
    this.refreshCurrentViewMode();
  }

  protected onWeekStartChange(): void {
    this.weekStart = this.clampToTerm(this.weekStart, this.selectedTerm());
    this.load();
  }

  private refreshCurrentViewMode(): void {
    const mode = this.viewMode();
    if (mode === 'month') this.loadMonthOccurrences(this.monthYear(), this.monthMonth());
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
    this.dayDate.set(iso);
    this.loadDayOccurrences(iso);
  }

  private loadMonthOccurrences(year: number, month: number): void {
    if (!this.selectedTermInstanceId) return;
    const from = new Date(year, month, 1).toISOString().slice(0, 10);
    const to = new Date(year, month + 1, 0).toISOString().slice(0, 10);
    this.occurrencesLoading.set(true);
    this.timetableService.getOccurrences(this.selectedTermInstanceId, from, to, 'personal').subscribe({
      next: (occs) => { this.occurrences.set(occs); this.occurrencesLoading.set(false); },
      error: () => { this.toast.error('Failed to load month view'); this.occurrencesLoading.set(false); },
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

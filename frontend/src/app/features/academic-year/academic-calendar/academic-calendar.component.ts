import {
  Component,
  computed,
  ElementRef,
  inject,
  OnInit,
  signal,
  ViewChild,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { forkJoin } from 'rxjs';
import { AcademicYearService } from '../academic-year.service';
import { BlockedPeriodService } from '../blocked-period.service';
import { DayMappingService } from '../day-mapping.service';
import { PeriodService } from '../../period/period.service';
import { Period } from '../../period/period.model';
import {
  AcademicYear,
  BlockedPeriod,
  CalendarEvent,
  CalendarEventType,
  DayMapping,
  HolidayCategory,
  TermInstance,
  TermType,
} from '../academic-year.model';
import { ToastService } from '../../../core/toast/toast.service';
import { PermissionService } from '../../../core/permissions/permission.service';
import { PrintService } from '../../../core/print/print.service';
import { CsvExporterService } from '../../../core/export/csv-exporter.service';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { TourService } from '../../../shared/tour/tour.service';
import { ACADEMIC_CALENDAR_TOUR } from '../../../shared/tour/tours/academic-calendar.tours';
import { formatBlockSummary } from './blocked-period-summary.util';
import {
  DAY_OF_WEEK_LABELS,
  EVENT_TYPE_BADGE_CLASS,
  EVENT_TYPE_ICONS,
  EVENT_TYPE_LABELS,
} from './calendar-display.constants';
import { DayDetailFlyoutComponent, DayDetailSection } from './day-detail-flyout/day-detail-flyout.component';
import { buildMonthGrids, MonthGrid, toIso } from './month-grid.util';

export type CalendarViewMode = 'timeline' | 'grid' | 'blocked-periods' | 'day-mappings';

@Component({
  selector: 'app-academic-calendar',
  standalone: true,
  imports: [
    AppDatePipe,
    RouterLink,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatMenuModule,
    MatTooltipModule,
    CmsEmptyStateComponent,
    CmsTourButtonComponent,
    CmsStatusBadgeComponent,
    DayDetailFlyoutComponent,
  ],
  templateUrl: './academic-calendar.component.html',
  styleUrl: './academic-calendar.component.scss',
})
export class AcademicCalendarComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly blockedPeriodService = inject(BlockedPeriodService);
  private readonly dayMappingService = inject(DayMappingService);
  private readonly periodService = inject(PeriodService);
  private readonly toast = inject(ToastService);
  private readonly printService = inject(PrintService);
  private readonly csvExporter = inject(CsvExporterService);
  protected readonly permissionService = inject(PermissionService);
  private readonly tourService = inject(TourService);

  @ViewChild('calendarPrintArea') calendarPrintArea!: ElementRef<HTMLElement>;

  /** Milliseconds in one day — used for date-diff calculations. */
  private static readonly MS_PER_DAY = 86_400_000;

  // ─── Loading / error state ───
  protected readonly loading = signal(false);
  protected readonly hasError = signal(false);

  // ─── Data signals ───
  protected readonly allAcademicYears = signal<AcademicYear[]>([]);
  protected readonly selectedAcademicYear = signal<AcademicYear | null>(null);
  protected readonly termInstances = signal<TermInstance[]>([]);
  protected readonly events = signal<CalendarEvent[]>([]);
  protected readonly blockedPeriods = signal<BlockedPeriod[]>([]);
  protected readonly dayMappings = signal<DayMapping[]>([]);
  protected readonly periods = signal<Period[]>([]);

  // ─── View mode ───
  protected readonly viewMode = signal<CalendarViewMode>('timeline');

  // ─── Day-detail flyout state ───
  protected readonly dayDetailTarget = signal<Date | null>(null);
  protected readonly dayDetailFocusEventId = signal<number | null>(null);
  protected readonly dayDetailFocusBlockId = signal<number | null>(null);
  protected readonly dayDetailFocusMappingId = signal<number | null>(null);
  protected readonly dayDetailSection = signal<DayDetailSection | null>(null);

  // ─── Event types for template ───
  protected readonly eventTypes: CalendarEventType[] = [
    'HOLIDAY', 'EXAM', 'CULTURAL', 'SPORTS', 'WORKSHOP', 'OTHER',
  ];
  protected readonly eventTypeLabels = EVENT_TYPE_LABELS;
  protected readonly eventTypeIcons = EVENT_TYPE_ICONS;

  // ─── Holiday categories (only meaningful when eventType === 'HOLIDAY') ───
  protected readonly holidayCategories: HolidayCategory[] = ['GOVERNMENT', 'LOCAL', 'INSTITUTIONAL'];
  protected readonly holidayCategoryLabels: Record<HolidayCategory, string> = {
    GOVERNMENT: 'Government',
    LOCAL: 'Local',
    INSTITUTIONAL: 'Institutional',
  };

  // ─── Role helpers ───
  protected readonly canManage = computed(() => this.permissionService.has('ACADEMIC_YEAR_MANAGE'));
  protected readonly canManageBlocks = computed(() => this.permissionService.has('BLOCKED_PERIOD_MANAGE'));
  protected readonly canManageDayMapping = computed(() => this.permissionService.has('TIMETABLE_DAY_MAPPING_MANAGE'));

  protected readonly dayOfWeekLabels = DAY_OF_WEEK_LABELS;

  // ─── Stats ───
  protected readonly stats = computed(() => {
    const ay = this.selectedAcademicYear();
    const terms = this.termInstances();
    const evts = this.events();

    if (!ay) return null;

    const start = new Date(ay.startDate);
    const end = new Date(ay.endDate);
    const totalDays = Math.round((end.getTime() - start.getTime()) / AcademicCalendarComponent.MS_PER_DAY) + 1;
    const totalWeeks = Math.round(totalDays / 7);

    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const currentTerm = terms.find((term) => {
      const start = new Date(term.startDate);
      const end = new Date(term.endDate);
      return today >= start && today <= end;
    });

    // No term ongoing right now -- rather than a bare, unexplained dash, say something concrete:
    // how long until the next term starts, or that there's nothing scheduled at all.
    let daysValue: number | string;
    let daysLabel: string;
    if (currentTerm) {
      daysValue = Math.max(
        0,
        Math.round((new Date(currentTerm.endDate).getTime() - today.getTime()) / AcademicCalendarComponent.MS_PER_DAY),
      );
      daysLabel = 'Days Left in Term';
    } else {
      const nextTerm = terms
        .filter((t) => new Date(t.startDate) > today)
        .sort((a, b) => new Date(a.startDate).getTime() - new Date(b.startDate).getTime())[0];
      if (nextTerm) {
        daysValue = Math.round((new Date(nextTerm.startDate).getTime() - today.getTime()) / AcademicCalendarComponent.MS_PER_DAY);
        daysLabel = 'Days Until Next Term';
      } else {
        daysValue = '—';
        daysLabel = 'No Active Term';
      }
    }

    return {
      totalWeeks,
      termCount: terms.length,
      daysValue,
      daysLabel,
      eventCount: evts.length,
    };
  });

  // ─── Upcoming events (next 30 days) ───
  protected readonly upcomingEvents = computed(() => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const in30 = new Date(today);
    in30.setDate(in30.getDate() + 30);

    return this.events()
      .filter((e) => {
        const d = new Date(e.startDate);
        return d >= today && d <= in30;
      })
      .slice(0, 5);
  });

  // ─── Grid months ───
  protected readonly monthGrids = computed<MonthGrid[]>(() => {
    const ay = this.selectedAcademicYear();
    if (!ay) return [];
    return buildMonthGrids(
      ay, this.termInstances(), this.events(), this.dayMappings(),
      (term) => this.getTermStatus(term), (term) => this.getTermLabel(term),
    );
  });

  protected blockSummary(block: BlockedPeriod): string {
    return formatBlockSummary(block, this.dayOfWeekLabels);
  }

  protected mappingSummary(mapping: DayMapping): string {
    return `${this.formatDisplayDate(mapping.mappedDate)} runs ${this.dayOfWeekLabels[mapping.borrowedDayOfWeek]}'s schedule`;
  }

  ngOnInit(): void {
    this.tourService.register('academic-calendar', ACADEMIC_CALENDAR_TOUR);
    this.loadAll();
    this.periodService.getAll(true).subscribe({
      next: (data) => this.periods.set(data),
      error: () => this.toast.error('Failed to load periods'),
    });
    this.reloadBlockedPeriods();
    this.reloadDayMappings();
  }

  // ─── Load helpers ───
  private loadAll(): void {
    this.loading.set(true);
    this.hasError.set(false);

    this.academicYearService.getAllAcademicYears().subscribe({
      next: (years) => {
        const sorted = [...years].sort(
          (a, b) => new Date(b.startDate).getTime() - new Date(a.startDate).getTime(),
        );
        this.allAcademicYears.set(sorted);
        const current = sorted.find((y) => y.isCurrent) ?? sorted[0] ?? null;
        if (current) {
          this.selectedAcademicYear.set(current);
          this.loadYearData(current.id);
        } else {
          this.hasError.set(true);
          this.loading.set(false);
        }
      },
      error: () => {
        this.hasError.set(true);
        this.loading.set(false);
      },
    });
  }

  protected selectYear(yearId: number): void {
    const year = this.allAcademicYears().find((y) => y.id === yearId);
    if (!year) return;
    this.selectedAcademicYear.set(year);
    this.loading.set(true);
    this.loadYearData(yearId);
  }

  /** Typed event handler for the year `<select>` element — avoids `$any()` in the template. */
  protected selectYearFromEvent(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.selectYear(Number(select.value));
  }

  private loadYearData(yearId: number): void {
    forkJoin({
      termInstances: this.academicYearService.getTermInstancesByAcademicYear(yearId),
      events: this.academicYearService.getCalendarEventsByAcademicYear(yearId),
    }).subscribe({
      next: ({ termInstances, events }) => {
        this.termInstances.set(termInstances);
        this.events.set(events);
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load calendar data');
        this.loading.set(false);
      },
    });
  }

  // ─── Term helpers ───
  protected getTermProgress(term: TermInstance): number {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const start = new Date(term.startDate);
    const end = new Date(term.endDate);
    if (today < start) return 0;
    if (today > end) return 100;
    const total = end.getTime() - start.getTime();
    if (total <= 0) return 100;
    const elapsed = today.getTime() - start.getTime();
    return Math.round((elapsed / total) * 100);
  }

  protected getTermDays(term: TermInstance): { elapsed: number; total: number } {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const start = new Date(term.startDate);
    const end = new Date(term.endDate);
    const total = Math.round((end.getTime() - start.getTime()) / AcademicCalendarComponent.MS_PER_DAY) + 1;
    const elapsed = Math.min(
      total,
      Math.max(0, Math.round((today.getTime() - start.getTime()) / AcademicCalendarComponent.MS_PER_DAY) + 1),
    );
    return { elapsed: today < start ? 0 : elapsed, total };
  }

  protected getTermLabel(term: TermInstance): string {
    return `${this.termTypeLabel(term.termType)} Term`;
  }

  /** Purely calendar-derived (today vs. this term's real dates) — used for the timeline
   *  marker/highlighting/progress label and month-grid day coloring, which are all visualizations
   *  of the calendar itself. Deliberately does NOT consult `term.status` (the admin-set workflow
   *  state: PLANNED/OPEN/LOCKED) — that field is now shown honestly on its own via the status
   *  badge (`term.status` passed directly) plus `adminStatusMismatch`, instead of being silently
   *  blended into this date-based read the way it used to be. */
  protected getTermStatus(term: TermInstance): 'UPCOMING' | 'ONGOING' | 'COMPLETED' {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const start = new Date(term.startDate);
    const end = new Date(term.endDate);
    if (today < start) return 'UPCOMING';
    if (today > end) return 'COMPLETED';
    return 'ONGOING';
  }

  /** Non-null (a plain-language explanation) when the admin-set term.status visibly disagrees
   *  with what today's date vs. the term's real dates would suggest -- e.g. a term marked LOCKED
   *  that hasn't even started yet. Surfaced as a warning next to the status badge rather than
   *  left as a silent, confusing contradiction between the badge and the progress bar beside it. */
  protected adminStatusMismatch(term: TermInstance): string | null {
    const calendarStatus = this.getTermStatus(term);
    if (term.status === 'LOCKED' && calendarStatus === 'UPCOMING') {
      return `Marked Locked, but doesn't start until ${this.formatDisplayDate(term.startDate)}`;
    }
    if (term.status === 'PLANNED' && calendarStatus === 'COMPLETED') {
      return `Marked Planned, but already ended on ${this.formatDisplayDate(term.endDate)}`;
    }
    if (term.status === 'PLANNED' && calendarStatus === 'ONGOING') {
      return `Marked Planned, but is currently within its dates`;
    }
    return null;
  }

  /** ISO 'YYYY-MM-DD' -> 'DD-MM-YYYY', matching this page's own date display convention
   *  (`appDate` pipe) -- used for plain-string tooltip text where a pipe can't be applied. */
  private formatDisplayDate(iso: string): string {
    const [y, m, d] = iso.split('-');
    return `${d}-${m}-${y}`;
  }

  protected termTypeLabel(type: TermType): string {
    return type === 'ODD' ? 'Odd' : 'Even';
  }

  // ─── Events panel helpers ───
  protected getEventsForTerm(term: TermInstance): CalendarEvent[] {
    return this.events().filter((event) => event.startDate <= term.endDate && event.endDate >= term.startDate);
  }

  /** Events not already shown under any term card above — the inverse of getEventsForTerm's
   *  overlap check. Without this, an event overlapping any term (the common case) would appear
   *  twice on the page: once under its term, once again here. */
  protected getYearLevelEvents(): CalendarEvent[] {
    const terms = this.termInstances();
    return this.events().filter((event) =>
      !terms.some((term) => event.startDate <= term.endDate && event.endDate >= term.startDate));
  }

  protected eventDotTitle(evt: CalendarEvent): string {
    return evt.holidayCategory ? `${evt.title} (${this.holidayCategoryLabels[evt.holidayCategory]})` : evt.title;
  }

  protected eventTypeBadgeClass(type: CalendarEventType): string {
    return EVENT_TYPE_BADGE_CLASS[type] ?? 'cms-badge--gray';
  }

  // ─── Day-detail flyout wiring ───
  /** Month Grid day-cell click. */
  protected openDayDetail(date: Date): void {
    this.dayDetailTarget.set(date);
    this.dayDetailFocusEventId.set(null);
    this.dayDetailFocusBlockId.set(null);
    this.dayDetailFocusMappingId.set(null);
    this.dayDetailSection.set(null);
  }

  protected openAddEvent(): void {
    if (!this.selectedAcademicYear()) return;
    this.dayDetailTarget.set(new Date());
    this.dayDetailFocusEventId.set(null);
    this.dayDetailFocusBlockId.set(null);
    this.dayDetailFocusMappingId.set(null);
    this.dayDetailSection.set('EVENTS');
  }

  protected openEditEvent(event: CalendarEvent): void {
    this.dayDetailTarget.set(new Date(`${event.startDate}T00:00:00`));
    this.dayDetailFocusEventId.set(event.id);
    this.dayDetailFocusBlockId.set(null);
    this.dayDetailFocusMappingId.set(null);
    this.dayDetailSection.set('EVENTS');
  }

  protected openAddBlock(): void {
    this.dayDetailTarget.set(new Date());
    this.dayDetailFocusEventId.set(null);
    this.dayDetailFocusBlockId.set(null);
    this.dayDetailFocusMappingId.set(null);
    this.dayDetailSection.set('BLOCKS');
  }

  protected openEditBlock(block: BlockedPeriod): void {
    const anchor = block.blockType === 'ONE_OFF' ? block.specificDate! : block.rangeStartDate!;
    this.dayDetailTarget.set(new Date(`${anchor}T00:00:00`));
    this.dayDetailFocusEventId.set(null);
    this.dayDetailFocusBlockId.set(block.id);
    this.dayDetailFocusMappingId.set(null);
    this.dayDetailSection.set('BLOCKS');
  }

  protected openAddDayMapping(): void {
    this.dayDetailTarget.set(new Date());
    this.dayDetailFocusEventId.set(null);
    this.dayDetailFocusBlockId.set(null);
    this.dayDetailFocusMappingId.set(null);
    this.dayDetailSection.set('DAY_MAPPING');
  }

  protected openEditDayMapping(mapping: DayMapping): void {
    this.dayDetailTarget.set(new Date(`${mapping.mappedDate}T00:00:00`));
    this.dayDetailFocusEventId.set(null);
    this.dayDetailFocusBlockId.set(null);
    this.dayDetailFocusMappingId.set(mapping.id);
    this.dayDetailSection.set(null);
  }

  protected closeDayDetail(): void {
    this.dayDetailTarget.set(null);
  }

  protected onDayDetailDataChanged(): void {
    this.reloadEvents();
    this.reloadBlockedPeriods();
    this.reloadDayMappings();
  }

  // ─── Event CRUD (delete only -- add/edit lives in the day-detail flyout) ───
  protected deleteEvent(event: CalendarEvent): void {
    if (!confirm(`Delete "${event.title}"?`)) return;
    this.academicYearService.deleteCalendarEvent(event.id).subscribe({
      next: () => {
        this.toast.success('Event deleted');
        this.reloadEvents();
      },
      error: (err) => this.toast.error(err?.error?.message ?? 'Failed to delete event'),
    });
  }

  /** "Delete this and all future occurrences" -- see the day-detail flyout's twin method for the
   *  full rationale. Past occurrences are never touched (enforced server-side). */
  protected deleteEventSeries(event: CalendarEvent): void {
    if (!confirm(
      `Delete "${event.title}" and every future occurrence of its holiday template? ` +
      `Past occurrences will not be affected.`,
    )) return;
    this.academicYearService.deleteCalendarEventSeries(event.id).subscribe({
      next: () => {
        this.toast.success('Event series deleted');
        this.reloadEvents();
      },
      error: (err) => this.toast.error(err?.error?.message ?? 'Failed to delete event series'),
    });
  }

  private reloadEvents(): void {
    const ay = this.selectedAcademicYear();
    if (!ay) return;
    this.academicYearService.getCalendarEventsByAcademicYear(ay.id).subscribe({
      next: (evts) => this.events.set(evts),
    });
  }

  // ─── Blocked period CRUD (delete only -- add/edit lives in the day-detail flyout) ───
  protected deleteBlock(block: BlockedPeriod): void {
    if (!confirm(`Delete this block ("${block.reason}")?`)) return;
    this.blockedPeriodService.delete(block.id).subscribe({
      next: () => {
        this.toast.success('Blocked period deleted');
        this.reloadBlockedPeriods();
      },
      error: (err) => this.toast.error(err?.error?.message ?? 'Failed to delete blocked period'),
    });
  }

  private reloadBlockedPeriods(): void {
    this.blockedPeriodService.getAll().subscribe({
      next: (data) => this.blockedPeriods.set(data),
      error: () => this.toast.error('Failed to load blocked periods'),
    });
  }

  // ─── Day mapping CRUD (delete only -- add/edit lives in the day-detail flyout) ───
  protected deleteMapping(mapping: DayMapping): void {
    if (!confirm(`Delete this day mapping ("${mapping.reason}")?`)) return;
    this.dayMappingService.delete(mapping.id).subscribe({
      next: () => {
        this.toast.success('Day mapping deleted');
        this.reloadDayMappings();
      },
      error: (err) => this.toast.error(err?.error?.message ?? 'Failed to delete day mapping'),
    });
  }

  private reloadDayMappings(): void {
    this.dayMappingService.getAll().subscribe({
      next: (data) => this.dayMappings.set(data),
      error: () => this.toast.error('Failed to load day mappings'),
    });
  }

  // ─── Print / Export ───
  protected printCalendar(): void {
    if (this.calendarPrintArea) {
      this.printService.printElement(this.calendarPrintArea);
    } else {
      this.printService.printRoute();
    }
  }

  protected exportEvents(): void {
    const ay = this.selectedAcademicYear();
    const evts = this.events();
    this.csvExporter.exportRows(
      `academic-calendar-${ay?.name ?? 'events'}`,
      [
        { key: 'title', header: 'Title' },
        { key: 'eventType', header: 'Type', format: (v) => EVENT_TYPE_LABELS[v as CalendarEventType] },
        { key: 'startDate', header: 'Start Date' },
        { key: 'endDate', header: 'End Date' },
        { key: 'description', header: 'Description', format: (v) => String(v ?? '') },
        {
          key: 'academicYear',
          header: 'Academic Year',
          format: (_v, row) => (row as CalendarEvent).academicYear?.name ?? ay?.name ?? '',
        },
      ],
      evts,
    );
  }

  // ─── Grid builder ───
  protected trackByMonth(index: number, grid: MonthGrid): string {
    return `${grid.year}-${grid.month}`;
  }

  protected trackById(index: number, item: { id: number }): number {
    return item.id;
  }
}

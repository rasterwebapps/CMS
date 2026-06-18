import {
  Component,
  computed,
  ElementRef,
  inject,
  OnInit,
  signal,
  ViewChild,
} from '@angular/core';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { forkJoin } from 'rxjs';
import { AcademicYearService } from '../academic-year.service';
import {
  AcademicYear,
  CalendarEvent,
  CalendarEventRequest,
  CalendarEventType,
  TermInstance,
  TermInstanceStatus,
  TermType,
} from '../academic-year.model';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { ToastService } from '../../../core/toast/toast.service';
import { PermissionService } from '../../../core/permissions/permission.service';
import { PrintService } from '../../../core/print/print.service';
import { CsvExporterService } from '../../../core/export/csv-exporter.service';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { ACADEMIC_CALENDAR_TOUR } from '../../../shared/tour/tours/academic-calendar.tours';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';

export type CalendarViewMode = 'timeline' | 'grid';

interface MonthGrid {
  year: number;
  month: number; // 0-based
  label: string;
  days: DayCell[];
}

interface DayCell {
  date: Date;
  dayNum: number;
  isCurrentMonth: boolean;
  termStatus: 'UPCOMING' | 'ONGOING' | 'COMPLETED' | null;
  termName: string | null;
  events: CalendarEvent[];
  isToday: boolean;
}

const EVENT_TYPE_LABELS: Record<CalendarEventType, string> = {
  HOLIDAY: 'Holiday',
  EXAM: 'Exam',
  CULTURAL: 'Cultural',
  SPORTS: 'Sports',
  WORKSHOP: 'Workshop',
  OTHER: 'Other',
};

const EVENT_TYPE_ICONS: Record<CalendarEventType, string> = {
  HOLIDAY: 'beach_access',
  EXAM: 'quiz',
  CULTURAL: 'theater_comedy',
  SPORTS: 'sports_soccer',
  WORKSHOP: 'handyman',
  OTHER: 'event',
};

@Component({
  selector: 'app-academic-calendar',
  standalone: true,
  imports: [
    AppDatePipe,
    FormsModule,
    ReactiveFormsModule,
    RouterLink,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatFormFieldModule,
    MatInputModule,
    MatMenuModule,
    MatTooltipModule,
    PageHeaderComponent,
    CmsEmptyStateComponent,
    CmsTourButtonComponent,
  ],
  templateUrl: './academic-calendar.component.html',
  styleUrl: './academic-calendar.component.scss',
})
export class AcademicCalendarComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);
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

  // ─── View mode ───
  protected readonly viewMode = signal<CalendarViewMode>('timeline');

  // ─── Event dialog state ───
  protected readonly showEventDialog = signal(false);
  protected readonly editingEvent = signal<CalendarEvent | null>(null);
  protected readonly eventSaving = signal(false);

  // ─── Event types for template ───
  protected readonly eventTypes: CalendarEventType[] = [
    'HOLIDAY', 'EXAM', 'CULTURAL', 'SPORTS', 'WORKSHOP', 'OTHER',
  ];
  protected readonly eventTypeLabels = EVENT_TYPE_LABELS;
  protected readonly eventTypeIcons = EVENT_TYPE_ICONS;

  // ─── Role helpers ───
  protected readonly canManage = computed(() => this.permissionService.has('ACADEMIC_YEAR_MANAGE'));

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

    const daysRemaining = currentTerm
      ? Math.max(
          0,
          Math.round((new Date(currentTerm.endDate).getTime() - today.getTime()) / AcademicCalendarComponent.MS_PER_DAY),
        )
      : null;

    return {
      totalWeeks,
      termCount: terms.length,
      daysRemaining,
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
    return this.buildMonthGrids(ay, this.termInstances(), this.events());
  });

  // ─── Event form ───
  protected readonly eventForm = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    description: [''],
    startDate: ['', Validators.required],
    endDate: ['', Validators.required],
    eventType: ['HOLIDAY' as CalendarEventType, Validators.required],
  });

  private readonly MONTH_NAMES = Array.from({ length: 12 }, (_, i) =>
    new Intl.DateTimeFormat('en', { month: 'long' }).format(new Date(2000, i, 1)),
  );

  ngOnInit(): void {
    this.tourService.register('academic-calendar', ACADEMIC_CALENDAR_TOUR);
    this.loadAll();
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

  protected getTermStatus(term: TermInstance): 'UPCOMING' | 'ONGOING' | 'COMPLETED' {
    const byStatus = this.mapTermStatus(term.status);
    if (byStatus) return byStatus;

    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const start = new Date(term.startDate);
    const end = new Date(term.endDate);
    if (today < start) return 'UPCOMING';
    if (today > end) return 'COMPLETED';
    return 'ONGOING';
  }

  protected termTypeLabel(type: TermType): string {
    return type === 'ODD' ? 'Odd' : 'Even';
  }

  // ─── Events panel helpers ───
  protected getEventsForTerm(term: TermInstance): CalendarEvent[] {
    return this.events().filter((event) => event.startDate <= term.endDate && event.endDate >= term.startDate);
  }

  protected getYearLevelEvents(): CalendarEvent[] {
    return this.events();
  }

  protected eventTypeBadgeClass(type: CalendarEventType): string {
    const map: Record<CalendarEventType, string> = {
      HOLIDAY: 'cms-badge--amber',
      EXAM: 'cms-badge--red',
      CULTURAL: 'cms-badge--violet',
      SPORTS: 'cms-badge--cyan',
      WORKSHOP: 'cms-badge--blue',
      OTHER: 'cms-badge--gray',
    };
    return map[type] ?? 'cms-badge--gray';
  }

  // ─── Event CRUD ───
  protected openAddEvent(): void {
    const ay = this.selectedAcademicYear();
    if (!ay) return;
    this.editingEvent.set(null);
    this.eventForm.reset({ eventType: 'HOLIDAY' });
    this.showEventDialog.set(true);
  }

  protected openEditEvent(event: CalendarEvent): void {
    this.editingEvent.set(event);
    this.eventForm.patchValue({
      title: event.title,
      description: event.description ?? '',
      startDate: event.startDate,
      endDate: event.endDate,
      eventType: event.eventType,
    });
    this.showEventDialog.set(true);
  }

  protected closeEventDialog(): void {
    this.showEventDialog.set(false);
    this.editingEvent.set(null);
    this.eventForm.reset({ eventType: 'HOLIDAY' });
  }

  protected saveEvent(): void {
    if (this.eventForm.invalid) {
      scrollToFirstInvalid(this.eventForm);
      return;
    }
    const ay = this.selectedAcademicYear();
    if (!ay) return;

    const val = this.eventForm.getRawValue();
    const req: CalendarEventRequest = {
      title: val.title!,
      description: val.description ?? undefined,
      startDate: val.startDate!,
      endDate: val.endDate!,
      eventType: val.eventType as CalendarEventType,
      academicYearId: ay.id,
    };

    this.eventSaving.set(true);
    const editing = this.editingEvent();
    const call$ = editing
      ? this.academicYearService.updateCalendarEvent(editing.id, req)
      : this.academicYearService.createCalendarEvent(req);

    call$.subscribe({
      next: () => {
        this.toast.success(editing ? 'Event updated' : 'Event created');
        this.closeEventDialog();
        this.eventSaving.set(false);
        this.reloadEvents();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to save event');
        this.eventSaving.set(false);
      },
    });
  }

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

  private reloadEvents(): void {
    const ay = this.selectedAcademicYear();
    if (!ay) return;
    this.academicYearService.getCalendarEventsByAcademicYear(ay.id).subscribe({
      next: (evts) => this.events.set(evts),
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
  private buildMonthGrids(
    ay: AcademicYear,
    termInstances: TermInstance[],
    events: CalendarEvent[],
  ): MonthGrid[] {
    const start = new Date(ay.startDate);
    const end = new Date(ay.endDate);
    const grids: MonthGrid[] = [];

    let cur = new Date(start.getFullYear(), start.getMonth(), 1);
    const endMonthStart = new Date(end.getFullYear(), end.getMonth(), 1);
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    while (cur <= endMonthStart) {
      const year = cur.getFullYear();
      const month = cur.getMonth();
      const firstDay = new Date(year, month, 1);
      const lastDay = new Date(year, month + 1, 0);

      // Pad start with blanks to align to Sunday
      const startDow = firstDay.getDay();
      const days: DayCell[] = [];

      for (let pad = 0; pad < startDow; pad++) {
        const d = new Date(year, month, -startDow + pad + 1);
        days.push(this.buildDayCell(d, false, termInstances, events, today));
      }
      for (let d = 1; d <= lastDay.getDate(); d++) {
        const date = new Date(year, month, d);
        days.push(this.buildDayCell(date, true, termInstances, events, today));
      }
      // Pad end to complete final week
      while (days.length % 7 !== 0) {
        const date = new Date(year, month + 1, days.length - lastDay.getDate() - startDow + 1);
        days.push(this.buildDayCell(date, false, termInstances, events, today));
      }

      grids.push({ year, month, label: `${this.MONTH_NAMES[month]} ${year}`, days });
      cur = new Date(year, month + 1, 1);
    }
    return grids;
  }

  private buildDayCell(
    date: Date,
    isCurrentMonth: boolean,
    termInstances: TermInstance[],
    events: CalendarEvent[],
    today: Date,
  ): DayCell {
    const iso = this.toIso(date);
    const term = termInstances.find((item) => item.startDate <= iso && item.endDate >= iso);
    const dayEvents = events.filter((e) => e.startDate <= iso && e.endDate >= iso);
    return {
      date,
      dayNum: date.getDate(),
      isCurrentMonth,
      termStatus: term ? this.getTermStatus(term) : null,
      termName: term ? this.getTermLabel(term) : null,
      events: dayEvents,
      isToday: date.getTime() === today.getTime(),
    };
  }

  private mapTermStatus(status: TermInstanceStatus): 'UPCOMING' | 'ONGOING' | 'COMPLETED' | null {
    switch (status) {
      case 'PLANNED': return 'UPCOMING';
      case 'OPEN': return 'ONGOING';
      case 'LOCKED': return 'COMPLETED';
      default: return null;
    }
  }

  private toIso(d: Date): string {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  protected trackByMonth(index: number, grid: MonthGrid): string {
    return `${grid.year}-${grid.month}`;
  }

  protected trackById(index: number, item: { id: number }): number {
    return item.id;
  }
}

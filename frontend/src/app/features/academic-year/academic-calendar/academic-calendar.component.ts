import {
  Component,
  computed,
  ElementRef,
  inject,
  OnInit,
  signal,
  ViewChild,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { forkJoin } from 'rxjs';
import { AcademicYearService } from '../academic-year.service';
import {
  AcademicYear,
  CalendarEvent,
  CalendarEventRequest,
  CalendarEventType,
  Semester,
} from '../academic-year.model';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { ToastService } from '../../../core/toast/toast.service';
import { AuthService } from '../../../core/auth/auth.service';
import { PrintService } from '../../../core/print/print.service';
import { CsvExporterService } from '../../../core/export/csv-exporter.service';

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
  semesterStatus: 'UPCOMING' | 'ONGOING' | 'COMPLETED' | null;
  semesterName: string | null;
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
    DatePipe,
    FormsModule,
    ReactiveFormsModule,
    RouterLink,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatFormFieldModule,
    MatInputModule,
    MatDialogModule,
    MatMenuModule,
    MatTooltipModule,
    PageHeaderComponent,
  ],
  templateUrl: './academic-calendar.component.html',
  styleUrl: './academic-calendar.component.scss',
})
export class AcademicCalendarComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly fb = inject(FormBuilder);
  private readonly printService = inject(PrintService);
  private readonly csvExporter = inject(CsvExporterService);
  protected readonly auth = inject(AuthService);

  @ViewChild('calendarPrintArea') calendarPrintArea!: ElementRef<HTMLElement>;

  // ─── Loading / error state ───
  protected readonly loading = signal(false);
  protected readonly hasError = signal(false);

  // ─── Data signals ───
  protected readonly allAcademicYears = signal<AcademicYear[]>([]);
  protected readonly selectedAcademicYear = signal<AcademicYear | null>(null);
  protected readonly semesters = signal<Semester[]>([]);
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
  protected readonly canManage = computed(
    () => this.auth.isAdmin() || this.auth.isCollegeAdmin(),
  );

  // ─── Stats ───
  protected readonly stats = computed(() => {
    const ay = this.selectedAcademicYear();
    const sems = this.semesters();
    const evts = this.events();

    if (!ay) return null;

    const start = new Date(ay.startDate);
    const end = new Date(ay.endDate);
    const totalDays = Math.round((end.getTime() - start.getTime()) / 86_400_000) + 1;
    const totalWeeks = Math.round(totalDays / 7);

    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const currentSem = sems.find((s) => {
      const ss = new Date(s.startDate);
      const se = new Date(s.endDate);
      return today >= ss && today <= se;
    });

    const daysRemaining = currentSem
      ? Math.max(
          0,
          Math.round((new Date(currentSem.endDate).getTime() - today.getTime()) / 86_400_000),
        )
      : null;

    return {
      totalWeeks,
      semesterCount: sems.length,
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
    return this.buildMonthGrids(ay, this.semesters(), this.events());
  });

  // ─── Event form ───
  protected readonly eventForm = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    description: [''],
    startDate: ['', Validators.required],
    endDate: ['', Validators.required],
    eventType: ['HOLIDAY' as CalendarEventType, Validators.required],
    semesterId: [null as number | null],
  });

  private readonly MONTH_NAMES = [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December',
  ];

  ngOnInit(): void {
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

  private loadYearData(yearId: number): void {
    forkJoin({
      semesters: this.academicYearService.getSemestersByAcademicYear(yearId),
      events: this.academicYearService.getCalendarEventsByAcademicYear(yearId),
    }).subscribe({
      next: ({ semesters, events }) => {
        this.semesters.set(semesters);
        this.events.set(events);
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load calendar data');
        this.loading.set(false);
      },
    });
  }

  // ─── Semester helpers ───
  protected getSemesterProgress(semester: Semester): number {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const start = new Date(semester.startDate);
    const end = new Date(semester.endDate);
    if (today < start) return 0;
    if (today > end) return 100;
    const total = end.getTime() - start.getTime();
    const elapsed = today.getTime() - start.getTime();
    return Math.round((elapsed / total) * 100);
  }

  protected getSemesterDays(semester: Semester): { elapsed: number; total: number } {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const start = new Date(semester.startDate);
    const end = new Date(semester.endDate);
    const total = Math.round((end.getTime() - start.getTime()) / 86_400_000) + 1;
    const elapsed = Math.min(
      total,
      Math.max(0, Math.round((today.getTime() - start.getTime()) / 86_400_000) + 1),
    );
    return { elapsed: today < start ? 0 : elapsed, total };
  }

  // ─── Events panel helpers ───
  protected getEventsForSemester(semester: Semester): CalendarEvent[] {
    return this.events().filter((e) => e.semester?.id === semester.id);
  }

  protected getUnassignedEvents(): CalendarEvent[] {
    return this.events().filter((e) => !e.semester);
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
    this.eventForm.reset({ eventType: 'HOLIDAY', semesterId: null });
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
      semesterId: event.semester?.id ?? null,
    });
    this.showEventDialog.set(true);
  }

  protected closeEventDialog(): void {
    this.showEventDialog.set(false);
    this.editingEvent.set(null);
    this.eventForm.reset({ eventType: 'HOLIDAY', semesterId: null });
  }

  protected saveEvent(): void {
    if (this.eventForm.invalid) {
      this.eventForm.markAllAsTouched();
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
      semesterId: val.semesterId ?? undefined,
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
      error: () => {
        this.toast.error('Failed to save event');
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
      error: () => this.toast.error('Failed to delete event'),
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
          key: 'semester',
          header: 'Semester',
          format: (_v, row) => (row as CalendarEvent).semester?.name ?? '',
        },
      ],
      evts,
    );
  }

  // ─── Grid builder ───
  private buildMonthGrids(
    ay: AcademicYear,
    semesters: Semester[],
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
        days.push(this.buildDayCell(d, false, semesters, events, today));
      }
      for (let d = 1; d <= lastDay.getDate(); d++) {
        const date = new Date(year, month, d);
        days.push(this.buildDayCell(date, true, semesters, events, today));
      }
      // Pad end to complete final week
      while (days.length % 7 !== 0) {
        const date = new Date(year, month + 1, days.length - lastDay.getDate() - startDow + 1);
        days.push(this.buildDayCell(date, false, semesters, events, today));
      }

      grids.push({ year, month, label: `${this.MONTH_NAMES[month]} ${year}`, days });
      cur = new Date(year, month + 1, 1);
    }
    return grids;
  }

  private buildDayCell(
    date: Date,
    isCurrentMonth: boolean,
    semesters: Semester[],
    events: CalendarEvent[],
    today: Date,
  ): DayCell {
    const iso = this.toIso(date);
    const sem = semesters.find((s) => s.startDate <= iso && s.endDate >= iso);
    const dayEvents = events.filter((e) => e.startDate <= iso && e.endDate >= iso);
    return {
      date,
      dayNum: date.getDate(),
      isCurrentMonth,
      semesterStatus: sem ? sem.status : null,
      semesterName: sem ? sem.name : null,
      events: dayEvents,
      isToday: date.getTime() === today.getTime(),
    };
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

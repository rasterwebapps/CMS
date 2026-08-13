import { Component, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { CmsFlyoutPanelComponent } from '../../../../shared/flyout-panel/flyout-panel.component';
import { ToastService } from '../../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../../shared/utils/scroll-to-invalid';
import { AcademicYearService } from '../../academic-year.service';
import { BlockedPeriodService } from '../../blocked-period.service';
import { DayMappingService } from '../../day-mapping.service';
import { Period } from '../../../period/period.model';
import {
  AcademicYear,
  AppDayOfWeek,
  BlockedPeriod,
  BlockedPeriodRequest,
  BlockType,
  CalendarEvent,
  CalendarEventRequest,
  CalendarEventType,
  DayMapping,
  DayMappingRequest,
  EventRecurrenceRequest,
  HolidayCategory,
  HolidayRecurrenceType,
  TermInstance,
  WeekOfMonth,
} from '../../academic-year.model';
import { HolidayTemplateService } from '../../../holiday-template/holiday-template.service';
import {
  DAY_MAPPING_BADGE_CLASS,
  DAY_MAPPING_ICON,
  DAY_OF_WEEK_LABELS,
  EVENT_TYPE_BADGE_CLASS,
  EVENT_TYPE_ICONS,
  EVENT_TYPE_LABELS,
} from '../calendar-display.constants';
import { formatBlockSummary } from '../blocked-period-summary.util';
import { FlyoutMiniCalendarComponent } from './flyout-mini-calendar/flyout-mini-calendar.component';

export type DayDetailSection = 'EVENTS' | 'BLOCKS' | 'DAY_MAPPING';

type EventFormMode = 'CLOSED' | 'ADD' | 'EDIT';
type BlockStep = 'FORM' | 'RESULT';
/** Mirrors the presets in iOS/Google Calendar's Repeat picker; CUSTOM opens the full
 *  frequency+interval+pattern+end-date controls. */
type RepeatPreset = 'NONE' | 'YEARLY' | 'MONTHLY' | 'WEEKLY' | 'DAILY' | 'CUSTOM';
type MonthlyPattern = 'DAY_OF_MONTH' | 'NTH_WEEKDAY';

interface BlockSubmitResult {
  succeeded: number;
  failed: { req: BlockedPeriodRequest; reason: string }[];
}

/** JS Date.getDay(): 0=Sun..6=Sat. AppDayOfWeek has no Sunday value (matches the backend
 *  enum), so index 0 deliberately maps to null. */
const JS_DAY_TO_APP_DAY: readonly (AppDayOfWeek | null)[] =
  [null, 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];

/** Above this many rows, force an explicit confirmation before firing that many sequential
 *  create() calls -- there's no batch endpoint, see blocked-period.service.ts. */
const MULTI_ROW_CONFIRM_THRESHOLD = 15;

@Component({
  selector: 'app-day-detail-flyout',
  standalone: true,
  imports: [
    FormsModule,
    ReactiveFormsModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatFormFieldModule,
    MatInputModule,
    MatTooltipModule,
    MatMenuModule,
    CmsFlyoutPanelComponent,
    FlyoutMiniCalendarComponent,
  ],
  templateUrl: './day-detail-flyout.component.html',
  styleUrl: './day-detail-flyout.component.scss',
})
export class DayDetailFlyoutComponent implements OnInit {
  readonly date = input.required<Date>();
  readonly academicYear = input.required<AcademicYear>();
  readonly periods = input.required<Period[]>();
  readonly events = input.required<CalendarEvent[]>();
  readonly blockedPeriods = input.required<BlockedPeriod[]>();
  readonly termInstances = input.required<TermInstance[]>();
  readonly dayMappings = input.required<DayMapping[]>();
  readonly canManageEvents = input.required<boolean>();
  readonly canManageBlocks = input.required<boolean>();
  readonly canManageDayMapping = input.required<boolean>();
  readonly focusEventId = input<number | null>(null);
  readonly focusBlockId = input<number | null>(null);
  readonly focusMappingId = input<number | null>(null);
  readonly initialSection = input<DayDetailSection | null>(null);

  readonly closed = output<void>();
  readonly dataChanged = output<void>();

  private readonly academicYearService = inject(AcademicYearService);
  private readonly blockedPeriodService = inject(BlockedPeriodService);
  private readonly dayMappingService = inject(DayMappingService);
  private readonly holidayTemplateService = inject(HolidayTemplateService);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  protected readonly eventTypes: CalendarEventType[] = ['HOLIDAY', 'EXAM', 'CULTURAL', 'SPORTS', 'WORKSHOP', 'OTHER'];
  protected readonly eventTypeLabels = EVENT_TYPE_LABELS;
  protected readonly eventTypeIcons = EVENT_TYPE_ICONS;
  protected readonly holidayCategories: HolidayCategory[] = ['GOVERNMENT', 'LOCAL', 'INSTITUTIONAL'];
  protected readonly holidayCategoryLabels: Record<HolidayCategory, string> = {
    GOVERNMENT: 'Government',
    LOCAL: 'Local',
    INSTITUTIONAL: 'Institutional',
  };
  protected readonly daysOfWeek: AppDayOfWeek[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
  protected readonly dayOfWeekLabels = DAY_OF_WEEK_LABELS;

  // ─── Date picker (drives every "on this date" query below) ───
  protected readonly selectedDateIso = signal('');
  protected readonly selectedWeekday = computed<AppDayOfWeek | null>(() => {
    const iso = this.selectedDateIso();
    if (!iso) return null;
    return JS_DAY_TO_APP_DAY[new Date(`${iso}T00:00:00`).getDay()];
  });
  protected readonly selectedDateLabel = computed(() => {
    const iso = this.selectedDateIso();
    if (!iso) return '';
    return new Intl.DateTimeFormat('en-GB', { weekday: 'long', day: '2-digit', month: 'short', year: 'numeric' })
      .format(new Date(`${iso}T00:00:00`));
  });

  // ─── Events on this date ───
  protected readonly eventsOnDate = computed(() => {
    const iso = this.selectedDateIso();
    if (!iso) return [];
    return this.events().filter((e) => e.startDate <= iso && e.endDate >= iso);
  });

  protected readonly eventFormMode = signal<EventFormMode>('CLOSED');
  protected readonly editingEventId = signal<number | null>(null);
  protected readonly eventSaving = signal(false);
  protected readonly eventForm = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    description: [''],
    startDate: ['', Validators.required],
    endDate: ['', Validators.required],
    eventType: ['HOLIDAY' as CalendarEventType, Validators.required],
    holidayCategory: [null as HolidayCategory | null],
  });

  protected showHolidayCategoryField(): boolean {
    return this.eventForm.get('eventType')?.value === 'HOLIDAY';
  }

  // ─── Half-day holiday period picker (only meaningful when eventType === 'HOLIDAY') ───
  /** Whole-day is the default -- an empty `holidayPeriodIds` selection, matching the backend's
   *  own "null/empty means every active period" contract exactly (see CalendarEventService
   *  .resolvePeriodIds), rather than the frontend independently tracking "all periods selected". */
  protected readonly holidayWholeDay = signal(true);
  protected readonly holidayPeriodIds = signal<Set<number>>(new Set());

  protected showPeriodPickerField(): boolean {
    return this.eventForm.get('eventType')?.value === 'HOLIDAY';
  }

  protected toggleHolidayWholeDay(): void {
    const next = !this.holidayWholeDay();
    this.holidayWholeDay.set(next);
    if (next) this.holidayPeriodIds.set(new Set());
  }

  protected toggleHolidayPeriod(periodId: number): void {
    if (this.holidayWholeDay()) return;
    const next = new Set(this.holidayPeriodIds());
    if (next.has(periodId)) next.delete(periodId);
    else next.add(periodId);
    this.holidayPeriodIds.set(next);
  }

  // ─── Repeats (mirrors a simplified iOS/Google Calendar Repeat picker) ───
  protected readonly weeksOfMonth: WeekOfMonth[] = ['FIRST', 'SECOND', 'THIRD', 'FOURTH', 'LAST'];
  protected readonly repeatPreset = signal<RepeatPreset>('NONE');
  protected readonly customFrequency = signal<HolidayRecurrenceType>('WEEKLY');
  protected readonly customInterval = signal(1);
  protected readonly customMonthlyPattern = signal<MonthlyPattern>('DAY_OF_MONTH');
  protected readonly customWeekOfMonth = signal<WeekOfMonth>('SECOND');
  protected readonly customDayOfWeek = signal<AppDayOfWeek>('MONDAY');
  protected readonly eventRepeatEndDate = signal<string | null>(null);
  protected readonly loadingRepeatTemplate = signal(false);
  /** Existing linked template's id when editing an already-repeating event -- carried through so
   *  saveEvent() knows an update (not a create) is happening server-side, though the actual
   *  create-vs-update decision is made backend-side purely from the event's own linkage; this is
   *  only used to show "Repeats" pre-selected instead of "Does not repeat" on open. */
  private editingTemplateId: number | null = null;

  /** Sunday isn't representable (the app's DayOfWeek enum has no Sunday value -- already globally
   *  non-teaching everywhere else in the codebase), so a start date landing on Sunday can't use
   *  the Weekly preset or Custom Weekly/nth-weekday-Monthly patterns. */
  protected startDateIsSunday(): boolean {
    const iso = this.eventForm.get('startDate')?.value;
    if (!iso) return false;
    return new Date(`${iso}T00:00:00`).getDay() === 0;
  }

  protected showRepeatField(): boolean {
    return this.eventFormMode() !== 'CLOSED';
  }

  protected onRepeatPresetChange(preset: RepeatPreset): void {
    this.repeatPreset.set(preset);
    if (preset === 'CUSTOM') {
      // Default Custom's starting point to whatever plain preset makes sense for today's weekday,
      // so switching into Custom doesn't dump the admin into an arbitrary, possibly-invalid state.
      this.customFrequency.set('WEEKLY');
    }
  }

  private appDayOfWeekFor(iso: string): AppDayOfWeek | null {
    return JS_DAY_TO_APP_DAY[new Date(`${iso}T00:00:00`).getDay()];
  }

  /** Builds the request payload from either a quick preset (deriving the actual pattern from the
   *  event's own chosen startDate) or the full Custom controls. Null means "does not repeat". */
  protected buildRecurrenceRequest(): EventRecurrenceRequest | null {
    const preset = this.repeatPreset();
    if (preset === 'NONE') return null;

    const startIso = this.eventForm.get('startDate')?.value as string;
    const startDate = new Date(`${startIso}T00:00:00`);

    if (preset === 'CUSTOM') {
      const frequency = this.customFrequency();
      const base: EventRecurrenceRequest = {
        recurrenceType: frequency,
        intervalCount: this.customInterval(),
        endDate: this.eventRepeatEndDate() || null,
      };
      if (frequency === 'YEARLY') {
        return { ...base, month: startDate.getMonth() + 1, dayOfMonth: startDate.getDate() };
      }
      if (frequency === 'MONTHLY') {
        return this.customMonthlyPattern() === 'DAY_OF_MONTH'
          ? { ...base, dayOfMonth: startDate.getDate() }
          : { ...base, weekOfMonth: this.customWeekOfMonth(), dayOfWeek: this.customDayOfWeek() };
      }
      if (frequency === 'WEEKLY') {
        return { ...base, dayOfWeek: this.customDayOfWeek() };
      }
      return base; // DAILY
    }

    // Quick presets derive the pattern from the event's own start date.
    if (preset === 'YEARLY') {
      return { recurrenceType: 'YEARLY', intervalCount: 1, month: startDate.getMonth() + 1, dayOfMonth: startDate.getDate() };
    }
    if (preset === 'MONTHLY') {
      return { recurrenceType: 'MONTHLY', intervalCount: 1, dayOfMonth: startDate.getDate() };
    }
    if (preset === 'WEEKLY') {
      const dow = this.appDayOfWeekFor(startIso);
      return dow ? { recurrenceType: 'WEEKLY', intervalCount: 1, dayOfWeek: dow } : null;
    }
    return { recurrenceType: 'DAILY', intervalCount: 1 };
  }

  private resetRepeatState(): void {
    this.repeatPreset.set('NONE');
    this.customFrequency.set('WEEKLY');
    this.customInterval.set(1);
    this.customMonthlyPattern.set('DAY_OF_MONTH');
    this.customWeekOfMonth.set('SECOND');
    this.customDayOfWeek.set('MONDAY');
    this.eventRepeatEndDate.set(null);
    this.editingTemplateId = null;
  }

  /** Prefills the Repeat picker from an already-linked template so editing a repeating event
   *  shows "Repeats" (in Custom mode, since a template's exact stored shape doesn't always map
   *  back cleanly onto one of the quick presets) instead of silently resetting to one-time. */
  private loadRepeatStateFromTemplate(sourceHolidayTemplateId: number): void {
    this.loadingRepeatTemplate.set(true);
    this.holidayTemplateService.getById(sourceHolidayTemplateId).subscribe({
      next: (template) => {
        this.editingTemplateId = template.id;
        this.repeatPreset.set('CUSTOM');
        this.customFrequency.set(template.recurrenceType);
        this.customInterval.set(template.intervalCount ?? 1);
        this.customMonthlyPattern.set(template.dayOfMonth != null ? 'DAY_OF_MONTH' : 'NTH_WEEKDAY');
        if (template.weekOfMonth) this.customWeekOfMonth.set(template.weekOfMonth);
        if (template.dayOfWeek) this.customDayOfWeek.set(template.dayOfWeek);
        this.eventRepeatEndDate.set(template.endDate ?? null);
        this.loadingRepeatTemplate.set(false);
      },
      error: () => {
        this.loadingRepeatTemplate.set(false);
      },
    });
  }

  protected eventTypeBadgeClass(type: CalendarEventType): string {
    return EVENT_TYPE_BADGE_CLASS[type] ?? 'cms-badge--gray';
  }

  // ─── Blocks on this date ───
  protected readonly blocksOnDate = computed(() => {
    const iso = this.selectedDateIso();
    const weekday = this.selectedWeekday();
    if (!iso) return [];
    return this.blockedPeriods().filter((b) => {
      if (b.blockType === 'ONE_OFF') return b.specificDate === iso;
      return b.dayOfWeek === weekday && b.rangeStartDate! <= iso && b.rangeEndDate! >= iso;
    });
  });

  protected readonly blockedPeriodIdsOnDate = computed(() => {
    const set = new Set<number>();
    for (const b of this.blocksOnDate()) set.add(b.periodId);
    return set;
  });

  protected readonly blockFormOpen = signal(false);
  protected readonly editingBlockId = signal<number | null>(null);
  protected readonly selectedPeriodIds = signal<Set<number>>(new Set());
  protected readonly blockType = signal<BlockType>('ONE_OFF');
  protected readonly repeatStartDate = signal('');
  protected readonly repeatEndDate = signal('');
  protected readonly repeatWeekdays = signal<Set<AppDayOfWeek>>(new Set());
  protected readonly blockReason = signal('');
  protected readonly blockSaving = signal(false);
  protected readonly blockStep = signal<BlockStep>('FORM');
  protected readonly blockResult = signal<BlockSubmitResult | null>(null);

  protected readonly blockRequestCount = computed(() => {
    const periodCount = this.selectedPeriodIds().size;
    if (periodCount === 0) return 0;
    if (this.blockType() === 'ONE_OFF') return periodCount;
    return periodCount * Math.max(1, this.repeatWeekdays().size);
  });

  ngOnInit(): void {
    const iso = this.toIso(this.date());
    this.selectedDateIso.set(iso);
    this.repeatStartDate.set(iso);
    this.repeatEndDate.set(this.academicYear().endDate);
    const weekday = this.selectedWeekday();
    if (weekday) this.repeatWeekdays.set(new Set([weekday]));

    const focusEventId = this.focusEventId();
    const focusBlockId = this.focusBlockId();
    const focusMappingId = this.focusMappingId();
    if (focusEventId != null) {
      const evt = this.events().find((e) => e.id === focusEventId);
      if (evt) this.openEditEventForm(evt);
    } else if (focusBlockId != null) {
      const block = this.blockedPeriods().find((b) => b.id === focusBlockId);
      if (block) this.openEditBlockForm(block);
    } else if (focusMappingId != null) {
      const mapping = this.dayMappings().find((m) => m.id === focusMappingId);
      if (mapping) this.openEditMappingForm(mapping);
    } else if (this.initialSection() === 'EVENTS') {
      this.openAddEventForm();
    } else if (this.initialSection() === 'BLOCKS') {
      this.blockFormOpen.set(true);
    } else if (this.initialSection() === 'DAY_MAPPING') {
      this.openAddMappingForm();
    }
  }

  protected onDateChange(value: string): void {
    this.selectedDateIso.set(value);
    this.selectedPeriodIds.set(new Set());
    if (!this.editingBlockId()) {
      this.repeatStartDate.set(value);
      const weekday = this.selectedWeekday();
      this.repeatWeekdays.set(weekday ? new Set([weekday]) : new Set());
    }
  }

  /** Tracks the range-picker's own click sequence -- deliberately separate from the form's
   *  startDate/endDate control values, which are always pre-filled with a real (equal) date pair
   *  the moment the form opens. Reading the form's current values directly (as an earlier version
   *  of this method did) made the very first click look like "a single day is already selected,
   *  extend its end" instead of "start picking a fresh range" -- the reported bug where a range
   *  needed three clicks. Null means "the next click starts a brand-new selection". */
  private rangePickerAnchor: string | null = null;

  /** Click handler for the inline mini-calendar -- dual role depending on what's open. With no
   *  Add/Edit Event form open, a click is exactly the old "change viewing date" native-input
   *  action. With the form open, a click builds a date range: the first click after the form
   *  opens (or after a range was just completed) always starts a fresh single-day selection; a
   *  second click on/after that date completes the range and resets the sequence, so a third
   *  click starts picking an entirely new range rather than extending further. The native
   *  Start/End Date inputs stay in sync automatically since both read/write the same form
   *  controls. */
  protected onCalendarDayClick(iso: string): void {
    if (this.eventFormMode() === 'CLOSED') {
      this.onDateChange(iso);
      return;
    }
    const startCtrl = this.eventForm.get('startDate')!;
    const endCtrl = this.eventForm.get('endDate')!;
    const anchor = this.rangePickerAnchor;

    if (!anchor || iso < anchor) {
      this.rangePickerAnchor = iso;
      startCtrl.setValue(iso);
      endCtrl.setValue(iso);
    } else {
      startCtrl.setValue(anchor);
      endCtrl.setValue(iso);
      this.rangePickerAnchor = null;
    }
  }

  private toIso(d: Date): string {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  protected close(): void {
    this.closed.emit();
  }

  protected trackById(_index: number, item: { id: number }): number {
    return item.id;
  }

  // ─── Event mini-form ───
  protected openAddEventForm(): void {
    this.editingEventId.set(null);
    this.rangePickerAnchor = null;
    this.eventForm.reset({
      title: '',
      description: '',
      eventType: 'HOLIDAY',
      holidayCategory: null,
      startDate: this.selectedDateIso(),
      endDate: this.selectedDateIso(),
    });
    this.holidayWholeDay.set(true);
    this.holidayPeriodIds.set(new Set());
    this.overlappingEvents.set([]);
    this.resetRepeatState();
    this.eventFormMode.set('ADD');
  }

  protected openEditEventForm(event: CalendarEvent): void {
    this.editingEventId.set(event.id);
    this.rangePickerAnchor = null;
    this.eventForm.patchValue({
      title: event.title,
      description: event.description ?? '',
      startDate: event.startDate,
      endDate: event.endDate,
      eventType: event.eventType,
      holidayCategory: event.holidayCategory,
    });
    const activeIds = this.periods().map((p) => p.id);
    const isWholeDay = event.blockedPeriodIds.length === 0
      || activeIds.every((id) => event.blockedPeriodIds.includes(id));
    this.holidayWholeDay.set(isWholeDay);
    this.holidayPeriodIds.set(isWholeDay ? new Set() : new Set(event.blockedPeriodIds));
    this.overlappingEvents.set([]);
    this.resetRepeatState();
    if (event.sourceHolidayTemplateId != null) {
      this.loadRepeatStateFromTemplate(event.sourceHolidayTemplateId);
    }
    this.eventFormMode.set('EDIT');
  }

  protected cancelEventForm(): void {
    this.eventFormMode.set('CLOSED');
    this.editingEventId.set(null);
    this.overlappingEvents.set([]);
    this.rangePickerAnchor = null;
  }

  // ─── Conflict detection (any event type overlapping the proposed range) ───
  protected readonly overlappingEvents = signal<CalendarEvent[]>([]);
  protected readonly checkingConflicts = signal(false);

  protected saveEvent(): void {
    if (this.eventForm.invalid) {
      scrollToFirstInvalid(this.eventForm);
      return;
    }
    const val = this.eventForm.getRawValue();
    this.checkingConflicts.set(true);
    this.academicYearService.checkOverlappingEvents(
      this.academicYear().id, val.startDate!, val.endDate!, this.editingEventId() ?? undefined,
    ).subscribe({
      next: (overlaps) => {
        this.checkingConflicts.set(false);
        if (overlaps.length > 0) {
          this.overlappingEvents.set(overlaps);
        } else {
          this.performSaveEvent();
        }
      },
      // Fail open -- a broken conflict-check endpoint must never block saving an otherwise-valid event.
      error: () => {
        this.checkingConflicts.set(false);
        this.performSaveEvent();
      },
    });
  }

  /** Explicit override once the admin has seen the overlap list and decided to proceed anyway. */
  protected proceedDespiteConflicts(): void {
    this.overlappingEvents.set([]);
    this.performSaveEvent();
  }

  protected removeConflictingEvent(event: CalendarEvent): void {
    if (!confirm(`Delete "${event.title}"?`)) return;
    this.academicYearService.deleteCalendarEvent(event.id).subscribe({
      next: () => {
        this.toast.success('Event deleted');
        this.overlappingEvents.set(this.overlappingEvents().filter((e) => e.id !== event.id));
        this.dataChanged.emit();
      },
      error: (err) => this.toast.error(err?.error?.message ?? 'Failed to delete event'),
    });
  }

  private performSaveEvent(): void {
    const val = this.eventForm.getRawValue();
    const req: CalendarEventRequest = {
      title: val.title!,
      description: val.description ?? undefined,
      startDate: val.startDate!,
      endDate: val.endDate!,
      eventType: val.eventType as CalendarEventType,
      academicYearId: this.academicYear().id,
      holidayCategory: val.eventType === 'HOLIDAY' ? (val.holidayCategory as HolidayCategory | null) : null,
      blockedPeriodIds: val.eventType === 'HOLIDAY' && !this.holidayWholeDay()
        ? [...this.holidayPeriodIds()] : undefined,
      repeats: this.repeatPreset() !== 'NONE',
      recurrence: this.buildRecurrenceRequest(),
    };

    this.eventSaving.set(true);
    const editingId = this.editingEventId();
    const call$ = editingId
      ? this.academicYearService.updateCalendarEvent(editingId, req)
      : this.academicYearService.createCalendarEvent(req);

    call$.subscribe({
      next: () => {
        this.toast.success(editingId ? 'Event updated' : 'Event created');
        this.eventSaving.set(false);
        this.cancelEventForm();
        this.dataChanged.emit();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to save event');
        this.eventSaving.set(false);
      },
    });
  }

  /** Plain single-event delete. For a template-seeded event, the delete button opens a menu
   *  offering this ("this occurrence only") alongside deleteEventSeries -- both ultimately land
   *  here or there, never a bare confirm() for a recurring event. */
  protected deleteEvent(event: CalendarEvent): void {
    if (!confirm(`Delete "${event.title}"?`)) return;
    this.academicYearService.deleteCalendarEvent(event.id).subscribe({
      next: () => {
        this.toast.success('Event deleted');
        this.dataChanged.emit();
      },
      error: (err) => this.toast.error(err?.error?.message ?? 'Failed to delete event'),
    });
  }

  /** "Delete this and all future occurrences" -- stops the source Holiday Template from seeding
   *  further years and removes this + every other future-dated instance it generated. Past
   *  occurrences are never touched (enforced server-side). */
  protected deleteEventSeries(event: CalendarEvent): void {
    if (!confirm(
      `Delete "${event.title}" and every future occurrence of its holiday template? ` +
      `Past occurrences will not be affected.`,
    )) return;
    this.academicYearService.deleteCalendarEventSeries(event.id).subscribe({
      next: () => {
        this.toast.success('Event series deleted');
        this.dataChanged.emit();
      },
      error: (err) => this.toast.error(err?.error?.message ?? 'Failed to delete event series'),
    });
  }

  // ─── Blocks ───
  protected formatBlockSummary(block: BlockedPeriod): string {
    return formatBlockSummary(block, this.dayOfWeekLabels);
  }

  protected periodName(periodId: number): string {
    return this.periods().find((p) => p.id === periodId)?.name ?? `Period #${periodId}`;
  }

  protected openBlockSection(): void {
    this.blockFormOpen.set(true);
  }

  /** Tapping an already-blocked period jumps straight to editing that block instead of
   *  independently re-selecting it -- there's no server-side uniqueness constraint, so
   *  allowing a second overlapping row here would silently create duplicates. */
  protected togglePeriod(periodId: number): void {
    if (this.blockedPeriodIdsOnDate().has(periodId)) {
      const existing = this.blocksOnDate().find((b) => b.periodId === periodId);
      if (existing) this.openEditBlockForm(existing);
      return;
    }
    const next = new Set(this.selectedPeriodIds());
    if (next.has(periodId)) next.delete(periodId);
    else next.add(periodId);
    this.selectedPeriodIds.set(next);
  }

  protected toggleWholeDay(): void {
    const selectable = this.periods()
      .filter((p) => !this.blockedPeriodIdsOnDate().has(p.id))
      .map((p) => p.id);
    const allSelected = selectable.length > 0 && selectable.every((id) => this.selectedPeriodIds().has(id));
    this.selectedPeriodIds.set(allSelected ? new Set() : new Set(selectable));
  }

  protected toggleRepeatWeekday(day: AppDayOfWeek): void {
    const next = new Set(this.repeatWeekdays());
    if (next.has(day)) next.delete(day);
    else next.add(day);
    this.repeatWeekdays.set(next);
  }

  protected openEditBlockForm(block: BlockedPeriod): void {
    this.editingBlockId.set(block.id);
    this.selectedPeriodIds.set(new Set([block.periodId]));
    this.blockType.set(block.blockType);
    if (block.blockType === 'RECURRING') {
      this.repeatStartDate.set(block.rangeStartDate!);
      this.repeatEndDate.set(block.rangeEndDate!);
      this.repeatWeekdays.set(new Set([block.dayOfWeek!]));
    }
    this.blockReason.set(block.reason);
    this.blockStep.set('FORM');
    this.blockFormOpen.set(true);
  }

  protected cancelBlockForm(): void {
    this.blockFormOpen.set(false);
    this.editingBlockId.set(null);
    this.selectedPeriodIds.set(new Set());
    this.blockReason.set('');
    this.blockType.set('ONE_OFF');
    const weekday = this.selectedWeekday();
    this.repeatWeekdays.set(weekday ? new Set([weekday]) : new Set());
    this.repeatStartDate.set(this.selectedDateIso());
    this.repeatEndDate.set(this.academicYear().endDate);
    this.blockStep.set('FORM');
    this.blockResult.set(null);
  }

  private buildBlockRequests(): BlockedPeriodRequest[] {
    const periodIds = [...this.selectedPeriodIds()];
    const reason = this.blockReason().trim();

    if (this.blockType() === 'ONE_OFF') {
      const iso = this.selectedDateIso();
      return periodIds.map((periodId) => ({
        periodId,
        blockType: 'ONE_OFF' as BlockType,
        specificDate: iso,
        dayOfWeek: null,
        rangeStartDate: null,
        rangeEndDate: null,
        reason,
      }));
    }

    const weekdays = [...this.repeatWeekdays()];
    const requests: BlockedPeriodRequest[] = [];
    for (const periodId of periodIds) {
      for (const dayOfWeek of weekdays) {
        requests.push({
          periodId,
          blockType: 'RECURRING' as BlockType,
          specificDate: null,
          dayOfWeek,
          rangeStartDate: this.repeatStartDate(),
          rangeEndDate: this.repeatEndDate(),
          reason,
        });
      }
    }
    return requests;
  }

  protected submitBlock(): void {
    if (this.selectedPeriodIds().size === 0) {
      this.toast.error('Select at least one period to block');
      return;
    }
    if (!this.blockReason().trim()) {
      this.toast.error('A reason is required');
      return;
    }
    if (this.blockType() === 'RECURRING') {
      if (this.repeatWeekdays().size === 0) {
        this.toast.error('Select at least one day of the week to repeat on');
        return;
      }
      if (!this.repeatStartDate() || !this.repeatEndDate()) {
        this.toast.error('A start and end date are required for a recurring block');
        return;
      }
      if (this.repeatEndDate() < this.repeatStartDate()) {
        this.toast.error('End date must not be before start date');
        return;
      }
    }

    // A single-row edit bypasses the multi-row batching entirely.
    const editingId = this.editingBlockId();
    if (editingId != null) {
      const [req] = this.buildBlockRequests();
      this.blockSaving.set(true);
      this.blockedPeriodService.update(editingId, req).subscribe({
        next: () => {
          this.toast.success('Blocked period updated');
          this.blockSaving.set(false);
          this.cancelBlockForm();
          this.dataChanged.emit();
        },
        error: (err) => {
          this.toast.error(err?.error?.message ?? 'Failed to update blocked period');
          this.blockSaving.set(false);
        },
      });
      return;
    }

    const requests = this.buildBlockRequests();
    if (
      requests.length > MULTI_ROW_CONFIRM_THRESHOLD &&
      !confirm(`This will create ${requests.length} separate blocked-period rows. Continue?`)
    ) {
      return;
    }

    this.blockSaving.set(true);
    const calls = requests.map((req) =>
      this.blockedPeriodService.create(req).pipe(
        map(() => ({ ok: true as const, req })),
        catchError((err) => of({ ok: false as const, req, reason: err?.error?.message ?? 'Failed to create block' })),
      ),
    );
    forkJoin(calls).subscribe((results) => {
      this.blockSaving.set(false);
      this.blockResult.set({
        succeeded: results.filter((r) => r.ok).length,
        failed: results.filter((r) => !r.ok).map((r) => ({ req: r.req, reason: (r as { reason: string }).reason })),
      });
      this.selectedPeriodIds.set(new Set());
      this.blockStep.set('RESULT');
      if (results.some((r) => r.ok)) this.dataChanged.emit();
    });
  }

  protected finishBlockResult(): void {
    this.cancelBlockForm();
  }

  protected deleteBlock(block: BlockedPeriod): void {
    if (!confirm(`Delete this block ("${block.reason}")?`)) return;
    this.blockedPeriodService.delete(block.id).subscribe({
      next: () => {
        this.toast.success('Blocked period deleted');
        this.dataChanged.emit();
      },
      error: (err) => this.toast.error(err?.error?.message ?? 'Failed to delete blocked period'),
    });
  }

  // ─── Day mapping override on this date ───
  protected readonly dayMappingIcon = DAY_MAPPING_ICON;
  protected readonly dayMappingBadgeClass = DAY_MAPPING_BADGE_CLASS;

  /** Mappings are unique per date, so this is a single optional value rather than a list. */
  protected readonly mappingOnDate = computed(() => {
    const iso = this.selectedDateIso();
    if (!iso) return null;
    return this.dayMappings().find((m) => m.mappedDate === iso) ?? null;
  });

  /** The term instance the selected date falls in -- required to build a DayMappingRequest.
   *  Null when the date sits outside every term this academic year has (e.g. a gap between an
   *  ODD and EVEN term), in which case the Add action is disabled with an explanatory message. */
  protected readonly termInstanceForSelectedDate = computed<TermInstance | null>(() => {
    const iso = this.selectedDateIso();
    if (!iso) return null;
    return this.termInstances().find((t) => t.startDate <= iso && t.endDate >= iso) ?? null;
  });

  protected readonly mappingFormOpen = signal(false);
  protected readonly editingMappingId = signal<number | null>(null);
  protected readonly mappingBorrowedDay = signal<AppDayOfWeek | null>(null);
  protected readonly mappingReason = signal('');
  protected readonly mappingSaving = signal(false);

  protected mappingBorrowedDayLabel(mapping: DayMapping): string {
    return this.dayOfWeekLabels[mapping.borrowedDayOfWeek];
  }

  protected openAddMappingForm(): void {
    this.editingMappingId.set(null);
    this.mappingBorrowedDay.set(null);
    this.mappingReason.set('');
    this.mappingFormOpen.set(true);
  }

  protected openEditMappingForm(mapping: DayMapping): void {
    this.editingMappingId.set(mapping.id);
    this.mappingBorrowedDay.set(mapping.borrowedDayOfWeek);
    this.mappingReason.set(mapping.reason);
    this.mappingFormOpen.set(true);
  }

  protected cancelMappingForm(): void {
    this.mappingFormOpen.set(false);
    this.editingMappingId.set(null);
    this.mappingBorrowedDay.set(null);
    this.mappingReason.set('');
  }

  protected saveMapping(): void {
    const term = this.termInstanceForSelectedDate();
    const borrowedDay = this.mappingBorrowedDay();
    const reason = this.mappingReason().trim();
    if (!term) {
      this.toast.error('This date does not fall within any term');
      return;
    }
    if (!borrowedDay) {
      this.toast.error('Select a day of week to borrow');
      return;
    }
    if (!reason) {
      this.toast.error('A reason is required');
      return;
    }

    const request: DayMappingRequest = {
      termInstanceId: term.id,
      mappedDate: this.selectedDateIso(),
      borrowedDayOfWeek: borrowedDay,
      reason,
    };

    this.mappingSaving.set(true);
    const editingId = this.editingMappingId();
    const call$ = editingId
      ? this.dayMappingService.update(editingId, request)
      : this.dayMappingService.create(request);

    call$.subscribe({
      next: () => {
        this.toast.success(editingId ? 'Day mapping updated' : 'Day mapping created');
        this.mappingSaving.set(false);
        this.cancelMappingForm();
        this.dataChanged.emit();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to save day mapping');
        this.mappingSaving.set(false);
      },
    });
  }

  protected deleteMapping(mapping: DayMapping): void {
    if (!confirm(`Delete this day mapping ("${mapping.reason}")?`)) return;
    this.dayMappingService.delete(mapping.id).subscribe({
      next: () => {
        this.toast.success('Day mapping deleted');
        this.dataChanged.emit();
      },
      error: (err) => this.toast.error(err?.error?.message ?? 'Failed to delete day mapping'),
    });
  }
}

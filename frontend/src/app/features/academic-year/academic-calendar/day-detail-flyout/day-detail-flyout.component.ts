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
  HolidayCategory,
} from '../../academic-year.model';
import {
  DAY_OF_WEEK_LABELS,
  EVENT_TYPE_BADGE_CLASS,
  EVENT_TYPE_ICONS,
  EVENT_TYPE_LABELS,
} from '../calendar-display.constants';
import { formatBlockSummary } from '../blocked-period-summary.util';
import { FlyoutMiniCalendarComponent } from './flyout-mini-calendar/flyout-mini-calendar.component';

export type DayDetailSection = 'EVENTS' | 'BLOCKS';

type EventFormMode = 'CLOSED' | 'ADD' | 'EDIT';
type BlockStep = 'FORM' | 'RESULT';

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
  readonly canManageEvents = input.required<boolean>();
  readonly canManageBlocks = input.required<boolean>();
  readonly focusEventId = input<number | null>(null);
  readonly focusBlockId = input<number | null>(null);
  readonly initialSection = input<DayDetailSection | null>(null);

  readonly closed = output<void>();
  readonly dataChanged = output<void>();

  private readonly academicYearService = inject(AcademicYearService);
  private readonly blockedPeriodService = inject(BlockedPeriodService);
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
    if (focusEventId != null) {
      const evt = this.events().find((e) => e.id === focusEventId);
      if (evt) this.openEditEventForm(evt);
    } else if (focusBlockId != null) {
      const block = this.blockedPeriods().find((b) => b.id === focusBlockId);
      if (block) this.openEditBlockForm(block);
    } else if (this.initialSection() === 'EVENTS') {
      this.openAddEventForm();
    } else if (this.initialSection() === 'BLOCKS') {
      this.blockFormOpen.set(true);
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

  /** Click handler for the inline mini-calendar -- dual role depending on what's open. With no
   *  Add/Edit Event form open, a click is exactly the old "change viewing date" native-input
   *  action. With the form open, a click builds a date range: the first click (or a click before
   *  the current start, or a click while a multi-day range is already set) starts a fresh
   *  single-day selection; a second click on/after that start date extends the end forward --
   *  the same two-click range gesture as a typical calendar range picker. The native Start/End
   *  Date inputs stay in sync automatically since both read/write the same form controls. */
  protected onCalendarDayClick(iso: string): void {
    if (this.eventFormMode() === 'CLOSED') {
      this.onDateChange(iso);
      return;
    }
    const startCtrl = this.eventForm.get('startDate')!;
    const endCtrl = this.eventForm.get('endDate')!;
    const currentStart = startCtrl.value as string | null;
    const currentEnd = endCtrl.value as string | null;
    const isMultiDaySelection = !!(currentStart && currentEnd && currentStart !== currentEnd);
    if (!currentStart || iso < currentStart || isMultiDaySelection) {
      startCtrl.setValue(iso);
      endCtrl.setValue(iso);
    } else {
      endCtrl.setValue(iso);
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
    this.eventFormMode.set('ADD');
  }

  protected openEditEventForm(event: CalendarEvent): void {
    this.editingEventId.set(event.id);
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
    this.eventFormMode.set('EDIT');
  }

  protected cancelEventForm(): void {
    this.eventFormMode.set('CLOSED');
    this.editingEventId.set(null);
    this.overlappingEvents.set([]);
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
}

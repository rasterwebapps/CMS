import { Component, EventEmitter, Input, Output, computed, signal } from '@angular/core';
import { CmsEmptyStateComponent } from '../empty-state/empty-state.component';
import { ClassScheduleOccurrence } from '../../features/timetable/timetable.model';
import { colorForSessionType } from '../util/session-color.util';

/** Session-type-neutral period shape this component groups rows by -- duplicated locally rather
 *  than imported from the Period feature module, matching the same "shared component never
 *  imports from a feature folder" convention WeekGridSession/WeekGridCandidateCell already
 *  follow (see week-grid.model.ts). */
export interface DayAgendaPeriod {
  id: number;
  name: string;
  startTime: string;
  endTime: string;
  periodOrder: number | null;
}

interface DayAgendaRow {
  key: string;
  label: string;
  startTime: string;
  endTime: string;
  occurrences: ClassScheduleOccurrence[];
}

@Component({
  selector: 'cms-day-agenda',
  standalone: true,
  imports: [CmsEmptyStateComponent],
  templateUrl: './day-agenda.component.html',
  styleUrl: './day-agenda.component.scss',
})
export class CmsDayAgendaComponent {
  @Input({ required: true }) date!: string;

  /** Shows a per-row action affordance (e.g. "Relocate Room") when true. Off by default so
   *  existing consumers (my-timetable, a faculty's own read-only agenda) are unaffected. */
  @Input() allowRoomRelocate = false;

  private readonly _occurrences = signal<ClassScheduleOccurrence[]>([]);
  @Input() set occurrences(value: ClassScheduleOccurrence[] | null | undefined) {
    this._occurrences.set(value ?? []);
  }

  /** The full active Period master list, used to render one row per period (blank when nothing is
   *  scheduled that period) instead of only rows for periods that happen to have a session. Empty
   *  by default, which keeps grouping to "one row per distinct start/end time seen in the data"
   *  only -- existing consumers that never pass this (my-timetable) are otherwise unaffected. */
  private readonly _periods = signal<DayAgendaPeriod[]>([]);
  @Input() set periods(value: DayAgendaPeriod[] | null | undefined) {
    this._periods.set(value ?? []);
  }

  /** Emitted when the room-relocate action is used on a non-cancelled occurrence. */
  @Output() relocateRoomClick = new EventEmitter<ClassScheduleOccurrence>();

  /** One row per period (or, without a periods list, one row per distinct start/end time actually
   *  present), each carrying every concurrent occurrence in that slot so the template can stack
   *  them as separate sections instead of rendering duplicate same-time rows. */
  protected readonly rows = computed<DayAgendaRow[]>(() => {
    const periodsList = this._periods();
    const occurrences = this._occurrences();

    if (periodsList.length === 0) {
      return this.groupByTime(occurrences).sort((a, b) => a.startTime.localeCompare(b.startTime));
    }

    const knownIds = new Set(periodsList.map((p) => p.id));
    const byPeriodId = new Map<number, ClassScheduleOccurrence[]>();
    const leftover: ClassScheduleOccurrence[] = [];
    for (const occ of occurrences) {
      const periodId = occ.session.periodId;
      if (periodId != null && knownIds.has(periodId)) {
        if (!byPeriodId.has(periodId)) byPeriodId.set(periodId, []);
        byPeriodId.get(periodId)!.push(occ);
      } else {
        leftover.push(occ);
      }
    }

    // A period with no exact periodId match can still be spanned by a leftover occurrence's
    // wall-clock range (e.g. an off-campus clinical shift covering periods 1-5 by time, not by
    // periodId, since it has no single period of its own). Such a period is not "Free" -- it's
    // already represented by the leftover's own row -- so it's dropped rather than shown empty.
    const periodRows: DayAgendaRow[] = [...periodsList]
      .filter((p) => {
        const ownOccurrences = byPeriodId.get(p.id) ?? [];
        if (ownOccurrences.length > 0) return true;
        return !leftover.some((occ) => this.timeRangesOverlap(p.startTime, p.endTime, occ.session.startTime, occ.session.endTime));
      })
      .sort((a, b) => (a.periodOrder ?? 0) - (b.periodOrder ?? 0) || a.startTime.localeCompare(b.startTime))
      .map((p) => ({
        key: `period-${p.id}`,
        label: p.name,
        startTime: p.startTime,
        endTime: p.endTime,
        occurrences: (byPeriodId.get(p.id) ?? []).sort((a, b) => a.session.startTime.localeCompare(b.session.startTime)),
      }));

    return [...periodRows, ...this.groupByTime(leftover)].sort((a, b) => a.startTime.localeCompare(b.startTime));
  });

  private timeRangesOverlap(aStart: string, aEnd: string, bStart: string, bEnd: string): boolean {
    return aStart < bEnd && bStart < aEnd;
  }

  private groupByTime(occurrences: ClassScheduleOccurrence[]): DayAgendaRow[] {
    const groups = new Map<string, DayAgendaRow>();
    for (const occ of occurrences) {
      const key = `${occ.session.startTime}-${occ.session.endTime}`;
      let row = groups.get(key);
      if (!row) {
        row = {
          key,
          label: `${occ.session.startTime}–${occ.session.endTime}`,
          startTime: occ.session.startTime,
          endTime: occ.session.endTime,
          occurrences: [],
        };
        groups.set(key, row);
      }
      row.occurrences.push(occ);
    }
    return Array.from(groups.values());
  }

  /** Same primary-color-tint accent Timetable Builder/Week Grid use for every session type — see
   *  {@link colorForSessionType}. This component's data shape carries no coCurricular flag, so it
   *  always colors by session type alone. */
  protected sessionColor(sessionType: ClassScheduleOccurrence['session']['sessionType']): string {
    return colorForSessionType(sessionType);
  }
}

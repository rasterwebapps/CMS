import { Component, EventEmitter, Input, Output, computed, signal } from '@angular/core';
import { CmsEmptyStateComponent } from '../empty-state/empty-state.component';
import { ClassScheduleOccurrence } from '../../features/timetable/timetable.model';

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

  /** Emitted when the room-relocate action is used on a non-cancelled occurrence. */
  @Output() relocateRoomClick = new EventEmitter<ClassScheduleOccurrence>();

  protected readonly sortedOccurrences = computed(() =>
    [...this._occurrences()].sort((a, b) => a.session.startTime.localeCompare(b.session.startTime)));
}

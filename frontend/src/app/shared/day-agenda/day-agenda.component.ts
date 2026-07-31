import { Component, Input, computed, signal } from '@angular/core';
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

  private readonly _occurrences = signal<ClassScheduleOccurrence[]>([]);
  @Input() set occurrences(value: ClassScheduleOccurrence[] | null | undefined) {
    this._occurrences.set(value ?? []);
  }

  protected readonly sortedOccurrences = computed(() =>
    [...this._occurrences()].sort((a, b) => a.session.startTime.localeCompare(b.session.startTime)));
}

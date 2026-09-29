import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ToastService } from '../../../core/toast/toast.service';
import { violationText } from '../../../shared/util/violation-text';
import { ClassScheduleOccurrence } from '../timetable.model';
import { PeriodService } from '../../period/period.service';
import { Period } from '../../period/period.model';
import { RescheduleService } from './reschedule.service';
import { VenueCandidate } from '../room-relocation/room-relocation.model';

export interface RescheduleModalData {
  occurrence: ClassScheduleOccurrence;
  /** The occurrence's own term's bounds — the target date can never legitimately leave this
   *  range (the backend rejects it as RESCHEDULE_OUTSIDE_TERM), so the picker is constrained to
   *  match rather than letting the user pick an invalid date and only find out on Apply. */
  termStartDate: string;
  termEndDate: string;
}

/** Moves one real occurrence to a different date/period/room, never touching the recurring
 *  ClassSchedule row -- distinct from Room Relocation (room only, same date) and Staff Swap
 *  (faculty only, same date/period). Candidate rooms only load once both a target date and
 *  period are chosen, since the backend's conflict check needs both. */
@Component({
  selector: 'app-reschedule-modal',
  standalone: true,
  imports: [FormsModule, MatDialogModule, MatProgressSpinnerModule],
  templateUrl: './reschedule-modal.component.html',
  styleUrl: './reschedule-modal.component.scss',
})
export class RescheduleModalComponent {
  private readonly dialogRef = inject(MatDialogRef<RescheduleModalComponent>);
  protected readonly data: RescheduleModalData = inject(MAT_DIALOG_DATA);
  private readonly rescheduleService = inject(RescheduleService);
  private readonly periodService = inject(PeriodService);
  private readonly toast = inject(ToastService);

  protected readonly periods = signal<Period[]>([]);
  protected readonly candidates = signal<VenueCandidate[]>([]);
  protected readonly loadingCandidates = signal(false);
  protected readonly saving = signal(false);

  protected targetDate: string | null = null;
  protected selectedPeriodId: number | null = null;
  protected selectedVenueId: number | null = null;

  constructor() {
    this.periodService.getAll(true).subscribe({
      next: (list) => this.periods.set(list),
      error: () => this.toast.error('Failed to load periods'),
    });
  }

  protected get sourceDate(): string {
    return this.data.occurrence.date;
  }

  protected get dateMin(): string {
    return this.data.termStartDate;
  }

  protected get dateMax(): string {
    return this.data.termEndDate;
  }

  protected onTargetChanged(): void {
    this.selectedVenueId = null;
    this.candidates.set([]);
    if (this.targetDate && (this.targetDate < this.dateMin || this.targetDate > this.dateMax)) {
      this.toast.error("This session's term runs from "
        + `${this.dateMin} to ${this.dateMax} — pick a date within that range.`);
      this.targetDate = null;
      return;
    }
    if (!this.targetDate || !this.selectedPeriodId) return;
    this.loadingCandidates.set(true);
    this.rescheduleService.findCandidates(
      this.data.occurrence.session.id, this.sourceDate, this.targetDate, this.selectedPeriodId,
    ).subscribe({
      next: (list) => { this.candidates.set(list); this.loadingCandidates.set(false); },
      error: (err) => {
        this.toast.error(violationText(err) ?? 'Failed to load candidate rooms');
        this.loadingCandidates.set(false);
      },
    });
  }

  protected confirmReschedule(): void {
    if (!this.targetDate || !this.selectedPeriodId || !this.selectedVenueId) return;
    this.saving.set(true);
    this.rescheduleService.apply(this.data.occurrence.session.id, {
      date: this.sourceDate,
      targetDate: this.targetDate,
      periodId: this.selectedPeriodId,
      venueId: this.selectedVenueId,
    }).subscribe({
      next: () => {
        this.toast.success('Session rescheduled');
        this.saving.set(false);
        this.dialogRef.close(true);
      },
      error: (err) => {
        this.toast.error(violationText(err) ?? 'Failed to reschedule session');
        this.saving.set(false);
      },
    });
  }

  protected close(): void {
    this.dialogRef.close(false);
  }
}

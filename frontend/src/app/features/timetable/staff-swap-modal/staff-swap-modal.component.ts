import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ToastService } from '../../../core/toast/toast.service';
import { violationText } from '../../../shared/util/violation-text';
import { ClassScheduleOccurrence, StaffSwapCandidate } from '../timetable.model';
import { TimetableService } from '../timetable.service';

export interface StaffSwapModalData {
  occurrence: ClassScheduleOccurrence;
}

/** Single-date faculty swap for one occurrence -- wraps the existing Staff Session Swap
 *  candidate/apply endpoints in a modal, mirroring RoomRelocationModalComponent's shape, for use
 *  from the Class Schedules date-wise browser's row action. */
@Component({
  selector: 'app-staff-swap-modal',
  standalone: true,
  imports: [FormsModule, MatDialogModule, MatProgressSpinnerModule],
  templateUrl: './staff-swap-modal.component.html',
  styleUrl: './staff-swap-modal.component.scss',
})
export class StaffSwapModalComponent {
  private readonly dialogRef = inject(MatDialogRef<StaffSwapModalComponent>);
  protected readonly data: StaffSwapModalData = inject(MAT_DIALOG_DATA);
  private readonly timetableService = inject(TimetableService);
  private readonly toast = inject(ToastService);

  protected readonly candidates = signal<StaffSwapCandidate[]>([]);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected selectedClassScheduleId: number | null = null;

  constructor() {
    this.timetableService.getStaffSwapCandidates(this.data.occurrence.session.id, this.data.occurrence.date).subscribe({
      next: (list) => { this.candidates.set(list); this.loading.set(false); },
      error: (err) => {
        this.toast.error(violationText(err) ?? 'Failed to load swap candidates');
        this.loading.set(false);
      },
    });
  }

  protected confirmSwap(): void {
    if (!this.selectedClassScheduleId) return;
    this.saving.set(true);
    this.timetableService.applyStaffSwap(
      this.data.occurrence.session.id, this.selectedClassScheduleId, this.data.occurrence.date,
    ).subscribe({
      next: () => {
        this.toast.success('Faculty swapped for this date');
        this.saving.set(false);
        this.dialogRef.close(true);
      },
      error: (err) => {
        this.toast.error(violationText(err) ?? 'Failed to swap faculty');
        this.saving.set(false);
      },
    });
  }

  protected close(): void {
    this.dialogRef.close(false);
  }
}

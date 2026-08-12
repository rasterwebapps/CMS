import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ToastService } from '../../../core/toast/toast.service';
import { violationText } from '../../../shared/util/violation-text';
import { ClassScheduleOccurrence } from '../timetable.model';
import { RoomRelocationService } from './room-relocation.service';
import { VenueCandidate } from './room-relocation.model';

export interface RoomRelocationModalData {
  occurrence: ClassScheduleOccurrence;
}

/** BR-55 backlog — single-date room relocation. Offers only conflict-free candidate venues (the
 *  backend already filters), plus a "reset to recurring room" action that's a harmless no-op if
 *  this occurrence's SUBSTITUTED status came from a faculty swap rather than a room override. */
@Component({
  selector: 'app-room-relocation-modal',
  standalone: true,
  imports: [FormsModule, MatDialogModule, MatProgressSpinnerModule],
  templateUrl: './room-relocation-modal.component.html',
  styleUrl: './room-relocation-modal.component.scss',
})
export class RoomRelocationModalComponent {
  private readonly dialogRef = inject(MatDialogRef<RoomRelocationModalComponent>);
  protected readonly data: RoomRelocationModalData = inject(MAT_DIALOG_DATA);
  private readonly roomRelocationService = inject(RoomRelocationService);
  private readonly toast = inject(ToastService);

  protected readonly candidates = signal<VenueCandidate[]>([]);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected selectedVenueId: number | null = null;

  constructor() {
    this.roomRelocationService.findCandidates(this.data.occurrence.session.id, this.data.occurrence.date).subscribe({
      next: (list) => { this.candidates.set(list); this.loading.set(false); },
      error: (err) => {
        this.toast.error(violationText(err) ?? 'Failed to load candidate rooms');
        this.loading.set(false);
      },
    });
  }

  protected confirmRelocate(): void {
    if (!this.selectedVenueId) return;
    this.saving.set(true);
    this.roomRelocationService.relocate(this.data.occurrence.session.id, {
      date: this.data.occurrence.date,
      venueId: this.selectedVenueId,
    }).subscribe({
      next: () => {
        this.toast.success('Room relocated for this date');
        this.saving.set(false);
        this.dialogRef.close(true);
      },
      error: (err) => {
        this.toast.error(violationText(err) ?? 'Failed to relocate room');
        this.saving.set(false);
      },
    });
  }

  protected confirmRevert(): void {
    this.saving.set(true);
    this.roomRelocationService.revert(this.data.occurrence.session.id, this.data.occurrence.date).subscribe({
      next: () => {
        this.toast.success('Reset to the recurring room');
        this.saving.set(false);
        this.dialogRef.close(true);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to reset room');
        this.saving.set(false);
      },
    });
  }

  protected close(): void {
    this.dialogRef.close(false);
  }
}

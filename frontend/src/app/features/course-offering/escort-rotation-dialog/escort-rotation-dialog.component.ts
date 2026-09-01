import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { EscortCandidate, EscortRotationPool } from '../../escort-rotation/escort-rotation.model';
import { EscortRotationService } from '../../escort-rotation/escort-rotation.service';
import { ToastService } from '../../../core/toast/toast.service';

export interface EscortRotationDialogData {
  batchId: number;
  batchName: string;
}

/** Admin setup for a clinical Batch's escort-duty rotation pool (OC-175 Piece 3). Fully computed
 *  round-robin, same shape as the existing student week-parity Rotation feature -- no self-claim
 *  UI, the admin just picks the eligible pool + anchor date once. */
@Component({
  selector: 'app-escort-rotation-dialog',
  standalone: true,
  imports: [FormsModule, MatDialogModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './escort-rotation-dialog.component.html',
  styleUrl: './escort-rotation-dialog.component.scss',
})
export class EscortRotationDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<EscortRotationDialogComponent>);
  protected readonly data: EscortRotationDialogData = inject(MAT_DIALOG_DATA);
  private readonly service = inject(EscortRotationService);
  private readonly toast = inject(ToastService);

  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly candidates = signal<EscortCandidate[]>([]);
  protected readonly pool = signal<EscortRotationPool | null>(null);
  protected readonly selectedFacultyIds = signal<number[]>([]);
  protected readonly anchorDate = signal<string>(new Date().toISOString().slice(0, 10));

  constructor() {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.service.eligibleCandidates(this.data.batchId).subscribe({
      next: (candidates) => { this.candidates.set(candidates); this.checkExistingPool(); },
      error: () => { this.toast.error('Failed to load eligible faculty'); this.loading.set(false); },
    });
  }

  private checkExistingPool(): void {
    this.service.getPool(this.data.batchId).subscribe({
      next: (pool) => { this.pool.set(pool); this.loading.set(false); },
      error: () => { this.pool.set(null); this.loading.set(false); },
    });
  }

  protected toggleCandidate(facultyId: number): void {
    const current = this.selectedFacultyIds();
    this.selectedFacultyIds.set(
      current.includes(facultyId) ? current.filter((id) => id !== facultyId) : [...current, facultyId]
    );
  }

  protected isSelected(facultyId: number): boolean {
    return this.selectedFacultyIds().includes(facultyId);
  }

  protected setupPool(): void {
    if (this.selectedFacultyIds().length < 2) {
      this.toast.error('Select at least 2 faculty for a rotation pool');
      return;
    }
    this.saving.set(true);
    this.service.setupPool({
      batchId: this.data.batchId,
      anchorOccurrenceDate: this.anchorDate(),
      facultyIds: this.selectedFacultyIds(),
    }).subscribe({
      next: (pool) => {
        this.toast.success('Escort rotation pool created');
        this.pool.set(pool);
        this.saving.set(false);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to create rotation pool');
        this.saving.set(false);
      },
    });
  }

  protected deactivatePool(): void {
    this.saving.set(true);
    this.service.deactivatePool(this.data.batchId).subscribe({
      next: () => {
        this.toast.success('Escort rotation pool deactivated');
        this.pool.set(null);
        this.selectedFacultyIds.set([]);
        this.saving.set(false);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to deactivate rotation pool');
        this.saving.set(false);
      },
    });
  }

  protected onClose(): void {
    this.dialogRef.close();
  }
}

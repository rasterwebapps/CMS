import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ToastService } from '../../../../core/toast/toast.service';
import { ProgressTrackingService } from '../../progress-tracking.service';
import { UnitPickerOption } from '../../progress-tracking.model';

export interface LogSpecialClassProgressDialogData {
  occurrenceId: number;
  subjectName: string;
  subjectCode: string;
  occurrenceDate: string;
  /** The session's own duration in hours -- the default hours-covered suggestion for a fresh
   *  log's first row, mirroring LogProgressDialogComponent's own periodHours behavior. */
  periodHours: number;
}

interface CoverageRow {
  unitId: number | null;
  hoursCovered: number | null;
  markedComplete: boolean;
}

/** BR-55 counterpart of {@link LogProgressDialogComponent} for a SPECIAL_CLASS/DAY_REPEAT/
 *  RECURRING_SPECIAL_CLASS occurrence -- these already have exactly one fixed date (set at request
 *  time), so unlike the regular-session dialog there's no date picker: the occurrence id alone
 *  identifies what's being logged. A separate, dedicated component rather than overloading the
 *  regular dialog with a second mode, since the two data shapes (a date list to pick from vs. one
 *  already-fixed date) are genuinely different. */
@Component({
  selector: 'app-log-special-class-progress-dialog',
  standalone: true,
  imports: [FormsModule, MatDialogModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './log-special-class-progress-dialog.component.html',
  styleUrl: './log-special-class-progress-dialog.component.scss',
})
export class LogSpecialClassProgressDialogComponent implements OnInit {
  protected readonly dialogRef = inject(MatDialogRef<LogSpecialClassProgressDialogComponent>);
  protected readonly data: LogSpecialClassProgressDialogData = inject(MAT_DIALOG_DATA);
  private readonly service = inject(ProgressTrackingService);
  private readonly toast = inject(ToastService);

  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly availableUnits = signal<UnitPickerOption[]>([]);
  protected readonly rows = signal<CoverageRow[]>([]);
  protected readonly remarks = signal('');

  protected readonly hasNoUnits = computed(() => !this.loading() && this.availableUnits().length === 0);

  protected readonly totalHoursLogged = computed(() =>
    this.rows().reduce((sum, r) => sum + (r.hoursCovered ?? 0), 0));

  ngOnInit(): void {
    this.service.getAvailableUnitsForOccurrence(this.data.occurrenceId).subscribe({
      next: (units) => {
        this.availableUnits.set(units);
        this.loadExistingOccurrence();
      },
      error: () => {
        this.toast.error('Failed to load syllabus units for this subject');
        this.loading.set(false);
      },
    });
  }

  private loadExistingOccurrence(): void {
    this.service.getOccurrenceById(this.data.occurrenceId).subscribe({
      next: (occurrence) => {
        this.rows.set(occurrence.unitCoverages.map((c) => ({
          unitId: c.unitId,
          hoursCovered: c.hoursCovered,
          markedComplete: c.markedComplete,
        })));
        this.remarks.set(occurrence.remarks ?? '');
        this.loading.set(false);
      },
      error: () => {
        // No coverage logged yet for this occurrence -- suggest the current (first incomplete) unit.
        const current = this.availableUnits().find((u) => !u.markedComplete);
        this.rows.set(current
          ? [{ unitId: current.id, hoursCovered: this.data.periodHours, markedComplete: false }]
          : []);
        this.remarks.set('');
        this.loading.set(false);
      },
    });
  }

  /** Units selectable for a given row -- mirrors LogProgressDialogComponent's own rule. */
  protected unitOptionsFor(rowIndex: number): UnitPickerOption[] {
    const usedElsewhere = new Set(
      this.rows().filter((_, i) => i !== rowIndex).map((r) => r.unitId).filter((id) => id !== null));
    return this.availableUnits().filter((u) => !usedElsewhere.has(u.id));
  }

  protected unitLabel(unit: UnitPickerOption): string {
    const status = unit.markedComplete ? 'Complete' : unit.hoursLoggedSoFar > 0 ? `${unit.hoursLoggedSoFar}h logged` : 'Not started';
    return `Unit ${unit.unitNumber} — ${unit.title} (${status})`;
  }

  protected onRowUnitChange(rowIndex: number, unitId: string): void {
    const rows = [...this.rows()];
    rows[rowIndex] = { ...rows[rowIndex], unitId: unitId ? Number(unitId) : null };
    this.rows.set(rows);
  }

  protected onRowHoursChange(rowIndex: number, hours: string): void {
    const rows = [...this.rows()];
    rows[rowIndex] = { ...rows[rowIndex], hoursCovered: hours === '' ? null : Number(hours) };
    this.rows.set(rows);
  }

  protected onRowCompleteChange(rowIndex: number, checked: boolean): void {
    const rows = [...this.rows()];
    rows[rowIndex] = { ...rows[rowIndex], markedComplete: checked };
    this.rows.set(rows);
  }

  protected addRow(): void {
    const usedIds = new Set(this.rows().map((r) => r.unitId));
    const next = this.availableUnits().find((u) => !usedIds.has(u.id) && !u.markedComplete)
      ?? this.availableUnits().find((u) => !usedIds.has(u.id));
    this.rows.set([...this.rows(), { unitId: next?.id ?? null, hoursCovered: null, markedComplete: false }]);
  }

  protected removeRow(rowIndex: number): void {
    this.rows.set(this.rows().filter((_, i) => i !== rowIndex));
  }

  protected canAddRow(): boolean {
    return this.rows().length < this.availableUnits().length;
  }

  protected save(): void {
    const units = this.rows()
      .filter((r) => r.unitId !== null)
      .map((r) => ({ unitId: r.unitId as number, hoursCovered: r.hoursCovered, markedComplete: r.markedComplete }));

    this.saving.set(true);
    this.service.logCoverageForOccurrence(this.data.occurrenceId, {
      units,
      remarks: this.remarks() || null,
    }).subscribe({
      next: () => {
        this.toast.success('Progress logged');
        this.saving.set(false);
        this.dialogRef.close(true);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to log progress');
        this.saving.set(false);
      },
    });
  }

  protected close(): void {
    this.dialogRef.close(false);
  }
}

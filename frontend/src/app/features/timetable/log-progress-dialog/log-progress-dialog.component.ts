import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ToastService } from '../../../core/toast/toast.service';
import { ProgressTrackingService } from '../progress-tracking.service';
import { SyllabusUnitOption } from '../progress-tracking.model';
import { AttendanceComponentType } from '../../curriculum/curriculum-version.model';

const COMPONENT_TYPE_LABELS: Record<AttendanceComponentType, string> = {
  THEORY: 'Theory',
  LAB: 'Lab',
  CLINICAL: 'Clinical',
};

export interface LogProgressDialogData {
  classScheduleId: number;
  subjectName: string;
  subjectCode: string;
  termStartDate: string;
}

@Component({
  selector: 'app-log-progress-dialog',
  standalone: true,
  imports: [FormsModule, MatDialogModule, MatButtonModule, MatProgressSpinnerModule],
  templateUrl: './log-progress-dialog.component.html',
  styleUrl: './log-progress-dialog.component.scss',
})
export class LogProgressDialogComponent implements OnInit {
  protected readonly dialogRef = inject(MatDialogRef<LogProgressDialogComponent>);
  protected readonly data: LogProgressDialogData = inject(MAT_DIALOG_DATA);
  private readonly service = inject(ProgressTrackingService);
  private readonly toast = inject(ToastService);

  protected readonly componentTypeLabels = COMPONENT_TYPE_LABELS;

  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly availableUnits = signal<SyllabusUnitOption[]>([]);
  protected readonly occurrenceDates = signal<string[]>([]);
  protected readonly selectedDate = signal<string | null>(null);
  protected readonly selectedUnitIds = signal<Set<number>>(new Set());
  protected readonly remarks = signal('');

  protected readonly hasNoUnits = computed(() => !this.loading() && this.availableUnits().length === 0);
  protected readonly hasNoDates = computed(() => !this.loading() && this.occurrenceDates().length === 0);

  ngOnInit(): void {
    this.service.getAvailableUnits(this.data.classScheduleId).subscribe({
      next: (units) => this.availableUnits.set(units),
      error: () => this.toast.error('Failed to load syllabus units for this subject'),
    });
    this.service.getLoggableOccurrenceDates(this.data.classScheduleId, this.data.termStartDate).subscribe({
      next: (dates) => {
        this.occurrenceDates.set(dates);
        const lastDate = dates[dates.length - 1] ?? null;
        this.selectedDate.set(lastDate);
        if (lastDate) this.loadExistingOccurrence(lastDate);
        else this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load valid session dates');
        this.loading.set(false);
      },
    });
  }

  protected onDateChange(date: string): void {
    this.selectedDate.set(date);
    this.loadExistingOccurrence(date);
  }

  private loadExistingOccurrence(date: string): void {
    this.loading.set(true);
    this.service.getOccurrence(this.data.classScheduleId, date).subscribe({
      next: (occurrence) => {
        this.selectedUnitIds.set(new Set(occurrence.coveredUnits.map((u) => u.id)));
        this.remarks.set(occurrence.remarks ?? '');
        this.loading.set(false);
      },
      error: () => {
        // No occurrence logged yet for this date -- start from a blank selection.
        this.selectedUnitIds.set(new Set());
        this.remarks.set('');
        this.loading.set(false);
      },
    });
  }

  protected toggleUnit(unitId: number, checked: boolean): void {
    const next = new Set(this.selectedUnitIds());
    if (checked) next.add(unitId);
    else next.delete(unitId);
    this.selectedUnitIds.set(next);
  }

  protected isSelected(unitId: number): boolean {
    return this.selectedUnitIds().has(unitId);
  }

  protected save(): void {
    const date = this.selectedDate();
    if (!date) return;
    this.saving.set(true);
    this.service.logCoverage({
      classScheduleId: this.data.classScheduleId,
      occurrenceDate: date,
      unitIds: Array.from(this.selectedUnitIds()),
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

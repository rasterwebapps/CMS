import { Component, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TimetableService } from '../../timetable.service';
import { ResourceGridType } from '../../timetable.model';
import { CmsWeekGridComponent } from '../../../../shared/week-grid/week-grid.component';
import { WeekGridSession } from '../../../../shared/week-grid/week-grid.model';
import { CmsWeekNavigatorComponent } from '../../../../shared/week-navigator/week-navigator.component';
import { ToastService } from '../../../../core/toast/toast.service';

export interface ResourceWeekModalData {
  resourceType: ResourceGridType;
  resourceId: number;
  resourceName: string;
  termInstanceId: number;
}

function mondayOf(date: Date): string {
  const d = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  const day = d.getDay(); // 0=Sunday..6=Saturday
  const diff = day === 0 ? -6 : 1 - day;
  d.setDate(d.getDate() + diff);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${dd}`;
}

/** Drilled into from one row of the Resource Timetable's daily grid — this resource's own full
 *  Mon-Sat week, across every cohort (not just whichever single cohort the main Timetable screen's
 *  own Faculty/Room filter happens to be scoped to). Reuses the same shared cms-week-grid the
 *  cohort Timetable screen renders its Week/Date-wise views with, fed a resource-scoped session
 *  list instead of a cohort-scoped one. */
@Component({
  selector: 'app-resource-week-modal',
  standalone: true,
  imports: [MatDialogModule, MatProgressSpinnerModule, CmsWeekGridComponent, CmsWeekNavigatorComponent],
  templateUrl: './resource-week-modal.component.html',
  styleUrl: './resource-week-modal.component.scss',
})
export class ResourceWeekModalComponent {
  protected readonly data: ResourceWeekModalData = inject(MAT_DIALOG_DATA);
  private readonly timetableService = inject(TimetableService);
  private readonly toast = inject(ToastService);

  protected readonly viewMode = signal<'DATE' | 'WEEKDAY'>('WEEKDAY');
  protected readonly weekStart = signal(mondayOf(new Date()));
  protected readonly loading = signal(false);
  protected readonly sessions = signal<WeekGridSession[]>([]);

  constructor() {
    this.load();
  }

  protected setViewMode(mode: 'DATE' | 'WEEKDAY'): void {
    if (this.viewMode() === mode) return;
    this.viewMode.set(mode);
    this.load();
  }

  protected onWeekStartChange(weekStart: string): void {
    this.weekStart.set(weekStart);
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    const weekStart = this.viewMode() === 'DATE' ? this.weekStart() : undefined;
    this.timetableService.getResourceWeekGrid(this.data.resourceType, this.data.resourceId, this.data.termInstanceId, weekStart)
      .subscribe({
        next: (cells) => {
          this.sessions.set(cells.map((c): WeekGridSession => ({
            id: c.sessionId,
            sessionType: c.sessionType,
            status: c.status,
            subjectName: c.subjectName,
            subjectCode: c.subjectCode,
            facultyName: c.facultyName,
            roomName: c.roomName,
            batchName: c.batchName,
            dayOfWeek: c.dayOfWeek,
            periodId: c.periodId,
            startTime: c.startTime,
            endTime: c.endTime,
            slotName: c.slotName,
          })));
          this.loading.set(false);
        },
        error: () => { this.toast.error('Failed to load this resource\'s week'); this.loading.set(false); },
      });
  }
}

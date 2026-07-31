import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { FacultyService } from '../faculty/faculty.service';
import { Faculty } from '../faculty/faculty.model';
import { PeriodService } from '../period/period.service';
import { Period } from '../period/period.model';
import { FacultyAvailabilityService } from './faculty-availability.service';
import { FacultyAvailabilityBlock } from './faculty-availability.model';
import { WEEK_GRID_DAYS, WEEK_GRID_DAY_LABELS } from '../../shared/week-grid/week-grid.model';
import { PermissionService } from '../../core/permissions/permission.service';
import { ToastService } from '../../core/toast/toast.service';

interface AvailabilityRow {
  key: string;
  label: string;
  startTime: string;
  endTime: string;
}

@Component({
  selector: 'app-faculty-availability',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './faculty-availability.component.html',
  styleUrl: './faculty-availability.component.scss',
})
export class FacultyAvailabilityComponent implements OnInit {
  private readonly facultyService = inject(FacultyService);
  private readonly periodService = inject(PeriodService);
  private readonly facultyAvailabilityService = inject(FacultyAvailabilityService);
  private readonly permissionService = inject(PermissionService);
  private readonly toast = inject(ToastService);

  protected readonly faculties = signal<Faculty[]>([]);
  protected readonly periods = signal<Period[]>([]);
  protected readonly blocks = signal<FacultyAvailabilityBlock[]>([]);
  protected readonly loading = signal(false);
  protected readonly toggling = signal<string | null>(null);

  protected selectedFacultyId: number | null = null;

  protected readonly days = WEEK_GRID_DAYS;
  protected readonly dayLabels = WEEK_GRID_DAY_LABELS;

  protected readonly periodRows = computed<AvailabilityRow[]>(() =>
    this.periods().map((p) => ({ key: `period-${p.id}`, label: p.name, startTime: p.startTime, endTime: p.endTime })));

  protected canManage(): boolean {
    return this.permissionService.has('FACULTY_AVAILABILITY_MANAGE');
  }

  ngOnInit(): void {
    this.facultyService.getAll().subscribe({
      next: (faculties) => {
        this.faculties.set(faculties);
        if (faculties.length > 0) {
          this.selectedFacultyId = faculties[0].id;
          this.onFacultyChange();
        }
      },
      error: () => this.toast.error('Failed to load faculty list'),
    });
    this.periodService.getAll(true).subscribe({ next: (periods) => this.periods.set(periods) });
  }

  protected onFacultyChange(): void {
    if (!this.selectedFacultyId) {
      this.blocks.set([]);
      return;
    }
    this.loading.set(true);
    this.facultyAvailabilityService.getForFaculty(this.selectedFacultyId).subscribe({
      next: (blocks) => { this.blocks.set(blocks); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load availability'); this.loading.set(false); },
    });
  }

  protected blockFor(day: string, row: AvailabilityRow): FacultyAvailabilityBlock | undefined {
    return this.blocks().find((b) => b.dayOfWeek === day && b.startTime === row.startTime && b.endTime === row.endTime);
  }

  protected toggleCell(day: string, row: AvailabilityRow): void {
    if (!this.selectedFacultyId || !this.canManage()) return;
    const cellKey = `${row.key}-${day}`;
    const existing = this.blockFor(day, row);
    this.toggling.set(cellKey);
    if (existing) {
      this.facultyAvailabilityService.removeBlock(existing.id).subscribe({
        next: () => {
          this.blocks.update((list) => list.filter((b) => b.id !== existing.id));
          this.toggling.set(null);
        },
        error: (err) => {
          this.toast.error(err?.error?.message ?? 'Failed to clear block');
          this.toggling.set(null);
        },
      });
    } else {
      this.facultyAvailabilityService.addBlock({
        facultyId: this.selectedFacultyId,
        dayOfWeek: day,
        startTime: row.startTime,
        endTime: row.endTime,
      }).subscribe({
        next: (block) => {
          this.blocks.update((list) => [...list, block]);
          this.toggling.set(null);
        },
        error: (err) => {
          this.toast.error(err?.error?.message ?? 'Failed to mark unavailable');
          this.toggling.set(null);
        },
      });
    }
  }
}

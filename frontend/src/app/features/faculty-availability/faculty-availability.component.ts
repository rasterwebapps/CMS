import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FacultyService } from '../faculty/faculty.service';
import { Faculty } from '../faculty/faculty.model';
import { PeriodService } from '../period/period.service';
import { Period } from '../period/period.model';
import { FacultyAvailabilityService } from './faculty-availability.service';
import { FacultyAvailabilityBlock } from './faculty-availability.model';
import { WEEK_GRID_DAYS, WEEK_GRID_DAY_LABELS } from '../../shared/week-grid/week-grid.model';
import { PermissionService } from '../../core/permissions/permission.service';
import { ToastService } from '../../core/toast/toast.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog/confirm-dialog.component';
import { BlockAvailabilityDialogComponent, BlockAvailabilityDialogData } from './block-availability-dialog/block-availability-dialog.component';

interface AvailabilityRow {
  key: string;
  label: string;
  startTime: string;
  endTime: string;
}

@Component({
  selector: 'app-faculty-availability',
  standalone: true,
  imports: [FormsModule, MatDialogModule, MatTooltipModule],
  templateUrl: './faculty-availability.component.html',
  styleUrl: './faculty-availability.component.scss',
})
export class FacultyAvailabilityComponent implements OnInit {
  private readonly facultyService = inject(FacultyService);
  private readonly periodService = inject(PeriodService);
  private readonly facultyAvailabilityService = inject(FacultyAvailabilityService);
  private readonly permissionService = inject(PermissionService);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);

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

  protected cellTooltip(day: string, row: AvailabilityRow): string {
    const block = this.blockFor(day, row);
    if (!block) return `Mark ${this.dayLabels[day]} ${row.label} as blocked`;
    return block.reason ? `Blocked: ${block.reason}` : 'Blocked (no reason given)';
  }

  protected toggleCell(day: string, row: AvailabilityRow): void {
    if (!this.selectedFacultyId || !this.canManage()) return;
    const existing = this.blockFor(day, row);
    if (existing) {
      this.confirmUnblock(day, row, existing);
    } else {
      this.confirmBlock(day, row);
    }
  }

  private confirmBlock(day: string, row: AvailabilityRow): void {
    const facultyName = this.faculties().find((f) => f.id === this.selectedFacultyId)?.fullName ?? 'This faculty member';
    const data: BlockAvailabilityDialogData = {
      facultyName, dayLabel: this.dayLabels[day], periodLabel: row.label,
    };
    this.dialog.open(BlockAvailabilityDialogComponent, { data, width: '420px' })
      .afterClosed().subscribe((reason: string | null) => {
        if (reason) this.doAddBlock(day, row, reason);
      });
  }

  private confirmUnblock(day: string, row: AvailabilityRow, existing: FacultyAvailabilityBlock): void {
    const facultyName = this.faculties().find((f) => f.id === this.selectedFacultyId)?.fullName ?? 'This faculty member';
    const reasonNote = existing.reason ? ` It was originally blocked because: "${existing.reason}"` : '';
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Unblock This Period',
        message: `Mark ${facultyName} as available again for ${this.dayLabels[day]}, ${row.label}?${reasonNote}`,
        confirmText: 'Unblock',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) this.doRemoveBlock(day, row, existing);
    });
  }

  private doAddBlock(day: string, row: AvailabilityRow, reason: string): void {
    if (!this.selectedFacultyId) return;
    const cellKey = `${row.key}-${day}`;
    this.toggling.set(cellKey);
    this.facultyAvailabilityService.addBlock({
      facultyId: this.selectedFacultyId,
      dayOfWeek: day,
      startTime: row.startTime,
      endTime: row.endTime,
      reason,
    }).subscribe({
      next: (block) => {
        this.blocks.update((list) => [...list, block]);
        this.toggling.set(null);
      },
      error: (err) => {
        // Hard-block conflict messages list every affected class -- can run long, so keep the
        // toast open until dismissed rather than the default 6s auto-dismiss.
        this.toast.error(err?.error?.message ?? 'Failed to mark unavailable', { durationMs: 0 });
        this.toggling.set(null);
      },
    });
  }

  private doRemoveBlock(day: string, row: AvailabilityRow, existing: FacultyAvailabilityBlock): void {
    const cellKey = `${row.key}-${day}`;
    this.toggling.set(cellKey);
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
  }
}

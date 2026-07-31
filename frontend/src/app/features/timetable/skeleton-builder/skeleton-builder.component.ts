import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear, CourseOffering, TermInstance } from '../../academic-year/academic-year.model';
import { PeriodService } from '../../period/period.service';
import { Period } from '../../period/period.model';
import { SkeletonBuilderService } from './skeleton-builder.service';
import { SkeletonBuilderResponse, SkeletonCell, SkeletonSessionType } from './skeleton-builder.model';
import { WEEK_GRID_DAYS, WEEK_GRID_DAY_LABELS } from '../../../shared/week-grid/week-grid.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';

@Component({
  selector: 'app-skeleton-builder',
  standalone: true,
  imports: [FormsModule, MatDialogModule, MatProgressSpinnerModule],
  templateUrl: './skeleton-builder.component.html',
  styleUrl: './skeleton-builder.component.scss',
})
export class SkeletonBuilderComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly periodService = inject(PeriodService);
  private readonly skeletonService = inject(SkeletonBuilderService);
  private readonly permissionService = inject(PermissionService);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);

  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly termInstances = signal<TermInstance[]>([]);
  protected readonly offerings = signal<CourseOffering[]>([]);
  protected readonly periods = signal<Period[]>([]);
  protected readonly skeleton = signal<SkeletonBuilderResponse | null>(null);

  protected readonly termsLoading = signal(false);
  protected readonly offeringsLoading = signal(false);
  protected readonly skeletonLoading = signal(false);
  protected readonly placing = signal(false);

  protected selectedAcademicYearId: number | null = null;
  protected selectedTermInstanceId: number | null = null;
  protected selectedOfferingId: number | null = null;

  protected readonly days = WEEK_GRID_DAYS;
  protected readonly dayLabels = WEEK_GRID_DAY_LABELS;

  protected readonly selectedCell = signal<{ day: string; periodId: number } | null>(null);
  protected selectedSessionType: SkeletonSessionType = 'THEORY';
  protected selectedBatchId: number | null = null;

  protected readonly needsBatch = computed(() => this.selectedSessionType !== 'THEORY');
  protected readonly selectedPeriod = computed(() => {
    const cell = this.selectedCell();
    if (!cell) return null;
    return this.periods().find((p) => p.id === cell.periodId) ?? null;
  });

  protected canManage(): boolean {
    return this.permissionService.has('TIMETABLE_SKELETON_MANAGE');
  }

  ngOnInit(): void {
    this.periodService.getAll(true).subscribe({
      next: (data) => this.periods.set(data),
      error: () => this.toast.error('Failed to load periods'),
    });
    this.academicYearService.getAllAcademicYears().subscribe({
      next: (years) => {
        this.academicYears.set(years);
        const initialYearId = years.find((y) => y.isCurrent)?.id ?? years[0]?.id ?? null;
        if (initialYearId) {
          this.selectedAcademicYearId = initialYearId;
          this.loadTermInstances(initialYearId);
        }
      },
      error: () => this.toast.error('Failed to load academic years'),
    });
  }

  protected onAcademicYearChange(): void {
    this.selectedTermInstanceId = null;
    this.offerings.set([]);
    this.selectedOfferingId = null;
    this.skeleton.set(null);
    if (this.selectedAcademicYearId) this.loadTermInstances(this.selectedAcademicYearId);
  }

  protected onTermChange(): void {
    this.selectedOfferingId = null;
    this.skeleton.set(null);
    if (this.selectedTermInstanceId) this.loadOfferings(this.selectedTermInstanceId);
    else this.offerings.set([]);
  }

  protected onOfferingChange(): void {
    this.cancelPlacement();
    if (this.selectedOfferingId) this.loadSkeleton(this.selectedOfferingId);
    else this.skeleton.set(null);
  }

  private loadTermInstances(academicYearId: number): void {
    this.termsLoading.set(true);
    this.academicYearService.getTermInstancesByAcademicYear(academicYearId).subscribe({
      next: (terms) => {
        this.termInstances.set(terms);
        this.termsLoading.set(false);
        this.selectedTermInstanceId = terms[0]?.id ?? null;
        if (this.selectedTermInstanceId) this.loadOfferings(this.selectedTermInstanceId);
      },
      error: () => { this.toast.error('Failed to load term instances'); this.termsLoading.set(false); },
    });
  }

  private loadOfferings(termInstanceId: number): void {
    this.offeringsLoading.set(true);
    this.academicYearService.getCourseOfferingsByTermInstance(termInstanceId).subscribe({
      next: (offerings) => {
        this.offerings.set(offerings.filter((o) => !o.isElective));
        this.offeringsLoading.set(false);
        this.selectedOfferingId = this.offerings()[0]?.id ?? null;
        if (this.selectedOfferingId) this.loadSkeleton(this.selectedOfferingId);
        else this.skeleton.set(null);
      },
      error: () => { this.toast.error('Failed to load course offerings'); this.offeringsLoading.set(false); },
    });
  }

  private loadSkeleton(courseOfferingId: number): void {
    this.skeletonLoading.set(true);
    this.cancelPlacement();
    this.skeletonService.getSkeleton(courseOfferingId).subscribe({
      next: (data) => { this.skeleton.set(data); this.skeletonLoading.set(false); },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to load skeleton');
        this.skeleton.set(null);
        this.skeletonLoading.set(false);
      },
    });
  }

  protected cellFor(day: string, periodId: number): SkeletonCell | undefined {
    return this.skeleton()?.cells.find((c) => c.dayOfWeek === day && c.periodId === periodId);
  }

  protected onCellClick(day: string, periodId: number): void {
    if (!this.canManage()) return;
    const existing = this.cellFor(day, periodId);
    if (existing) {
      if (existing.isStaffed) return; // read-only once staffed -- edit via Class Schedule screen
      this.confirmRemove(existing);
      return;
    }
    this.selectedCell.set({ day, periodId });
    this.selectedSessionType = 'THEORY';
    this.selectedBatchId = null;
  }

  protected cancelPlacement(): void {
    this.selectedCell.set(null);
  }

  protected confirmPlacement(): void {
    const cell = this.selectedCell();
    const offeringId = this.selectedOfferingId;
    if (!cell || !offeringId) return;
    if (this.needsBatch() && !this.selectedBatchId) {
      this.toast.error('A batch is required for a Lab or Clinical session');
      return;
    }
    this.placing.set(true);
    this.skeletonService.placeCell({
      courseOfferingId: offeringId,
      sessionType: this.selectedSessionType,
      dayOfWeek: cell.day,
      periodId: cell.periodId,
      batchId: this.selectedBatchId,
    }).subscribe({
      next: () => {
        this.toast.success('Placed');
        this.placing.set(false);
        this.cancelPlacement();
        this.loadSkeleton(offeringId);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to place session');
        this.placing.set(false);
      },
    });
  }

  private confirmRemove(cell: SkeletonCell): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Remove Placement',
        message: `Remove the ${cell.sessionType} session placed on ${this.dayLabels[cell.dayOfWeek]}, ${cell.slotName}?`,
        confirmText: 'Remove',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) this.doRemove(cell.id);
    });
  }

  private doRemove(cellId: number): void {
    const offeringId = this.selectedOfferingId;
    if (!offeringId) return;
    this.skeletonService.removeCell(cellId).subscribe({
      next: () => { this.toast.success('Removed'); this.loadSkeleton(offeringId); },
      error: (err) => this.toast.error(err?.error?.message ?? 'Failed to remove placement'),
    });
  }
}

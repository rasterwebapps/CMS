import { Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear, CohortSummary, CourseOffering, TermInstance } from '../../academic-year/academic-year.model';
import { CapacityPlannerService } from './capacity-planner.service';
import { CapacityPlan } from './capacity-planner.model';
import { BatchService } from '../../batch/batch.service';
import { CmsCapacityMeterComponent } from '../../../shared/capacity-meter/capacity-meter.component';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';
import { PortionBlueprintService } from '../portion-blueprint.service';
import { PortionShortfall } from '../portion-blueprint.model';

@Component({
  selector: 'app-capacity-planner',
  standalone: true,
  imports: [FormsModule, MatProgressSpinnerModule, RouterLink, CmsCapacityMeterComponent, DecimalPipe],
  templateUrl: './capacity-planner.component.html',
  styleUrl: './capacity-planner.component.scss',
})
export class CapacityPlannerComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly capacityPlannerService = inject(CapacityPlannerService);
  private readonly batchService = inject(BatchService);
  private readonly permissionService = inject(PermissionService);
  private readonly toast = inject(ToastService);
  private readonly portionBlueprintService = inject(PortionBlueprintService);

  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly termInstances = signal<TermInstance[]>([]);
  protected readonly cohorts = signal<CohortSummary[]>([]);
  protected readonly offerings = signal<CourseOffering[]>([]);
  protected readonly plan = signal<CapacityPlan | null>(null);

  protected readonly loading = signal(false);
  protected readonly creatingBatches = signal(false);

  // ─── Portion-completion shortfall (reuses this screen's own bufferHours) ───
  protected readonly shortfall = signal<PortionShortfall | null>(null);
  protected readonly generatingBlueprint = signal(false);

  protected canViewShortfall(): boolean {
    return this.permissionService.has('PROGRESS_REPORT_VIEW');
  }

  protected canManageBlueprint(): boolean {
    return this.permissionService.has('PORTION_BLUEPRINT_MANAGE');
  }

  protected selectedAcademicYearId: number | null = null;
  protected selectedTermInstanceId: number | null = null;
  protected selectedCohortId: number | null = null;
  protected targetBatchSize: number | null = null;
  protected selectedOfferingId: number | null = null;

  protected canCreateBatches(): boolean {
    return this.permissionService.has('TIMETABLE_CAPACITY_PLANNER_BATCH_CREATE');
  }

  ngOnInit(): void {
    this.academicYearService.getAllAcademicYears().subscribe({
      next: (years) => {
        this.academicYears.set(years);
        const initialYearId = years.find((y) => y.isCurrent)?.id ?? years[0]?.id ?? null;
        if (initialYearId) {
          this.selectedAcademicYearId = initialYearId;
          this.loadTermInstances(initialYearId);
          this.loadCohorts(initialYearId);
        }
      },
      error: () => this.toast.error('Failed to load academic years'),
    });
  }

  protected onAcademicYearChange(): void {
    this.selectedTermInstanceId = null;
    this.selectedCohortId = null;
    this.offerings.set([]);
    this.plan.set(null);
    this.shortfall.set(null);
    if (this.selectedAcademicYearId) {
      this.loadTermInstances(this.selectedAcademicYearId);
      this.loadCohorts(this.selectedAcademicYearId);
    }
  }

  protected onTermChange(): void {
    this.selectedOfferingId = null;
    this.offerings.set([]);
    this.plan.set(null);
    this.shortfall.set(null);
  }

  protected onCohortChange(): void {
    this.selectedOfferingId = null;
    this.offerings.set([]);
    this.plan.set(null);
    this.shortfall.set(null);
  }

  private loadTermInstances(academicYearId: number): void {
    this.academicYearService.getTermInstancesByAcademicYear(academicYearId).subscribe({
      next: (terms) => {
        this.termInstances.set(terms);
        this.selectedTermInstanceId = terms[0]?.id ?? null;
      },
      error: () => this.toast.error('Failed to load term instances'),
    });
  }

  private loadCohorts(academicYearId: number): void {
    this.academicYearService.getAllCohorts().subscribe({
      next: (cohorts) => {
        this.cohorts.set(cohorts);
        this.selectedCohortId = cohorts[0]?.id ?? null;
      },
      error: () => this.toast.error('Failed to load cohorts'),
    });
  }

  /** Scoped to the plan's own semester (a cohort's enrolled students all share one semester in a
   *  given term) — the shared TermInstance otherwise packs every concurrent year's subjects
   *  together, which would dump every year's offerings into one dropdown. Falls back to
   *  unfiltered only if the cohort has no enrollment yet to derive a semester from. */
  private loadOfferings(termInstanceId: number, semesterNumber: number | null): void {
    this.academicYearService.getCourseOfferingsByTermInstance(termInstanceId, semesterNumber ?? undefined).subscribe({
      next: (offerings) => {
        this.offerings.set(offerings.filter((o) => !o.isElective));
        this.selectedOfferingId = this.offerings()[0]?.id ?? null;
      },
      error: () => this.toast.error('Failed to load course offerings'),
    });
  }

  protected loadPlan(): void {
    if (!this.selectedTermInstanceId || !this.selectedCohortId) return;
    this.loading.set(true);
    this.shortfall.set(null);
    const termInstanceId = this.selectedTermInstanceId;
    const cohortId = this.selectedCohortId;
    this.capacityPlannerService.getPlan(termInstanceId, cohortId, this.targetBatchSize)
      .subscribe({
        next: (data) => {
          this.plan.set(data);
          this.loading.set(false);
          this.loadOfferings(data.termInstanceId, data.semesterNumber);
          if (this.canViewShortfall()) this.loadShortfall(termInstanceId, cohortId);
        },
        error: (err) => {
          this.toast.error(err?.error?.message ?? 'Failed to load capacity plan');
          this.plan.set(null);
          this.loading.set(false);
        },
      });
  }

  private loadShortfall(termInstanceId: number, cohortId: number): void {
    this.portionBlueprintService.checkShortfall(termInstanceId, cohortId).subscribe({
      next: (data) => this.shortfall.set(data),
      // Quiet failure -- the shortfall banner is a bonus signal, not core to this screen's purpose.
      error: () => this.shortfall.set(null),
    });
  }

  protected generateBlueprint(): void {
    if (!this.selectedOfferingId) return;
    const offeringId = this.selectedOfferingId;
    this.generatingBlueprint.set(true);
    this.portionBlueprintService.generateBlueprint(offeringId).subscribe({
      next: () => {
        this.toast.success('Blueprint generated');
        this.generatingBlueprint.set(false);
        if (this.selectedTermInstanceId && this.selectedCohortId) {
          this.loadShortfall(this.selectedTermInstanceId, this.selectedCohortId);
        }
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to generate blueprint');
        this.generatingBlueprint.set(false);
      },
    });
  }

  protected createSuggestedBatches(): void {
    const plan = this.plan();
    if (!plan || !this.selectedOfferingId || plan.labBatchesNeeded <= 0) return;
    this.creatingBatches.set(true);
    this.batchService.autoCreate({
      courseOfferingId: this.selectedOfferingId,
      count: plan.labBatchesNeeded,
      capacity: plan.targetBatchSize,
    }).subscribe({
      next: (created) => {
        this.toast.success(`Created ${created.length} batch(es)`);
        this.creatingBatches.set(false);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to create batches');
        this.creatingBatches.set(false);
      },
    });
  }
}

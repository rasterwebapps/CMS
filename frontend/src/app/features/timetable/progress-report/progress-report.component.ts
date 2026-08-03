import { Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { PermissionService } from '../../../core/permissions/permission.service';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear, TermInstance } from '../../academic-year/academic-year.model';
import { ToastService } from '../../../core/toast/toast.service';
import { ProgressTrackingService } from '../progress-tracking.service';
import { OfferingProgress, TermProgressSummary } from '../progress-tracking.model';
import { PortionBlueprintService } from '../portion-blueprint.service';
import { UnitVariance } from '../portion-blueprint.model';

@Component({
  selector: 'app-progress-report',
  standalone: true,
  imports: [FormsModule, MatProgressSpinnerModule, MatTooltipModule, DecimalPipe],
  templateUrl: './progress-report.component.html',
  styleUrl: './progress-report.component.scss',
})
export class ProgressReportComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly progressService = inject(ProgressTrackingService);
  private readonly portionBlueprintService = inject(PortionBlueprintService);
  private readonly permissionService = inject(PermissionService);
  private readonly toast = inject(ToastService);

  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly termInstances = signal<TermInstance[]>([]);
  protected readonly summary = signal<TermProgressSummary | null>(null);
  protected readonly loading = signal(false);
  protected readonly termsLoading = signal(false);

  protected readonly expandedOfferingId = signal<number | null>(null);
  protected readonly offeringDetail = signal<OfferingProgress | null>(null);
  protected readonly detailLoading = signal(false);

  // ─── Portion-completion blueprint (Planned vs Projected/Actual) ───
  // This screen is reachable only via PROGRESS_REPORT_VIEW, an admin/HOD-tier permission (see
  // V323's seeding) -- faculty never reach this route at all, so the PORTION_PLAN_VISIBLE_TO_FACULTY
  // config toggle has nothing to gate here; it applies to a faculty-facing surface (e.g. the Log
  // Progress dialog) that isn't wired up yet. Admin/HOD always see the full variance, per plan.
  protected readonly variance = signal<UnitVariance[]>([]);
  protected readonly varianceLoading = signal(false);
  protected readonly generatingBlueprint = signal(false);

  protected canManageBlueprint(): boolean {
    return this.permissionService.has('PORTION_BLUEPRINT_MANAGE');
  }

  protected selectedAcademicYearId: number | null = null;
  protected selectedTermInstanceId: number | null = null;

  ngOnInit(): void {
    this.academicYearService.getAllAcademicYears().subscribe({
      next: (years) => {
        this.academicYears.set(years);
        const initialYearId = years.find((y) => y.isCurrent)?.id ?? years[0]?.id ?? null;
        if (initialYearId) {
          this.selectedAcademicYearId = initialYearId;
          this.loadTermInstances(initialYearId);
        }
      },
      error: () => { this.toast.error('Failed to load academic years'); },
    });
  }

  protected onAcademicYearChange(): void {
    this.selectedTermInstanceId = null;
    this.summary.set(null);
    if (this.selectedAcademicYearId) this.loadTermInstances(this.selectedAcademicYearId);
  }

  protected onTermChange(): void {
    this.load();
  }

  private loadTermInstances(academicYearId: number): void {
    this.termsLoading.set(true);
    this.academicYearService.getTermInstancesByAcademicYear(academicYearId).subscribe({
      next: (terms) => {
        this.termInstances.set(terms);
        this.termsLoading.set(false);
        this.selectedTermInstanceId = terms[0]?.id ?? null;
        this.load();
      },
      error: () => { this.toast.error('Failed to load term instances'); this.termsLoading.set(false); },
    });
  }

  private load(): void {
    if (!this.selectedTermInstanceId) { this.summary.set(null); return; }
    this.loading.set(true);
    this.expandedOfferingId.set(null);
    this.progressService.getTermProgressSummary(this.selectedTermInstanceId).subscribe({
      next: (data) => {
        this.summary.set(data);
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load progress summary'); this.loading.set(false); },
    });
  }

  protected toggleExpand(courseOfferingId: number): void {
    if (this.expandedOfferingId() === courseOfferingId) {
      this.expandedOfferingId.set(null);
      this.offeringDetail.set(null);
      this.variance.set([]);
      return;
    }
    this.expandedOfferingId.set(courseOfferingId);
    this.detailLoading.set(true);
    this.progressService.getOfferingProgress(courseOfferingId).subscribe({
      next: (detail) => {
        this.offeringDetail.set(detail);
        this.detailLoading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load subject detail');
        this.detailLoading.set(false);
      },
    });
    this.loadVariance(courseOfferingId);
  }

  private loadVariance(courseOfferingId: number): void {
    this.varianceLoading.set(true);
    this.portionBlueprintService.getProjection(courseOfferingId).subscribe({
      next: (data) => {
        this.variance.set(data);
        this.varianceLoading.set(false);
      },
      error: () => {
        // No blueprint generated yet is a normal, expected state -- fail quiet, not an error toast.
        this.variance.set([]);
        this.varianceLoading.set(false);
      },
    });
  }

  protected varianceFor(unitId: number): UnitVariance | null {
    return this.variance().find((v) => v.unitId === unitId) ?? null;
  }

  protected generateBlueprint(courseOfferingId: number): void {
    if (!confirm(
      'Generate (or regenerate) the portion-completion blueprint for this subject? ' +
      'This freezes new planned completion dates from the current timetable, replacing any earlier blueprint.',
    )) return;
    this.generatingBlueprint.set(true);
    this.portionBlueprintService.generateBlueprint(courseOfferingId).subscribe({
      next: () => {
        this.toast.success('Blueprint generated');
        this.generatingBlueprint.set(false);
        this.loadVariance(courseOfferingId);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to generate blueprint');
        this.generatingBlueprint.set(false);
      },
    });
  }
}

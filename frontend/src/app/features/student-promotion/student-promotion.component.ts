import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CmsEmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { ToastService } from '../../core/toast/toast.service';
import { PermissionService } from '../../core/permissions/permission.service';
import { AcademicYearService } from '../academic-year/academic-year.service';
import { AcademicYear, CohortSummary, TermInstance } from '../academic-year/academic-year.model';
import { StudentPromotionService } from './student-promotion.service';
import {
  CohortTermOption,
  PromotionDecisionInput,
  PromotionEditableRow,
  PromotionExecuteResponse,
  PromotionOutcome,
  PromotionPreviewResponse,
} from './student-promotion.model';
import { TourService } from '../../shared/tour/tour.service';
import { CmsTourButtonComponent } from '../../shared/tour/tour-button.component';
import { STUDENT_PROMOTION_TOUR, STUDENT_PROMOTION_FLOW_MAP } from '../../shared/tour/tours/student-promotion.tours';

type Step = 'select' | 'preview' | 'result';

const OUTCOME_LABELS: Record<PromotionOutcome, string> = {
  PROMOTED: 'Promote',
  PROMOTED_WITH_ARREARS: 'Promote (with arrears)',
  DETAINED_REPEAT: 'Detain — repeat year',
  GRADUATED: 'Graduate',
  EXCLUDED: 'Exclude (skip this cycle)',
};

const BLOCK_REASON_LABELS: Record<string, string> = {
  ARREARS_AT_FINAL_YEAR_GATE: 'Unresolved arrears — cannot enter/complete Final Year',
  MAX_DURATION_EXCEEDED: 'Exceeds maximum permitted duration for this program',
};

@Component({
  selector: 'app-student-promotion',
  standalone: true,
  imports: [FormsModule, MatTableModule, MatTooltipModule, CmsEmptyStateComponent, CmsTourButtonComponent],
  templateUrl: './student-promotion.component.html',
  styleUrl: './student-promotion.component.scss',
})
export class StudentPromotionComponent implements OnInit {
  private readonly academicYearSvc = inject(AcademicYearService);
  private readonly promotionSvc = inject(StudentPromotionService);
  private readonly toast = inject(ToastService);
  private readonly tourService = inject(TourService);
  protected readonly permissions = inject(PermissionService);

  protected readonly outcomeLabels = OUTCOME_LABELS;
  protected readonly outcomeOptions: PromotionOutcome[] =
    ['PROMOTED', 'PROMOTED_WITH_ARREARS', 'GRADUATED', 'DETAINED_REPEAT', 'EXCLUDED'];

  protected readonly step = signal<Step>('select');
  protected readonly loadingPreview = signal(false);
  protected readonly executing = signal(false);

  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly cohorts = signal<CohortSummary[]>([]);
  protected readonly fromTermInstances = signal<TermInstance[]>([]);
  protected readonly toTermInstances = signal<TermInstance[]>([]);

  // Streamlined path: pick a cohort, the system detects which term(s) it currently has ENROLLED
  // students in and suggests the destination — no academic-year cascade needed for the common
  // case (a cohort has exactly one active term). The full manual cascade below stays available
  // as a fallback for edge cases (a brand-new cohort with no enrollment yet, or a non-standard
  // rollover) via `showAdvanced`.
  protected readonly loadingActiveTerms = signal(false);
  protected readonly activeTermOptions = signal<CohortTermOption[]>([]);
  protected readonly suggestedToTerm = signal<CohortTermOption | null>(null);
  protected readonly showAdvanced = signal(false);

  protected selectedCohortId: number | null = null;
  protected selectedFromAcademicYearId: number | null = null;
  protected selectedFromTermInstanceId: number | null = null;
  protected selectedToAcademicYearId: number | null = null;
  protected selectedToTermInstanceId: number | null = null;

  protected generateCourseRegistrations = true;
  protected generateFeeDemands = true;

  protected readonly preview = signal<PromotionPreviewResponse | null>(null);
  protected readonly rows = signal<PromotionEditableRow[]>([]);
  protected readonly executeResult = signal<PromotionExecuteResponse | null>(null);

  protected readonly displayedColumns = ['student', 'attendance', 'arrears', 'block', 'outcome', 'remarks'];

  protected readonly readyForPreview = computed(() =>
    !!this.selectedCohortId && !!this.selectedFromTermInstanceId && !!this.selectedToTermInstanceId);

  ngOnInit(): void {
    this.tourService.register('student-promotion', STUDENT_PROMOTION_TOUR);
    this.tourService.registerFlowMap('student-promotion', STUDENT_PROMOTION_FLOW_MAP);

    this.academicYearSvc.getAllAcademicYears().subscribe({
      next: (years) => {
        const sorted = [...years].sort((a, b) =>
          new Date(b.startDate).getTime() - new Date(a.startDate).getTime());
        this.academicYears.set(sorted);
      },
    });
    this.academicYearSvc.getAllCohorts().subscribe({ next: (c) => this.cohorts.set(c) });
  }

  protected onCohortChange(): void {
    this.resetTermSelection();
    if (!this.selectedCohortId) return;

    this.loadingActiveTerms.set(true);
    this.promotionSvc.getActiveTerms(this.selectedCohortId).subscribe({
      next: (options) => {
        this.activeTermOptions.set(options);
        this.loadingActiveTerms.set(false);
        if (options.length === 1) {
          this.selectFromTerm(options[0].termInstanceId);
        } else if (options.length === 0) {
          // Nothing to auto-detect (e.g. a brand-new cohort with no enrollment yet) — fall
          // back to manual academic-year/term selection.
          this.showAdvanced.set(true);
        }
      },
      error: () => {
        this.loadingActiveTerms.set(false);
        this.showAdvanced.set(true);
      },
    });
  }

  /** Called when the admin picks a "from" term — either automatically (single option) or from
   *  the compact list (multiple options) — and fetches/auto-fills the suggested destination. */
  protected selectFromTerm(termInstanceId: number): void {
    this.selectedFromTermInstanceId = termInstanceId;
    this.suggestedToTerm.set(null);
    this.selectedToTermInstanceId = null;

    this.promotionSvc.getSuggestedNextTerm(termInstanceId).subscribe({
      next: (suggestion) => {
        this.suggestedToTerm.set(suggestion);
        if (suggestion) {
          this.selectedToTermInstanceId = suggestion.termInstanceId;
        } else {
          // No next term exists yet (e.g. next academic year hasn't been created) — admin must
          // pick a destination manually.
          this.showAdvanced.set(true);
        }
      },
      error: () => this.showAdvanced.set(true),
    });
  }

  protected toggleAdvanced(): void {
    this.showAdvanced.set(!this.showAdvanced());
  }

  private resetTermSelection(): void {
    this.activeTermOptions.set([]);
    this.suggestedToTerm.set(null);
    this.showAdvanced.set(false);
    this.selectedFromAcademicYearId = null;
    this.selectedFromTermInstanceId = null;
    this.selectedToAcademicYearId = null;
    this.selectedToTermInstanceId = null;
    this.fromTermInstances.set([]);
    this.toTermInstances.set([]);
  }

  protected onFromAcademicYearChange(): void {
    this.selectedFromTermInstanceId = null;
    if (!this.selectedFromAcademicYearId) {
      this.fromTermInstances.set([]);
      return;
    }
    this.academicYearSvc.getTermInstancesByAcademicYear(this.selectedFromAcademicYearId).subscribe({
      next: (terms) => this.fromTermInstances.set(terms),
    });
  }

  protected onToAcademicYearChange(): void {
    this.selectedToTermInstanceId = null;
    if (!this.selectedToAcademicYearId) {
      this.toTermInstances.set([]);
      return;
    }
    this.academicYearSvc.getTermInstancesByAcademicYear(this.selectedToAcademicYearId).subscribe({
      next: (terms) => this.toTermInstances.set(terms),
    });
  }

  /** Suggests the destination term chronologically following the chosen source term — same
   *  academic year's EVEN term after ODD, or the next academic year's ODD term after EVEN.
   *  Purely a convenience default; the admin can still override both dropdowns. */
  protected onFromTermChange(): void {
    const fromTerm = this.fromTermInstances().find((t) => t.id === this.selectedFromTermInstanceId);
    if (!fromTerm) return;

    if (fromTerm.termType === 'ODD') {
      this.selectedToAcademicYearId = fromTerm.academicYearId;
      this.academicYearSvc.getTermInstancesByAcademicYear(fromTerm.academicYearId).subscribe({
        next: (terms) => {
          this.toTermInstances.set(terms);
          this.selectedToTermInstanceId = terms.find((t) => t.termType === 'EVEN')?.id ?? null;
        },
      });
    } else {
      const currentIndex = this.academicYears().findIndex((y) => y.id === fromTerm.academicYearId);
      const nextYear = currentIndex > 0 ? this.academicYears()[currentIndex - 1] : null; // sorted desc
      if (!nextYear) return;
      this.selectedToAcademicYearId = nextYear.id;
      this.academicYearSvc.getTermInstancesByAcademicYear(nextYear.id).subscribe({
        next: (terms) => {
          this.toTermInstances.set(terms);
          this.selectedToTermInstanceId = terms.find((t) => t.termType === 'ODD')?.id ?? null;
        },
      });
    }
  }

  protected loadPreview(): void {
    if (!this.readyForPreview()) return;
    this.loadingPreview.set(true);
    this.promotionSvc.preview({
      cohortId: this.selectedCohortId!,
      fromTermInstanceId: this.selectedFromTermInstanceId!,
      toTermInstanceId: this.selectedToTermInstanceId!,
    }).subscribe({
      next: (response) => {
        this.preview.set(response);
        this.rows.set(response.students.map((row) => ({
          preview: row,
          outcome: row.recommendedOutcome ?? 'EXCLUDED',
          remarks: '',
        })));
        this.loadingPreview.set(false);
        this.step.set('preview');
        if (!response.students.length) {
          this.toast.warning('No enrolled students found for this cohort/term.');
        }
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to load promotion preview');
        this.loadingPreview.set(false);
      },
    });
  }

  protected backToSelect(): void {
    this.step.set('select');
    this.preview.set(null);
    this.rows.set([]);
  }

  protected isBlocked(row: PromotionEditableRow): boolean {
    return row.preview.blockReasons.length > 0;
  }

  protected blockLabel(reason: string): string {
    return BLOCK_REASON_LABELS[reason] ?? reason;
  }

  protected outcomeOptionsFor(row: PromotionEditableRow): PromotionOutcome[] {
    if (this.isBlocked(row)) {
      return ['DETAINED_REPEAT', 'EXCLUDED'];
    }
    return this.outcomeOptions;
  }

  protected executeAll(): void {
    const preview = this.preview();
    if (!preview) return;
    this.executing.set(true);
    const decisions: PromotionDecisionInput[] = this.rows().map((r) => ({
      studentId: r.preview.studentId,
      outcome: r.outcome,
      remarks: r.remarks.trim() || null,
    }));

    this.promotionSvc.execute({
      cohortId: preview.cohortId,
      fromTermInstanceId: preview.fromTermInstanceId,
      toTermInstanceId: preview.toTermInstanceId,
      decisions,
      generateCourseRegistrations: this.generateCourseRegistrations,
      generateFeeDemands: this.generateFeeDemands,
    }).subscribe({
      next: (result) => {
        this.executeResult.set(result);
        this.executing.set(false);
        this.step.set('result');
        if (result.rejectedDecisions.length) {
          this.toast.warning(`${result.rejectedDecisions.length} decision(s) were rejected — see details below.`);
        } else {
          this.toast.success('Promotion executed successfully.');
        }
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to execute promotion');
        this.executing.set(false);
      },
    });
  }

  protected startOver(): void {
    this.step.set('select');
    this.preview.set(null);
    this.rows.set([]);
    this.executeResult.set(null);
    this.selectedCohortId = null;
    this.resetTermSelection();
  }
}

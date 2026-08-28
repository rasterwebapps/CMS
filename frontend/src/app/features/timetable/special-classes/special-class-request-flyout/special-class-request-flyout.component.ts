import { Component, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TitleCasePipe } from '@angular/common';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CmsFlyoutPanelComponent } from '../../../../shared/flyout-panel/flyout-panel.component';
import { ToastService } from '../../../../core/toast/toast.service';
import { violationText } from '../../../../shared/util/violation-text';
import { AcademicYearService } from '../../../academic-year/academic-year.service';
import { AcademicYear, CohortSummary, CourseOffering, TermInstance } from '../../../academic-year/academic-year.model';
import { PeriodService } from '../../../period/period.service';
import { Period } from '../../../period/period.model';
import { ClassroomService } from '../../../classroom/classroom.service';
import { Classroom } from '../../../classroom/classroom.model';
import { LabService } from '../../../lab/lab.service';
import { Lab } from '../../../lab/lab.model';
import { ClinicalVenueService } from '../../../clinical-venue/clinical-venue.service';
import { ClinicalVenue } from '../../../clinical-venue/clinical-venue.model';
import { FacultyService } from '../../../faculty/faculty.service';
import { Faculty } from '../../../faculty/faculty.model';
import { SpecialClassService } from '../special-class.service';
import { SpecialClassSessionType, WeekDay } from '../special-class.model';

type Mode = 'PICK_MODE' | 'SINGLE_SUBJECT' | 'DAY_REPEAT';

const WEEKDAYS: WeekDay[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];

/** BR-55: request-creation flyout for a faculty-initiated special class. Two modes share one
 *  academic-year/term/cohort cascade (mirrors SkeletonBuilderComponent's own cascade) — a
 *  single-subject ad-hoc session, or a whole-day-repeat batch. Submits and lets the caller catch
 *  `TimetableConstraintViolationException` via the shared {@link violationText} util, matching
 *  Skeleton Builder/Staffing's own toast-based conflict surfacing rather than a live dry-run
 *  preview endpoint. */
@Component({
  selector: 'app-special-class-request-flyout',
  standalone: true,
  imports: [FormsModule, TitleCasePipe, MatProgressSpinnerModule, CmsFlyoutPanelComponent],
  templateUrl: './special-class-request-flyout.component.html',
  styleUrl: './special-class-request-flyout.component.scss',
})
export class SpecialClassRequestFlyoutComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly periodService = inject(PeriodService);
  private readonly classroomService = inject(ClassroomService);
  private readonly labService = inject(LabService);
  private readonly clinicalVenueService = inject(ClinicalVenueService);
  private readonly facultyService = inject(FacultyService);
  private readonly specialClassService = inject(SpecialClassService);
  private readonly toast = inject(ToastService);

  /** Deep-link entry point (e.g. from Global Auto-Schedule's "last remaining subject" alert) —
   *  when set, skips straight to the single-subject form with term/cohort/offering pre-selected
   *  instead of making the admin re-pick everything they just saw was short. All four (bar
   *  sessionType) are required together; a partial set is treated as no prefill at all. */
  readonly prefillAcademicYearId = input<number | null>(null);
  readonly prefillTermInstanceId = input<number | null>(null);
  readonly prefillCohortId = input<number | null>(null);
  readonly prefillCourseOfferingId = input<number | null>(null);
  readonly prefillSessionType = input<SpecialClassSessionType | null>(null);

  readonly closed = output<void>();
  readonly saved = output<void>();

  protected readonly mode = signal<Mode>('PICK_MODE');
  protected readonly weekDays = WEEKDAYS;

  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly termInstances = signal<TermInstance[]>([]);
  protected readonly cohorts = signal<CohortSummary[]>([]);
  protected readonly courseOfferings = signal<CourseOffering[]>([]);
  protected readonly periods = signal<Period[]>([]);
  protected readonly classrooms = signal<Classroom[]>([]);
  protected readonly labs = signal<Lab[]>([]);
  protected readonly clinicalVenues = signal<ClinicalVenue[]>([]);
  protected readonly faculty = signal<Faculty[]>([]);
  protected readonly loadingOfferings = signal(false);
  protected readonly saving = signal(false);

  protected selectedAcademicYearId: number | null = null;
  protected selectedTermInstanceId: number | null = null;
  protected selectedCohortId: number | null = null;

  // Single-subject fields
  protected selectedCourseOfferingId: number | null = null;
  protected occurrenceDate = '';
  protected periodId: number | null = null;
  protected sessionType: SpecialClassSessionType = 'THEORY';
  protected venueId: number | null = null;
  protected requestedFacultyId: number | null = null;
  protected cohortSectionId: number | null = null;
  protected reason = '';

  // Day-repeat fields
  protected sourceDayOfWeek: WeekDay = 'MONDAY';
  protected targetDate = '';
  protected repeatCohortSectionId: number | null = null;
  protected repeatReason = '';

  protected readonly selectedOffering = computed(() =>
    this.courseOfferings().find((o) => o.id === this.selectedCourseOfferingId) ?? null);

  constructor() {
    this.periodService.getAll(true).subscribe({ next: (data) => this.periods.set(data) });
    this.classroomService.getAll(true).subscribe({ next: (data) => this.classrooms.set(data) });
    this.labService.getAll().subscribe({ next: (data) => this.labs.set(data) });
    this.clinicalVenueService.getAll(true).subscribe({ next: (data) => this.clinicalVenues.set(data) });
    this.facultyService.getAll().subscribe({ next: (data) => this.faculty.set(data) });
    // Both callbacks below fire asynchronously (well after ngOnInit), so reading the prefill
    // inputs here is safe (unlike a synchronous constructor-body read, which would throw NG0950)
    // — this only guards against the deep-link's own selections being clobbered once these lists
    // resolve; the lists themselves always load normally either way.
    this.academicYearService.getAllAcademicYears().subscribe({
      next: (years) => {
        this.academicYears.set(years);
        if (this.hasPrefill()) return;
        const initialYearId = years.find((y) => y.isCurrent)?.id ?? years[0]?.id ?? null;
        if (initialYearId) {
          this.selectedAcademicYearId = initialYearId;
          this.loadTermInstances(initialYearId);
        }
      },
    });
    this.academicYearService.getAllCohorts().subscribe({
      next: (cohorts) => {
        this.cohorts.set(cohorts);
        if (!this.hasPrefill()) this.selectedCohortId = cohorts[0]?.id ?? null;
      },
    });
  }

  protected hasPrefill(): boolean {
    return this.prefillTermInstanceId() != null && this.prefillCohortId() != null && this.prefillCourseOfferingId() != null;
  }

  /** Required signal inputs aren't guaranteed bound until ngOnInit — reading them any earlier
   *  (e.g. the constructor body) throws NG0950. */
  ngOnInit(): void {
    if (!this.hasPrefill()) return;
    this.selectedAcademicYearId = this.prefillAcademicYearId();
    this.selectedTermInstanceId = this.prefillTermInstanceId();
    this.selectedCohortId = this.prefillCohortId();
    if (this.prefillSessionType()) {
      this.sessionType = this.prefillSessionType()!;
    }
    if (this.selectedAcademicYearId != null) {
      this.academicYearService.getTermInstancesByAcademicYear(this.selectedAcademicYearId).subscribe({
        next: (terms) => this.termInstances.set(terms),
        error: () => this.toast.error('Failed to load term instances'),
      });
    }
    this.mode.set('SINGLE_SUBJECT');
    this.loadingOfferings.set(true);
    const termInstanceId = this.prefillTermInstanceId()!;
    const cohortId = this.prefillCohortId()!;
    this.academicYearService.getCourseOfferingsByTermInstance(termInstanceId, undefined, cohortId)
      .subscribe({
        next: (offerings) => {
          this.courseOfferings.set(offerings);
          this.loadingOfferings.set(false);
          this.selectedCourseOfferingId = this.prefillCourseOfferingId();
          this.onCourseOfferingChange();
        },
        error: () => { this.toast.error('Failed to load course offerings'); this.loadingOfferings.set(false); },
      });
  }

  protected pickMode(next: 'SINGLE_SUBJECT' | 'DAY_REPEAT'): void {
    this.mode.set(next);
    this.loadCourseOfferings();
  }

  protected backToModePicker(): void {
    this.mode.set('PICK_MODE');
  }

  protected onAcademicYearChange(): void {
    this.selectedTermInstanceId = null;
    if (this.selectedAcademicYearId) this.loadTermInstances(this.selectedAcademicYearId);
  }

  protected onTermOrCohortChange(): void {
    this.loadCourseOfferings();
  }

  private loadTermInstances(academicYearId: number): void {
    this.academicYearService.getTermInstancesByAcademicYear(academicYearId).subscribe({
      next: (terms) => {
        this.termInstances.set(terms);
        this.selectedTermInstanceId = terms[0]?.id ?? null;
        this.loadCourseOfferings();
      },
      error: () => this.toast.error('Failed to load term instances'),
    });
  }

  private loadCourseOfferings(): void {
    if (!this.selectedTermInstanceId || !this.selectedCohortId || this.mode() !== 'SINGLE_SUBJECT') return;
    this.loadingOfferings.set(true);
    this.academicYearService.getCourseOfferingsByTermInstance(this.selectedTermInstanceId, undefined, this.selectedCohortId)
      .subscribe({
        next: (offerings) => {
          this.courseOfferings.set(offerings);
          this.loadingOfferings.set(false);
        },
        error: () => { this.toast.error('Failed to load course offerings'); this.loadingOfferings.set(false); },
      });
  }

  /** Prefills the (still freely editable) Faculty dropdown from the offering's whole-cohort
   *  assignment, if one exists — cohortSectionId isn't entered yet at this point in the form, so a
   *  split cohort with independently-assigned sections has no single answer to prefill and is left
   *  blank for the admin to pick themselves. */
  protected onCourseOfferingChange(): void {
    const offering = this.selectedOffering();
    this.requestedFacultyId = null;
    if (!offering || this.selectedCohortId == null) return;
    this.academicYearService.getSectionFaculty(offering.id).subscribe({
      next: (res) => {
        const row = res.sections.find((r) => r.cohortId === this.selectedCohortId && r.cohortSectionId === null);
        this.requestedFacultyId = row?.facultyId ?? null;
      },
    });
  }

  protected venueOptionsFor(type: SpecialClassSessionType): { id: number; name: string }[] {
    if (type === 'THEORY') return this.classrooms().map((c) => ({ id: c.id, name: c.name }));
    if (type === 'LAB') return this.labs().map((l) => ({ id: l.id, name: l.name }));
    return this.clinicalVenues().map((v) => ({ id: v.id, name: v.name }));
  }

  protected onSessionTypeChange(): void {
    this.venueId = null;
  }

  protected canSubmitSingle(): boolean {
    const offering = this.selectedOffering();
    return !!(offering && this.occurrenceDate && this.periodId && this.venueId && this.requestedFacultyId);
  }

  protected submitSingle(): void {
    const offering = this.selectedOffering();
    if (!this.canSubmitSingle() || !offering) return;
    this.saving.set(true);
    this.specialClassService.requestSingleSubject({
      occurrenceDate: this.occurrenceDate,
      periodId: this.periodId!,
      subjectId: offering.subjectId,
      courseOfferingId: offering.id,
      cohortSectionId: this.cohortSectionId,
      sessionType: this.sessionType,
      classroomId: this.sessionType === 'THEORY' ? this.venueId : null,
      labId: this.sessionType === 'LAB' ? this.venueId : null,
      clinicalVenueId: this.sessionType === 'CLINICAL' ? this.venueId : null,
      requestedFacultyId: this.requestedFacultyId!,
      reason: this.reason || null,
    }).subscribe({
      next: () => {
        this.toast.success('Special class requested — awaiting admin approval');
        this.saving.set(false);
        this.saved.emit();
      },
      error: (err) => {
        this.toast.error(violationText(err) ?? 'Failed to request special class');
        this.saving.set(false);
      },
    });
  }

  protected canSubmitRepeat(): boolean {
    return !!(this.selectedTermInstanceId && this.targetDate && this.repeatCohortSectionId);
  }

  protected submitRepeat(): void {
    if (!this.canSubmitRepeat()) return;
    this.saving.set(true);
    this.specialClassService.requestDayRepeat({
      termInstanceId: this.selectedTermInstanceId!,
      sourceDayOfWeek: this.sourceDayOfWeek,
      targetDate: this.targetDate,
      cohortSectionId: this.repeatCohortSectionId!,
      reason: this.repeatReason || null,
    }).subscribe({
      next: (result) => {
        const skipped = result.skippedCount > 0 ? ` (${result.skippedCount} session(s) skipped — cohort unresolved)` : '';
        this.toast.success(`${result.created.length} session(s) requested — awaiting admin approval${skipped}`);
        this.saving.set(false);
        this.saved.emit();
      },
      error: (err) => {
        this.toast.error(violationText(err) ?? 'Failed to request day repeat');
        this.saving.set(false);
      },
    });
  }

  protected onClose(): void {
    this.closed.emit();
  }
}

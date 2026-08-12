import { Component, computed, inject, output, signal } from '@angular/core';
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
export class SpecialClassRequestFlyoutComponent {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly periodService = inject(PeriodService);
  private readonly classroomService = inject(ClassroomService);
  private readonly labService = inject(LabService);
  private readonly clinicalVenueService = inject(ClinicalVenueService);
  private readonly facultyService = inject(FacultyService);
  private readonly specialClassService = inject(SpecialClassService);
  private readonly toast = inject(ToastService);

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
    this.academicYearService.getAllAcademicYears().subscribe({
      next: (years) => {
        this.academicYears.set(years);
        const initialYearId = years.find((y) => y.isCurrent)?.id ?? years[0]?.id ?? null;
        if (initialYearId) {
          this.selectedAcademicYearId = initialYearId;
          this.loadTermInstances(initialYearId);
        }
      },
    });
    this.academicYearService.getAllCohorts().subscribe({
      next: (cohorts) => { this.cohorts.set(cohorts); this.selectedCohortId = cohorts[0]?.id ?? null; },
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

  protected onCourseOfferingChange(): void {
    const offering = this.selectedOffering();
    this.requestedFacultyId = offering?.facultyId ?? null;
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

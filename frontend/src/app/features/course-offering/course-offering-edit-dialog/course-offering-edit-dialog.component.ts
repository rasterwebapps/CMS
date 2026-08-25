import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import {
  CourseOffering,
  EligibleFacultyCandidate,
  SectionFacultyAssignment,
} from '../../academic-year/academic-year.model';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';
import { violationText } from '../../../shared/util/violation-text';

/** Kept for the other dialogs (Batch Manage, Class Incharge) that still import this shape from
 *  here — this dialog itself no longer uses a flat FacultyOption list. */
export interface FacultyOption {
  id: number;
  name: string;
  specialityId: number | null;
}

export interface CourseOfferingEditDialogData {
  offering: CourseOffering;
  /** Set when opened via a "reassign this offering" deep link (e.g. Skeleton Builder's Global
   *  Auto-Schedule capacity report) — shown as an informational hint once the suggested faculty's
   *  name resolves; the admin still has to pick them from the right row themselves and save. */
  suggestedFacultyId?: number | null;
}

@Component({
  selector: 'app-course-offering-edit-dialog',
  standalone: true,
  imports: [MatDialogModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './course-offering-edit-dialog.component.html',
  styleUrl: './course-offering-edit-dialog.component.scss',
})
export class CourseOfferingEditDialogComponent implements OnInit {
  private readonly dialogRef = inject(MatDialogRef<CourseOfferingEditDialogComponent>);
  protected readonly data: CourseOfferingEditDialogData = inject(MAT_DIALOG_DATA);
  private readonly academicYearService = inject(AcademicYearService);
  private readonly permissionService = inject(PermissionService);
  private readonly toast = inject(ToastService);

  protected readonly canManageAssignment = computed(() => this.permissionService.has('SECTION_FACULTY_MANAGE'));

  /** One row per cohort using this offering — a row with cohortSectionId set is one active section
   *  of a split cohort (assign via updateSectionFaculty); cohortSectionId null means the whole
   *  cohort has no split (assign via updateCohortFaculty). Every cohort always gets at least one
   *  row now — there is no separate offering-wide primary field anymore. */
  protected readonly assignmentLoading = signal(false);
  protected readonly assignmentRows = signal<SectionFacultyAssignment[]>([]);
  protected readonly assignmentApplicable = signal(true);
  protected readonly assignmentSavingKey = signal<string | null>(null);

  /** Every eligible (Speciality match OR the subject's Eligible Faculty list) active faculty for
   *  this offering, each annotated with real remaining term capacity and sorted most-free-first by
   *  the backend — backs the Faculty Pool checklist below. */
  protected readonly eligibleCandidates = signal<EligibleFacultyCandidate[]>([]);
  protected readonly eligibleCandidatesLoading = signal(false);

  /** Pending faculty-pool edit buffer — seeded from eligibleCandidates()'s inPool flags on load
   *  and after every successful pool save, mutated by checkbox toggles in between. Every
   *  assignment row below stays scoped to the last-SAVED pool (each candidate's real inPool flag),
   *  not this buffer, so "who can be assigned" only ever reflects what's actually persisted —
   *  building the pool and assigning from it are deliberately two separate steps. */
  protected readonly poolSelection = signal<Set<number>>(new Set());
  protected readonly poolSaving = signal(false);
  protected readonly poolDirty = computed(() => {
    const persisted = new Set(this.eligibleCandidates().filter((c) => c.inPool).map((c) => c.facultyId));
    const pending = this.poolSelection();
    if (persisted.size !== pending.size) return true;
    for (const id of pending) if (!persisted.has(id)) return true;
    return false;
  });

  /** Name of the deep-link-suggested faculty, resolved once eligibleCandidates loads — null until
   *  then, or if they're not actually in the eligible list. */
  protected readonly suggestedFacultyName = computed(() => {
    const id = this.data.suggestedFacultyId;
    if (id == null) return null;
    return this.eligibleCandidates().find((c) => c.facultyId === id)?.facultyName ?? null;
  });

  /** Section-level and cohort-level eligible-candidate lists, fetched lazily and cached per row the
   *  first time it renders — avoids firing every row's request up front. */
  private readonly sectionCandidatesCache = signal<Map<number, EligibleFacultyCandidate[]>>(new Map());
  private readonly sectionCandidatesLoading = new Set<number>();
  private readonly cohortCandidatesCache = signal<Map<number, EligibleFacultyCandidate[]>>(new Map());
  private readonly cohortCandidatesLoading = new Set<number>();

  ngOnInit(): void {
    this.assignmentLoading.set(true);
    this.academicYearService.getSectionFaculty(this.data.offering.id).subscribe({
      next: (res) => {
        this.assignmentApplicable.set(res.applicable);
        this.assignmentRows.set(res.sections);
        this.assignmentLoading.set(false);
      },
      error: () => {
        this.assignmentLoading.set(false);
        this.toast.error('Failed to load faculty assignment');
      },
    });

    this.eligibleCandidatesLoading.set(true);
    this.academicYearService.getEligibleFaculty(this.data.offering.id).subscribe({
      next: (candidates) => {
        this.eligibleCandidates.set(candidates);
        this.poolSelection.set(new Set(candidates.filter((c) => c.inPool).map((c) => c.facultyId)));
        this.eligibleCandidatesLoading.set(false);
      },
      error: () => { this.eligibleCandidatesLoading.set(false); },
    });
  }

  /** Flips a candidate's membership in the pending pool-edit buffer — does not save anything on
   *  its own; the admin still has to click "Save Pool". */
  protected toggleInPool(facultyId: number): void {
    this.poolSelection.update((current) => {
      const next = new Set(current);
      if (next.has(facultyId)) next.delete(facultyId); else next.add(facultyId);
      return next;
    });
  }

  /** Persists the pending buffer as this offering's faculty pool. The backend blocks removing
   *  anyone currently relied upon (holding any assignment row) with a clear error — surfaced the
   *  same way every other save path in this dialog reports a violation. On success, refreshes
   *  eligibleCandidates (updating every inPool flag) and drops both candidate caches so every row
   *  re-fetches against the newly-saved pool next render. */
  protected saveFacultyPool(): void {
    this.poolSaving.set(true);
    this.academicYearService.updateFacultyPool(this.data.offering.id, [...this.poolSelection()]).subscribe({
      next: (candidates) => {
        this.poolSaving.set(false);
        this.eligibleCandidates.set(candidates);
        this.poolSelection.set(new Set(candidates.filter((c) => c.inPool).map((c) => c.facultyId)));
        this.sectionCandidatesCache.set(new Map());
        this.cohortCandidatesCache.set(new Map());
        this.toast.success('Faculty pool updated');
      },
      error: (err) => {
        this.poolSaving.set(false);
        this.toast.error(violationText(err) ?? err?.error?.message ?? 'Failed to update faculty pool');
      },
    });
  }

  /** A unique key per row regardless of type — cohortSectionId and cohortId are different id
   *  spaces, so a plain numeric key alone could collide between a section row and a cohort row. */
  protected rowKey(row: SectionFacultyAssignment): string {
    return row.cohortSectionId != null ? `section-${row.cohortSectionId}` : `cohort-${row.cohortId}`;
  }

  /** Assignment options for a row, scoped to the persisted pool only — routes to the section- or
   *  cohort-scoped eligible-candidate fetch depending on the row's shape. */
  protected candidatesFor(row: SectionFacultyAssignment): EligibleFacultyCandidate[] {
    const raw = row.cohortSectionId != null
      ? this.sectionCandidatesRawFor(row.cohortSectionId)
      : this.cohortCandidatesRawFor(row.cohortId);
    return raw.filter((c) => c.inPool);
  }

  private sectionCandidatesRawFor(cohortSectionId: number): EligibleFacultyCandidate[] {
    const cached = this.sectionCandidatesCache().get(cohortSectionId);
    if (cached) return cached;
    if (!this.sectionCandidatesLoading.has(cohortSectionId)) {
      this.sectionCandidatesLoading.add(cohortSectionId);
      this.academicYearService.getEligibleFacultyForSection(this.data.offering.id, cohortSectionId).subscribe({
        next: (candidates) => {
          this.sectionCandidatesCache.update((m) => new Map(m).set(cohortSectionId, candidates));
        },
        error: () => { this.sectionCandidatesLoading.delete(cohortSectionId); },
      });
    }
    return [];
  }

  private cohortCandidatesRawFor(cohortId: number): EligibleFacultyCandidate[] {
    const cached = this.cohortCandidatesCache().get(cohortId);
    if (cached) return cached;
    if (!this.cohortCandidatesLoading.has(cohortId)) {
      this.cohortCandidatesLoading.add(cohortId);
      this.academicYearService.getEligibleFacultyForCohort(this.data.offering.id, cohortId).subscribe({
        next: (candidates) => {
          this.cohortCandidatesCache.update((m) => new Map(m).set(cohortId, candidates));
        },
        error: () => { this.cohortCandidatesLoading.delete(cohortId); },
      });
    }
    return [];
  }

  /** Shared label for a candidate row's eligibility badge. */
  protected candidateBadgeText(c: EligibleFacultyCandidate): string {
    if (c.viaEligibleList) return 'Eligible list';
    if (c.specialityMatch) return 'Speciality match';
    return 'Currently assigned';
  }

  /** Shared label for a candidate row's remaining-capacity figure. */
  protected candidateHoursText(c: EligibleFacultyCandidate): string {
    if (c.capacityTier === 'NONE') return 'No cap configured';
    if (c.overCapacity) return 'Over capacity';
    return `${Math.round(c.remainingHours * 10) / 10}h free`;
  }

  /** Instant-save per row, same as Section Faculty already worked before this dialog was
   *  generalized — the backend hard-blocks an over-capacity or ineligible pick and the violation
   *  surfaces via toast, rather than a separate live pre-save preview. */
  protected onAssignmentChange(row: SectionFacultyAssignment, facultyId: number | null): void {
    const key = this.rowKey(row);
    this.assignmentSavingKey.set(key);
    const request$ = row.cohortSectionId != null
      ? this.academicYearService.updateSectionFaculty(this.data.offering.id, row.cohortSectionId, facultyId)
      : this.academicYearService.updateCohortFaculty(this.data.offering.id, row.cohortId, facultyId);
    request$.subscribe({
      next: (updated) => {
        this.assignmentSavingKey.set(null);
        this.assignmentRows.update((rows) => rows.map((r) => (this.rowKey(r) === key ? updated : r)));
        this.toast.success(`${row.sectionLabel ?? row.cohortName} updated`);
      },
      error: (err) => {
        this.assignmentSavingKey.set(null);
        this.toast.error(violationText(err) ?? err?.error?.message ?? 'Failed to update faculty assignment');
      },
    });
  }

  protected onClose(): void {
    this.dialogRef.close();
  }
}

import { Component, computed, inject, signal } from '@angular/core';
import { DecimalPipe, TitleCasePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { EligibleFacultyCandidate } from '../../../academic-year/academic-year.model';
import { AcademicYearService } from '../../../academic-year/academic-year.service';
import { SkeletonCell, SkeletonSubject } from '../skeleton-builder.model';

export interface SkeletonCellReplaceDialogData {
  cell: SkeletonCell;
  /** The whole cohort's subject list, straight off the loaded skeleton — the dialog filters it
   *  down to legal replacement targets itself rather than the caller pre-filtering, so the
   *  "why isn't X offered?" rules live in one place next to the copy that explains them. */
  subjects: SkeletonSubject[];
  /** Needed for the cohort-scoped eligible-faculty fallback when the cell has no section. */
  cohortId: number;
}

/** What the user picked, handed back to the grid to actually call the API — the dialog itself
 *  never writes, so a failed replace re-opens with the same choices rather than losing them. */
export interface SkeletonCellReplaceDialogResult {
  courseOfferingId: number;
  facultyId: number;
}

/** One selectable replacement subject, with the quota arithmetic already resolved for THIS cell's
 *  section so the template stays declarative. */
interface ReplacementOption {
  courseOfferingId: number;
  subjectName: string;
  subjectCode: string;
  requiredSessionsPerWeek: number;
  placedSessionsPerWeek: number;
  /** True when this subject already has at least every session its curriculum requires for this
   *  section. Purely informational — it is NOT a block. A replace is hour-neutral (one slot changes
   *  hands, the term total is unchanged), so the backend deliberately applies no budget cap to it;
   *  this only sorts under-quota subjects to the top, since those are what an admin is realistically
   *  moving a slot to. */
  atQuota: boolean;
}

/** Replace what a placed Theory session teaches, keeping its day/period/audience.
 *
 *  <p>Read-only on open and write-free on close: it gathers a subject + faculty and returns them.
 *  The grid owns the actual call so that a constraint violation surfaces in the grid's existing
 *  violation-toast path, next to every other placement error, instead of this dialog growing a
 *  second, parallel error surface.
 *
 *  <p>Faculty is deliberately re-picked rather than carried over — the incoming subject is a
 *  different subject, and the outgoing cell's faculty is very often not eligible to teach it. The
 *  section-scoped eligible-faculty endpoint already ranks candidates most-free-first and annotates
 *  real remaining capacity, so this reuses it verbatim rather than re-deriving eligibility here. */
@Component({
  selector: 'app-skeleton-cell-replace-dialog',
  standalone: true,
  imports: [FormsModule, DecimalPipe, TitleCasePipe, MatDialogModule, MatProgressSpinnerModule],
  templateUrl: './skeleton-cell-replace-dialog.component.html',
  styleUrl: './skeleton-cell-replace-dialog.component.scss',
})
export class SkeletonCellReplaceDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<SkeletonCellReplaceDialogComponent>);
  protected readonly data: SkeletonCellReplaceDialogData = inject(MAT_DIALOG_DATA);
  private readonly academicYearService = inject(AcademicYearService);

  protected readonly selectedOfferingId = signal<number | null>(null);
  protected readonly selectedFacultyId = signal<number | null>(null);
  protected readonly faculty = signal<EligibleFacultyCandidate[]>([]);
  protected readonly loadingFaculty = signal(false);
  protected readonly facultyError = signal<string | null>(null);

  /** Every non-elective Theory offering in this cohort except the one already in the slot, with
   *  quota resolved against this cell's own section.
   *
   *  <p>Electives are excluded on both sides: the backend refuses them outright, because every
   *  member of an elective group shares a single slot and re-pointing one member here would
   *  desynchronise the group. Place Elective Block is the tool for that. */
  protected readonly options = computed<ReplacementOption[]>(() => {
    const currentOfferingId = this.data.cell.courseOfferingId;
    const sectionId = this.data.cell.cohortSectionId;
    return this.data.subjects
      .filter((s) => s.electiveGroupId == null && s.courseOfferingId !== currentOfferingId)
      .map((s) => {
        // Match the budget row for this cell's own section, falling back only to a section-less
        // whole-cohort THEORY row. Never fall back to a DIFFERENT section's row: a subject taught
        // to Section 2 but not Section 1 would otherwise be offered here showing Section 2's quota,
        // and replacing into it would hand this section a subject it isn't enrolled in.
        const budget = sectionId == null
          ? s.budgets.find((b) => b.sessionType === 'THEORY')
          : s.budgets.find((b) => b.sessionType === 'THEORY' && b.cohortSectionId === sectionId)
            ?? s.budgets.find((b) => b.sessionType === 'THEORY' && b.cohortSectionId == null);
        if (!budget) return null;
        return {
          courseOfferingId: s.courseOfferingId,
          subjectName: s.subjectName,
          subjectCode: s.subjectCode,
          requiredSessionsPerWeek: budget.requiredSessionsPerWeek,
          placedSessionsPerWeek: budget.placedSessionsPerWeek,
          atQuota: budget.placedSessionsPerWeek >= budget.requiredSessionsPerWeek,
        } satisfies ReplacementOption;
      })
      .filter((o): o is ReplacementOption => o !== null)
      // Subjects that still owe sessions first — those are the ones an admin is realistically
      // moving a slot to. At-quota subjects stay visible at the bottom, disabled.
      .sort((a, b) => Number(a.atQuota) - Number(b.atQuota)
        || a.subjectName.localeCompare(b.subjectName));
  });

  protected readonly hasSelectableOption = computed(() => this.options().length > 0);

  /** True when every candidate is already at or over its curriculum requirement — the normal state
   *  on a grid the auto-scheduler has packed with extra hours. Not an error, just worth saying out
   *  loud so the admin understands the "3/1 per week" figures they're choosing between. */
  protected readonly allAtQuota = computed(() =>
    this.options().length > 0 && this.options().every((o) => o.atQuota));

  protected readonly selectedFaculty = computed(() =>
    this.faculty().find((f) => f.facultyId === this.selectedFacultyId()) ?? null);

  protected readonly canSave = computed(() =>
    this.selectedOfferingId() != null && this.selectedFacultyId() != null && !this.loadingFaculty());

  /** The multi-period warning is worth showing before the fact: the backend replaces every row in
   *  the session group together (replacing one period of a 2-period block would leave the block
   *  teaching two different subjects), so a user clicking one cell can be changing two. */
  protected readonly isMultiPeriod = computed(() => this.data.cell.sessionGroupId != null);

  protected onOfferingChange(offeringId: number | null): void {
    this.selectedOfferingId.set(offeringId);
    this.selectedFacultyId.set(null);
    this.faculty.set([]);
    this.facultyError.set(null);
    if (offeringId == null) return;

    this.loadingFaculty.set(true);
    const sectionId = this.data.cell.cohortSectionId;
    const request$ = sectionId != null
      ? this.academicYearService.getEligibleFacultyForSection(offeringId, sectionId)
      : this.academicYearService.getEligibleFacultyForCohort(offeringId, this.data.cohortId);

    request$.subscribe({
      next: (candidates) => {
        this.faculty.set(candidates);
        this.loadingFaculty.set(false);
        // Backend sorts most-free-first, so the head of the list is the safest default. Never
        // auto-pick someone already over capacity — that turns a convenience into a silent
        // workload violation the user didn't choose.
        const first = candidates[0];
        if (first && !first.overCapacity) this.selectedFacultyId.set(first.facultyId);
      },
      error: (err) => {
        this.loadingFaculty.set(false);
        this.facultyError.set(err?.error?.message ?? 'Could not load eligible faculty for this subject.');
      },
    });
  }

  protected onSave(): void {
    const courseOfferingId = this.selectedOfferingId();
    const facultyId = this.selectedFacultyId();
    if (courseOfferingId == null || facultyId == null) return;
    this.dialogRef.close({ courseOfferingId, facultyId } satisfies SkeletonCellReplaceDialogResult);
  }

  protected onCancel(): void {
    this.dialogRef.close();
  }
}

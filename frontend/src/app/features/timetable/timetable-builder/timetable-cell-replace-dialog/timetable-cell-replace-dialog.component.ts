import { Component, computed, inject, signal } from '@angular/core';
import { DecimalPipe, TitleCasePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { EligibleFacultyCandidate } from '../../../academic-year/academic-year.model';
import { AcademicYearService } from '../../../academic-year/academic-year.service';
import { TimetableCell, TimetableSubject } from '../timetable-builder.model';

export interface TimetableCellReplaceDialogData {
  cell: TimetableCell;
  /** The whole cohort's subject list, straight off the loaded skeleton — the dialog filters it
   *  down to legal replacement targets itself rather than the caller pre-filtering, so the
   *  "why isn't X offered?" rules live in one place next to the copy that explains them. */
  subjects: TimetableSubject[];
  /** Needed for the cohort-scoped eligible-faculty fallback when the cell has no section. */
  cohortId: number;
}

/** What the user picked, handed back to the grid to actually call the API — the dialog itself
 *  never writes, so a failed replace re-opens with the same choices rather than losing them. */
export interface TimetableCellReplaceDialogResult {
  courseOfferingId: number;
  facultyId: number;
}

/** One selectable replacement subject, with the quota arithmetic already resolved for THIS cell's
 *  section so the template stays declarative. */
interface ReplacementOption {
  courseOfferingId: number;
  subjectName: string;
  subjectCode: string;
  requiredHours: number;
  placedHours: number;
  /** True when this subject's placed sessions already deliver its curriculum hours for this
   *  section across the term. Purely informational — it is NOT a block. A replace is hour-neutral (one slot changes
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
  selector: 'app-timetable-cell-replace-dialog',
  standalone: true,
  imports: [FormsModule, DecimalPipe, TitleCasePipe, MatDialogModule, MatProgressSpinnerModule],
  templateUrl: './timetable-cell-replace-dialog.component.html',
  styleUrl: './timetable-cell-replace-dialog.component.scss',
})
export class TimetableCellReplaceDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<TimetableCellReplaceDialogComponent>);
  protected readonly data: TimetableCellReplaceDialogData = inject(MAT_DIALOG_DATA);
  private readonly academicYearService = inject(AcademicYearService);

  protected readonly selectedOfferingId = signal<number | null>(null);
  protected readonly selectedFacultyId = signal<number | null>(null);
  protected readonly faculty = signal<EligibleFacultyCandidate[]>([]);
  protected readonly loadingFaculty = signal(false);
  protected readonly facultyError = signal<string | null>(null);

  /** Every non-elective Theory offering in this cohort except the one already in the slot, with
   *  quota resolved against this cell's own section.
   *
   *  <p>Electives are never offered as the replacement: an institution-decided elective runs only
   *  its chosen option (already placed by Run Automation), and a student-choice group's options
   *  must keep sharing one slot, which the backend enforces. */
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
          requiredHours: budget.totalHours,
          placedHours: budget.deliveredHours,
          atQuota: budget.deliveredTermRuns >= budget.requiredTermRuns,
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
    this.selectedOfferingId() != null
    && this.selectedFacultyId() != null
    && !this.loadingFaculty()
    // Never offer to submit a pick the save is certain to refuse for this slot.
    && this.selectedFaculty()?.slotBlockedReason == null);

  /** How many candidates the save would refuse for this exact slot — stated plainly so a mostly
   *  unavailable list doesn't read as broken. */
  protected readonly blockedCount = computed(() =>
    this.faculty().filter((f) => f.slotBlockedReason != null).length);

  /** The multi-period warning is worth showing before the fact: the backend replaces every row in
   *  the session group together (replacing one period of a 2-period block would leave the block
   *  teaching two different subjects), so a user clicking one cell can be changing two. */
  protected readonly isMultiPeriod = computed(() => this.data.cell.sessionGroupId != null);

  /** True when the slot being replaced is a LIBRARY/SPORTS filler rather than a real Theory
   *  subject — unlike a Theory-to-Theory replace, this genuinely adds a new delivered hour to the
   *  incoming subject rather than handing one over from an outgoing subject, so the dialog's
   *  "hour-neutral" and "subject displaced" copy don't apply and are swapped for filler-specific text. */
  protected readonly isFillerSource = computed(() => this.data.cell.sessionType !== 'THEORY');

  protected onOfferingChange(offeringId: number | null): void {
    this.selectedOfferingId.set(offeringId);
    this.selectedFacultyId.set(null);
    this.faculty.set([]);
    this.facultyError.set(null);
    if (offeringId == null) return;

    this.loadingFaculty.set(true);
    const sectionId = this.data.cell.cohortSectionId;
    // The cell id makes each candidate carry whether the save would actually refuse them for this
    // day and time. The slot doesn't move in a replace, so the check is valid even though the
    // subject is changing — what's being tested is this person's load at that hour.
    const cellId = this.data.cell.id;
    const request$ = sectionId != null
      ? this.academicYearService.getEligibleFacultyForSection(offeringId, sectionId, cellId)
      : this.academicYearService.getEligibleFacultyForCohort(offeringId, this.data.cohortId, cellId);

    request$.subscribe({
      next: (candidates) => {
        this.faculty.set(candidates);
        this.loadingFaculty.set(false);
        // Backend sorts assignable-first, then most-free-first, so the head of the list is the
        // safest default. Never auto-pick someone the save would refuse for this slot, nor someone
        // already over capacity — either turns a convenience into a choice the user didn't make.
        const first = candidates[0];
        if (first && !first.overCapacity && first.slotBlockedReason == null) {
          this.selectedFacultyId.set(first.facultyId);
        }
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
    this.dialogRef.close({ courseOfferingId, facultyId } satisfies TimetableCellReplaceDialogResult);
  }

  protected onCancel(): void {
    this.dialogRef.close();
  }
}

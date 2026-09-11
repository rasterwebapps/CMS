import { Component, computed, inject, signal } from '@angular/core';
import { DecimalPipe, TitleCasePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { EligibleFacultyCandidate } from '../../../academic-year/academic-year.model';
import { AcademicYearService } from '../../../academic-year/academic-year.service';
import { SkeletonCell } from '../skeleton-builder.model';

export interface SkeletonCellReassignFacultyDialogData {
  cell: SkeletonCell;
  /** Used for the cohort-scoped candidate list — see {@link loadCandidates}. */
  cohortId: number;
}

/** Just the chosen faculty; the grid owns the API call, same split as the Replace dialog. */
export interface SkeletonCellReassignFacultyDialogResult {
  facultyId: number;
}

/** Change who teaches an already-placed session, without touching what is taught or when.
 *
 *  <p>Distinct from Replace: Replace hands the slot to a different subject (and therefore always
 *  needs a new faculty too), while this keeps the subject and swaps only the person. It is the
 *  common case by far — a staffing correction, not a curriculum one — and previously required
 *  leaving the grid for the Staffing screen.
 *
 *  <p>Room is never part of this. For a non-elective Theory, Lab or Clinical session the backend
 *  re-resolves the room from the committed Cohort Room Allocation and ignores any classroom sent,
 *  so there is nothing meaningful to offer. Elective Theory is the one case that does take a free
 *  classroom pick, and it is excluded from this dialog for that reason (see the grid's
 *  {@code reassignBlockedReason}). */
@Component({
  selector: 'app-skeleton-cell-reassign-faculty-dialog',
  standalone: true,
  imports: [FormsModule, DecimalPipe, TitleCasePipe, MatDialogModule, MatProgressSpinnerModule],
  templateUrl: './skeleton-cell-reassign-faculty-dialog.component.html',
  styleUrl: './skeleton-cell-reassign-faculty-dialog.component.scss',
})
export class SkeletonCellReassignFacultyDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<SkeletonCellReassignFacultyDialogComponent>);
  protected readonly data: SkeletonCellReassignFacultyDialogData = inject(MAT_DIALOG_DATA);
  private readonly academicYearService = inject(AcademicYearService);

  protected readonly selectedFacultyId = signal<number | null>(null);
  protected readonly faculty = signal<EligibleFacultyCandidate[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  protected readonly selectedFaculty = computed(() =>
    this.faculty().find((f) => f.facultyId === this.selectedFacultyId()) ?? null);

  /** The person currently teaching it, flagged by the backend even when they would no longer pass
   *  eligibility (a grandfathered assignment predating today's rules). Shown so the admin can see
   *  what they are changing away from. */
  protected readonly currentFaculty = computed(() =>
    this.faculty().find((f) => f.currentlyAssigned) ?? null);

  /** Disabled until the pick actually differs — re-submitting the same person would spend a
   *  round trip and a toast to change nothing. */
  protected readonly canSave = computed(() => {
    const picked = this.selectedFacultyId();
    return picked != null && !this.loading() && picked !== this.currentFaculty()?.facultyId;
  });

  protected readonly isMultiPeriod = computed(() => this.data.cell.sessionGroupId != null);

  constructor() {
    this.loadCandidates();
  }

  /** Theory is section-scoped, so its candidates are projected against this section's own Theory
   *  hours. A Lab/Clinical cell's real cost is the cohort's whole theory+lab+clinical load, which
   *  is what the cohort-scoped endpoint measures — using the section one there would understate
   *  every candidate's commitment. */
  private loadCandidates(): void {
    const cell = this.data.cell;
    if (cell.courseOfferingId == null) {
      this.loading.set(false);
      this.error.set('This session has no subject, so it has no faculty to reassign.');
      return;
    }

    const request$ = cell.sessionType === 'THEORY' && cell.cohortSectionId != null
      ? this.academicYearService.getEligibleFacultyForSection(cell.courseOfferingId, cell.cohortSectionId)
      : this.academicYearService.getEligibleFacultyForCohort(cell.courseOfferingId, this.data.cohortId);

    request$.subscribe({
      next: (candidates) => {
        this.faculty.set(candidates);
        this.loading.set(false);
        // Start on whoever holds it today, so the dropdown reads as "currently X" rather than an
        // empty prompt, and Save stays disabled until something actually changes.
        const current = candidates.find((c) => c.currentlyAssigned);
        if (current) this.selectedFacultyId.set(current.facultyId);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Could not load eligible faculty for this subject.');
      },
    });
  }

  protected onSave(): void {
    const facultyId = this.selectedFacultyId();
    if (facultyId == null) return;
    this.dialogRef.close({ facultyId } satisfies SkeletonCellReassignFacultyDialogResult);
  }

  protected onCancel(): void {
    this.dialogRef.close();
  }
}

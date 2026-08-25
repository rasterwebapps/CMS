import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import {
  CourseOffering,
  CourseOfferingUpdateRequest,
  FacultyCapacityCheckResult,
  SectionFacultyAssignment,
} from '../../academic-year/academic-year.model';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';
import { violationText } from '../../../shared/util/violation-text';

export interface FacultyOption {
  id: number;
  name: string;
  specialityId: number | null;
}

export interface CourseOfferingEditDialogData {
  offering: CourseOffering;
  facultyOptions: FacultyOption[];
  /** Set when opened via a "reassign this offering" deep link (e.g. Skeleton Builder's Global
   *  Auto-Schedule capacity report) — pre-fills the Faculty picker with the suggested candidate
   *  and runs the capacity check immediately, but never saves anything on its own; the admin
   *  still has to review and click Save themselves. */
  suggestedFacultyId?: number | null;
}

@Component({
  selector: 'app-course-offering-edit-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, FormsModule, MatDialogModule, MatIconModule, MatProgressSpinnerModule, DecimalPipe, RouterLink],
  templateUrl: './course-offering-edit-dialog.component.html',
  styleUrl: './course-offering-edit-dialog.component.scss',
})
export class CourseOfferingEditDialogComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<CourseOfferingEditDialogComponent>);
  protected readonly data: CourseOfferingEditDialogData = inject(MAT_DIALOG_DATA);
  private readonly academicYearService = inject(AcademicYearService);
  private readonly permissionService = inject(PermissionService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly saving = signal(false);
  protected readonly capacityChecking = signal(false);
  protected readonly capacityCheck = signal<FacultyCapacityCheckResult | null>(null);
  /** The faculty id the current capacityCheck() result is for — captured alongside the check
   *  itself since FacultyCapacityCheckResult carries no facultyId of its own, so the "fix their
   *  work hours" deep link always points at the right person even after the picker moves on. */
  protected readonly capacityCheckFacultyId = signal<number | null>(null);
  protected readonly canManageFaculty = computed(() => this.permissionService.has('FACULTY_MANAGE'));

  /** Per-section Theory faculty overrides — accounting-only (see CourseOfferingSectionFaculty on
   *  the backend), fetched separately from the main form since it's an independent, immediately-
   *  saved-per-row concern, not part of the primary Save/Cancel flow. Hidden entirely (not an
   *  error state) when the backend can't uniquely resolve this offering's cohort, or when the
   *  cohort has fewer than 2 active sections — there's nothing to split in either case. */
  protected readonly sectionFacultyLoading = signal(false);
  protected readonly sectionFacultySections = signal<SectionFacultyAssignment[]>([]);
  private readonly sectionFacultyApplicable = signal(false);
  protected readonly sectionFacultySavingId = signal<number | null>(null);
  protected readonly canManageSectionFaculty = computed(() => this.permissionService.has('SECTION_FACULTY_MANAGE'));
  protected readonly showSectionFaculty = computed(() =>
    this.sectionFacultyApplicable() && this.sectionFacultySections().length >= 2);

  /** True when this dialog was opened via a "reassign" deep link and the suggested faculty
   *  differs from who's currently assigned — drives the pre-fill hint banner in the template so
   *  the admin knows why the picker didn't open on the offering's existing faculty. */
  protected readonly prefilledFromSuggestion =
    this.data.suggestedFacultyId != null && this.data.suggestedFacultyId !== this.data.offering.facultyId;

  protected readonly form: FormGroup = this.fb.group({
    facultyId: [this.prefilledFromSuggestion ? this.data.suggestedFacultyId : this.data.offering.facultyId],
    secondaryFacultyId: [this.data.offering.secondaryFacultyId],
  });

  private readonly primaryFacultyIdLive = toSignal(this.form.get('facultyId')!.valueChanges, {
    initialValue: this.data.offering.facultyId,
  });
  private readonly secondaryFacultyIdLive = toSignal(this.form.get('secondaryFacultyId')!.valueChanges, {
    initialValue: this.data.offering.secondaryFacultyId,
  });

  /** Faculty must belong to the subject's own department (Speciality) — OR be explicitly listed on
   *  the subject's admin-curated Eligible Faculty list (Subject form, additive-only widening, e.g.
   *  for a short-staffed department) — to be assignable. The faculty already on this offering stays
   *  visible/selectable even if it predates the rule (grandfathered), so an admin editing just the
   *  section label doesn't lose their current faculty from the list. No restriction at all when the
   *  subject has no speciality set. Also excludes whoever is live-selected as the secondary faculty
   *  — the same person can't be their own substitute — except the primary's own current selection
   *  stays visible so a same-person pairing saved before this rule existed still renders instead of
   *  going blank. */
  protected readonly eligibleFacultyOptions = computed<FacultyOption[]>(() => {
    const specialityId = this.data.offering.subjectSpecialityId;
    const eligibleIds = this.data.offering.subjectEligibleFacultyIds;
    const secondaryId = this.secondaryFacultyIdLive();
    const currentPrimaryId = this.primaryFacultyIdLive();
    const base = !specialityId
      ? this.data.facultyOptions
      : this.data.facultyOptions.filter((f) =>
          f.specialityId === specialityId || eligibleIds.includes(f.id)
          || f.id === this.data.offering.facultyId || f.id === this.data.suggestedFacultyId);
    return base.filter((f) => f.id !== secondaryId || f.id === currentPrimaryId);
  });

  /** OC-127 gap-closure follow-up: secondaryFacultyId reopened from informational-only to a real
   *  substitute-matching-eligible co-instructor, so it now needs the same eligibility filter as the
   *  primary (Speciality match OR the subject's Eligible Faculty list) — grandfathered against its
   *  own current value (not the primary's) so an existing secondary faculty predating this rule
   *  stays visible/selectable. Same live-exclusion of the primary's current selection, for the same
   *  same-person reason as above. */
  protected readonly eligibleSecondaryFacultyOptions = computed<FacultyOption[]>(() => {
    const specialityId = this.data.offering.subjectSpecialityId;
    const eligibleIds = this.data.offering.subjectEligibleFacultyIds;
    const primaryId = this.primaryFacultyIdLive();
    const currentSecondaryId = this.secondaryFacultyIdLive();
    const base = !specialityId
      ? this.data.facultyOptions
      : this.data.facultyOptions.filter((f) =>
          f.specialityId === specialityId || eligibleIds.includes(f.id) || f.id === this.data.offering.secondaryFacultyId);
    return base.filter((f) => f.id !== primaryId || f.id === currentSecondaryId);
  });

  /** True when the offering already has (or was just edited into) the same person as both
   *  primary and secondary faculty — a pre-existing bad pairing that predates this fix, or an
   *  edge case the dropdown filtering above can't fully rule out on its own. Blocks Save with the
   *  same hard-block styling as the capacity check, matching the backend's own same-person guard
   *  in CourseOfferingServiceImpl. */
  protected readonly sameFacultyConflict = computed(() => {
    const primaryId = this.primaryFacultyIdLive();
    const secondaryId = this.secondaryFacultyIdLive();
    return primaryId != null && primaryId === secondaryId;
  });

  /** Live pre-save capacity check — fires whenever the Faculty picker settles on a new value, so
   *  the admin sees "this would put them over capacity" before ever clicking Save, not just as a
   *  hard-block surprise afterward. Debounced since every keyboard/pointer change to a native
   *  `<select>` still emits a full valueChanges event; skips re-assigning the offering's own
   *  already-current faculty (matches the backend's own grandfathering — nothing to change,
   *  can't change anyone's workload). Also re-runs on window focus so an admin who follows the
   *  "Fix their work hours" link into a new tab, raises the cap there, and switches back to this
   *  still-open dialog sees the warning clear without touching the picker again. */
  constructor() {
    this.form.get('facultyId')!.valueChanges
      .pipe(debounceTime(400), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe((facultyId: number | null) => this.runCapacityCheck(facultyId));

    const onWindowFocus = () => this.runCapacityCheck(this.form.get('facultyId')!.value, { force: true });
    window.addEventListener('focus', onWindowFocus);
    this.destroyRef.onDestroy(() => window.removeEventListener('focus', onWindowFocus));

    // A pre-filled suggestion is the form's INITIAL value, which valueChanges never emits for —
    // run the same check manually so the admin sees Fits/Over-capacity immediately on open,
    // instead of only after they touch the picker themselves.
    if (this.prefilledFromSuggestion) this.runCapacityCheck(this.data.suggestedFacultyId ?? null);
  }

  ngOnInit(): void {
    this.sectionFacultyLoading.set(true);
    this.academicYearService.getSectionFaculty(this.data.offering.id).subscribe({
      next: (res) => {
        this.sectionFacultyApplicable.set(res.applicable);
        this.sectionFacultySections.set(res.sections);
        this.sectionFacultyLoading.set(false);
      },
      // Advisory-only feature — a lookup failure (e.g. lacking SECTION_FACULTY_VIEW) just hides
      // the block silently rather than interrupting the primary edit flow with a toast.
      error: () => { this.sectionFacultyLoading.set(false); },
    });
  }

  /** Same eligibility filter as the primary Faculty field (Speciality match OR the subject's
   *  Eligible Faculty list), grandfathered against this specific section's own current value (not
   *  the primary's) — a section faculty predating this rule, or overridden before the subject had a
   *  speciality set, stays visible/selectable. */
  protected sectionFacultyOptionsFor(row: SectionFacultyAssignment): FacultyOption[] {
    const specialityId = this.data.offering.subjectSpecialityId;
    if (!specialityId) return this.data.facultyOptions;
    const eligibleIds = this.data.offering.subjectEligibleFacultyIds;
    return this.data.facultyOptions.filter((f) =>
      f.specialityId === specialityId || eligibleIds.includes(f.id) || f.id === row.facultyId);
  }

  protected onSectionFacultyChange(row: SectionFacultyAssignment, facultyId: number | null): void {
    this.sectionFacultySavingId.set(row.cohortSectionId);
    this.academicYearService.updateSectionFaculty(this.data.offering.id, row.cohortSectionId, facultyId).subscribe({
      next: (updated) => {
        this.sectionFacultySavingId.set(null);
        this.sectionFacultySections.update((rows) =>
          rows.map((r) => (r.cohortSectionId === updated.cohortSectionId ? updated : r)));
        this.toast.success(`${row.sectionLabel} updated`);
      },
      error: (err) => {
        this.sectionFacultySavingId.set(null);
        this.toast.error(violationText(err) ?? err?.error?.message ?? 'Failed to update section faculty');
      },
    });
  }

  private runCapacityCheck(facultyId: number | null, opts?: { force?: boolean }): void {
    if (!opts?.force) {
      this.capacityCheck.set(null);
      this.capacityCheckFacultyId.set(null);
    }
    if (facultyId == null || facultyId === this.data.offering.facultyId) return;
    if (opts?.force && this.capacityCheckFacultyId() !== facultyId) return;
    this.capacityChecking.set(true);
    this.academicYearService.checkFacultyCapacity(this.data.offering.id, facultyId, this.data.offering.termInstanceId).subscribe({
      next: (result) => {
        this.capacityChecking.set(false);
        this.capacityCheck.set(result);
        this.capacityCheckFacultyId.set(facultyId);
      },
      error: () => { this.capacityChecking.set(false); },
    });
  }

  protected onSubmit(): void {
    const v = this.form.value;
    const request: CourseOfferingUpdateRequest = {
      facultyId: v.facultyId ?? null,
      secondaryFacultyId: v.secondaryFacultyId ?? null,
    };

    this.saving.set(true);
    this.academicYearService.updateCourseOffering(this.data.offering.id, request).subscribe({
      next: (updated) => {
        this.saving.set(false);
        this.toast.success('Course offering updated');
        this.dialogRef.close(updated);
      },
      error: (err) => {
        this.saving.set(false);
        this.toast.error(violationText(err) ?? err?.error?.message ?? 'Failed to update course offering');
      },
    });
  }

  protected onCancel(): void {
    this.dialogRef.close();
  }
}

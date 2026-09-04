import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import {
  CourseOffering,
  EligibleFacultyCandidate,
  SectionFacultyAssignment,
} from '../../academic-year/academic-year.model';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { Batch, BatchRequest } from '../../batch/batch.model';
import { BatchService } from '../../batch/batch.service';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';
import { violationText } from '../../../shared/util/violation-text';

export interface TeachingAssignmentDialogData {
  offering: CourseOffering;
  /** Set when opened via a "reassign this offering" deep link (e.g. Skeleton Builder's Global
   *  Auto-Schedule capacity report) — shown as an informational hint once the suggested faculty's
   *  name resolves; the admin still has to pick them from the right row themselves and save.
   *  Deep links only ever target Theory contributions, so this only affects Theory rows. */
  suggestedFacultyId?: number | null;
}

interface TheoryUnitRow {
  kind: 'THEORY';
  key: string;
  unitLabel: string;
  source: SectionFacultyAssignment;
}

interface CoordinatorUnitRow {
  kind: 'LAB' | 'CLINICAL';
  key: string;
  unitLabel: string;
  source: Batch;
}

type TeachingUnitRow = TheoryUnitRow | CoordinatorUnitRow;

/** Single stop for all of an offering's teaching-staff decisions: who teaches Theory to each
 *  cohort/section, and who coordinates each Lab/Clinical batch — merged into one flat table so
 *  admins actually do the coordinator half instead of skipping it because it lived behind a
 *  separate button (Manage Batches). Backed by the same two APIs the old separate dialogs used
 *  (Section Faculty + Batches for this offering); no backend change. Batch roster/capacity/name
 *  and deactivation remain in Manage Batches — this dialog is assignment-only. Class Incharge
 *  stays fully separate: it's per-section, not per-offering, and term-wide rather than per-row. */
@Component({
  selector: 'app-teaching-assignment-dialog',
  standalone: true,
  imports: [FormsModule, MatDialogModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './teaching-assignment-dialog.component.html',
  styleUrl: './teaching-assignment-dialog.component.scss',
})
export class TeachingAssignmentDialogComponent implements OnInit {
  private readonly dialogRef = inject(MatDialogRef<TeachingAssignmentDialogComponent>);
  protected readonly data: TeachingAssignmentDialogData = inject(MAT_DIALOG_DATA);
  private readonly academicYearService = inject(AcademicYearService);
  private readonly batchService = inject(BatchService);
  private readonly permissionService = inject(PermissionService);
  private readonly toast = inject(ToastService);

  /** Theory dropdown is only interactive with SECTION_FACULTY_MANAGE, matching the permission the
   *  underlying PUT actually enforces (same gate the old Assign Faculty dialog used). */
  protected readonly canManageTheory = computed(() => this.permissionService.has('SECTION_FACULTY_MANAGE'));
  /** Coordinator dropdown is only interactive with BATCH_MANAGE (V273) — same gate Manage Batches
   *  already used. A row a user can't edit still shows, just disabled, so both permission holders
   *  see the whole picture even if they can only act on half of it. */
  protected readonly canManageCoordinator = computed(() => this.permissionService.has('BATCH_MANAGE'));

  protected readonly assignmentLoading = signal(false);
  protected readonly assignmentRows = signal<SectionFacultyAssignment[]>([]);
  protected readonly assignmentApplicable = signal(true);

  protected readonly batchesLoading = signal(false);
  protected readonly batches = signal<Batch[]>([]);

  /** Every eligible (Speciality match OR the subject's Eligible Faculty list) active faculty for
   *  this offering — backs both the Faculty Pool checklist and, unfiltered, the Coordinator
   *  picker (coordinator picks were never gated by the admin-curated pool, same as the old
   *  Manage Batches dialog — preserved as-is here). */
  protected readonly eligibleCandidates = signal<EligibleFacultyCandidate[]>([]);
  protected readonly eligibleCandidatesLoading = signal(false);

  protected readonly poolSelection = signal<Set<number>>(new Set());
  protected readonly poolSaving = signal(false);
  protected readonly poolDirty = computed(() => {
    const persisted = new Set(this.eligibleCandidates().filter((c) => c.inPool).map((c) => c.facultyId));
    const pending = this.poolSelection();
    if (persisted.size !== pending.size) return true;
    for (const id of pending) if (!persisted.has(id)) return true;
    return false;
  });

  protected readonly savingKey = signal<string | null>(null);

  protected readonly suggestedFacultyName = computed(() => {
    const id = this.data.suggestedFacultyId;
    if (id == null) return null;
    return this.eligibleCandidates().find((c) => c.facultyId === id)?.facultyName ?? null;
  });

  private readonly sectionCandidatesCache = signal<Map<number, EligibleFacultyCandidate[]>>(new Map());
  private readonly sectionCandidatesLoading = new Set<number>();
  private readonly cohortCandidatesCache = signal<Map<number, EligibleFacultyCandidate[]>>(new Map());
  private readonly cohortCandidatesLoading = new Set<number>();

  /** Theory rows first (base subject teaching), then Lab batches, then Clinical batches, each its
   *  own labeled section — same reading order the two separate dialogs presented before, just
   *  grouped so a busy offering with a dozen Lab/Clinical batches doesn't read as one undifferentiated
   *  list. Empty groups are dropped rather than shown with a "nothing here" placeholder. */
  protected readonly groupedRows = computed<{ kind: 'THEORY' | 'LAB' | 'CLINICAL'; label: string; rows: TeachingUnitRow[] }[]>(() => {
    const theoryRows: TeachingUnitRow[] = this.assignmentRows().map((r) => ({
      kind: 'THEORY',
      key: this.theoryRowKey(r),
      unitLabel: r.cohortName + (r.sectionLabel ? ' — ' + r.sectionLabel : ''),
      source: r,
    }));
    const sortedBatches = [...this.batches()].sort((a, b) => a.name.localeCompare(b.name, undefined, { numeric: true }));
    const labRows: TeachingUnitRow[] = sortedBatches
      .filter((b) => b.labId != null)
      .map((b) => ({ kind: 'LAB', key: `batch-${b.id}`, unitLabel: b.name, source: b }));
    const clinicalRows: TeachingUnitRow[] = sortedBatches
      .filter((b) => b.labId == null)
      .map((b) => ({ kind: 'CLINICAL', key: `batch-${b.id}`, unitLabel: b.name, source: b }));
    return [
      { kind: 'THEORY' as const, label: 'Theory', rows: theoryRows },
      { kind: 'LAB' as const, label: 'Lab Coordinators', rows: labRows },
      { kind: 'CLINICAL' as const, label: 'Clinical Coordinators', rows: clinicalRows },
    ].filter((g) => g.rows.length > 0);
  });

  protected readonly rows = computed<TeachingUnitRow[]>(() => this.groupedRows().flatMap((g) => g.rows));

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

    this.batchesLoading.set(true);
    this.batchService.getByCourseOffering(this.data.offering.id).subscribe({
      next: (batches) => { this.batches.set(batches); this.batchesLoading.set(false); },
      error: () => {
        this.batchesLoading.set(false);
        this.toast.error('Failed to load batches');
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

  protected toggleInPool(facultyId: number): void {
    this.poolSelection.update((current) => {
      const next = new Set(current);
      if (next.has(facultyId)) next.delete(facultyId); else next.add(facultyId);
      return next;
    });
  }

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

  private theoryRowKey(row: SectionFacultyAssignment): string {
    return row.cohortSectionId != null ? `section-${row.cohortSectionId}` : `cohort-${row.cohortId}`;
  }

  /** Candidate options for a row's dropdown — Theory rows are scoped to the persisted, section/
   *  cohort-eligible slice of the pool; Coordinator rows use the full unfiltered eligible list
   *  (unchanged from the old Manage Batches behavior). */
  protected candidatesFor(row: TeachingUnitRow): EligibleFacultyCandidate[] {
    if (row.kind !== 'THEORY') return this.eligibleCandidates();
    const raw = row.source.cohortSectionId != null
      ? this.sectionCandidatesRawFor(row.source.cohortSectionId)
      : this.cohortCandidatesRawFor(row.source.cohortId);
    return raw.filter((c) => c.inPool);
  }

  private sectionCandidatesRawFor(cohortSectionId: number): EligibleFacultyCandidate[] {
    const cached = this.sectionCandidatesCache().get(cohortSectionId);
    if (cached) return cached;
    if (!this.sectionCandidatesLoading.has(cohortSectionId)) {
      this.sectionCandidatesLoading.add(cohortSectionId);
      this.academicYearService.getEligibleFacultyForSection(this.data.offering.id, cohortSectionId).subscribe({
        next: (candidates) => {
          this.sectionCandidatesLoading.delete(cohortSectionId);
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
          this.cohortCandidatesLoading.delete(cohortId);
          this.cohortCandidatesCache.update((m) => new Map(m).set(cohortId, candidates));
        },
        error: () => { this.cohortCandidatesLoading.delete(cohortId); },
      });
    }
    return [];
  }

  protected candidateBadgeText(c: EligibleFacultyCandidate): string {
    if (c.viaEligibleList) return 'Eligible list';
    if (c.specialityMatch) return 'Speciality match';
    return 'Currently assigned';
  }

  protected candidateHoursText(c: EligibleFacultyCandidate): string {
    if (c.capacityTier === 'NONE') return 'No cap configured';
    if (c.overCapacity) return 'Over capacity';
    return `${Math.round(c.remainingHours * 10) / 10}h free`;
  }

  protected rowEditable(row: TeachingUnitRow): boolean {
    return row.kind === 'THEORY' ? this.canManageTheory() : this.canManageCoordinator();
  }

  protected currentFacultyId(row: TeachingUnitRow): number | null {
    return row.kind === 'THEORY' ? row.source.facultyId : row.source.coordinatorFacultyId;
  }

  protected onRowFacultyChange(row: TeachingUnitRow, facultyId: number | null): void {
    if (row.kind === 'THEORY') this.saveTheory(row.source, facultyId);
    else this.saveCoordinator(row.source, facultyId);
  }

  private saveTheory(row: SectionFacultyAssignment, facultyId: number | null): void {
    const key = this.theoryRowKey(row);
    this.savingKey.set(key);
    const request$ = row.cohortSectionId != null
      ? this.academicYearService.updateSectionFaculty(this.data.offering.id, row.cohortSectionId, facultyId)
      : this.academicYearService.updateCohortFaculty(this.data.offering.id, row.cohortId, facultyId);
    request$.subscribe({
      next: (updated) => {
        this.savingKey.set(null);
        this.assignmentRows.update((rows) => rows.map((r) => (this.theoryRowKey(r) === key ? updated : r)));
        this.toast.success(`${row.sectionLabel ?? row.cohortName} updated`);
        this.refreshCapacityFigures();
      },
      error: (err) => {
        this.savingKey.set(null);
        this.toast.error(violationText(err) ?? err?.error?.message ?? 'Failed to update faculty assignment');
      },
    });
  }

  /** Preserves the batch's current name/capacity — updateBatch is a full replace, and this dialog
   *  only ever changes coordinatorFacultyId. */
  private saveCoordinator(batch: Batch, facultyId: number | null): void {
    const key = `batch-${batch.id}`;
    this.savingKey.set(key);
    const request: BatchRequest = {
      courseOfferingId: this.data.offering.id,
      name: batch.name,
      capacity: batch.capacity,
      coordinatorFacultyId: facultyId,
    };
    this.batchService.update(batch.id, request).subscribe({
      next: (updated) => {
        this.savingKey.set(null);
        this.batches.update((rows) => rows.map((b) => (b.id === updated.id ? updated : b)));
        this.toast.success(`${batch.name} updated`);
        this.refreshCapacityFigures();
      },
      error: (err) => {
        this.savingKey.set(null);
        this.toast.error(err?.error?.message ?? 'Failed to update batch coordinator');
      },
    });
  }

  private refreshCapacityFigures(): void {
    this.sectionCandidatesCache.set(new Map());
    this.cohortCandidatesCache.set(new Map());
    this.academicYearService.getEligibleFaculty(this.data.offering.id).subscribe({
      next: (candidates) => this.eligibleCandidates.set(candidates),
    });
  }

  protected onClose(): void {
    this.dialogRef.close();
  }
}

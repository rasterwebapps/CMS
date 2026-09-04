import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { forkJoin, Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  CourseOffering,
  EligibleFacultyCandidate,
  SectionFacultyAssignment,
} from '../../academic-year/academic-year.model';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { Batch, BatchRequest, BatchStudent, describeImpact, impactHasAny } from '../../batch/batch.model';
import { BatchService } from '../../batch/batch.service';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { uniqueFieldValidator } from '../../../shared/validators/unique-field.validator';
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

interface BatchUnitRow {
  kind: 'LAB' | 'CLINICAL';
  key: string;
  unitLabel: string;
  source: Batch;
}

type TeachingUnitRow = TheoryUnitRow | BatchUnitRow;

/** Single stop for all of an offering's teaching-staff AND batch-admin decisions: who teaches
 *  Theory to each cohort/section, who coordinates each Lab/Clinical batch, each batch's own
 *  name/capacity/roster, and delete — merged from the former separate Manage Batches dialog
 *  (retired) so nothing about a batch requires leaving this screen. Theory faculty, coordinator
 *  faculty, and batch name/capacity are staged edits behind one Save button (validated as a whole
 *  before it enables); Delete/Roster stay as immediate actions with their own confirmation, since
 *  those are lifecycle actions rather than form fields. Delete is a real DELETE, not a soft flag --
 *  hard-blocked whenever the batch still has students or timetable data, so by the time it's
 *  allowed the row carries no history worth keeping; there is deliberately no Reactivate, since a
 *  hard-deleted batch has nothing to bring back (a mistaken delete means recreating the batch via
 *  Capacity Auto-Plan). Class Incharge stays fully separate: it's per-section, not per-offering,
 *  and term-wide rather than per-row. */
@Component({
  selector: 'app-teaching-assignment-dialog',
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule, RouterLink, MatDialogModule, MatIconModule, MatProgressSpinnerModule],
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
  private readonly dialog = inject(MatDialog);
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);

  /** Theory dropdown is only interactive with SECTION_FACULTY_MANAGE, matching the permission the
   *  underlying PUT actually enforces (same gate the old Assign Faculty dialog used). */
  protected readonly canManageTheory = computed(() => this.permissionService.has('SECTION_FACULTY_MANAGE'));
  /** Coordinator/name/capacity/roster/delete all gate on BATCH_MANAGE (V273) — same permission
   *  Manage Batches always used for batch admin, now exercised from this one screen instead. */
  protected readonly canManageBatch = computed(() => this.permissionService.has('BATCH_MANAGE'));

  protected readonly assignmentLoading = signal(false);
  protected readonly assignmentRows = signal<SectionFacultyAssignment[]>([]);
  protected readonly assignmentApplicable = signal(true);

  protected readonly batchesLoading = signal(false);
  /** Active only — a deleted batch is simply gone, and inactive rows from the (unrelated)
   *  automatic Cohort Room Allocation revert path are never surfaced here either (see
   *  BatchService.getBatchesForOffering on the backend). */
  protected readonly batches = signal<Batch[]>([]);

  protected readonly saving = signal(false);

  /** One reactive FormGroup per batch (name/capacity/coordinatorFacultyId), rebuilt whenever the
   *  batch list reloads. */
  protected readonly batchForms = signal<Map<number, FormGroup>>(new Map());
  /** Theory faculty has no validation to gate on, so it stays a simple staged-value map rather than
   *  a full reactive form — only the batch fields need FormGroup/async-validator machinery. */
  protected readonly theoryPending = signal<Map<string, number | null>>(new Map());

  protected readonly registeredStudents = signal<{ studentId: number; studentName: string }[]>([]);
  protected readonly expandedBatchId = signal<number | null>(null);
  protected readonly roster = signal<BatchStudent[]>([]);
  protected readonly rosterLoading = signal(false);

  /** Every eligible (Speciality match OR the subject's Eligible Faculty list) active faculty for
   *  this offering — backs the Theory row and Coordinator pickers directly. No pool-curation step
   *  in between: an offering automatically inherits its subject's eligible faculty the moment it
   *  exists, and every row (Theory or Coordinator) picks straight from that list. */
  protected readonly eligibleCandidates = signal<EligibleFacultyCandidate[]>([]);
  protected readonly eligibleCandidatesLoading = signal(false);

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
      { kind: 'LAB' as const, label: 'Lab Batches', rows: labRows },
      { kind: 'CLINICAL' as const, label: 'Clinical Batches', rows: clinicalRows },
    ].filter((g) => g.rows.length > 0);
  });

  protected readonly rows = computed<TeachingUnitRow[]>(() => this.groupedRows().flatMap((g) => g.rows));

  ngOnInit(): void {
    this.loadAssignment();
    this.loadBatches();

    this.academicYearService.getCourseRegistrationsByCourseOffering(this.data.offering.id).subscribe({
      next: (regs) => this.registeredStudents.set(regs.map((r) => ({ studentId: r.studentId, studentName: r.studentName }))),
      error: () => this.registeredStudents.set([]),
    });

    this.eligibleCandidatesLoading.set(true);
    this.academicYearService.getEligibleFaculty(this.data.offering.id).subscribe({
      next: (candidates) => {
        this.eligibleCandidates.set(candidates);
        this.eligibleCandidatesLoading.set(false);
      },
      error: () => { this.eligibleCandidatesLoading.set(false); },
    });
  }

  private loadAssignment(): void {
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
  }

  private loadBatches(): void {
    this.batchesLoading.set(true);
    this.batchService.getByCourseOffering(this.data.offering.id).subscribe({
      next: (batches) => {
        this.batches.set(batches);
        this.buildBatchForms();
        this.batchesLoading.set(false);
      },
      error: () => {
        this.batchesLoading.set(false);
        this.toast.error('Failed to load batches');
      },
    });
  }

  /** Rebuilt fresh on every load/reload so each capacity control's floor tracks that batch's
   *  current enrolledCount, and the name-uniqueness async validator always excludes the right id. */
  private buildBatchForms(): void {
    const map = new Map<number, FormGroup>();
    for (const b of this.batches()) {
      const group = this.fb.group({
        name: [b.name, [Validators.required]],
        capacity: [b.capacity, [Validators.required, Validators.min(Math.max(1, b.enrolledCount))]],
        coordinatorFacultyId: [b.coordinatorFacultyId],
      });
      group.get('name')!.setAsyncValidators(
        uniqueFieldValidator(
          this.http,
          `${environment.apiUrl}/batches/name-exists`,
          () => b.id,
          () => ({ courseOfferingId: this.data.offering.id }),
        ),
      );
      group.get('name')!.updateValueAndValidity({ emitEvent: false });
      map.set(b.id, group);
    }
    this.batchForms.set(map);
  }

  protected batchFormGroup(batchId: number): FormGroup | undefined {
    return this.batchForms().get(batchId);
  }

  private theoryRowKey(row: SectionFacultyAssignment): string {
    return row.cohortSectionId != null ? `section-${row.cohortSectionId}` : `cohort-${row.cohortId}`;
  }

  /** Candidate options for a row's dropdown — Theory rows are scoped to the section/cohort-eligible
   *  list (subject-derived eligibility, grandfathering whoever currently holds that exact row);
   *  Coordinator rows use the full offering-level eligible list. Neither goes through a separate
   *  curation step — every offering automatically inherits its subject's eligible faculty. */
  protected candidatesFor(row: TeachingUnitRow): EligibleFacultyCandidate[] {
    if (row.kind !== 'THEORY') return this.eligibleCandidates();
    return row.source.cohortSectionId != null
      ? this.sectionCandidatesRawFor(row.source.cohortSectionId)
      : this.cohortCandidatesRawFor(row.source.cohortId);
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
    if (c.currentlyAssigned) return 'Currently assigned';
    if (c.viaEligibleList) return 'Eligible list';
    if (c.specialityMatch) return 'Speciality match';
    return 'Active faculty';
  }

  /** Live preview of what Save would commit: each row's hour cost matches the backend's own
   *  attribution exactly (TimetableGlobalAutoScheduleService#termHoursForOfferingInCohort/
   *  #batchHours) -- a Theory row owes offering.theoryHours per section, a Lab-linked batch owes
   *  offering.labHours, a Clinical-linked batch owes offering.clinicalHours, never divided across
   *  batches. Reassigning a row away from its persisted holder frees their hours; assigning it to
   *  someone new (pending, not yet saved) charges them -- so a faculty picked for several rows in
   *  this session sees their free-hours figure drop with each pick, before Save. */
  private computeHourAdjustments(): Map<number, number> {
    const deltas = new Map<number, number>();
    const apply = (facultyId: number | null, hours: number) => {
      if (facultyId == null || hours <= 0) return;
      deltas.set(facultyId, (deltas.get(facultyId) ?? 0) + hours);
    };

    for (const [key, pendingFacultyId] of this.theoryPending()) {
      const row = this.assignmentRows().find((r) => this.theoryRowKey(r) === key);
      if (!row || pendingFacultyId === row.facultyId) continue;
      apply(row.facultyId, this.data.offering.theoryHours);
      apply(pendingFacultyId, -this.data.offering.theoryHours);
    }

    for (const [batchId, group] of this.batchForms()) {
      const batch = this.batches().find((b) => b.id === batchId);
      if (!batch) continue;
      const pendingFacultyId = group.get('coordinatorFacultyId')?.value ?? null;
      if (pendingFacultyId === batch.coordinatorFacultyId) continue;
      const hours = batch.labId != null ? this.data.offering.labHours : this.data.offering.clinicalHours;
      apply(batch.coordinatorFacultyId, hours);
      apply(pendingFacultyId, -hours);
    }

    return deltas;
  }

  protected candidateHoursText(c: EligibleFacultyCandidate): string {
    if (c.capacityTier === 'NONE') return 'No cap configured';
    const adjusted = c.remainingHours + (this.computeHourAdjustments().get(c.facultyId) ?? 0);
    if (adjusted < 0) return 'Over capacity';
    return `${Math.round(adjusted * 10) / 10}h free`;
  }

  protected rowEditable(row: TeachingUnitRow): boolean {
    return row.kind === 'THEORY' ? this.canManageTheory() : this.canManageBatch();
  }

  protected currentTheoryFacultyId(row: TheoryUnitRow): number | null {
    const pending = this.theoryPending().get(row.key);
    return pending !== undefined ? pending : row.source.facultyId;
  }

  protected onTheoryFacultyChange(row: TheoryUnitRow, facultyId: number | null): void {
    this.theoryPending.update((m) => {
      const next = new Map(m);
      if (facultyId === row.source.facultyId) next.delete(row.key); else next.set(row.key, facultyId);
      return next;
    });
  }

  /** Whether anything staged (Theory picks or any batch field) differs from what's persisted. */
  protected isDirty(): boolean {
    if (this.theoryPending().size > 0) return true;
    for (const group of this.batchForms().values()) if (group.dirty) return true;
    return false;
  }

  /** `pending` covers the async name-uniqueness check still in flight — Save stays disabled until
   *  it resolves, not just until the synchronous validators pass. */
  protected isInvalid(): boolean {
    for (const group of this.batchForms().values()) if (group.invalid || group.pending) return true;
    return false;
  }

  protected canSave(): boolean {
    return this.isDirty() && !this.isInvalid() && !this.saving();
  }

  protected saveAll(): void {
    if (!this.canSave()) return;
    const calls: Observable<unknown>[] = [];

    for (const [key, facultyId] of this.theoryPending()) {
      const row = this.assignmentRows().find((r) => this.theoryRowKey(r) === key);
      if (!row) continue;
      calls.push(
        row.cohortSectionId != null
          ? this.academicYearService.updateSectionFaculty(this.data.offering.id, row.cohortSectionId, facultyId, row.version)
          : this.academicYearService.updateCohortFaculty(this.data.offering.id, row.cohortId, facultyId, row.version),
      );
    }

    for (const [batchId, group] of this.batchForms()) {
      if (!group.dirty) continue;
      const v = group.value;
      const currentBatch = this.batches().find((b) => b.id === batchId);
      const request: BatchRequest = {
        courseOfferingId: this.data.offering.id,
        name: (v.name ?? '').trim(),
        capacity: v.capacity,
        coordinatorFacultyId: v.coordinatorFacultyId ?? null,
        version: currentBatch?.version ?? 0,
      };
      calls.push(this.batchService.update(batchId, request));
    }

    if (calls.length === 0) return;

    this.saving.set(true);
    forkJoin(calls).subscribe({
      next: () => {
        this.saving.set(false);
        this.theoryPending.set(new Map());
        this.toast.success('Changes saved');
        this.reloadAll();
      },
      error: (err) => {
        this.saving.set(false);
        this.toast.error(violationText(err) ?? err?.error?.message ?? 'Failed to save — reloading to show what actually went through');
        this.reloadAll();
      },
    });
  }

  private reloadAll(): void {
    this.loadAssignment();
    this.loadBatches();
    this.refreshCapacityFigures();
  }

  private refreshCapacityFigures(): void {
    this.sectionCandidatesCache.set(new Map());
    this.cohortCandidatesCache.set(new Map());
    this.academicYearService.getEligibleFaculty(this.data.offering.id).subscribe({
      next: (candidates) => this.eligibleCandidates.set(candidates),
    });
  }

  protected toggleRoster(batch: Batch): void {
    if (this.expandedBatchId() === batch.id) {
      this.expandedBatchId.set(null);
      return;
    }
    this.expandedBatchId.set(batch.id);
    this.refreshRoster(batch.id);
  }

  protected isInRoster(studentId: number): boolean {
    return this.roster().some((s) => s.studentId === studentId);
  }

  protected toggleStudent(batch: Batch, studentId: number): void {
    const call = this.isInRoster(studentId)
      ? this.batchService.removeStudent(batch.id, studentId)
      : this.batchService.addStudent(batch.id, studentId);
    call.subscribe({
      next: () => {
        this.refreshRoster(batch.id);
        this.loadBatches();
      },
      error: (err) => this.toast.error(err?.error?.message ?? 'Failed to update roster'),
    });
  }

  private refreshRoster(batchId: number): void {
    this.rosterLoading.set(true);
    this.batchService.getRoster(batchId).subscribe({
      next: (roster) => { this.roster.set(roster); this.rosterLoading.set(false); },
      error: () => { this.roster.set([]); this.rosterLoading.set(false); },
    });
  }

  /** Checked before Delete is even offered a confirmation — hard-blocked (not just warned)
   *  whenever anything is still attached, so the confirm dialog never appears for a batch that's
   *  actually going to be refused. A confirmed delete is permanent: there is no Reactivate. */
  protected deleteBatch(batch: Batch): void {
    this.batchService.getLifecycleImpact(batch.id).subscribe({
      next: (impact) => {
        if (impactHasAny(impact)) {
          this.toast.error(`Cannot delete "${batch.name}" — it still has ${describeImpact(impact)}. Remove them first.`);
          return;
        }
        this.dialog.open(ConfirmDialogComponent, {
          data: {
            title: 'Delete Batch',
            message: `Permanently delete "${batch.name}"? It has no students or timetable data attached, so this is safe, but it cannot be undone.`,
            confirmText: 'Delete',
            cancelText: 'Cancel',
          },
        }).afterClosed().subscribe((confirmed) => {
          if (!confirmed) return;
          this.batchService.deleteBatch(batch.id).subscribe({
            next: () => { this.toast.success(`${batch.name} deleted`); this.reloadAll(); },
            error: (err) => this.toast.error(err?.error?.message ?? 'Failed to delete batch'),
          });
        });
      },
      error: () => this.toast.error('Failed to check batch usage before deleting'),
    });
  }

  protected onClose(): void {
    this.dialogRef.close();
  }
}

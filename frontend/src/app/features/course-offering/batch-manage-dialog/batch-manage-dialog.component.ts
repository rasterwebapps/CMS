import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CourseOffering, EligibleFacultyCandidate } from '../../academic-year/academic-year.model';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { Batch, BatchRequest, BatchStudent } from '../../batch/batch.model';
import { BatchService } from '../../batch/batch.service';
import { ToastService } from '../../../core/toast/toast.service';

export interface BatchManageDialogData {
  offering: CourseOffering;
}

@Component({
  selector: 'app-batch-manage-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatIconModule, MatProgressSpinnerModule, MatTooltipModule],
  templateUrl: './batch-manage-dialog.component.html',
  styleUrl: './batch-manage-dialog.component.scss',
})
export class BatchManageDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<BatchManageDialogComponent>);
  protected readonly data: BatchManageDialogData = inject(MAT_DIALOG_DATA);
  private readonly batchService = inject(BatchService);
  private readonly academicYearService = inject(AcademicYearService);
  private readonly toast = inject(ToastService);

  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly batches = signal<Batch[]>([]);
  protected readonly registeredStudents = signal<{ studentId: number; studentName: string }[]>([]);
  protected readonly editingBatchId = signal<number | null>(null);
  protected readonly showForm = signal(false);
  protected readonly expandedBatchId = signal<number | null>(null);
  protected readonly roster = signal<BatchStudent[]>([]);
  protected readonly rosterLoading = signal(false);
  /** OC-183: coordinator picker now offers the offering's real eligible-faculty pool (same
   *  source the Assign Faculty screen uses, subject-speciality filtered) rather than the flat
   *  all-faculty list. One faculty coordinating multiple batches is fine — the backend only
   *  blocks a genuine same-time double-booking once schedules are actually placed. */
  protected readonly eligibleFaculty = signal<EligibleFacultyCandidate[]>([]);

  /** Edit-only (OC-191) -- name/capacity/coordinator for a batch Capacity Auto-Plan already
   *  created with a real Lab/Clinical venue attached. Manual batch *creation* was removed: it
   *  produced a permanently venue-less batch (no UI path ever attaches one after the fact), which
   *  the auto-scheduler can't tell is meant to be Lab or Clinical and can never assign a real room
   *  to -- a scheduling dead end. Batches must originate from committing a room allocation in
   *  Capacity Auto-Plan. */
  protected readonly form: FormGroup = this.fb.group({
    name: ['', Validators.required],
    capacity: [20, [Validators.required, Validators.min(1)]],
    coordinatorFacultyId: [null],
  });

  constructor() {
    this.loadBatches();
    this.academicYearService.getCourseRegistrationsByCourseOffering(this.data.offering.id).subscribe({
      next: (regs) => this.registeredStudents.set(
        regs.map((r) => ({ studentId: r.studentId, studentName: r.studentName }))),
      error: () => this.registeredStudents.set([]),
    });
    this.academicYearService.getEligibleFaculty(this.data.offering.id).subscribe({
      next: (candidates) => this.eligibleFaculty.set(candidates),
      error: () => this.eligibleFaculty.set([]),
    });
  }

  private loadBatches(): void {
    this.loading.set(true);
    this.batchService.getByCourseOffering(this.data.offering.id).subscribe({
      next: (batches) => { this.batches.set(batches); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load batches'); this.loading.set(false); },
    });
  }

  protected startEdit(batch: Batch): void {
    this.editingBatchId.set(batch.id);
    this.form.reset({
      name: batch.name,
      capacity: batch.capacity,
      coordinatorFacultyId: batch.coordinatorFacultyId,
    });
    this.showForm.set(true);
  }

  protected cancelForm(): void {
    this.showForm.set(false);
    this.editingBatchId.set(null);
  }

  protected submitBatch(): void {
    const editingId = this.editingBatchId();
    if (this.form.invalid || editingId == null) return;
    const v = this.form.value;
    const request: BatchRequest = {
      courseOfferingId: this.data.offering.id,
      name: v.name.trim(),
      capacity: v.capacity,
      coordinatorFacultyId: v.coordinatorFacultyId ?? null,
    };
    this.saving.set(true);
    this.batchService.update(editingId, request).subscribe({
      next: () => {
        this.toast.success('Batch updated');
        this.saving.set(false);
        this.showForm.set(false);
        this.editingBatchId.set(null);
        this.loadBatches();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to save batch');
        this.saving.set(false);
      },
    });
  }

  protected deactivate(batch: Batch): void {
    this.batchService.deactivate(batch.id).subscribe({
      next: () => { this.toast.success('Batch deactivated'); this.loadBatches(); },
      error: (err) => this.toast.error(err?.error?.message ?? 'Failed to deactivate batch'),
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

  protected onClose(): void {
    this.dialogRef.close();
  }
}

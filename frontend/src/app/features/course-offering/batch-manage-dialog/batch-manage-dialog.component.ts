import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CourseOffering } from '../../academic-year/academic-year.model';
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

  /** Groups by the batch's real `cohortSectionId`/`sectionLabel` (Capacity Auto-Plan always sets
   *  this — see the Batch creation hard gate) rather than parsing it out of the batch name, which
   *  is only a display convention. A batch with no section (shouldn't happen post-gate, but covers
   *  any pre-gate leftover) falls into a trailing "Other" bucket instead of being dropped. Section
   *  order follows first appearance, numerically-sorted ("Section 2" before "Section 10"). */
  protected readonly groupedBatches = computed<{ label: string; batches: Batch[] }[]>(() => {
    const groups = new Map<string, Batch[]>();
    for (const batch of this.batches()) {
      const label = batch.sectionLabel ?? 'Other';
      (groups.get(label) ?? groups.set(label, []).get(label)!).push(batch);
    }
    return [...groups.entries()]
      .sort(([a], [b]) => a.localeCompare(b, undefined, { numeric: true }))
      .map(([label, batches]) => ({ label, batches }));
  });
  protected readonly registeredStudents = signal<{ studentId: number; studentName: string }[]>([]);
  protected readonly editingBatchId = signal<number | null>(null);
  protected readonly showForm = signal(false);
  protected readonly expandedBatchId = signal<number | null>(null);
  protected readonly roster = signal<BatchStudent[]>([]);
  protected readonly rosterLoading = signal(false);

  /** Edit-only (OC-191) -- name/capacity for a batch Capacity Auto-Plan already created with a
   *  real Lab/Clinical venue attached. Manual batch *creation* was removed: it produced a
   *  permanently venue-less batch (no UI path ever attaches one after the fact), which the
   *  auto-scheduler can't tell is meant to be Lab or Clinical and can never assign a real room to
   *  -- a scheduling dead end. Batches must originate from committing a room allocation in
   *  Capacity Auto-Plan. Coordinator assignment moved to the Assign Faculty dialog -- kept out of
   *  this form so there's only one place that writes Batch.coordinatorFaculty. */
  protected readonly form: FormGroup = this.fb.group({
    name: ['', Validators.required],
    capacity: [20, [Validators.required, Validators.min(1)]],
  });

  constructor() {
    this.loadBatches();
    this.academicYearService.getCourseRegistrationsByCourseOffering(this.data.offering.id).subscribe({
      next: (regs) => this.registeredStudents.set(
        regs.map((r) => ({ studentId: r.studentId, studentName: r.studentName }))),
      error: () => this.registeredStudents.set([]),
    });
  }

  private loadBatches(): void {
    this.loading.set(true);
    this.batchService.getByCourseOffering(this.data.offering.id).subscribe({
      next: (batches) => { this.batches.set(batches); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load batches'); this.loading.set(false); },
    });
  }

  /** Preserved separately from the form -- this dialog no longer edits it, but the save still has
   *  to resend the batch's current value since updateBatch is a full replace. */
  private editingCoordinatorFacultyId: number | null = null;

  protected startEdit(batch: Batch): void {
    this.editingBatchId.set(batch.id);
    this.editingCoordinatorFacultyId = batch.coordinatorFacultyId;
    this.form.reset({
      name: batch.name,
      capacity: batch.capacity,
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
      coordinatorFacultyId: this.editingCoordinatorFacultyId,
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

import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CourseOffering } from '../../academic-year/academic-year.model';
import { Batch } from '../../batch/batch.model';
import { BatchService } from '../../batch/batch.service';
import {
  ClinicalShiftGroup,
  ClinicalShiftGroupRequest,
  ClinicalShiftTheoryBlockRequest,
  DayOfWeek,
} from '../../clinical-shift-group/clinical-shift-group.model';
import { ClinicalShiftGroupService } from '../../clinical-shift-group/clinical-shift-group.service';
import { EscortRotationDialogComponent, EscortRotationDialogData } from '../escort-rotation-dialog/escort-rotation-dialog.component';
import { ToastService } from '../../../core/toast/toast.service';

export interface ClinicalShiftGroupDialogData {
  offering: CourseOffering;
}

const DAYS: DayOfWeek[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];

/** Admin setup for OC-175 Piece 2: a recurring off-campus clinical shift window spanning several
 *  clinical Batches (different venues) + one shared, reconvened theory class. Requires the
 *  offering's clinical shift duration to already be configured (see Assign Faculty's per-offering
 *  "Clinical Shift Config" action) -- the clinical block's end time is derived from that, not set
 *  here per group. */
@Component({
  selector: 'app-clinical-shift-group-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatIconModule, MatProgressSpinnerModule, MatTooltipModule],
  templateUrl: './clinical-shift-group-dialog.component.html',
  styleUrl: './clinical-shift-group-dialog.component.scss',
})
export class ClinicalShiftGroupDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<ClinicalShiftGroupDialogComponent>);
  protected readonly data: ClinicalShiftGroupDialogData = inject(MAT_DIALOG_DATA);
  private readonly shiftGroupService = inject(ClinicalShiftGroupService);
  private readonly batchService = inject(BatchService);
  private readonly dialog = inject(MatDialog);
  private readonly toast = inject(ToastService);

  protected readonly days = DAYS;
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly groups = signal<ClinicalShiftGroup[]>([]);
  protected readonly allBatches = signal<Batch[]>([]);
  protected readonly showForm = signal(false);

  protected readonly form: FormGroup = this.fb.group({
    label: ['', Validators.required],
    dayOfWeek: ['MONDAY' as DayOfWeek, Validators.required],
    clinicalStartTime: ['07:00', Validators.required],
  });

  protected readonly configured = this.data.offering.clinicalShiftDurationMinutes != null;
  protected readonly editingBlocksGroupId = signal<number | null>(null);
  protected readonly blockDrafts = signal<{ startTime: string; endTime: string }[]>([]);
  protected readonly generateDate = signal<Record<number, string>>({});
  protected readonly generating = signal<number | null>(null);

  constructor() {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.shiftGroupService.getForOffering(this.data.offering.id).subscribe({
      next: (groups) => {
        this.groups.set(groups);
        this.batchService.getByCourseOffering(this.data.offering.id).subscribe({
          next: (batches) => { this.allBatches.set(batches); this.loading.set(false); },
          error: () => { this.allBatches.set([]); this.loading.set(false); },
        });
      },
      error: () => { this.toast.error('Failed to load clinical shift groups'); this.loading.set(false); },
    });
  }

  /** Batches with a clinical venue that aren't already linked to this specific group -- the
   *  candidate pool for that group's "Link Batch" picker. */
  protected linkableBatches(group: ClinicalShiftGroup): Batch[] {
    const linkedIds = new Set(group.batches.map((b) => b.batchId));
    return this.allBatches().filter((b) => b.clinicalVenueId != null && !linkedIds.has(b.id));
  }

  protected startAdd(): void {
    this.form.reset({ label: '', dayOfWeek: 'MONDAY', clinicalStartTime: '07:00' });
    this.showForm.set(true);
  }

  protected cancelForm(): void {
    this.showForm.set(false);
  }

  protected submitGroup(): void {
    if (this.form.invalid) return;
    const v = this.form.value;
    const request: ClinicalShiftGroupRequest = {
      courseOfferingId: this.data.offering.id,
      label: v.label.trim(),
      dayOfWeek: v.dayOfWeek,
      clinicalStartTime: v.clinicalStartTime,
    };
    this.saving.set(true);
    this.shiftGroupService.create(request).subscribe({
      next: () => {
        this.toast.success('Shift group created');
        this.saving.set(false);
        this.showForm.set(false);
        this.load();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to create shift group');
        this.saving.set(false);
      },
    });
  }

  protected deactivateGroup(group: ClinicalShiftGroup): void {
    this.shiftGroupService.deactivate(group.id).subscribe({
      next: () => { this.toast.success('Shift group deactivated'); this.load(); },
      error: (err) => this.toast.error(err?.error?.message ?? 'Failed to deactivate shift group'),
    });
  }

  protected linkBatch(group: ClinicalShiftGroup, batchId: number): void {
    if (!batchId) return;
    this.shiftGroupService.linkBatch(group.id, batchId).subscribe({
      next: () => { this.toast.success('Batch linked'); this.load(); },
      error: (err) => this.toast.error(err?.error?.message ?? 'Failed to link batch'),
    });
  }

  protected unlinkBatch(group: ClinicalShiftGroup, batchId: number): void {
    this.shiftGroupService.unlinkBatch(group.id, batchId).subscribe({
      next: () => { this.toast.success('Batch unlinked'); this.load(); },
      error: (err) => this.toast.error(err?.error?.message ?? 'Failed to unlink batch'),
    });
  }

  /** Theory blocks default to the offering's own subject -- the common case for a reconvened
   *  class; picking a different subject per block is not exposed in this v1 editor. */
  protected startEditBlocks(group: ClinicalShiftGroup): void {
    this.editingBlocksGroupId.set(group.id);
    this.blockDrafts.set(
      group.theoryBlocks.length > 0
        ? group.theoryBlocks.map((b) => ({ startTime: b.startTime, endTime: b.endTime }))
        : [{ startTime: '14:00', endTime: '17:00' }]
    );
  }

  protected cancelEditBlocks(): void {
    this.editingBlocksGroupId.set(null);
  }

  protected addBlockDraft(): void {
    this.blockDrafts.set([...this.blockDrafts(), { startTime: '', endTime: '' }]);
  }

  protected removeBlockDraft(index: number): void {
    this.blockDrafts.set(this.blockDrafts().filter((_, i) => i !== index));
  }

  protected updateBlockDraft(index: number, field: 'startTime' | 'endTime', value: string): void {
    const drafts = [...this.blockDrafts()];
    drafts[index] = { ...drafts[index], [field]: value };
    this.blockDrafts.set(drafts);
  }

  protected saveBlocks(group: ClinicalShiftGroup): void {
    const requests: ClinicalShiftTheoryBlockRequest[] = this.blockDrafts()
      .filter((b) => b.startTime && b.endTime)
      .map((b, i) => ({
        sequenceOrder: i,
        startTime: b.startTime,
        endTime: b.endTime,
        subjectId: this.data.offering.subjectId,
      }));
    if (requests.length === 0) {
      this.toast.error('Add at least one theory block');
      return;
    }
    this.saving.set(true);
    this.shiftGroupService.replaceTheoryBlocks(group.id, requests).subscribe({
      next: () => {
        this.toast.success('Theory blocks saved');
        this.saving.set(false);
        this.editingBlocksGroupId.set(null);
        this.load();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to save theory blocks');
        this.saving.set(false);
      },
    });
  }

  protected setGenerateDate(groupId: number, date: string): void {
    this.generateDate.set({ ...this.generateDate(), [groupId]: date });
  }

  /** Materializes this group's clinical (per linked batch) + shared theory occurrences for one
   *  date -- the attendance-capable rows attendance marking actually keys off. Idempotent server-
   *  side, so re-running for an already-generated date is safe. */
  protected generateForDate(group: ClinicalShiftGroup): void {
    const date = this.generateDate()[group.id];
    if (!date) {
      this.toast.error('Pick a date first');
      return;
    }
    this.generating.set(group.id);
    this.shiftGroupService.generateForDate(group.id, date).subscribe({
      next: (count) => {
        this.toast.success(`Generated ${count} occurrence(s) for ${date}`);
        this.generating.set(null);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to generate occurrences');
        this.generating.set(null);
      },
    });
  }

  protected openEscortRotation(batchId: number, batchName: string): void {
    const data: EscortRotationDialogData = { batchId, batchName };
    this.dialog.open(EscortRotationDialogComponent, { data, width: '480px' });
  }

  protected onClose(): void {
    this.dialogRef.close();
  }
}

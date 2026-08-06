import { Component, computed, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Period } from '../../period/period.model';
import { Batch } from '../../batch/batch.model';
import { BatchService } from '../../batch/batch.service';
import { RotationGroupService } from './rotation.service';
import { RotationCandidateSlot, RotationGroupCreateRequest, RotationMemberInput } from './rotation.model';
import { CmsFlyoutPanelComponent } from '../../../shared/flyout-panel/flyout-panel.component';
import { ToastService } from '../../../core/toast/toast.service';

type Step = 'PICK_SLOTS' | 'ASSIGN_MEMBERS';

/**
 * Sets up a week-parity Rotation Group: pick 2+ already-placed skeleton cells sharing one
 * day+period (e.g. "English Lab, Wed P3-4" + "Tamil Lab, Wed P3-4"), then declare which existing
 * per-subject Batch represents each physical group ("Batch 1", "Batch 2", ...) at each slot.
 * Follows the same "select source -> fetch candidates -> confirm" shape as
 * StaffSessionSwapComponent, generalized from a 2-way pairing to N slots/N members.
 *
 * The day-swap rotation shape needs none of this — it's just independently-placed cells across
 * two days, each with its own fixed batch, already supported by the plain placement flow.
 */
@Component({
  selector: 'app-rotation-setup-flyout',
  standalone: true,
  imports: [FormsModule, MatProgressSpinnerModule, CmsFlyoutPanelComponent],
  templateUrl: './rotation-setup-flyout.component.html',
  styleUrl: './rotation-setup-flyout.component.scss',
})
export class RotationSetupFlyoutComponent {
  private readonly rotationService = inject(RotationGroupService);
  private readonly batchService = inject(BatchService);
  private readonly toast = inject(ToastService);

  readonly termInstanceId = input.required<number>();
  readonly periods = input.required<Period[]>();
  readonly days = input.required<readonly string[]>();
  readonly dayLabels = input.required<Record<string, string>>();

  readonly closed = output<void>();
  readonly saved = output<void>();

  protected readonly step = signal<Step>('PICK_SLOTS');
  protected selectedDay: string | null = null;
  protected selectedPeriodId: number | null = null;

  protected readonly loadingCandidates = signal(false);
  protected readonly candidates = signal<RotationCandidateSlot[]>([]);
  protected readonly selectedSlotIds = signal<Set<number>>(new Set());

  protected readonly selectedSlots = computed(() =>
    this.candidates().filter((c) => this.selectedSlotIds().has(c.classScheduleId)));

  protected label = '';
  protected anchorDate = '';
  protected readonly memberLabels = signal<string[]>(['Batch 1', 'Batch 2']);
  /** memberIndex -> classScheduleId -> batchId */
  protected readonly assignments = signal<Record<number, Record<number, number | null>>>({});
  protected readonly batchOptionsBySlot = signal<Record<number, Batch[]>>({});
  protected readonly loadingBatchOptions = signal(false);
  protected readonly saving = signal(false);

  protected readonly requiredDayLabel = computed(() => {
    const slots = this.selectedSlots();
    return slots.length ? this.dayLabels()[slots[0].dayOfWeek] : '';
  });

  protected findCandidates(): void {
    if (!this.selectedDay || !this.selectedPeriodId) return;
    this.loadingCandidates.set(true);
    this.selectedSlotIds.set(new Set());
    this.rotationService.candidateSlots(this.termInstanceId(), this.selectedDay, this.selectedPeriodId).subscribe({
      next: (list) => { this.candidates.set(list); this.loadingCandidates.set(false); },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to load candidate cells');
        this.loadingCandidates.set(false);
      },
    });
  }

  protected toggleSlot(classScheduleId: number): void {
    const next = new Set(this.selectedSlotIds());
    if (next.has(classScheduleId)) next.delete(classScheduleId);
    else next.add(classScheduleId);
    this.selectedSlotIds.set(next);
  }

  protected canProceedToAssignments(): boolean {
    return this.selectedSlots().length >= 2;
  }

  protected proceedToAssignments(): void {
    const slots = this.selectedSlots();
    const memberCount = slots.length; // a rotation group is always a square rotation: N slots, N members
    this.memberLabels.set(Array.from({ length: memberCount }, (_, i) => `Batch ${i + 1}`));

    const initial: Record<number, Record<number, number | null>> = {};
    for (let m = 0; m < memberCount; m++) {
      initial[m] = {};
      for (const slot of slots) {
        // Member 0 defaults to whatever batch this cell was originally placed with.
        initial[m][slot.classScheduleId] = m === 0 ? slot.batchId : null;
      }
    }
    this.assignments.set(initial);

    this.loadingBatchOptions.set(true);
    const uniqueOfferingIds = [...new Set(slots.map((s) => s.courseOfferingId).filter((id): id is number => id != null))];
    let remaining = uniqueOfferingIds.length;
    if (remaining === 0) { this.loadingBatchOptions.set(false); }
    const bySlot: Record<number, Batch[]> = {};
    for (const offeringId of uniqueOfferingIds) {
      this.batchService.getByCourseOffering(offeringId).subscribe({
        next: (batches) => {
          for (const slot of slots.filter((s) => s.courseOfferingId === offeringId)) {
            bySlot[slot.classScheduleId] = batches;
          }
          remaining -= 1;
          if (remaining === 0) { this.batchOptionsBySlot.set(bySlot); this.loadingBatchOptions.set(false); }
        },
        error: () => {
          remaining -= 1;
          if (remaining === 0) { this.batchOptionsBySlot.set(bySlot); this.loadingBatchOptions.set(false); }
        },
      });
    }

    this.step.set('ASSIGN_MEMBERS');
  }

  protected backToSlotPicker(): void {
    this.step.set('PICK_SLOTS');
  }

  protected updateMemberLabel(index: number, value: string): void {
    const next = [...this.memberLabels()];
    next[index] = value;
    this.memberLabels.set(next);
  }

  protected updateAssignment(memberIndex: number, classScheduleId: number, batchId: number | null): void {
    const next = { ...this.assignments() };
    next[memberIndex] = { ...next[memberIndex], [classScheduleId]: batchId };
    this.assignments.set(next);
  }

  protected canSave(): boolean {
    if (!this.label.trim() || !this.anchorDate) return false;
    const slots = this.selectedSlots();
    const memberAssignments = this.assignments();
    for (let m = 0; m < this.memberLabels().length; m++) {
      if (!this.memberLabels()[m]?.trim()) return false;
      for (const slot of slots) {
        if (!memberAssignments[m]?.[slot.classScheduleId]) return false;
      }
    }
    return true;
  }

  protected save(): void {
    if (!this.canSave()) return;
    const slots = this.selectedSlots();
    const members: RotationMemberInput[] = this.memberLabels().map((label, memberOrder) => ({
      memberOrder,
      label,
      assignments: slots.map((slot) => ({
        classScheduleId: slot.classScheduleId,
        batchId: this.assignments()[memberOrder][slot.classScheduleId] as number,
      })),
    }));

    const request: RotationGroupCreateRequest = {
      termInstanceId: this.termInstanceId(),
      label: this.label,
      anchorOccurrenceDate: this.anchorDate,
      slots: slots.map((slot, slotOrder) => ({ classScheduleId: slot.classScheduleId, slotOrder })),
      members,
    };

    this.saving.set(true);
    this.rotationService.create(request).subscribe({
      next: () => {
        this.toast.success('Rotation group created');
        this.saving.set(false);
        this.saved.emit();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to create rotation group');
        this.saving.set(false);
      },
    });
  }

  protected onClose(): void {
    this.closed.emit();
  }
}

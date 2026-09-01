import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';

import { PermissionService } from '../../../../core/permissions/permission.service';
import { ToastService } from '../../../../core/toast/toast.service';
import { CapacityPlannerService } from '../capacity-planner.service';
import { VenueRebalancePreview } from '../capacity-planner.model';

/**
 * "Rebalance now" — inline preview-then-confirm panel for moving the minimum number of
 * already-committed batches off an over/tight-capacity Lab/Clinical venue onto a better-fitting
 * eligible one. Self-contained: fetches its own preview, applies on confirm, emits `rebalanced`
 * so the host screen can refresh its own capacity data — never assumes what that refresh looks
 * like, since the two hosts (Global Auto-Schedule precheck, Capacity Planner) refresh differently.
 *
 * Usage: <cms-venue-rebalance-panel [termInstanceId]="..." [sessionType]="v.venueType"
 *          [venueId]="v.venueId" (rebalanced)="onRecheck()" />
 */
@Component({
  selector: 'cms-venue-rebalance-panel',
  standalone: true,
  templateUrl: './venue-rebalance-panel.component.html',
  styleUrl: './venue-rebalance-panel.component.scss',
})
export class VenueRebalancePanelComponent {
  private readonly capacityPlannerService = inject(CapacityPlannerService);
  private readonly toast = inject(ToastService);
  private readonly permissionService = inject(PermissionService);

  @Input({ required: true }) termInstanceId!: number;
  @Input({ required: true }) sessionType!: 'LAB' | 'CLINICAL';
  @Input({ required: true }) venueId!: number;

  /** Fires once `applyRebalance` succeeds — the host is responsible for re-fetching whatever
   *  capacity data it shows (Re-check on the precheck panel, the plan/term-overview reload on
   *  Capacity Planner). This component never assumes which. */
  @Output() readonly rebalanced = new EventEmitter<void>();

  protected readonly expanded = signal(false);
  protected readonly loading = signal(false);
  protected readonly applying = signal(false);
  protected readonly preview = signal<VenueRebalancePreview | null>(null);
  protected readonly errorMessage = signal<string | null>(null);

  protected canRebalance(): boolean {
    return this.permissionService.has('TIMETABLE_CAPACITY_PLANNER_REBALANCE');
  }

  protected open(): void {
    this.expanded.set(true);
    this.errorMessage.set(null);
    this.loading.set(true);
    this.capacityPlannerService.previewRebalance(this.termInstanceId, this.sessionType, this.venueId).subscribe({
      next: (preview) => {
        this.loading.set(false);
        this.preview.set(preview);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err?.error?.message ?? 'Failed to load rebalance preview');
      },
    });
  }

  protected cancel(): void {
    this.expanded.set(false);
    this.preview.set(null);
    this.errorMessage.set(null);
  }

  protected confirm(): void {
    const p = this.preview();
    if (!p || p.willMove.length === 0 || this.applying()) return;
    this.applying.set(true);
    const batchIds = p.willMove.map((m) => m.batchId);
    this.capacityPlannerService.applyRebalance(this.termInstanceId, this.sessionType, this.venueId, batchIds).subscribe({
      next: (result) => {
        this.applying.set(false);
        this.expanded.set(false);
        this.preview.set(null);
        this.toast.success(
          `Moved ${result.batchesMoved} batch(es), cleared ${result.sessionsCleared} session(s) for re-placement — run Run Automation to re-place them.`,
        );
        this.rebalanced.emit();
      },
      error: (err) => {
        this.applying.set(false);
        this.errorMessage.set(err?.error?.message ?? 'Failed to apply rebalance');
      },
    });
  }
}

import { Component, computed, inject, signal } from '@angular/core';
import { TitleCasePipe } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { SkeletonPlannedMove, SkeletonRelocationPlan } from '../skeleton-builder.model';
import { WEEK_GRID_DAY_LABELS } from '../../../../shared/week-grid/week-grid.model';

export interface SkeletonCellSwapDialogData {
  title: string;
  subtitle: string;
  /** Every window from the relocation preview (the Swap menu) — the dialog lists the legal ones. */
  options: SkeletonRelocationPlan[];
  /** A window already picked (a drag, or a Clinical duty day): the dialog only confirms it. */
  chosen: SkeletonRelocationPlan | null;
  periodNames: Record<number, string>;
  confirmText: string;
}

export interface SkeletonCellSwapDialogResult {
  plan: SkeletonRelocationPlan;
}

/** One selectable window, pre-formatted so the template stays declarative. */
interface WindowOption {
  key: string;
  plan: SkeletonRelocationPlan;
  dayLabel: string;
  slotLabel: string;
  kind: 'MOVE' | 'SWAP';
  detail: string;
}

/** Move or swap a session — whole block included, with every parallel batch — and the before →
 *  after preview of everything that moves, confirmed before anything is applied.
 *
 *  <p>From the Swap menu it lists every legal window the relocation preview found: MOVE into empty
 *  periods, or SWAP with the sessions there, which take this session's periods in the same order.
 *  Dragging does the same thing and is quicker when both places are on screen; this covers
 *  far-apart slots and is keyboard-reachable. After a drop, or a dragged Clinical duty banner, it
 *  only shows the preview for the one choice already made. */
@Component({
  selector: 'app-skeleton-cell-swap-dialog',
  standalone: true,
  imports: [TitleCasePipe, MatDialogModule],
  templateUrl: './skeleton-cell-swap-dialog.component.html',
  styleUrl: './skeleton-cell-swap-dialog.component.scss',
})
export class SkeletonCellSwapDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<SkeletonCellSwapDialogComponent>);
  protected readonly data: SkeletonCellSwapDialogData = inject(MAT_DIALOG_DATA);

  protected readonly selectedKey = signal<string | null>(null);

  /** Legal windows only, in the backend's order — down the week, then through the day, the same
   *  order the grid is laid out in. */
  protected readonly options = computed<WindowOption[]>(() =>
    this.data.options
      .filter((plan) => plan.valid)
      .map((plan) => ({
        key: `${plan.dayOfWeek}|${plan.startPeriodId}`,
        plan,
        dayLabel: this.dayLabel(plan.dayOfWeek),
        slotLabel: this.runLabel(plan.periodIds),
        kind: plan.kind === 'SWAP' ? 'SWAP' : 'MOVE',
        detail: plan.kind === 'SWAP'
          ? 'with ' + plan.moves.slice(1).map((m) => m.subjectCode).join(', ')
          : 'empty periods',
      } satisfies WindowOption)));

  /** Grouped for the template so each day gets a heading instead of one flat list. */
  protected readonly optionsByDay = computed(() => {
    const groups = new Map<string, WindowOption[]>();
    for (const o of this.options()) {
      const list = groups.get(o.dayLabel);
      if (list) list.push(o);
      else groups.set(o.dayLabel, [o]);
    }
    return [...groups.entries()].map(([dayLabel, items]) => ({ dayLabel, items }));
  });

  protected readonly selectedPlan = computed<SkeletonRelocationPlan | null>(() =>
    this.data.chosen ?? this.options().find((o) => o.key === this.selectedKey())?.plan ?? null);

  protected dayLabel(day: string): string {
    return WEEK_GRID_DAY_LABELS[day] ?? day;
  }

  /** "Period 3" or "Period 3–Period 4": a block's run of periods as the grid names them. */
  protected runLabel(periodIds: number[]): string {
    const names = periodIds.map((id) => this.data.periodNames[id] ?? `#${id}`);
    return names.length <= 1 ? (names[0] ?? '') : `${names[0]}–${names[names.length - 1]}`;
  }

  protected moveFrom(move: SkeletonPlannedMove): string {
    return `${this.dayLabel(move.fromDay)} ${this.runLabel(move.fromPeriodIds)}`;
  }

  protected moveTo(move: SkeletonPlannedMove): string {
    return `${this.dayLabel(move.toDay)} ${this.runLabel(move.toPeriodIds)}`;
  }

  protected onSave(): void {
    const plan = this.selectedPlan();
    if (!plan) return;
    this.dialogRef.close({ plan } satisfies SkeletonCellSwapDialogResult);
  }

  protected onCancel(): void {
    this.dialogRef.close();
  }
}

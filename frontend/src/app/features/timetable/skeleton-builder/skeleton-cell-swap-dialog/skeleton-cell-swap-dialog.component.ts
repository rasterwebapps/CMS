import { Component, computed, inject, signal } from '@angular/core';
import { TitleCasePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { SkeletonCell } from '../skeleton-builder.model';
import { WEEK_GRID_DAYS, WEEK_GRID_DAY_LABELS } from '../../../../shared/week-grid/week-grid.model';

export interface SkeletonCellSwapDialogData {
  /** The cell the menu was opened on — the one whose slot is being offered up. */
  cell: SkeletonCell;
  /** Every cell currently loaded for this cohort; the dialog filters to legal partners itself. */
  cells: SkeletonCell[];
}

export interface SkeletonCellSwapDialogResult {
  targetCellId: number;
}

/** One selectable swap partner, pre-formatted so the template stays declarative. */
interface SwapOption {
  cellId: number;
  dayOfWeek: string;
  dayLabel: string;
  slotName: string;
  startTime: string;
  subjectCode: string;
  sessionType: string;
  occupantLabel: string | null;
}

/** Exchange two placed sessions' day/period — each keeps its own subject, faculty and students,
 *  they simply trade times.
 *
 *  <p>Dragging one cell onto another already does this, and remains the quicker gesture for two
 *  cells sitting near each other. This exists for the case dragging is bad at: a precise exchange
 *  between cells far apart on the grid, or off-screen entirely, without a long drag across a
 *  scrolling table — and it is reachable from the keyboard, which a drag is not.
 *
 *  <p>Partners are not pre-validated. The move-preview endpoint that powers drag highlighting
 *  judges each slot as if moving into it, so it marks every OCCUPIED slot invalid — and a swap
 *  partner is occupied by definition, which is the whole point. Validating on submit instead keeps
 *  this consistent with Replace and Reassign, and surfaces the real reason through the same
 *  violation toast rather than silently hiding a candidate. */
@Component({
  selector: 'app-skeleton-cell-swap-dialog',
  standalone: true,
  imports: [FormsModule, TitleCasePipe, MatDialogModule],
  templateUrl: './skeleton-cell-swap-dialog.component.html',
  styleUrl: './skeleton-cell-swap-dialog.component.scss',
})
export class SkeletonCellSwapDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<SkeletonCellSwapDialogComponent>);
  protected readonly data: SkeletonCellSwapDialogData = inject(MAT_DIALOG_DATA);

  protected readonly selectedTargetId = signal<number | null>(null);

  /** Every other cell this one may legally trade slots with, mirroring what {@code swapCells}
   *  accepts: DRAFT, not itself, and not part of a multi-period session — the backend refuses
   *  those outright ("a multi-period session can't be swapped here yet"), so offering one would
   *  guarantee a rejection.
   *
   *  <p>Electives are excluded as well. Every member of an elective group must sit in the same
   *  slot, so moving one member anywhere else trips {@code checkElectiveGroupSlot}
   *  ("SKELETON_ELECTIVE_GROUP_SLOT_MISMATCH") without exception — its siblings stay put and the
   *  group splits. Offering one would be offering a guaranteed rejection. Place Elective Block
   *  moves the whole group's slot together, which is what that actually calls for.
   *
   *  <p>Cells belonging to a different section are deliberately kept: two sections exchanging
   *  times is a perfectly ordinary thing to want, and each side keeps its own audience. The label
   *  names the section so the choice is never ambiguous. */
  protected readonly options = computed<SwapOption[]>(() => {
    const source = this.data.cell;
    return this.data.cells
      .filter((c) => c.id !== source.id && c.status === 'DRAFT' && c.sessionGroupId == null
        && c.electiveGroupId == null)
      .map((c) => ({
        cellId: c.id,
        dayOfWeek: c.dayOfWeek,
        dayLabel: WEEK_GRID_DAY_LABELS[c.dayOfWeek] ?? c.dayOfWeek,
        slotName: c.slotName,
        startTime: c.startTime,
        subjectCode: c.subjectCode,
        sessionType: c.sessionType,
        occupantLabel: c.cohortSectionLabel ?? c.batchName,
      } satisfies SwapOption))
      // Reading order: down the week, then through the day — the same order the grid itself is
      // laid out in, so scanning the list feels like scanning the grid.
      .sort((a, b) => WEEK_GRID_DAYS.indexOf(a.dayOfWeek) - WEEK_GRID_DAYS.indexOf(b.dayOfWeek)
        || a.startTime.localeCompare(b.startTime));
  });

  /** Grouped for the template so each day gets a heading instead of one flat list of 25+ rows. */
  protected readonly optionsByDay = computed(() => {
    const groups = new Map<string, SwapOption[]>();
    for (const o of this.options()) {
      const list = groups.get(o.dayLabel);
      if (list) list.push(o);
      else groups.set(o.dayLabel, [o]);
    }
    return [...groups.entries()].map(([dayLabel, items]) => ({ dayLabel, items }));
  });

  protected readonly selectedOption = computed(() =>
    this.options().find((o) => o.cellId === this.selectedTargetId()) ?? null);

  /** The source cell's day in the SAME short form the list headings and target rows use. The
   *  confirmation sentence names both sides, and mixing forms there ("moves to Tue … moves to
   *  Thursday") reads like two different things are being described. */
  protected readonly sourceDayLabel = computed(() =>
    WEEK_GRID_DAY_LABELS[this.data.cell.dayOfWeek] ?? this.data.cell.dayOfWeek);

  protected readonly canSave = computed(() => this.selectedTargetId() != null);

  /** The backend rejects a multi-period session on either side, so if the cell the menu was opened
   *  on is itself multi-period there is nothing to offer at all. The grid gates the menu item on
   *  this too; the message here is the backstop. */
  protected readonly sourceIsMultiPeriod = computed(() => this.data.cell.sessionGroupId != null);

  protected onSave(): void {
    const targetCellId = this.selectedTargetId();
    if (targetCellId == null) return;
    this.dialogRef.close({ targetCellId } satisfies SkeletonCellSwapDialogResult);
  }

  protected onCancel(): void {
    this.dialogRef.close();
  }
}

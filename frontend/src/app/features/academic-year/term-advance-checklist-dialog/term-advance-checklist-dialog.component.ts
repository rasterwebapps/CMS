import { Component, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AcademicYearService } from '../academic-year.service';
import { TermInstanceStatus } from '../academic-year.model';
import { ToastService } from '../../../core/toast/toast.service';

export interface TermAdvanceChecklistDialogData {
  termInstanceId: number;
  termType: 'ODD' | 'EVEN';
  targetStatus: TermInstanceStatus;
  academicYearName: string;
}

interface ChecklistItem {
  label: string;
  /** null = purely self-attested, no system signal exists to judge it either way. */
  warn: boolean | null;
  checked: boolean;
}

/** Replaces the old single-paragraph ConfirmDialogComponent for term-status advancement —
 *  BR-53 shipped that dialog, but with how much now depends on term status (course offerings,
 *  fee demand generation/collection eligibility, and the timetable freezing once LOCKED), a
 *  single warning paragraph isn't enough for an action that's permanent either way (neither
 *  PLANNED→OPEN nor OPEN→LOCKED has a reverse path in the backend). Checklist items are
 *  system-verified where a reliable signal exists, but the hard block is on ticking every item +
 *  a separate final acknowledgment, never on the checks themselves passing -- two of the real
 *  dependencies (exam results published, fees "finalized") have no system signal anywhere in the
 *  codebase, and a lockout tied to incomplete logic would be worse than trusting the admin's own
 *  judgment once they've been shown the real numbers. */
@Component({
  selector: 'app-term-advance-checklist-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule, MatCheckboxModule, MatProgressSpinnerModule],
  templateUrl: './term-advance-checklist-dialog.component.html',
  styleUrl: './term-advance-checklist-dialog.component.scss',
})
export class TermAdvanceChecklistDialogComponent implements OnInit {
  protected readonly dialogRef = inject(MatDialogRef<TermAdvanceChecklistDialogComponent>);
  protected readonly data: TermAdvanceChecklistDialogData = inject(MAT_DIALOG_DATA);
  private readonly academicYearService = inject(AcademicYearService);
  private readonly toast = inject(ToastService);

  protected readonly loading = signal(true);
  protected readonly items = signal<ChecklistItem[]>([]);
  protected readonly acknowledged = signal(false);

  protected readonly title = this.data.targetStatus === 'OPEN' ? 'Open Term' : 'Lock Term';

  protected readonly introLines: string[] = this.buildIntroLines();

  ngOnInit(): void {
    this.academicYearService.getTermAdvanceChecklist(this.data.termInstanceId, this.data.targetStatus).subscribe({
      next: (checklist) => {
        this.items.set(this.buildItems(checklist));
        this.loading.set(false);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to load the pre-check for this action');
        this.dialogRef.close(false);
      },
    });
  }

  private buildIntroLines(): string[] {
    const termLabel = `${this.data.termType} term for ${this.data.academicYearName}`;
    if (this.data.targetStatus === 'OPEN') {
      return [
        `Opening the ${termLabel} generates course offerings from the curriculum, making it available for course registration and fee collection.`,
        'This cannot be undone or reverted once opened.',
      ];
    }
    return [
      `Locking the ${termLabel} deactivates all its course offerings and freezes its timetable — placement, staffing, approve, discard, and revert-to-draft all stop working for it.`,
      'This is permanent. It cannot be undone or reverted once locked.',
    ];
  }

  private buildItems(checklist: { cohortsWithoutCurriculum: string[]; outstandingFeeDemandCount: number; outstandingFeeDemandAmount: number; draftTimetableSessionCount: number }): ChecklistItem[] {
    if (this.data.targetStatus === 'OPEN') {
      if (checklist.cohortsWithoutCurriculum.length > 0) {
        return checklist.cohortsWithoutCurriculum.map((name) => ({
          label: `"${name}" has no curriculum version mapped — 0 offerings will be generated for it`,
          warn: true,
          checked: false,
        }));
      }
      return [{ label: 'Every active cohort has a curriculum version mapped', warn: false, checked: false }];
    }

    const items: ChecklistItem[] = [];
    items.push(checklist.outstandingFeeDemandCount > 0
      ? { label: `${checklist.outstandingFeeDemandCount} outstanding fee demand(s) totalling ₹${checklist.outstandingFeeDemandAmount.toLocaleString('en-IN')}`, warn: true, checked: false }
      : { label: 'No outstanding fee demands for this term', warn: false, checked: false });
    items.push(checklist.draftTimetableSessionCount > 0
      ? { label: `${checklist.draftTimetableSessionCount} timetable session(s) still in Draft — once locked, they can no longer be approved, edited, or discarded`, warn: true, checked: false }
      : { label: 'No unapproved (draft) timetable sessions for this term', warn: false, checked: false });
    // No system signal exists anywhere in the codebase for exam-result publication against a
    // term -- self-attested rather than pretending it's verified.
    items.push({ label: 'Exam results are published and finalized for this term', warn: null, checked: false });
    return items;
  }

  protected toggleItem(index: number): void {
    this.items.update((list) => list.map((item, i) => (i === index ? { ...item, checked: !item.checked } : item)));
  }

  protected toggleAcknowledged(): void {
    this.acknowledged.update((v) => !v);
  }

  protected canConfirm(): boolean {
    return !this.loading() && this.acknowledged() && this.items().every((item) => item.checked);
  }

  protected onCancel(): void {
    this.dialogRef.close(false);
  }

  protected onConfirm(): void {
    if (!this.canConfirm()) return;
    this.dialogRef.close(true);
  }
}

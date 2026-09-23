import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { DecimalPipe, TitleCasePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { TimetableCoverageGap } from '../timetable.model';

export interface CoverageOverrideDialogData {
  gaps: TimetableCoverageGap[];
}

/** Shown when Approve refuses because one or more cohorts still have curriculum-required Theory/
 *  Lab/Clinical hours never placed as real sessions (OC-256) — only ever opened for a reviewer who
 *  already holds TIMETABLE_APPROVE_INCOMPLETE_OVERRIDE (see timetable-builder.component.ts's
 *  publishCohorts), since anyone else has no path to satisfy this dialog anyway. A separate small
 *  dialog rather than extending ConfirmDialogComponent for the same reason
 *  BlockAvailabilityDialogComponent is: its afterClosed() contract is a plain boolean used by many
 *  other callers. Returns the trimmed reason string on confirm, or null on cancel. */
@Component({
  selector: 'app-coverage-override-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, DecimalPipe, TitleCasePipe, MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>Approve With Incomplete Coverage?</h2>
    <mat-dialog-content>
      <p class="coverage-override-dialog__summary">
        This term still has curriculum-required hours that were never scheduled. Approving now publishes it as-is —
        the gaps below stay unscheduled until someone places them in Timetable Builder.
      </p>
      <ul class="coverage-override-dialog__gap-list">
        @for (gap of data.gaps; track gap.cohortId + gap.sessionType) {
          <li class="coverage-override-dialog__gap-row">
            <span class="coverage-override-dialog__gap-cohort">{{ gap.cohortName }}</span>
            <span class="text-muted">{{ gap.sessionType | titlecase }}</span>
            <span class="coverage-override-dialog__gap-unassigned">{{ gap.unassignedHours | number: '1.0-1' }}h of {{ gap.totalHours | number: '1.0-1' }}h unscheduled</span>
          </li>
        }
      </ul>
      <form [formGroup]="form">
        <div class="field-group">
          <label for="coverage-override-reason" class="field-label">Reason <span class="required-star">*</span></label>
          <textarea id="coverage-override-reason" class="field-textarea" formControlName="reason" rows="3"
            placeholder="Why is it acceptable to publish this term with these gaps still unscheduled?"
            [class.field-textarea--error]="form.get('reason')?.invalid && form.get('reason')?.touched"></textarea>
          @if (form.get('reason')?.hasError('required') && form.get('reason')?.touched) {
            <p class="field-error">A reason is required to approve with incomplete coverage</p>
          }
        </div>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button (click)="onCancel()">Cancel</button>
      <button mat-flat-button color="warn" [disabled]="form.invalid" (click)="onConfirm()">Approve Anyway</button>
    </mat-dialog-actions>
  `,
  styles: `
    .coverage-override-dialog__summary {
      margin: 0 0 12px;
      color: var(--mat-sys-on-surface-variant);
    }
    .coverage-override-dialog__gap-list {
      list-style: none;
      margin: 0 0 16px;
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    .coverage-override-dialog__gap-row {
      display: flex;
      gap: 8px;
      align-items: baseline;
      flex-wrap: wrap;
    }
    .coverage-override-dialog__gap-cohort {
      font-weight: 600;
    }
    .coverage-override-dialog__gap-unassigned {
      color: var(--cms-error, #ef4444);
      font-weight: 600;
    }
  `,
})
export class CoverageOverrideDialogComponent {
  protected readonly dialogRef = inject(MatDialogRef<CoverageOverrideDialogComponent>);
  protected readonly data: CoverageOverrideDialogData = inject(MAT_DIALOG_DATA);
  private readonly fb = inject(FormBuilder);

  protected readonly form: FormGroup = this.fb.group({
    reason: ['', [Validators.required, Validators.maxLength(500)]],
  });

  protected onCancel(): void {
    this.dialogRef.close(null);
  }

  protected onConfirm(): void {
    if (this.form.invalid) {
      this.form.get('reason')?.markAsTouched();
      return;
    }
    this.dialogRef.close((this.form.value.reason as string).trim());
  }
}

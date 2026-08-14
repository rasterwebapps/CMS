import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';

export interface BlockAvailabilityDialogData {
  facultyName: string;
  dayLabel: string;
  periodLabel: string;
}

export interface BlockAvailabilityDialogResult {
  reason: string;
  startDate: string | null;
  endDate: string | null;
}

/** Collects the required reason (and optional date range) before a Faculty Availability block is
 *  created. Deliberately a separate small dialog rather than extending the generic
 *  ConfirmDialogComponent -- that component's afterClosed() contract is a plain boolean, used by
 *  many other screens, and adding input fields would mean changing what every existing caller
 *  receives back. */
@Component({
  selector: 'app-block-availability-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>Block This Period</h2>
    <mat-dialog-content>
      <p class="block-availability-dialog__summary">
        {{ data.facultyName }} — {{ data.dayLabel }}, {{ data.periodLabel }}
      </p>
      <form [formGroup]="form">
        <div class="field-group">
          <label for="block-reason" class="field-label">Reason <span class="required-star">*</span></label>
          <textarea id="block-reason" class="field-textarea" formControlName="reason" rows="3"
            placeholder="Why is this faculty member unavailable at this time?"
            [class.field-textarea--error]="form.get('reason')?.invalid && form.get('reason')?.touched"></textarea>
          @if (form.get('reason')?.hasError('required') && form.get('reason')?.touched) {
            <p class="field-error">A reason is required to block a period</p>
          }
        </div>

        <div class="field-group">
          <label for="block-recurrence" class="field-label">Applies</label>
          <select id="block-recurrence" class="field-select" formControlName="recurrenceMode">
            <option value="recurring">Recurring (no end date)</option>
            <option value="ranged">For a date range (selected weeks)</option>
          </select>
        </div>

        @if (form.value.recurrenceMode === 'ranged') {
          <div class="field-row">
            <div class="field-group">
              <label for="block-start-date" class="field-label">Start Date <span class="required-star">*</span></label>
              <input id="block-start-date" type="date" class="field-input" formControlName="startDate"
                [class.field-input--error]="form.get('startDate')?.invalid && form.get('startDate')?.touched" />
            </div>
            <div class="field-group" style="margin-bottom: 0;">
              <label for="block-end-date" class="field-label">End Date <span class="required-star">*</span></label>
              <input id="block-end-date" type="date" class="field-input" formControlName="endDate"
                [class.field-input--error]="form.get('endDate')?.invalid && form.get('endDate')?.touched" />
            </div>
          </div>
          @if (form.hasError('endBeforeStart')) {
            <p class="field-error">End date must not be before start date</p>
          }
        }
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button (click)="onCancel()">Cancel</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid" (click)="onConfirm()">Block</button>
    </mat-dialog-actions>
  `,
  styles: `
    .block-availability-dialog__summary {
      margin: 0 0 12px;
      color: var(--mat-sys-on-surface-variant);
    }
  `,
})
export class BlockAvailabilityDialogComponent {
  protected readonly dialogRef = inject(MatDialogRef<BlockAvailabilityDialogComponent>);
  protected readonly data: BlockAvailabilityDialogData = inject(MAT_DIALOG_DATA);
  private readonly fb = inject(FormBuilder);

  protected readonly form: FormGroup = this.fb.group({
    reason: ['', [Validators.required, Validators.maxLength(255)]],
    recurrenceMode: ['recurring'],
    startDate: [''],
    endDate: [''],
  }, { validators: (group) => {
    const mode = group.get('recurrenceMode')?.value;
    if (mode !== 'ranged') return null;
    const start = group.get('startDate')?.value;
    const end = group.get('endDate')?.value;
    if (!start || !end) return { rangeRequired: true };
    return end < start ? { endBeforeStart: true } : null;
  } });

  constructor() {
    this.form.get('recurrenceMode')?.valueChanges.subscribe((mode: string) => {
      const startCtrl = this.form.get('startDate');
      const endCtrl = this.form.get('endDate');
      if (mode === 'ranged') {
        startCtrl?.setValidators(Validators.required);
        endCtrl?.setValidators(Validators.required);
      } else {
        startCtrl?.clearValidators();
        endCtrl?.clearValidators();
        startCtrl?.setValue('');
        endCtrl?.setValue('');
      }
      startCtrl?.updateValueAndValidity({ emitEvent: false });
      endCtrl?.updateValueAndValidity({ emitEvent: false });
    });
  }

  protected onCancel(): void {
    this.dialogRef.close(null);
  }

  protected onConfirm(): void {
    if (this.form.invalid) {
      this.form.get('reason')?.markAsTouched();
      this.form.get('startDate')?.markAsTouched();
      this.form.get('endDate')?.markAsTouched();
      return;
    }
    const v = this.form.value;
    const result: BlockAvailabilityDialogResult = {
      reason: (v.reason as string).trim(),
      startDate: v.recurrenceMode === 'ranged' ? v.startDate : null,
      endDate: v.recurrenceMode === 'ranged' ? v.endDate : null,
    };
    this.dialogRef.close(result);
  }
}

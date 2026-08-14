import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';

export interface BlockAvailabilityDialogData {
  facultyName: string;
  dayLabel: string;
  periodLabel: string;
}

/** Collects the required reason before a Faculty Availability block is created. Deliberately a
 *  separate small dialog rather than extending the generic ConfirmDialogComponent -- that
 *  component's afterClosed() contract is a plain boolean, used by many other screens, and adding
 *  an input field would mean changing what every existing caller receives back. */
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
        <div class="field-group" style="margin-bottom: 0;">
          <label for="block-reason" class="field-label">Reason <span class="required-star">*</span></label>
          <textarea id="block-reason" class="field-textarea" formControlName="reason" rows="3"
            placeholder="Why is this faculty member unavailable at this time?"
            [class.field-textarea--error]="form.get('reason')?.invalid && form.get('reason')?.touched"></textarea>
          @if (form.get('reason')?.hasError('required') && form.get('reason')?.touched) {
            <p class="field-error">A reason is required to block a period</p>
          }
        </div>
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

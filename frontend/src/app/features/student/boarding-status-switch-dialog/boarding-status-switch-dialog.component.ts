import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { StudentService } from '../student.service';
import { BoardingStatusSwitchAnalysis, StudentTypeValue, TermFeeOverrideInput } from '../student.model';
import { ToastService } from '../../../core/toast/toast.service';
import { InrPipe } from '../../../shared/pipes/inr.pipe';

export interface BoardingStatusSwitchDialogData {
  studentId: number;
  studentName: string;
  currentStudentType: StudentTypeValue;
}

type Step = 'REVIEW' | 'EDIT_FEES' | 'CONFIRM';

interface TermFeeRowModel {
  semesterNumber: number;
  yearOfStudy: number;
  defaultAmount: number | null;
  editedAmount: number | null;
}

@Component({
  selector: 'app-boarding-status-switch-dialog',
  standalone: true,
  imports: [
    FormsModule,
    MatButtonModule,
    MatDialogModule,
    MatIconModule,
    MatProgressSpinnerModule,
    InrPipe,
  ],
  templateUrl: './boarding-status-switch-dialog.component.html',
  styleUrl: './boarding-status-switch-dialog.component.scss',
})
export class BoardingStatusSwitchDialogComponent implements OnInit {
  private readonly dialogRef = inject(MatDialogRef<BoardingStatusSwitchDialogComponent>);
  readonly data: BoardingStatusSwitchDialogData = inject(MAT_DIALOG_DATA);
  private readonly studentService = inject(StudentService);
  private readonly toast = inject(ToastService);

  protected step = signal<Step>('REVIEW');
  protected loading = signal(true);
  protected submitting = signal(false);
  protected analysis = signal<BoardingStatusSwitchAnalysis | null>(null);
  protected termFeeRows = signal<TermFeeRowModel[]>([]);

  protected remarks = '';

  protected readonly targetStudentType: StudentTypeValue =
    this.data.currentStudentType === 'HOSTELER' ? 'DAY_SCHOLAR' : 'HOSTELER';

  ngOnInit(): void {
    this.studentService.analyzeBoardingStatusSwitch(this.data.studentId, this.targetStudentType).subscribe({
      next: (analysis) => {
        this.analysis.set(analysis);
        this.termFeeRows.set(
          analysis.termFees.map((row) => ({
            semesterNumber: row.semesterNumber,
            yearOfStudy: row.yearOfStudy,
            defaultAmount: row.existingOverride ?? row.calculatedAmount,
            editedAmount: row.existingOverride ?? row.calculatedAmount,
          }))
        );
        this.loading.set(false);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to analyse boarding status switch');
        this.loading.set(false);
      },
    });
  }

  protected proceedToEditFees(): void {
    this.step.set('EDIT_FEES');
  }

  protected proceedToConfirm(): void {
    this.step.set('CONFIRM');
  }

  protected back(): void {
    this.step.set(this.step() === 'CONFIRM' ? 'EDIT_FEES' : 'REVIEW');
  }

  protected onFeeAmountChange(row: TermFeeRowModel): void {
    // A negative number can still reach here via paste/autofill even with the keydown guard —
    // revert to the row's last valid value instead of letting it through to submission, where
    // the backend's @Positive check would otherwise surface as a raw validation-message toast.
    if (row.editedAmount != null && row.editedAmount < 0) {
      row.editedAmount = row.defaultAmount;
    }
  }

  protected blockNegativeKey(event: KeyboardEvent): void {
    if (event.key === '-' || event.key === 'Minus') {
      event.preventDefault();
    }
  }

  protected totalAmount(): number {
    return this.termFeeRows().reduce((sum, row) => sum + (row.editedAmount ?? row.defaultAmount ?? 0), 0);
  }

  protected executeSwitch(): void {
    this.submitting.set(true);
    const termFeeOverrides: TermFeeOverrideInput[] = this.termFeeRows()
      .filter((row) => row.editedAmount != null && row.editedAmount > 0 && row.editedAmount !== row.defaultAmount)
      .map((row) => ({ semesterNumber: row.semesterNumber, amount: row.editedAmount! }));

    this.studentService
      .executeBoardingStatusSwitch(this.data.studentId, {
        newStudentType: this.targetStudentType,
        remarks: this.remarks.trim() || undefined,
        termFeeOverrides: termFeeOverrides.length > 0 ? termFeeOverrides : undefined,
      })
      .subscribe({
        next: (record) => {
          this.submitting.set(false);
          this.dialogRef.close(record);
        },
        error: (err) => {
          this.toast.error(err?.error?.message ?? 'Failed to switch boarding status');
          this.submitting.set(false);
        },
      });
  }

  protected label(type: StudentTypeValue): string {
    return type === 'HOSTELER' ? 'Hosteler' : 'Day Scholar';
  }
}

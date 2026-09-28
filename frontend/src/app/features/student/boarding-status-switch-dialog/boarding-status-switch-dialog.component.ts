import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { StudentService } from '../student.service';
import { BoardingStatusSwitchAnalysis, StudentTypeValue } from '../student.model';
import { ToastService } from '../../../core/toast/toast.service';
import { InrPipe } from '../../../shared/pipes/inr.pipe';

export interface BoardingStatusSwitchDialogData {
  studentId: number;
  studentName: string;
  currentStudentType: StudentTypeValue;
}

type Step = 'REVIEW' | 'CONFIRM';

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

  protected remarks = '';

  protected readonly targetStudentType: StudentTypeValue =
    this.data.currentStudentType === 'HOSTELER' ? 'DAY_SCHOLAR' : 'HOSTELER';

  ngOnInit(): void {
    this.studentService.analyzeBoardingStatusSwitch(this.data.studentId, this.targetStudentType).subscribe({
      next: (analysis) => {
        this.analysis.set(analysis);
        this.loading.set(false);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to analyse boarding status switch');
        this.loading.set(false);
      },
    });
  }

  protected proceedToConfirm(): void {
    this.step.set('CONFIRM');
  }

  protected back(): void {
    this.step.set('REVIEW');
  }

  protected executeSwitch(): void {
    this.submitting.set(true);
    this.studentService
      .executeBoardingStatusSwitch(this.data.studentId, {
        newStudentType: this.targetStudentType,
        remarks: this.remarks.trim() || undefined,
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

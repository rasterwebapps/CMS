import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { StudentService } from '../student.service';
import { ProgramTransferAnalysis, ProgramTransferDocumentInfo } from '../student.model';
import { Program } from '../../program/program.model';
import { ToastService } from '../../../core/toast/toast.service';

export interface ProgramTransferDialogData {
  studentId: number;
  studentName: string;
  currentProgramId: number;
  currentProgramName: string;
  programs: Program[];
}

type Step = 'SELECT' | 'REVIEW' | 'CONFIRM';

@Component({
  selector: 'app-program-transfer-dialog',
  standalone: true,
  imports: [
    FormsModule,
    MatButtonModule,
    MatDialogModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatFormFieldModule,
    MatCheckboxModule,
  ],
  templateUrl: './program-transfer-dialog.component.html',
  styleUrl: './program-transfer-dialog.component.scss',
})
export class ProgramTransferDialogComponent implements OnInit {
  private readonly dialogRef = inject(MatDialogRef<ProgramTransferDialogComponent>);
  readonly data: ProgramTransferDialogData = inject(MAT_DIALOG_DATA);
  private readonly studentService = inject(StudentService);
  private readonly toast = inject(ToastService);

  protected step = signal<Step>('SELECT');
  protected loading = signal(false);
  protected submitting = signal(false);

  protected selectedProgramId: number | null = null;
  protected analysis = signal<ProgramTransferAnalysis | null>(null);

  protected consentConfirmed = false;
  protected notes = '';

  protected get availablePrograms(): Program[] {
    return this.data.programs.filter((p) => p.id !== this.data.currentProgramId);
  }

  ngOnInit(): void {}

  protected analyzeTransfer(): void {
    if (!this.selectedProgramId) return;
    this.loading.set(true);
    this.studentService.analyzeProgramTransfer(this.data.studentId, this.selectedProgramId).subscribe({
      next: (analysis) => {
        this.analysis.set(analysis);
        this.step.set('REVIEW');
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to analyse program transfer');
        this.loading.set(false);
      },
    });
  }

  protected proceedToConfirm(): void {
    this.step.set('CONFIRM');
  }

  protected back(): void {
    if (this.step() === 'CONFIRM') { this.step.set('REVIEW'); return; }
    if (this.step() === 'REVIEW') { this.step.set('SELECT'); this.analysis.set(null); }
  }

  protected executeTransfer(): void {
    if (!this.consentConfirmed) return;
    const an = this.analysis()!;
    const documentIdsToReturn = an.irrelevantDocuments
      .filter((d) => d.documentId != null)
      .map((d) => d.documentId!);

    this.submitting.set(true);
    this.studentService
      .executeProgramTransfer(this.data.studentId, {
        newProgramId: an.newProgramId,
        documentIdsToReturn,
        consentConfirmed: true,
        notes: this.notes.trim() || undefined,
      })
      .subscribe({
        next: (record) => {
          this.submitting.set(false);
          this.dialogRef.close(record);
        },
        error: (err) => {
          this.toast.error(err?.error?.message ?? 'Failed to execute program transfer');
          this.submitting.set(false);
        },
      });
  }

  protected statusIcon(status: string): string {
    switch (status) {
      case 'VERIFIED': return 'verified';
      case 'UPLOADED': return 'upload_file';
      case 'REJECTED': return 'cancel';
      default: return 'radio_button_unchecked';
    }
  }

  protected statusColor(status: string): string {
    switch (status) {
      case 'VERIFIED': return 'verified';
      case 'UPLOADED': return 'uploaded';
      case 'REJECTED': return 'rejected';
      default: return 'not-uploaded';
    }
  }

  protected get irrelevantWithFile(): ProgramTransferDocumentInfo[] {
    return (this.analysis()?.irrelevantDocuments ?? []).filter((d) => d.documentId != null);
  }
}

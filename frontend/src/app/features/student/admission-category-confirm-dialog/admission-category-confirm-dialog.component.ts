import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { formatAdmissionCategory } from '../student.model';
import {
  getCounsellingStateWarning,
  isCounsellingCategory,
  isTamilNaduState,
} from '../../../shared/utils/counselling-state.util';

export interface AdmissionCategoryConfirmDialogData {
  studentName: string;
  enquiryCategory: string;
  oldFinalCategory: string;
  newFinalCategory: string;
  studentState?: string | null;
}

@Component({
  selector: 'app-admission-category-confirm-dialog',
  standalone: true,
  imports: [
    FormsModule,
    MatButtonModule,
    MatDialogModule,
    MatIconModule,
  ],
  templateUrl: './admission-category-confirm-dialog.component.html',
  styleUrl: './admission-category-confirm-dialog.component.scss',
})
export class AdmissionCategoryConfirmDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<AdmissionCategoryConfirmDialogComponent>);
  readonly data: AdmissionCategoryConfirmDialogData = inject(MAT_DIALOG_DATA);

  protected reason = '';
  protected touched = signal(false);

  protected readonly formatCategory = formatAdmissionCategory;

  protected readonly isStudentTamilNadu = isTamilNaduState(this.data.studentState);

  protected readonly stateMismatchWarning: string | null = isCounsellingCategory(
    this.data.newFinalCategory,
  )
    ? getCounsellingStateWarning(
        formatAdmissionCategory(this.data.newFinalCategory),
        this.data.studentState,
        'student',
      )
    : null;

  protected get isReasonValid(): boolean {
    const trimmed = this.reason.trim();
    return trimmed.length > 0 && trimmed.length <= 1000;
  }

  protected onCancel(): void {
    this.dialogRef.close(null);
  }

  protected onConfirm(): void {
    this.touched.set(true);
    if (!this.isReasonValid) {
      return;
    }
    this.dialogRef.close(this.reason.trim());
  }
}

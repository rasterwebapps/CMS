import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

export interface CounsellingStateConfirmDialogData {
  title: string;
  personLabel: string;
  personName: string;
  categoryFieldLabel: string;
  categoryValueLabel: string;
  stateName: string | null;
  warningMessage: string;
  confirmText?: string;
}

@Component({
  selector: 'app-counselling-state-confirm-dialog',
  standalone: true,
  imports: [MatButtonModule, MatDialogModule, MatIconModule],
  templateUrl: './counselling-state-confirm-dialog.component.html',
  styleUrl: './counselling-state-confirm-dialog.component.scss',
})
export class CounsellingStateConfirmDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<CounsellingStateConfirmDialogComponent>);
  readonly data: CounsellingStateConfirmDialogData = inject(MAT_DIALOG_DATA);

  protected onCancel(): void {
    this.dialogRef.close(false);
  }

  protected onConfirm(): void {
    this.dialogRef.close(true);
  }
}

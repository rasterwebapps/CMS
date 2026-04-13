import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';

export interface ConfirmDialogData {
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
}

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>
      <p>{{ data.message }}</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button (click)="onCancel()">
        {{ data.cancelText || 'Cancel' }}
      </button>
      <button mat-flat-button color="warn" (click)="onConfirm()">
        {{ data.confirmText || 'Confirm' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    mat-dialog-content p {
      margin: 0;
      color: var(--mat-sys-on-surface-variant);
    }
  `,
})
export class ConfirmDialogComponent {
  protected readonly dialogRef = inject(MatDialogRef<ConfirmDialogComponent>);
  protected readonly data: ConfirmDialogData = inject(MAT_DIALOG_DATA);

  protected onCancel(): void {
    this.dialogRef.close(false);
  }

  protected onConfirm(): void {
    this.dialogRef.close(true);
  }
}

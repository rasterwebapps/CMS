import { Component, Inject } from '@angular/core';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import type { ShortcutDefinition } from '../../core/shortcuts/keyboard-shortcuts.service';

interface DialogData {
  shortcuts: readonly ShortcutDefinition[];
}

@Component({
  selector: 'app-keyboard-shortcuts-dialog',
  standalone: true,
  imports: [MatDialogModule, MatIconModule, MatButtonModule],
  templateUrl: './keyboard-shortcuts-dialog.component.html',
  styleUrl: './keyboard-shortcuts-dialog.component.scss',
})
export class KeyboardShortcutsDialogComponent {
  protected readonly shortcuts: readonly ShortcutDefinition[];

  constructor(
    @Inject(MAT_DIALOG_DATA) data: DialogData,
    private readonly dialogRef: MatDialogRef<KeyboardShortcutsDialogComponent>,
  ) {
    this.shortcuts = data.shortcuts;
  }

  protected close(): void {
    this.dialogRef.close();
  }

  /** Splits a shortcut label like `g d` or `Ctrl/⌘ K` into individual key chips. */
  protected keyParts(keys: string): string[] {
    return keys.split(' ').filter((k) => k.length > 0);
  }
}

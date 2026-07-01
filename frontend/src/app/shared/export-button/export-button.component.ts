import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

export type ExportFormat = 'excel' | 'pdf';

@Component({
  selector: 'cms-export-button',
  standalone: true,
  imports: [MatButtonModule, MatMenuModule, MatIconModule, MatTooltipModule],
  template: `
    <button mat-stroked-button [matMenuTriggerFor]="exportMenu"
            class="cms-export-btn" [matTooltip]="tooltip" [disabled]="disabled">
      <mat-icon>download</mat-icon>
      <span>{{ label }}</span>
      <mat-icon class="cms-export-chevron">arrow_drop_down</mat-icon>
    </button>

    <mat-menu #exportMenu="matMenu">
      @if (formats.includes('excel')) {
        <button mat-menu-item (click)="export.emit('excel')">
          <mat-icon>table_view</mat-icon>
          <span>Excel (.xlsx)</span>
        </button>
      }
      @if (formats.includes('pdf')) {
        <button mat-menu-item (click)="export.emit('pdf')">
          <mat-icon>picture_as_pdf</mat-icon>
          <span>PDF</span>
        </button>
      }
    </mat-menu>
  `,
  styles: [`
    .cms-export-btn {
      display: inline-flex;
      align-items: center;
      gap: 4px;
    }
    .cms-export-chevron {
      font-size: 18px;
      width: 18px;
      height: 18px;
    }
  `],
})
export class ExportButtonComponent {
  @Input() label = 'Export';
  @Input() tooltip = 'Export data as Excel or PDF';
  @Input() formats: ExportFormat[] = ['excel', 'pdf'];
  @Input() disabled = false;
  @Output() export = new EventEmitter<ExportFormat>();
}

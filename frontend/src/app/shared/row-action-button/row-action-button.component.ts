import { Component, EventEmitter, Input, Output } from '@angular/core';
import { NgClass } from '@angular/common';
import { MatTooltipModule, TooltipPosition } from '@angular/material/tooltip';

export type RowActionVariant = 'accent' | 'success' | 'info' | 'danger' | 'neutral' | 'warning';

/**
 * Single source of truth for table row-action buttons across the app —
 * the outlined, tint-at-rest/solid-on-hover `.btn-row-outline` style.
 * Icon-only when `label` is omitted, text+icon otherwise.
 *
 * Usage:
 *   <cms-row-action-btn variant="accent" ariaLabel="Verify documents" (clicked)="verify(row)">
 *     <svg>...</svg> Verify
 *   </cms-row-action-btn>
 */
@Component({
  selector: 'cms-row-action-btn',
  standalone: true,
  imports: [NgClass, MatTooltipModule],
  templateUrl: './row-action-button.component.html',
})
export class CmsRowActionButtonComponent {
  @Input() variant: RowActionVariant = 'accent';
  @Input() iconOnly = false;
  @Input() ariaLabel = '';
  @Input() tooltip?: string;
  @Input() tooltipPosition: TooltipPosition = 'above';
  @Input() disabled = false;

  @Output() clicked = new EventEmitter<MouseEvent>();

  protected get variantClass(): string {
    return `btn-row-outline--${this.variant}`;
  }
}

import { Component, Input } from '@angular/core';

/**
 * Live preview shell used on entry (form) screens. Renders a card
 * with a header label, accent stripe, optional "live" pulsing dot
 * and a body slot that the parent fills with form-driven content.
 *
 * Usage:
 *   <cms-preview-card label="Live preview" [showLiveDot]="true">
 *     <!-- preview body content goes here -->
 *   </cms-preview-card>
 */
@Component({
  selector: 'cms-preview-card',
  standalone: true,
  templateUrl: './preview-card.component.html',
  styleUrl: './preview-card.component.scss',
})
export class CmsPreviewCardComponent {
  @Input() label = 'Live preview';
  @Input() showLiveDot = true;
}

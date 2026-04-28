import { Component, Input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

/** A single contextual hint shown inside a {@link CmsTipsCardComponent}. */
export interface CmsTip {
  /** Material icon ligature name (e.g. 'check', 'person', 'edit'). */
  icon: string;
  /** Bold one-line heading for the tip. */
  title: string;
  /** Short supporting sentence. */
  subtitle: string;
}

/**
 * Right-rail card that lists short contextual tips next to entry forms.
 *
 * Tips render via Angular Material's `<mat-icon>` so callers only supply
 * an icon name (string) — no raw HTML/SVG markup is ever inserted into
 * the DOM.
 *
 * Usage:
 *   <cms-tips-card [tips]="tips" />
 */
@Component({
  selector: 'cms-tips-card',
  standalone: true,
  imports: [MatIconModule],
  templateUrl: './tips-card.component.html',
  styleUrl: './tips-card.component.scss',
})
export class CmsTipsCardComponent {
  @Input() heading = 'Tips';
  @Input() tips: CmsTip[] = [];
}


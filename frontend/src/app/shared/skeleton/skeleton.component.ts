import { Component, Input, OnChanges } from '@angular/core';

/**
 * Renders a CSS shimmer placeholder while content loads.
 *
 * Usage:
 *   <cms-skeleton [lines]="3" height="14px"></cms-skeleton>
 *   <cms-skeleton [circle]="true" height="40px"></cms-skeleton>
 */
@Component({
  selector: 'cms-skeleton',
  standalone: true,
  templateUrl: './skeleton.component.html',
  styleUrl: './skeleton.component.scss',
})
export class CmsSkeletonComponent implements OnChanges {
  /** Number of shimmer lines to render (ignored when circle=true). */
  @Input() lines = 1;

  /** Height of each line (or diameter of the circle). */
  @Input() height = '16px';

  /** When true, renders a single circular shimmer instead of lines. */
  @Input() circle = false;

  protected lineItems: number[] = [0];

  ngOnChanges(): void {
    this.lineItems = Array.from({ length: Math.max(1, this.lines) }, (_, i) => i);
  }
}

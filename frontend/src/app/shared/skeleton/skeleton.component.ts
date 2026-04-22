import { Component, Input, computed, signal } from '@angular/core';

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
export class CmsSkeletonComponent {
  private readonly _lines = signal(1);

  /** Number of shimmer lines to render (ignored when circle=true). */
  @Input()
  set lines(value: number) {
    this._lines.set(value);
  }

  /** Height of each line (or diameter of the circle). */
  @Input() height = '16px';

  /** When true, renders a single circular shimmer instead of lines. */
  @Input() circle = false;

  protected readonly lineItems = computed(() =>
    Array.from({ length: Math.max(1, this._lines()) }, (_, i) => i),
  );
}

import { Directive, ElementRef, Input, OnDestroy, AfterViewInit, inject } from '@angular/core';
import { TourService } from '../../core/tour/tour.service';

/**
 * Registers the host element as the anchor for a named tour step.
 *
 * Attach to any element in any component template. The tour tooltip will be
 * positioned relative to this element when the matching step is active.
 *
 * @example
 * ```html
 * <h1 [cmsTourStep]="'my-step-id'">Page heading</h1>
 * <button [cmsTourStep]="'save-button'">Save</button>
 * ```
 *
 * The directive de-registers itself automatically on `ngOnDestroy`, so
 * navigating away from a route cleanly removes its anchors.
 */
@Directive({
  selector: '[cmsTourStep]',
  standalone: true,
})
export class TourStepDirective implements AfterViewInit, OnDestroy {
  private readonly tourService = inject(TourService);
  private readonly elementRef = inject(ElementRef<HTMLElement>);

  /** The step id this element anchors. Must match a `TourStep.id`. */
  @Input({ alias: 'cmsTourStep', required: true }) stepId!: string;

  ngAfterViewInit(): void {
    if (this.stepId) {
      this.tourService.registerElement(this.stepId, this.elementRef);
    }
  }

  ngOnDestroy(): void {
    if (this.stepId) {
      this.tourService.unregisterElement(this.stepId);
    }
  }
}

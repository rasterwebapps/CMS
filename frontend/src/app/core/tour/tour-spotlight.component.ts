import {
  Component,
  OnInit,
  OnDestroy,
  inject,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  NgZone,
} from '@angular/core';
import { TourService } from './tour.service';

/**
 * Full-screen dim overlay that "spotlights" the active tour target element.
 *
 * Rendered by {@link TourService} into a CDK `OverlayRef` with
 * `GlobalPositionStrategy` so it covers the entire viewport.
 *
 * The spotlight effect is achieved by positioning a small `.spotlight-hole`
 * over the target element, then applying a large `box-shadow` to dim
 * everything outside it. Clicking the dim area dismisses the tour.
 */
@Component({
  selector: 'cms-tour-spotlight',
  standalone: true,
  template: `
    <div class="spotlight-mask" (click)="onMaskClick($event)">
      @if (holeStyle) {
        <div class="spotlight-hole" [style]="holeStyle"></div>
      }
    </div>
  `,
  styleUrl: './tour-spotlight.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TourSpotlightComponent implements OnInit, OnDestroy {
  private readonly tourService = inject(TourService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly ngZone = inject(NgZone);

  /** px breathing room around the highlighted target element. */
  private static readonly SPOTLIGHT_PADDING = 6;

  /** Set by {@link TourService} after the component is attached. */
  set targetEl(el: HTMLElement | null) {
    this._targetEl = el;
    this.updateHole();
  }

  private _targetEl: HTMLElement | null = null;
  protected holeStyle: Record<string, string> | null = null;

  private resizeObserver: ResizeObserver | null = null;
  private animFrameId: number | null = null;

  ngOnInit(): void {
    this.updateHole();
    this.attachResizeObserver();
    window.addEventListener('resize', this.onResize);
    window.addEventListener('scroll', this.onResize, { capture: true, passive: true });
  }

  ngOnDestroy(): void {
    window.removeEventListener('resize', this.onResize);
    window.removeEventListener('scroll', this.onResize, { capture: true });
    this.resizeObserver?.disconnect();
    if (this.animFrameId !== null) {
      cancelAnimationFrame(this.animFrameId);
    }
  }

  protected onMaskClick(event: MouseEvent): void {
    // Clicks that land directly on the mask (outside the hole) hard-close the tour.
    const target = event.target as HTMLElement;
    if (target.classList.contains('spotlight-mask')) {
      this.tourService.hardClose();
    }
  }

  private updateHole(): void {
    if (!this._targetEl) {
      this.holeStyle = null;
      this.cdr.markForCheck();
      return;
    }

    const rect = this._targetEl.getBoundingClientRect();
    const p = TourSpotlightComponent.SPOTLIGHT_PADDING;
    this.holeStyle = {
      top: `${rect.top - p}px`,
      left: `${rect.left - p}px`,
      width: `${rect.width + p * 2}px`,
      height: `${rect.height + p * 2}px`,
    };
    this.cdr.markForCheck();
  }

  private attachResizeObserver(): void {
    if (typeof ResizeObserver === 'undefined') return;
    this.resizeObserver = new ResizeObserver(() => this.scheduleUpdate());
    if (this._targetEl) {
      this.resizeObserver.observe(this._targetEl);
    }
    this.resizeObserver.observe(document.documentElement);
  }

  private readonly onResize = (): void => this.scheduleUpdate();

  private scheduleUpdate(): void {
    if (this.animFrameId !== null) return;
    this.animFrameId = requestAnimationFrame(() => {
      this.animFrameId = null;
      this.ngZone.run(() => this.updateHole());
    });
  }
}

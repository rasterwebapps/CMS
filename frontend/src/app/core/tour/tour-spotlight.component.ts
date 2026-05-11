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

interface PanelStyles {
  top: Record<string, string>;
  bottom: Record<string, string>;
  left: Record<string, string>;
  right: Record<string, string>;
}

/**
 * Full-screen dim overlay that "spotlights" the active tour target element.
 *
 * Instead of a single mask div with a pointer-events:none hole (which blocks
 * clicks meant for the target), this component renders four separate dim panels
 * that surround the highlighted area. The hole itself has no overlay, so clicks
 * inside it reach the real element — critical for event-driven steps like the
 * menu-search step where the user must type into the spotlit input.
 */
@Component({
  selector: 'cms-tour-spotlight',
  standalone: true,
  template: `
    @if (panels) {
      <div class="dim-panel" [style]="panels.top"    (click)="onDimClick()"></div>
      <div class="dim-panel" [style]="panels.bottom" (click)="onDimClick()"></div>
      <div class="dim-panel" [style]="panels.left"   (click)="onDimClick()"></div>
      <div class="dim-panel" [style]="panels.right"  (click)="onDimClick()"></div>
      <div class="spotlight-ring" [style]="ringStyle"></div>
    } @else {
      <!-- No target: full-screen dim (centered steps) -->
      <div class="dim-panel dim-panel--full" (click)="onDimClick()"></div>
    }
  `,
  styleUrl: './tour-spotlight.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TourSpotlightComponent implements OnInit, OnDestroy {
  private readonly tourService = inject(TourService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly ngZone = inject(NgZone);

  private static readonly PAD = 6;

  set targetEl(el: HTMLElement | null) {
    this._targetEl = el;
    this.updatePanels();
  }

  private _targetEl: HTMLElement | null = null;
  protected panels: PanelStyles | null = null;
  protected ringStyle: Record<string, string> | null = null;

  private resizeObserver: ResizeObserver | null = null;
  private animFrameId: number | null = null;

  ngOnInit(): void {
    this.updatePanels();
    this.attachResizeObserver();
    window.addEventListener('resize', this.onResize);
    window.addEventListener('scroll', this.onResize, { capture: true, passive: true });
  }

  ngOnDestroy(): void {
    window.removeEventListener('resize', this.onResize);
    window.removeEventListener('scroll', this.onResize, { capture: true });
    this.resizeObserver?.disconnect();
    if (this.animFrameId !== null) cancelAnimationFrame(this.animFrameId);
  }

  protected onDimClick(): void {
    this.tourService.hardClose();
  }

  private updatePanels(): void {
    if (!this._targetEl) {
      this.panels = null;
      this.ringStyle = null;
      this.cdr.markForCheck();
      return;
    }

    const rect = this._targetEl.getBoundingClientRect();
    const p = TourSpotlightComponent.PAD;
    const vw = window.innerWidth;
    const vh = window.innerHeight;

    const hTop    = Math.max(0, rect.top - p);
    const hLeft   = Math.max(0, rect.left - p);
    const hRight  = Math.min(vw, rect.right + p);
    const hBottom = Math.min(vh, rect.bottom + p);
    const hW = hRight - hLeft;
    const hH = hBottom - hTop;

    this.panels = {
      top:    { top: '0',         left: '0',          width: '100%',            height: `${hTop}px`       },
      bottom: { top: `${hBottom}px`, left: '0',        width: '100%',            bottom: '0'               },
      left:   { top: `${hTop}px`, left: '0',          width: `${hLeft}px`,      height: `${hH}px`         },
      right:  { top: `${hTop}px`, left: `${hRight}px`, right: '0',             height: `${hH}px`         },
    };

    this.ringStyle = {
      top:    `${hTop}px`,
      left:   `${hLeft}px`,
      width:  `${hW}px`,
      height: `${hH}px`,
    };

    this.cdr.markForCheck();
  }

  private attachResizeObserver(): void {
    if (typeof ResizeObserver === 'undefined') return;
    this.resizeObserver = new ResizeObserver(() => this.scheduleUpdate());
    if (this._targetEl) this.resizeObserver.observe(this._targetEl);
    this.resizeObserver.observe(document.documentElement);
  }

  private readonly onResize = (): void => this.scheduleUpdate();

  private scheduleUpdate(): void {
    if (this.animFrameId !== null) return;
    this.animFrameId = requestAnimationFrame(() => {
      this.animFrameId = null;
      this.ngZone.run(() => this.updatePanels());
    });
  }
}

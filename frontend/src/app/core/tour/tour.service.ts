import {
  Injectable,
  PLATFORM_ID,
  Injector,
  inject,
  signal,
  computed,
  DestroyRef,
  ElementRef,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { isPlatformBrowser } from '@angular/common';
import {
  Overlay,
  OverlayRef,
  GlobalPositionStrategy,
  FlexibleConnectedPositionStrategy,
  ConnectedPosition,
} from '@angular/cdk/overlay';
import { ComponentPortal } from '@angular/cdk/portal';
import { fromEvent, Subscription } from 'rxjs';
import { filter, take } from 'rxjs/operators';

import { TourStep } from './tour-step.model';
import { TourSpotlightComponent } from './tour-spotlight.component';
import { TourTooltipComponent } from './tour-tooltip.component';

/** localStorage key for the "don't show again" preference. */
const PREF_KEY = 'cms_tour_show_onboarding';

/** Delay in ms before auto-advancing after a successful event-driven interaction. */
const EVENT_ADVANCE_DELAY_MS = 600;

/** Named sets of tour steps indexed by tour id. */
const tourRegistry = new Map<string, TourStep[]>();

/**
 * Orchestrates the Live Data Walkthrough.
 *
 * Usage:
 * ```ts
 * const tour = inject(TourService);
 * tour.registerTour('onboarding', ONBOARDING_TOUR_STEPS);
 * tour.maybeAutoStart('onboarding'); // respects localStorage pref
 * tour.startTour('onboarding');      // always starts (Help button)
 * ```
 */
@Injectable({ providedIn: 'root' })
export class TourService {
  private readonly overlay = inject(Overlay);
  private readonly injector = inject(Injector);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly destroyRef = inject(DestroyRef);

  // ── Public state ────────────────────────────────
  /** Index of the currently visible step; `-1` when no tour is active. */
  readonly currentStepIndex = signal<number>(-1);

  /** Steps for the currently running tour. */
  readonly steps = signal<TourStep[]>([]);

  /** `true` while the tour is running. */
  readonly isActive = computed(() => this.currentStepIndex() >= 0);

  /** The step currently being displayed; `null` when inactive. */
  readonly currentStep = computed<TourStep | null>(() => {
    const idx = this.currentStepIndex();
    const list = this.steps();
    return idx >= 0 && idx < list.length ? list[idx] : null;
  });

  /**
   * `true` while waiting for a user interaction on an event-driven step.
   * "Next" button is disabled during this window.
   */
  readonly isWaiting = signal<boolean>(false);

  /**
   * `true` when the user has NOT yet permanently dismissed the tour.
   * Initialised from `localStorage` so it survives page reloads.
   */
  readonly showTour = signal<boolean>(this.loadPref());

  // ── Internal ────────────────────────────────────
  private spotlightRef: OverlayRef | null = null;
  private spotlightInstance: TourSpotlightComponent | null = null;
  private tooltipRef: OverlayRef | null = null;
  private eventSub: Subscription | null = null;

  /** Elements registered by `[cmsTourStep]` directives. */
  private readonly stepRegistry = new Map<string, ElementRef<HTMLElement>>();

  // ──────────────────────────────────────────────────────────────────────────
  // Directive registry
  // ──────────────────────────────────────────────────────────────────────────

  /** Called by {@link TourStepDirective} on `ngAfterViewInit`. */
  registerElement(stepId: string, ref: ElementRef<HTMLElement>): void {
    this.stepRegistry.set(stepId, ref);
  }

  /** Called by {@link TourStepDirective} on `ngOnDestroy`. */
  unregisterElement(stepId: string): void {
    this.stepRegistry.delete(stepId);
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Tour registration
  // ──────────────────────────────────────────────────────────────────────────

  /** Register a named set of steps so they can be started by id. */
  registerTour(tourId: string, steps: TourStep[]): void {
    tourRegistry.set(tourId, steps);
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Lifecycle
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Start the tour only if the user has not previously hard-closed it.
   * Intended for the post-login auto-start hook in `AppComponent`.
   */
  maybeAutoStart(tourId: string): void {
    if (!isPlatformBrowser(this.platformId)) return;
    if (this.showTour()) {
      this.startTour(tourId);
    }
  }

  /**
   * Start the tour unconditionally, ignoring the stored preference.
   * Called from the Help button or keyboard shortcut so users can always
   * re-launch the tour even after hard-closing it.
   */
  startTour(tourId: string): void {
    if (!isPlatformBrowser(this.platformId)) return;
    const tourSteps = tourRegistry.get(tourId);
    if (!tourSteps || tourSteps.length === 0) return;

    // Tear down any existing tour first.
    this.tearDown();

    this.steps.set(tourSteps);
    this.currentStepIndex.set(0);
    this.activateStep(0);
  }

  /** Advance to the next step. No-op on the last step (caller should call end). */
  advance(): void {
    const idx = this.currentStepIndex();
    const last = this.steps().length - 1;
    if (idx >= last) {
      this.end();
      return;
    }
    this.cancelEventSub();
    this.currentStepIndex.set(idx + 1);
    this.activateStep(idx + 1);
  }

  /** Go back to the previous step. No-op on the first step. */
  previous(): void {
    const idx = this.currentStepIndex();
    if (idx <= 0) return;
    this.cancelEventSub();
    this.currentStepIndex.set(idx - 1);
    this.activateStep(idx - 1);
  }

  /**
   * Soft-close: ends the tour without updating the "don't show again" pref.
   * Triggered by Escape key or the plain "Dismiss" link.
   */
  end(): void {
    this.tearDown();
  }

  /**
   * Hard-close: ends the tour AND sets `show_onboarding: false` in
   * `localStorage` so it does not auto-start on subsequent page loads.
   * Triggered by "Don't show again" or clicking the dim backdrop.
   */
  hardClose(): void {
    this.showTour.set(false);
    this.savePref(false);
    this.tearDown();
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Internal helpers
  // ──────────────────────────────────────────────────────────────────────────

  private activateStep(index: number): void {
    const step = this.steps()[index];
    if (!step) return;

    const target = this.resolveTarget(step);
    this.isWaiting.set(false);

    // Position / create the overlays.
    this.updateSpotlight(target);
    this.updateTooltip(step, target);

    // For event-driven steps, subscribe to the DOM event.
    if (step.advanceOn && step.advanceOn !== 'next-button' && target) {
      this.isWaiting.set(true);
      const { event } = step.advanceOn as { event: string };
      this.eventSub = fromEvent(target, event)
        .pipe(
          filter((e) => !step.validatorFn || step.validatorFn(e)),
          take(1),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe(() => {
          this.isWaiting.set(false);
          // Short delay so the user sees feedback before advancing.
          setTimeout(() => this.advance(), EVENT_ADVANCE_DELAY_MS);
        });
    }
  }

  /** Resolve the DOM element for the step (directive → selector → null). */
  private resolveTarget(step: TourStep): HTMLElement | null {
    if (step.targetSelector) {
      return document.querySelector<HTMLElement>(step.targetSelector);
    }
    const ref = this.stepRegistry.get(step.id);
    return ref ? ref.nativeElement : null;
  }

  // ── Spotlight ─────────────────────────────────

  private updateSpotlight(target: HTMLElement | null): void {
    if (!this.spotlightRef) {
      this.spotlightRef = this.overlay.create({
        positionStrategy: this.createCenteredPositionStrategy(),
        hasBackdrop: false,
        panelClass: 'cms-tour-spotlight-panel',
      });
      const portal = new ComponentPortal(TourSpotlightComponent, null, this.injector);
      const componentRef = this.spotlightRef.attach(portal);
      this.spotlightInstance = componentRef.instance;
    }

    if (this.spotlightInstance) {
      this.spotlightInstance.targetEl = target;
    }
  }

  // ── Tooltip ───────────────────────────────────

  private updateTooltip(step: TourStep, target: HTMLElement | null): void {
    if (this.tooltipRef) {
      this.tooltipRef.dispose();
      this.tooltipRef = null;
    }

    let positionStrategy: GlobalPositionStrategy | FlexibleConnectedPositionStrategy;

    if (target) {
      const preferredPlacement = step.placement ?? 'bottom';
      const positions = this.buildPositions(preferredPlacement);
      positionStrategy = this.overlay
        .position()
        .flexibleConnectedTo(target)
        .withPositions(positions)
        .withFlexibleDimensions(false)
        .withPush(true)
        .withViewportMargin(16);
    } else {
      // No target — centre the tooltip on screen.
      positionStrategy = this.createCenteredPositionStrategy();
    }

    this.tooltipRef = this.overlay.create({
      positionStrategy,
      scrollStrategy: this.overlay.scrollStrategies.reposition(),
      hasBackdrop: false,
      panelClass: 'cms-tour-tooltip-panel',
    });

    const portal = new ComponentPortal(TourTooltipComponent, null, this.injector);
    this.tooltipRef.attach(portal);
  }

  /** Returns a `GlobalPositionStrategy` centred horizontally and vertically. */
  private createCenteredPositionStrategy(): GlobalPositionStrategy {
    return this.overlay.position().global().centerHorizontally().centerVertically();
  }

  private buildPositions(preferred: 'top' | 'bottom' | 'left' | 'right'): ConnectedPosition[] {
    const positions: Record<string, ConnectedPosition> = {
      bottom: { originX: 'center', originY: 'bottom', overlayX: 'center', overlayY: 'top', offsetY: 12 },
      top: { originX: 'center', originY: 'top', overlayX: 'center', overlayY: 'bottom', offsetY: -12 },
      right: { originX: 'end', originY: 'center', overlayX: 'start', overlayY: 'center', offsetX: 12 },
      left: { originX: 'start', originY: 'center', overlayX: 'end', overlayY: 'center', offsetX: -12 },
    };
    const order = ['bottom', 'top', 'right', 'left'];
    return [preferred, ...order.filter((p) => p !== preferred)].map((p) => positions[p]);
  }

  // ── Teardown ─────────────────────────────────

  private tearDown(): void {
    this.cancelEventSub();

    if (this.spotlightRef) {
      this.spotlightRef.dispose();
      this.spotlightRef = null;
      this.spotlightInstance = null;
    }
    if (this.tooltipRef) {
      this.tooltipRef.dispose();
      this.tooltipRef = null;
    }

    this.currentStepIndex.set(-1);
    this.steps.set([]);
    this.isWaiting.set(false);
  }

  private cancelEventSub(): void {
    if (this.eventSub) {
      this.eventSub.unsubscribe();
      this.eventSub = null;
    }
  }

  // ── localStorage ─────────────────────────────

  private loadPref(): boolean {
    if (typeof localStorage === 'undefined') return true;
    try {
      const stored = localStorage.getItem(PREF_KEY);
      // If never set, default to showing the tour.
      if (stored === null) return true;
      return stored !== 'false';
    } catch {
      return true;
    }
  }

  private savePref(value: boolean): void {
    try {
      localStorage.setItem(PREF_KEY, String(value));
    } catch {
      // Ignore storage errors (private browsing, quota exceeded, etc.)
    }
  }
}

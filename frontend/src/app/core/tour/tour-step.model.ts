/**
 * Describes a single step in a guided tour.
 *
 * Steps are rendered by {@link TourTooltipComponent} and positioned via the
 * Angular CDK Overlay. Target elements can be registered by the
 * {@link TourStepDirective} (`[cmsTourStep]`) or resolved at runtime via a
 * CSS `targetSelector`.
 */
export interface TourStep {
  /**
   * Unique identifier for this step. Also used as the key that
   * {@link TourStepDirective} registers under.
   */
  id: string;

  /** Tooltip heading shown in the step counter row. */
  title: string;

  /** Main body copy for the tooltip. Supports plain text. */
  body: string;

  /**
   * CSS selector resolved with `document.querySelector()` at the moment the
   * step becomes active. Takes precedence over any directive registration
   * when both are present. If omitted and no directive is registered,
   * the tooltip is centered on screen.
   */
  targetSelector?: string;

  /**
   * When set the tooltip renders a compact 3-row alignment demo inside the
   * body to illustrate how numeric/currency, status badge, and text/name
   * columns are aligned in CMS data tables.
   *
   * - `'right'`  — highlight the currency row.
   * - `'center'` — highlight the badge row.
   * - `'left'`   — highlight the text row.
   * - `'all'`    — show all three rows with equal weight (overview step).
   */
  alignmentHint?: 'right' | 'center' | 'left' | 'all';

  /**
   * Controls when the "Next" button becomes enabled.
   *
   * - `'next-button'` (default) — always enabled; user clicks to advance.
   * - `{ event: string }` — the tour listens for the named DOM event on the
   *   target element and only advances after `validatorFn` returns `true`.
   */
  advanceOn?: 'next-button' | { event: string };

  /**
   * Optional guard for event-driven steps. Called with the DOM event each
   * time the tracked event fires. The tour advances only on the first
   * emission for which this function returns `true`.
   *
   * When omitted, any emission of the event advances the tour.
   */
  validatorFn?: (event: Event) => boolean;

  /**
   * Preferred placement of the tooltip relative to the target element.
   * The CDK `FlexibleConnectedPositionStrategy` will fall back through
   * `['bottom','top','right','left']` automatically when the preferred
   * position overflows the viewport.
   *
   * Ignored when the step has no target (the tooltip is centred).
   */
  placement?: 'top' | 'bottom' | 'left' | 'right';
}

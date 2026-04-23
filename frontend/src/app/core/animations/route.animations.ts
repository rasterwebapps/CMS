import {
  animate,
  group,
  query,
  style,
  transition,
  trigger,
} from '@angular/animations';

/**
 * Route-level fade-slide transition wired to the root `<router-outlet>`.
 *
 * Durations and easing are kept in sync with the design tokens
 * `--cms-duration-md` (200ms) and `--cms-ease-out` (`cubic-bezier(0.4, 0, 0.2, 1)`).
 * Hard-coded here because Angular animation metadata can't read CSS variables.
 */
export const routeFadeSlide = trigger('routeAnim', [
  transition('* <=> *', [
    query(
      ':enter, :leave',
      [
        style({
          position: 'absolute',
          left: 0,
          right: 0,
          top: 0,
          width: '100%',
        }),
      ],
      { optional: true },
    ),
    query(':enter', [style({ opacity: 0, transform: 'translateY(8px)' })], {
      optional: true,
    }),
    group([
      query(
        ':leave',
        [
          animate(
            '120ms cubic-bezier(0.4, 0, 0.2, 1)',
            style({ opacity: 0, transform: 'translateY(-4px)' }),
          ),
        ],
        { optional: true },
      ),
      query(
        ':enter',
        [
          animate(
            '200ms 60ms cubic-bezier(0.4, 0, 0.2, 1)',
            style({ opacity: 1, transform: 'translateY(0)' }),
          ),
        ],
        { optional: true },
      ),
    ]),
  ]),
]);

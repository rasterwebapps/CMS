/**
 * Toast notification primitives shared between {@link ToastService} and the
 * {@link ToastHostComponent} renderer.
 */

/** Visual / semantic flavour of a toast. */
export type ToastType = 'success' | 'error' | 'warning' | 'info';

/** A single active toast tracked by {@link ToastService}. */
export interface Toast {
  /** Stable id used for keyed rendering and {@link ToastService.dismiss}. */
  readonly id: number;
  /** Visual flavour — drives accent color, icon, and ARIA role. */
  readonly type: ToastType;
  /** Primary message text. */
  readonly message: string;
  /** Auto-dismiss delay in milliseconds. `0` keeps the toast visible until dismissed. */
  readonly durationMs: number;
  /** Wall-clock timestamp when the toast was created (used for FIFO ordering). */
  readonly createdAt: number;
}

/** Optional overrides for the default toast behaviour. */
export interface ToastOptions {
  /** Override the default auto-dismiss duration in milliseconds. Pass `0` to keep open. */
  durationMs?: number;
}

/** Default auto-dismiss durations per type, in milliseconds. */
export const DEFAULT_TOAST_DURATIONS: Readonly<Record<ToastType, number>> = {
  success: 3000,
  info: 4000,
  warning: 5000,
  error: 6000,
};

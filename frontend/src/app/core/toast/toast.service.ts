import { Injectable, signal } from '@angular/core';

import {
  DEFAULT_TOAST_DURATIONS,
  Toast,
  ToastOptions,
  ToastType,
} from './toast.model';

/**
 * Lightweight notification service that replaces direct `MatSnackBar` usage
 * across the application.
 *
 * Toasts are rendered by the {@link ToastHostComponent}, which subscribes to
 * the {@link toasts} signal exposed below. The service is provided in root
 * scope and is safe to inject into any component or service.
 *
 * @example
 * ```ts
 * private readonly toast = inject(ToastService);
 * this.toast.success('Department created');
 * ```
 */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private readonly _toasts = signal<Toast[]>([]);
  /** Read-only view of currently active toasts (FIFO order). */
  readonly toasts = this._toasts.asReadonly();

  private nextId = 1;
  private readonly timers = new Map<number, ReturnType<typeof setTimeout>>();

  /** Show a green success toast. */
  success(message: string, options?: ToastOptions): number {
    return this.show('success', message, options);
  }

  /** Show a red error toast. */
  error(message: string, options?: ToastOptions): number {
    return this.show('error', message, options);
  }

  /** Show an amber warning toast. */
  warning(message: string, options?: ToastOptions): number {
    return this.show('warning', message, options);
  }

  /** Show a blue informational toast. */
  info(message: string, options?: ToastOptions): number {
    return this.show('info', message, options);
  }

  /** Remove a specific toast by id. Safe to call for unknown ids. */
  dismiss(id: number): void {
    const timer = this.timers.get(id);
    if (timer !== undefined) {
      clearTimeout(timer);
      this.timers.delete(id);
    }
    this._toasts.update((current) => current.filter((t) => t.id !== id));
  }

  /** Remove all currently displayed toasts. */
  dismissAll(): void {
    for (const timer of this.timers.values()) {
      clearTimeout(timer);
    }
    this.timers.clear();
    this._toasts.set([]);
  }

  private show(type: ToastType, message: string, options?: ToastOptions): number {
    const id = this.nextId++;
    const durationMs = options?.durationMs ?? DEFAULT_TOAST_DURATIONS[type];
    const toast: Toast = {
      id,
      type,
      message,
      durationMs,
      createdAt: Date.now(),
    };
    this._toasts.update((current) => [...current, toast]);

    if (durationMs > 0) {
      const timer = setTimeout(() => this.dismiss(id), durationMs);
      this.timers.set(id, timer);
    }
    return id;
  }
}

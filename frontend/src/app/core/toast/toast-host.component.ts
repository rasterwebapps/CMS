import { Component, inject } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

import { ToastType } from './toast.model';
import { ToastService } from './toast.service';

const ICONS: Readonly<Record<ToastType, string>> = {
  success: 'check_circle',
  error: 'error',
  warning: 'warning',
  info: 'info',
};

/**
 * Renders the stack of active toasts produced by {@link ToastService}.
 * Mounted once at the application root.
 */
@Component({
  selector: 'app-toast-host',
  standalone: true,
  imports: [MatIconModule],
  templateUrl: './toast-host.component.html',
  styleUrl: './toast-host.component.scss',
})
export class ToastHostComponent {
  private readonly toastService = inject(ToastService);

  protected readonly toasts = this.toastService.toasts;

  protected iconFor(type: ToastType): string {
    return ICONS[type];
  }

  /** Toasts that surface errors / warnings get `role="alert"` for assistive tech. */
  protected ariaRoleFor(type: ToastType): 'alert' | 'status' {
    return type === 'error' || type === 'warning' ? 'alert' : 'status';
  }

  protected dismiss(id: number): void {
    this.toastService.dismiss(id);
  }
}

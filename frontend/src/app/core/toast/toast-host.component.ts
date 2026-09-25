import { AfterViewInit, Component, Injector, OnDestroy, TemplateRef, ViewChild, ViewContainerRef, effect, inject } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { Overlay, OverlayRef } from '@angular/cdk/overlay';
import { TemplatePortal } from '@angular/cdk/portal';

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
 *
 * Rendered via CDK Overlay (portaled to `.cdk-overlay-container` at the document root)
 * instead of relying on being a plain descendant of `app-root` — same reasoning as
 * `CmsFlyoutPanelComponent`. `app-root` (styles.scss) sets `position: relative; z-index: 1`
 * for unrelated layout reasons, which makes it a stacking context: this host's own
 * `z-index: 1100` (toast-host.component.scss) only ever wins against *siblings inside*
 * app-root, never against `.cdk-overlay-container`, which Angular CDK appends as app-root's
 * own sibling directly under `<body>`. Any flyout/dialog open at the same time (all CDK
 * Overlay-based) therefore painted over every toast — the toast was in the DOM, fully
 * formed, just invisible — so an error toast fired while a flyout was open (e.g. the Global
 * Auto-Schedule "Confirm ticked substitution(s)" 409 rejection) looked like the click did
 * nothing.
 *
 * `panelClass: cms-toast-overlay-pane` (styles.scss) raises this host's own overlay pane
 * *and* its `.cdk-global-overlay-wrapper` above the CDK default (1000, shared by every
 * pane/wrapper) -- necessary but, empirically, NOT sufficient on its own: two
 * `.cdk-global-overlay-wrapper` siblings under the same `.cdk-overlay-container` did not
 * repaint in z-index order live in testing even with a forced `z-index: 999999 !important`
 * on both, which is why this overlay is torn down and recreated from scratch every time the
 * toast stack goes from empty to non-empty (`effect()` below) instead of being created once
 * in `ngAfterViewInit` and left attached -- a freshly-created overlay reliably painted above
 * an already-open flyout/dialog where a z-index-only fix on a long-lived one did not, so
 * this host is kept overlay-free while idle and only exists for as long as a toast does.
 */
@Component({
  selector: 'app-toast-host',
  standalone: true,
  imports: [MatIconModule],
  templateUrl: './toast-host.component.html',
  styleUrl: './toast-host.component.scss',
})
export class ToastHostComponent implements AfterViewInit, OnDestroy {
  private readonly toastService = inject(ToastService);
  private readonly overlay = inject(Overlay);
  private readonly viewContainerRef = inject(ViewContainerRef);
  private readonly injector = inject(Injector);

  @ViewChild('hostTpl') private readonly hostTpl!: TemplateRef<unknown>;
  private overlayRef: OverlayRef | null = null;

  protected readonly toasts = this.toastService.toasts;

  ngAfterViewInit(): void {
    effect(() => {
      const hasToasts = this.toasts().length > 0;
      if (hasToasts && !this.overlayRef) {
        this.overlayRef = this.overlay.create({
          positionStrategy: this.overlay.position().global(),
          panelClass: 'cms-toast-overlay-pane',
        });
        this.overlayRef.attach(new TemplatePortal(this.hostTpl, this.viewContainerRef));
      } else if (!hasToasts && this.overlayRef) {
        this.overlayRef.dispose();
        this.overlayRef = null;
      }
    }, { injector: this.injector });
  }

  ngOnDestroy(): void {
    this.overlayRef?.dispose();
  }

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

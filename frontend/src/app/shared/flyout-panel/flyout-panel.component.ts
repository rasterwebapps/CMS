import { AfterViewInit, Component, OnDestroy, TemplateRef, ViewChild, ViewContainerRef, inject, input, output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { Overlay, OverlayRef } from '@angular/cdk/overlay';
import { TemplatePortal } from '@angular/cdk/portal';

/**
 * Generic slide-in side panel shell — backdrop + sliding panel + header/body/footer
 * layout, with the actual title/content/buttons supplied by the consumer via the
 * `flyoutHeader`/`flyoutBody`/`flyoutFooter` projection slots. Visual spec ported from
 * `shared/fee-receipt-dialog`, the only prior example of this pattern in the app.
 *
 * Consumers own their own show/hide signal and are expected to wrap this component in
 * `@if (target(); as t)` so it's created fresh on every open (no exit animation, matching
 * the fee-receipt-dialog precedent) — this keeps any `ngOnInit`-based fetch-on-open logic
 * working unchanged.
 *
 * Rendered via CDK Overlay (portaled to the app's overlay container at the document
 * root) instead of a local position:fixed div, same reasoning as
 * `shared/column-picker`'s dropdown. Any host page wrapped in `.detail-page`/similar
 * (styles.scss's `cms-rise-up` entrance animation, applied with fill-mode `both`) ends
 * up with a *computed* `transform: matrix(1,0,0,1,0,0)` forever after the animation
 * settles — never the literal keyword `none`, even though it's visually identical to
 * it — and per spec any non-`none` transform on an ancestor creates a new containing
 * block for `position: fixed` descendants. A plain fixed div here was silently
 * re-anchored to that ancestor's box instead of the viewport, so it scrolled along with
 * the page instead of staying put (confirmed live via getComputedStyle — `.detail-page`
 * reports `matrix(1,0,0,1,0,0)`, not `none`). The overlay escapes that entirely by
 * attaching outside the whole routed-page DOM subtree, exactly like MatDialog already
 * does successfully elsewhere in this app. `scrollStrategies.block()` additionally locks
 * background scroll while open, matching standard modal behavior.
 */
@Component({
  selector: 'cms-flyout-panel',
  standalone: true,
  imports: [MatIconModule],
  templateUrl: './flyout-panel.component.html',
  styleUrl: './flyout-panel.component.scss',
})
export class CmsFlyoutPanelComponent implements AfterViewInit, OnDestroy {
  readonly width = input('440px');
  readonly ariaLabel = input('Panel');

  readonly closed = output<void>();

  @ViewChild('panelTpl') private readonly panelTpl!: TemplateRef<unknown>;

  private readonly overlay = inject(Overlay);
  private readonly viewContainerRef = inject(ViewContainerRef);
  private overlayRef: OverlayRef | null = null;

  ngAfterViewInit(): void {
    this.overlayRef = this.overlay.create({
      positionStrategy: this.overlay.position().global(),
      scrollStrategy: this.overlay.scrollStrategies.block(),
    });
    this.overlayRef.attach(new TemplatePortal(this.panelTpl, this.viewContainerRef));
  }

  ngOnDestroy(): void {
    this.overlayRef?.dispose();
  }

  protected onClose(): void {
    this.closed.emit();
  }
}

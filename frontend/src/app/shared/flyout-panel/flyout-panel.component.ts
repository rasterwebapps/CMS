import { Component, input, output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

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
 */
@Component({
  selector: 'cms-flyout-panel',
  standalone: true,
  imports: [MatIconModule],
  templateUrl: './flyout-panel.component.html',
  styleUrl: './flyout-panel.component.scss',
})
export class CmsFlyoutPanelComponent {
  readonly width = input('440px');
  readonly ariaLabel = input('Panel');

  readonly closed = output<void>();

  protected onClose(): void {
    this.closed.emit();
  }
}

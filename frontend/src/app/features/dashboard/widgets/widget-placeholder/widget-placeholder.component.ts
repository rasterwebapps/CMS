import { Component, Input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

/**
 * Universal placeholder shown for any widget key that doesn't yet have
 * a real component registered in the widget registry.
 *
 * Displays a glassmorphism skeleton card with the widget's label and icon.
 * Phase 4 replaces placeholders one by one with real self-contained widgets.
 *
 * Accepts widgetKey / widgetLabel / widgetIcon via ngComponentOutletInputs —
 * Angular silently ignores these on real components that don't declare them,
 * so this contract is backward-compatible as Phase 4 builds real components.
 */
@Component({
  selector: 'app-widget-placeholder',
  standalone: true,
  imports: [MatIconModule],
  templateUrl: './widget-placeholder.component.html',
  styleUrl:    './widget-placeholder.component.scss',
})
export class WidgetPlaceholderComponent {
  @Input() widgetKey?:   string;
  @Input() widgetLabel?: string;
  @Input() widgetIcon?:  string;
}

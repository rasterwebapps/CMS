import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

/**
 * Displays a centred empty-state panel with an optional action button.
 *
 * Usage:
 *   <cms-empty-state
 *     icon="inbox"
 *     title="No enquiries yet"
 *     subtitle="Start by adding a new enquiry"
 *     actionLabel="New Enquiry"
 *     (actionClick)="openForm()"
 *   ></cms-empty-state>
 */
@Component({
  selector: 'cms-empty-state',
  standalone: true,
  imports: [MatIconModule],
  templateUrl: './empty-state.component.html',
  styleUrl: './empty-state.component.scss',
})
export class CmsEmptyStateComponent {
  /** Material icon name shown above the title. */
  @Input() icon = 'inbox';

  /** Primary heading text. */
  @Input() title = 'No data available';

  /** Optional secondary description. */
  @Input() subtitle = '';

  /** Label for the optional CTA button. Omit to hide the button. */
  @Input() actionLabel = '';

  /** Emitted when the CTA button is clicked. */
  @Output() actionClick = new EventEmitter<void>();
}

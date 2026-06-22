import { Component, Input } from '@angular/core';

/**
 * Icon for the activate/deactivate row action. Pass the record's *current*
 * active state — the icon shows the action that will result from clicking:
 * circle+line ("turn off") when currently active, circle+check ("turn on")
 * when currently inactive.
 */
@Component({
  selector: 'cms-icon-toggle-status',
  standalone: true,
  template: `
    @if (active) {
      <svg [attr.width]="size" [attr.height]="size" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <circle cx="12" cy="12" r="9"/>
        <line x1="8" y1="12" x2="16" y2="12"/>
      </svg>
    } @else {
      <svg [attr.width]="size" [attr.height]="size" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <circle cx="12" cy="12" r="9"/>
        <polyline points="8 12 11 15 16 9"/>
      </svg>
    }
  `,
})
export class CmsIconToggleStatusComponent {
  @Input() active = false;
  @Input() size = 15;
}

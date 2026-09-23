import { Component, inject } from '@angular/core';
import { MatMenuModule } from '@angular/material/menu';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { WardContextService } from '../../core/ward-context/ward-context.service';

/** Global top-bar dropdown letting a guardian pick which ward (child) is "active" for every
 *  ward-scoped screen underneath (parent dashboard, ward fees). Renders nothing when the caller
 *  has no wards loaded -- app.html only mounts this component behind a permission check, but this
 *  guard makes the component safe to drop in anywhere regardless. */
@Component({
  selector: 'app-ward-switcher',
  standalone: true,
  imports: [MatMenuModule, MatIconModule, MatTooltipModule],
  templateUrl: './ward-switcher.component.html',
  styleUrl: './ward-switcher.component.scss',
})
export class WardSwitcherComponent {
  protected readonly wardContext = inject(WardContextService);

  protected selectWard(studentId: number): void {
    this.wardContext.selectWard(studentId);
  }
}

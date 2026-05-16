import { Component, Input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ConnectionItem, SAMPLE_CONNECTIONS } from '../widget.models';

/**
 * Colleagues / connections widget. Static sample list for now; will source
 * from `FacultyService.list()` filtered by department in a follow-up.
 */
@Component({
  selector: 'cms-connections-card',
  standalone: true,
  imports: [MatIconModule, MatTooltipModule],
  templateUrl: './connections-card.component.html',
  styleUrl: './connections-card.component.scss',
  host: { '[style.--ca]': '"#10b981"' },
})
export class ConnectionsCardComponent {
  // ngComponentOutletInputs compatibility
  @Input() widgetKey?:   string;
  @Input() widgetLabel?: string;
  @Input() widgetIcon?:  string;
  @Input() title = 'Colleagues';
  @Input() people: ConnectionItem[] = SAMPLE_CONNECTIONS;
}


import { Component, input, output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

export interface DashboardTab {
  role: string;
  label: string;
  icon: string;
  color: string;
}

@Component({
  selector: 'app-dashboard-tabs',
  standalone: true,
  imports: [MatIconModule, MatButtonModule],
  templateUrl: './dashboard-tabs.component.html',
  styleUrl: './dashboard-tabs.component.scss',
})
export class DashboardTabsComponent {
  readonly tabs = input.required<DashboardTab[]>();
  readonly activeRole = input.required<string>();
  readonly tabChange = output<string>();

  protected selectTab(role: string): void {
    this.tabChange.emit(role);
  }

  protected onKeydown(event: KeyboardEvent, index: number): void {
    const tabs = this.tabs();
    if (event.key === 'ArrowRight') {
      const next = tabs[(index + 1) % tabs.length];
      this.tabChange.emit(next.role);
    } else if (event.key === 'ArrowLeft') {
      const prev = tabs[(index - 1 + tabs.length) % tabs.length];
      this.tabChange.emit(prev.role);
    }
  }
}

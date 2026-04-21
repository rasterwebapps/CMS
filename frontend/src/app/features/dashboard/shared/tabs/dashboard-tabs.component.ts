import { Component, input, output, ViewChildren, QueryList, ElementRef } from '@angular/core';
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

  @ViewChildren('tabBtn') tabButtons!: QueryList<ElementRef<HTMLButtonElement>>;

  protected selectTab(role: string): void {
    this.tabChange.emit(role);
  }

  protected onKeydown(event: KeyboardEvent, index: number): void {
    const tabs = this.tabs();
    let targetIndex: number | null = null;

    if (event.key === 'ArrowRight') {
      targetIndex = (index + 1) % tabs.length;
    } else if (event.key === 'ArrowLeft') {
      targetIndex = (index - 1 + tabs.length) % tabs.length;
    }

    if (targetIndex !== null) {
      event.preventDefault();
      this.tabChange.emit(tabs[targetIndex].role);
      // Move focus to the newly activated tab
      const btns = this.tabButtons.toArray();
      btns[targetIndex]?.nativeElement.focus();
    }
  }
}

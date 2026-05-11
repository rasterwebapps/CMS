import { Component, inject, OnInit, signal } from '@angular/core';
import { UserRoleService } from '../../core/permissions/user-role.service';
import { AdminDashboardComponent } from './admin/admin-dashboard.component';
import { DEFAULT_WIDGET_KEYS } from './widget-registry';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [AdminDashboardComponent],
  template: `
    <app-admin-dashboard [visibleWidgets]="visibleWidgets()" />
  `,
})
export class DashboardComponent implements OnInit {
  private readonly roleSvc = inject(UserRoleService);

  /**
   * Widget keys for the current user's role.
   * Null while loading; resolved to configured list or defaults once ready.
   */
  protected readonly visibleWidgets = signal<string[] | null>(null);

  ngOnInit(): void {
    this.roleSvc.getUserDashboardWidgets().subscribe({
      next: (keys) => {
        this.visibleWidgets.set(keys.length > 0 ? keys : DEFAULT_WIDGET_KEYS);
      },
      error: () => {
        // API unavailable — fall back to showing all default widgets
        this.visibleWidgets.set(DEFAULT_WIDGET_KEYS);
      },
    });
  }
}

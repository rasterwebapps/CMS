import { Component, Type, computed, inject } from '@angular/core';
import { NgComponentOutlet } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

import { DashboardConfigService } from './services/dashboard-config.service';

// Legacy role dashboards — kept until Phase 8 removes them
import { AdminDashboardComponent }      from './admin/admin-dashboard.component';
import { FrontOfficeDashboardComponent } from './front-office/front-office-dashboard.component';
import { FacultyDashboardComponent }    from './faculty/faculty-dashboard.component';
import { CashierDashboardComponent }    from './cashier/cashier-dashboard.component';
import { StudentDashboardComponent }    from './student/student-dashboard.component';

import { PermissionService }   from '../../core/permissions/permission.service';
import { AuthService }         from '../../core/auth/auth.service';
import { DEFAULT_WIDGET_KEYS, widgetByKey } from './widget-registry';
import { WidgetPlaceholderComponent }   from './widgets/widget-placeholder/widget-placeholder.component';

interface WidgetItem {
  key:       string;
  colSpan:   number;
  rowSpan:   number;
  component: Type<unknown>;
  /** Inputs forwarded via ngComponentOutletInputs to every widget. */
  inputs:    Record<string, unknown>;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    NgComponentOutlet,
    RouterLink,
    MatIconModule,
    // legacy sub-dashboards
    AdminDashboardComponent,
    FrontOfficeDashboardComponent,
    FacultyDashboardComponent,
    CashierDashboardComponent,
    StudentDashboardComponent,
  ],
  templateUrl: './dashboard.html',
  styleUrl:    './dashboard.scss',
})
export class DashboardComponent {
  protected readonly permissionService  = inject(PermissionService);
  private  readonly authService         = inject(AuthService);
  protected readonly dashboardConfigSvc = inject(DashboardConfigService);

  protected readonly canCustomize = computed(() =>
    this.dashboardConfigSvc.canCustomize()
  );

  // ── Config resolution ───────────────────────────────────────────────────

  /**
   * Resolved widget config DTOs — user override → role default → fallback keys.
   * Includes span metadata from Phase 1 DB changes.
   */
  protected readonly widgetConfigs = computed(() => {
    const saved = this.permissionService.dashboardWidgets();
    if (saved.length > 0) return saved;
    return DEFAULT_WIDGET_KEYS.map((key, i) => ({
      key, order: i,
      colSpan:    widgetByKey(key)?.defaultColSpan ?? 1,
      rowSpan:    widgetByKey(key)?.defaultRowSpan ?? 1,
      configJson: null,
    }));
  });

  /**
   * Key-only list — kept for the legacy role sub-dashboards during transition.
   * Removed in Phase 8.
   */
  protected readonly visibleWidgets = computed<string[] | null>(() =>
    this.widgetConfigs().map(w => w.key)
  );

  // ── Dynamic renderer ────────────────────────────────────────────────────

  /**
   * True when the current user has a recognised role (meaning they get either
   * a DB-seeded config from V134 or the DEFAULT_WIDGET_KEYS fallback layout).
   * False only when the user has NO role at all — shows the "Account not
   * configured" screen via the legacy path.
   *
   * After V134 seeds default configs, this is always true for valid users.
   * The legacy role-specific dashboards remain in the @else branch until Phase 8.
   */
  protected readonly useDynamicRenderer = computed(() =>
    this.widgetConfigs().length > 0 && this.permissionService.loaded()
  );

  /**
   * Widget items hydrated with their Angular component types and
   * ngComponentOutletInputs so the grid can render them declaratively.
   */
  protected readonly widgetItems = computed<WidgetItem[]>(() =>
    this.widgetConfigs().map(cfg => {
      const def  = widgetByKey(cfg.key);
      return {
        key:       cfg.key,
        colSpan:   cfg.colSpan,
        rowSpan:   cfg.rowSpan,
        component: def?.component ?? WidgetPlaceholderComponent,
        inputs:    {
          widgetKey:   cfg.key,
          widgetLabel: def?.label,
          widgetIcon:  def?.icon,
        },
      };
    })
  );

  // ── Legacy role routing (Phase 8 will remove this) ──────────────────────

  protected readonly activeDashboard = computed<
    'admin' | 'front-office' | 'cashier' | 'faculty' | 'student' | null
  >(() => {
    if (this.permissionService.isRole('devadmin','supportadmin','admin','collegeadmin','college_admin'))
      return 'admin';
    if (this.permissionService.isRole('frontoffice','front_office'))
      return 'front-office';
    if (this.permissionService.isRole('cashier'))
      return 'cashier';
    if (this.permissionService.isRole('faculty'))
      return 'faculty';
    if (this.permissionService.isRole('student'))
      return 'student';
    return null;
  });

  protected logout(): void {
    void this.authService.logout();
  }
}

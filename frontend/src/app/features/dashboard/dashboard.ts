import { Component, OnInit, Type, computed, inject } from '@angular/core';
import { NgComponentOutlet } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

import { DashboardConfigService } from './services/dashboard-config.service';
import { PermissionService }      from '../../core/permissions/permission.service';
import { AuthService }            from '../../core/auth/auth.service';
import { DEFAULT_WIDGET_KEYS, widgetByKey } from './widget-registry';
import { WidgetPlaceholderComponent } from './widgets/widget-placeholder/widget-placeholder.component';
import { TourService } from '../../shared/tour/tour.service';
import { CmsTourButtonComponent } from '../../shared/tour/tour-button.component';
import { DASHBOARD_TOUR, DASHBOARD_FLOW_MAP } from '../../shared/tour/tours/dashboard.tours';

interface WidgetItem {
  key:       string;
  colSpan:   number;
  rowSpan:   number;
  component: Type<unknown>;
  inputs:    Record<string, unknown>;
}

const KNOWN_ROLES = [
  'devadmin', 'supportadmin', 'admin', 'collegeadmin', 'college_admin',
  'frontoffice', 'front_office', 'cashier', 'faculty', 'student',
] as const;

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [NgComponentOutlet, RouterLink, MatIconModule, CmsTourButtonComponent],
  templateUrl: './dashboard.html',
  styleUrl:    './dashboard.scss',
})
export class DashboardComponent implements OnInit {
  protected readonly permissionService  = inject(PermissionService);
  private   readonly authService        = inject(AuthService);
  protected readonly dashboardConfigSvc = inject(DashboardConfigService);
  private   readonly tourService        = inject(TourService);

  ngOnInit(): void {
    this.tourService.register('dashboard', DASHBOARD_TOUR);
    this.tourService.registerFlowMap('dashboard', DASHBOARD_FLOW_MAP);
  }

  protected readonly canCustomize = computed(() =>
    this.dashboardConfigSvc.canCustomize()
  );

  private readonly hasKnownRole = computed(() =>
    this.permissionService.isRole(...KNOWN_ROLES)
  );

  protected readonly widgetConfigs = computed(() => {
    const saved = this.permissionService.dashboardWidgets();
    if (saved.length > 0) return saved;
    // Fall back to the default layout only when the user has a recognised role.
    // Users with no role get an empty array which triggers the no-access screen.
    if (this.hasKnownRole()) {
      return DEFAULT_WIDGET_KEYS.map((key, i) => ({
        key, order: i,
        colSpan:    widgetByKey(key)?.defaultColSpan ?? 1,
        rowSpan:    widgetByKey(key)?.defaultRowSpan ?? 1,
        configJson: null,
      }));
    }
    return [];
  });

  protected readonly widgetItems = computed<WidgetItem[]>(() =>
    this.widgetConfigs().map(cfg => {
      const def = widgetByKey(cfg.key);
      return {
        key:       cfg.key,
        colSpan:   cfg.colSpan,
        rowSpan:   cfg.rowSpan,
        component: def?.component ?? WidgetPlaceholderComponent,
        inputs: {
          widgetKey:   cfg.key,
          widgetLabel: def?.label,
          widgetIcon:  def?.icon,
        },
      };
    })
  );

  protected logout(): void {
    void this.authService.logout();
  }
}

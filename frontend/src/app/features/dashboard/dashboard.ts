import { Component, inject, signal, computed, OnInit, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { AuthService } from '../../core/auth/auth.service';
import { AdminDashboardComponent } from './admin/admin-dashboard.component';
import { FrontOfficeDashboardComponent } from './front-office/front-office-dashboard.component';
import { FacultyDashboardComponent } from './faculty/faculty-dashboard.component';
import { CashierDashboardComponent } from './cashier/cashier-dashboard.component';
import { DashboardTab } from './shared/tabs/dashboard-tabs.component';

const SESSION_KEY = 'cms_dashboard_active_role';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    AdminDashboardComponent,
    FrontOfficeDashboardComponent,
    FacultyDashboardComponent,
    CashierDashboardComponent,
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class DashboardComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly platformId = inject(PLATFORM_ID);

  protected readonly dashboardRoles = this.authService.dashboardRoles;

  protected readonly showTabs = computed(() => this.dashboardRoles().length > 1);

  protected readonly activeRole = signal<string>('');

  protected readonly tabConfig = computed((): DashboardTab[] => {
    const roleLabels: Record<string, { label: string; icon: string; color: string }> = {
      ROLE_ADMIN:         { label: 'Developer Admin', icon: 'admin_panel_settings', color: '#6366f1' },
      ROLE_COLLEGE_ADMIN: { label: 'College Admin',   icon: 'domain',               color: '#0284c7' },
      ROLE_FRONT_OFFICE:  { label: 'Front Office',    icon: 'contact_phone',        color: '#0ea5e9' },
      ROLE_CASHIER:       { label: 'Cashier',         icon: 'account_balance',      color: '#0f766e' },
      ROLE_FACULTY:       { label: 'Faculty',         icon: 'school',               color: '#10b981' },
      ROLE_LAB_INCHARGE:  { label: 'Lab Incharge',    icon: 'science',              color: '#8b5cf6' },
      ROLE_TECHNICIAN:    { label: 'Technician',      icon: 'build',                color: '#f59e0b' },
      ROLE_STUDENT:       { label: 'Student',         icon: 'person',               color: '#06b6d4' },
      ROLE_PARENT:        { label: 'Parent',          icon: 'family_restroom',      color: '#ec4899' },
    };
    return this.dashboardRoles().map((role) => ({
      role,
      ...(roleLabels[role] ?? { label: role, icon: 'dashboard', color: '#6366f1' }),
    }));
  });

  ngOnInit(): void {
    const roles = this.dashboardRoles();
    if (roles.length === 0) return;

    // Restore persisted active role from sessionStorage
    let restored = '';
    if (isPlatformBrowser(this.platformId)) {
      restored = sessionStorage.getItem(SESSION_KEY) ?? '';
    }
    this.activeRole.set(roles.includes(restored) ? restored : roles[0]);
  }

  protected setActiveRole(role: string): void {
    this.activeRole.set(role);
    if (isPlatformBrowser(this.platformId)) {
      sessionStorage.setItem(SESSION_KEY, role);
    }
  }
}



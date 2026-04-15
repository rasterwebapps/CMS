import { Component, inject, signal, computed, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatExpansionModule } from '@angular/material/expansion';
import { AuthService } from './core/auth/auth.service';

interface NavItem {
  label: string;
  icon: string;
  route: string;
  roles?: string[];
}

interface NavGroup {
  label: string;
  icon: string;
  items: NavItem[];
}

type NavEntry = NavItem | NavGroup;

function isNavGroup(entry: NavEntry): entry is NavGroup {
  return 'items' in entry;
}

@Component({
  selector: 'app-root',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatSidenavModule,
    MatToolbarModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    MatTooltipModule,
    MatExpansionModule,
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly authService = inject(AuthService);
  private readonly platformId = inject(PLATFORM_ID);

  protected readonly darkTheme = signal(false);
  protected readonly sidenavOpened = signal(true);
  protected readonly isNavGroup = isNavGroup;

  private static readonly EXPANDED_GROUPS_KEY = 'cms_nav_expanded_groups';

  private readonly navEntries: NavEntry[] = [
    { label: 'Dashboard', icon: 'dashboard', route: '/dashboard' },
    {
      label: 'Masters / Preferences',
      icon: 'tune',
      items: [
        { label: 'Departments', icon: 'business', route: '/departments' },
        { label: 'Programs', icon: 'school', route: '/programs' },
        { label: 'Courses', icon: 'menu_book', route: '/courses' },
        { label: 'Academic Years', icon: 'calendar_month', route: '/academic-years' },
        { label: 'Semesters', icon: 'date_range', route: '/semesters' },
        { label: 'Labs', icon: 'science', route: '/labs' },
        { label: 'Fee Structures', icon: 'account_balance', route: '/fee-structures' },
        { label: 'Equipment', icon: 'devices', route: '/equipment' },
        { label: 'Settings', icon: 'settings', route: '/settings', roles: ['ROLE_ADMIN'] },
      ],
    },
    {
      label: 'Admission Management',
      icon: 'how_to_reg',
      items: [
        { label: 'Enquiries', icon: 'contact_mail', route: '/enquiries' },
        { label: 'Agents', icon: 'support_agent', route: '/agents', roles: ['ROLE_ADMIN'] },
        { label: 'Faculty', icon: 'groups', route: '/faculty' },
        { label: 'Students', icon: 'person', route: '/students' },
      ],
    },
    {
      label: 'Curriculum & Academics',
      icon: 'auto_stories',
      items: [
        { label: 'Syllabi', icon: 'library_books', route: '/syllabi' },
        { label: 'Experiments', icon: 'biotech', route: '/experiments' },
        { label: 'CO/PO Mapping', icon: 'account_tree', route: '/curriculum-mappings' },
        { label: 'Lab Schedules', icon: 'calendar_view_week', route: '/lab-schedules' },
        { label: 'Attendance', icon: 'fact_check', route: '/attendance' },
      ],
    },
    {
      label: 'Examinations',
      icon: 'quiz',
      items: [
        { label: 'Examinations', icon: 'quiz', route: '/examinations' },
        { label: 'Exam Results', icon: 'grade', route: '/exam-results' },
      ],
    },
    {
      label: 'Finance',
      icon: 'account_balance_wallet',
      items: [
        { label: 'Student Fees', icon: 'account_balance_wallet', route: '/student-fees' },
        { label: 'Fee Payments', icon: 'payments', route: '/fee-payments' },
        {
          label: 'Fee Finalization',
          icon: 'lock',
          route: '/student-fees/finalize',
          roles: ['ROLE_ADMIN'],
        },
      ],
    },
    {
      label: 'Lab & Infrastructure',
      icon: 'construction',
      items: [
        { label: 'Inventory', icon: 'inventory_2', route: '/inventory' },
        { label: 'Maintenance', icon: 'build', route: '/maintenance' },
      ],
    },
    { label: 'Reports', icon: 'assessment', route: '/reports' },
  ];

  protected readonly expandedGroups = signal<Record<string, boolean>>(this.loadExpandedGroups());

  protected readonly filteredNavEntries = computed(() => {
    return this.navEntries
      .map((entry) => {
        if (isNavGroup(entry)) {
          const filteredItems = entry.items.filter((item) => {
            if (!item.roles || item.roles.length === 0) {
              return true;
            }
            return item.roles.some((role) => this.authService.hasRole(role));
          });
          if (filteredItems.length === 0) {
            return null;
          }
          return { ...entry, items: filteredItems };
        }
        if (!entry.roles || entry.roles.length === 0) {
          return entry;
        }
        return entry.roles.some((role) => this.authService.hasRole(role)) ? entry : null;
      })
      .filter((entry): entry is NavEntry => entry !== null);
  });

  protected setGroupExpanded(groupLabel: string, expanded: boolean): void {
    this.expandedGroups.update((groups) => {
      const updated = { ...groups, [groupLabel]: expanded };
      this.saveExpandedGroups(updated);
      return updated;
    });
  }

  protected isGroupExpanded(groupLabel: string): boolean {
    return this.expandedGroups()[groupLabel] ?? false;
  }

  private loadExpandedGroups(): Record<string, boolean> {
    if (isPlatformBrowser(this.platformId)) {
      try {
        const stored = localStorage.getItem(App.EXPANDED_GROUPS_KEY);
        if (stored) {
          return JSON.parse(stored) as Record<string, boolean>;
        }
      } catch {
        // Ignore parse errors
      }
    }
    return {};
  }

  private saveExpandedGroups(groups: Record<string, boolean>): void {
    if (isPlatformBrowser(this.platformId)) {
      try {
        localStorage.setItem(App.EXPANDED_GROUPS_KEY, JSON.stringify(groups));
      } catch {
        // Ignore storage errors
      }
    }
  }

  protected toggleTheme(): void {
    this.darkTheme.update((v) => !v);
    if (isPlatformBrowser(this.platformId)) {
      const html = document.documentElement;
      if (this.darkTheme()) {
        html.classList.add('dark-theme');
        html.classList.remove('light-theme');
      } else {
        html.classList.add('light-theme');
        html.classList.remove('dark-theme');
      }
    }
  }

  protected toggleSidenav(): void {
    this.sidenavOpened.update((v) => !v);
  }

  protected async logout(): Promise<void> {
    await this.authService.logout();
  }
}

import { Component, inject, signal, computed, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive, Router, NavigationEnd } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map } from 'rxjs';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatDivider } from '@angular/material/divider';
import { MatBadgeModule } from '@angular/material/badge';
import { AuthService } from './core/auth/auth.service';
import { ThemePickerComponent } from './shared/theme-picker/theme-picker.component';
import { GlobalSearchComponent } from './shared/global-search/global-search.component';

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

// Routes that activate focus mode (collapse global toolbar to maximise vertical space)
const FOCUS_MODE_PATTERNS: RegExp[] = [
  /\/admissions\/new$/,
  /\/admissions\/[^/]+\/edit$/,
  /\/students\/new$/,
  /\/students\/[^/]+\/edit$/,
  /\/student-fees\/finalize$/,
  /\/student-fees\/collect-payment$/,
  /\/fee-payments\/new$/,
  /\/fee-structures\/new$/,
  /\/fee-structures\/edit(\?.*)?$/,
  /\/enquiries\/new$/,
  /\/enquiries\/[^/]+\/edit$/,
  /\/enquiries\/[^/]+\/convert$/,
];

const FOCUS_MODE_TITLES: { pattern: RegExp; title: string }[] = [
  { pattern: /\/admissions\/new$/, title: 'New Admission' },
  { pattern: /\/admissions\/[^/]+\/edit$/, title: 'Edit Admission' },
  { pattern: /\/students\/new$/, title: 'New Student Registration' },
  { pattern: /\/students\/[^/]+\/edit$/, title: 'Edit Student' },
  { pattern: /\/student-fees\/finalize$/, title: 'Fee Finalization' },
  { pattern: /\/student-fees\/collect-payment$/, title: 'Collect Payment' },
  { pattern: /\/fee-payments\/new$/, title: 'New Fee Payment' },
  { pattern: /\/fee-structures\/new$/, title: 'New Fee Structure' },
  { pattern: /\/fee-structures\/edit(\?.*)?$/, title: 'Edit Fee Structure' },
  { pattern: /\/enquiries\/new$/, title: 'New Enquiry' },
  { pattern: /\/enquiries\/[^/]+\/edit$/, title: 'Edit Enquiry' },
  { pattern: /\/enquiries\/[^/]+\/convert$/, title: 'Create Admission' },
];

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
    MatDivider,
    MatBadgeModule,
    ThemePickerComponent,
    GlobalSearchComponent,
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly authService = inject(AuthService);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly router = inject(Router);

  protected readonly darkTheme = signal(false);
  protected readonly sidenavCollapsed = signal(this.loadCollapsedState());
  protected readonly menuSearch = signal('');
  protected readonly toolbarLogoError = signal(false);
  protected readonly sidenavLogoError = signal(false);
  protected readonly notificationCount = signal(0);
  protected readonly isNavGroup = isNavGroup;

  private static readonly EXPANDED_GROUPS_KEY = 'cms_nav_expanded_groups';
  private static readonly COLLAPSED_KEY = 'cms_sidenav_collapsed';

  private readonly currentUrl = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map((e) => e.urlAfterRedirects),
    ),
    { initialValue: this.router.url },
  );

  protected readonly focusMode = computed(() => {
    const url = this.currentUrl() ?? '';
    return FOCUS_MODE_PATTERNS.some((p) => p.test(url));
  });

  protected readonly focusModeTitle = computed(() => {
    const url = this.currentUrl() ?? '';
    const match = FOCUS_MODE_TITLES.find((t) => t.pattern.test(url));
    return match?.title ?? 'Form';
  });

  private readonly CMS_ROLE_NAMES: Record<string, string> = {
    ROLE_ADMIN: 'Admin',
    ROLE_FACULTY: 'Faculty',
    ROLE_STUDENT: 'Student',
    ROLE_LAB_INCHARGE: 'Lab Incharge',
    ROLE_TECHNICIAN: 'Technician',
    ROLE_PARENT: 'Parent',
    ROLE_FRONT_OFFICE: 'Front Office',
  };

  protected readonly primaryRole = computed(() => {
    const priority = [
      'ROLE_ADMIN',
      'ROLE_FACULTY',
      'ROLE_LAB_INCHARGE',
      'ROLE_TECHNICIAN',
      'ROLE_FRONT_OFFICE',
      'ROLE_STUDENT',
      'ROLE_PARENT',
    ];
    const role = priority.find((r) => this.authService.roles().includes(r));
    return role ? (this.CMS_ROLE_NAMES[role] ?? '') : '';
  });

  private readonly navEntries: NavEntry[] = [
    { label: 'Dashboard', icon: 'dashboard', route: '/dashboard' },
    {
      label: 'Preferences',
      icon: 'tune',
      items: [
        { label: 'Departments', icon: 'business', route: '/departments' },
        { label: 'Programs', icon: 'school', route: '/programs' },
        { label: 'Courses', icon: 'menu_book', route: '/courses' },
        { label: 'Academic Years', icon: 'calendar_month', route: '/academic-years' },
        { label: 'Semesters', icon: 'date_range', route: '/semesters' },
        { label: 'Academic Calendar', icon: 'event_note', route: '/academic-calendar' },
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
        { label: 'Admissions', icon: 'assignment_ind', route: '/admissions' },
        { label: 'Agents', icon: 'support_agent', route: '/agents', roles: ['ROLE_ADMIN'] },
        {
          label: 'Referral Types',
          icon: 'share',
          route: '/referral-types',
          roles: ['ROLE_ADMIN'],
        },
        { label: 'Faculty', icon: 'groups', route: '/faculty' },
        { label: 'Students', icon: 'person', route: '/students' },
        { label: 'Roll Number Assignment', icon: 'tag', route: '/students/roll-numbers', roles: ['ROLE_ADMIN'] },
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
        { label: 'Fee Payments', icon: 'payments', route: '/fee-payments', roles: ['ROLE_ADMIN', 'ROLE_FRONT_OFFICE'] },
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
    const search = this.menuSearch().toLowerCase().trim();
    return this.navEntries
      .map((entry) => {
        if (isNavGroup(entry)) {
          let filteredItems = entry.items.filter((item) => {
            if (!item.roles || item.roles.length === 0) {
              return true;
            }
            return item.roles.some((role) => this.authService.hasRole(role));
          });

          if (search) {
            const groupMatches = entry.label.toLowerCase().includes(search);
            filteredItems = filteredItems.filter(
              (item) => groupMatches || item.label.toLowerCase().includes(search),
            );
          }

          if (filteredItems.length === 0) {
            return null;
          }
          return { ...entry, items: filteredItems };
        }
        if (!entry.roles || entry.roles.length === 0) {
          if (search && !entry.label.toLowerCase().includes(search)) return null;
          return entry;
        }
        if (!entry.roles.some((role) => this.authService.hasRole(role))) return null;
        if (search && !entry.label.toLowerCase().includes(search)) return null;
        return entry;
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
    this.sidenavCollapsed.update((v) => !v);
    if (isPlatformBrowser(this.platformId)) {
      try {
        localStorage.setItem(App.COLLAPSED_KEY, JSON.stringify(this.sidenavCollapsed()));
      } catch {
        // Ignore storage errors
      }
    }
  }

  private loadCollapsedState(): boolean {
    if (isPlatformBrowser(this.platformId)) {
      try {
        const stored = localStorage.getItem(App.COLLAPSED_KEY);
        if (stored) {
          return JSON.parse(stored) as boolean;
        }
      } catch {
        // Ignore parse errors
      }
    }
    return false;
  }

  protected onMenuSearchInput(event: Event): void {
    this.menuSearch.set((event.target as HTMLInputElement).value);
  }

  protected navigateBack(): void {
    window.history.back();
  }

  protected async logout(): Promise<void> {
    await this.authService.logout();
  }
}

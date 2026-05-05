import { Component, inject, signal, computed, PLATFORM_ID, OnInit } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive, Router, NavigationEnd } from '@angular/router';
import { HttpClient } from '@angular/common/http';
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
import { filter } from 'rxjs';
import { AuthService } from './core/auth/auth.service';
import { PermissionService } from './core/permissions/permission.service';
import { BreadcrumbService } from './core/breadcrumb/breadcrumb.service';
import { LayoutService } from './core/layout/layout.service';
import { ResponsiveService } from './core/layout/responsive.service';
import { KeyboardShortcutsService } from './core/shortcuts/keyboard-shortcuts.service';
import { ThemePickerComponent } from './shared/theme-picker/theme-picker.component';
import { GlobalSearchComponent } from './shared/global-search/global-search.component';
import { BreadcrumbBarComponent } from './shared/breadcrumb-bar/breadcrumb-bar.component';
import { ToastHostComponent } from './core/toast/toast-host.component';
import { TourService } from './core/tour/tour.service';
import { ONBOARDING_TOUR_STEPS } from './core/tour/tours/onboarding.tour';
import { environment } from '../environments';

interface NavItem {
  label: string;
  icon: string;
  route: string;
  /** DB permission codes — show only if user holds at least one. Empty/absent = visible to all. */
  permissions?: string[];
}

interface NavGroup {
  label: string;
  icon: string;
  items: NavItem[];
  /** DB permission codes — show only if user holds at least one. Empty/absent = visible to all. */
  permissions?: string[];
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
    MatDivider,
    MatBadgeModule,
    ThemePickerComponent,
    GlobalSearchComponent,
    BreadcrumbBarComponent,
    ToastHostComponent,
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App implements OnInit {
  protected readonly authService = inject(AuthService);
  protected readonly permissionService = inject(PermissionService);
  private readonly layoutService = inject(LayoutService);
  private readonly breadcrumbService = inject(BreadcrumbService);
  protected readonly responsiveService = inject(ResponsiveService);
  private readonly shortcutsService = inject(KeyboardShortcutsService);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly router = inject(Router);
  private readonly http = inject(HttpClient);
  private readonly tourService = inject(TourService);

  protected readonly darkTheme = signal(false);
  /**
   * `true` → user collapsed the sidenav into a 68 px icon rail.
   * Stored in `localStorage` under {@link App.COLLAPSED_KEY}. When the rail is
   * unpinned (i.e. `sidenavCollapsed() === true`), it expands as an overlay
   * while {@link hoverExpanded} is `true`.
   */
  protected readonly sidenavCollapsed = signal(this.loadCollapsedState());
  /** Tracks whether the unpinned rail is currently being hovered. */
  protected readonly hoverExpanded = signal(false);
  /** Mobile drawer open state. Independent of pinned/collapsed. */
  protected readonly mobileDrawerOpen = signal(false);
  protected readonly menuSearch = signal('');
  protected readonly toolbarLogoError = signal(false);
  protected readonly sidenavLogoError = signal(false);
  protected readonly notificationCount = signal(0);
  protected readonly enquiryBadgeCount = signal(0);
  protected readonly isNavGroup = isNavGroup;

  /** True only when the rail is unpinned AND not currently hover-expanded. */
  protected readonly sidenavRail = computed(
    () => this.sidenavCollapsed() && !this.hoverExpanded(),
  );

  protected readonly isMobile = this.responsiveService.isMobile;

  /** Current top-level section label derived from BreadcrumbService for the toolbar sub-label. */
  protected readonly currentSectionLabel = computed(() => {
    const crumbs = this.breadcrumbService.breadcrumbs();
    if (!crumbs || crumbs.length === 0) return '';
    // First crumb is the top-level section (e.g., "Dashboard", "Enquiries")
    return crumbs[0].label ?? '';
  });

  private static readonly EXPANDED_GROUPS_KEY = 'cms_nav_expanded_groups';
  private static readonly COLLAPSED_KEY = 'cms_sidenav_collapsed';

  protected readonly focusMode = this.layoutService.isFocusMode;
  protected readonly focusModeTitle = this.layoutService.focusModeTitle;

  private readonly CMS_ROLE_NAMES: Record<string, string> = {
    ROLE_ADMIN: 'Admin',
    ROLE_COLLEGE_ADMIN: 'College Admin',
    ROLE_CASHIER: 'Cashier',
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
      'ROLE_COLLEGE_ADMIN',
      'ROLE_CASHIER',
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
        { label: 'Departments',     icon: 'business',         route: '/departments',      permissions: ['DEPT_VIEW', 'DEPT_MANAGE'] },
        { label: 'Programs',        icon: 'school',           route: '/programs',         permissions: ['PROGRAM_VIEW', 'PROGRAM_MANAGE'] },
        { label: 'Courses',         icon: 'menu_book',        route: '/courses',          permissions: ['COURSE_VIEW', 'COURSE_MANAGE'] },
        { label: 'Academic Years',  icon: 'calendar_month',   route: '/academic-years',   permissions: ['ACADEMIC_YEAR_VIEW', 'ACADEMIC_YEAR_MANAGE'] },
        { label: 'Semesters',       icon: 'date_range',       route: '/semesters',        permissions: ['SEMESTER_VIEW', 'SEMESTER_MANAGE'] },
        { label: 'Academic Calendar', icon: 'event_note',     route: '/academic-calendar', permissions: ['ACADEMIC_YEAR_MANAGE'] },
        { label: 'Labs',            icon: 'science',          route: '/labs',             permissions: ['LAB_MANAGE'] },
        { label: 'Fee Structures',  icon: 'account_balance',  route: '/fee-structures',   permissions: ['FEE_STRUCTURE_VIEW', 'FEE_STRUCTURE_MANAGE'] },
        { label: 'Equipment',       icon: 'devices',          route: '/equipment',        permissions: ['EQUIPMENT_MANAGE'] },
        { label: 'Faculty',         icon: 'groups',           route: '/faculty',          permissions: ['FACULTY_VIEW', 'FACULTY_MANAGE'] },
        { label: 'Agents',          icon: 'support_agent',    route: '/agents',           permissions: ['AGENT_VIEW', 'AGENT_MANAGE'] },
        { label: 'Referral Types',  icon: 'share',            route: '/referral-types',   permissions: ['REFERRAL_TYPE_VIEW', 'REFERRAL_TYPE_MANAGE'] },
        { label: 'Communities',     icon: 'people',           route: '/communities',      permissions: ['COMMUNITY_VIEW', 'COMMUNITY_MANAGE'] },
        { label: 'Blood Groups',    icon: 'bloodtype',        route: '/blood-groups',     permissions: ['BLOOD_GROUP_VIEW', 'BLOOD_GROUP_MANAGE'] },
        { label: 'Settings',        icon: 'settings',         route: '/settings',         permissions: ['SETTINGS_MANAGE'] },
      ],
    },
    {
      label: 'Admission Management',
      icon: 'how_to_reg',
      items: [
        { label: 'Enquiries',            icon: 'contact_mail',  route: '/enquiries',                    permissions: ['ENQUIRY_VIEW', 'ENQUIRY_CREATE', 'ENQUIRY_EDIT'] },
        { label: 'Submit Documents',     icon: 'upload_file',   route: '/enquiries/document-submission', permissions: ['DOCUMENT_SUBMISSION_MANAGE'] },
        { label: 'Complete Admission',   icon: 'how_to_reg',    route: '/enquiries/admission-completion', permissions: ['ADMISSION_CREATE'] },
        { label: 'Admissions',           icon: 'assignment_ind', route: '/admissions',                  permissions: ['ADMISSION_VIEW', 'ADMISSION_CREATE', 'ADMISSION_EDIT'] },
        { label: 'Students',             icon: 'person',        route: '/students',                     permissions: ['STUDENT_VIEW', 'STUDENT_CREATE', 'STUDENT_EDIT'] },
        { label: 'Roll Number Assignment', icon: 'tag',         route: '/students/roll-numbers',        permissions: ['ROLL_NUMBER_ASSIGN'] },
        { label: 'Import Data',          icon: 'upload',        route: '/import',                       permissions: ['IMPORT_DATA'] },
      ],
    },
    {
      label: 'Curriculum & Academics',
      icon: 'auto_stories',
      items: [
        { label: 'Syllabi',             icon: 'library_books',      route: '/syllabi',            permissions: ['SYLLABUS_VIEW', 'SYLLABUS_MANAGE'] },
        { label: 'Experiments',         icon: 'biotech',            route: '/experiments',        permissions: ['EXPERIMENT_VIEW', 'EXPERIMENT_MANAGE'] },
        { label: 'CO/PO Mapping',       icon: 'account_tree',       route: '/curriculum-mappings', permissions: ['COPO_VIEW', 'COPO_MANAGE', 'CURRICULUM_VIEW'] },
        { label: 'Curriculum Versions', icon: 'layers',             route: '/curriculum-versions', permissions: ['CURRICULUM_VIEW', 'CURRICULUM_MANAGE'] },
        { label: 'Lab Schedules',       icon: 'calendar_view_week', route: '/lab-schedules',      permissions: ['LAB_SCHEDULE_VIEW', 'LAB_SCHEDULE_MANAGE'] },
        { label: 'Attendance',          icon: 'fact_check',         route: '/attendance',         permissions: ['ATTENDANCE_VIEW', 'ATTENDANCE_MANAGE'] },
      ],
    },
    {
      label: 'Examinations',
      icon: 'quiz',
      items: [
        { label: 'Examinations', icon: 'quiz',  route: '/examinations', permissions: ['EXAMINATION_VIEW', 'EXAMINATION_MANAGE'] },
        { label: 'Exam Results', icon: 'grade', route: '/exam-results',  permissions: ['EXAM_RESULT_VIEW', 'EXAM_RESULT_MANAGE'] },
      ],
    },
    {
      label: 'Finance',
      icon: 'account_balance_wallet',
      items: [
        { label: 'Student Fees',    icon: 'account_balance_wallet', route: '/student-fees',        permissions: ['STUDENT_FEE_VIEW', 'STUDENT_FEE_MANAGE'] },
        { label: 'Fee Collection',  icon: 'payments',               route: '/fee-collection',      permissions: ['FEE_COLLECT'] },
        { label: 'Fee Finalization', icon: 'lock',                  route: '/student-fees/finalize', permissions: ['FEE_FINALIZE', 'FEE_STRUCTURE_MANAGE'] },
      ],
    },
    {
      label: 'Lab & Infrastructure',
      icon: 'construction',
      items: [
        { label: 'Inventory',   icon: 'inventory_2', route: '/inventory',   permissions: ['INVENTORY_VIEW', 'INVENTORY_MANAGE'] },
        { label: 'Maintenance', icon: 'build',       route: '/maintenance', permissions: ['MAINTENANCE_VIEW', 'MAINTENANCE_MANAGE'] },
      ],
    },
    { label: 'Reports',     icon: 'assessment',     route: '/reports',      permissions: ['REPORT_VIEW'] },
    { label: 'Fee Reports', icon: 'request_quote',  route: '/fee-reports',  permissions: ['FEE_REPORT_VIEW'] },
    {
      label: 'Administration',
      icon: 'admin_panel_settings',
      items: [
        { label: 'User Management',    icon: 'manage_accounts', route: '/user-management', permissions: ['USER_VIEW'] },
        { label: 'Roles & Permissions', icon: 'shield',         route: '/role-management', permissions: ['ROLE_VIEW'] },
      ],
    },
  ];

  protected readonly expandedGroups = signal<Record<string, boolean>>(this.loadExpandedGroups());

  protected readonly filteredNavEntries = computed(() => {
    const search = this.menuSearch().toLowerCase().trim();
    return this.navEntries
      .map((entry) => {
        if (isNavGroup(entry)) {
          // Check if user has access to the group itself (group-level permission guard)
          if (entry.permissions && entry.permissions.length > 0) {
            if (!this.permissionService.hasAny(...entry.permissions)) {
              return null;
            }
          }

          let filteredItems = entry.items.filter((item) => {
            if (!item.permissions || item.permissions.length === 0) {
              return true;
            }
            return this.permissionService.hasAny(...item.permissions);
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
        if (!entry.permissions || entry.permissions.length === 0) {
          if (search && !entry.label.toLowerCase().includes(search)) return null;
          return entry;
        }
        if (!this.permissionService.hasAny(...entry.permissions)) return null;
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

  ngOnInit(): void {
    // Install global keyboard shortcuts (g-leader navigation + ? cheat-sheet).
    this.shortcutsService.install();

    // Register the onboarding tour and auto-start it for first-time users.
    if (isPlatformBrowser(this.platformId)) {
      this.tourService.registerTour('onboarding', ONBOARDING_TOUR_STEPS);
      this.tourService.maybeAutoStart('onboarding');
    }

    // Auto-close the mobile drawer on route changes.
    this.router.events.pipe(filter((e) => e instanceof NavigationEnd)).subscribe(() => {
      if (this.isMobile()) {
        this.mobileDrawerOpen.set(false);
      }
    });

    // Fetch enquiry badge count for admin users from the dashboard summary endpoint.
    // This avoids a separate API call by reusing the enquiry funnel data.
    if (isPlatformBrowser(this.platformId) && this.authService.isAdmin()) {
      this.http
        .get<{ enquiryFunnel?: Record<string, number> }>(`${environment.apiUrl}/dashboard/summary`)
        .subscribe({
          next: (data) => {
            const enquired = data.enquiryFunnel?.['ENQUIRED'] ?? 0;
            const interested = data.enquiryFunnel?.['INTERESTED'] ?? 0;
            this.enquiryBadgeCount.set(enquired + interested);
          },
          error: () => { /* silently ignore badge fetch errors */ },
        });
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

  /**
   * Toggles the *pinned* state of the sidenav. When unpinned, the rail
   * collapses to a 68 px icon strip and expands as an overlay on hover.
   */
  protected toggleSidenav(): void {
    this.sidenavCollapsed.update((v) => !v);
    // Drop any active hover state when the user explicitly pins/unpins.
    this.hoverExpanded.set(false);
    if (isPlatformBrowser(this.platformId)) {
      try {
        localStorage.setItem(App.COLLAPSED_KEY, JSON.stringify(this.sidenavCollapsed()));
      } catch {
        // Ignore storage errors
      }
    }
  }

  /** Hover-expand handlers — only active when the rail is unpinned. */
  protected onSidenavMouseEnter(): void {
    if (this.sidenavCollapsed() && !this.isMobile()) {
      this.hoverExpanded.set(true);
    }
  }

  protected onSidenavMouseLeave(): void {
    if (this.hoverExpanded()) {
      this.hoverExpanded.set(false);
    }
  }

  protected toggleMobileDrawer(): void {
    this.mobileDrawerOpen.update((v) => !v);
  }

  protected closeMobileDrawer(): void {
    this.mobileDrawerOpen.set(false);
  }

  protected openKeyboardShortcuts(): void {
    this.shortcutsService.openCheatSheet();
  }

  /** Starts the onboarding tour regardless of the "don't show again" preference. */
  protected startTour(): void {
    this.tourService.startTour('onboarding');
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

  /** Animation key used by the root `<router-outlet>` `[@routeAnim]` binding.
   *  Prefer the route's static config path so dynamic params (e.g. `:id`) don't
   *  re-trigger the transition when the same component stays mounted. */
  protected getRouteAnimationData(outlet: RouterOutlet): unknown {
    if (!outlet?.isActivated) return '';
    const data = outlet.activatedRouteData?.['animation'];
    if (data) return data;
    return outlet.activatedRoute?.snapshot?.routeConfig?.path ?? '';
  }

  protected async logout(): Promise<void> {
    await this.authService.logout();
  }
}

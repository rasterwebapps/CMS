import { Component, ElementRef, HostListener, ViewChild, inject, signal, computed, PLATFORM_ID, OnInit, AfterViewInit, NgZone } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive, Router, NavigationEnd } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule, MatMenuTrigger } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatDivider } from '@angular/material/divider';
import { MatBadgeModule } from '@angular/material/badge';
import { filter } from 'rxjs';
import { AuthService } from './core/auth/auth.service';
import { PermissionService } from './core/permissions/permission.service';
import { LayoutService } from './core/layout/layout.service';
import { ResponsiveService } from './core/layout/responsive.service';
import { KeyboardShortcutsService } from './core/shortcuts/keyboard-shortcuts.service';
import { GlobalSearchComponent } from './shared/global-search/global-search.component';
import { BreadcrumbBarComponent } from './shared/breadcrumb-bar/breadcrumb-bar.component';
import { ToastHostComponent } from './core/toast/toast-host.component';
import { TourService, ONBOARDING_TOUR_STEPS } from './core/tour';
import { ProfileService } from './features/profile/profile.service';
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
    GlobalSearchComponent,
    BreadcrumbBarComponent,
    ToastHostComponent,
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App implements OnInit, AfterViewInit {
  protected readonly authService = inject(AuthService);
  protected readonly permissionService = inject(PermissionService);
  protected readonly profileService = inject(ProfileService);
  private readonly layoutService = inject(LayoutService);
  protected readonly responsiveService = inject(ResponsiveService);
  private readonly shortcutsService = inject(KeyboardShortcutsService);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly router = inject(Router);
  private readonly http = inject(HttpClient);
  private readonly tourService = inject(TourService);

  private readonly ngZone = inject(NgZone);

  /** Tracks the current route URL so collapsed-group active state is reactive. */
  private readonly currentUrl = signal(this.router.url);

  /** Shared timer for hover-open / hover-close delays on the collapsed flyout. */
  private hoverMenuTimer: ReturnType<typeof setTimeout> | null = null;
  /** The trigger whose flyout is currently visible. */
  private activeMenuTrigger: MatMenuTrigger | null = null;

  protected readonly navSearchActive = signal(false);
  @ViewChild('navSearchInput') private navSearchInputRef?: ElementRef<HTMLInputElement>;

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
  protected readonly notificationCount = signal(0);
  protected readonly enquiryBadgeCount = signal(0);
  protected readonly isNavGroup = isNavGroup;

  /** True only when the rail is unpinned AND not currently hover-expanded. */
  protected readonly sidenavRail = computed(
    () => this.sidenavCollapsed() && !this.hoverExpanded(),
  );

  protected readonly isMobile = this.responsiveService.isMobile;

  private static readonly EXPANDED_GROUPS_KEY = 'cms_nav_expanded_groups';
  private static readonly COLLAPSED_KEY = 'cms_sidenav_collapsed';

  protected readonly focusMode = this.layoutService.isFocusMode;
  protected readonly focusModeTitle = this.layoutService.focusModeTitle;

  /** 1–2 letter initials derived from the Keycloak username. */
  protected readonly userInitials = computed(() => {
    const name = this.authService.username() ?? '';
    const parts = name.split(/[\s_\-\.]+/).filter(Boolean);
    if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase();
    return name.slice(0, 2).toUpperCase();
  });

  private readonly navEntries: NavEntry[] = [
    // 1. Overview — primary landmarks
    {
      label: 'Overview',
      icon: 'home',
      items: [
        { label: 'Dashboard',  icon: 'dashboard',      route: '/dashboard' },
        { label: 'My Profile', icon: 'account_circle', route: '/profile' },
      ],
    },
    // 2. Admission Management
    {
      label: 'Admission Management',
      icon: 'how_to_reg',
      items: [
        { label: 'Enquiries',          icon: 'contact_mail',   route: '/enquiries',                     permissions: ['ENQUIRY_VIEW', 'ENQUIRY_CREATE', 'ENQUIRY_EDIT'] },
        { label: 'Finalize Fee',       icon: 'lock',           route: '/student-fees/finalize',         permissions: ['FEE_FINALIZE'] },
        { label: 'Collect Payment',    icon: 'payments',       route: '/fee-collection',                permissions: ['FEE_COLLECT'] },
        { label: 'Submit Documents',   icon: 'upload_file',    route: '/enquiries/document-submission',  permissions: ['DOCUMENT_SUBMISSION_VIEW', 'DOCUMENT_SUBMISSION_MANAGE'] },
        { label: 'Verify Documents',   icon: 'verified',       route: '/enquiries/document-verification',permissions: ['DOCUMENT_VERIFICATION_MANAGE'] },
        { label: 'Complete Admission', icon: 'how_to_reg',     route: '/enquiries/admission-completion', permissions: ['ADMISSION_CREATE'] },
        { label: 'Admission Explorer', icon: 'assignment_ind', route: '/admissions',                    permissions: ['ADMISSION_VIEW', 'ADMISSION_CREATE', 'ADMISSION_EDIT'] },
      ],
    },
    // 3. Student Management
    {
      label: 'Student Management',
      icon: 'person',
      items: [
        { label: 'Student Explorer',       icon: 'person',   route: '/students',               permissions: ['STUDENT_VIEW', 'STUDENT_CREATE', 'STUDENT_EDIT'] },
        { label: 'Assign Roll Numbers',    icon: 'tag',      route: '/students/roll-numbers',  permissions: ['ROLL_NUMBER_ASSIGN'] },
        { label: 'Scholarship Applications', icon: 'verified', route: '/scholarship-applications', permissions: ['SCHOLARSHIP_APPROVE'] },
        { label: 'Data Import',            icon: 'upload',   route: '/import',                 permissions: ['IMPORT_DATA'] },
      ],
    },
    // 4. Finance
    {
      label: 'Finance',
      icon: 'account_balance_wallet',
      items: [
        { label: 'Fee Explorer', icon: 'account_balance_wallet', route: '/student-fees', permissions: ['STUDENT_FEE_VIEW', 'STUDENT_FEE_MANAGE'] },
        { label: 'Receipts',     icon: 'receipt_long',           route: '/receipts',     permissions: ['RECEIPT_VIEW'] },
      ],
    },
    // 5. Academics
    {
      label: 'Academics',
      icon: 'auto_stories',
      items: [
        { label: 'Syllabus',            icon: 'library_books',      route: '/syllabi',             permissions: ['SYLLABUS_VIEW', 'SYLLABUS_MANAGE'] },
        { label: 'Experiments',         icon: 'biotech',            route: '/experiments',         permissions: ['EXPERIMENT_VIEW', 'EXPERIMENT_MANAGE'] },
        { label: 'CO/PO Mapping',       icon: 'account_tree',       route: '/curriculum-mappings', permissions: ['COPO_VIEW', 'COPO_MANAGE', 'CURRICULUM_VIEW'] },
        { label: 'Curriculum Versions', icon: 'layers',             route: '/curriculum-versions', permissions: ['CURRICULUM_VIEW', 'CURRICULUM_MANAGE'] },
        { label: 'Lab Schedules',       icon: 'calendar_view_week', route: '/lab-schedules',       permissions: ['LAB_SCHEDULE_VIEW', 'LAB_SCHEDULE_MANAGE'] },
        { label: 'Attendance',          icon: 'fact_check',         route: '/attendance',          permissions: ['ATTENDANCE_VIEW', 'ATTENDANCE_MANAGE'] },
        { label: 'Manage Exams',        icon: 'quiz',               route: '/examinations',        permissions: ['EXAMINATION_VIEW', 'EXAMINATION_MANAGE'] },
        { label: 'Exam Results',        icon: 'grade',              route: '/exam-results',        permissions: ['EXAM_RESULT_VIEW', 'EXAM_RESULT_MANAGE'] },
      ],
    },
    // 6. Infrastructure
    {
      label: 'Infrastructure',
      icon: 'construction',
      items: [
        { label: 'Inventory',   icon: 'inventory_2', route: '/inventory',   permissions: ['INVENTORY_VIEW', 'INVENTORY_MANAGE'] },
        { label: 'Maintenance', icon: 'build',       route: '/maintenance', permissions: ['MAINTENANCE_VIEW', 'MAINTENANCE_MANAGE'] },
      ],
    },
    // 7. Reports & Analytics
    {
      label: 'Reports & Analytics',
      icon: 'analytics',
      items: [
        { label: 'General Reports', icon: 'assessment',    route: '/reports',     permissions: ['REPORT_VIEW'] },
        { label: 'Fee Reports',     icon: 'request_quote', route: '/fee-reports', permissions: ['FEE_REPORT_VIEW'] },
      ],
    },
    // 8. Preferences
    {
      label: 'Preferences',
      icon: 'tune',
      items: [
        { label: 'Departments',        icon: 'business',         route: '/departments',             permissions: ['DEPT_VIEW', 'DEPT_MANAGE'] },
        { label: 'Programs',           icon: 'school',           route: '/programs',                permissions: ['PROGRAM_VIEW', 'PROGRAM_MANAGE'] },
        { label: 'Courses',            icon: 'menu_book',        route: '/courses',                 permissions: ['COURSE_VIEW', 'COURSE_MANAGE'] },
        { label: 'Academic Years',     icon: 'calendar_month',   route: '/academic-years',          permissions: ['ACADEMIC_YEAR_VIEW', 'ACADEMIC_YEAR_MANAGE'] },
        { label: 'Academic Calendar',  icon: 'event_note',       route: '/academic-calendar',       permissions: ['ACADEMIC_YEAR_MANAGE'] },
        { label: 'Number Sequences',   icon: 'tag',              route: '/number-sequences',        permissions: ['NUMBER_SEQUENCE_VIEW'] },
        { label: 'Labs',               icon: 'science',          route: '/labs',                    permissions: ['LAB_VIEW', 'LAB_MANAGE'] },
        { label: 'Fee Structures',     icon: 'account_balance',  route: '/fee-structures',          permissions: ['FEE_STRUCTURE_VIEW', 'FEE_STRUCTURE_MANAGE'] },
        { label: 'Equipment',          icon: 'devices',          route: '/equipment',               permissions: ['EQUIPMENT_VIEW', 'EQUIPMENT_MANAGE'] },
        { label: 'Faculty',            icon: 'groups',           route: '/faculty',                 permissions: ['FACULTY_VIEW', 'FACULTY_MANAGE'] },
        { label: 'Faculty Doc Config',   icon: 'rule',          route: '/faculty/document-config', permissions: ['FACULTY_MANAGE'] },
        { label: 'Scholarship Types',    icon: 'school',        route: '/scholarships',             permissions: ['SCHOLARSHIP_MANAGE'] },
        { label: 'Agents',               icon: 'support_agent', route: '/agents',                  permissions: ['AGENT_VIEW', 'AGENT_MANAGE'] },
        { label: 'Referral Types',     icon: 'share',            route: '/referral-types',          permissions: ['REFERRAL_TYPE_VIEW', 'REFERRAL_TYPE_MANAGE'] },
        { label: 'Communities',        icon: 'people',           route: '/communities',             permissions: ['COMMUNITY_VIEW', 'COMMUNITY_MANAGE'] },
        { label: 'Blood Groups',       icon: 'bloodtype',        route: '/blood-groups',            permissions: ['BLOOD_GROUP_VIEW', 'BLOOD_GROUP_MANAGE'] },
        { label: 'Location Master',    icon: 'public',           route: '/india-locations',         permissions: ['INDIA_LOCATION_VIEW', 'INDIA_LOCATION_MANAGE'] },
        { label: 'Settings',           icon: 'settings',         route: '/settings',                permissions: ['SETTINGS_VIEW', 'SETTINGS_MANAGE'] },
      ],
    },
    // 9. User Management
    {
      label: 'User Management',
      icon: 'manage_accounts',
      items: [
        { label: 'Users',               icon: 'manage_accounts', route: '/user-management', permissions: ['USER_VIEW'] },
        { label: 'Roles & Permissions', icon: 'shield',          route: '/role-management', permissions: ['ROLE_VIEW'] },
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
    // Plain toggle — only collapse-on-navigate (syncExpandedGroupToRoute) enforces
    // the one-open-at-a-time rule. Clicking a header must never collapse the
    // currently active section or disturb the selected screen.
    this.expandedGroups.update((groups) => {
      const updated = { ...groups, [groupLabel]: expanded };
      this.saveExpandedGroups(updated);
      return updated;
    });
  }

  protected isGroupExpanded(groupLabel: string): boolean {
    return this.expandedGroups()[groupLabel] ?? false;
  }

  /**
   * Expands the group that owns the current route and collapses all others.
   * Called on initial load and after every navigation so the sidenav always
   * reflects exactly where the user is.
   */
  private syncExpandedGroupToRoute(url: string): void {
    for (const entry of this.navEntries) {
      if (!isNavGroup(entry)) continue;
      const ownsRoute = entry.items.some(
        (item) => url === item.route || url.startsWith(item.route + '/'),
      );
      if (ownsRoute) {
        const next: Record<string, boolean> = {};
        for (const e of this.navEntries) {
          if (isNavGroup(e)) next[e.label] = e.label === entry.label;
        }
        this.expandedGroups.set(next);
        this.saveExpandedGroups(next);
        return;
      }
    }
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

    // Load initial profile avatar for the toolbar.
    this.profileService.loadAvatar();

    // Sync expanded group on initial load
    this.syncExpandedGroupToRoute(this.router.url);

    // Keep currentUrl signal in sync, auto-expand the active group, auto-close mobile drawer.
    this.router.events.pipe(filter((e) => e instanceof NavigationEnd)).subscribe((e) => {
      const url = (e as NavigationEnd).urlAfterRedirects;
      this.currentUrl.set(url);
      this.syncExpandedGroupToRoute(url);
      this.navSearchActive.set(false);
      this.menuSearch.set('');
      if (this.isMobile()) {
        this.mobileDrawerOpen.set(false);
      }
    });

    // Fetch enquiry badge count for users who can view enquiry workflows.
    if (isPlatformBrowser(this.platformId) && this.permissionService.has('ENQUIRY_VIEW')) {
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

  ngAfterViewInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    // Register and conditionally auto-start the onboarding tour.
    // Delayed until after the first render+layout so that getBoundingClientRect()
    // returns real positions instead of zeros, and @if blocks have resolved.
    this.tourService.registerTour('onboarding', ONBOARDING_TOUR_STEPS);
    setTimeout(() => this.tourService.maybeAutoStart('onboarding'), 900);
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

  /**
   * Mobile-only: opens/closes the navigation drawer. The drawer is in
   * `over` mode on mobile so toggling it doesn't push content.
   */
  protected toggleMobileDrawer(): void {
    this.mobileDrawerOpen.update((v) => !v);
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

  protected activateNavSearch(): void {
    this.navSearchActive.set(true);
    setTimeout(() => this.navSearchInputRef?.nativeElement.focus(), 0);
  }

  protected clearNavSearch(): void {
    this.menuSearch.set('');
    this.navSearchActive.set(false);
  }

  protected onNavSearchKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Escape') this.clearNavSearch();
  }

  @HostListener('document:keydown', ['$event'])
  protected onDocumentSlashKey(event: KeyboardEvent): void {
    if (event.key !== '/') return;
    if (event.ctrlKey || event.metaKey || event.altKey) return;
    const tag = (event.target as HTMLElement).tagName;
    if (['INPUT', 'TEXTAREA', 'SELECT'].includes(tag)) return;
    if ((event.target as HTMLElement).isContentEditable) return;
    if (this.sidenavRail() && !this.isMobile()) return;
    event.preventDefault();
    this.activateNavSearch();
  }

  protected navigateBack(): void {
    window.history.back();
  }


  /**
   * Open the flyout on hover.
   * Immediately closes any other open flyout (no backdrop means we manage this ourselves).
   * Cancels any pending close timer so re-entering the button keeps the menu alive.
   */
  protected openHoverMenu(trigger: MatMenuTrigger): void {
    this.clearHoverTimer();
    // Close the previously open group flyout immediately
    if (this.activeMenuTrigger && this.activeMenuTrigger !== trigger) {
      this.activeMenuTrigger.closeMenu();
      this.activeMenuTrigger = null;
    }
    if (trigger.menuOpen) return;
    this.hoverMenuTimer = setTimeout(() => {
      this.ngZone.run(() => {
        trigger.openMenu();
        this.activeMenuTrigger = trigger;
      });
      this.hoverMenuTimer = null;
    }, 100);
  }

  /**
   * Schedule closing the menu after the cursor leaves the button or the panel.
   * Also cancels a pending open so quickly brushing over a button never shows the flyout.
   */
  protected scheduleCloseHoverMenu(trigger: MatMenuTrigger): void {
    this.clearHoverTimer();
    if (!trigger.menuOpen) return;
    this.hoverMenuTimer = setTimeout(() => {
      this.ngZone.run(() => {
        trigger.closeMenu();
        if (this.activeMenuTrigger === trigger) this.activeMenuTrigger = null;
      });
      this.hoverMenuTimer = null;
    }, 250);
  }

  /**
   * Called via (menuOpened). Attaches hover listeners directly to the CDK panel
   * element so moving from the button into the panel cancels the close timer.
   * We also check immediately whether the cursor is already inside the panel
   * (fast mouse movement can beat requestAnimationFrame).
   */
  protected attachHoverPanel(trigger: MatMenuTrigger): void {
    const attach = () => {
      const panels = document.querySelectorAll<HTMLElement>('.mat-mdc-menu-panel');
      const panel = panels[panels.length - 1];
      if (!panel) return;

      const onEnter = () => this.clearHoverTimer();
      const onLeave = () => this.scheduleCloseHoverMenu(trigger);

      panel.addEventListener('mouseenter', onEnter);
      panel.addEventListener('mouseleave', onLeave);

      // If the cursor is already inside the panel, cancel any pending close now
      if (panel.matches(':hover')) this.clearHoverTimer();

      const sub = trigger.menuClosed.subscribe(() => {
        panel.removeEventListener('mouseenter', onEnter);
        panel.removeEventListener('mouseleave', onLeave);
        if (this.activeMenuTrigger === trigger) this.activeMenuTrigger = null;
        sub.unsubscribe();
      });
    };

    // Try synchronously first; fall back to rAF if panel not yet in DOM
    const immediate = document.querySelectorAll<HTMLElement>('.mat-mdc-menu-panel');
    if (immediate.length) { attach(); } else { requestAnimationFrame(attach); }
  }

  private clearHoverTimer(): void {
    if (this.hoverMenuTimer) {
      clearTimeout(this.hoverMenuTimer);
      this.hoverMenuTimer = null;
    }
  }

  /**
   * Returns true when any item inside the group matches the current route.
   * Used to highlight the group icon in the collapsed icon rail.
   */
  protected isGroupActive(entry: NavGroup): boolean {
    const url = this.currentUrl();
    return entry.items.some(
      (item) => url === item.route || url.startsWith(item.route + '/'),
    );
  }

  protected async logout(): Promise<void> {
    await this.authService.logout();
  }
}

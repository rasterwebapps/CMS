import { Component, ElementRef, HostListener, ViewChild, inject, signal, computed, PLATFORM_ID, OnInit, AfterViewInit } from '@angular/core';
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
import { LayoutService } from './core/layout/layout.service';
import { ResponsiveService } from './core/layout/responsive.service';
import { KeyboardShortcutsService } from './core/shortcuts/keyboard-shortcuts.service';
import { GlobalSearchComponent } from './shared/global-search/global-search.component';
import { BreadcrumbBarComponent } from './shared/breadcrumb-bar/breadcrumb-bar.component';
import { ToastHostComponent } from './core/toast/toast-host.component';
import { TourService, ONBOARDING_TOUR_STEPS } from './core/tour';
import { ProfileService } from './features/profile/profile.service';
import { ThemeService } from './core/theme/theme.service';
import { NotificationService } from './core/notifications/notification.service';
import { NavItem, NavGroup, NavEntry, isNavGroup, NAV_ENTRIES } from './core/nav/nav-config';
import { environment } from '../environments';

/** Nav route matching must ignore query string/fragment — routes never carry one. */
function stripQueryAndFragment(url: string): string {
  return url.split(/[?#]/)[0];
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
  private readonly themeService = inject(ThemeService);
  protected readonly notificationService = inject(NotificationService);

  /** Tracks the current route URL (path only — see stripQueryAndFragment) so collapsed-group active state is reactive. */
  private readonly currentUrl = signal(stripQueryAndFragment(this.router.url));

  /** Timer for rail-hover peek open/close delay. */
  private railHoverTimer: ReturnType<typeof setTimeout> | null = null;
  /** True while the mouse is over the rail or the peek tray — drives the peek animation. */
  protected readonly railHovering = signal(false);

  /** The NavGroup currently selected on the icon rail — drives second-tray content. */
  protected readonly activeRailGroup = signal<NavGroup | null>(null);

  /** The NavGroup the cursor is currently hovering over on the rail (peek override). */
  protected readonly hoveredRailGroup = signal<NavGroup | null>(null);

  protected readonly navSearchActive = signal(false);
  @ViewChild('navSearchInput') private navSearchInputRef?: ElementRef<HTMLInputElement>;

  /** Persisted in localStorage; falls back to OS preference when never explicitly set. */
  protected readonly darkTheme = signal(this.loadDarkThemeState());
  /** `true` → sidenav collapsed to the 68px icon rail. Persisted in localStorage. */
  protected readonly sidenavCollapsed = signal(this.loadCollapsedState());
  /** Mobile drawer open state. Independent of pinned/collapsed. */
  protected readonly mobileDrawerOpen = signal(false);
  protected readonly menuSearch = signal('');
  protected readonly toolbarLogoError = signal(false);
  protected readonly notificationCount = computed(() => this.notificationService.feed().length);
  protected readonly navBadgeCounts = signal<Record<string, number>>({});
  protected readonly isNavGroup = isNavGroup;

  /** True when the sidenav is collapsed to the 68px icon rail on desktop. */
  protected readonly sidenavRail = computed(
    () => this.sidenavCollapsed() && !this.isMobile(),
  );

  protected readonly isMobile = this.responsiveService.isMobile;

  private static readonly EXPANDED_GROUPS_KEY = 'cms_nav_expanded_groups';
  private static readonly COLLAPSED_KEY = 'cms_sidenav_collapsed';
  private static readonly DARK_THEME_KEY = 'cms_dark_theme';

  protected readonly focusMode = this.layoutService.isFocusMode;
  protected readonly focusModeTitle = this.layoutService.focusModeTitle;

  /** 1–2 letter initials derived from the Keycloak username. */
  protected readonly userInitials = computed(() => {
    const name = this.authService.username() ?? '';
    const parts = name.split(/[\s_\-\.]+/).filter(Boolean);
    if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase();
    return name.slice(0, 2).toUpperCase();
  });

  private readonly navEntries: NavEntry[] = NAV_ENTRIES;

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

  /**
   * The permission-filtered NavGroup to display in the desktop second tray.
   * During a hover-peek, the hovered group overrides the selected group.
   * Falls back to the first available group when nothing is selected.
   */
  protected readonly activeGroupForTray = computed<NavGroup | null>(() => {
    const entries = this.filteredNavEntries();

    // Hover-peek: show whichever button the cursor is over
    const hovered = this.hoveredRailGroup();
    if (hovered && this.railHovering()) {
      const match = entries.find((e) => isNavGroup(e) && e.label === hovered.label);
      if (match) return match as NavGroup;
    }

    // Persistent selection (clicked or navigated)
    const active = this.activeRailGroup();
    if (active) {
      const match = entries.find((e) => isNavGroup(e) && e.label === active.label);
      if (match) return match as NavGroup;
    }

    return (entries.find(isNavGroup) as NavGroup) ?? null;
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
        this.activeRailGroup.set(entry);
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
    // Reflect the persisted/OS-derived dark mode choice onto <html> before first paint settles.
    // ThemeService.init() (app initializer) ran before this class existed, so its dark/light-dependent
    // tokens were computed from the OS media query alone — recompute now that the explicit class is set.
    if (isPlatformBrowser(this.platformId)) {
      this.applyThemeClass();
      this.themeService.refreshForColorScheme();
    }

    // Install global keyboard shortcuts (g-leader navigation + ? cheat-sheet).
    this.shortcutsService.install();

    // Load initial profile avatar for the toolbar.
    this.profileService.loadAvatar();

    // Load the in-app notification feed (BR-53) for the toolbar bell.
    if (isPlatformBrowser(this.platformId)) {
      this.notificationService.loadFeed().subscribe({ error: () => { /* silently ignore feed fetch errors */ } });
    }

    // Sync expanded group on initial load
    this.syncExpandedGroupToRoute(stripQueryAndFragment(this.router.url));

    // Keep currentUrl signal in sync, auto-expand the active group, auto-close mobile drawer.
    this.router.events.pipe(filter((e) => e instanceof NavigationEnd)).subscribe((e) => {
      // Nav active-state matching (isNavItemActive/isGroupActive/hasExactNavMatch below) compares
      // this against route paths, which never carry a query string — a mat-sort-header changing
      // ?sort=... (e.g. on Submit Documents) must not change which nav item reads as active.
      const url = stripQueryAndFragment((e as NavigationEnd).urlAfterRedirects);
      this.currentUrl.set(url);
      this.syncExpandedGroupToRoute(url);
      this.navSearchActive.set(false);
      this.menuSearch.set('');
      if (this.isMobile()) {
        this.mobileDrawerOpen.set(false);
      }
    });

    // Fetch admission funnel badge counts for users who can view enquiry workflows.
    if (isPlatformBrowser(this.platformId) && this.permissionService.has('ENQUIRY_VIEW')) {
      this.http
        .get<{ enquiryFunnel?: Record<string, number>; collectPaymentEligibleCount?: number; enquiryCollectPaymentEligibleCount?: number }>(`${environment.apiUrl}/dashboard/summary`)
        .subscribe({
          next: (data) => {
            const f = data.enquiryFunnel ?? {};
            const enquired      = (f['ENQUIRED']           ?? 0) + (f['INTERESTED']         ?? 0);
            const finalizeFee   = f['INTERESTED']          ?? 0;
            const collectPayment= data.enquiryCollectPaymentEligibleCount ?? 0;
            const docSubmit     = (f['FEES_PAID']          ?? 0) + (f['PARTIALLY_PAID']     ?? 0);
            const docVerify     = f['DOCUMENTS_SUBMITTED'] ?? 0;
            const admComplete   = f['DOCUMENTS_VERIFIED']  ?? 0;
            this.navBadgeCounts.set({
              '/enquiries':                       enquired,
              '/student-fees/finalize':           finalizeFee,
              '/fee-collection':                  collectPayment,
              '/enquiries/document-submission':   docSubmit,
              '/enquiries/document-verification': docVerify,
              '/enquiries/admission-completion':  admComplete,
            });
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

  protected dismissNotification(id: number, event: Event): void {
    event.stopPropagation();
    this.notificationService.dismiss(id).subscribe({ error: () => { /* silently ignore dismiss errors */ } });
  }

  protected toggleTheme(): void {
    this.darkTheme.update((v) => !v);
    if (isPlatformBrowser(this.platformId)) {
      this.applyThemeClass();
      try {
        localStorage.setItem(App.DARK_THEME_KEY, JSON.stringify(this.darkTheme()));
      } catch {
        // Ignore storage errors
      }
      this.themeService.refreshForColorScheme();
    }
  }

  /** Reflects the current `darkTheme` signal onto the `<html>` element's class list. */
  private applyThemeClass(): void {
    const html = document.documentElement;
    if (this.darkTheme()) {
      html.classList.add('dark-theme');
      html.classList.remove('light-theme');
    } else {
      html.classList.add('light-theme');
      html.classList.remove('dark-theme');
    }
  }

  /** Toggles the second tray visibility. First tray (rail) is always pinned. */
  protected toggleSidenav(): void {
    this.sidenavCollapsed.update((v) => !v);
    this.railHovering.set(false);
    if (isPlatformBrowser(this.platformId)) {
      try {
        localStorage.setItem(App.COLLAPSED_KEY, JSON.stringify(this.sidenavCollapsed()));
      } catch {
        // Ignore storage errors
      }
    }
  }

  /** Mouse enters the first-tray rail — start peek timer if second tray is hidden. */
  protected onRailEnter(): void {
    if (!this.sidenavCollapsed() || this.isMobile()) return;
    if (this.railHoverTimer) clearTimeout(this.railHoverTimer);
    this.railHoverTimer = setTimeout(() => this.railHovering.set(true), 120);
  }

  /** Mouse leaves the sidenav — schedule peek close and clear the hover group. */
  protected onRailLeave(): void {
    if (this.railHoverTimer) clearTimeout(this.railHoverTimer);
    this.railHoverTimer = setTimeout(() => {
      this.railHovering.set(false);
      this.hoveredRailGroup.set(null);
    }, 220);
  }

  /**
   * Mobile-only: opens/closes the navigation drawer. The drawer is in
   * `over` mode on mobile so toggling it doesn't push content.
   */
  protected toggleMobileDrawer(): void {
    this.mobileDrawerOpen.update((v) => !v);
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

  /** Restores the user's explicit dark/light choice; falls back to OS preference when never set. */
  private loadDarkThemeState(): boolean {
    if (!isPlatformBrowser(this.platformId)) {
      return false;
    }
    try {
      const stored = localStorage.getItem(App.DARK_THEME_KEY);
      if (stored) {
        return JSON.parse(stored) as boolean;
      }
    } catch {
      // Ignore parse errors
    }
    return window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false;
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
    if (this.sidenavCollapsed() && !this.railHovering() && !this.isMobile()) return;
    event.preventDefault();
    this.activateNavSearch();
  }

  protected navigateBack(): void {
    window.history.back();
  }

  /**
   * True when no nav item in any group has an exact route match for `url`.
   * Used to allow prefix matching only for routes that aren't explicit nav entries.
   */
  private hasExactNavMatch(url: string): boolean {
    return this.navEntries.some((e) => {
      if (!isNavGroup(e)) return e.route === url;
      return e.items.some((i) => i.route === url);
    });
  }

  /** Returns true when any item inside the group matches the current route. */
  protected isGroupActive(entry: NavGroup): boolean {
    const url = this.currentUrl();
    return entry.items.some((item) => {
      if (url === item.route) return true;
      // Prefix match only when the URL has no dedicated nav entry of its own.
      if (url.startsWith(item.route + '/')) return !this.hasExactNavMatch(url);
      return false;
    });
  }

  /** Returns true when the nav item matches the current route. */
  protected isNavItemActive(item: NavItem): boolean {
    const url = this.currentUrl();
    if (url === item.route) return true;
    // Prefix match only when the URL has no dedicated nav entry of its own.
    if (url.startsWith(item.route + '/')) return !this.hasExactNavMatch(url);
    return false;
  }

  /** Selects a group on the rail and ensures the second tray is open. */
  protected selectRailGroup(entry: NavGroup): void {
    this.activeRailGroup.set(entry);
    if (this.sidenavCollapsed() && !this.isMobile()) {
      this.sidenavCollapsed.set(false);
      this.railHovering.set(false);
    }
  }

  protected async logout(): Promise<void> {
    await this.authService.logout();
  }
}

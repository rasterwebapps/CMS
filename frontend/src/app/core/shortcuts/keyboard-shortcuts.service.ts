import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { LayoutService } from '../layout/layout.service';
import { TourService } from '../tour/tour.service';

export interface ShortcutDefinition {
  /** Display label for the shortcut, e.g. `g d`. */
  keys: string;
  /** Human-readable description of what the shortcut does. */
  description: string;
}

/** Key sequences that follow the `g` leader key, mapped to navigation routes. */
const GO_SHORTCUTS: Record<string, string> = {
  d: '/dashboard',
  e: '/enquiries',
  a: '/admissions',
  s: '/students',
  f: '/faculty',
  p: '/fee-payments',
};

const LEADER_TIMEOUT_MS = 1500;

/**
 * Global keyboard shortcuts:
 * - `g` leader key followed by `d`/`e`/`a`/`s`/`f`/`p` navigates to the
 *   matching top-level section.
 * - `?` opens the keyboard shortcuts cheat-sheet dialog.
 *
 * Shortcuts are disabled while focus is in an editable field and on
 * focus-mode routes. The existing `Cmd/Ctrl+K` handler in
 * `GlobalSearchComponent` is left untouched (we ignore any keypress with a
 * Ctrl/Meta/Alt modifier).
 */
@Injectable({ providedIn: 'root' })
export class KeyboardShortcutsService {
  private readonly router = inject(Router);
  private readonly layoutService = inject(LayoutService);
  private readonly dialog = inject(MatDialog);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly tourService = inject(TourService);

  private leaderActive = false;
  private leaderTimer: ReturnType<typeof setTimeout> | null = null;
  private installed = false;

  readonly shortcuts: readonly ShortcutDefinition[] = [
    { keys: 'g d', description: 'Go to Dashboard' },
    { keys: 'g e', description: 'Go to Enquiries' },
    { keys: 'g a', description: 'Go to Admissions' },
    { keys: 'g s', description: 'Go to Students' },
    { keys: 'g f', description: 'Go to Faculty' },
    { keys: 'g p', description: 'Go to Fee Payments' },
    // Listed for completeness in the cheat-sheet — actually handled by GlobalSearchComponent.
    { keys: 'Ctrl/⌘ K', description: 'Focus global search' },
    { keys: '?', description: 'Show this keyboard shortcuts dialog' },
    { keys: 'h', description: 'Start Help tour' },
  ];

  /** Installs the global `keydown` listener. Safe to call more than once. */
  install(): void {
    if (this.installed || !isPlatformBrowser(this.platformId)) return;
    this.installed = true;
    document.addEventListener('keydown', this.onKeyDown);
  }

  openCheatSheet(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    // Avoid stacking duplicate dialogs (matched by injected dialog data tag).
    if (this.dialog.openDialogs.some((d) => d.id === 'cms-keyboard-shortcuts')) {
      return;
    }
    // Lazy-load the dialog component so it stays out of the initial bundle.
    import('../../shared/keyboard-shortcuts-dialog/keyboard-shortcuts-dialog.component').then(
      ({ KeyboardShortcutsDialogComponent }) => {
        this.dialog.open(KeyboardShortcutsDialogComponent, {
          id: 'cms-keyboard-shortcuts',
          width: '480px',
          maxWidth: '90vw',
          autoFocus: false,
          data: { shortcuts: this.shortcuts },
        });
      },
    );
  }

  private readonly onKeyDown = (event: KeyboardEvent): void => {
    // Never interfere with browser/system shortcuts that pair with modifiers.
    if (event.ctrlKey || event.metaKey || event.altKey) {
      this.cancelLeader();
      return;
    }

    if (this.shouldIgnore(event)) {
      this.cancelLeader();
      return;
    }

    // ?  →  open cheat-sheet dialog (works even in focus mode).
    if (event.key === '?') {
      event.preventDefault();
      this.cancelLeader();
      this.openCheatSheet();
      return;
    }

    // h  →  start Help tour (works even in focus mode).
    if (event.key === 'h' || event.key === 'H') {
      event.preventDefault();
      this.cancelLeader();
      this.tourService.startTour('onboarding');
      return;
    }

    // Suppress nav shortcuts on focus-mode routes (forms / wizards).
    if (this.layoutService.isFocusMode()) {
      this.cancelLeader();
      return;
    }

    if (this.leaderActive) {
      const target = GO_SHORTCUTS[event.key.toLowerCase()];
      this.cancelLeader();
      if (target) {
        event.preventDefault();
        this.router.navigateByUrl(target);
      }
      return;
    }

    if (event.key === 'g') {
      this.leaderActive = true;
      this.leaderTimer = setTimeout(() => this.cancelLeader(), LEADER_TIMEOUT_MS);
    }
  };

  private cancelLeader(): void {
    this.leaderActive = false;
    if (this.leaderTimer !== null) {
      clearTimeout(this.leaderTimer);
      this.leaderTimer = null;
    }
  }

  private shouldIgnore(event: KeyboardEvent): boolean {
    const target = event.target as HTMLElement | null;
    if (!target) return false;
    if (target.isContentEditable) return true;
    const tag = target.tagName;
    return tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT';
  }
}

import { Injectable, inject, computed, DestroyRef } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { toSignal, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter, map } from 'rxjs';

// Routes that activate focus mode (collapse global toolbar to maximise vertical space)
// Focus mode is disabled — all screens use the regular layout.
export const FOCUS_MODE_PATTERNS: RegExp[] = [];

export const FOCUS_MODE_TITLES: { pattern: RegExp; title: string }[] = [];

@Injectable({ providedIn: 'root' })
export class LayoutService {
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  private readonly currentUrl = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map((e) => e.urlAfterRedirects),
      takeUntilDestroyed(this.destroyRef),
    ),
    { initialValue: this.router.url },
  );

  readonly isFocusMode = computed(() => {
    const url = this.currentUrl() ?? '';
    return FOCUS_MODE_PATTERNS.some((p) => p.test(url));
  });

  readonly focusModeTitle = computed(() => {
    const url = this.currentUrl() ?? '';
    const match = FOCUS_MODE_TITLES.find((t) => t.pattern.test(url));
    return match?.title ?? 'Form';
  });
}

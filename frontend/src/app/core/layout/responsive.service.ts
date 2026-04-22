import { Injectable, PLATFORM_ID, computed, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

export type Viewport = 'mobile' | 'tablet' | 'desktop';

/**
 * Tracks the current viewport bucket (`mobile` ≤ 767px, `tablet` 768–1023px,
 * `desktop` ≥ 1024px) using `window.matchMedia`. SSR-safe — defaults to
 * `desktop` when `window` is unavailable.
 */
@Injectable({ providedIn: 'root' })
export class ResponsiveService {
  private readonly platformId = inject(PLATFORM_ID);

  private readonly _viewport = signal<Viewport>(this.computeInitial());

  readonly viewport = this._viewport.asReadonly();
  readonly isMobile = computed(() => this._viewport() === 'mobile');
  readonly isTablet = computed(() => this._viewport() === 'tablet');
  readonly isDesktop = computed(() => this._viewport() === 'desktop');

  constructor() {
    if (!isPlatformBrowser(this.platformId)) return;

    const mobileMql = window.matchMedia('(max-width: 767px)');
    const tabletMql = window.matchMedia('(min-width: 768px) and (max-width: 1023px)');

    const update = (): void => {
      if (mobileMql.matches) this._viewport.set('mobile');
      else if (tabletMql.matches) this._viewport.set('tablet');
      else this._viewport.set('desktop');
    };

    // matchMedia in modern browsers (Angular 21 supports only evergreen).
    mobileMql.addEventListener('change', update);
    tabletMql.addEventListener('change', update);
  }

  private computeInitial(): Viewport {
    if (!isPlatformBrowser(this.platformId)) return 'desktop';
    const w = window.innerWidth;
    if (w <= 767) return 'mobile';
    if (w <= 1023) return 'tablet';
    return 'desktop';
  }
}

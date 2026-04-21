import { Injectable, inject, computed, DestroyRef } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { toSignal, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter, map } from 'rxjs';

// Routes that activate focus mode (collapse global toolbar to maximise vertical space)
export const FOCUS_MODE_PATTERNS: RegExp[] = [
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

export const FOCUS_MODE_TITLES: { pattern: RegExp; title: string }[] = [
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

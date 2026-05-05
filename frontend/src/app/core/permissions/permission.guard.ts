import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { PermissionService } from './permission.service';

/** Route guard: requires one or more permission codes. */
export function requiresPermission(...codes: string[]): CanActivateFn {
  return () => {
    const perm   = inject(PermissionService);
    const router = inject(Router);
    if (!perm.loaded()) {
      return router.createUrlTree(['/dashboard']);
    }
    if (perm.hasAny(...codes)) return true;
    return router.createUrlTree(['/dashboard']);
  };
}

/** Route guard: requires hierarchy level ≤ maxLevel. */
export function requiresLevel(maxLevel: number): CanActivateFn {
  return () => {
    const perm   = inject(PermissionService);
    const router = inject(Router);
    if (perm.level() <= maxLevel) return true;
    return router.createUrlTree(['/dashboard']);
  };
}

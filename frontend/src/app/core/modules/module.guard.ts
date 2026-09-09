import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { ModuleService } from './module.service';
import { resolveModulesForUrl } from './module-route-index';
import { PermissionService } from '../permissions/permission.service';

/**
 * Route guard: blocks navigation to a URL whose module(s) — resolved from nav-config.ts via
 * {@link resolveModulesForUrl} — are all disabled for this deployment. A URL with no module
 * mapping at all (core/unmapped) is always allowed. Applied once, inside the shared `withAuth`/
 * `withPermission` combinators in app.routes.ts, rather than on each of the ~250 individual
 * route entries — see the module-architecture decision log.
 *
 * <p>A platform system-role user (DEV_ADMIN, SUPPORT_ADMIN) bypasses module gating entirely,
 * mirroring the backend's `PermSecurityBean` exemption — see the module-architecture decision log.
 */
export function requiresEnabledModule(): CanActivateFn {
  return (_route, state) => {
    const moduleService = inject(ModuleService);
    const permissionService = inject(PermissionService);
    const router = inject(Router);

    if (permissionService.isDevAdmin() || permissionService.isSupportAdmin()) {
      return true;
    }

    const modules = resolveModulesForUrl(state.url);
    if (modules === null) {
      return true; // core/unmapped route — never module-gated
    }
    if (!moduleService.loaded()) {
      return router.createUrlTree(['/dashboard']);
    }
    if (moduleService.hasAny(...modules)) {
      return true;
    }
    return router.createUrlTree(['/dashboard']);
  };
}

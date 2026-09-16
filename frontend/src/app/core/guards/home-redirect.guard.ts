import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { PermissionService } from '../permissions/permission.service';

/** Sends the default/unmatched-route landing (`''`, `'**'`) to a role-appropriate home instead
 *  of always the generic admin-oriented `/dashboard`. Only changes *default landing* -- a
 *  student who explicitly navigates to `/dashboard` (e.g. to use DASHBOARD_CUSTOMIZE, which
 *  STUDENT already has) still reaches it normally; this guard only sits on the empty-path/
 *  catch-all routes, not on `/dashboard` itself.
 *
 *  Found via: STUDENT landing on the generic `/dashboard` by default triggers its universal
 *  (not role-filtered) DEFAULT_WIDGET_KEYS list, which includes widgets STUDENT has no
 *  permission for (equipment-status, fee-overview, pending-approvals, trend chart) -- each
 *  silently 403s in the background. STUDENT already has a dedicated, correctly-scoped
 *  destination (`/student/my-dashboard`, nav-labelled "My Dashboard") that this guard now
 *  actually routes default landings to. */
export const homeRedirectGuard: CanActivateFn = () => {
  const permissionService = inject(PermissionService);
  const router = inject(Router);

  if (permissionService.isRole('student')) {
    return router.parseUrl('/student/my-dashboard');
  }
  return router.parseUrl('/dashboard');
};

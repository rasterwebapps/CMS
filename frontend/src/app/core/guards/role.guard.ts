import { inject } from '@angular/core';
import { CanActivateFn, ActivatedRouteSnapshot, Router } from '@angular/router';
import { PermissionService } from '../permissions/permission.service';

export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const permissionService = inject(PermissionService);
  const router = inject(Router);

  const requiredRoles = route.data['roles'] as string[] | undefined;

  if (!requiredRoles || requiredRoles.length === 0) {
    return true;
  }

  const hasRole = permissionService.loaded() && permissionService.isRole(...requiredRoles);

  if (hasRole) {
    return true;
  }

  return router.createUrlTree(['/dashboard']);
};

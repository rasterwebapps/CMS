import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ToastService } from '../toast/toast.service';

/**
 * Surfaces the backend's distinct `MODULE_NOT_ENABLED` response (thrown by `PermSecurityBean`
 * when a permission's module is disabled for this deployment — see the module-architecture
 * decision log) as a clear toast, rather than letting it fall through as a generic error. In
 * normal use the frontend's own module-aware nav/route guards prevent a user from ever reaching
 * a disabled module's screen in the first place; this only fires for a direct API call that
 * slips past that (e.g. a module disabled mid-session, or a stale open tab).
 *
 * <p>Re-throws the error afterward so any component-level error handling still runs unchanged.
 */
export const moduleNotEnabledInterceptor: HttpInterceptorFn = (req, next) => {
  const toast = inject(ToastService);

  return next(req).pipe(
    catchError((err: unknown) => {
      if (err instanceof HttpErrorResponse && err.status === 403 && err.error?.code === 'MODULE_NOT_ENABLED') {
        toast.warning("This feature isn't part of your organization's enabled modules.");
      }
      return throwError(() => err);
    }),
  );
};

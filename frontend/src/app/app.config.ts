import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
  provideAppInitializer,
  inject,
  LOCALE_ID,
} from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeEnIn from '@angular/common/locales/en-IN';
import { provideRouter, withViewTransitions } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
// Note: provideClientHydration() intentionally omitted — this app is served from a
// plain Nginx SPA (no SSR Node.js server). Including it without SSR causes unnecessary
// hydration passes and can contribute to stale-state change-detection issues.
import { MAT_DIALOG_DEFAULT_OPTIONS } from '@angular/material/dialog';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { AuthService } from './core/auth/auth.service';
import { PermissionService } from './core/permissions/permission.service';
import { ThemeService } from './core/theme/theme.service';

// Register Indian locale so all Angular pipes (number, date, currency) use
// Indian number grouping (2-2-3 system: ₹12,34,567) by default.
registerLocaleData(localeEnIn);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withViewTransitions()),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideAnimationsAsync(),
    { provide: LOCALE_ID, useValue: 'en-IN' },
    {
      provide: MAT_DIALOG_DEFAULT_OPTIONS,
      useValue: { panelClass: 'cms-dialog-panel' },
    },
    provideAppInitializer(async () => {
      const authService = inject(AuthService);
      const permissionService = inject(PermissionService);

      const authenticated = await authService.init();

      if (!authenticated) {
        // Not logged in — redirect to Keycloak login page.
        // Using check-sso + explicit login() gives reliable PKCE-safe redirect.
        await authService.login();
        return; // browser will navigate away; stop initialisation here
      }

      await permissionService.load();

      // Note: if permissionService.loaded() is still false here, the backend returned
      // an error (unreachable, 401 issuer mismatch, user not in app_users). We do NOT
      // call login() in this path — doing so while a stale #code/state hash is in the
      // URL would bake those params into the redirect_uri and cause an infinite loop.
      // The user will land on the app with no permissions; role-guards will redirect to /dashboard.
    }),
    provideAppInitializer(() => {
      inject(ThemeService).init();
    }),
  ],
};

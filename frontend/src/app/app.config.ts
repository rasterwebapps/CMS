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
import { provideClientHydration } from '@angular/platform-browser';
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
    provideClientHydration(),
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

      if (!permissionService.loaded()) {
        // Token was accepted by Keycloak but rejected by the backend (wrong issuer
        // or user not in app_users). Force re-login to get a fresh token.
        await authService.login();
      }
    }),
    provideAppInitializer(() => {
      inject(ThemeService).init();
    }),
  ],
};

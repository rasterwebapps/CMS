import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
  provideAppInitializer,
  inject,
  LOCALE_ID,
} from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeEnIn from '@angular/common/locales/en-IN';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideClientHydration } from '@angular/platform-browser';
import { MAT_DIALOG_DEFAULT_OPTIONS } from '@angular/material/dialog';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { AuthService } from './core/auth/auth.service';
import { ThemeService } from './core/theme/theme.service';

// Register Indian locale so all Angular pipes (number, date, currency) use
// Indian number grouping (2-2-3 system: ₹12,34,567) by default.
registerLocaleData(localeEnIn);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideAnimationsAsync(),
    provideClientHydration(),
    { provide: LOCALE_ID, useValue: 'en-IN' },
    {
      provide: MAT_DIALOG_DEFAULT_OPTIONS,
      useValue: { panelClass: 'cms-dialog-panel' },
    },
    provideAppInitializer(() => {
      const authService = inject(AuthService);
      return authService.init();
    }),
    provideAppInitializer(() => {
      inject(ThemeService).init();
    }),
  ],
};

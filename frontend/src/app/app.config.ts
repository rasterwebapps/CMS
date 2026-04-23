import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
  provideAppInitializer,
  inject,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideClientHydration } from '@angular/platform-browser';
import { MAT_DIALOG_DEFAULT_OPTIONS } from '@angular/material/dialog';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { AuthService } from './core/auth/auth.service';
import { ThemeService } from './core/theme/theme.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideAnimationsAsync(),
    provideClientHydration(),
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

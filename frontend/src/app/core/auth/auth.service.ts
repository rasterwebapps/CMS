import { Injectable, signal, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import Keycloak from 'keycloak-js';
import { environment } from '../../../environments';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly platformId = inject(PLATFORM_ID);
  private keycloak: Keycloak | null = null;

  private readonly _authenticated = signal(false);
  private readonly _username = signal('');
  private readonly _roles = signal<string[]>([]);
  private readonly _token = signal('');

  readonly authenticated = this._authenticated.asReadonly();
  readonly username = this._username.asReadonly();
  readonly roles = this._roles.asReadonly();
  readonly token = this._token.asReadonly();

  async init(): Promise<boolean> {
    if (!isPlatformBrowser(this.platformId)) {
      return false;
    }

    this.keycloak = new Keycloak(environment.keycloak);

    try {
      const authenticated = await this.keycloak.init({
        onLoad: 'check-sso',
        checkLoginIframe: false,
        // The deployed app is served over HTTPS, so use the supported PKCE S256 method.
        pkceMethod: 'S256',
      });

      if (authenticated) {
        this.updateState();
      }

      return authenticated;
    } catch (err) {
      console.error('[CMS] Keycloak init error:', err);
      return false;
    }
  }

  async login(): Promise<void> {
    // Strip any stale OAuth callback params from the hash so keycloak-js builds
    // a clean redirect_uri. Without this, a failed check-sso leaves #code=...&state=...
    // in the URL and the next login call re-encodes them into the redirect_uri, causing
    // an infinite redirect loop.
    const redirectUri = window.location.href.split('#')[0];
    await this.keycloak?.login({ redirectUri });
  }

  async logout(): Promise<void> {
    // Revoke the access token server-side before ending the Keycloak session.
    // This ensures the token is immediately invalid even within its remaining lifetime.
    // Uses fetch() directly to avoid the circular DI path through authInterceptor.
    const token = this.keycloak?.token;
    if (token) {
      try {
        await fetch(`${environment.apiUrl}/auth/revoke`, {
          method: 'POST',
          headers: { Authorization: `Bearer ${token}` },
        });
      } catch {
        // Don't block logout if the revoke call fails
      }
    }
    await this.keycloak?.logout({ redirectUri: window.location.origin });
  }

  async refreshToken(): Promise<boolean> {
    if (!this.keycloak) {
      return false;
    }

    try {
      const refreshed = await this.keycloak.updateToken(30);
      if (refreshed) {
        this.updateState();
      }
      return true;
    } catch {
      return false;
    }
  }

  getToken(): string | undefined {
    return this.keycloak?.token;
  }

  /**
   * Returns a valid access token, refreshing it first if it expires within 30 seconds.
   *
   * If the refresh fails (e.g. the refresh token has also expired), this method returns
   * `undefined` and updates the `authenticated` signal to `false`.
   *
   * IMPORTANT: This method intentionally does NOT call `login()` on failure. Calling
   * `login()` here would trigger a Keycloak browser-redirect from inside the HTTP
   * interceptor while Angular may still be bootstrapping (`provideAppInitializer` is
   * awaiting `permissionService.load()`). That race condition leaves a half-written
   * PKCE `code_verifier` in `sessionStorage`, which corrupts the next page load and
   * causes the "page unresponsive / rolling indefinitely" symptom reported on second visit.
   *
   * Re-authentication is handled by `authGuard`, which calls `login()` the moment the
   * user navigates to any protected route after the session has expired.
   */
  async getValidToken(): Promise<string | undefined> {
    if (!this.keycloak) {
      return undefined;
    }
    try {
      await this.keycloak.updateToken(30);
      this.updateState();
      return this.keycloak.token;
    } catch (err) {
      console.warn('[CMS] Token refresh failed — session may have expired. Re-auth will happen on next navigation.', err);
      // Sync the signal so authGuard detects the expired session on the next navigation.
      this.updateState();
      return undefined;
    }
  }

  private updateState(): void {
    if (!this.keycloak) {
      return;
    }

    this._authenticated.set(this.keycloak.authenticated ?? false);
    this._token.set(this.keycloak.token ?? '');

    const tokenParsed = this.keycloak.tokenParsed;
    if (tokenParsed) {
      this._username.set(this.extractPreferredUsername(tokenParsed));
      const realmAccess = tokenParsed.realm_access;
      this._roles.set(realmAccess?.roles ?? []);
    }
  }

  private extractPreferredUsername(tokenParsed: Record<string, unknown>): string {
    const username = tokenParsed['preferred_username'];
    return typeof username === 'string' ? username : '';
  }
}

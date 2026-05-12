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
    await this.keycloak?.login();
  }

  async logout(): Promise<void> {
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
   * If the refresh fails (e.g. the refresh token has also expired), the user is
   * redirected to the Keycloak login page and `undefined` is returned.
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
      console.error('Failed to refresh Keycloak token — redirecting to login', err);
      await this.login();
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

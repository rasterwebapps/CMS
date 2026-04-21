import { Injectable, signal, computed, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import Keycloak from 'keycloak-js';

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

  readonly isAdmin = computed(() => this._roles().includes('ROLE_ADMIN'));
  readonly isFaculty = computed(() => this._roles().includes('ROLE_FACULTY'));
  readonly isStudent = computed(() => this._roles().includes('ROLE_STUDENT'));
  readonly isLabIncharge = computed(() => this._roles().includes('ROLE_LAB_INCHARGE'));
  readonly isTechnician = computed(() => this._roles().includes('ROLE_TECHNICIAN'));
  readonly isParent = computed(() => this._roles().includes('ROLE_PARENT'));
  readonly isFrontOffice = computed(() => this._roles().includes('ROLE_FRONT_OFFICE'));

  async init(): Promise<boolean> {
    if (!isPlatformBrowser(this.platformId)) {
      return false;
    }

    this.keycloak = new Keycloak({
      url: 'http://localhost:8280',
      realm: 'cms',
      clientId: 'cms-frontend',
    });

    try {
      const authenticated = await this.keycloak.init({
        onLoad: 'login-required',
        checkLoginIframe: false,
      });

      if (authenticated) {
        this.updateState();
      }

      return authenticated;
    } catch {
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

  async getValidToken(): Promise<string | undefined> {
    if (!this.keycloak) {
      return undefined;
    }
    try {
      await this.keycloak.updateToken(30);
      this.updateState();
      return this.keycloak.token;
    } catch {
      await this.login();
      return undefined;
    }
  }

  hasRole(role: string): boolean {
    return this._roles().includes(role);
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

import { Injectable, signal, computed } from '@angular/core';
import Keycloak from 'keycloak-js';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private keycloak: Keycloak;
  private readonly _authenticated = signal(false);
  private readonly _username = signal('');
  private readonly _roles = signal<string[]>([]);

  readonly authenticated = this._authenticated.asReadonly();
  readonly username = this._username.asReadonly();
  readonly roles = this._roles.asReadonly();

  readonly isAdmin = computed(() => this._roles().includes('ROLE_ADMIN'));
  readonly isFaculty = computed(() => this._roles().includes('ROLE_FACULTY'));
  readonly isStudent = computed(() => this._roles().includes('ROLE_STUDENT'));
  readonly isLabIncharge = computed(() => this._roles().includes('ROLE_LAB_INCHARGE'));
  readonly isTechnician = computed(() => this._roles().includes('ROLE_TECHNICIAN'));
  readonly isParent = computed(() => this._roles().includes('ROLE_PARENT'));

  constructor() {
    this.keycloak = new Keycloak({
      url: environment.keycloak.url,
      realm: environment.keycloak.realm,
      clientId: environment.keycloak.clientId,
    });
  }

  async init(): Promise<boolean> {
    const authenticated = await this.keycloak.init({
      onLoad: 'login-required',
      checkLoginIframe: false,
    });

    this._authenticated.set(authenticated);

    if (authenticated) {
      this._username.set(this.keycloak.tokenParsed?.['preferred_username'] ?? '');
      this._roles.set(this.keycloak.tokenParsed?.['realm_access']?.['roles'] ?? []);
    }

    return authenticated;
  }

  login(): Promise<void> {
    return this.keycloak.login();
  }

  logout(): Promise<void> {
    return this.keycloak.logout({ redirectUri: window.location.origin });
  }

  async getToken(): Promise<string> {
    try {
      await this.keycloak.updateToken(30);
    } catch {
      await this.login();
    }
    return this.keycloak.token ?? '';
  }

  hasRole(role: string): boolean {
    return this._roles().includes(role);
  }

  hasAnyRole(...roles: string[]): boolean {
    return roles.some((role) => this._roles().includes(role));
  }
}

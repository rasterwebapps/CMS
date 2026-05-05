import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments';

export interface MyPermissionsResponse {
  username: string;
  roleName: string;
  roleDisplayName: string;
  hierarchyLevel: number;
  permissions: string[];
}

@Injectable({ providedIn: 'root' })
export class PermissionService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/permissions`;

  private readonly _response = signal<MyPermissionsResponse | null>(null);

  readonly loaded    = computed(() => this._response() !== null);
  readonly roleName  = computed(() => this._response()?.roleName ?? '');
  readonly roleLabel = computed(() => this._response()?.roleDisplayName ?? '');
  readonly level     = computed(() => this._response()?.hierarchyLevel ?? 99);
  readonly isDevAdmin     = computed(() => this._response()?.roleName === 'DEV_ADMIN');
  readonly isSupportAdmin = computed(() => this.level() <= 2);
  readonly isAdminOrAbove = computed(() => this.level() <= 3);
  readonly canManageUsers = computed(() => this.has('USER_VIEW'));
  readonly canManageRoles = computed(() => this.has('ROLE_VIEW'));

  /** Load permissions from server — call once after login. */
  load(): Promise<void> {
    return new Promise((resolve) => {
      this.http.get<MyPermissionsResponse>(`${this.base}/my`).subscribe({
        next: (r) => { this._response.set(r); resolve(); },
        error: () => resolve(),
      });
    });
  }

  /** Returns true if the user holds the given permission code. */
  has(code: string): boolean {
    return this._response()?.permissions.includes(code) ?? false;
  }

  /** Returns true if the user holds ANY of the given permission codes. */
  hasAny(...codes: string[]): boolean {
    const perms = this._response()?.permissions ?? [];
    return codes.some(c => perms.includes(c));
  }

  /** Returns true if the user holds ALL of the given permission codes. */
  hasAll(...codes: string[]): boolean {
    const perms = this._response()?.permissions ?? [];
    return codes.every(c => perms.includes(c));
  }

  clear(): void {
    this._response.set(null);
  }
}

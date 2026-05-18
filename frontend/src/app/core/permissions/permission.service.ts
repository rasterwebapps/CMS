import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments';
import { WidgetConfigDto } from './permission.model';

export type { WidgetConfigDto };

export interface MyPermissionsResponse {
  username: string;
  roleName: string;
  roleDisplayName: string;
  hierarchyLevel: number;
  permissions: string[];
  dashboardWidgets: WidgetConfigDto[];
}

@Injectable({ providedIn: 'root' })
export class PermissionService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/permissions`;

  private readonly _response = signal<MyPermissionsResponse | null>(null);

  readonly loaded           = computed(() => this._response() !== null);
  readonly roleName         = computed(() => this._response()?.roleName ?? '');
  readonly roleLabel        = computed(() => this._response()?.roleDisplayName ?? '');
  readonly level            = computed(() => this._response()?.hierarchyLevel ?? 99);
  readonly permissions      = computed(() => this._response()?.permissions ?? []);
  /** Ordered widget configs (key + span metadata) for this user's resolved dashboard. */
  readonly dashboardWidgets = computed(() => this._response()?.dashboardWidgets ?? []);
  readonly normalizedRoleName = computed(() =>
    this.roleName()
      .replace(/_/g, '')
      .trim()
      .toLowerCase(),
  );
  readonly isDevAdmin     = computed(() => this.isRole('devadmin'));
  readonly isSupportAdmin = computed(() => this.isRole('supportadmin'));
  readonly isAdminOrAbove = computed(() => this.level() <= 3);
  readonly canManageUsers = computed(() => this.has('USER_VIEW'));
  readonly canManageRoles = computed(() => this.has('ROLE_VIEW'));

  /** Load permissions from server — call once after login. */
  load(): Promise<void> {
    return new Promise((resolve) => {
      this.http.get<MyPermissionsResponse>(`${this.base}/my`).subscribe({
        next: (response) => {
          this._response.set(response);
          resolve();
        },
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
    return codes.some((code) => perms.includes(code));
  }

  /** Returns true if the user holds ALL of the given permission codes. */
  hasAll(...codes: string[]): boolean {
    const perms = this._response()?.permissions ?? [];
    return codes.every((code) => perms.includes(code));
  }

  /** Returns true if the user has any of the normalized roles. */
  isRole(...roleNames: string[]): boolean {
    const normalizedCurrentRole = this.normalizedRoleName();
    return roleNames.some(
      (roleName) => roleName.replace(/_/g, '').trim().toLowerCase() === normalizedCurrentRole,
    );
  }

  clear(): void {
    this._response.set(null);
  }
}

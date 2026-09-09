import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments';

export interface ModuleDto {
  code: string;
  displayName: string;
}

export interface EnabledModulesResponse {
  enabledModules: ModuleDto[];
}

/**
 * This deployment's enabled feature modules — fetched once at app init (alongside
 * {@link import('../permissions/permission.service').PermissionService}) from the backend's
 * module registry (see {@code GET /modules/enabled}, backend-owned per the module-architecture
 * decision log). Nav and routing both AND this against the existing permission check: an entry
 * tagged with a module the deployment hasn't enabled is hidden/blocked even if the user's role
 * happens to carry the underlying permission — module gate always wins.
 *
 * <p>An entry with no `modules` tag at all (e.g. Overview, User Management) is "core" and is
 * never gated here, matching the backend's `PermSecurityBean` treatment of permission codes with
 * no module mapping.
 *
 * <p>Mirrors {@link import('../permissions/permission.service').PermissionService}'s load/error
 * shape: on a failed fetch, {@link loaded} stays `false` and callers (route guards, nav
 * filtering) treat that the same way a failed permission load is treated — deny rather than
 * silently show everything.
 */
@Injectable({ providedIn: 'root' })
export class ModuleService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/modules`;

  private readonly _enabledCodes = signal<Set<string> | null>(null);

  readonly loaded = computed(() => this._enabledCodes() !== null);

  /** Load this deployment's enabled modules from the server — call once after login. */
  load(): Promise<void> {
    return new Promise((resolve) => {
      this.http.get<EnabledModulesResponse>(`${this.base}/enabled`).subscribe({
        next: (response) => {
          this._enabledCodes.set(new Set(response.enabledModules.map((m) => m.code)));
          resolve();
        },
        error: () => resolve(),
      });
    });
  }

  /** Returns true if this deployment has the given module enabled. False if not yet loaded. */
  isEnabled(moduleCode: string): boolean {
    return this._enabledCodes()?.has(moduleCode) ?? false;
  }

  /** Returns true if ANY of the given module codes is enabled for this deployment. */
  hasAny(...moduleCodes: string[]): boolean {
    return moduleCodes.some((code) => this.isEnabled(code));
  }

  clear(): void {
    this._enabledCodes.set(null);
  }
}

import { Injectable, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../../../environments';
import { PermissionService, WidgetConfigDto } from '../../../core/permissions/permission.service';

/**
 * Manages the user's personal dashboard widget layout.
 *
 * Wraps GET/PUT/DELETE /dashboard/config.
 * After a successful save or reset, the permissions response is reloaded so that
 * permissionService.dashboardWidgets() reflects the new config immediately —
 * this causes DashboardComponent.widgetItems() to recompute and the grid to update.
 */
@Injectable({ providedIn: 'root' })
export class DashboardConfigService {

  private readonly http              = inject(HttpClient);
  private readonly permissionService = inject(PermissionService);
  private readonly base              = `${environment.apiUrl}/dashboard/config`;

  // ── Permission guard ────────────────────────────────────────────────────

  /** Whether the current user is allowed to save a personal layout. */
  readonly canCustomize = computed(() =>
    this.permissionService.has('DASHBOARD_CUSTOMIZE')
  );

  // ── API methods ─────────────────────────────────────────────────────────

  /**
   * Fetches the caller's current resolved widget config fresh from the server.
   * Returns the personal override if one exists, otherwise the role default.
   * Not cached — always goes to the network (used when opening the picker).
   */
  getMyConfig(): Observable<WidgetConfigDto[]> {
    return this.http.get<WidgetConfigDto[]>(this.base);
  }

  /**
   * Saves a personal layout and reloads permissions so the dashboard
   * immediately reflects the new config.
   */
  saveConfig(configs: WidgetConfigDto[]): Observable<WidgetConfigDto[]> {
    return this.http.put<WidgetConfigDto[]>(this.base, configs).pipe(
      tap(() => this.permissionService.load()),
    );
  }

  /**
   * Resets to the role default by deleting personal overrides,
   * then reloads permissions.
   */
  resetConfig(): Observable<void> {
    return this.http.delete<void>(this.base).pipe(
      tap(() => this.permissionService.load()),
    );
  }
}

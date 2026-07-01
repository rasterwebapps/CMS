import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  AppRoleResponse, AppUserResponse,
  CreateUserRequest, UpdateUserRequest,
  AllPermissionsResponse, WidgetConfigDto,
} from './permission.model';

@Injectable({ providedIn: 'root' })
export class UserRoleService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiUrl;

  // ── Users ────────────────────────────────────────────────────
  getUsers(): Observable<AppUserResponse[]> {
    return this.http.get<AppUserResponse[]>(`${this.base}/user-management`);
  }

  createUser(req: CreateUserRequest): Observable<AppUserResponse> {
    return this.http.post<AppUserResponse>(`${this.base}/user-management`, req);
  }

  updateUser(id: number, req: UpdateUserRequest): Observable<AppUserResponse> {
    return this.http.put<AppUserResponse>(`${this.base}/user-management/${id}`, req);
  }

  deactivateUser(id: number): Observable<void> {
    return this.http.put<void>(`${this.base}/user-management/${id}/deactivate`, {});
  }

  reactivateUser(id: number): Observable<void> {
    return this.http.put<void>(`${this.base}/user-management/${id}/reactivate`, {});
  }

  // ── Roles ────────────────────────────────────────────────────
  getRoles(): Observable<AppRoleResponse[]> {
    return this.http.get<AppRoleResponse[]>(`${this.base}/role-management`);
  }

  getRole(id: number): Observable<AppRoleResponse> {
    return this.http.get<AppRoleResponse>(`${this.base}/role-management/${id}`);
  }

  createRole(req: { name: string; displayName: string; description: string; permissionCodes: string[] }): Observable<AppRoleResponse> {
    return this.http.post<AppRoleResponse>(`${this.base}/role-management`, req);
  }

  updateRolePermissions(id: number, permissionCodes: string[]): Observable<AppRoleResponse> {
    return this.http.put<AppRoleResponse>(`${this.base}/role-management/${id}/permissions`, permissionCodes);
  }

  updateRoleDashboardWidgets(id: number, configs: WidgetConfigDto[]): Observable<AppRoleResponse> {
    return this.http.put<AppRoleResponse>(`${this.base}/role-management/${id}/dashboard-widgets`, configs);
  }

  // ── Permissions ───────────────────────────────────────────────
  /** All permissions with tier info — for the Permission Tier Management screen (DEV_ADMIN only). */
  getAllPermissions(): Observable<AllPermissionsResponse[]> {
    return this.http.get<AllPermissionsResponse[]>(`${this.base}/permissions/all`);
  }

  /** Permissions the current user is allowed to delegate to sub-roles — for the Role editor picker. */
  getDelegatablePermissions(): Observable<AllPermissionsResponse[]> {
    return this.http.get<AllPermissionsResponse[]>(`${this.base}/permissions/delegatable`);
  }

  updatePermissionTier(id: number, tier: number): Observable<AllPermissionsResponse> {
    return this.http.put<AllPermissionsResponse>(`${this.base}/permissions/${id}/tier`, { tier });
  }

  // ── Dashboard ─────────────────────────────────────────────────
  getUserDashboardWidgets(): Observable<string[]> {
    return this.http.get<string[]>(`${this.base}/dashboard/widgets`);
  }
}

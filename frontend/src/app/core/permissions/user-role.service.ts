import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  AppRoleResponse, AppUserResponse,
  CreateUserRequest, UpdateUserRequest,
  AllPermissionsResponse,
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

  // ── Permissions ───────────────────────────────────────────────
  getAllPermissions(): Observable<AllPermissionsResponse[]> {
    return this.http.get<AllPermissionsResponse[]>(`${this.base}/permissions/all`);
  }
}

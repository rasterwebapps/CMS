import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';

export interface ProfileIdentity {
  entityType: 'FACULTY' | 'STUDENT' | 'ADMIN';
  entityId: number | null;
  admissionId: number | null;
  programId: number | null;
  displayName: string;
  email: string | null;
}

export interface SelfUpdateRequest {
  phone?: string | null;
  bloodGroup?: string | null;
  postalAddress?: string | null;
  street?: string | null;
  city?: string | null;
  district?: string | null;
  state?: string | null;
  pincode?: string | null;
}

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/profile`;

  // ── Shared avatar signal (toolbar + profile page stay in sync) ──────────────
  private readonly _avatarDataUrl = signal<string | null>(null);
  /** Reactive data-URL for the current user's profile photo. Null when not set. */
  readonly avatarDataUrl = this._avatarDataUrl.asReadonly();

  /** Set the shared avatar to an existing data URL (or null to clear). */
  setAvatarDataUrl(url: string | null): void {
    this._avatarDataUrl.set(url);
  }

  /**
   * Load the current user's photo from the API and store it as a data URL
   * in {@link avatarDataUrl}. Called on app startup and after photo changes.
   */
  loadAvatar(): void {
    this.getMyPhoto().subscribe({
      next: (blob) => {
        if (!blob || blob.size === 0) {
          this._avatarDataUrl.set(null);
          return;
        }
        const reader = new FileReader();
        reader.onload = (e) => this._avatarDataUrl.set((e.target?.result as string) ?? null);
        reader.readAsDataURL(blob);
      },
      error: () => this._avatarDataUrl.set(null),
    });
  }

  getMyProfile(): Observable<ProfileIdentity> {
    return this.http.get<ProfileIdentity>(`${this.baseUrl}/me`);
  }

  getMyPhoto(): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/me/photo`, { responseType: 'blob' });
  }

  uploadPhoto(file: File): Observable<void> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<void>(`${this.baseUrl}/me/photo`, formData);
  }

  deletePhoto(): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/me/photo`);
  }

  updateSelfInfo(request: SelfUpdateRequest): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/me/self-info`, request);
  }
}

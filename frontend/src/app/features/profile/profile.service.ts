import { Injectable, inject } from '@angular/core';
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

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/profile`;

  getMyProfile(): Observable<ProfileIdentity> {
    return this.http.get<ProfileIdentity>(`${this.baseUrl}/me`);
  }
}

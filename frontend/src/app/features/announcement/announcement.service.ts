import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import { Announcement } from './announcement.model';

@Injectable({
  providedIn: 'root',
})
export class AnnouncementService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/announcements`;

  /** Current authenticated user's own announcement feed, resolved server-side by role/cohort/
   *  section/ward membership -- never a client-supplied audience filter. */
  getMyFeed(): Observable<Announcement[]> {
    return this.http.get<Announcement[]>(`${this.baseUrl}/my`);
  }

  getMyUnreadCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/my/unread-count`);
  }

  markRead(id: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${id}/read`, {});
  }
}

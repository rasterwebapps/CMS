import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs/operators';
import { environment } from '../../../environments';

export interface AppNotification {
  id: number;
  categoryKey: string;
  title: string;
  message: string;
  link: string | null;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/notifications`;

  readonly feed = signal<AppNotification[]>([]);

  loadFeed() {
    return this.http.get<AppNotification[]>(`${this.baseUrl}/feed`).pipe(
      tap((feed) => this.feed.set(feed))
    );
  }

  dismiss(id: number) {
    this.feed.update((items) => items.filter((n) => n.id !== id));
    return this.http.post<void>(`${this.baseUrl}/${id}/dismiss`, {});
  }
}

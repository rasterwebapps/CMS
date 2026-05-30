import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs/operators';
import { environment } from '../../../environments';

export interface NotificationPreference {
  categoryKey: string;
  enabled: boolean;
  channel: string; // 'IN_APP' | 'EMAIL' | 'BOTH'
}

@Injectable({ providedIn: 'root' })
export class NotificationPreferenceService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/notifications/preferences`;

  readonly preferences = signal<NotificationPreference[]>([]);
  readonly loading = signal(false);

  load() {
    this.loading.set(true);
    return this.http.get<NotificationPreference[]>(this.baseUrl).pipe(
      tap({
        next: (prefs) => { this.preferences.set(prefs); this.loading.set(false); },
        error: () => { this.loading.set(false); },
      })
    );
  }

  updateAll(prefs: NotificationPreference[]) {
    return this.http.put<NotificationPreference[]>(this.baseUrl, { preferences: prefs }).pipe(
      tap({ next: (updated) => this.preferences.set(updated) })
    );
  }

  updateOne(categoryKey: string, enabled: boolean, channel: string) {
    const current = this.preferences();
    const updated = current.map(p =>
      p.categoryKey === categoryKey ? { ...p, enabled, channel } : p
    );
    const payload = [{ categoryKey, enabled, channel }];
    this.preferences.set(updated);
    return this.http.put<NotificationPreference[]>(this.baseUrl, { preferences: payload }).pipe(
      tap({ next: (res) => this.preferences.set(res) })
    );
  }

  /** Map backend channel (IN_APP) to UI channel (in-app) */
  static toUiChannel(ch: string): 'in-app' | 'email' | 'both' {
    if (ch === 'EMAIL') return 'email';
    if (ch === 'BOTH') return 'both';
    return 'in-app';
  }

  /** Map UI channel (in-app) to backend channel (IN_APP) */
  static toApiChannel(ch: string): string {
    if (ch === 'email') return 'EMAIL';
    if (ch === 'both') return 'BOTH';
    return 'IN_APP';
  }
}

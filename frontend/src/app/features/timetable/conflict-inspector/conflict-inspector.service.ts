import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments';
import { ConflictScanResponse } from './conflict-inspector.model';

@Injectable({ providedIn: 'root' })
export class ConflictInspectorService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/timetables/conflict-inspector`;

  scan(termInstanceId: number): Observable<ConflictScanResponse> {
    return this.http.get<ConflictScanResponse>(this.baseUrl, {
      params: { termInstanceId: termInstanceId.toString() },
    });
  }
}

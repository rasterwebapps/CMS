import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments';
import {
  SkeletonBuilderResponse,
  SkeletonCell,
  SkeletonCellPlacementRequest,
  SkeletonPlacementCandidate,
  SkeletonSessionType,
} from './skeleton-builder.model';

@Injectable({ providedIn: 'root' })
export class SkeletonBuilderService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/timetables/skeleton`;

  getCohortSkeleton(termInstanceId: number, cohortId: number): Observable<SkeletonBuilderResponse> {
    return this.http.get<SkeletonBuilderResponse>(this.baseUrl, {
      params: { termInstanceId: termInstanceId.toString(), cohortId: cohortId.toString() },
    });
  }

  suggestCandidates(courseOfferingId: number, sessionType: SkeletonSessionType, batchId: number | null): Observable<SkeletonPlacementCandidate[]> {
    const params: Record<string, string> = {
      courseOfferingId: courseOfferingId.toString(),
      sessionType,
    };
    if (batchId != null) params['batchId'] = batchId.toString();
    return this.http.get<SkeletonPlacementCandidate[]>(`${this.baseUrl}/suggest`, { params });
  }

  placeCell(request: SkeletonCellPlacementRequest): Observable<SkeletonCell> {
    return this.http.post<SkeletonCell>(`${this.baseUrl}/cells`, request);
  }

  removeCell(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/cells/${id}`);
  }
}

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments';
import {
  AutoPlaceResult,
  SkeletonBuilderResponse,
  SkeletonCell,
  SkeletonCellMoveRequest,
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

  suggestCandidates(courseOfferingId: number, sessionType: SkeletonSessionType, batchId: number | null, cohortSectionId: number | null): Observable<SkeletonPlacementCandidate[]> {
    const params: Record<string, string> = {
      courseOfferingId: courseOfferingId.toString(),
      sessionType,
    };
    if (batchId != null) params['batchId'] = batchId.toString();
    if (cohortSectionId != null) params['cohortSectionId'] = cohortSectionId.toString();
    return this.http.get<SkeletonPlacementCandidate[]>(`${this.baseUrl}/suggest`, { params });
  }

  placeCell(request: SkeletonCellPlacementRequest): Observable<SkeletonCell> {
    return this.http.post<SkeletonCell>(`${this.baseUrl}/cells`, request);
  }

  removeCell(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/cells/${id}`);
  }

  moveCell(id: number, request: SkeletonCellMoveRequest): Observable<SkeletonCell> {
    return this.http.put<SkeletonCell>(`${this.baseUrl}/cells/${id}/move`, request);
  }

  autoPlace(termInstanceId: number, cohortId: number): Observable<AutoPlaceResult> {
    return this.http.post<AutoPlaceResult>(`${this.baseUrl}/auto-place`, null, {
      params: { termInstanceId: termInstanceId.toString(), cohortId: cohortId.toString() },
    });
  }
}

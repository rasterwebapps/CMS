import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments';
import { SkeletonBuilderResponse, SkeletonCell, SkeletonCellPlacementRequest } from './skeleton-builder.model';

@Injectable({ providedIn: 'root' })
export class SkeletonBuilderService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/timetables/skeleton`;

  getSkeleton(courseOfferingId: number): Observable<SkeletonBuilderResponse> {
    return this.http.get<SkeletonBuilderResponse>(this.baseUrl, {
      params: { courseOfferingId: courseOfferingId.toString() },
    });
  }

  placeCell(request: SkeletonCellPlacementRequest): Observable<SkeletonCell> {
    return this.http.post<SkeletonCell>(`${this.baseUrl}/cells`, request);
  }

  removeCell(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/cells/${id}`);
  }
}

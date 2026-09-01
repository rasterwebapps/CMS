import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  ClinicalShiftGroup,
  ClinicalShiftGroupRequest,
  ClinicalShiftTheoryBlock,
  ClinicalShiftTheoryBlockRequest,
} from './clinical-shift-group.model';

@Injectable({ providedIn: 'root' })
export class ClinicalShiftGroupService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/clinical-shift-groups`;

  getForOffering(courseOfferingId: number): Observable<ClinicalShiftGroup[]> {
    return this.http.get<ClinicalShiftGroup[]>(this.baseUrl, {
      params: { courseOfferingId: courseOfferingId.toString() },
    });
  }

  create(request: ClinicalShiftGroupRequest): Observable<ClinicalShiftGroup> {
    return this.http.post<ClinicalShiftGroup>(this.baseUrl, request);
  }

  update(id: number, request: ClinicalShiftGroupRequest): Observable<ClinicalShiftGroup> {
    return this.http.put<ClinicalShiftGroup>(`${this.baseUrl}/${id}`, request);
  }

  deactivate(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  linkBatch(id: number, batchId: number): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/${id}/batches/${batchId}`, null);
  }

  unlinkBatch(id: number, batchId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}/batches/${batchId}`);
  }

  replaceTheoryBlocks(id: number, blocks: ClinicalShiftTheoryBlockRequest[]): Observable<ClinicalShiftTheoryBlock[]> {
    return this.http.put<ClinicalShiftTheoryBlock[]>(`${this.baseUrl}/${id}/theory-blocks`, blocks);
  }

  generateForDate(id: number, date: string): Observable<number> {
    return this.http.post<number>(`${this.baseUrl}/${id}/generate`, null, { params: { date } });
  }
}

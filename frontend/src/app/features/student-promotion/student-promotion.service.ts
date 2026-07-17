import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  CohortTermOption,
  PromotionExecuteRequest,
  PromotionExecuteResponse,
  PromotionPreviewRequest,
  PromotionPreviewResponse,
  StudentPromotionDecisionDto,
} from './student-promotion.model';

@Injectable({
  providedIn: 'root',
})
export class StudentPromotionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/student-promotions`;

  /** Term instances the cohort currently has ENROLLED students in — usually exactly one. */
  getActiveTerms(cohortId: number): Observable<CohortTermOption[]> {
    return this.http.get<CohortTermOption[]>(`${this.baseUrl}/active-terms`, {
      params: { cohortId: cohortId.toString() },
    });
  }

  /** The chronologically next term after fromTermInstanceId, or null if it doesn't exist yet
   *  (e.g. the next academic year hasn't been created) — backend returns 204 in that case. */
  getSuggestedNextTerm(fromTermInstanceId: number): Observable<CohortTermOption | null> {
    return this.http.get<CohortTermOption | null>(`${this.baseUrl}/suggested-next-term`, {
      params: { fromTermInstanceId: fromTermInstanceId.toString() },
    });
  }

  preview(request: PromotionPreviewRequest): Observable<PromotionPreviewResponse> {
    return this.http.post<PromotionPreviewResponse>(`${this.baseUrl}/preview`, request);
  }

  execute(request: PromotionExecuteRequest): Observable<PromotionExecuteResponse> {
    return this.http.post<PromotionExecuteResponse>(`${this.baseUrl}/execute`, request);
  }

  getHistoryByCohort(cohortId: number): Observable<StudentPromotionDecisionDto[]> {
    return this.http.get<StudentPromotionDecisionDto[]>(`${this.baseUrl}/history`, {
      params: { cohortId: cohortId.toString() },
    });
  }

  getHistoryByStudent(studentId: number): Observable<StudentPromotionDecisionDto[]> {
    return this.http.get<StudentPromotionDecisionDto[]>(`${this.baseUrl}/history`, {
      params: { studentId: studentId.toString() },
    });
  }
}

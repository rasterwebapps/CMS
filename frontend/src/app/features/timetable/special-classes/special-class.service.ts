import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments';
import {
  DayRepeatRequestPayload,
  DayRepeatResult,
  RecurringSpecialClassRequestPayload,
  RecurringSpecialClassResult,
  SpecialClassApprovalStatus,
  SpecialClassOccurrence,
  SpecialClassRequestPayload,
} from './special-class.model';

/** All optional -- an omitted filter is not sent as a query param at all. */
export interface SpecialClassSearchFilter {
  status?: SpecialClassApprovalStatus | null;
  facultyId?: number | null;
  dateFrom?: string | null;
  dateTo?: string | null;
  subjectId?: number | null;
  /** A Cohort id (see AcademicYearService.getAllCohorts) -- not a CohortSection id. */
  cohortId?: number | null;
  search?: string | null;
}

@Injectable({ providedIn: 'root' })
export class SpecialClassService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/timetables/special-classes`;

  /** Always an array, even for a single-period request -- one row per requested period, all
   *  sharing one `requestBatchId` once there's more than one (see `approveBatch`/`rejectBatch`). */
  requestSingleSubject(request: SpecialClassRequestPayload): Observable<SpecialClassOccurrence[]> {
    return this.http.post<SpecialClassOccurrence[]>(`${this.baseUrl}/single-subject`, request);
  }

  requestDayRepeat(request: DayRepeatRequestPayload): Observable<DayRepeatResult> {
    return this.http.post<DayRepeatResult>(`${this.baseUrl}/day-repeat`, request);
  }

  requestRecurring(request: RecurringSpecialClassRequestPayload): Observable<RecurringSpecialClassResult> {
    return this.http.post<RecurringSpecialClassResult>(`${this.baseUrl}/recurring`, request);
  }

  myRequests(): Observable<SpecialClassOccurrence[]> {
    return this.http.get<SpecialClassOccurrence[]>(`${this.baseUrl}/my-requests`);
  }

  approvalQueue(filter: SpecialClassSearchFilter = {}): Observable<SpecialClassOccurrence[]> {
    let params = new HttpParams();
    if (filter.status) params = params.set('status', filter.status);
    if (filter.facultyId != null) params = params.set('facultyId', filter.facultyId);
    if (filter.dateFrom) params = params.set('dateFrom', filter.dateFrom);
    if (filter.dateTo) params = params.set('dateTo', filter.dateTo);
    if (filter.subjectId != null) params = params.set('subjectId', filter.subjectId);
    if (filter.cohortId != null) params = params.set('cohortId', filter.cohortId);
    if (filter.search) params = params.set('search', filter.search);
    return this.http.get<SpecialClassOccurrence[]>(`${this.baseUrl}/approval-queue`, { params });
  }

  approve(id: number): Observable<SpecialClassOccurrence> {
    return this.http.put<SpecialClassOccurrence>(`${this.baseUrl}/${id}/approve`, {});
  }

  approveBatch(requestBatchId: string): Observable<SpecialClassOccurrence[]> {
    return this.http.put<SpecialClassOccurrence[]>(`${this.baseUrl}/batches/${requestBatchId}/approve`, {});
  }

  reject(id: number, rejectionReason: string): Observable<SpecialClassOccurrence> {
    return this.http.put<SpecialClassOccurrence>(`${this.baseUrl}/${id}/reject`, { rejectionReason });
  }

  rejectBatch(requestBatchId: string, rejectionReason: string): Observable<SpecialClassOccurrence[]> {
    return this.http.put<SpecialClassOccurrence[]>(`${this.baseUrl}/batches/${requestBatchId}/reject`, { rejectionReason });
  }

  cancel(id: number): Observable<SpecialClassOccurrence> {
    return this.http.put<SpecialClassOccurrence>(`${this.baseUrl}/${id}/cancel`, {});
  }
}

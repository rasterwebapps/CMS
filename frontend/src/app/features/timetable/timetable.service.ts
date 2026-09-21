import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import { ConflictAcknowledgmentStatus } from './conflict-inspector/conflict-inspector.model';
import {
  ClassSchedule,
  ClassScheduleOccurrence,
  ClinicalShiftSummaryItem,
  CohortTermStatusSummary,
  MyTimetableResponse,
  ResourceGridRow,
  ResourceGridType,
  StaffSwapCandidate,
  SwapCandidate,
  SwapTarget,
  TimetableActionResponse,
  TimetableOccurrenceScope,
} from './timetable.model';

@Injectable({ providedIn: 'root' })
export class TimetableService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/timetables`;

  getDraft(termInstanceId: number): Observable<ClassSchedule[]> {
    const params = new HttpParams().set('termInstanceId', termInstanceId);
    return this.http.get<ClassSchedule[]>(`${this.baseUrl}/draft`, { params });
  }

  getPublished(termInstanceId: number): Observable<ClassSchedule[]> {
    const params = new HttpParams().set('termInstanceId', termInstanceId);
    return this.http.get<ClassSchedule[]>(this.baseUrl, { params });
  }

  getClinicalShiftSummary(termInstanceId: number): Observable<ClinicalShiftSummaryItem[]> {
    const params = new HttpParams().set('termInstanceId', termInstanceId);
    return this.http.get<ClinicalShiftSummaryItem[]>(`${this.baseUrl}/draft/clinical-shift-summary`, { params });
  }

  getCohortStatusSummary(termInstanceId: number): Observable<CohortTermStatusSummary[]> {
    const params = new HttpParams().set('termInstanceId', termInstanceId);
    return this.http.get<CohortTermStatusSummary[]>(`${this.baseUrl}/draft/cohort-status-summary`, { params });
  }

  /** OC-260: `cohortIds` is required -- Approve is cohort-scoped now, publishing the chosen subset
   *  without touching any other cohort's draft. `overrideIncompleteCoverage`/`overrideReason` are
   *  only ever sent on a resubmission after the plain first attempt came back with a coverage-gap
   *  conflict (see {@link TimetableCoverageGap}) and an authorized reviewer
   *  (TIMETABLE_APPROVE_INCOMPLETE_OVERRIDE) accepted it with a reason — see
   *  TimetableController#approve's `@PreAuthorize`, the actual enforcement point. */
  approve(termInstanceId: number, cohortIds: number[], overrideIncompleteCoverage = false, overrideReason?: string): Observable<TimetableActionResponse> {
    return this.http.post<TimetableActionResponse>(`${this.baseUrl}/${termInstanceId}/approve`, { cohortIds, overrideIncompleteCoverage, overrideReason });
  }

  /** OC-260: moved from a bodyless DELETE to a body-bearing POST so it can carry the cohort
   *  selection, matching {@link revertToDraft}'s own shape. */
  clear(termInstanceId: number, cohortIds: number[]): Observable<TimetableActionResponse> {
    return this.http.post<TimetableActionResponse>(`${this.baseUrl}/${termInstanceId}/discard-draft`, { cohortIds });
  }

  revertToDraft(termInstanceId: number, cohortIds: number[]): Observable<TimetableActionResponse> {
    return this.http.post<TimetableActionResponse>(`${this.baseUrl}/${termInstanceId}/revert-to-draft`, { cohortIds });
  }

  /** OC-260: per-cohort counterpart of {@link ConflictInspectorService#getAcknowledgmentStatus} --
   *  whether this cohort's own row can show "Conflicts Resolved" right now. */
  getCohortConflictStatus(termInstanceId: number, cohortId: number): Observable<ConflictAcknowledgmentStatus> {
    return this.http.get<ConflictAcknowledgmentStatus>(`${this.baseUrl}/${termInstanceId}/cohorts/${cohortId}/conflict-status`);
  }

  /** Rejects (409, same shape as a conflict scan's violations) if this cohort isn't actually clean
   *  at the moment of calling — the backend always re-scans rather than trusting a stale result. */
  acknowledgeCohortConflicts(termInstanceId: number, cohortId: number): Observable<ConflictAcknowledgmentStatus> {
    return this.http.post<ConflictAcknowledgmentStatus>(`${this.baseUrl}/${termInstanceId}/cohorts/${cohortId}/acknowledge-conflicts`, null);
  }

  getOccurrences(
    termInstanceId: number, from: string, to: string, scope: TimetableOccurrenceScope,
  ): Observable<ClassScheduleOccurrence[]> {
    const params = new HttpParams()
      .set('termInstanceId', termInstanceId).set('from', from).set('to', to).set('scope', scope);
    return this.http.get<ClassScheduleOccurrence[]>(`${this.baseUrl}/occurrences`, { params });
  }

  getMyTimetable(termInstanceId: number, weekStart?: string): Observable<MyTimetableResponse> {
    let params = new HttpParams().set('termInstanceId', termInstanceId);
    if (weekStart) params = params.set('weekStart', weekStart);
    return this.http.get<MyTimetableResponse>(`${this.baseUrl}/me`, { params });
  }

  /** Either `date` (resolved server-side through any DayMappingOverride, e.g. a compensatory
   *  working day) or `dayOfWeek` (the grid's plain Mon-Sat planning-mode toggle) must be
   *  supplied; `date` takes precedence if both are. */
  getResourceGrid(type: ResourceGridType, termInstanceId: number,
                   opts: { date?: string; dayOfWeek?: string }): Observable<ResourceGridRow[]> {
    let params = new HttpParams().set('termInstanceId', termInstanceId);
    params = opts.date ? params.set('date', opts.date) : params.set('dayOfWeek', opts.dayOfWeek!);
    const path = type === 'FACULTY' ? 'resource-grid/faculty' : 'resource-grid/classroom';
    return this.http.get<ResourceGridRow[]>(`${this.baseUrl}/${path}`, { params });
  }

  getStaffSwapCandidates(classScheduleId: number, date: string): Observable<StaffSwapCandidate[]> {
    const params = new HttpParams().set('date', date);
    return this.http.get<StaffSwapCandidate[]>(`${this.baseUrl}/staff-swap/sessions/${classScheduleId}/candidates`, { params });
  }

  applyStaffSwap(classScheduleId: number, targetClassScheduleId: number, date: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/staff-swap/sessions/${classScheduleId}/apply`, { targetClassScheduleId, date });
  }

  getSwapCandidates(termInstanceId: number, sessionId: number): Observable<SwapCandidate[]> {
    return this.http.get<SwapCandidate[]>(`${this.baseUrl}/${termInstanceId}/sessions/${sessionId}/swap-candidates`);
  }

  swapSession(termInstanceId: number, sessionId: number, target: SwapTarget): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${termInstanceId}/sessions/${sessionId}/swap`, target);
  }
}

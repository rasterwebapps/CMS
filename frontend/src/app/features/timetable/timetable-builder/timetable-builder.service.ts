import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments';
import {
  DutyDayMovePreview,
  DutyDayMoveRequest,
  ElectiveGroupScheduleResponse,
  TimetableRelocateRequest,
  TimetableRelocationPlan,
  GlobalAutoSchedulePrerequisites,
  GlobalAutoScheduleResult,
  GlobalCapacityPrecheckResult,
  TimetableBuilderResponse,
  TimetableCell,
  TimetableCellMoveRequest,
  TimetableCellPlacementRequest,
  TimetableCellReplaceRequest,
  TimetableCellReplaceResponse,
  TimetableCellSwapRequest,
  TimetablePlacementCandidate,
  TimetableSessionType,
  TimetableSlotPreview,
} from './timetable-builder.model';

@Injectable({ providedIn: 'root' })
export class TimetableBuilderService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/timetables/skeleton`;

  getCohortSkeleton(termInstanceId: number, cohortId: number): Observable<TimetableBuilderResponse> {
    return this.http.get<TimetableBuilderResponse>(this.baseUrl, {
      params: { termInstanceId: termInstanceId.toString(), cohortId: cohortId.toString() },
    });
  }

  suggestCandidates(courseOfferingId: number, sessionType: TimetableSessionType, batchId: number | null, cohortSectionId: number | null): Observable<TimetablePlacementCandidate[]> {
    const params: Record<string, string> = {
      courseOfferingId: courseOfferingId.toString(),
      sessionType,
    };
    if (batchId != null) params['batchId'] = batchId.toString();
    if (cohortSectionId != null) params['cohortSectionId'] = cohortSectionId.toString();
    return this.http.get<TimetablePlacementCandidate[]>(`${this.baseUrl}/suggest`, { params });
  }

  placeCell(request: TimetableCellPlacementRequest): Observable<TimetableCell> {
    return this.http.post<TimetableCell>(`${this.baseUrl}/cells`, request);
  }

  removeCell(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/cells/${id}`);
  }

  moveCell(id: number, request: TimetableCellMoveRequest): Observable<TimetableCell> {
    return this.http.put<TimetableCell>(`${this.baseUrl}/cells/${id}/move`, request);
  }

  /** Pin a cell so the next Run Automation packs the week around it, or unpin it to hand it back
   *  to automation. Applies to every period of a multi-period session server-side. */
  setCellPinned(id: number, pinned: boolean): Observable<TimetableCell> {
    return this.http.put<TimetableCell>(`${this.baseUrl}/cells/${id}/pin`, null, {
      params: { pinned },
    });
  }

  /** Hand this Theory cell's slot to a different subject + faculty, keeping its day/period/audience.
   *  Applies to every period of a multi-period session server-side, and pins the result (a
   *  deliberate human decision, same as a drag-move). The response carries the displaced subject's
   *  resulting weekly shortfall so the caller can surface what now needs re-placing. */
  replaceCell(id: number, request: TimetableCellReplaceRequest): Observable<TimetableCellReplaceResponse> {
    return this.http.put<TimetableCellReplaceResponse>(`${this.baseUrl}/cells/${id}/replace`, request);
  }

  swapCells(id: number, request: TimetableCellSwapRequest): Observable<TimetableCell[]> {
    return this.http.put<TimetableCell[]>(`${this.baseUrl}/cells/${id}/swap`, request);
  }

  previewMoveTargets(id: number, cohortId: number): Observable<TimetableSlotPreview[]> {
    return this.http.get<TimetableSlotPreview[]>(`${this.baseUrl}/cells/${id}/move-preview`, {
      params: { cohortId: cohortId.toString() },
    });
  }

  /** Every same-length window this session could go to with its whole block — MOVE, SWAP, or why not. */
  previewRelocation(id: number, cohortId: number): Observable<TimetableRelocationPlan[]> {
    return this.http.get<TimetableRelocationPlan[]>(`${this.baseUrl}/cells/${id}/relocate-preview`, {
      params: { cohortId: cohortId.toString() },
    });
  }

  /** Move or swap a session with its whole block, all-or-nothing; everything moved is pinned. */
  relocate(id: number, request: TimetableRelocateRequest): Observable<TimetableCell[]> {
    return this.http.put<TimetableCell[]>(`${this.baseUrl}/cells/${id}/relocate`, request);
  }

  previewDutyDayMove(shiftGroupId: number, cohortId: number): Observable<DutyDayMovePreview[]> {
    return this.http.get<DutyDayMovePreview[]>(`${this.baseUrl}/clinical-shift-groups/${shiftGroupId}/day-preview`, {
      params: { cohortId: cohortId.toString() },
    });
  }

  moveDutyDay(shiftGroupId: number, request: DutyDayMoveRequest): Observable<TimetableCell[]> {
    return this.http.put<TimetableCell[]>(`${this.baseUrl}/clinical-shift-groups/${shiftGroupId}/day`, request);
  }

  getElectiveGroupSchedule(electiveGroupId: number, termInstanceId: number): Observable<ElectiveGroupScheduleResponse> {
    return this.http.get<ElectiveGroupScheduleResponse>(`${this.baseUrl}/elective-groups/${electiveGroupId}/schedule`, {
      params: { termInstanceId: termInstanceId.toString() },
    });
  }

  checkGlobalAutoPlacePrerequisites(termInstanceId: number, cohortId: number | null): Observable<GlobalAutoSchedulePrerequisites> {
    const params: Record<string, string> = { termInstanceId: termInstanceId.toString() };
    if (cohortId != null) params['cohortId'] = cohortId.toString();
    return this.http.get<GlobalAutoSchedulePrerequisites>(`${this.baseUrl}/global-auto-place/prerequisites`, { params });
  }

  /** Whether this term already has any active DRAFT session placed, for any cohort — checked before
   *  opening the Global Auto-Schedule flyout for an "All cohorts" run, so the admin confirms
   *  up front that running will overwrite what's already there. */
  hasExistingDraftContent(termInstanceId: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/global-auto-place/has-existing-draft`, {
      params: { termInstanceId: termInstanceId.toString() },
    });
  }

  precheckGlobalAutoPlace(termInstanceId: number): Observable<GlobalCapacityPrecheckResult> {
    return this.http.get<GlobalCapacityPrecheckResult>(`${this.baseUrl}/global-auto-place/precheck`, {
      params: { termInstanceId: termInstanceId.toString() },
    });
  }

  globalAutoPlace(termInstanceId: number, cohortId: number | null): Observable<GlobalAutoScheduleResult> {
    const params: Record<string, string> = { termInstanceId: termInstanceId.toString() };
    if (cohortId != null) params['cohortId'] = cohortId.toString();
    return this.http.post<GlobalAutoScheduleResult>(`${this.baseUrl}/global-auto-place`, null, { params });
  }

  /** Applies the run report's clinical duty-length fit to one Course Offering (OC-227). The server
   *  recomputes the minutes itself — nothing but the offering id is sent. */
  applyClinicalDutyFit(courseOfferingId: number): Observable<unknown> {
    return this.http.post<unknown>(`${this.baseUrl}/clinical-duty-fit/${courseOfferingId}`, null);
  }
}

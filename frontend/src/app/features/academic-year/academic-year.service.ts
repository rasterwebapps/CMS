import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import { ClinicalShiftConfigUpdateRequest } from '../clinical-shift-group/clinical-shift-group.model';
import {
  AcademicYear,
  AcademicYearFullUpdateRequest,
  AcademicYearRequest,
  CalendarEvent,
  CalendarEventRequest,
  CalendarEventType,
  ClassInchargeAssignment,
  CohortLapsedSummary,
  CohortSeatsRequest,
  CohortSummary,
  CourseOffering,
  CourseOfferingFacultySummary,
  CourseOfferingSectionFacultyResponse,
  CourseOfferingStatusUpdateRequest,
  CourseOfferingStatusUpdateResponse,
  CourseRegistration,
  EligibleFacultyCandidate,
  ElectiveBulkAssignmentResponse,
  ElectiveGroupSummary,
  ElectiveSelectionMode,
  FacultyCapacityCheckResult,
  SectionFacultyAssignment,
  FeeDemand,
  GenerateCourseOfferingsResponse,
  GenerateCourseRegistrationsResponse,
  GenerateDemandsResponse,
  GenerateEnrollmentsResponse,
  DemandStatus,
  Page,
  SeatAvailabilityResponse,
  StudentTermEnrollment,
  TermAdvanceChecklist,
  TermInstance,
  TermInstanceStatus,
  TermInstanceUpdateRequest,
  TermBillingSchedule,
  TermBillingScheduleRequest,
  WeekOfMonth,
} from './academic-year.model';

@Injectable({
  providedIn: 'root',
})
export class AcademicYearService {
  private readonly http = inject(HttpClient);
  private readonly academicYearUrl = `${environment.apiUrl}/academic-years`;
  private readonly calendarEventUrl = `${environment.apiUrl}/calendar-events`;

  // Academic Year methods
  getAllAcademicYears(): Observable<AcademicYear[]> {
    return this.http.get<AcademicYear[]>(this.academicYearUrl);
  }

  getPage(p: { search?: string; isCurrent?: boolean; page?: number; size?: number; sort?: string; direction?: 'asc' | 'desc' }): Observable<Page<AcademicYear>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search) params = params.set('search', p.search);
    if (p.isCurrent !== undefined && p.isCurrent !== null) params = params.set('isCurrent', String(p.isCurrent));
    if (p.sort) params = params.set('sort', `${p.sort},${p.direction ?? 'asc'}`);
    return this.http.get<Page<AcademicYear>>(`${this.academicYearUrl}/page`, { params });
  }

  getAcademicYearById(id: number): Observable<AcademicYear> {
    return this.http.get<AcademicYear>(`${this.academicYearUrl}/${id}`);
  }

  getCurrentAcademicYear(): Observable<AcademicYear> {
    return this.http.get<AcademicYear>(`${this.academicYearUrl}/current`);
  }

  createAcademicYear(request: AcademicYearRequest): Observable<AcademicYear> {
    return this.http.post<AcademicYear>(this.academicYearUrl, request);
  }

  updateAcademicYear(id: number, request: AcademicYearRequest): Observable<AcademicYear> {
    return this.http.put<AcademicYear>(`${this.academicYearUrl}/${id}`, request);
  }

  updateAcademicYearFull(id: number, request: AcademicYearFullUpdateRequest): Observable<AcademicYear> {
    return this.http.put<AcademicYear>(`${this.academicYearUrl}/${id}/full`, request);
  }

  deleteAcademicYear(id: number): Observable<void> {
    return this.http.delete<void>(`${this.academicYearUrl}/${id}`);
  }

  getCohortsByAcademicYear(academicYearId: number): Observable<CohortSummary[]> {
    return this.http.get<CohortSummary[]>(`${environment.apiUrl}/cohorts`, {
      params: { academicYearId: academicYearId.toString() },
    });
  }

  /** All cohorts regardless of admission year — used by screens that operate on a cohort's
   *  current progression rather than its admission batch (e.g. Student Promotion). */
  getAllCohorts(): Observable<CohortSummary[]> {
    return this.http.get<CohortSummary[]>(`${environment.apiUrl}/cohorts`);
  }

  initializeCohorts(academicYearId: number): Observable<CohortSummary[]> {
    return this.http.post<CohortSummary[]>(`${environment.apiUrl}/cohorts/initialize`, null, {
      params: { academicYearId: academicYearId.toString() },
    });
  }

  updateCohortSeats(cohortId: number, request: CohortSeatsRequest): Observable<CohortSummary> {
    return this.http.patch<CohortSummary>(`${environment.apiUrl}/cohorts/${cohortId}/seats`, request);
  }

  deleteCohort(cohortId: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/cohorts/${cohortId}`);
  }

  setQuotaStatus(cohortId: number, quota: 'MANAGEMENT' | 'COUNSELLING', closed: boolean): Observable<CohortSummary> {
    return this.http.patch<CohortSummary>(
      `${environment.apiUrl}/cohorts/${cohortId}/quota-status`,
      { quota, closed },
    );
  }

  getSeatAvailability(courseId: number, academicYearId: number, quota: 'MANAGEMENT' | 'COUNSELLING'): Observable<SeatAvailabilityResponse> {
    return this.http.get<SeatAvailabilityResponse>(
      `${environment.apiUrl}/cohorts/seat-availability`,
      { params: { courseId: courseId.toString(), academicYearId: academicYearId.toString(), quota } },
    );
  }

  getLapsedSummary(academicYearId?: number): Observable<CohortLapsedSummary> {
    const params = academicYearId ? new HttpParams().set('academicYearId', academicYearId.toString()) : undefined;
    return this.http.get<CohortLapsedSummary>(`${environment.apiUrl}/cohorts/lapsed-summary`, { params });
  }

  // Calendar Event methods
  getCalendarEventsByAcademicYear(
    academicYearId: number,
    eventType?: CalendarEventType,
  ): Observable<CalendarEvent[]> {
    let params = new HttpParams();
    if (eventType) {
      params = params.set('eventType', eventType);
    }
    return this.http.get<CalendarEvent[]>(
      `${this.calendarEventUrl}/academic-year/${academicYearId}`,
      { params },
    );
  }

  createCalendarEvent(request: CalendarEventRequest): Observable<CalendarEvent> {
    return this.http.post<CalendarEvent>(this.calendarEventUrl, request);
  }

  updateCalendarEvent(id: number, request: CalendarEventRequest): Observable<CalendarEvent> {
    return this.http.put<CalendarEvent>(`${this.calendarEventUrl}/${id}`, request);
  }

  deleteCalendarEvent(id: number): Observable<void> {
    return this.http.delete<void>(`${this.calendarEventUrl}/${id}`);
  }

  /** "Delete this and all future occurrences" for an event seeded from a recurring Holiday
   *  Template -- stops the template from seeding further years and removes this + any other
   *  future-dated instance it generated. Past occurrences are never touched. */
  deleteCalendarEventSeries(id: number): Observable<void> {
    return this.http.delete<void>(`${this.calendarEventUrl}/${id}/series`);
  }

  /** Save-time conflict check -- any event (any type) overlapping a proposed date range, so the
   *  flyout can warn before creating/updating an event that collides with something already
   *  scheduled. */
  checkOverlappingEvents(
    academicYearId: number,
    start: string,
    end: string,
    excludeId?: number,
  ): Observable<CalendarEvent[]> {
    let params = new HttpParams().set('start', start).set('end', end);
    if (excludeId != null) params = params.set('excludeId', excludeId.toString());
    return this.http.get<CalendarEvent[]>(
      `${this.calendarEventUrl}/academic-year/${academicYearId}/overlapping`,
      { params },
    );
  }

  // TermInstance methods
  getTermInstancesByAcademicYear(academicYearId: number): Observable<TermInstance[]> {
    return this.http.get<TermInstance[]>(`${environment.apiUrl}/term-instances`, {
      params: { academicYearId: academicYearId.toString() }
    });
  }

  updateTermInstance(id: number, request: TermInstanceUpdateRequest): Observable<TermInstance> {
    return this.http.put<TermInstance>(`${environment.apiUrl}/term-instances/${id}`, request);
  }

  getTermAdvanceChecklist(id: number, targetStatus: TermInstanceStatus): Observable<TermAdvanceChecklist> {
    return this.http.get<TermAdvanceChecklist>(`${environment.apiUrl}/term-instances/${id}/advance-checklist`, {
      params: { targetStatus },
    });
  }

  /** Empty means this term hasn't opted in to Saturday scheduling at all — Mon-Fri only. */
  getWorkingSaturdays(termInstanceId: number): Observable<WeekOfMonth[]> {
    return this.http.get<WeekOfMonth[]>(`${environment.apiUrl}/term-instances/${termInstanceId}/working-saturdays`);
  }

  updateWorkingSaturdays(termInstanceId: number, weeks: WeekOfMonth[]): Observable<WeekOfMonth[]> {
    return this.http.put<WeekOfMonth[]>(
      `${environment.apiUrl}/term-instances/${termInstanceId}/working-saturdays`, { weeks });
  }

  // TermBillingSchedule methods
  getTermBillingSchedulesByAcademicYear(academicYearId: number): Observable<TermBillingSchedule[]> {
    return this.http.get<TermBillingSchedule[]>(`${environment.apiUrl}/term-billing-schedules`, {
      params: { academicYearId: academicYearId.toString() }
    });
  }

  createOrUpdateTermBillingSchedule(request: TermBillingScheduleRequest): Observable<TermBillingSchedule> {
    return this.http.post<TermBillingSchedule>(`${environment.apiUrl}/term-billing-schedules`, request);
  }

  // StudentTermEnrollment methods
  getEnrollmentsByTermInstance(termInstanceId: number): Observable<StudentTermEnrollment[]> {
    return this.http.get<StudentTermEnrollment[]>(
      `${environment.apiUrl}/student-term-enrollments?termInstanceId=${termInstanceId}`,
    );
  }

  /** Scoped by the elective group's own course (server-side) so it never mixes in another
   *  program/course's students who happen to share the same termInstance+semesterNumber --
   *  the correct filter for Elective Assignment, unlike getEnrollmentsByTermInstance alone. */
  getEnrollmentsByElectiveGroup(termInstanceId: number, electiveGroupId: number): Observable<StudentTermEnrollment[]> {
    return this.http.get<StudentTermEnrollment[]>(`${environment.apiUrl}/student-term-enrollments`, {
      params: { termInstanceId: termInstanceId.toString(), electiveGroupId: electiveGroupId.toString() },
    });
  }

  generateEnrollments(termInstanceId: number): Observable<GenerateEnrollmentsResponse> {
    return this.http.post<GenerateEnrollmentsResponse>(
      `${environment.apiUrl}/student-term-enrollments/generate?termInstanceId=${termInstanceId}`,
      {},
    );
  }

  // CourseOffering methods
  /** Pass cohortId whenever the caller already knows which cohort it's planning for — it's the
   *  only filter that also pins curriculumVersion server-side, so it never mixes in another
   *  cohort/program's offerings that happen to share the same termInstanceId+semesterNumber (a
   *  shared TermInstance can concurrently host several cohorts/programs at once). semesterNumber
   *  alone only guards against mixing different admission years of the SAME curriculum. */
  getCourseOfferingsByTermInstance(
    termInstanceId: number,
    semesterNumber?: number,
    cohortId?: number,
  ): Observable<CourseOffering[]> {
    let params = new HttpParams().set('termInstanceId', termInstanceId.toString());
    if (cohortId != null) {
      params = params.set('cohortId', cohortId.toString());
    } else if (semesterNumber != null) {
      // Backend query param is named termNumber (matches CourseOffering.termNumber / the
      // course_offerings.term_number column) -- semesterNumber is just this method's public name.
      params = params.set('termNumber', semesterNumber.toString());
    }
    return this.http.get<CourseOffering[]>(`${environment.apiUrl}/course-offerings`, { params });
  }

  generateCourseOfferings(termInstanceId: number): Observable<GenerateCourseOfferingsResponse> {
    return this.http.post<GenerateCourseOfferingsResponse>(
      `${environment.apiUrl}/course-offerings/generate?termInstanceId=${termInstanceId}`,
      {},
    );
  }

  getCourseOfferingById(id: number): Observable<CourseOffering> {
    return this.http.get<CourseOffering>(`${environment.apiUrl}/course-offerings/${id}`);
  }

  /** Live pre-save check for a whole-cohort assignment (no section split) — same math the save
   *  hard-blocks on, surfaced early so the admin sees it before saving. */
  checkFacultyCapacityForCohort(offeringId: number, cohortId: number, facultyId: number): Observable<FacultyCapacityCheckResult> {
    const params = new HttpParams().set('cohortId', cohortId.toString()).set('facultyId', facultyId.toString());
    return this.http.get<FacultyCapacityCheckResult>(
      `${environment.apiUrl}/course-offerings/${offeringId}/cohort-faculty-capacity-check`, { params });
  }

  /** Every eligible (Speciality match OR the subject's Eligible Faculty list) active faculty for
   *  this offering, annotated with remaining term capacity, sorted most-free-first by the backend. */
  getEligibleFaculty(offeringId: number): Observable<EligibleFacultyCandidate[]> {
    return this.http.get<EligibleFacultyCandidate[]>(`${environment.apiUrl}/course-offerings/${offeringId}/eligible-faculty`);
  }

  /** Section-scoped counterpart of {@link getEligibleFaculty} — each candidate's remaining capacity
   *  is projected against just this section's own Theory hours rather than the whole offering's. */
  getEligibleFacultyForSection(offeringId: number, cohortSectionId: number): Observable<EligibleFacultyCandidate[]> {
    return this.http.get<EligibleFacultyCandidate[]>(
      `${environment.apiUrl}/course-offerings/${offeringId}/sections/${cohortSectionId}/eligible-faculty`);
  }

  /** Cohort-scoped counterpart of {@link getEligibleFacultyForSection} — for a cohort with no
   *  active section split, projecting each candidate's load against the cohort's whole
   *  theory+lab+clinical hours. */
  getEligibleFacultyForCohort(offeringId: number, cohortId: number): Observable<EligibleFacultyCandidate[]> {
    return this.http.get<EligibleFacultyCandidate[]>(
      `${environment.apiUrl}/course-offerings/${offeringId}/cohorts/${cohortId}/eligible-faculty`);
  }

  /** Replaces the offering's admin-curated faculty pool wholesale — the primary/section assignment
   *  pickers are then scoped to just this pool. Returns the refreshed eligible-faculty list. */
  updateFacultyPool(offeringId: number, facultyIds: number[]): Observable<EligibleFacultyCandidate[]> {
    return this.http.put<EligibleFacultyCandidate[]>(
      `${environment.apiUrl}/course-offerings/${offeringId}/faculty-pool`, { facultyIds });
  }

  updateCourseOfferingStatus(id: number, request: CourseOfferingStatusUpdateRequest): Observable<CourseOfferingStatusUpdateResponse> {
    return this.http.patch<CourseOfferingStatusUpdateResponse>(`${environment.apiUrl}/course-offerings/${id}/status`, request);
  }

  updateClinicalShiftConfig(id: number, request: ClinicalShiftConfigUpdateRequest): Observable<CourseOffering> {
    return this.http.put<CourseOffering>(`${environment.apiUrl}/course-offerings/${id}/clinical-shift-config`, request);
  }

  /** Roll-up of every offering's currently-assigned faculty in a term instance, in one call —
   *  backs the Assign Faculty list table's Faculty column. */
  getFacultyAssignmentSummary(termInstanceId: number): Observable<CourseOfferingFacultySummary[]> {
    const params = new HttpParams().set('termInstanceId', termInstanceId.toString());
    return this.http.get<CourseOfferingFacultySummary[]>(
      `${environment.apiUrl}/course-offerings/faculty-assignment-summary`, { params });
  }

  getSectionFaculty(offeringId: number): Observable<CourseOfferingSectionFacultyResponse> {
    return this.http.get<CourseOfferingSectionFacultyResponse>(`${environment.apiUrl}/course-offerings/${offeringId}/section-faculty`);
  }

  /** facultyId null clears this section's assignment. */
  updateSectionFaculty(offeringId: number, cohortSectionId: number, facultyId: number | null): Observable<SectionFacultyAssignment> {
    return this.http.put<SectionFacultyAssignment>(
      `${environment.apiUrl}/course-offerings/${offeringId}/section-faculty/${cohortSectionId}`, { facultyId });
  }

  /** Whole-cohort counterpart of {@link updateSectionFaculty} — for a cohort with no active
   *  section split. facultyId null clears the assignment. */
  updateCohortFaculty(offeringId: number, cohortId: number, facultyId: number | null): Observable<SectionFacultyAssignment> {
    return this.http.put<SectionFacultyAssignment>(
      `${environment.apiUrl}/course-offerings/${offeringId}/cohort-faculty/${cohortId}`, { facultyId });
  }

  getClassIncharge(termInstanceId: number): Observable<ClassInchargeAssignment[]> {
    const params = new HttpParams().set('termInstanceId', termInstanceId.toString());
    return this.http.get<ClassInchargeAssignment[]>(`${environment.apiUrl}/class-incharge`, { params });
  }

  /** facultyId null clears this section's incharge — there is no fallback to revert to. */
  updateClassIncharge(cohortSectionId: number, facultyId: number | null): Observable<ClassInchargeAssignment> {
    return this.http.put<ClassInchargeAssignment>(
      `${environment.apiUrl}/class-incharge/${cohortSectionId}`, { facultyId });
  }

  // CourseRegistration methods
  getCourseRegistrationsByEnrollment(enrollmentId: number): Observable<CourseRegistration[]> {
    return this.http.get<CourseRegistration[]>(
      `${environment.apiUrl}/course-registrations?enrollmentId=${enrollmentId}`,
    );
  }

  getCourseRegistrationsByCourseOffering(courseOfferingId: number): Observable<CourseRegistration[]> {
    return this.http.get<CourseRegistration[]>(
      `${environment.apiUrl}/course-registrations?courseOfferingId=${courseOfferingId}`,
    );
  }

  generateCourseRegistrations(termInstanceId: number): Observable<GenerateCourseRegistrationsResponse> {
    return this.http.post<GenerateCourseRegistrationsResponse>(
      `${environment.apiUrl}/course-registrations/generate?termInstanceId=${termInstanceId}`,
      {},
    );
  }

  assignElectiveChoice(enrollmentId: number, courseOfferingId: number): Observable<CourseRegistration> {
    return this.http.post<CourseRegistration>(
      `${environment.apiUrl}/course-registrations/elective-assignment`,
      { enrollmentId, courseOfferingId },
    );
  }

  getElectiveOfferingOptions(termInstanceId: number, electiveGroupId: number): Observable<CourseOffering[]> {
    return this.http.get<CourseOffering[]>(`${environment.apiUrl}/course-offerings/elective-options`, {
      params: { termInstanceId: termInstanceId.toString(), electiveGroupId: electiveGroupId.toString() }
    });
  }

  /** Institution-decided mode: assigns every eligible student in the group to the same offering,
   *  overwriting any existing choice they already had in that group. */
  bulkAssignElectiveChoice(
    termInstanceId: number, electiveGroupId: number, courseOfferingId: number,
  ): Observable<ElectiveBulkAssignmentResponse> {
    return this.http.post<ElectiveBulkAssignmentResponse>(
      `${environment.apiUrl}/course-registrations/elective-assignment/bulk`,
      { termInstanceId, electiveGroupId, courseOfferingId },
    );
  }

  updateElectiveGroupSelectionMode(
    electiveGroupId: number, selectionMode: ElectiveSelectionMode,
  ): Observable<{ id: number; selectionMode: ElectiveSelectionMode }> {
    return this.http.put<{ id: number; selectionMode: ElectiveSelectionMode }>(
      `${environment.apiUrl}/curriculum-elective-groups/${electiveGroupId}/selection-mode`,
      { selectionMode },
    );
  }

  /** One row per elective group open in a term, with eligible/assigned counts and scheduled
   *  status — backs the Elective Assignment screen's group-launcher view. */
  getElectiveGroupSummaries(termInstanceId: number): Observable<ElectiveGroupSummary[]> {
    return this.http.get<ElectiveGroupSummary[]>(
      `${environment.apiUrl}/course-registrations/elective-assignment/summary`,
      { params: { termInstanceId: termInstanceId.toString() } },
    );
  }

  // FeeDemand methods
  getFeeDemandsByTermInstance(
    termInstanceId: number,
    status?: DemandStatus,
  ): Observable<FeeDemand[]> {
    let url = `${environment.apiUrl}/fee-demands?termInstanceId=${termInstanceId}`;
    if (status) {
      url += `&status=${status}`;
    }
    return this.http.get<FeeDemand[]>(url);
  }

  generateFeeDemands(termInstanceId: number): Observable<GenerateDemandsResponse> {
    return this.http.post<GenerateDemandsResponse>(
      `${environment.apiUrl}/fee-demands/generate?termInstanceId=${termInstanceId}`,
      {},
    );
  }

}


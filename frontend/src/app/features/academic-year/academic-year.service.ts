import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  AcademicYear,
  AcademicYearFullUpdateRequest,
  AcademicYearRequest,
  CalendarEvent,
  CalendarEventRequest,
  CalendarEventType,
  CohortLapsedSummary,
  CohortSeatsRequest,
  CohortSummary,
  CourseOffering,
  CourseOfferingUpdateRequest,
  CourseRegistration,
  FeeDemand,
  GenerateCourseOfferingsResponse,
  GenerateCourseRegistrationsResponse,
  GenerateDemandsResponse,
  GenerateEnrollmentsResponse,
  DemandStatus,
  Page,
  SeatAvailabilityResponse,
  StudentTermEnrollment,
  TermInstance,
  TermInstanceUpdateRequest,
  TermBillingSchedule,
  TermBillingScheduleRequest,
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

  // TermInstance methods
  getTermInstancesByAcademicYear(academicYearId: number): Observable<TermInstance[]> {
    return this.http.get<TermInstance[]>(`${environment.apiUrl}/term-instances`, {
      params: { academicYearId: academicYearId.toString() }
    });
  }

  updateTermInstance(id: number, request: TermInstanceUpdateRequest): Observable<TermInstance> {
    return this.http.put<TermInstance>(`${environment.apiUrl}/term-instances/${id}`, request);
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
    const baseUrl = environment.apiUrl.replace('/api/v1', '');
    return this.http.get<StudentTermEnrollment[]>(
      `${baseUrl}/api/student-term-enrollments?termInstanceId=${termInstanceId}`,
    );
  }

  generateEnrollments(termInstanceId: number): Observable<GenerateEnrollmentsResponse> {
    const baseUrl = environment.apiUrl.replace('/api/v1', '');
    return this.http.post<GenerateEnrollmentsResponse>(
      `${baseUrl}/api/student-term-enrollments/generate?termInstanceId=${termInstanceId}`,
      {},
    );
  }

  // CourseOffering methods
  getCourseOfferingsByTermInstance(
    termInstanceId: number,
    semesterNumber?: number,
  ): Observable<CourseOffering[]> {
    const baseUrl = environment.apiUrl.replace('/api/v1', '');
    let params = new HttpParams().set('termInstanceId', termInstanceId.toString());
    if (semesterNumber != null) {
      params = params.set('semesterNumber', semesterNumber.toString());
    }
    return this.http.get<CourseOffering[]>(`${baseUrl}/api/course-offerings`, { params });
  }

  generateCourseOfferings(termInstanceId: number): Observable<GenerateCourseOfferingsResponse> {
    const baseUrl = environment.apiUrl.replace('/api/v1', '');
    return this.http.post<GenerateCourseOfferingsResponse>(
      `${baseUrl}/api/course-offerings/generate?termInstanceId=${termInstanceId}`,
      {},
    );
  }

  updateCourseOffering(id: number, request: CourseOfferingUpdateRequest): Observable<CourseOffering> {
    const baseUrl = environment.apiUrl.replace('/api/v1', '');
    return this.http.put<CourseOffering>(`${baseUrl}/api/course-offerings/${id}`, request);
  }

  deactivateCourseOffering(id: number): Observable<void> {
    const baseUrl = environment.apiUrl.replace('/api/v1', '');
    return this.http.delete<void>(`${baseUrl}/api/course-offerings/${id}`);
  }

  // CourseRegistration methods
  getCourseRegistrationsByEnrollment(enrollmentId: number): Observable<CourseRegistration[]> {
    const baseUrl = environment.apiUrl.replace('/api/v1', '');
    return this.http.get<CourseRegistration[]>(
      `${baseUrl}/api/course-registrations?enrollmentId=${enrollmentId}`,
    );
  }

  getCourseRegistrationsByCourseOffering(courseOfferingId: number): Observable<CourseRegistration[]> {
    const baseUrl = environment.apiUrl.replace('/api/v1', '');
    return this.http.get<CourseRegistration[]>(
      `${baseUrl}/api/course-registrations?courseOfferingId=${courseOfferingId}`,
    );
  }

  generateCourseRegistrations(termInstanceId: number): Observable<GenerateCourseRegistrationsResponse> {
    const baseUrl = environment.apiUrl.replace('/api/v1', '');
    return this.http.post<GenerateCourseRegistrationsResponse>(
      `${baseUrl}/api/course-registrations/generate?termInstanceId=${termInstanceId}`,
      {},
    );
  }

  // FeeDemand methods
  getFeeDemandsByTermInstance(
    termInstanceId: number,
    status?: DemandStatus,
  ): Observable<FeeDemand[]> {
    const baseUrl = environment.apiUrl.replace('/api/v1', '');
    let url = `${baseUrl}/api/fee-demands?termInstanceId=${termInstanceId}`;
    if (status) {
      url += `&status=${status}`;
    }
    return this.http.get<FeeDemand[]>(url);
  }

  generateFeeDemands(termInstanceId: number): Observable<GenerateDemandsResponse> {
    const baseUrl = environment.apiUrl.replace('/api/v1', '');
    return this.http.post<GenerateDemandsResponse>(
      `${baseUrl}/api/fee-demands/generate?termInstanceId=${termInstanceId}`,
      {},
    );
  }

}


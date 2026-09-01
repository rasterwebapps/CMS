import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments';
import { CapacityPlan, FacultyWorkloadOverviewReport, FacultyWorkloadReport, LabClinicalVenueCapacity, PlanningBasis, TermCapacityOverview, VenueRebalancePreview, VenueRebalanceResult } from './capacity-planner.model';

@Injectable({ providedIn: 'root' })
export class CapacityPlannerService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/timetables/capacity-plan`;

  getPlan(termInstanceId: number, cohortId: number, planningBasis: PlanningBasis): Observable<CapacityPlan> {
    const params: Record<string, string> = {
      termInstanceId: termInstanceId.toString(),
      cohortId: cohortId.toString(),
      planningBasis,
    };
    return this.http.get<CapacityPlan>(this.baseUrl, { params });
  }

  getFacultyWorkloadReport(termInstanceId: number): Observable<FacultyWorkloadReport> {
    return this.http.get<FacultyWorkloadReport>(`${this.baseUrl}/faculty-workload`, {
      params: { termInstanceId: termInstanceId.toString() },
    });
  }

  getFacultyWorkloadOverview(termInstanceId: number): Observable<FacultyWorkloadOverviewReport> {
    return this.http.get<FacultyWorkloadOverviewReport>(`${this.baseUrl}/faculty-workload-overview`, {
      params: { termInstanceId: termInstanceId.toString() },
    });
  }

  getTermOverview(termInstanceId: number, planningBasis: PlanningBasis): Observable<TermCapacityOverview> {
    return this.http.get<TermCapacityOverview>(`${this.baseUrl}/term-overview`, {
      params: { termInstanceId: termInstanceId.toString(), planningBasis },
    });
  }

  /** The real weekly-demand-vs-window over/tight classification — deliberately NOT derived from
   *  {@link getPlan}'s own `labUtilization`/`clinicalVenueUtilization` figures, which measure a
   *  different thing entirely (already-placed schedule cells vs. a fixed slot grid). This is what
   *  actually gates whether "Rebalance now" has anything to do. */
  getVenueCapacity(termInstanceId: number, planningBasis: PlanningBasis): Observable<LabClinicalVenueCapacity> {
    return this.http.get<LabClinicalVenueCapacity>(`${this.baseUrl}/venue-capacity`, {
      params: { termInstanceId: termInstanceId.toString(), planningBasis },
    });
  }

  previewRebalance(termInstanceId: number, sessionType: 'LAB' | 'CLINICAL', venueId: number): Observable<VenueRebalancePreview> {
    return this.http.get<VenueRebalancePreview>(`${this.baseUrl}/rebalance-preview`, {
      params: { termInstanceId: termInstanceId.toString(), sessionType, venueId: venueId.toString() },
    });
  }

  applyRebalance(termInstanceId: number, sessionType: 'LAB' | 'CLINICAL', venueId: number, batchIds: number[]): Observable<VenueRebalanceResult> {
    return this.http.post<VenueRebalanceResult>(`${this.baseUrl}/rebalance`, { sessionType, venueId, batchIds }, {
      params: { termInstanceId: termInstanceId.toString() },
    });
  }
}

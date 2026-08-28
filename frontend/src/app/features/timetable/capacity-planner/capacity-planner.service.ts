import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments';
import { CapacityPlan, FacultyWorkloadOverviewReport, FacultyWorkloadReport, PlanningBasis, TermCapacityOverview } from './capacity-planner.model';

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
}

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments';
import { CapacityPlan } from './capacity-planner.model';

@Injectable({ providedIn: 'root' })
export class CapacityPlannerService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/timetables/capacity-plan`;

  getPlan(termInstanceId: number, cohortId: number, targetBatchSize: number | null): Observable<CapacityPlan> {
    const params: Record<string, string> = {
      termInstanceId: termInstanceId.toString(),
      cohortId: cohortId.toString(),
    };
    if (targetBatchSize) params['targetBatchSize'] = targetBatchSize.toString();
    return this.http.get<CapacityPlan>(this.baseUrl, { params });
  }
}

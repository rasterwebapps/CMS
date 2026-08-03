import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import { PortionShortfall, SyllabusUnitPlan, UnitVariance } from './portion-blueprint.model';

@Injectable({ providedIn: 'root' })
export class PortionBlueprintService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/portion-blueprint`;

  generateBlueprint(courseOfferingId: number): Observable<SyllabusUnitPlan[]> {
    return this.http.post<SyllabusUnitPlan[]>(
      `${this.baseUrl}/course-offerings/${courseOfferingId}/generate`, {});
  }

  getBlueprint(courseOfferingId: number): Observable<SyllabusUnitPlan[]> {
    return this.http.get<SyllabusUnitPlan[]>(`${this.baseUrl}/course-offerings/${courseOfferingId}`);
  }

  getProjection(courseOfferingId: number): Observable<UnitVariance[]> {
    return this.http.get<UnitVariance[]>(`${this.baseUrl}/course-offerings/${courseOfferingId}/projection`);
  }

  checkShortfall(termInstanceId: number, cohortId: number): Observable<PortionShortfall> {
    const params = new HttpParams()
      .set('termInstanceId', termInstanceId.toString())
      .set('cohortId', cohortId.toString());
    return this.http.get<PortionShortfall>(`${this.baseUrl}/shortfall`, { params });
  }
}

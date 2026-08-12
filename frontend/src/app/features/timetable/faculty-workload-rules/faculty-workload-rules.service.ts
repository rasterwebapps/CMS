import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments';
import { FacultyWorkloadRules } from './faculty-workload-rules.model';

@Injectable({ providedIn: 'root' })
export class FacultyWorkloadRulesService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/timetables/workload-rules`;

  get(): Observable<FacultyWorkloadRules> {
    return this.http.get<FacultyWorkloadRules>(this.baseUrl);
  }

  update(rules: FacultyWorkloadRules): Observable<FacultyWorkloadRules> {
    return this.http.put<FacultyWorkloadRules>(this.baseUrl, rules);
  }
}

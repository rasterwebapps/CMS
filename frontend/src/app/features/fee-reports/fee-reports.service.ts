import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  FeeCollectionSummary,
  FeeDemandReport,
  StudentFeeLedgerReport,
} from './fee-reports.model';

@Injectable({
  providedIn: 'root',
})
export class FeeReportsService {
  private readonly http = inject(HttpClient);

  getOutstandingDemands(termInstanceId: number): Observable<FeeDemandReport[]> {
    return this.http.get<FeeDemandReport[]>(
      `${environment.apiUrl}/fee-reports/outstanding`,
      { params: new HttpParams().set('termInstanceId', termInstanceId.toString()) },
    );
  }

  getCollectionSummary(termInstanceId: number): Observable<FeeCollectionSummary[]> {
    return this.http.get<FeeCollectionSummary[]>(
      `${environment.apiUrl}/fee-reports/collection-summary`,
      { params: new HttpParams().set('termInstanceId', termInstanceId.toString()) },
    );
  }

  getStudentLedger(studentId: number): Observable<StudentFeeLedgerReport> {
    return this.http.get<StudentFeeLedgerReport>(
      `${environment.apiUrl}/fee-reports/student-ledger`,
      { params: new HttpParams().set('studentId', studentId.toString()) },
    );
  }
}

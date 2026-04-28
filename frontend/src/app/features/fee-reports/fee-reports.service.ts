import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  FeeCollectionSummary,
  FeeDemandReport,
  StudentFeeLedgerReport,
  TermFeePaymentReport,
} from './fee-reports.model';

@Injectable({
  providedIn: 'root',
})
export class FeeReportsService {
  private readonly http = inject(HttpClient);

  private get baseUrl(): string {
    return environment.apiUrl.replace('/api/v1', '');
  }

  getOutstandingDemands(termInstanceId: number): Observable<FeeDemandReport[]> {
    return this.http.get<FeeDemandReport[]>(
      `${this.baseUrl}/api/fee-reports/outstanding`,
      { params: new HttpParams().set('termInstanceId', termInstanceId.toString()) },
    );
  }

  getCollectionSummary(termInstanceId: number): Observable<FeeCollectionSummary[]> {
    return this.http.get<FeeCollectionSummary[]>(
      `${this.baseUrl}/api/fee-reports/collection-summary`,
      { params: new HttpParams().set('termInstanceId', termInstanceId.toString()) },
    );
  }

  getLateFeeCollection(termInstanceId: number): Observable<TermFeePaymentReport[]> {
    return this.http.get<TermFeePaymentReport[]>(
      `${this.baseUrl}/api/fee-reports/late-fee-collection`,
      { params: new HttpParams().set('termInstanceId', termInstanceId.toString()) },
    );
  }

  getStudentLedger(studentId: number): Observable<StudentFeeLedgerReport> {
    return this.http.get<StudentFeeLedgerReport>(
      `${this.baseUrl}/api/fee-reports/student-ledger`,
      { params: new HttpParams().set('studentId', studentId.toString()) },
    );
  }
}

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  LibraryBook,
  LibraryBookRequest,
  BookStatus,
  LibraryBookImportValidationResult,
  LibraryBookImportExecuteResult,
  LibraryIssue,
  LibraryIssueRequest,
  LibraryReturnRequest,
  LibraryRenewRequest,
  IssueStatus,
  LibraryMemberType,
  LibraryPeriodical,
  LibraryPeriodicalRequest,
  JournalType,
  SubscriptionStatus,
  LibrarySetting,
  LibrarySettingUpdateRequest,
  LibraryFineDetail,
  LibraryFineActionRequest,
  FineStatus,
} from './library.model';

@Injectable({ providedIn: 'root' })
export class LibraryService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/library/books`;
  private readonly importUrl = `${this.baseUrl}/import`;

  // ── Book Catalogue ────────────────────────────────────────────

  getAll(status?: BookStatus): Observable<LibraryBook[]> {
    const params = status ? new HttpParams().set('status', status) : undefined;
    return this.http.get<LibraryBook[]>(this.baseUrl, { params });
  }

  getById(id: number): Observable<LibraryBook> {
    return this.http.get<LibraryBook>(`${this.baseUrl}/${id}`);
  }

  create(request: LibraryBookRequest): Observable<LibraryBook> {
    return this.http.post<LibraryBook>(this.baseUrl, request);
  }

  update(id: number, request: LibraryBookRequest): Observable<LibraryBook> {
    return this.http.put<LibraryBook>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  checkAccessionNumberExists(accessionNumber: string, excludeId?: number): Observable<{ exists: boolean }> {
    let params = new HttpParams().set('accessionNumber', accessionNumber);
    if (excludeId != null) params = params.set('excludeId', excludeId.toString());
    return this.http.get<{ exists: boolean }>(`${this.baseUrl}/accession-number-exists`, { params });
  }

  // ── Fines ─────────────────────────────────────────────────────

  getFines(status?: FineStatus): Observable<LibraryFineDetail[]> {
    const params = status ? new HttpParams().set('status', status) : undefined;
    return this.http.get<LibraryFineDetail[]>(`${environment.apiUrl}/library/fines`, { params });
  }

  waiveFine(id: number, request?: LibraryFineActionRequest): Observable<LibraryFineDetail> {
    return this.http.post<LibraryFineDetail>(`${environment.apiUrl}/library/fines/${id}/waive`, request ?? {});
  }

  collectFine(id: number, request?: LibraryFineActionRequest): Observable<LibraryFineDetail> {
    return this.http.post<LibraryFineDetail>(`${environment.apiUrl}/library/fines/${id}/collect`, request ?? {});
  }

  // ── Settings ─────────────────────────────────────────────────

  getSettings(): Observable<LibrarySetting[]> {
    return this.http.get<LibrarySetting[]>(`${environment.apiUrl}/library/settings`);
  }

  updateSetting(key: string, request: LibrarySettingUpdateRequest): Observable<LibrarySetting> {
    return this.http.put<LibrarySetting>(`${environment.apiUrl}/library/settings/${key}`, request);
  }

  // ── Reports ───────────────────────────────────────────────────

  getOverdueReport(): Observable<LibraryIssue[]> {
    return this.http.get<LibraryIssue[]>(`${environment.apiUrl}/library/reports/overdue`);
  }

  getFineReport(memberType?: LibraryMemberType): Observable<LibraryIssue[]> {
    const params = memberType ? new HttpParams().set('memberType', memberType) : undefined;
    return this.http.get<LibraryIssue[]>(`${environment.apiUrl}/library/reports/fines`, { params });
  }

  getIssueHistoryReport(memberType?: LibraryMemberType, status?: IssueStatus): Observable<LibraryIssue[]> {
    let params = new HttpParams();
    if (memberType) params = params.set('memberType', memberType);
    if (status)     params = params.set('status', status);
    return this.http.get<LibraryIssue[]>(`${environment.apiUrl}/library/reports/issue-history`, { params });
  }

  getAccessionRegisterReport(subjectCategory?: string, status?: BookStatus): Observable<LibraryBook[]> {
    let params = new HttpParams();
    if (subjectCategory) params = params.set('subjectCategory', subjectCategory);
    if (status)          params = params.set('status', status);
    return this.http.get<LibraryBook[]>(`${environment.apiUrl}/library/reports/accession-register`, { params });
  }

  // ── Circulation ───────────────────────────────────────────────

  getIssues(memberType?: LibraryMemberType, status?: IssueStatus): Observable<LibraryIssue[]> {
    let params = new HttpParams();
    if (memberType) params = params.set('memberType', memberType);
    if (status)     params = params.set('status', status);
    return this.http.get<LibraryIssue[]>(`${environment.apiUrl}/library/issues`, { params });
  }

  getMyIssues(): Observable<LibraryIssue[]> {
    return this.http.get<LibraryIssue[]>(`${environment.apiUrl}/library/issues/my`);
  }

  issueBook(request: LibraryIssueRequest): Observable<LibraryIssue> {
    return this.http.post<LibraryIssue>(`${environment.apiUrl}/library/issues`, request);
  }

  returnBook(issueId: number, request: LibraryReturnRequest): Observable<LibraryIssue> {
    return this.http.post<LibraryIssue>(`${environment.apiUrl}/library/issues/${issueId}/return`, request);
  }

  renewBook(issueId: number, request: LibraryRenewRequest): Observable<LibraryIssue> {
    return this.http.post<LibraryIssue>(`${environment.apiUrl}/library/issues/${issueId}/renew`, request);
  }

  // ── Periodicals ───────────────────────────────────────────────

  getPeriodicals(status?: SubscriptionStatus, journalType?: JournalType): Observable<LibraryPeriodical[]> {
    let params = new HttpParams();
    if (status)      params = params.set('status', status);
    if (journalType) params = params.set('journalType', journalType);
    return this.http.get<LibraryPeriodical[]>(`${environment.apiUrl}/library/periodicals`, { params });
  }

  getPeriodicalById(id: number): Observable<LibraryPeriodical> {
    return this.http.get<LibraryPeriodical>(`${environment.apiUrl}/library/periodicals/${id}`);
  }

  createPeriodical(request: LibraryPeriodicalRequest): Observable<LibraryPeriodical> {
    return this.http.post<LibraryPeriodical>(`${environment.apiUrl}/library/periodicals`, request);
  }

  updatePeriodical(id: number, request: LibraryPeriodicalRequest): Observable<LibraryPeriodical> {
    return this.http.put<LibraryPeriodical>(`${environment.apiUrl}/library/periodicals/${id}`, request);
  }

  deletePeriodical(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/library/periodicals/${id}`);
  }

  // ── Import ────────────────────────────────────────────────────

  downloadImportTemplate(): void {
    const a = document.createElement('a');
    a.href = `${this.importUrl}/template`;
    a.download = 'library_books_import_template.xlsx';
    a.click();
  }

  validateImport(file: File, skipErroredRows = true): Observable<LibraryBookImportValidationResult> {
    const form = new FormData();
    form.append('file', file);
    const params = new HttpParams().set('skipErroredRows', String(skipErroredRows));
    return this.http.post<LibraryBookImportValidationResult>(`${this.importUrl}/validate`, form, { params });
  }

  executeImport(file: File, skipErroredRows = true): Observable<LibraryBookImportExecuteResult> {
    const form = new FormData();
    form.append('file', file);
    const params = new HttpParams().set('skipErroredRows', String(skipErroredRows));
    return this.http.post<LibraryBookImportExecuteResult>(`${this.importUrl}/execute`, form, { params });
  }
}

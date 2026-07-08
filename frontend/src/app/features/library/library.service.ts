import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  Page,
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
  LibraryItemType,
  LibraryCirculationLookup,
  Library,
  LibraryRack,
  LibraryRackRequest,
  LibraryShelf,
  LibraryShelfRequest,
  LibraryBookTransferRequest,
  LibraryBookBulkTransferRequest,
  LibraryBookTransferResult,
  LibraryBookShelfTransfer,
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

  getBooksPage(p: { search?: string; status?: BookStatus | null; category?: string | null; rackId?: number | null; shelfId?: number | null; page?: number; size?: number; sort?: string; direction?: string }): Observable<Page<LibraryBook>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search)   params = params.set('search', p.search);
    if (p.status)   params = params.set('status', p.status);
    if (p.category) params = params.set('category', p.category);
    if (p.rackId)   params = params.set('rackId', p.rackId.toString());
    if (p.shelfId)  params = params.set('shelfId', p.shelfId.toString());
    if (p.sort)     params = params.set('sort', `${p.sort},${p.direction ?? 'asc'}`);
    return this.http.get<Page<LibraryBook>>(`${this.baseUrl}/page`, { params });
  }

  exportBooks(format: 'excel' | 'pdf', p: { search?: string; status?: BookStatus | null; category?: string | null; rackId?: number | null; shelfId?: number | null }): Observable<Blob> {
    let params = new HttpParams().set('format', format);
    if (p.search)   params = params.set('search', p.search);
    if (p.status)   params = params.set('status', p.status);
    if (p.category) params = params.set('category', p.category);
    if (p.rackId)   params = params.set('rackId', p.rackId.toString());
    if (p.shelfId)  params = params.set('shelfId', p.shelfId.toString());
    return this.http.get(`${this.baseUrl}/export`, { params, responseType: 'blob' });
  }

  checkAccessionNumberExists(accessionNumber: string, excludeId?: number): Observable<{ exists: boolean }> {
    let params = new HttpParams().set('accessionNumber', accessionNumber);
    if (excludeId != null) params = params.set('excludeId', excludeId.toString());
    return this.http.get<{ exists: boolean }>(`${this.baseUrl}/accession-number-exists`, { params });
  }

  // ── Book transfer ─────────────────────────────────────────────

  transferBook(id: number, request: LibraryBookTransferRequest): Observable<LibraryBookShelfTransfer> {
    return this.http.post<LibraryBookShelfTransfer>(`${this.baseUrl}/${id}/transfer`, request);
  }

  bulkTransferBooks(request: LibraryBookBulkTransferRequest): Observable<LibraryBookTransferResult> {
    return this.http.post<LibraryBookTransferResult>(`${this.baseUrl}/transfer/bulk`, request);
  }

  getBookTransferHistory(id: number): Observable<LibraryBookShelfTransfer[]> {
    return this.http.get<LibraryBookShelfTransfer[]>(`${this.baseUrl}/${id}/transfers`);
  }

  // ── Libraries ────────────────────────────────────────────────

  getLibraries(): Observable<Library[]> {
    return this.http.get<Library[]>(`${environment.apiUrl}/libraries`);
  }

  // ── Racks ────────────────────────────────────────────────────

  getRacks(libraryId?: number, activeOnly = false): Observable<LibraryRack[]> {
    let params = new HttpParams().set('activeOnly', String(activeOnly));
    if (libraryId != null) params = params.set('libraryId', libraryId.toString());
    return this.http.get<LibraryRack[]>(`${environment.apiUrl}/library/racks`, { params });
  }

  getRacksPage(p: { search?: string; libraryId?: number | null; page?: number; size?: number; sort?: string; direction?: string }): Observable<Page<LibraryRack>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search)    params = params.set('search', p.search);
    if (p.libraryId) params = params.set('libraryId', p.libraryId.toString());
    if (p.sort)      params = params.set('sort', `${p.sort},${p.direction ?? 'asc'}`);
    return this.http.get<Page<LibraryRack>>(`${environment.apiUrl}/library/racks/page`, { params });
  }

  getRackById(id: number): Observable<LibraryRack> {
    return this.http.get<LibraryRack>(`${environment.apiUrl}/library/racks/${id}`);
  }

  createRack(request: LibraryRackRequest): Observable<LibraryRack> {
    return this.http.post<LibraryRack>(`${environment.apiUrl}/library/racks`, request);
  }

  updateRack(id: number, request: LibraryRackRequest): Observable<LibraryRack> {
    return this.http.put<LibraryRack>(`${environment.apiUrl}/library/racks/${id}`, request);
  }

  deleteRack(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/library/racks/${id}`);
  }

  updateRackStatus(id: number, isActive: boolean): Observable<{ id: number; isActive: boolean; updatedAt: string }> {
    return this.http.patch<{ id: number; isActive: boolean; updatedAt: string }>(`${environment.apiUrl}/library/racks/${id}/status`, { isActive });
  }

  checkRackNameExists(value: string, libraryId: number, excludeId?: number): Observable<boolean> {
    let params = new HttpParams().set('value', value).set('libraryId', libraryId.toString());
    if (excludeId != null) params = params.set('excludeId', excludeId.toString());
    return this.http.get<boolean>(`${environment.apiUrl}/library/racks/name-exists`, { params });
  }

  checkRackCodeExists(value: string, libraryId: number, excludeId?: number): Observable<boolean> {
    let params = new HttpParams().set('value', value).set('libraryId', libraryId.toString());
    if (excludeId != null) params = params.set('excludeId', excludeId.toString());
    return this.http.get<boolean>(`${environment.apiUrl}/library/racks/code-exists`, { params });
  }

  // ── Shelves (tiers within a rack) ────────────────────────────

  getShelves(rackId?: number, libraryId?: number, activeOnly = false): Observable<LibraryShelf[]> {
    let params = new HttpParams().set('activeOnly', String(activeOnly));
    if (rackId != null)    params = params.set('rackId', rackId.toString());
    if (libraryId != null) params = params.set('libraryId', libraryId.toString());
    return this.http.get<LibraryShelf[]>(`${environment.apiUrl}/library/shelves`, { params });
  }

  getShelvesPage(p: { search?: string; rackId?: number | null; page?: number; size?: number; sort?: string; direction?: string }): Observable<Page<LibraryShelf>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search) params = params.set('search', p.search);
    if (p.rackId) params = params.set('rackId', p.rackId.toString());
    if (p.sort)   params = params.set('sort', `${p.sort},${p.direction ?? 'asc'}`);
    return this.http.get<Page<LibraryShelf>>(`${environment.apiUrl}/library/shelves/page`, { params });
  }

  getShelfById(id: number): Observable<LibraryShelf> {
    return this.http.get<LibraryShelf>(`${environment.apiUrl}/library/shelves/${id}`);
  }

  createShelf(request: LibraryShelfRequest): Observable<LibraryShelf> {
    return this.http.post<LibraryShelf>(`${environment.apiUrl}/library/shelves`, request);
  }

  updateShelf(id: number, request: LibraryShelfRequest): Observable<LibraryShelf> {
    return this.http.put<LibraryShelf>(`${environment.apiUrl}/library/shelves/${id}`, request);
  }

  deleteShelf(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/library/shelves/${id}`);
  }

  updateShelfStatus(id: number, isActive: boolean): Observable<{ id: number; isActive: boolean; updatedAt: string }> {
    return this.http.patch<{ id: number; isActive: boolean; updatedAt: string }>(`${environment.apiUrl}/library/shelves/${id}/status`, { isActive });
  }

  checkShelfNameExists(value: string, rackId: number, excludeId?: number): Observable<boolean> {
    let params = new HttpParams().set('value', value).set('rackId', rackId.toString());
    if (excludeId != null) params = params.set('excludeId', excludeId.toString());
    return this.http.get<boolean>(`${environment.apiUrl}/library/shelves/name-exists`, { params });
  }

  checkShelfCodeExists(value: string, rackId: number, excludeId?: number): Observable<boolean> {
    let params = new HttpParams().set('value', value).set('rackId', rackId.toString());
    if (excludeId != null) params = params.set('excludeId', excludeId.toString());
    return this.http.get<boolean>(`${environment.apiUrl}/library/shelves/code-exists`, { params });
  }

  // ── Fines ─────────────────────────────────────────────────────

  getFinesPage(p: { search?: string; status?: FineStatus | null; memberType?: LibraryMemberType | null; page?: number; size?: number; sort?: string; direction?: string }): Observable<Page<LibraryFineDetail>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search)     params = params.set('search', p.search);
    if (p.status)     params = params.set('status', p.status);
    if (p.memberType) params = params.set('memberType', p.memberType);
    if (p.sort)       params = params.set('sort', `${p.sort},${p.direction ?? 'desc'}`);
    return this.http.get<Page<LibraryFineDetail>>(`${environment.apiUrl}/library/fines/page`, { params });
  }

  exportFines(format: 'excel' | 'pdf', p: { search?: string; status?: FineStatus | null; memberType?: LibraryMemberType | null }): Observable<Blob> {
    let params = new HttpParams().set('format', format);
    if (p.search)     params = params.set('search', p.search);
    if (p.status)     params = params.set('status', p.status);
    if (p.memberType) params = params.set('memberType', p.memberType);
    return this.http.get(`${environment.apiUrl}/library/fines/export`, { params, responseType: 'blob' });
  }

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

  getIssuesPage(p: { search?: string; status?: IssueStatus | null; memberType?: LibraryMemberType | null; itemType?: LibraryItemType | null; page?: number; size?: number; sort?: string; direction?: string }): Observable<Page<LibraryIssue>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search)     params = params.set('search', p.search);
    if (p.status)     params = params.set('status', p.status);
    if (p.memberType) params = params.set('memberType', p.memberType);
    if (p.itemType)   params = params.set('itemType', p.itemType);
    if (p.sort)       params = params.set('sort', `${p.sort},${p.direction ?? 'desc'}`);
    return this.http.get<Page<LibraryIssue>>(`${environment.apiUrl}/library/issues/page`, { params });
  }

  exportIssues(format: 'excel' | 'pdf', p: { search?: string; status?: IssueStatus | null; memberType?: LibraryMemberType | null; itemType?: LibraryItemType | null }): Observable<Blob> {
    let params = new HttpParams().set('format', format);
    if (p.search)     params = params.set('search', p.search);
    if (p.status)     params = params.set('status', p.status);
    if (p.memberType) params = params.set('memberType', p.memberType);
    if (p.itemType)   params = params.set('itemType', p.itemType);
    return this.http.get(`${environment.apiUrl}/library/issues/export`, { params, responseType: 'blob' });
  }

  lookupByAccessionNumber(accessionNumber: string): Observable<LibraryCirculationLookup> {
    const params = new HttpParams().set('accessionNumber', accessionNumber);
    return this.http.get<LibraryCirculationLookup>(`${environment.apiUrl}/library/issues/lookup`, { params });
  }

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

  getPeriodicalsPage(p: { search?: string; subscriptionStatus?: SubscriptionStatus | null; journalType?: JournalType | null; page?: number; size?: number; sort?: string; direction?: string }): Observable<Page<LibraryPeriodical>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search)             params = params.set('search', p.search);
    if (p.subscriptionStatus) params = params.set('subscriptionStatus', p.subscriptionStatus);
    if (p.journalType)        params = params.set('journalType', p.journalType);
    if (p.sort)               params = params.set('sort', `${p.sort},${p.direction ?? 'asc'}`);
    return this.http.get<Page<LibraryPeriodical>>(`${environment.apiUrl}/library/periodicals/page`, { params });
  }

  exportPeriodicals(format: 'excel' | 'pdf', p: { search?: string; subscriptionStatus?: SubscriptionStatus | null; journalType?: JournalType | null }): Observable<Blob> {
    let params = new HttpParams().set('format', format);
    if (p.search)             params = params.set('search', p.search);
    if (p.subscriptionStatus) params = params.set('subscriptionStatus', p.subscriptionStatus);
    if (p.journalType)        params = params.set('journalType', p.journalType);
    return this.http.get(`${environment.apiUrl}/library/periodicals/export`, { params, responseType: 'blob' });
  }

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

  checkPeriodicalAccessionNumberExists(accessionNumber: string, excludeId?: number): Observable<{ exists: boolean }> {
    let params = new HttpParams().set('accessionNumber', accessionNumber);
    if (excludeId != null) params = params.set('excludeId', excludeId.toString());
    return this.http.get<{ exists: boolean }>(`${environment.apiUrl}/library/periodicals/accession-number-exists`, { params });
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

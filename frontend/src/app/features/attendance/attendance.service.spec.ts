import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';

import { AttendanceService } from './attendance.service';
import { environment } from '../../../environments';

describe('AttendanceService', () => {
  let service: AttendanceService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/attendance`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AttendanceService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  // OC-242: attendance-list's fix relies on getBySubject to build the request correctly with
  // and without a date filter -- both real, reachable call shapes from the fixed component.
  describe('getBySubject', () => {
    it('requests by subjectId only when no date is given', () => {
      service.getBySubject(5).subscribe();

      const req = httpMock.expectOne(`${baseUrl}?subjectId=5`);
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });

    it('includes the date filter when provided', () => {
      service.getBySubject(5, '2026-08-01').subscribe();

      const req = httpMock.expectOne(`${baseUrl}?subjectId=5&date=2026-08-01`);
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });
  });

  it('getMyAttendance calls the self-service endpoint', () => {
    service.getMyAttendance().subscribe();

    const req = httpMock.expectOne(`${baseUrl}/my`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('delete issues a DELETE to the record id', () => {
    service.delete(42).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/42`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});

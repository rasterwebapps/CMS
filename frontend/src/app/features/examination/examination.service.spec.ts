import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';

import { ExaminationService } from './examination.service';
import { environment } from '../../../environments';

describe('ExaminationService', () => {
  let service: ExaminationService;
  let httpMock: HttpTestingController;
  const resultUrl = `${environment.apiUrl}/exam-results`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ExaminationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('getMyResults calls the self-service endpoint', () => {
    service.getMyResults().subscribe();

    const req = httpMock.expectOne(`${resultUrl}/my`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  // OC-256: guardian ward-scoped exam results, resolved+re-validated server-side from studentId.
  it('getWardResults calls the ward-scoped endpoint with studentId', () => {
    service.getWardResults(45).subscribe();

    const req = httpMock.expectOne(`${resultUrl}/my-wards?studentId=45`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});

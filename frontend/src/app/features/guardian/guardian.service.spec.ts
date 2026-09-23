import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';

import { GuardianService } from './guardian.service';
import { environment } from '../../../environments';

describe('GuardianService', () => {
  let service: GuardianService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/guardians`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(GuardianService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('findAll requests the admin guardian list', () => {
    service.findAll().subscribe();

    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('create posts a new guardian', () => {
    const request = { firstName: 'Test', lastName: 'Guardian', email: 'g@test.com' };
    service.create(request).subscribe();

    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush({});
  });

  it('linkWard posts to the link endpoint with isPrimary as a query param', () => {
    service.linkWard(1, 10, true).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/1/wards/10?isPrimary=true`);
    expect(req.request.method).toBe('POST');
    req.flush(null);
  });

  it('unlinkWard issues a DELETE to the link endpoint', () => {
    service.unlinkWard(1, 10).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/1/wards/10`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('findByStudent requests guardians linked to a student', () => {
    service.findByStudent(45).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/students/45/guardians`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('myWards requests the caller\'s own wards', () => {
    service.myWards().subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/guardian/wards`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});

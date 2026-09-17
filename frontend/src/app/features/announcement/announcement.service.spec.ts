import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';

import { AnnouncementService } from './announcement.service';
import { environment } from '../../../environments';

describe('AnnouncementService', () => {
  let service: AnnouncementService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/announcements`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AnnouncementService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('getMyFeed requests the caller\'s own feed', () => {
    service.getMyFeed().subscribe();

    const req = httpMock.expectOne(`${baseUrl}/my`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('getMyUnreadCount requests the unread count', () => {
    service.getMyUnreadCount().subscribe();

    const req = httpMock.expectOne(`${baseUrl}/my/unread-count`);
    expect(req.request.method).toBe('GET');
    req.flush(0);
  });

  it('markRead posts to the read endpoint for the given id', () => {
    service.markRead(7).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/7/read`);
    expect(req.request.method).toBe('POST');
    req.flush(null);
  });
});

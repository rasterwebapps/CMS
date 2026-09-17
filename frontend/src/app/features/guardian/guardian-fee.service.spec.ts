import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';

import { GuardianFeeService } from './guardian-fee.service';
import { environment } from '../../../environments';

describe('GuardianFeeService', () => {
  let service: GuardianFeeService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/guardian/wards`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(GuardianFeeService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('getWardFeeSummary requests the ward-scoped fee summary', () => {
    service.getWardFeeSummary(45).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/45/fee-summary`);
    expect(req.request.method).toBe('GET');
    req.flush(null);
  });

  it('getWardReceipts requests the ward-scoped receipts', () => {
    service.getWardReceipts(45).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/45/fee-receipts`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('getWardPenalties requests the ward-scoped penalties', () => {
    service.getWardPenalties(45).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/45/fee-penalties`);
    expect(req.request.method).toBe('GET');
    req.flush(null);
  });

  it('createFeePaymentOrder posts the chosen amount', () => {
    service.createFeePaymentOrder(45, 2500).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/45/fee-payments/orders`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ amount: 2500 });
    req.flush({ razorpayOrderId: 'order_1', amount: 2500, currency: 'INR', razorpayKeyId: 'rzp_test' });
  });
});

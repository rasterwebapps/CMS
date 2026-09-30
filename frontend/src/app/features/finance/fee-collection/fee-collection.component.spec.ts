import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

import { FeeCollectionComponent, FeeEntry } from './fee-collection.component';
import { EnquiryService } from '../../enquiry/enquiry.service';
import { FinanceService } from '../finance.service';
import { ToastService } from '../../../core/toast/toast.service';
import { PermissionService } from '../../../core/permissions/permission.service';
import { TourService } from '../../../shared/tour/tour.service';
import { Enquiry } from '../../enquiry/enquiry.model';

// Regression coverage for the "enquiry stuck at PARTIALLY_PAID can't collect a balance" bug:
// an enquiry whose current/open installment is fully paid but whose next term hasn't opened
// yet has collectibleOutstanding === 0 even though it still owes money overall. The gate used
// to hide such rows from the collect-payment list entirely (and refuse to open the payment
// form for them even via deep link), making the ENQUIRY_FEE_COLLECT_ADVANCE permission's whole
// purpose — collecting ahead of an unopened term — unreachable in exactly the case it exists
// for. canCollectRow/canCollectEnquiryBalance now let such a row through when the caller holds
// that permission.
describe('FeeCollectionComponent — advance-eligible row gating', () => {
  let component: FeeCollectionComponent;
  let permissionService: { has: ReturnType<typeof vi.fn> };

  function setup(canCollectAdvance: boolean): void {
    permissionService = { has: vi.fn((code: string) => code === 'ENQUIRY_FEE_COLLECT_ADVANCE' && canCollectAdvance) };

    TestBed.configureTestingModule({
      imports: [FeeCollectionComponent],
      providers: [
        { provide: ActivatedRoute, useValue: { queryParamMap: of(null), snapshot: { queryParamMap: { get: () => null } } } },
        { provide: Router, useValue: { navigate: vi.fn() } },
        { provide: EnquiryService, useValue: {} },
        { provide: FinanceService, useValue: {} },
        { provide: ToastService, useValue: { info: vi.fn(), error: vi.fn() } },
        { provide: TourService, useValue: { register: vi.fn(), registerFlowMap: vi.fn() } },
        { provide: PermissionService, useValue: permissionService },
      ],
    });

    // ngOnInit (which does the real HTTP loads) is never triggered — no detectChanges() call —
    // so these mocks only need to exist for DI resolution, not behave like the real services.
    component = TestBed.createComponent(FeeCollectionComponent).componentInstance;
  }

  const internal = () => component as unknown as {
    canCollectRow(entry: FeeEntry): boolean;
    canCollectEnquiryBalance(enquiry: Enquiry): boolean;
  };

  const entry = (overrides: Partial<FeeEntry>): FeeEntry => ({
    type: 'ENQUIRY', id: 1, name: 'Test', rollNumber: null,
    programName: 'BSc Nursing', courseName: null,
    totalFee: 100000, totalPaid: 50000,
    totalOutstanding: 0, lifetimeOutstanding: 0, currentDue: 0,
    ...overrides,
  });

  const enquiry = (overrides: Partial<Enquiry>): Enquiry => ({
    status: 'PARTIALLY_PAID', finalizedNetFee: 100000, totalPaidAmount: 50000, collectibleOutstanding: 0,
    ...overrides,
  } as unknown as Enquiry);

  it('is collectible when something is due right now, regardless of the advance permission', () => {
    setup(false);
    expect(internal().canCollectRow(entry({ totalOutstanding: 20000, lifetimeOutstanding: 50000 }))).toBe(true);
  });

  it('blocks a fully-settled row (nothing due now, no remaining lifetime balance)', () => {
    setup(true);
    expect(internal().canCollectRow(entry({ totalOutstanding: 0, lifetimeOutstanding: 0 }))).toBe(false);
  });

  it('lets an enquiry with nothing currently due through when the caller can collect in advance', () => {
    setup(true);
    expect(internal().canCollectRow(entry({ type: 'ENQUIRY', totalOutstanding: 0, lifetimeOutstanding: 50000 }))).toBe(true);
  });

  it('hides that same enquiry from a caller without the advance permission', () => {
    setup(false);
    expect(internal().canCollectRow(entry({ type: 'ENQUIRY', totalOutstanding: 0, lifetimeOutstanding: 50000 }))).toBe(false);
  });

  it('never extends advance eligibility to a STUDENT row (advance mode is enquiry-only)', () => {
    setup(true);
    expect(internal().canCollectRow(entry({ type: 'STUDENT', totalOutstanding: 0, lifetimeOutstanding: 50000 }))).toBe(false);
  });

  it('keeps a PARTIALLY_PAID enquiry with an unopened next term visible to an advance-eligible user', () => {
    setup(true);
    // Mirrors the reported production case: first installment fully paid (collectibleOutstanding
    // 0 because the next term hasn't opened yet), but finalizedNetFee - totalPaidAmount > 0.
    const e = enquiry({ status: 'PARTIALLY_PAID', finalizedNetFee: 950000, totalPaidAmount: 50000, collectibleOutstanding: 0 });
    expect(internal().canCollectEnquiryBalance(e)).toBe(true);
  });

  it('excludes that same enquiry for a user without the advance permission', () => {
    setup(false);
    const e = enquiry({ status: 'PARTIALLY_PAID', finalizedNetFee: 950000, totalPaidAmount: 50000, collectibleOutstanding: 0 });
    expect(internal().canCollectEnquiryBalance(e)).toBe(false);
  });

  it('still excludes NOT_INTERESTED/CANCELLED enquiries even with the advance permission', () => {
    setup(true);
    const e = enquiry({ status: 'NOT_INTERESTED', finalizedNetFee: 950000, totalPaidAmount: 0, collectibleOutstanding: 0 });
    expect(internal().canCollectEnquiryBalance(e)).toBe(false);
  });

  it('still excludes an enquiry whose fees were never finalized', () => {
    setup(true);
    const e = enquiry({ status: 'INTERESTED', finalizedNetFee: null, totalPaidAmount: 0, collectibleOutstanding: null });
    expect(internal().canCollectEnquiryBalance(e)).toBe(false);
  });
});

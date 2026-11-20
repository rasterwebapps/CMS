import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

import { EnquiryFormComponent } from './enquiry-form.component';
import { EnquiryService } from '../enquiry.service';
import { AgentService } from '../../agent/agent.service';
import { ReferralTypeService } from '../../referral-type/referral-type.service';
import { StaffReferrerService } from '../../staff-referrer/staff-referrer.service';
import { ToastService } from '../../../core/toast/toast.service';
import { CounsellingStateConfirmDialogComponent } from '../../../shared/counselling-state-confirm-dialog/counselling-state-confirm-dialog.component';

describe('EnquiryFormComponent (Counselling State Advisory & Confirm Dialog)', () => {
  let enquiryServiceMock: {
    getEnquiryById: ReturnType<typeof vi.fn>;
    createEnquiry: ReturnType<typeof vi.fn>;
    updateEnquiry: ReturnType<typeof vi.fn>;
  };
  let dialogOpenSpy: ReturnType<typeof vi.fn>;

  async function setup(dialogConfirm = true): Promise<ComponentFixture<EnquiryFormComponent>> {
    enquiryServiceMock = {
      getEnquiryById: vi.fn(),
      createEnquiry: vi.fn().mockReturnValue(of({ id: 99 })),
      updateEnquiry: vi.fn().mockReturnValue(of({ id: 99 })),
    };
    dialogOpenSpy = vi.fn().mockReturnValue({
      afterClosed: () => of(dialogConfirm),
    });

    await TestBed.configureTestingModule({
      imports: [EnquiryFormComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({}),
              queryParams: {},
            },
          },
        },
        { provide: EnquiryService, useValue: enquiryServiceMock },
        { provide: AgentService, useValue: { getActiveAgents: () => of([]) } },
        {
          provide: ReferralTypeService,
          useValue: {
            getActiveReferralTypes: () =>
              of([{ id: 1, name: 'Walk-in', code: 'WALK_IN', hasCommission: false }]),
          },
        },
        { provide: StaffReferrerService, useValue: { getActive: () => of([]) } },
        { provide: ToastService, useValue: { success: vi.fn(), error: vi.fn() } },
        { provide: MatDialog, useValue: { open: dialogOpenSpy } },
      ],
    })
      .overrideComponent(EnquiryFormComponent, {
        add: {
          providers: [{ provide: MatDialog, useValue: { open: dialogOpenSpy } }],
        },
      })
      .compileComponents();

    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);

    const fixture = TestBed.createComponent(EnquiryFormComponent);
    fixture.detectChanges();

    const httpMock = TestBed.inject(HttpTestingController);
    httpMock.match((r) => r.url.endsWith('/programs')).forEach((r) =>
      r.flush([
        {
          id: 1,
          name: 'B.Sc Nursing',
          code: 'BSCN',
          durationYears: 4,
          specialities: [],
          minimumAgeYears: null,
          ageCutoffDay: null,
          ageCutoffMonth: null,
        },
      ]),
    );
    httpMock.match((r) => r.url.endsWith('/fee-states')).forEach((r) =>
      r.flush([
        { id: 1, name: 'Tamil Nadu', code: 'TAMIL_NADU', isDefault: true, isFallback: false, sortOrder: 1 },
        { id: 2, name: 'Other State', code: 'OTHER_STATE', isDefault: false, isFallback: true, sortOrder: 2 },
      ]),
    );
    httpMock.match(() => true).forEach((r) => r.flush([]));
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => {
    TestBed.resetTestingModule();
  });

  function fillValidEnquiry(comp: EnquiryFormComponent, state: string, quota: 'MANAGEMENT' | 'COUNSELLING'): void {
    const internal = comp as unknown as {
      form: EnquiryFormComponent['form'];
      totalFees: { set: (v: number) => void };
      feeNotFound: { set: (v: boolean) => void };
    };
    internal.form.patchValue({
      name: 'Anjali Menon',
      phone: '9876543210',
      dateOfBirth: '2007-05-12',
      gender: 'FEMALE',
      country: 1,
      state,
      district: 'Ernakulam',
      programId: 1,
      admissionQuota: quota,
      studentType: 'DAY_SCHOLAR',
      enquiryDate: '2026-10-14',
      referralTypeId: 1,
    });
    const httpMock = TestBed.inject(HttpTestingController);
    httpMock.match(() => true).forEach((r) => r.flush([]));
    internal.feeNotFound.set(false);
    internal.totalFees.set(85000);
  }

  it('shows inline state advisory banner and prompts confirmation dialog when saving COUNSELLING enquiry with Kerala state', async () => {
    const fixture = await setup(true);
    fillValidEnquiry(fixture.componentInstance, 'Kerala', 'COUNSELLING');
    fixture.detectChanges();

    const banners = (fixture.nativeElement as HTMLElement).querySelectorAll('.state-advisory-banner');
    expect(banners.length).toBe(2);
    expect(banners[0].textContent).toContain('Kerala');

    (fixture.componentInstance as unknown as { onSubmit: () => void }).onSubmit();

    expect(dialogOpenSpy).toHaveBeenCalledWith(
      CounsellingStateConfirmDialogComponent,
      expect.objectContaining({
        data: expect.objectContaining({
          personName: 'Anjali Menon',
          stateName: 'Kerala',
          categoryValueLabel: 'Counselling Quota',
        }),
      }),
    );
    expect(enquiryServiceMock.createEnquiry).toHaveBeenCalledTimes(1);
  });

  it('aborts enquiry save when user cancels the non-Tamil Nadu counselling confirmation dialog', async () => {
    const fixture = await setup(false);
    fillValidEnquiry(fixture.componentInstance, 'Kerala', 'COUNSELLING');
    fixture.detectChanges();

    (fixture.componentInstance as unknown as { onSubmit: () => void }).onSubmit();

    expect(dialogOpenSpy).toHaveBeenCalledTimes(1);
    expect(enquiryServiceMock.createEnquiry).not.toHaveBeenCalled();
  });

  it('saves directly without warning banner or dialog when state is Tamil Nadu', async () => {
    const fixture = await setup(true);
    fillValidEnquiry(fixture.componentInstance, 'Tamil Nadu', 'COUNSELLING');
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('.state-advisory-banner')).toBeNull();

    (fixture.componentInstance as unknown as { onSubmit: () => void }).onSubmit();

    expect(dialogOpenSpy).not.toHaveBeenCalled();
    expect(enquiryServiceMock.createEnquiry).toHaveBeenCalledTimes(1);
  });
});

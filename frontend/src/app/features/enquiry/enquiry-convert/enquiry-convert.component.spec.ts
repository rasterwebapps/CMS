import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

import { EnquiryConvertComponent } from './enquiry-convert.component';
import { EnquiryService } from '../enquiry.service';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { CommunityService } from '../../community/community.service';
import { BloodGroupService } from '../../blood-group/blood-group.service';
import { IndiaLocationService } from '../../india-location/india-location.service';
import { AdmissionService } from '../../admission/admission.service';
import { SettingsService } from '../../settings/settings.service';
import { ToastService } from '../../../core/toast/toast.service';
import { CounsellingStateConfirmDialogComponent } from '../../../shared/counselling-state-confirm-dialog/counselling-state-confirm-dialog.component';

describe('EnquiryConvertComponent (Counselling State Advisory & Confirm Dialog)', () => {
  let enquiryServiceMock: {
    getEnquiryById: ReturnType<typeof vi.fn>;
    getConversionPrefill: ReturnType<typeof vi.fn>;
    getDocumentVerificationStatus: ReturnType<typeof vi.fn>;
    convertEnquiry: ReturnType<typeof vi.fn>;
  };
  let dialogOpenSpy: ReturnType<typeof vi.fn>;

  async function setup(
    quota: 'MANAGEMENT' | 'COUNSELLING',
    initialState: string,
    dialogConfirm = true,
  ): Promise<ComponentFixture<EnquiryConvertComponent>> {
    enquiryServiceMock = {
      getEnquiryById: vi.fn().mockReturnValue(
        of({
          id: 55,
          name: 'Anjali Menon',
          email: 'anjali@college.edu',
          phone: '9876543210',
          status: 'DOCUMENTS_VERIFIED',
          programId: 1,
          programName: 'B.Sc Nursing',
          courseId: 1,
          courseName: 'Nursing',
          admissionQuota: quota,
          studentType: 'DAY_SCHOLAR',
        }),
      ),
      getConversionPrefill: vi.fn().mockReturnValue(
        of({
          firstName: 'Anjali',
          lastName: 'Menon',
          email: 'anjali@college.edu',
          phone: '9876543210',
          dateOfBirth: '2007-05-12',
          gender: 'FEMALE',
          countryId: 1,
          state: initialState,
          district: 'Ernakulam',
          suggestedApplicationDate: '2026-10-14',
          suggestedAcademicYearFrom: 2026,
        }),
      ),
      getDocumentVerificationStatus: vi.fn().mockReturnValue(
        of({
          allUploaded: true,
          allVerified: true,
          notUploadedDocumentTypes: [],
          unverifiedDocumentTypes: [],
        }),
      ),
      convertEnquiry: vi.fn().mockReturnValue(
        of({
          id: 55,
          name: 'Anjali Menon',
          admissionNumber: '2026650099',
          convertedStudentId: 500,
          admissionQuota: quota,
        }),
      ),
    };
    dialogOpenSpy = vi.fn().mockReturnValue({
      afterClosed: () => of(dialogConfirm),
    });

    await TestBed.configureTestingModule({
      imports: [EnquiryConvertComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: convertToParamMap({ id: '55' }) },
          },
        },
        { provide: EnquiryService, useValue: enquiryServiceMock },
        {
          provide: AcademicYearService,
          useValue: {
            getAllAcademicYears: () =>
              of([
                {
                  id: 10,
                  name: '2026-2027',
                  startDate: '2026-06-01',
                  endDate: '2027-05-31',
                  isCurrent: true,
                },
              ]),
            getSeatAvailability: () =>
              of({ closed: false, full: false, overManagementQuota: false, filled: 5, total: 60 }),
          },
        },
        { provide: CommunityService, useValue: { getActiveCommunities: () => of([{ id: 1, code: 'BC', name: 'BC' }]) } },
        { provide: BloodGroupService, useValue: { getActiveBloodGroups: () => of([{ id: 1, code: 'O_POS', name: 'O+' }]) } },
        {
          provide: IndiaLocationService,
          useValue: {
            getCountries: () => of([{ id: 1, name: 'India', isoCode: 'IN' }]),
            getStatesByCountry: () =>
              of([
                { id: 1, name: 'Tamil Nadu', code: 'TN' },
                { id: 2, name: 'Kerala', code: 'KL' },
              ]),
            getDistricts: () => of([{ id: 1, name: 'Ernakulam' }]),
          },
        },
        {
          provide: AdmissionService,
          useValue: {
            getByStudent: () => of({ id: 1 }),
            getDocuments: () => of([]),
            getDocumentChecklist: () => of({ mandatory: {}, optional: {} }),
          },
        },
        { provide: SettingsService, useValue: { getAll: () => of([]) } },
        { provide: ToastService, useValue: { success: vi.fn(), warning: vi.fn(), error: vi.fn() } },
        { provide: MatDialog, useValue: { open: dialogOpenSpy } },
      ],
    })
      .overrideComponent(EnquiryConvertComponent, {
        add: {
          providers: [{ provide: MatDialog, useValue: { open: dialogOpenSpy } }],
        },
      })
      .compileComponents();

    const fixture = TestBed.createComponent(EnquiryConvertComponent);
    fixture.detectChanges();
    return fixture;
  }

  function fillConvertForm(comp: EnquiryConvertComponent, stateName: string): void {
    const form = (comp as unknown as { form: EnquiryConvertComponent['form'] }).form;
    form.patchValue({
      firstName: 'Anjali',
      lastName: 'Menon',
      email: 'anjali@college.edu',
      phone: '9876543210',
      admissionDate: '2026-10-14',
      applicationDate: '2026-10-14',
      dateOfBirth: '2007-05-12',
      gender: 'FEMALE',
      aadharNumber: '123456789012',
      nationality: 'Indian',
      religion: 'Hindu',
      communityCategory: 'BC',
      caste: 'Nair',
      bloodGroup: 'O_POS',
      fatherName: 'Suresh Menon',
      fatherPhone: '9876543211',
      fatherEmail: 'suresh@example.com',
      motherName: 'Lakshmi Menon',
      motherPhone: '9876543212',
      motherEmail: 'lakshmi@example.com',
      address: {
        country: 1,
        postalAddress: '12 Green Villa',
        street: 'MG Road',
        city: 'Kochi',
        state: stateName,
        district: 'Ernakulam',
        pincode: '682001',
      },
      declarationPlace: 'Salem',
      declarationDate: '2026-10-14',
      parentConsentGiven: true,
      applicantConsentGiven: true,
    });
  }

  beforeEach(() => {
    TestBed.resetTestingModule();
  });

  it('displays state advisory banner and prompts confirmation dialog when converting COUNSELLING enquiry with Kerala address state', async () => {
    const fixture = await setup('COUNSELLING', 'Tamil Nadu', true);
    // Simulate staff changing address state from Tamil Nadu to Kerala on Create Admission screen
    fillConvertForm(fixture.componentInstance, 'Kerala');
    fixture.detectChanges();

    const banners = (fixture.nativeElement as HTMLElement).querySelectorAll('.state-advisory-banner');
    expect(banners.length).toBeGreaterThan(0);
    expect(banners[0].textContent).toContain('Kerala');

    (fixture.componentInstance as unknown as { onSubmit: () => void }).onSubmit();

    expect(dialogOpenSpy).toHaveBeenCalledWith(
      CounsellingStateConfirmDialogComponent,
      expect.objectContaining({
        data: expect.objectContaining({
          personName: 'Anjali Menon',
          stateName: 'Kerala',
          confirmText: 'Confirm & Create Admission',
        }),
      }),
    );
    expect(enquiryServiceMock.convertEnquiry).toHaveBeenCalledTimes(1);
  });

  it('does not convert when user cancels the non-Tamil Nadu counselling confirmation dialog', async () => {
    const fixture = await setup('COUNSELLING', 'Kerala', false);
    fillConvertForm(fixture.componentInstance, 'Kerala');
    fixture.detectChanges();

    (fixture.componentInstance as unknown as { onSubmit: () => void }).onSubmit();

    expect(dialogOpenSpy).toHaveBeenCalledTimes(1);
    expect(enquiryServiceMock.convertEnquiry).not.toHaveBeenCalled();
  });
});

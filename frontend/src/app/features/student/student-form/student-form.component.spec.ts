import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

import { StudentFormComponent } from './student-form.component';
import { StudentService } from '../student.service';
import { CommunityService } from '../../community/community.service';
import { BloodGroupService } from '../../blood-group/blood-group.service';
import { ToastService } from '../../../core/toast/toast.service';
import { Student } from '../student.model';
import { AdmissionCategoryConfirmDialogComponent } from '../admission-category-confirm-dialog/admission-category-confirm-dialog.component';
import { CounsellingStateConfirmDialogComponent } from '../../../shared/counselling-state-confirm-dialog/counselling-state-confirm-dialog.component';

describe('StudentFormComponent (Admission Category Enquiry vs Final & State Advisory)', () => {
  let studentServiceMock: {
    getById: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
  };
  let dialogOpenSpy: ReturnType<typeof vi.fn>;

  function buildStudent(overrides: Partial<Student> = {}): Student {
    return {
      id: 397,
      rollNumber: '959652026003',
      admissionNumber: '2026650001',
      firstName: 'AGALYA',
      lastName: 'R',
      fullName: 'AGALYA R',
      email: 'agalya@college.edu',
      phone: '9876543210',
      programId: 1,
      programName: 'B.Sc Nursing',
      yearOfStudy: 1,
      admissionDate: '2026-10-14',
      status: 'ACTIVE',
      admissionCategory: 'MANAGEMENT',
      finalAdmissionCategory: 'MANAGEMENT',
      canEditFinalAdmissionCategory: true,
      gender: 'FEMALE',
      countryId: 1,
      state: 'Tamil Nadu',
      district: 'Salem',
      createdAt: '2026-10-14T00:00:00Z',
      updatedAt: '2026-10-14T00:00:00Z',
      ...overrides,
    };
  }

  async function setup(
    routeId?: string,
    student?: Student,
    dialogReturnValue: unknown = 'Allotted seat in TN Govt Counselling Round 2',
  ): Promise<ComponentFixture<StudentFormComponent>> {
    studentServiceMock = {
      getById: vi.fn().mockReturnValue(of(student ?? buildStudent())),
      create: vi.fn().mockReturnValue(of(buildStudent())),
      update: vi.fn().mockReturnValue(of(buildStudent())),
    };
    dialogOpenSpy = vi.fn().mockReturnValue({
      afterClosed: () => of(dialogReturnValue),
    });

    await TestBed.configureTestingModule({
      imports: [StudentFormComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap(routeId ? { id: routeId } : {}),
              queryParams: {},
            },
          },
        },
        { provide: StudentService, useValue: studentServiceMock },
        { provide: CommunityService, useValue: { getActiveCommunities: () => of([]) } },
        { provide: BloodGroupService, useValue: { getActiveBloodGroups: () => of([]) } },
        { provide: ToastService, useValue: { success: vi.fn(), error: vi.fn() } },
        { provide: MatDialog, useValue: { open: dialogOpenSpy } },
      ],
    })
      .overrideComponent(StudentFormComponent, {
        add: {
          providers: [{ provide: MatDialog, useValue: { open: dialogOpenSpy } }],
        },
      })
      .compileComponents();

    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);

    const fixture = TestBed.createComponent(StudentFormComponent);
    fixture.detectChanges();

    const httpMock = TestBed.inject(HttpTestingController);
    httpMock.match(() => true).forEach((req) => req.flush([]));
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => {
    TestBed.resetTestingModule();
  });

  it('locks Admission Category (Enquiry) and enables Admission Category (Final) for 1st-term student, prompting confirmation reason on change', async () => {
    const fixture = await setup('397', buildStudent({ yearOfStudy: 1, canEditFinalAdmissionCategory: true }));
    const comp = fixture.componentInstance as unknown as {
      form: StudentFormComponent['form'];
      onSubmit: () => void;
    };

    expect(comp.form.get('admissionCategory')?.disabled).toBe(true);
    expect(comp.form.get('finalAdmissionCategory')?.disabled).toBe(false);

    comp.form.get('finalAdmissionCategory')?.setValue('MANAGEMENT_TO_COUNSELLING');
    fixture.detectChanges();

    // State is Tamil Nadu -> no state advisory banner
    expect((fixture.nativeElement as HTMLElement).querySelector('.state-advisory-banner')).toBeNull();

    comp.onSubmit();

    expect(dialogOpenSpy).toHaveBeenCalledTimes(1);
    expect(dialogOpenSpy).toHaveBeenCalledWith(
      AdmissionCategoryConfirmDialogComponent,
      expect.objectContaining({
        data: expect.objectContaining({
          newFinalCategory: 'MANAGEMENT_TO_COUNSELLING',
          studentState: 'Tamil Nadu',
        }),
      }),
    );
    expect(studentServiceMock.update).toHaveBeenCalledWith(
      397,
      expect.objectContaining({
        admissionCategory: 'MANAGEMENT',
        finalAdmissionCategory: 'MANAGEMENT_TO_COUNSELLING',
        admissionCategoryChangeReason: 'Allotted seat in TN Govt Counselling Round 2',
      }),
    );
  });

  it('shows inline state advisory banner and passes Kerala state to confirm dialog when switching Kerala student to MANAGEMENT_TO_COUNSELLING or COUNSELLING', async () => {
    const fixture = await setup(
      '401',
      buildStudent({ id: 401, firstName: 'ANJALI', lastName: 'MENON', state: 'Kerala' }),
    );
    const comp = fixture.componentInstance as unknown as {
      form: StudentFormComponent['form'];
      onSubmit: () => void;
    };

    // Initially MANAGEMENT + Kerala -> no state advisory banner
    expect((fixture.nativeElement as HTMLElement).querySelector('.state-advisory-banner')).toBeNull();

    // Switch to MANAGEMENT_TO_COUNSELLING -> inline state advisory banner appears immediately
    comp.form.get('finalAdmissionCategory')?.setValue('MANAGEMENT_TO_COUNSELLING');
    fixture.detectChanges();

    const banners = (fixture.nativeElement as HTMLElement).querySelectorAll('.state-advisory-banner');
    expect(banners.length).toBeGreaterThan(0);
    expect(banners[0].textContent).toContain('Kerala');

    comp.onSubmit();
    expect(dialogOpenSpy).toHaveBeenCalledWith(
      AdmissionCategoryConfirmDialogComponent,
      expect.objectContaining({
        data: expect.objectContaining({
          studentName: 'ANJALI MENON',
          newFinalCategory: 'MANAGEMENT_TO_COUNSELLING',
          studentState: 'Kerala',
        }),
      }),
    );
  });

  it('prompts CounsellingStateConfirmDialogComponent on Create Student when COUNSELLING is chosen with non-Tamil Nadu state', async () => {
    const fixture = await setup(undefined, undefined, true);
    const comp = fixture.componentInstance as unknown as {
      form: StudentFormComponent['form'];
      onSubmit: () => void;
    };

    comp.form.patchValue({
      rollNumber: 'CS2026099',
      firstName: 'Rahul',
      lastName: 'Nair',
      email: 'rahul@college.edu',
      programId: 1,
      yearOfStudy: 1,
      admissionDate: '2026-10-14',
      status: 'ACTIVE',
      admissionCategory: 'COUNSELLING',
      finalAdmissionCategory: 'COUNSELLING',
      gender: 'MALE',
      state: 'Kerala',
    });
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('.state-advisory-banner')).not.toBeNull();

    comp.onSubmit();

    expect(dialogOpenSpy).toHaveBeenCalledWith(
      CounsellingStateConfirmDialogComponent,
      expect.objectContaining({
        data: expect.objectContaining({
          personName: 'Rahul Nair',
          stateName: 'Kerala',
          categoryValueLabel: 'Counselling',
        }),
      }),
    );
    expect(studentServiceMock.create).toHaveBeenCalledTimes(1);
  });

  it('disables both Admission Category (Enquiry) and Admission Category (Final) for 2nd-year+ students', async () => {
    const fixture = await setup(
      '2',
      buildStudent({
        id: 2,
        admissionNumber: '2025650001',
        yearOfStudy: 2,
        canEditFinalAdmissionCategory: false,
      }),
    );
    const comp = fixture.componentInstance as unknown as { form: StudentFormComponent['form'] };

    expect(comp.form.get('admissionCategory')?.disabled).toBe(true);
    expect(comp.form.get('finalAdmissionCategory')?.disabled).toBe(true);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'Only 1st term / 1st semester student records can be modified.',
    );
  });

  it('auto-syncs Admission Category (Final) when Admission Category (Enquiry) is chosen in Add mode', async () => {
    const fixture = await setup();
    const comp = fixture.componentInstance as unknown as { form: StudentFormComponent['form'] };

    expect(comp.form.get('admissionCategory')?.disabled).toBe(false);
    expect(comp.form.get('finalAdmissionCategory')?.disabled).toBe(false);

    comp.form.get('admissionCategory')?.setValue('COUNSELLING');
    expect(comp.form.get('finalAdmissionCategory')?.value).toBe('COUNSELLING');
  });
});

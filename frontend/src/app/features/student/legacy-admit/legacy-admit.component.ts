import { Component, DestroyRef, inject, OnInit, signal, computed } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { HttpClient } from '@angular/common/http';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { environment } from '../../../../environments/environment';
import { ProgramService } from '../../program/program.service';
import { Program } from '../../program/program.model';
import { CourseService } from '../../course/course.service';
import { Course } from '../../course/course.model';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear } from '../../academic-year/academic-year.model';
import { CommunityService } from '../../community/community.service';
import { Community } from '../../community/community.model';
import { BloodGroupService } from '../../blood-group/blood-group.service';
import { BloodGroup } from '../../blood-group/blood-group.model';
import { ReferralTypeService } from '../../referral-type/referral-type.service';
import { ReferralType } from '../../referral-type/referral-type.model';
import { AgentService } from '../../agent/agent.service';
import { Agent } from '../../agent/agent.model';
import { StudentService } from '../student.service';
import { Student } from '../student.model';
import { FacultyService } from '../../faculty/faculty.service';
import { Faculty } from '../../faculty/faculty.model';
import { FinanceService } from '../../finance/finance.service';
import { FeeState } from '../../finance/finance.model';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { CmsCountryStateDistrictSelectorComponent } from '../../../shared/country-state-district-selector/country-state-district-selector.component';

interface LegacyAdmitResponse {
  studentId: number;
  admissionNumber: string;
  studentName: string;
  rollNumber: string;
  enquiryId: number;
  yearsWithFeeRecords: number;
  paymentRowsCreated: number;
  totalHistoricalPaid: number;
}

@Component({
  selector: 'app-legacy-admit',
  standalone: true,
  imports: [
    ReactiveFormsModule, RouterLink,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule,
    InrPipe, CmsCountryStateDistrictSelectorComponent,
  ],
  templateUrl: './legacy-admit.component.html',
  styleUrl: './legacy-admit.component.scss',
})
export class LegacyAdmitComponent implements OnInit {

  private readonly fb              = inject(FormBuilder);
  private readonly router          = inject(Router);
  private readonly http            = inject(HttpClient);
  private readonly destroyRef      = inject(DestroyRef);
  private readonly programSvc      = inject(ProgramService);
  private readonly courseSvc       = inject(CourseService);
  private readonly academicYearSvc = inject(AcademicYearService);
  private readonly communitySvc    = inject(CommunityService);
  private readonly bloodGroupSvc   = inject(BloodGroupService);
  private readonly referralSvc     = inject(ReferralTypeService);
  private readonly agentSvc        = inject(AgentService);
  private readonly studentSvc      = inject(StudentService);
  private readonly facultySvc      = inject(FacultyService);
  private readonly financeSvc      = inject(FinanceService);

  protected readonly loading             = signal(true);
  protected readonly saving              = signal(false);
  protected readonly saveError           = signal<string | null>(null);
  protected readonly successState        = signal<LegacyAdmitResponse | null>(null);
  protected readonly feeGuidelineLoading = signal(false);
  protected readonly programs      = signal<Program[]>([]);
  protected readonly courses       = signal<Course[]>([]);
  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly communities   = signal<Community[]>([]);
  protected readonly bloodGroups   = signal<BloodGroup[]>([]);
  protected readonly referralTypes = signal<ReferralType[]>([]);
  protected readonly agents        = signal<Agent[]>([]);
  protected readonly allStudents   = signal<Student[]>([]);
  protected readonly allFaculty    = signal<Faculty[]>([]);
  protected readonly feeStates     = signal<FeeState[]>([]);

  protected readonly selectedProgram = signal<Program | null>(null);
  protected readonly isYearly        = computed(() => this.selectedProgram()?.assessmentPattern === 'YEARLY');
  protected readonly yearCount       = computed(() => this.selectedProgram()?.durationYears ?? 0);

  protected readonly paymentRows    = signal<FormGroup[]>([]);
  protected readonly paymentSummary = signal<Map<number, number>>(new Map());

  protected readonly PAYMENT_MODES = ['CASH', 'DD', 'CHEQUE', 'BANK_TRANSFER', 'UPI', 'ONLINE'];

  protected readonly form: FormGroup = this.fb.group({
    // Student identity
    firstName:    ['', Validators.required],
    lastName:     ['', Validators.required],
    email:        ['', [Validators.required, Validators.email]],
    phone:        ['', Validators.required],
    rollNumber:   ['', [Validators.required, Validators.maxLength(50)]],
    universityRegistrationNumber: ['', [Validators.required, Validators.maxLength(50)]],
    umisNumber:   ['', [Validators.maxLength(50)]],

    // Admission context
    programId:             [null as number | null, Validators.required],
    courseId:              [null as number | null],
    joiningAcademicYearId: [null as number | null, Validators.required],
    admissionDate:         ['', Validators.required],
    applicationDate:       ['', Validators.required],
    yearOfStudy:           [1, [Validators.required, Validators.min(1)]],
    admissionQuota:        ['MANAGEMENT'],
    studentType:           ['DAY_SCHOLAR'],
    feeStateId:            [null as number | null],

    // Personal
    dateOfBirth:       ['', Validators.required],
    gender:            ['FEMALE', Validators.required],
    aadharNumber:      ['', Validators.required],
    nationality:       ['Indian', Validators.required],
    religion:          ['', Validators.required],
    communityCategory: ['', Validators.required],
    caste:             ['', Validators.required],
    bloodGroup:        ['', Validators.required],
    physicalDisability: [false],

    // Family
    fatherName:   ['', Validators.required],
    fatherPhone:  ['', Validators.required],
    fatherEmail:  ['', [Validators.required, Validators.email]],
    motherName:   ['', Validators.required],
    motherPhone:  ['', Validators.required],
    motherEmail:  ['', [Validators.required, Validators.email]],
    parentMobile: ['', Validators.required],

    // Address
    address: this.fb.group({
      country:       [null as number | null],
      postalAddress: ['', Validators.required],
      street:        ['', Validators.required],
      city:          ['', Validators.required],
      district:      [''],
      state:         [''],
      pincode:       ['', Validators.required],
    }),

    // Consent
    parentConsentGiven:    [false, Validators.requiredTrue],
    applicantConsentGiven: [false, Validators.requiredTrue],

    // Declaration
    declarationPlace: [''],
    declarationDate:  [''],

    // Referral
    referralTypeId:    [null as number | null],
    agentId:           [null as number | null],
    commissionAmount:  [null as number | null],
    referredStudentId: [null as number | null],
    referredFacultyId: [null as number | null],

    // Fee structure (FormArray — rebuilt when programme changes)
    yearFees: this.fb.array([]),

    // Payment history
    payments: this.fb.array([]),
  });

  get yearFeesArray(): FormArray { return this.form.get('yearFees') as FormArray; }
  get paymentsArray(): FormArray  { return this.form.get('payments') as FormArray; }
  get addressGroup():  FormGroup  { return this.form.get('address') as FormGroup; }

  ngOnInit(): void {
    forkJoin({
      programs:     this.programSvc.getAll(),
      years:        this.academicYearSvc.getAllAcademicYears(),
      communities:  this.communitySvc.getActiveCommunities(),
      bloodGroups:  this.bloodGroupSvc.getActiveBloodGroups(),
      referrals:    this.referralSvc.getActiveReferralTypes(),
      agents:       this.agentSvc.getActiveAgents(),
      feeStates:    this.financeSvc.getFeeStates(),
    }).subscribe({
      next: ({ programs, years, communities, bloodGroups, referrals, agents, feeStates }) => {
        this.programs.set(programs.filter((p: Program) => p.status === 'ACTIVE'));
        this.academicYears.set([...years].sort((a, b) => b.name.localeCompare(a.name)));
        this.communities.set(communities);
        this.bloodGroups.set(bloodGroups);
        this.referralTypes.set(referrals);
        this.agents.set(agents);
        this.feeStates.set(feeStates);
        this.enforceCounsellingState(feeStates, this.form.get('admissionQuota')?.value);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });

    this.form.get('admissionQuota')?.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(quota => {
        this.enforceCounsellingState(this.feeStates(), quota);
        this.tryLoadFeeGuideline();
      });

    const feeParamControls = ['gender', 'studentType', 'feeStateId', 'joiningAcademicYearId', 'courseId'];
    feeParamControls.forEach(name => {
      this.form.get(name)?.valueChanges
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(() => this.tryLoadFeeGuideline());
    });

    this.paymentsArray.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(vals => this.updatePaymentSummary(vals as { yearNumber: number; amount: number | null }[]));
  }

  private enforceCounsellingState(states: { id: number; name: string }[], quota: string | null): void {
    const ctrl = this.form.get('feeStateId');
    if (!ctrl) return;
    if (quota === 'COUNSELLING') {
      const tn = states.find(s => s.name.toLowerCase() === 'tamil nadu');
      if (tn) ctrl.setValue(tn.id);
      ctrl.disable({ emitEvent: false });
    } else {
      ctrl.enable({ emitEvent: false });
    }
  }

  protected onProgramChange(programId: number | null): void {
    this.courses.set([]);
    this.form.get('courseId')?.setValue(null);
    this.selectedProgram.set(null);
    this.rebuildYearFees(0);

    if (!programId) return;

    const prog = this.programs().find(p => p.id === programId) ?? null;
    this.selectedProgram.set(prog);

    if (prog) {
      this.rebuildYearFees(prog.durationYears);
      this.courseSvc.getByProgram(programId).subscribe({
        next: c => this.courses.set(c),
        error: () => this.courses.set([]),
      });
    }
  }

  private rebuildYearFees(count: number): void {
    const arr = this.yearFeesArray;
    while (arr.length > 0) arr.removeAt(0);
    for (let i = 1; i <= count; i++) {
      arr.push(this.fb.group({
        yearNumber: [i],
        masterFee:  [null as number | null],
        discount:   [null as number | null],
      }));
    }
    this.tryLoadFeeGuideline();
  }

  private tryLoadFeeGuideline(): void {
    if (this.yearFeesArray.length === 0) return;
    const v = this.form.value;
    if (!v.programId || !v.admissionQuota || !v.gender || !v.feeStateId || !v.joiningAcademicYearId) return;

    this.feeGuidelineLoading.set(true);
    const params = new URLSearchParams({
      programId:      v.programId.toString(),
      quota:          v.admissionQuota,
      feeStateId:     v.feeStateId.toString(),
      gender:         v.gender,
      academicYearId: v.joiningAcademicYearId.toString(),
    });
    if (v.courseId)    params.set('courseId', v.courseId.toString());
    if (v.studentType) params.set('studentType', v.studentType);

    this.http.get<{ totalFee: number; items: { amount: number; yearAmounts: { yearNumber: number; amount: number }[] }[] }>(
      `${environment.apiUrl}/fee-structures/guideline?${params}`
    ).subscribe({
      next: data => {
        const totals = new Map<number, number>();
        for (const item of data.items) {
          if (item.yearAmounts?.length) {
            for (const ya of item.yearAmounts) {
              totals.set(ya.yearNumber, (totals.get(ya.yearNumber) ?? 0) + ya.amount);
            }
          } else {
            for (let y = 1; y <= this.yearCount(); y++) {
              totals.set(y, (totals.get(y) ?? 0) + (item.amount ?? 0));
            }
          }
        }
        const arr = this.yearFeesArray;
        for (let i = 0; i < arr.length; i++) {
          const g = arr.at(i) as FormGroup;
          const yr = g.get('yearNumber')?.value as number;
          g.get('masterFee')?.setValue(totals.get(yr) ?? null, { emitEvent: false });
        }
        this.feeGuidelineLoading.set(false);
      },
      error: () => {
        const arr = this.yearFeesArray;
        for (let i = 0; i < arr.length; i++) {
          (arr.at(i) as FormGroup).get('masterFee')?.setValue(null, { emitEvent: false });
        }
        this.feeGuidelineLoading.set(false);
      },
    });
  }

  private updatePaymentSummary(vals: { yearNumber: number; amount: number | null }[]): void {
    const map = new Map<number, number>();
    for (const p of vals) {
      const yr = +p.yearNumber;
      map.set(yr, (map.get(yr) ?? 0) + (+(p.amount || 0)));
    }
    this.paymentSummary.set(map);
  }

  protected addPayment(): void {
    const g = this.fb.group({
      yearNumber:           [1, Validators.required],
      semesterSequence:     [1, Validators.required],
      paymentDate:          ['', Validators.required],
      amount:               [null as number | null, [Validators.required, Validators.min(0.01)]],
      paymentMode:          ['CASH', Validators.required],
      receiptNumber:        [''],
      transactionReference: [''],
      remarks:              [''],
    });
    this.paymentsArray.push(g);
    this.paymentRows.update(rows => [...rows, g]);
  }

  protected removePayment(i: number): void {
    this.paymentsArray.removeAt(i);
    this.paymentRows.update(rows => rows.filter((_, idx) => idx !== i));
    this.updatePaymentSummary(this.paymentsArray.value as { yearNumber: number; amount: number | null }[]);
  }

  protected onReferralTypeChange(referralTypeId: number | null): void {
    this.form.patchValue({ agentId: null, commissionAmount: null, referredStudentId: null, referredFacultyId: null });
    if (!referralTypeId) return;
    const rt = this.referralTypes().find(r => r.id === referralTypeId);
    if (rt?.code === 'STUDENT' || rt?.code === 'ALUMNI') {
      if (this.allStudents().length === 0) {
        this.studentSvc.getAll().subscribe({ next: s => this.allStudents.set(s) });
      }
    } else if (rt?.code === 'FACULTY') {
      if (this.allFaculty().length === 0) {
        this.facultySvc.getAll().subscribe({ next: f => this.allFaculty.set(f) });
      }
    }
  }

  protected referralCode(): string | null {
    const id = this.form.get('referralTypeId')?.value;
    if (!id) return null;
    return this.referralTypes().find(r => r.id === id)?.code ?? null;
  }

  protected finalFeeForYear(i: number): number {
    const g = this.yearFeesArray.at(i) as FormGroup;
    return Math.max(0, (+(g.get('masterFee')?.value || 0)) - (+(g.get('discount')?.value || 0)));
  }

  protected collectedForYear(yearNumber: number): number {
    return this.paymentSummary().get(yearNumber) ?? 0;
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.saveError.set(null);

    const v = this.form.value;

    const yearFees = (v.yearFees as { yearNumber: number; masterFee: number | null; discount: number | null }[])
      .filter(yf => (yf.masterFee ?? 0) > 0)
      .map(yf => ({
        yearNumber: yf.yearNumber,
        totalFee: Math.max(0, (yf.masterFee ?? 0) - (yf.discount ?? 0)),
      }));

    const payments = (v.payments as {
      yearNumber: number; semesterSequence: number; paymentDate: string;
      amount: number; paymentMode: string; receiptNumber: string;
      transactionReference: string; remarks: string;
    }[]).map(p => ({
      yearNumber:           p.yearNumber,
      semesterSequence:     this.isYearly() ? 1 : p.semesterSequence,
      paymentDate:          p.paymentDate,
      amount:               p.amount,
      paymentMode:          p.paymentMode,
      receiptNumber:        p.receiptNumber || null,
      transactionReference: p.transactionReference || null,
      remarks:              p.remarks || null,
    }));

    const body = {
      firstName:  v.firstName,  lastName: v.lastName,
      email:      v.email,      phone:    v.phone,
      rollNumber: v.rollNumber,
      universityRegistrationNumber: v.universityRegistrationNumber,
      umisNumber: v.umisNumber || null,
      programId:             v.programId,
      courseId:              v.courseId,
      joiningAcademicYearId: v.joiningAcademicYearId,
      admissionDate:         v.admissionDate,
      applicationDate:       v.applicationDate,
      yearOfStudy:           v.yearOfStudy,
      admissionQuota:        v.admissionQuota,
      studentType:           v.studentType,
      dateOfBirth:           v.dateOfBirth,
      gender:                v.gender,
      aadharNumber:          v.aadharNumber || null,
      nationality:           v.nationality,
      religion:              v.religion,
      communityCategory:     v.communityCategory,
      caste:                 v.caste,
      bloodGroup:            v.bloodGroup,
      physicalDisability:    v.physicalDisability,
      fatherName:            v.fatherName,
      fatherPhone:           v.fatherPhone,
      fatherEmail:           v.fatherEmail || null,
      motherName:            v.motherName,
      motherPhone:           v.motherPhone,
      motherEmail:           v.motherEmail || null,
      parentMobile:          v.parentMobile || null,
      address: {
        countryId:     v.address.country ?? 1,
        postalAddress: v.address.postalAddress,
        street:        v.address.street,
        city:          v.address.city,
        district:      v.address.district || null,
        state:         v.address.state || null,
        pincode:       v.address.pincode,
      },
      declarationPlace:      v.declarationPlace || null,
      declarationDate:       v.declarationDate || null,
      referralTypeId:        v.referralTypeId,
      agentId:               v.agentId,
      commissionAmount:      v.commissionAmount,
      referredStudentId:     v.referredStudentId,
      referredFacultyId:     v.referredFacultyId,
      yearFees,
      payments,
    };

    this.http.post<LegacyAdmitResponse>(`${environment.apiUrl}/students/legacy-admit`, body)
      .subscribe({
        next: res => {
          this.saving.set(false);
          this.successState.set(res);
        },
        error: (err) => {
          this.saving.set(false);
          this.saveError.set(err?.error?.message ?? 'Failed to save. Please try again.');
        },
      });
  }

  protected admitAnother(): void {
    this.successState.set(null);
    this.form.reset({
      admissionQuota: 'MANAGEMENT', studentType: 'DAY_SCHOLAR',
      gender: 'FEMALE', physicalDisability: false, nationality: 'Indian',
      yearOfStudy: 1, parentConsentGiven: false, applicantConsentGiven: false,
    });
    this.courses.set([]);
    this.selectedProgram.set(null);
    this.rebuildYearFees(0);
    this.paymentRows.set([]);
    this.paymentSummary.set(new Map());
    while (this.paymentsArray.length > 0) this.paymentsArray.removeAt(0);
  }

  protected viewStudent(studentId: number): void {
    this.router.navigate(['/students', studentId]);
  }

  protected yearOptions(): number[] {
    return Array.from({ length: this.yearCount() }, (_, i) => i + 1);
  }
}

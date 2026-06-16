import { Component, DestroyRef, inject, OnDestroy, OnInit, signal, computed } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { HttpClient } from '@angular/common/http';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { environment } from '../../../../environments';
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
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { RETRO_ADMIT_TOUR } from '../../../shared/tour/tours/student.tours';
import { PAYMENT_MODES, getPaymentModeLabel } from '../../../shared/utils/payment-mode.utils';

interface RetroAdmitResponse {
  studentId: number;
  admissionNumber: string;
  studentName: string;
  rollNumber: string;
  enquiryId: number;
  yearsWithFeeRecords: number;
  paymentRowsCreated: number;
  totalHistoricalPaid: number;
}

const RETRO_STEPS = [
  { label: 'Admission Context', description: 'Program, dates, quota and fee state' },
  { label: 'Student Details', description: 'Identity, demographics and profile' },
  { label: 'Family & Address', description: 'Parent contacts and address details' },
  { label: 'Consent & Declaration', description: 'Required consent confirmations' },
  { label: 'Referral', description: 'Referral source and commission details' },
  { label: 'Fee Structure', description: 'Year-wise fees and adjustments' },
  { label: 'Payment History', description: 'Historical collections and receipts' },
] as const;

@Component({
  selector: 'app-retro-admit',
  standalone: true,
  imports: [
    ReactiveFormsModule, RouterLink,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule,
    InrPipe, CmsCountryStateDistrictSelectorComponent,
    CmsTourButtonComponent,
  ],
  templateUrl: './retro-admit.component.html',
  styleUrl: './retro-admit.component.scss',
})
export class RetroAdmitComponent implements OnInit, OnDestroy {

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
  private readonly tourService     = inject(TourService);

  protected readonly loading             = signal(true);
  protected readonly saving              = signal(false);
  protected readonly saveError           = signal<string | null>(null);
  protected readonly successState        = signal<RetroAdmitResponse | null>(null);
  protected readonly feeGuidelineLoading  = signal(false);
  protected readonly feeGuidelineEmpty   = signal(false);
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

  protected readonly referralCategory = signal<'AGENT' | 'STUDENT' | 'ALUMNI' | 'FACULTY' | 'NONE'>('NONE');
  protected readonly agentSearchTerm  = signal('');
  protected readonly agentSearchOpen  = signal(false);

  protected readonly steps = RETRO_STEPS;
  protected readonly currentStep = signal(0);
  protected readonly completedCount = computed(() => this.currentStep());

  protected readonly isStepComplete = computed(() => {
    const cur = this.currentStep();
    return (i: number) => i < cur;
  });

  // ── Scroll-spy ────────────────────────────────────────────────────────────
  private scrollContainer: Element | null = null;
  private scrollHandler: (() => void) | null = null;
  private rafId: number | null = null;

  protected readonly selectedProgram = signal<Program | null>(null);
  protected readonly isYearly        = computed(() => this.selectedProgram()?.assessmentPattern === 'YEARLY');
  protected readonly yearCount       = computed(() => this.selectedProgram()?.durationYears ?? 0);

  protected readonly filteredAgents = computed(() => {
    const term = this.agentSearchTerm().trim().toLowerCase();
    if (!term) return this.agents().slice(0, 20);
    return this.agents()
      .filter(a => a.name.toLowerCase().includes(term) || (a.phone ?? '').toLowerCase().includes(term))
      .slice(0, 20);
  });

  protected get today(): string {
    return new Date().toISOString().split('T')[0];
  }

  private get currentAcademicStartYear(): number {
    const now = new Date();
    return now.getMonth() >= 5 ? now.getFullYear() : now.getFullYear() - 1;
  }

  protected readonly pastAcademicYears = computed(() =>
    this.academicYears().filter(y => {
      const startYear = parseInt(y.name.substring(0, 4), 10);
      return isNaN(startYear) || startYear < this.currentAcademicStartYear;
    })
  );

  protected readonly paymentRows       = signal<FormGroup[]>([]);
  protected readonly fifoCollected     = signal<Map<number, number>>(new Map());

  protected readonly PAYMENT_MODES = PAYMENT_MODES;
  protected readonly getPaymentModeLabel = getPaymentModeLabel;

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
    this.tourService.register('retro-admit', RETRO_ADMIT_TOUR);

    // Enforce at least one payment entry and aggregate payment cap.
    this.paymentsArray.setValidators([
      this.paymentsAtLeastOneValidator(),
      this.paymentsDoNotExceedActualFeeValidator(),
    ]);
    this.paymentsArray.updateValueAndValidity({ emitEvent: false });

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
        setTimeout(() => this.setupScrollSpy(), 100);
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
      .subscribe(() => {
        this.recomputeFifo();
        this.paymentsArray.updateValueAndValidity({ emitEvent: false });
      });

    this.form.get('joiningAcademicYearId')?.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(id => {
        this.form.get('yearOfStudy')?.setValue(this.computeYearOfStudy(id), { emitEvent: false });
      });

    this.yearFeesArray.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.recomputeFifo();
        this.paymentsArray.updateValueAndValidity({ emitEvent: false });
      });
  }

  private computeYearOfStudy(joiningAcademicYearId: number | null): number {
    if (!joiningAcademicYearId) return 1;
    const year = this.academicYears().find(y => y.id === joiningAcademicYearId);
    if (!year) return 1;
    const joinStart = parseInt(year.name.substring(0, 4), 10);
    if (isNaN(joinStart)) return 1;
    const now = new Date();
    const currentAcademicStart = now.getMonth() >= 5 ? now.getFullYear() : now.getFullYear() - 1;
    return Math.max(1, currentAcademicStart - joinStart + 1);
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
        actualFee:  [null as number | null, [Validators.required, Validators.min(0.01)]],
      }, { validators: this.actualFeeNotGreaterThanGuideline }));
    }
    this.tryLoadFeeGuideline();
  }

  private actualFeeNotGreaterThanGuideline(group: AbstractControl) {
    if (!(group instanceof FormGroup)) return null;
    const actualFee = +(group.get('actualFee')?.value || 0);
    const masterFee = +(group.get('masterFee')?.value || 0);
    if (masterFee > 0 && actualFee > masterFee) {
      group.get('actualFee')?.setErrors({ ...group.get('actualFee')?.errors, actualFeeExceedsGuideline: true });
      return { actualFeeExceedsGuideline: { actual: actualFee, guideline: masterFee } };
    } else {
      const err = group.get('actualFee')?.errors;
      if (err && 'actualFeeExceedsGuideline' in err) {
        delete err['actualFeeExceedsGuideline'];
        group.get('actualFee')?.setErrors(Object.keys(err).length > 0 ? err : null);
      }
    }
    return null;
  }

  private paymentsAtLeastOneValidator() {
    return (_: AbstractControl): ValidationErrors | null =>
      this.paymentsArray.length === 0 ? { paymentsRequired: true } : null;
  }

  private paymentsDoNotExceedActualFeeValidator() {
    return (_: AbstractControl): ValidationErrors | null => {
      const totalPaid = this.paymentsArray.controls.reduce((sum, ctrl) => sum + +(ctrl.get('amount')?.value || 0), 0);
      const totalActualFee = this.yearFeesArray.controls.reduce((sum, ctrl) => sum + +(ctrl.get('actualFee')?.value || 0), 0);

      if (totalPaid <= totalActualFee) {
        return null;
      }

      return {
        paymentsExceedActualFee: {
          totalPaid,
          totalActualFee,
          excessAmount: totalPaid - totalActualFee,
        },
      };
    };
  }

  private tryLoadFeeGuideline(): void {
    if (this.yearFeesArray.length === 0) return;
    const v = this.form.getRawValue();
    if (!v.programId || !v.admissionQuota || !v.gender || !v.feeStateId || !v.joiningAcademicYearId) return;

    this.feeGuidelineLoading.set(true);
    this.feeGuidelineEmpty.set(false);
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
        this.feeGuidelineEmpty.set(totals.size === 0);
      },
      error: () => {
        const arr = this.yearFeesArray;
        for (let i = 0; i < arr.length; i++) {
          (arr.at(i) as FormGroup).get('masterFee')?.setValue(null, { emitEvent: false });
        }
        this.feeGuidelineLoading.set(false);
        this.feeGuidelineEmpty.set(true);
      },
    });
  }

  private recomputeFifo(): void {
    const payments = (this.paymentsArray.value as { amount: number | null }[])
      .map(p => +(p.amount || 0))
      .filter(a => a > 0);

    const yearCount = this.yearCount();
    const isYearly  = this.isYearly();
    const semsPerYear = isYearly ? 1 : 2;

    // Build ordered semester slots from yearFees
    const slots: { yearNumber: number; semSequence: number; fee: number }[] = [];
    for (let y = 1; y <= yearCount; y++) {
      const g = this.yearFeesArray.at(y - 1) as FormGroup;
      const fee = +(g?.get('actualFee')?.value || g?.get('masterFee')?.value || 0);
      const feePerSem = semsPerYear > 1 ? fee / semsPerYear : fee;
      for (let s = 1; s <= semsPerYear; s++) {
        slots.push({ yearNumber: y, semSequence: s, fee: feePerSem });
      }
    }

    // FIFO allocation
    const collected = new Map<number, number>();
    let slotIdx = 0;
    let slotRemaining = slots[0]?.fee ?? 0;

    for (const pmt of payments) {
      let remaining = pmt;
      while (remaining > 0 && slotIdx < slots.length) {
        const slot = slots[slotIdx];
        const apply = Math.min(remaining, slotRemaining);
        remaining   -= apply;
        slotRemaining -= apply;
        collected.set(slot.yearNumber, (collected.get(slot.yearNumber) ?? 0) + apply);
        if (slotRemaining <= 0) {
          slotIdx++;
          slotRemaining = slots[slotIdx]?.fee ?? 0;
        }
      }
    }
    this.fifoCollected.set(collected);
  }

  protected addPayment(): void {
    const g = this.fb.group({
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
    this.recomputeFifo();
  }

  protected onReferralTypeChange(referralTypeId: number | null): void {
    this.form.patchValue({ agentId: null, commissionAmount: null, referredStudentId: null, referredFacultyId: null });
    this.agentSearchTerm.set('');
    this.agentSearchOpen.set(false);
    if (!referralTypeId) {
      this.referralCategory.set('NONE');
      return;
    }
    const rt = this.referralTypes().find(r => r.id === referralTypeId);
    const commission = rt?.hasCommission ? (rt.commissionAmount ?? null) : null;
    if (commission !== null) {
      this.form.patchValue({ commissionAmount: commission });
    }
    const code = rt?.code ?? '';
    if (code === 'AGENT_REFERRAL') {
      this.referralCategory.set('AGENT');
    } else if (code === 'STUDENT') {
      this.referralCategory.set('STUDENT');
      if (this.allStudents().length === 0) {
        this.studentSvc.getAll().subscribe({ next: s => this.allStudents.set(s) });
      }
    } else if (code === 'ALUMNI') {
      this.referralCategory.set('ALUMNI');
      if (this.allStudents().length === 0) {
        this.studentSvc.getAll().subscribe({ next: s => this.allStudents.set(s) });
      }
    } else if (code === 'FACULTY') {
      this.referralCategory.set('FACULTY');
      if (this.allFaculty().length === 0) {
        this.facultySvc.getAll().subscribe({ next: f => this.allFaculty.set(f) });
      }
    } else {
      this.referralCategory.set('NONE');
    }
  }

  private selectedReferralType() {
    const id = this.form.get('referralTypeId')?.value;
    if (!id) return undefined;
    return this.referralTypes().find(r => r.id === id);
  }

  protected selectAgent(id: number, name: string): void {
    this.form.patchValue({ agentId: id });
    this.agentSearchTerm.set(name);
    this.agentSearchOpen.set(false);
    this.onAgentChange(id);
  }

  protected clearAgent(): void {
    this.form.patchValue({ agentId: null });
    this.agentSearchTerm.set('');
    this.onAgentChange(null);
  }

  protected onAgentSearchInput(event: Event): void {
    this.agentSearchTerm.set((event.target as HTMLInputElement).value);
    this.agentSearchOpen.set(true);
    this.form.patchValue({ agentId: null }, { emitEvent: false });
  }

  protected onAgentChange(agentId: number | null): void {
    const rt = this.selectedReferralType();
    if (!rt) return;
    if (agentId === null || agentId === undefined) {
      this.form.patchValue({ commissionAmount: rt.hasCommission ? (rt.commissionAmount ?? null) : null });
      return;
    }
    const agent = this.agents().find(a => a.id === agentId);
    const commission = agent?.commissionAmount != null && Number(agent.commissionAmount) > 0
      ? Number(agent.commissionAmount)
      : (rt.hasCommission ? (rt.commissionAmount ?? null) : null);
    this.form.patchValue({ commissionAmount: commission });
  }

  protected discountForYear(i: number): number {
    const g = this.yearFeesArray.at(i) as FormGroup;
    const guide  = +(g.get('masterFee')?.value || 0);
    const actual = +(g.get('actualFee')?.value  || 0);
    return actual > 0 ? Math.max(0, guide - actual) : 0;
  }

  protected collectedForYear(yearNumber: number): number {
    return this.fifoCollected().get(yearNumber) ?? 0;
  }

  protected get totalGuidelineFee(): number {
    return this.yearFeesArray.controls.reduce((sum, ctrl) => sum + +(ctrl.get('masterFee')?.value || 0), 0);
  }

  protected get totalActualFee(): number {
    return this.yearFeesArray.controls.reduce((sum, ctrl) => sum + +(ctrl.get('actualFee')?.value || 0), 0);
  }

  protected get totalDiscount(): number {
    return this.yearFeesArray.controls.reduce((sum, _, i) => sum + this.discountForYear(i), 0);
  }

  protected get totalCollected(): number {
    let sum = 0;
    this.fifoCollected().forEach(v => (sum += v));
    return sum;
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      setTimeout(() => {
        const firstInvalid = document.querySelector<HTMLElement>(
          'input.ng-invalid, select.ng-invalid, textarea.ng-invalid'
        );
        if (firstInvalid) {
          firstInvalid.scrollIntoView({ behavior: 'smooth', block: 'center' });
          firstInvalid.focus({ preventScroll: true });

          // Move the stepper cursor to the section that contains the first invalid field,
          // so the user immediately knows which part of the form needs attention.
          const sectionEl = firstInvalid.closest<HTMLElement>('[id^="retro-section-"]');
          if (sectionEl) {
            const idx = parseInt(sectionEl.id.replace('retro-section-', ''), 10);
            if (!isNaN(idx)) {
              this.currentStep.set(idx);
            }
          }
        } else if (this.paymentsArray.hasError('paymentsRequired')) {
          this.scrollToSection(6);
        }
      }, 50);
      return;
    }
    this.saving.set(true);
    this.saveError.set(null);

    const v = this.form.value;

    const yearFees = (v.yearFees as { yearNumber: number; masterFee: number | null; actualFee: number | null }[])
      .map(yf => ({
        yearNumber: yf.yearNumber,
        totalFee:   yf.actualFee ?? 0,
      }));

    const payments = (v.payments as {
      paymentDate: string; amount: number; paymentMode: string;
      receiptNumber: string; transactionReference: string; remarks: string;
    }[]).map(p => ({
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
      umisNumber: v.umisNumber || undefined,
      programId:             v.programId,
      courseId:              v.courseId || undefined,
      joiningAcademicYearId: v.joiningAcademicYearId,
      admissionDate:         v.admissionDate,
      applicationDate:       v.applicationDate,
      yearOfStudy:           v.yearOfStudy,
      admissionQuota:        v.admissionQuota,
      studentType:           v.studentType,
      dateOfBirth:           v.dateOfBirth,
      gender:                v.gender,
      aadharNumber:          v.aadharNumber || undefined,
      nationality:           v.nationality,
      religion:              v.religion,
      communityCategory:     v.communityCategory,
      caste:                 v.caste,
      bloodGroup:            v.bloodGroup,
      physicalDisability:    v.physicalDisability,
      fatherName:            v.fatherName,
      fatherPhone:           v.fatherPhone,
      fatherEmail:           v.fatherEmail || undefined,
      motherName:            v.motherName,
      motherPhone:           v.motherPhone,
      motherEmail:           v.motherEmail || undefined,
      address: {
        countryId:     v.address.country ?? 1,
        postalAddress: v.address.postalAddress,
        street:        v.address.street,
        city:          v.address.city,
        district:      v.address.district || undefined,
        state:         v.address.state || undefined,
        pincode:       v.address.pincode,
      },
      declarationPlace:      v.declarationPlace || undefined,
      declarationDate:       v.declarationDate || undefined,
      referralTypeId:        v.referralTypeId || undefined,
      agentId:               v.agentId || undefined,
      commissionAmount:      v.commissionAmount || undefined,
      referredStudentId:     v.referredStudentId || undefined,
      referredFacultyId:     v.referredFacultyId || undefined,
      yearFees,
      payments,
    };

    this.http.post<RetroAdmitResponse>(`${environment.apiUrl}/students/retro-admit`, body)
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
    this.fifoCollected.set(new Map());
    this.feeGuidelineEmpty.set(false);
    while (this.paymentsArray.length > 0) this.paymentsArray.removeAt(0);
  }

  protected viewStudent(studentId: number): void {
    this.router.navigate(['/students', studentId]);
  }

  private getRenderedStepIndexes(): number[] {
    const rendered: number[] = [];
    for (let i = 0; i < this.steps.length; i++) {
      if (document.getElementById(`retro-section-${i}`)) {
        rendered.push(i);
      }
    }
    return rendered;
  }


  protected scrollToSection(index: number): void {
    const section = document.getElementById(`retro-section-${index}`);
    const container = this.scrollContainer ?? document.querySelector('main.app-content');
    if (!section || !container) {
      return;
    }

    const containerRect = container.getBoundingClientRect();
    const sectionRect = section.getBoundingClientRect();
    const targetTop = container.scrollTop + (sectionRect.top - containerRect.top) - 16;
    container.scrollTo({ top: Math.max(0, targetTop), behavior: 'smooth' });
    this.currentStep.set(index);
  }

  private setupScrollSpy(): void {
    this.scrollContainer = document.querySelector('main.app-content');
    if (!this.scrollContainer) return;

    this.updateActiveSection();

    const onScroll = () => {
      if (this.rafId !== null) return;
      this.rafId = requestAnimationFrame(() => {
        this.rafId = null;
        this.updateActiveSection();
      });
    };

    this.scrollHandler = onScroll;
    this.scrollContainer.addEventListener('scroll', onScroll, { passive: true });
  }

  private updateActiveSection(): void {
    const container = this.scrollContainer;
    if (!container) return;

    const renderedSteps = this.getRenderedStepIndexes();
    if (renderedSteps.length === 0) {
      this.currentStep.set(0);
      return;
    }

    const containerRect = container.getBoundingClientRect();
    const footerEl = document.querySelector('.conv-form-footer') as HTMLElement | null;
    const footerHeight = footerEl?.getBoundingClientRect().height ?? 0;

    const effectiveTop = containerRect.top;
    const effectiveBottom = containerRect.bottom - footerHeight;
    const effectiveHeight = Math.max(1, effectiveBottom - effectiveTop);

    const firstSection = document.getElementById(`retro-section-${renderedSteps[0]}`);
    const lastStepIndex = renderedSteps[renderedSteps.length - 1];
    const lastSection = document.getElementById(`retro-section-${lastStepIndex}`);

    const reachedBottom = container.scrollTop + container.clientHeight >= container.scrollHeight - 4;
    if (reachedBottom) {
      this.currentStep.set(lastStepIndex);
      return;
    }

    if (firstSection && firstSection.getBoundingClientRect().top >= effectiveTop - 8) {
      this.currentStep.set(renderedSteps[0]);
      return;
    }

    // If the final section is visibly in the lower viewport, force it active.
    if (lastSection && lastSection.getBoundingClientRect().top <= effectiveBottom - 24) {
      this.currentStep.set(lastStepIndex);
      return;
    }

    const triggerY = effectiveTop + Math.min(Math.max(effectiveHeight * 0.72, 180), 460);

    let active = renderedSteps[0];
    for (const index of renderedSteps) {
      const el = document.getElementById(`retro-section-${index}`);
      if (!el) continue;
      if (el.getBoundingClientRect().top <= triggerY) {
        active = index;
      } else {
        break;
      }
    }

    // The triggerY threshold (72 %) can leave a dead zone for near-bottom sections:
    // a section is clearly visible in the lower viewport but its top hasn't crossed
    // the trigger line yet (e.g. Fee Structure when Payment History is still off-screen).
    // Check whether the section immediately after the trigger-detected active one is
    // already visible, and if so promote it — this fixes the Fee Structure step.
    const activePos = renderedSteps.indexOf(active);
    if (activePos >= 0 && activePos + 1 < renderedSteps.length) {
      const nextIdx = renderedSteps[activePos + 1];
      const nextEl = document.getElementById(`retro-section-${nextIdx}`);
      if (nextEl && nextEl.getBoundingClientRect().top < effectiveBottom - 24) {
        active = nextIdx;
      }
    }

    this.currentStep.set(active);
  }

  ngOnDestroy(): void {
    if (this.scrollContainer && this.scrollHandler) {
      this.scrollContainer.removeEventListener('scroll', this.scrollHandler);
    }
    if (this.rafId !== null) cancelAnimationFrame(this.rafId);
  }
}


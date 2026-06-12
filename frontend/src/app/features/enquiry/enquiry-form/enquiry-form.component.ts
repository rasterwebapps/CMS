import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { EnquiryService } from '../enquiry.service';
import { EnquiryRequest } from '../enquiry.model';
import { Agent } from '../../agent/agent.model';
import { AgentService } from '../../agent/agent.service';
import { ReferralType } from '../../referral-type/referral-type.model';
import { ReferralTypeService } from '../../referral-type/referral-type.service';
import { environment } from '../../../../environments';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { ENQUIRY_FORM_TOUR } from '../../../shared/tour/tours/enquiry.tours';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';
import { CmsCountryStateDistrictSelectorComponent } from '../../../shared/country-state-district-selector/country-state-district-selector.component';
interface ProgramInfo {
  id: number;
  name: string;
  code: string;
  durationYears: number;
  specialities: { id: number; name: string }[];
  minimumAgeYears: number | null;
  ageCutoffDay: number | null;
  ageCutoffMonth: number | null;
}
interface CourseInfo {
  id: number;
  name: string;
  code: string;
  specialization: string | null;
  programId: number;
}
interface FeeStructureItem {
  feeType: string;
  amount: number;
  yearAmounts: { yearNumber: number; yearLabel: string; amount: number }[];
}

interface FeeGuidelineResponse {
  totalFee: number;
  items: FeeStructureItem[];
}

interface FeeState {
  id: number;
  name: string;
  code: string;
  isDefault: boolean;
  isFallback: boolean;
  sortOrder: number;
}
const ENQ_STEPS = [
  { label: 'Personal Details', description: 'Name, contact and date of birth' },
  { label: 'Location', description: 'Country, state and district details' },
  { label: 'Academic Interest', description: 'Program, course, quota and type' },
  { label: 'Enquiry Details', description: 'Referral source and remarks' },
] as const;

const ENQ_STEP_FIELDS: Record<number, string[]> = {
  0: ['name', 'phone', 'dateOfBirth', 'gender'],
  1: ['country', 'state'],
  2: ['programId', 'admissionQuota', 'studentType'],
  3: ['enquiryDate', 'referralTypeId'],
};

@Component({
  selector: 'app-enquiry-form',
  standalone: true,
  imports: [
    InrPipe,
    RouterLink, ReactiveFormsModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule,
    CmsTourButtonComponent,
    CmsCountryStateDistrictSelectorComponent],
  templateUrl: './enquiry-form.component.html',
  styleUrl: './enquiry-form.component.scss',
})
export class EnquiryFormComponent implements OnInit, OnDestroy {
  private scrollListener: (() => void) | null = null;
  private scrollContainer: Element | null = null;
  private rafId: number | null = null;
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly enquiryService = inject(EnquiryService);
  private readonly agentService = inject(AgentService);
  private readonly referralTypeService = inject(ReferralTypeService);
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);
  private readonly tourService = inject(TourService);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);

  // ── Stepper ──────────────────────────────────────────────────────────────
  protected readonly enqSteps       = ENQ_STEPS;
  protected readonly currentEnqStep = signal(0);

  protected readonly isEnqStepComplete = computed(() => {
    const cur = this.currentEnqStep();
    return (i: number) => i < cur;
  });


  protected goToNextEnq(): void {
    const step   = this.currentEnqStep();
    const fields = ENQ_STEP_FIELDS[step] ?? [];
    let valid = true;
    for (const key of fields) {
      const ctrl = this.form.get(key);
      if (ctrl) { ctrl.markAsTouched(); if (ctrl.invalid) valid = false; }
    }
    // Step 3: validate conditional referral fields
    if (step === 3) {
      const cat = this.referralCategory();
      if (cat === 'AGENT') {
        const c = this.form.get('agentId'); c?.markAsTouched(); if (c?.invalid) valid = false;
      }
      if (cat === 'STAFF') {
        const c = this.form.get('referredStaffName'); c?.markAsTouched(); if (c?.invalid) valid = false;
      }
    }
    if (!valid) { scrollToFirstInvalid(this.form); return; }
    this.currentEnqStep.update(s => Math.min(s + 1, ENQ_STEPS.length - 1));
    document.querySelector('main.app-content')?.scrollTo({ top: 0, behavior: 'smooth' });
  }

  protected goToPrevEnq(): void {
    this.currentEnqStep.update(s => Math.max(s - 1, 0));
    document.querySelector('main.app-content')?.scrollTo({ top: 0, behavior: 'smooth' });
  }

  protected scrollToSection(sectionIndex: number): void {
    const section = document.querySelector(`[data-section="${sectionIndex}"]`) as HTMLElement;
    if (!section) return;
    const container = this.scrollContainer ?? document.querySelector('main.app-content');
    if (!container) { section.scrollIntoView({ behavior: 'smooth', block: 'start' }); return; }

    const containerRect = container.getBoundingClientRect();
    const sectionRect   = section.getBoundingClientRect();
    const targetTop     = container.scrollTop + (sectionRect.top - containerRect.top) - 16;
    container.scrollTo({ top: Math.max(0, targetTop), behavior: 'smooth' });
    this.currentEnqStep.set(sectionIndex);
  }

  protected onScroll(): void {
    const container = this.scrollContainer;
    if (!container) return;

    const containerRect = container.getBoundingClientRect();
    const triggerY      = containerRect.top + containerRect.height * 0.75;

    const sections = Array.from(document.querySelectorAll('.section-wrapper')) as HTMLElement[];
    let activeSection = 0;
    for (let i = 0; i < sections.length; i++) {
      if (sections[i].getBoundingClientRect().top <= triggerY) {
        activeSection = i;
      } else {
        break;
      }
    }

    this.currentEnqStep.set(activeSection);
  }

  protected readonly pageTitle = signal('Add Enquiry');
  protected readonly programs = signal<ProgramInfo[]>([]);
  protected readonly courses = signal<CourseInfo[]>([]);
  protected readonly agents = signal<Agent[]>([]);
  protected readonly referralTypes = signal<ReferralType[]>([]);
  protected readonly referralAdditionalAmount = signal(0);
  // Person search signals (loaded on-demand when referral type changes)
  protected readonly studentList = signal<{ id: number; fullName: string; rollNumber: string }[]>([]);
  protected readonly alumniList = signal<{ id: number; fullName: string; rollNumber: string }[]>([]);
  protected readonly facultyList = signal<{ id: number; fullName: string; employeeCode: string }[]>([]);
  protected readonly personSearchTerm = signal('');
  protected readonly personSearchOpen = signal(false);
  // Agent search signals
  protected readonly agentSearchTerm = signal('');
  protected readonly agentSearchOpen = signal(false);
  protected readonly feeError = signal(false);
  protected readonly feeLoading    = signal(false);
  protected readonly feeNotFound   = signal(false);
  protected readonly feeStates     = signal<FeeState[]>([]);
  private readonly yearWiseFees = signal<string>('');

  protected readonly quotaOptions = [
    { value: 'MANAGEMENT',  label: 'Management Quota' },
    { value: 'COUNSELLING', label: 'Counselling Quota' },
  ];
  protected readonly statusOptions = ['ENQUIRED', 'INTERESTED', 'NOT_INTERESTED', 'FEES_FINALIZED', 'FEES_PAID', 'PARTIALLY_PAID', 'DOCUMENTS_SUBMITTED', 'DOCUMENTS_VERIFIED', 'ADMITTED', 'CLOSED'];
  /** Max date for enquiry date input — today as YYYY-MM-DD string */
  protected readonly maxDateStr: string = new Date().toISOString().split('T')[0];
  /** Max date for DOB input — yesterday (DOB must be in the past) */
  protected readonly dobMaxStr: string = (() => {
    const d = new Date(); d.setDate(d.getDate() - 1); return d.toISOString().split('T')[0];
  })();
  /** Gender options for the gender select */
  protected readonly genderOptions = [
    { value: 'FEMALE', label: 'Female' },
    { value: 'MALE',   label: 'Male' },
    { value: 'OTHER',  label: 'Other' },
  ];
  /** Guard flag — prevents ping-pong between DOB ↔ Age valueChanges listeners */
  private dobAgeSyncing = false;
  protected readonly selectedProgram = signal<ProgramInfo | null>(null);
  protected readonly ageRestrictionError = signal<string | null>(null);
  protected readonly totalFees = signal(0);
  private itemId: number | null = null;
  protected readonly form: FormGroup = this.fb.group({
    name:           ['', [Validators.required, Validators.maxLength(255)]],
    email:          [''],
    phone:          ['', Validators.required],
    programId:      [null as number | null, Validators.required],
    courseId:       [null as number | null],        // required conditionally — set by updateCourseValidator()
    enquiryDate:    [this.maxDateStr, Validators.required],
    referralTypeId: [null as number | null, Validators.required],
    status:         ['ENQUIRED'],
    agentId:        [null as number | null],
    remarks:        [''],
    studentType:    ['DAY_SCHOLAR' as 'DAY_SCHOLAR' | 'HOSTELER', Validators.required],
    admissionQuota: ['MANAGEMENT' as 'MANAGEMENT' | 'COUNSELLING', Validators.required],
    country:        [null as number | null, Validators.required],
    state:          ['', Validators.required],
    district:       [''],
    referredStudentId: [null as number | null],
    referredFacultyId: [null as number | null],
    referredStaffName: [null as string | null],
    dateOfBirth:    ['', Validators.required],
    age:            [null as number | null, [Validators.min(0), Validators.max(150)]],
    gender:         ['FEMALE' as 'FEMALE' | 'MALE' | 'OTHER', Validators.required],
  });
  /** Tracks which referral-related sub-form to show; updated imperatively in onReferralTypeChange. */
  protected readonly referralCategory = signal<'AGENT' | 'STUDENT' | 'ALUMNI' | 'FACULTY' | 'STAFF' | 'NONE'>('NONE');
  /** Filtered student list based on the current search term. */
  protected readonly filteredStudents = computed(() => {
    const term = this.personSearchTerm().trim().toLowerCase();
    if (!term) return this.studentList().slice(0, 20);
    return this.studentList()
      .filter(s => s.fullName.toLowerCase().includes(term) || s.rollNumber.toLowerCase().includes(term))
      .slice(0, 20);
  });
  /** Filtered alumni list based on the current search term. */
  protected readonly filteredAlumni = computed(() => {
    const term = this.personSearchTerm().trim().toLowerCase();
    if (!term) return this.alumniList().slice(0, 20);
    return this.alumniList()
      .filter(s => s.fullName.toLowerCase().includes(term) || s.rollNumber.toLowerCase().includes(term))
      .slice(0, 20);
  });
  /** Filtered faculty list based on the current search term. */
  protected readonly filteredFaculty = computed(() => {
    const term = this.personSearchTerm().trim().toLowerCase();
    if (!term) return this.facultyList().slice(0, 20);
    return this.facultyList()
      .filter(f => f.fullName.toLowerCase().includes(term) || f.employeeCode.toLowerCase().includes(term))
      .slice(0, 20);
  });
  /** Filtered agent list based on the current agent search term. */
  protected readonly filteredAgents = computed(() => {
    const term = this.agentSearchTerm().trim().toLowerCase();
    if (!term) return this.agents().slice(0, 20);
    return this.agents()
      .filter(a => a.name.toLowerCase().includes(term) || (a.phone ?? '').toLowerCase().includes(term))
      .slice(0, 20);
  });
  protected setStudentType(type: 'DAY_SCHOLAR' | 'HOSTELER'): void {
    this.form.patchValue({ studentType: type });
    this.onStudentTypeChange();
  }
  ngOnDestroy(): void {
    if (this.scrollContainer && this.scrollListener) {
      this.scrollContainer.removeEventListener('scroll', this.scrollListener);
    }
    if (this.rafId !== null) cancelAnimationFrame(this.rafId);
    this.scrollListener = null;
  }

  ngOnInit(): void {
    this.tourService.register('enquiry-form', ENQUIRY_FORM_TOUR);

    this.scrollContainer = document.querySelector('main.app-content');
    const onScroll = () => {
      if (this.rafId !== null) return;
      this.rafId = requestAnimationFrame(() => {
        this.rafId = null;
        this.onScroll();
      });
    };
    this.scrollListener = onScroll;
    this.scrollContainer?.addEventListener('scroll', onScroll, { passive: true });

    // Re-compute fee whenever the address state changes
    this.form.get('state')?.valueChanges.subscribe(() => this.tryLoadFeeGuideline());

    this.form.get('enquiryDate')?.valueChanges.subscribe(() => this.recomputeAgeRestrictionError());

    this.form.get('dateOfBirth')?.valueChanges.subscribe((dob: string | null) => {
      if (this.dobAgeSyncing) return;
      const age = this.calcAgeFromDob(dob);
      this.dobAgeSyncing = true;
      this.form.get('age')?.setValue(age, { emitEvent: false });
      this.dobAgeSyncing = false;
      this.recomputeAgeRestrictionError();
    });
    this.form.get('age')?.valueChanges.subscribe((age: number | null) => {
      if (this.dobAgeSyncing) return;
      const dob = this.calcDobFromAge(age);
      this.dobAgeSyncing = true;
      this.form.get('dateOfBirth')?.setValue(dob, { emitEvent: false });
      this.dobAgeSyncing = false;
      this.recomputeAgeRestrictionError();
    });
    this.http.get<ProgramInfo[]>(`${environment.apiUrl}/programs`).subscribe({
      next: (data) => this.programs.set(data),
    });
    this.http.get<FeeState[]>(`${environment.apiUrl}/fee-states`).subscribe({
      next: (states) => this.feeStates.set(states),
    });
    this.agentService.getActiveAgents().subscribe({
      next: (data) => this.agents.set(data),
      error: () => {},
    });
    this.referralTypeService.getActiveReferralTypes().subscribe({
      next: (data) => this.referralTypes.set(data),
      error: () => {},
    });
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.itemId = Number(id);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Enquiry');
      this.form.get('status')?.disable();
      this.loading.set(true);
      this.enquiryService.getEnquiryById(this.itemId).subscribe({
        next: (item) => {
          this.form.patchValue({
            name: item.name, email: item.email, phone: item.phone, programId: item.programId,
            courseId: item.courseId,
            enquiryDate: item.enquiryDate ?? this.maxDateStr,
            referralTypeId: item.referralTypeId, status: item.status,
            agentId: item.agentId,
            remarks: item.remarks,
            studentType: item.studentType ?? 'DAY_SCHOLAR',
            admissionQuota: item.admissionQuota ?? 'MANAGEMENT',
            country: item.countryId ?? null,
            state: item.state ?? '',
            district: item.district ?? '',
            referredStudentId: item.referredStudentId ?? null,
            referredFacultyId: item.referredFacultyId ?? null,
            referredStaffName: item.referredStaffName ?? null,
            dateOfBirth: item.dateOfBirth ?? '',
            gender: item.gender ?? 'FEMALE',
          });
          // Pre-populate person search term for display
          if (item.referredStudentName) {
            this.personSearchTerm.set(item.referredStudentName);
          } else if (item.referredFacultyName) {
            this.personSearchTerm.set(item.referredFacultyName);
          }
          if (item.agentName) {
            this.agentSearchTerm.set(item.agentName);
          }
          if (item.referralTypeId) {
            // Re-use the same logic as the change handler (sets referralCategory signal too)
            this.onReferralTypeChange(item.referralTypeId);
            // Restore person IDs that were cleared by onReferralTypeChange
            this.form.patchValue({
              agentId: item.agentId ?? null,
              referredStudentId: item.referredStudentId ?? null,
              referredFacultyId: item.referredFacultyId ?? null,
              referredStaffName: item.referredStaffName ?? null,
            });
            if (item.agentName) { this.agentSearchTerm.set(item.agentName); }
          }
          if (item.agentId) {
            this.onAgentChange(item.agentId);
          }
          if (item.programId) {
            const program = this.programs().find((p) => p.id === item.programId) ?? null;
            this.selectedProgram.set(program);
            this.loadCoursesForProgram(item.programId);
            // Fee loading is deferred until feeStateId is also patched (the fee-states
            // API call above may still be in flight). A brief scheduler trick ensures
            // the fee-state has been set before we attempt the guideline call.
            setTimeout(() => this.tryLoadFeeGuideline(), 300);
          }
          this.loading.set(false);
        },
        error: () => {
          this.toast.error('Failed to load');
          void this.router.navigate(['/enquiries']);
        },
      });
    }
  }
  protected onProgramChange(programId: number): void {
    this.form.patchValue({ courseId: null });
    this.courses.set([]);
    this.selectedProgram.set(null);
    this.totalFees.set(0);
    this.feeNotFound.set(false);
    if (programId) {
      this.loadCoursesForProgram(programId);
      const program = this.programs().find((p) => p.id === programId) ?? null;
      this.selectedProgram.set(program);
    }
    this.recomputeAgeRestrictionError();
  }

  protected onCourseChange(_courseId: number): void {
    this.tryLoadFeeGuideline();
  }

  protected onStudentTypeChange(): void {
    this.tryLoadFeeGuideline();
  }

  protected onDimensionChange(): void {
    this.tryLoadFeeGuideline();
  }

  /** Resolve the fee state ID.
   *  Counselling quota is always Tamil Nadu. Management resolves from address state text. */
  private resolveFeeStateId(): number | null {
    const states = this.feeStates();
    if (!states.length) return null;
    const quota = this.form.get('admissionQuota')?.value as string;
    if (quota === 'COUNSELLING') {
      return states.find(s => s.name.toLowerCase() === 'tamil nadu')?.id
        ?? states.find(s => s.isFallback)?.id ?? null;
    }
    const stateText = (this.form.get('state')?.value as string ?? '').trim().toLowerCase();
    const exact = states.find(s => s.name.toLowerCase() === stateText);
    if (exact) return exact.id;
    return states.find(s => s.isFallback)?.id ?? null;
  }

  private tryLoadFeeGuideline(): void {
    const v = this.form.getRawValue();
    const feeStateId = this.resolveFeeStateId();
    if (!v.programId || !v.admissionQuota || !feeStateId || !v.gender || !v.studentType) {
      this.totalFees.set(0);
      this.feeNotFound.set(false);
      return;
    }
    if (this.courses().length > 0 && !v.courseId) {
      // Program has courses but none selected yet — don't show a misleading "not found"
      this.totalFees.set(0);
      this.feeNotFound.set(false);
      return;
    }
    const params = new URLSearchParams({
      programId:   v.programId.toString(),
      quota:       v.admissionQuota,
      feeStateId:  feeStateId.toString(),
      gender:      v.gender,
      studentType: v.studentType,
    });
    if (v.courseId) params.set('courseId', v.courseId.toString());

    this.feeLoading.set(true);
    this.feeNotFound.set(false);
    this.http.get<FeeGuidelineResponse>(`${environment.apiUrl}/fee-structures/guideline?${params.toString()}`).subscribe({
      next: (data) => {
        this.feeLoading.set(false);
        this.totalFees.set(data.totalFee);
        this._applyFeeItems(data.items);
      },
      error: (err) => {
        this.feeLoading.set(false);
        if (err.status === 404) {
          this.feeNotFound.set(true);
          this.totalFees.set(0);
        } else {
          this.totalFees.set(0);
        }
      },
    });
  }

  /** Builds yearWiseFees from backend-filtered items (backend handles student-type filtering). */
  private _applyFeeItems(items: FeeStructureItem[]): void {
    if (!items.length) {
      this.yearWiseFees.set('');
      return;
    }
    const yearMap = new Map<number, number>();
    for (const item of items) {
      for (const ya of item.yearAmounts ?? []) {
        yearMap.set(ya.yearNumber, (yearMap.get(ya.yearNumber) ?? 0) + this.amountToPaise(ya.amount));
      }
    }
    if (yearMap.size > 0) {
      const sorted = Array.from(yearMap.entries())
        .sort(([a], [b]) => a - b)
        .map(([yearNumber, amount]) => ({ yearNumber, amount: this.paiseToAmount(amount) }));
      this.yearWiseFees.set(JSON.stringify(sorted));
    } else {
      this.yearWiseFees.set('');
    }
  }
  protected updateCourseValidator(): void {
    const ctrl = this.form.get('courseId');
    if (this.courses().length > 0) {
      ctrl?.setValidators(Validators.required);
    } else {
      ctrl?.clearValidators();
      ctrl?.setValue(null);
      // Program has no courses — fee lookup can proceed with courseId = null
      this.tryLoadFeeGuideline();
    }
    ctrl?.updateValueAndValidity({ emitEvent: false });
  }
  private loadCoursesForProgram(programId: number): void {
    this.http.get<CourseInfo[]>(`${environment.apiUrl}/courses/program/${programId}`).subscribe({
      next: (data) => { this.courses.set(data); this.updateCourseValidator(); },
      error: () => { this.courses.set([]); this.updateCourseValidator(); },
    });
  }
  protected selectedReferralType(): ReferralType | undefined {
    const rtId = this.form.get('referralTypeId')?.value;
    if (!rtId) return undefined;
    return this.referralTypes().find((r) => r.id === rtId);
  }
  protected onReferralTypeChange(referralTypeId: number): void {
    // Reset all person selectors
    this.form.patchValue({ agentId: null, referredStudentId: null, referredFacultyId: null, referredStaffName: null });
    this.personSearchTerm.set('');
    this.personSearchOpen.set(false);
    this.agentSearchTerm.set('');
    this.agentSearchOpen.set(false);
    if (!referralTypeId) {
      this.referralAdditionalAmount.set(0);
      this.referralCategory.set('NONE');
      this.updateAgentValidator(false);
      this.updateStaffNameValidator(false);
      return;
    }
    const rt = this.referralTypes().find((r) => r.id === referralTypeId);
    this.referralAdditionalAmount.set(rt?.hasCommission ? (rt?.commissionAmount ?? 0) : 0);
    const code = rt?.code ?? '';
    if (code === 'AGENT_REFERRAL') {
      this.referralCategory.set('AGENT');
      this.updateAgentValidator(true);
      this.updateStaffNameValidator(false);
    } else if (code === 'STUDENT') {
      this.referralCategory.set('STUDENT');
      this.updateAgentValidator(false);
      this.updateStaffNameValidator(false);
      this.loadStudentList();
    } else if (code === 'ALUMNI') {
      this.referralCategory.set('ALUMNI');
      this.updateAgentValidator(false);
      this.updateStaffNameValidator(false);
      this.loadAlumniList();
    } else if (code === 'FACULTY') {
      this.referralCategory.set('FACULTY');
      this.updateAgentValidator(false);
      this.updateStaffNameValidator(false);
      this.loadFacultyList();
    } else if (code === 'STAFF') {
      this.referralCategory.set('STAFF');
      this.updateAgentValidator(false);
      this.updateStaffNameValidator(true);
    } else {
      this.referralCategory.set('NONE');
      this.updateAgentValidator(false);
      this.updateStaffNameValidator(false);
    }
  }
  protected updateAgentValidator(required: boolean): void {
    const ctrl = this.form.get('agentId');
    if (required) {
      ctrl?.setValidators(Validators.required);
    } else {
      ctrl?.clearValidators();
    }
    ctrl?.updateValueAndValidity({ emitEvent: false });
  }
  protected updateStaffNameValidator(required: boolean): void {
    const ctrl = this.form.get('referredStaffName');
    if (required) {
      ctrl?.setValidators([Validators.required, Validators.maxLength(255)]);
    } else {
      ctrl?.clearValidators();
    }
    ctrl?.updateValueAndValidity({ emitEvent: false });
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
    // Clear the stored agentId when user types (until they pick from the list)
    this.form.patchValue({ agentId: null }, { emitEvent: false });
  }
  protected onAgentChange(agentId: number | null): void {
    const rt = this.selectedReferralType();
    if (!rt) return;
    if (agentId === null || agentId === undefined) {
      this.referralAdditionalAmount.set(rt?.hasCommission ? (rt?.commissionAmount ?? 0) : 0);
      return;
    }
    const agent = this.agents().find((a) => a.id === agentId);
    const commission = agent?.commissionAmount != null && Number(agent.commissionAmount) > 0
      ? Number(agent.commissionAmount)
      : (rt?.hasCommission ? (rt?.commissionAmount ?? 0) : 0);
    this.referralAdditionalAmount.set(commission);
  }
  protected selectPerson(id: number, name: string, type: 'STUDENT' | 'ALUMNI' | 'FACULTY'): void {
    if (type === 'STUDENT' || type === 'ALUMNI') {
      this.form.patchValue({ referredStudentId: id });
    } else {
      this.form.patchValue({ referredFacultyId: id });
    }
    this.personSearchTerm.set(name);
    this.personSearchOpen.set(false);
  }
  protected clearPerson(): void {
    this.form.patchValue({ referredStudentId: null, referredFacultyId: null });
    this.personSearchTerm.set('');
  }
  protected onPersonSearchInput(event: Event): void {
    this.personSearchTerm.set((event.target as HTMLInputElement).value);
    this.personSearchOpen.set(true);
  }
  private loadStudentList(): void {
    if (this.studentList().length > 0) return;
    this.http.get<{ id: number; fullName: string; rollNumber: string }[]>(
      `${environment.apiUrl}/students?activeOnly=true`
    ).subscribe({
      next: (data) => this.studentList.set(data),
      error: () => {},
    });
  }
  private loadAlumniList(): void {
    if (this.alumniList().length > 0) return;
    this.http.get<{ id: number; fullName: string; rollNumber: string }[]>(
      `${environment.apiUrl}/students`
    ).subscribe({
      next: (data) => this.alumniList.set(data),
      error: () => {},
    });
  }
  private loadFacultyList(): void {
    if (this.facultyList().length > 0) return;
    this.http.get<{ id: number; fullName: string; employeeCode: string }[]>(
      `${environment.apiUrl}/faculty`
    ).subscribe({
      next: (data) => this.facultyList.set(data),
      error: () => {},
    });
  }
  protected onSubmit(): void {
    if (this.form.invalid) { scrollToFirstInvalid(this.form); return; }
    if (this.ageRestrictionError()) { return; }
    if (this.feeNotFound()) { this.feeError.set(true); return; }
    if (this.totalFees() <= 0) { this.feeError.set(true); return; }
    this.feeError.set(false);
    const v = this.form.value;
    const request: EnquiryRequest = {
      name: v.name.trim(), email: v.email || undefined, phone: v.phone || undefined,
      programId: v.programId || undefined, courseId: v.courseId || undefined,
      enquiryDate: v.enquiryDate, referralTypeId: v.referralTypeId,
      status: this.isEditMode() ? (this.form.getRawValue().status ?? undefined) : undefined, agentId: v.agentId || undefined,
      remarks: v.remarks || undefined,
      referralAdditionalAmount: this.referralAdditionalAmount() || undefined,
      studentType: v.studentType || undefined,
      yearWiseFees: this.yearWiseFees() || undefined,
      countryId: v.country || undefined,
      state: v.state?.trim() || undefined,
      district: v.district?.trim() || undefined,
      referredStudentId: v.referredStudentId ?? undefined,
      referredFacultyId: v.referredFacultyId ?? undefined,
      referredStaffName: v.referredStaffName?.trim() || undefined,
      dateOfBirth: v.dateOfBirth,
      gender: v.gender,
      admissionQuota: v.admissionQuota || undefined,
      feeStateId: this.resolveFeeStateId() ?? undefined,
    };
    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.enquiryService.updateEnquiry(this.itemId!, request)
      : this.enquiryService.createEnquiry(request);
    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Updated' : 'Created');
        void this.router.navigate(['/enquiries']);
      },
      error: () => { this.toast.error('Failed to save'); this.saving.set(false); },
    });
  }
  private amountToPaise(value: number | null | undefined): number {
    return Math.round((Number(value) || 0) * 100);
  }
  private paiseToAmount(value: number): number {
    return value / 100;
  }
  /** Calculate completed age in years from a YYYY-MM-DD date string. Returns null if invalid. */
  private calcAgeFromDob(dob: string | null | undefined): number | null {
    if (!dob) return null;
    const birth = new Date(dob);
    if (isNaN(birth.getTime())) return null;
    const today = new Date();
    let age = today.getFullYear() - birth.getFullYear();
    const mDiff = today.getMonth() - birth.getMonth();
    if (mDiff < 0 || (mDiff === 0 && today.getDate() < birth.getDate())) age--;
    return age < 0 ? null : age;
  }
  /** Approximate DOB from age: today minus N years (same month/day). Returns YYYY-MM-DD or ''. */
  private calcDobFromAge(age: number | null | undefined): string {
    if (age == null || age < 0 || age > 150) return '';
    const d = new Date();
    d.setFullYear(d.getFullYear() - age);
    return d.toISOString().split('T')[0];
  }

  private recomputeAgeRestrictionError(): void {
    const program = this.selectedProgram();
    if (!program?.minimumAgeYears || !program?.ageCutoffDay || !program?.ageCutoffMonth) {
      this.ageRestrictionError.set(null);
      return;
    }
    const dobStr = this.form.get('dateOfBirth')?.value as string | null;
    const enquiryDateStr = this.form.get('enquiryDate')?.value as string | null;
    if (!dobStr || !enquiryDateStr) {
      this.ageRestrictionError.set(null);
      return;
    }
    const dob = new Date(dobStr);
    const refYear = new Date(enquiryDateStr).getFullYear();
    const cutoff = new Date(refYear, program.ageCutoffMonth - 1, program.ageCutoffDay);
    let age = refYear - dob.getFullYear();
    const mDiff = cutoff.getMonth() - dob.getMonth();
    if (mDiff < 0 || (mDiff === 0 && cutoff.getDate() < dob.getDate())) age--;
    if (age < program.minimumAgeYears) {
      const monthNames = ['January','February','March','April','May','June',
                          'July','August','September','October','November','December'];
      const cutoffStr = `${program.ageCutoffDay} ${monthNames[program.ageCutoffMonth - 1]} ${refYear}`;
      this.ageRestrictionError.set(
        `Student must be at least ${program.minimumAgeYears} years old as of ${cutoffStr}.`
      );
    } else {
      this.ageRestrictionError.set(null);
    }
  }
}

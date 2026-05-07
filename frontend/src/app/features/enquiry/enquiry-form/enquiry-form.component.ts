import { Component, computed, inject, OnInit, signal } from '@angular/core';
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
import { LayoutService } from '../../../core/layout/layout.service';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { ENQUIRY_FORM_TOUR } from '../../../shared/tour/tours/enquiry.tours';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';

interface ProgramInfo {
  id: number;
  name: string;
  code: string;
  durationYears: number;
  departments: { id: number; name: string }[];
}

interface CourseInfo {
  id: number;
  name: string;
  code: string;
  specialization: string | null;
  programId: number;
}

interface FeeStructureInfo {
  id: number;
  programId: number;
  programName: string;
  courseId: number | null;
  courseName: string | null;
  feeType: string;
  amount: number;
  description: string;
  isMandatory: boolean;
  isActive: boolean;
  yearAmounts: { yearNumber: number; yearLabel: string; amount: number }[];
}

@Component({
  selector: 'app-enquiry-form',
  standalone: true,
  imports: [
    InrPipe,
    RouterLink, ReactiveFormsModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule,
    CmsTourButtonComponent],
  templateUrl: './enquiry-form.component.html',
  styleUrl: './enquiry-form.component.scss',
})
export class EnquiryFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly enquiryService = inject(EnquiryService);
  private readonly agentService = inject(AgentService);
  private readonly referralTypeService = inject(ReferralTypeService);
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);
  protected readonly layoutService = inject(LayoutService);
  private readonly tourService = inject(TourService);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
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
  private readonly yearWiseFees = signal<string>('');
  protected readonly statusOptions = ['ENQUIRED', 'INTERESTED', 'NOT_INTERESTED', 'FEES_FINALIZED', 'FEES_PAID', 'PARTIALLY_PAID', 'DOCUMENTS_SUBMITTED', 'CONVERTED', 'CLOSED'];

  /** Max date for enquiry date input — today as YYYY-MM-DD string */
  protected readonly maxDateStr: string = new Date().toISOString().split('T')[0];

  /** Fee structures loaded for the selected program */
  protected readonly feeStructures = signal<FeeStructureInfo[]>([]);
  protected readonly selectedProgram = signal<ProgramInfo | null>(null);
  protected readonly totalFees = signal(0);
  protected readonly finalCalculatedFee = computed(() => this.totalFees());

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
    country:        ['India', Validators.required],
    state:          ['Tamil Nadu', Validators.required],
    district:       [''],
    referredStudentId: [null as number | null],
    referredFacultyId: [null as number | null],
    referredStaffName: [null as string | null],
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

  ngOnInit(): void {
    this.tourService.register('enquiry-form', ENQUIRY_FORM_TOUR);
    this.http.get<ProgramInfo[]>(`${environment.apiUrl}/programs`).subscribe({
      next: (data) => this.programs.set(data),
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
            country: item.country ?? 'India',
            state: item.state ?? 'Tamil Nadu',
            district: item.district ?? '',
            referredStudentId: item.referredStudentId ?? null,
            referredFacultyId: item.referredFacultyId ?? null,
            referredStaffName: item.referredStaffName ?? null,
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
            if (item.courseId) {
              this.loadFeeStructures(item.programId, item.courseId);
            }
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
    this.feeStructures.set([]);
    this.selectedProgram.set(null);
    this.totalFees.set(0);
    if (programId) {
      this.loadCoursesForProgram(programId);
      const program = this.programs().find((p) => p.id === programId) ?? null;
      this.selectedProgram.set(program);
    }
  }

  protected onCourseChange(courseId: number): void {
    const programId = this.form.get('programId')?.value;
    if (programId && courseId) {
      this.loadFeeStructures(programId, courseId);
    } else {
      this.feeStructures.set([]);
      this.totalFees.set(0);
    }
  }

  protected onStudentTypeChange(): void {
    // Recompute total fees based on new student type
    this.computeTotalFromFeeStructures(this.feeStructures());
  }

  private computeTotalFromFeeStructures(data: FeeStructureInfo[]): void {
    const studentType = this.form.get('studentType')?.value as 'DAY_SCHOLAR' | 'HOSTELER' | null;

    const relevant = data.filter((fs) => {
      if (fs.feeType === 'HOSTEL_FEE')    return studentType === 'HOSTELER';
      if (fs.feeType === 'TRANSPORT_FEE') return studentType === 'DAY_SCHOLAR';
      return true;
    });

    this.totalFees.set(this.paiseToAmount(
      relevant.reduce((s, fs) => s + this.amountToPaise(fs.amount), 0)
    ));

    // Build year-wise fee breakdown from yearAmounts on each fee structure item
    const yearMap = new Map<number, number>();
    for (const fs of relevant) {
      for (const ya of fs.yearAmounts ?? []) {
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
    }
    ctrl?.updateValueAndValidity({ emitEvent: false });
  }

  private loadCoursesForProgram(programId: number): void {
    this.http.get<CourseInfo[]>(`${environment.apiUrl}/courses/program/${programId}`).subscribe({
      next: (data) => { this.courses.set(data); this.updateCourseValidator(); },
      error: () => { this.courses.set([]); this.updateCourseValidator(); },
    });
  }

  private loadFeeStructures(programId: number, courseId: number): void {
    const url = `${environment.apiUrl}/fee-structures?programId=${programId}&courseId=${courseId}`;

    this.http.get<FeeStructureInfo[]>(url).subscribe({
      next: (data) => {
        this.feeStructures.set(data);
        this.computeTotalFromFeeStructures(data);
      },
      error: () => {
        this.feeStructures.set([]);
        this.totalFees.set(0);
      },
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
    if (this.totalFees() <= 0) { this.feeError.set(true); return; }
    this.feeError.set(false);
    const v = this.form.value;

    const request: EnquiryRequest = {
      name: v.name.trim(), email: v.email || undefined, phone: v.phone || undefined,
      programId: v.programId || undefined, courseId: v.courseId || undefined,
      enquiryDate: v.enquiryDate, referralTypeId: v.referralTypeId,
      status: this.isEditMode() ? v.status : undefined, agentId: v.agentId || undefined,
      remarks: v.remarks || undefined,
      feeGuidelineTotal: this.totalFees() || undefined,
      referralAdditionalAmount: undefined,
      finalCalculatedFee: this.finalCalculatedFee() || undefined,
      studentType: v.studentType || undefined,
      yearWiseFees: this.yearWiseFees() || undefined,
      country: v.country?.trim() || undefined,
      state: v.state?.trim() || undefined,
      district: v.district?.trim() || undefined,
      referredStudentId: v.referredStudentId ?? undefined,
      referredFacultyId: v.referredFacultyId ?? undefined,
      referredStaffName: v.referredStaffName?.trim() || undefined,
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
}

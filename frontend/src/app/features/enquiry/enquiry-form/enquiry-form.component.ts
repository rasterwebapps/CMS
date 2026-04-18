import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { CurrencyPipe } from '@angular/common';
import { EnquiryService } from '../enquiry.service';
import { EnquiryRequest } from '../enquiry.model';
import { Agent } from '../../agent/agent.model';
import { AgentService } from '../../agent/agent.service';
import { ReferralType } from '../../referral-type/referral-type.model';
import { ReferralTypeService } from '../../referral-type/referral-type.service';
import { environment } from '../../../../environments';

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
    RouterLink, ReactiveFormsModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, MatSnackBarModule, CurrencyPipe,
  ],
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
  private readonly snackBar = inject(MatSnackBar);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Enquiry');
  protected readonly programs = signal<ProgramInfo[]>([]);
  protected readonly courses = signal<CourseInfo[]>([]);
  protected readonly agents = signal<Agent[]>([]);
  protected readonly referralTypes = signal<ReferralType[]>([]);
  protected readonly referralAdditionalAmount = signal(0);
  protected readonly statusOptions = ['ENQUIRED', 'INTERESTED', 'NOT_INTERESTED', 'FEES_FINALIZED', 'FEES_PAID', 'PARTIALLY_PAID', 'DOCUMENTS_SUBMITTED', 'CONVERTED', 'CLOSED'];
  protected readonly studentTypeOptions: { value: 'DAY_SCHOLAR' | 'HOSTELER'; label: string }[] = [
    { value: 'DAY_SCHOLAR', label: 'Day Scholar' },
    { value: 'HOSTELER', label: 'Hosteler' },
  ];

  /** Max date for enquiry date input — today as YYYY-MM-DD string */
  protected readonly maxDateStr: string = new Date().toISOString().split('T')[0];

  /** Fee structures loaded for the selected program */
  protected readonly feeStructures = signal<FeeStructureInfo[]>([]);
  protected readonly selectedProgram = signal<ProgramInfo | null>(null);
  protected readonly totalFees = signal(0);
  protected readonly finalCalculatedFee = computed(() => this.totalFees() + this.referralAdditionalAmount());

  private itemId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    email: [''],
    phone: [''],
    programId: [null as number | null],
    courseId: [null as number | null],
    enquiryDate: [this.maxDateStr, Validators.required],
    referralTypeId: [null as number | null, Validators.required],
    status: ['ENQUIRED'],
    agentId: [null as number | null],
    remarks: [''],
    studentType: [null as 'DAY_SCHOLAR' | 'HOSTELER' | null],
  });

  ngOnInit(): void {
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
            studentType: item.studentType ?? null,
          });
          if (item.referralTypeId) {
            this.onReferralTypeChange(item.referralTypeId);
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
          this.snackBar.open('Failed to load', 'Close', { duration: 3000 });
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
    // Generic fees exclude HOSTEL_FEE and TRANSPORT_FEE
    const genericTotal = data
      .filter((fs) => fs.feeType !== 'HOSTEL_FEE' && fs.feeType !== 'TRANSPORT_FEE')
      .reduce((sum, fs) => sum + fs.amount, 0);
    let additionalFee = 0;
    if (studentType === 'DAY_SCHOLAR') {
      additionalFee = data
        .filter((fs) => fs.feeType === 'TRANSPORT_FEE')
        .reduce((sum, fs) => sum + fs.amount, 0);
    } else if (studentType === 'HOSTELER') {
      additionalFee = data
        .filter((fs) => fs.feeType === 'HOSTEL_FEE')
        .reduce((sum, fs) => sum + fs.amount, 0);
    }
    this.totalFees.set(genericTotal + additionalFee);
  }

  private loadCoursesForProgram(programId: number): void {
    this.http.get<CourseInfo[]>(`${environment.apiUrl}/courses/program/${programId}`).subscribe({
      next: (data) => this.courses.set(data),
      error: () => this.courses.set([]),
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

  protected isAgentReferral(): boolean {
    const rtId = this.form.get('referralTypeId')?.value;
    if (!rtId) return false;
    const rt = this.referralTypes().find((r) => r.id === rtId);
    return rt?.code === 'AGENT_REFERRAL';
  }

  protected selectedReferralType(): ReferralType | undefined {
    const rtId = this.form.get('referralTypeId')?.value;
    if (!rtId) return undefined;
    return this.referralTypes().find((r) => r.id === rtId);
  }

  protected onReferralTypeChange(referralTypeId: number): void {
    if (!referralTypeId) {
      this.referralAdditionalAmount.set(0);
      return;
    }
    const rt = this.referralTypes().find((r) => r.id === referralTypeId);
    this.referralAdditionalAmount.set(rt?.hasCommission ? (rt?.commissionAmount ?? 0) : 0);
  }

  protected onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.value;

    const request: EnquiryRequest = {
      name: v.name.trim(), email: v.email || undefined, phone: v.phone || undefined,
      programId: v.programId || undefined, courseId: v.courseId || undefined,
      enquiryDate: v.enquiryDate, referralTypeId: v.referralTypeId,
      status: this.isEditMode() ? v.status : undefined, agentId: v.agentId || undefined,
      remarks: v.remarks || undefined,
      feeGuidelineTotal: this.totalFees() || undefined,
      referralAdditionalAmount: this.referralAdditionalAmount() || undefined,
      finalCalculatedFee: this.finalCalculatedFee() || undefined,
      studentType: v.studentType || undefined,
    };
    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.enquiryService.updateEnquiry(this.itemId!, request)
      : this.enquiryService.createEnquiry(request);
    op$.subscribe({
      next: () => {
        this.snackBar.open(this.isEditMode() ? 'Updated' : 'Created', 'Close', { duration: 3000 });
        void this.router.navigate(['/enquiries']);
      },
      error: () => { this.snackBar.open('Failed to save', 'Close', { duration: 3000 }); this.saving.set(false); },
    });
  }
}

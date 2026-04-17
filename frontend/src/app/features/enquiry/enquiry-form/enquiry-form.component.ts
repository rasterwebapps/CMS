import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
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
    RouterLink, ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatCardModule, MatProgressSpinnerModule, MatSnackBarModule,
    MatDatepickerModule, MatNativeDateModule, CurrencyPipe,
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

  /** Max date for enquiry date picker — today */
  protected readonly maxDate = new Date();

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
    enquiryDate: [new Date(), Validators.required],
    referralTypeId: [null as number | null, Validators.required],
    status: ['ENQUIRED'],
    agentId: [null as number | null],
    remarks: [''],
    feeDiscussedAmount: [null as number | null],
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
            enquiryDate: item.enquiryDate ? new Date(item.enquiryDate + 'T00:00:00') : new Date(),
            referralTypeId: item.referralTypeId, status: item.status,
            agentId: item.agentId,
            remarks: item.remarks,
            feeDiscussedAmount: item.feeDiscussedAmount,
            studentType: item.studentType ?? null,
          });
          if (item.referralTypeId) {
            this.onReferralTypeChange(item.referralTypeId);
          }
          if (item.programId) {
            this.loadCoursesForProgram(item.programId);
            this.loadFeeStructures(item.programId, item.courseId ?? undefined);
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
    if (programId) {
      this.loadCoursesForProgram(programId);
      this.loadFeeStructures(programId);
    } else {
      this.feeStructures.set([]);
      this.selectedProgram.set(null);
      this.totalFees.set(0);
    }
  }

  protected onCourseChange(courseId: number): void {
    const programId = this.form.get('programId')?.value;
    if (programId) {
      this.loadFeeStructures(programId, courseId ?? undefined);
    }
  }

  protected onStudentTypeChange(): void {
    // Recompute total fees based on new student type
    this.computeTotalFromFeeStructures(this.feeStructures());
  }

  private computeTotalFromFeeStructures(data: FeeStructureInfo[]): void {
    const studentType = this.form.get('studentType')?.value as 'DAY_SCHOLAR' | 'HOSTELER' | null;
    let filtered = data;
    if (studentType === 'DAY_SCHOLAR') {
      filtered = data.filter((fs) => fs.feeType !== 'HOSTEL_FEE');
    } else if (studentType === 'HOSTELER') {
      filtered = data.filter((fs) => fs.feeType !== 'TRANSPORT_FEE');
    }
    const total = filtered.reduce((sum, fs) => sum + fs.amount, 0);
    this.totalFees.set(total);
  }

  private loadCoursesForProgram(programId: number): void {
    this.http.get<CourseInfo[]>(`${environment.apiUrl}/courses?programId=${programId}`).subscribe({
      next: (data) => this.courses.set(data),
      error: () => this.courses.set([]),
    });
  }

  private loadFeeStructures(programId: number, courseId?: number): void {
    const program = this.programs().find((p) => p.id === programId) ?? null;
    this.selectedProgram.set(program);

    let url = `${environment.apiUrl}/fee-structures?programId=${programId}`;
    if (courseId) {
      url += `&courseId=${courseId}`;
    }

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

    // Format date to YYYY-MM-DD string
    let dateStr = '';
    if (v.enquiryDate instanceof Date) {
      const d = v.enquiryDate;
      dateStr = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    } else {
      dateStr = v.enquiryDate;
    }

    const request: EnquiryRequest = {
      name: v.name.trim(), email: v.email || undefined, phone: v.phone || undefined,
      programId: v.programId || undefined, courseId: v.courseId || undefined,
      enquiryDate: dateStr, referralTypeId: v.referralTypeId,
      status: this.isEditMode() ? v.status : undefined, agentId: v.agentId || undefined,
      remarks: v.remarks || undefined,
      feeDiscussedAmount: v.feeDiscussedAmount || undefined,
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

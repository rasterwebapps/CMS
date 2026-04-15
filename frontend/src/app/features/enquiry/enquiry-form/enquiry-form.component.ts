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
  degreeType: string;
  durationYears: number;
  department: { id: number; name: string };
}

interface FeeStructureInfo {
  id: number;
  programId: number;
  programName: string;
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
  protected readonly agents = signal<Agent[]>([]);
  protected readonly referralTypes = signal<ReferralType[]>([]);
  protected readonly referralAdditionalAmount = signal(0);
  protected readonly sources = ['WALK_IN', 'PHONE', 'ONLINE', 'AGENT_REFERRAL', 'STAFF', 'ALUMNI', 'PARENT', 'ADVERTISEMENT'];
  protected readonly statusOptions = ['ENQUIRED', 'INTERESTED', 'NOT_INTERESTED', 'FEES_FINALIZED', 'FEES_PAID', 'PARTIALLY_PAID', 'DOCUMENTS_SUBMITTED', 'CONVERTED', 'CLOSED'];

  /** Max date for enquiry date picker — today */
  protected readonly maxDate = new Date();

  /** Fee structures loaded for the selected program */
  protected readonly feeStructures = signal<FeeStructureInfo[]>([]);
  protected readonly selectedProgram = signal<ProgramInfo | null>(null);
  protected readonly totalFees = signal(0);
  protected readonly yearWiseFees = signal<number[]>([]);
  protected readonly finalCalculatedFee = computed(() => this.totalFees() + this.referralAdditionalAmount());

  private itemId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    email: [''],
    phone: [''],
    programId: [null as number | null],
    enquiryDate: [new Date(), Validators.required],
    source: ['', Validators.required],
    status: ['ENQUIRED'],
    agentId: [null as number | null],
    referralTypeId: [null as number | null],
    assignedTo: [''],
    remarks: [''],
    feeDiscussedAmount: [null as number | null],
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
            enquiryDate: item.enquiryDate ? new Date(item.enquiryDate + 'T00:00:00') : new Date(),
            source: item.source, status: item.status,
            agentId: item.agentId, referralTypeId: item.referralTypeId,
            assignedTo: item.assignedTo, remarks: item.remarks,
            feeDiscussedAmount: item.feeDiscussedAmount,
          });
          if (item.referralTypeId) {
            this.onReferralTypeChange(item.referralTypeId);
          }
          if (item.programId) {
            this.loadFeeStructures(item.programId);
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
    if (programId) {
      this.loadFeeStructures(programId);
    } else {
      this.feeStructures.set([]);
      this.selectedProgram.set(null);
      this.totalFees.set(0);
      this.yearWiseFees.set([]);
    }
  }

  private loadFeeStructures(programId: number): void {
    const program = this.programs().find((p) => p.id === programId) ?? null;
    this.selectedProgram.set(program);

    this.http.get<FeeStructureInfo[]>(`${environment.apiUrl}/fee-structures?programId=${programId}`).subscribe({
      next: (data) => {
        this.feeStructures.set(data);
        const total = data.reduce((sum, fs) => sum + fs.amount, 0);
        this.totalFees.set(total);

        // Build year-wise split: prefer yearAmounts from fee structures if available
        const years = program?.durationYears ?? 1;
        const yearAmountsMap = new Map<number, number>();
        let hasYearAmounts = false;
        for (const fs of data) {
          if (fs.yearAmounts && fs.yearAmounts.length > 0) {
            hasYearAmounts = true;
            for (const ya of fs.yearAmounts) {
              yearAmountsMap.set(ya.yearNumber, (yearAmountsMap.get(ya.yearNumber) ?? 0) + ya.amount);
            }
          }
        }

        if (hasYearAmounts) {
          const splits: number[] = [];
          for (let i = 1; i <= years; i++) {
            splits.push(yearAmountsMap.get(i) ?? 0);
          }
          this.yearWiseFees.set(splits);
        } else {
          const perYear = years > 0 ? total / years : total;
          const splits: number[] = [];
          for (let i = 0; i < years; i++) {
            splits.push(Math.round(perYear * 100) / 100);
          }
          this.yearWiseFees.set(splits);
        }
      },
      error: () => {
        this.feeStructures.set([]);
        this.totalFees.set(0);
        this.yearWiseFees.set([]);
      },
    });
  }

  protected isAgentReferral(): boolean {
    return this.form.get('source')?.value === 'AGENT_REFERRAL';
  }

  protected onReferralTypeChange(referralTypeId: number): void {
    if (!referralTypeId) {
      this.referralAdditionalAmount.set(0);
      return;
    }
    const rt = this.referralTypes().find((r) => r.id === referralTypeId);
    this.referralAdditionalAmount.set(rt?.guidelineValue ?? 0);
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

    const yearWiseFeesJson = this.yearWiseFees().length > 0
      ? JSON.stringify(this.yearWiseFees().map((amount, i) => ({ yearNumber: i + 1, amount })))
      : undefined;

    const request: EnquiryRequest = {
      name: v.name.trim(), email: v.email || undefined, phone: v.phone || undefined,
      programId: v.programId || undefined, enquiryDate: dateStr, source: v.source,
      status: this.isEditMode() ? v.status : undefined, agentId: v.agentId || undefined,
      referralTypeId: v.referralTypeId || undefined,
      assignedTo: v.assignedTo || undefined, remarks: v.remarks || undefined,
      feeDiscussedAmount: v.feeDiscussedAmount || undefined,
      feeGuidelineTotal: this.totalFees() || undefined,
      referralAdditionalAmount: this.referralAdditionalAmount() || undefined,
      finalCalculatedFee: this.finalCalculatedFee() || undefined,
      yearWiseFees: yearWiseFeesJson,
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

import { Component, computed, effect, inject, OnInit, signal, ViewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatTooltipModule } from '@angular/material/tooltip';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { forkJoin } from 'rxjs';
import { EnquiryService } from '../../enquiry/enquiry.service';
import { FinanceService } from '../finance.service';
import { Enquiry, EnquiryPaymentRequest, EnquiryYearWiseFeeStatusResponse } from '../../enquiry/enquiry.model';
import { StudentFeeSummary, InstallmentFeeDetail, ReceiptDisplayData } from '../finance.model';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { ToastService } from '../../../core/toast/toast.service';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';
import { getPaymentModeLabel, PAYMENT_MODES } from '../../../shared/utils/payment-mode.utils';
import { CashDenominationComponent } from '../../../shared/cash-denomination/cash-denomination.component';
import { FeeReceiptDialogComponent } from '../../../shared/fee-receipt-dialog/fee-receipt-dialog.component';
import { transactionReferenceRequiredValidator } from '../../../shared/validators/transaction-reference-validator';

export type FilterType   = 'ALL' | 'ENQUIRY' | 'STUDENT';
export type FilterStatus = 'ALL' | 'OVERDUE' | 'OUTSTANDING';
export type PersonType   = 'ENQUIRY' | 'STUDENT';

export interface FeeEntry {
  type: PersonType;
  id: number;
  name: string;
  programName: string;
  courseName: string | null;
  totalFee: number;
  totalPaid: number;
  totalOutstanding: number;
  nextDueDate: string | null;
  nextDueLabel: string | null;
}

@Component({
  selector: 'app-fee-collection',
  standalone: true,
  imports: [
    AppDatePipe,
    InrPipe,
    ReactiveFormsModule,
    MatIconModule, MatProgressSpinnerModule,
    MatTableModule, MatPaginatorModule, MatSortModule, MatTooltipModule,
    CmsEmptyStateComponent,
    CashDenominationComponent,
    FeeReceiptDialogComponent,
  ],
  templateUrl: './fee-collection.component.html',
  styleUrl: './fee-collection.component.scss',
})
export class FeeCollectionComponent implements OnInit {
  private readonly route          = inject(ActivatedRoute);
  private readonly enquiryService = inject(EnquiryService);
  private readonly financeService = inject(FinanceService);
  private readonly toast          = inject(ToastService);
  private readonly fb             = inject(FormBuilder);

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  protected readonly loading          = signal(true);
  protected readonly feeEntries       = signal<FeeEntry[]>([]);
  protected readonly selectedEntry    = signal<FeeEntry | null>(null);
  protected readonly feeStatus        = signal<EnquiryYearWiseFeeStatusResponse | null>(null);
  protected readonly studentSemesters = signal<InstallmentFeeDetail[]>([]);
  protected readonly saving           = signal(false);
  protected readonly denominationValid = signal(false);
  protected readonly receipt          = signal<ReceiptDisplayData | null>(null);

  protected readonly searchTerm   = signal('');
  protected readonly filterType   = signal<FilterType>('ALL');
  protected readonly filterStatus = signal<FilterStatus>('ALL');

  protected readonly displayedColumns = [
    'name', 'type', 'programName', 'totalFee', 'totalPaid', 'totalOutstanding', 'nextDueDate', 'actions',
  ];
  protected readonly dataSource = new MatTableDataSource<FeeEntry>([]);

  protected readonly filteredEntries = computed(() => {
    const term   = this.searchTerm().toLowerCase().trim();
    const type   = this.filterType();
    const status = this.filterStatus();
    const today  = new Date();

    return this.feeEntries().filter(e => {
      if (term && !e.name.toLowerCase().includes(term)) return false;
      if (type !== 'ALL' && e.type !== type) return false;
      if (status === 'OVERDUE'     && !(e.nextDueDate && new Date(e.nextDueDate) < today)) return false;
      return status !== 'OUTSTANDING' || e.totalOutstanding > 0;
    });
  });

  protected readonly totalOutstandingSum = computed(() =>
    this.feeEntries().reduce((s, e) => s + e.totalOutstanding, 0)
  );

  protected readonly paymentModes = PAYMENT_MODES;
  protected readonly getPaymentModeLabel = getPaymentModeLabel;

  protected readonly form: FormGroup = this.fb.group({
    amount:               [null, [Validators.required, Validators.min(1)]],
    paymentDate:          ['', Validators.required],
    paymentMode:          ['', Validators.required],
    transactionReference: ['', [transactionReferenceRequiredValidator('paymentMode')]],
    remarks:              [''],
  });

  protected readonly semesterRows = computed<Array<{
    label: string; fee: number; paid: number; outstanding: number;
    dueDate: string | null; isPaid: boolean; isNext: boolean;
  }>>(() => {
    const entry = this.selectedEntry();
    if (!entry) return [];

    if (entry.type === 'ENQUIRY') {
      const fs = this.feeStatus();
      if (!fs) return [];
      const sems = fs.installmentBreakdown;
      return sems.map((s, i) => ({
        label:       s.installmentLabel,
        fee:         s.allocatedFee,
        paid:        s.paidAmount,
        outstanding: s.outstanding,
        dueDate:     s.dueDate,
        isPaid:      s.outstanding === 0,
        isNext:      s.outstanding > 0 && (i === 0 || sems[i - 1].outstanding === 0),
      }));
    } else {
      const sems = this.studentSemesters();
      return sems.map((s, i) => ({
        label:       s.installmentLabel,
        fee:         s.amount,
        paid:        s.amountPaid,
        outstanding: s.pendingAmount,
        dueDate:     s.dueDate,
        isPaid:      s.pendingAmount === 0,
        isNext:      s.pendingAmount > 0 && (i === 0 || sems[i - 1].pendingAmount === 0),
      }));
    }
  });

  protected readonly totalFee         = computed(() => this.semesterRows().reduce((s, r) => s + r.fee, 0));
  protected readonly totalPaid        = computed(() => this.semesterRows().reduce((s, r) => s + r.paid, 0));
  protected readonly totalOutstanding = computed(() => this.semesterRows().reduce((s, r) => s + r.outstanding, 0));

  protected get hasActiveFilters(): boolean {
    return !!this.searchTerm() || this.filterType() !== 'ALL' || this.filterStatus() !== 'ALL';
  }

  constructor() {
    effect(() => {
      this.dataSource.data = this.filteredEntries();
      if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
    });

    // Trigger validation update when payment mode changes
    this.form.get('paymentMode')?.valueChanges.subscribe(() => {
      this.form.get('transactionReference')?.updateValueAndValidity();
    });
  }

  ngOnInit(): void {
    this.loadAll(() => this.applyDeepLink());
  }

  private loadAll(onComplete?: () => void): void {
    this.loading.set(true);
    forkJoin({
      enquiries: this.enquiryService.getEnquiries(),
      students:  this.financeService.searchStudentFees(),
    }).subscribe({
      next: ({ enquiries, students }) => {
        const enquiryEntries: FeeEntry[] = enquiries
          .filter(e => this.canCollectEnquiryBalance(e))
          .map(e => this.enquiryToEntry(e));

        const studentEntries: FeeEntry[] = (students.students ?? [])
          .filter(s => s.totalPending > 0)
          .map(s => this.studentToEntry(s));

        const all = [...enquiryEntries, ...studentEntries].sort((a, b) => {
          if (!a.nextDueDate && !b.nextDueDate) return a.name.localeCompare(b.name);
          if (!a.nextDueDate) return 1;
          if (!b.nextDueDate) return -1;
          return a.nextDueDate.localeCompare(b.nextDueDate);
        });

        this.feeEntries.set(all);
        this.loading.set(false);
        onComplete?.();
      },
      error: () => {
        this.toast.error('Failed to load fee data');
        this.loading.set(false);
      },
    });
  }

  private applyDeepLink(): void {
    const param = this.route.snapshot.queryParamMap.get('enquiryId');
    if (!param) return;
    const id = Number(param);
    const entry = this.feeEntries().find(e => e.type === 'ENQUIRY' && e.id === id);
    if (entry) {
      this.selectEntry(entry);
    } else {
      this.toast.info('This enquiry is not eligible for balance collection');
    }
  }

  private canCollectEnquiryBalance(enquiry: Enquiry): boolean {
    const blockedStatuses = ['NOT_INTERESTED', 'CANCELLED', 'CLOSED', 'ADMITTED', 'CONVERTED'];
    return enquiry.finalizedNetFee !== null && enquiry.finalizedNetFee !== undefined &&
      !blockedStatuses.includes(enquiry.status);
  }

  private enquiryToEntry(e: Enquiry): FeeEntry {
    const totalFee = e.finalizedNetFee ?? 0;
    const totalPaid = e.totalPaidAmount ?? 0;
    return {
      type: 'ENQUIRY', id: e.id, name: e.name,
      programName: e.programName ?? '—', courseName: e.courseName,
      totalFee, totalPaid,
      totalOutstanding: Math.max(0, totalFee - totalPaid),
      nextDueDate: null, nextDueLabel: null,
    };
  }

  private studentToEntry(s: StudentFeeSummary): FeeEntry {
    return {
      type: 'STUDENT', id: s.studentId, name: s.studentName,
      programName: s.programName ?? '—', courseName: null,
      totalFee: s.totalFee, totalPaid: s.totalPaid, totalOutstanding: s.totalPending,
      nextDueDate: null, nextDueLabel: null,
    };
  }

  protected selectEntry(entry: FeeEntry): void {
    this.selectedEntry.set(entry);
    this.feeStatus.set(null);
    this.studentSemesters.set([]);
    this.receipt.set(null);
    this.form.patchValue({ paymentDate: new Date().toISOString().split('T')[0] });

    if (entry.type === 'ENQUIRY') {
      this.enquiryService.getYearWiseFeeStatus(entry.id).subscribe({
        next: (fs) => {
          this.feeStatus.set(fs);
          const sems = fs.installmentBreakdown;
          const nextSem = sems.find((s, i) => s.outstanding > 0 && (i === 0 || sems[i - 1].outstanding === 0));
          const prefill = nextSem ? nextSem.outstanding : fs.totalOutstanding;
          if (!this.form.get('amount')?.value) {
            this.form.patchValue({ amount: prefill > 0 ? prefill : null });
          }
        },
        error: () => {
          this.form.patchValue({ amount: entry.totalOutstanding > 0 ? entry.totalOutstanding : null });
        },
      });
    } else {
      this.financeService.getFeeAllocationStatus(entry.id).subscribe({
        next: (alloc) => {
          this.studentSemesters.set(alloc.installmentFees);
          const sems = alloc.installmentFees;
          const nextSem = sems.find((s, i) => s.pendingAmount > 0 && (i === 0 || sems[i - 1].pendingAmount === 0));
          const prefill = nextSem ? nextSem.pendingAmount : sems.reduce((acc, sf) => acc + sf.pendingAmount, 0);
          if (!this.form.get('amount')?.value) {
            this.form.patchValue({ amount: prefill > 0 ? prefill : null });
          }
        },
        error: () => {
          this.form.patchValue({ amount: entry.totalOutstanding > 0 ? entry.totalOutstanding : null });
        },
      });
    }
  }

  protected isCashMode(): boolean {
    return this.form.get('paymentMode')?.value === 'CASH';
  }

  protected backToList(): void {
    this.selectedEntry.set(null);
    this.feeStatus.set(null);
    this.studentSemesters.set([]);
    this.receipt.set(null);
    this.denominationValid.set(false);
    this.form.reset();
  }

  protected onSubmit(): void {
    if (this.form.invalid) { scrollToFirstInvalid(this.form); return; }
    if (this.isCashMode() && !this.denominationValid()) return;
    const entry = this.selectedEntry();
    if (!entry) return;

    const v = this.form.value;
    this.saving.set(true);

    if (entry.type === 'ENQUIRY') {
      const req: EnquiryPaymentRequest = {
        amountPaid:           v.amount,
        paymentDate:          v.paymentDate,
        paymentMode:          v.paymentMode,
        transactionReference: v.transactionReference || undefined,
        remarks:              v.remarks || undefined,
      };
      this.enquiryService.collectPayment(entry.id, req).subscribe({
        next: (res) => {
          this.saving.set(false);
          const towardsLabel = res.feeCategory === 'TUITION_AND_HOSTEL'
            ? 'Tuition Fees And Hostel Fees' : 'Tuition Fees';
          this.receipt.set({
            receiptNumber:       res.receiptNumber,
            payerType:           'ENQUIRY',
            payerName:           res.enquiryName,
            payerIdentifier:     null,
            programName:         entry.courseName ?? (entry.programName !== '—' ? entry.programName : null),
            amountPaid:          Number(res.amountPaid),
            paymentDate:         String(res.paymentDate),
            paymentMode:         String(res.paymentMode),
            transactionReference: res.transactionReference,
            remarks:             res.remarks,
            feeCategory:         res.feeCategory,
            installmentsCovered: towardsLabel,
            installmentBreakdown: [{ label: towardsLabel, amount: Number(res.amountPaid) }],
          });
        },
        error: () => { this.toast.error('Failed to collect payment'); this.saving.set(false); },
      });
    } else {
      this.financeService.collectPayment(entry.id, {
        amount: v.amount, paymentDate: v.paymentDate, paymentMode: v.paymentMode,
        transactionReference: v.transactionReference || undefined,
        remarks: v.remarks || undefined,
      }).subscribe({
        next: (res) => {
          this.saving.set(false);
          this.receipt.set({
            receiptNumber:       res.receiptNumber,
            payerType:           'STUDENT',
            payerName:           res.studentName,
            payerIdentifier:     res.rollNumber,
            programName:         entry.programName !== '—' ? entry.programName : null,
            amountPaid:          Number(res.amountPaid),
            paymentDate:         String(res.paymentDate),
            paymentMode:         String(res.paymentMode),
            transactionReference: res.transactionReference,
            remarks:             res.remarks,
            installmentsCovered: res.installmentBreakdown.map(i => i.installmentLabel).join(', '),
            installmentBreakdown: res.installmentBreakdown.map(i => ({
              label: i.installmentLabel,
              amount: Number(i.amountApplied),
            })),
          });
        },
        error: () => { this.toast.error('Failed to collect payment'); this.saving.set(false); },
      });
    }
  }

  protected doneWithReceipt(): void {
    this.receipt.set(null);
    this.loadAll();
    this.backToList();
  }

  protected onSearch(event: Event): void {
    this.searchTerm.set((event.target as HTMLInputElement).value);
  }

  protected setFilterType(value: FilterType): void {
    this.filterType.set(value);
  }

  protected setFilterStatus(value: FilterStatus): void {
    this.filterStatus.set(value);
  }

  protected clearFilters(): void {
    this.searchTerm.set('');
    this.filterType.set('ALL');
    this.filterStatus.set('ALL');
  }

  protected isOverdue(dueDate: string | null): boolean {
    return !!dueDate && new Date(dueDate) < new Date();
  }

  protected isTransactionRefRequired(): boolean {
    const mode = this.form.get('paymentMode')?.value;
    return ['UPI', 'BANK_TRANSFER', 'CHEQUE', 'DEMAND_DRAFT'].includes(mode);
  }
}

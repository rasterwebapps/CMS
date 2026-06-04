import { Component, inject, OnInit, signal, computed, ViewChild, effect } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { PercentPipe } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { EnquiryService } from '../../enquiry/enquiry.service';
import { Enquiry, FeeFinalizationRequest } from '../../enquiry/enquiry.model';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { ToastService } from '../../../core/toast/toast.service';
import { environment } from '../../../../environments';

interface YearFeeRow {
  yearNumber: number;
  yearLabel: string;
  originalAmount: number;
  finalAmount: number;
}

interface Program { id: number; name: string; durationYears: number; }

@Component({
  selector: 'app-fee-finalization',
  standalone: true,
  imports: [
    InrPipe, PercentPipe,
    ReactiveFormsModule,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule,
    MatTableModule, MatTooltipModule,
    MatPaginatorModule, MatSortModule,
    CmsEmptyStateComponent,
  ],
  templateUrl: './fee-finalization.component.html',
  styleUrl: './fee-finalization.component.scss',
})
export class FeeFinalizationComponent implements OnInit {
  private readonly route    = inject(ActivatedRoute);
  private readonly http     = inject(HttpClient);
  private readonly enquiryService = inject(EnquiryService);
  private readonly toast    = inject(ToastService);

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  protected readonly loading          = signal(false);
  protected readonly saving           = signal(false);
  protected readonly selectedEnquiry  = signal<Enquiry | null>(null);
  protected readonly yearRows         = signal<YearFeeRow[]>([]);
  protected readonly globalDiscount   = signal<number>(0);
  protected readonly authoritativeProposedFeeByEnquiryId = signal<Record<number, number>>({});

  // ── List filters ────────────────────────────────────────────────────────────
  protected readonly searchValue        = signal('');
  protected readonly selectedProgramId  = signal<number | null>(null);
  protected readonly selectedQuota      = signal<'MANAGEMENT' | 'COUNSELLING' | null>(null);
  protected readonly programs           = signal<Program[]>([]);
  protected readonly allEnquiries       = signal<Enquiry[]>([]);

  protected readonly filteredEnquiries = computed(() => {
    const search = this.searchValue().toLowerCase().trim();
    const progId = this.selectedProgramId();
    const quota  = this.selectedQuota();
    return this.allEnquiries().filter(e => {
      if (progId != null && e.programId !== progId) return false;
      if (quota  != null && e.admissionQuota !== quota) return false;
      if (!search) return true;
      return (
        e.name.toLowerCase().includes(search) ||
        !!e.programName?.toLowerCase().includes(search) ||
        !!e.courseName?.toLowerCase().includes(search) ||
        (e.admissionQuota === 'MANAGEMENT' && 'management'.includes(search)) ||
        (e.admissionQuota === 'COUNSELLING' && 'counselling'.includes(search))
      );
    });
  });

  protected readonly discountReasonCtrl = new FormControl('');
  protected readonly discountReason = signal('');

  protected readonly displayedColumns = [
    'name', 'programName', 'courseName', 'quota', 'referralTypeName', 'finalCalculatedFee', 'actions',
  ];
  protected readonly dataSource = new MatTableDataSource<Enquiry>([]);

  // ── Derived totals ──────────────────────────────────────────────────────────
  protected readonly totalOriginal = computed(() =>
    this.paiseToAmount(this.yearRows().reduce((s, r) => s + this.amountToPaise(r.originalAmount), 0))
  );
  protected readonly totalFinal = computed(() =>
    this.paiseToAmount(this.yearRows().reduce((s, r) => s + this.amountToPaise(r.finalAmount), 0))
  );
  protected readonly totalDiscount = computed(() =>
    this.paiseToAmount(Math.max(0, this.amountToPaise(this.totalOriginal()) - this.amountToPaise(this.totalFinal())))
  );
  protected readonly discountPct = computed(() => {
    const orig = this.totalOriginal();
    return orig > 0 ? (this.totalDiscount() / orig) : 0;
  });
  protected readonly hasDiscount = computed(() => this.totalDiscount() > 0);

  protected readonly anyYearBelowZero = computed(() =>
    this.yearRows().some(r => r.finalAmount < 0)
  );
  protected readonly anyYearExceedsOriginal = computed(() =>
    this.yearRows().some(r => r.finalAmount > r.originalAmount)
  );
  protected readonly discountExceedsTotal = computed(() =>
    this.globalDiscount() > this.totalOriginal()
  );
  /** True when a discount is entered but the reason field is empty. */
  protected readonly discountReasonMissing = computed(() =>
    this.hasDiscount() && !this.discountReason().trim()
  );
  protected readonly canSubmit = computed(() =>
    !this.anyYearBelowZero() &&
    !this.anyYearExceedsOriginal() &&
    !this.discountExceedsTotal() &&
    !this.discountReasonMissing() &&
    !!this.selectedEnquiry() &&
    this.yearRows().length > 0
  );

  constructor() {
    effect(() => {
      this.dataSource.data = this.filteredEnquiries();
      if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
    });
    // Keep discountReason signal in sync with the FormControl (covers programmatic resets)
    this.discountReasonCtrl.valueChanges.subscribe(v => this.discountReason.set(v ?? ''));
  }

  ngOnInit(): void {
    this.http.get<Program[]>(`${environment.apiUrl}/programs`).subscribe({
      next: (d) => this.programs.set(d),
    });

    const enquiryId = this.route.snapshot.queryParamMap.get('enquiryId');
    if (enquiryId) {
      this.loading.set(true);
      this.enquiryService.getEnquiryById(Number(enquiryId)).subscribe({
        next: (e) => {
          this.allEnquiries.set([e]);
          this.selectEnquiry(e);
          this.loading.set(false);
        },
        error: () => { this.toast.error('Failed to load enquiry'); this.loadList(); },
      });
    } else {
      this.loadList();
    }
  }

  private loadList(): void {
    this.loading.set(true);
    this.enquiryService.getByStatus('INTERESTED').subscribe({
      next: (data) => {
        this.allEnquiries.set(data);
        this.hydrateAuthoritativeProposedFees(data);
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load enquiries'); this.loading.set(false); },
    });
  }

  protected getProposedFee(enquiry: Enquiry): number | null {
    const authoritative = this.authoritativeProposedFeeByEnquiryId()[enquiry.id];
    if (authoritative != null) {
      return authoritative;
    }
    return enquiry.finalCalculatedFee ?? enquiry.feeGuidelineTotal ?? null;
  }

  private hydrateAuthoritativeProposedFees(enquiries: Enquiry[]): void {
    for (const enquiry of enquiries) {
      if (!enquiry.programId || !enquiry.admissionQuota || !enquiry.feeStateId || !enquiry.gender || !enquiry.studentType) {
        continue;
      }

      const params = new URLSearchParams({
        programId: enquiry.programId.toString(),
        quota: enquiry.admissionQuota,
        feeStateId: enquiry.feeStateId.toString(),
        gender: enquiry.gender,
      });
      if (enquiry.courseId) params.set('courseId', enquiry.courseId.toString());
      if (enquiry.studentType) params.set('studentType', enquiry.studentType);

      this.http.get<{ totalFee: number }>(`${environment.apiUrl}/fee-structures/guideline?${params.toString()}`).subscribe({
        next: (response) => this.setAuthoritativeProposedFee(enquiry.id, response.totalFee),
        error: () => {
          // Keep existing row value if authoritative hydration fails.
        },
      });
    }
  }

  private setAuthoritativeProposedFee(enquiryId: number, amount: number): void {
    this.authoritativeProposedFeeByEnquiryId.update(current => ({
      ...current,
      [enquiryId]: amount,
    }));
  }

  protected applySearch(event: Event): void {
    this.searchValue.set((event.target as HTMLInputElement).value);
  }

  protected onProgramFilter(id: number | null): void {
    this.selectedProgramId.set(id);
  }

  protected onQuotaFilter(value: string): void {
    const v = value as 'MANAGEMENT' | 'COUNSELLING' | '';
    this.selectedQuota.set(v || null);
  }

  protected clearFilters(): void {
    this.searchValue.set('');
    this.selectedProgramId.set(null);
    this.selectedQuota.set(null);
  }

  protected get hasActiveFilters(): boolean {
    return !!this.searchValue() || this.selectedProgramId() != null || this.selectedQuota() != null;
  }

  protected selectEnquiry(enquiry: Enquiry): void {
    this.selectedEnquiry.set(enquiry);
    this.discountReasonCtrl.setValue('');
    this.discountReason.set('');
    this.globalDiscount.set(0);
    this.initYearRows(enquiry);
  }

  private initYearRows(enquiry: Enquiry): void {
    // 1. Prefer guideline fetch so finalization always reflects current authoritative fee filtering.
    if (enquiry.programId && enquiry.admissionQuota && enquiry.feeStateId &&
        enquiry.gender && enquiry.studentType) {
      const params = new URLSearchParams({
        programId:   enquiry.programId.toString(),
        quota:       enquiry.admissionQuota,
        feeStateId:  enquiry.feeStateId.toString(),
        gender:      enquiry.gender,
        studentType: enquiry.studentType,
      });
      if (enquiry.courseId) params.set('courseId', enquiry.courseId.toString());

      this.http.get<{ totalFee: number; items: { feeType: string; amount: number; yearAmounts: { yearNumber: number; amount: number }[] }[] }>(
        `${environment.apiUrl}/fee-structures/guideline?${params.toString()}`
      ).subscribe({
        next: (data) => {
          this.setAuthoritativeProposedFee(enquiry.id, data.totalFee);
          const yearMap = new Map<number, number>();
          for (const item of data.items) {
            for (const ya of item.yearAmounts ?? []) {
              yearMap.set(ya.yearNumber, (yearMap.get(ya.yearNumber) ?? 0) + this.amountToPaise(ya.amount));
            }
          }
          if (yearMap.size > 0) {
            const sorted = Array.from(yearMap.entries())
              .sort(([a], [b]) => a - b)
              .map(([yearNumber, amount]) => ({ yearNumber, amount: this.paiseToAmount(amount) }));
            this.applyYearRows(sorted);
          } else {
            this.applyStoredYearRowsFallback(enquiry);
          }
        },
        error: () => this.applyStoredYearRowsFallback(enquiry),
      });
      return;
    }

    // 2. Fallback to stored rows for older enquiries.
    this.applyStoredYearRowsFallback(enquiry);
  }

  private applyStoredYearRowsFallback(enquiry: Enquiry): void {
    if (enquiry.yearWiseFees) {
      try {
        const parsed: { yearNumber: number; amount: number }[] = JSON.parse(enquiry.yearWiseFees);
        if (parsed.length > 0) { this.applyYearRows(parsed); return; }
      } catch { /* fall through */ }
    }

    this.applyEqualSplitFallback(enquiry);
  }

  private applyYearRows(rows: { yearNumber: number; amount: number }[]): void {
    this.yearRows.set(rows.map(y => ({
      yearNumber: y.yearNumber,
      yearLabel: `Year ${y.yearNumber}`,
      originalAmount: y.amount,
      finalAmount: y.amount,
    })));
  }

  private applyEqualSplitFallback(enquiry: Enquiry): void {
    const totalPaise = this.amountToPaise(enquiry.finalCalculatedFee ?? enquiry.feeGuidelineTotal ?? 0);
    // Use the program's actual duration years; fall back to 4 (typical nursing program)
    const program = this.programs().find(p => p.id === enquiry.programId);
    const n = program?.durationYears ?? 4;
    const perYearPaise = Math.floor(totalPaise / n);
    this.yearRows.set(
      Array.from({ length: n }, (_, i) => ({
        yearNumber: i + 1,
        yearLabel: `Year ${i + 1}`,
        originalAmount: this.paiseToAmount(i < n - 1 ? perYearPaise : totalPaise - perYearPaise * (n - 1)),
        finalAmount:    this.paiseToAmount(i < n - 1 ? perYearPaise : totalPaise - perYearPaise * (n - 1)),
      }))
    );
  }

  protected updateYearAmount(index: number, raw: string): void {
    const requestedVal = this.paiseToAmount(Math.max(0, this.amountToPaise(parseFloat(raw) || 0)));
    const rows = this.yearRows().map((r, i) => {
      if (i === index) return { ...r, finalAmount: requestedVal };
      return r;
    });
    this.yearRows.set(rows);
    const finalPaise = rows.reduce((s, r) => s + this.amountToPaise(r.finalAmount), 0);
    this.globalDiscount.set(this.paiseToAmount(Math.max(0, this.amountToPaise(this.totalOriginal()) - finalPaise)));
  }

  protected applyGlobalDiscount(raw: string): void {
    const discountPaise = Math.max(0, this.amountToPaise(parseFloat(raw) || 0));
    this.globalDiscount.set(this.paiseToAmount(discountPaise));
    const totalPaise = this.amountToPaise(this.totalOriginal());
    if (totalPaise <= 0) return;
    const rows = this.yearRows().map((r, i, arr) => {
      if (i < arr.length - 1) {
        const share = Math.round(discountPaise * (this.amountToPaise(r.originalAmount) / totalPaise));
        return { ...r, finalAmount: this.paiseToAmount(Math.max(0, this.amountToPaise(r.originalAmount) - share)) };
      } else {
        const previousFinals = this.yearRows().slice(0, -1).map(r2 => {
          const share = Math.round(discountPaise * (this.amountToPaise(r2.originalAmount) / totalPaise));
          return Math.max(0, this.amountToPaise(r2.originalAmount) - share);
        });
        const sumPrev = previousFinals.reduce((s, v) => s + v, 0);
        return { ...r, finalAmount: this.paiseToAmount(Math.max(0, totalPaise - discountPaise - sumPrev)) };
      }
    });
    this.yearRows.set(rows);
  }

  protected resetDiscount(): void {
    this.yearRows.set(this.yearRows().map(r => ({ ...r, finalAmount: r.originalAmount })));
    this.globalDiscount.set(0);
  }

  protected backToList(): void {
    this.selectedEnquiry.set(null);
    this.yearRows.set([]);
    this.globalDiscount.set(0);
    this.discountReasonCtrl.setValue('');
    this.discountReason.set('');
    this.loadList();
  }

  protected onSubmit(): void {
    if (!this.canSubmit()) return;
    const enquiry = this.selectedEnquiry()!;
    const yearWiseJson = JSON.stringify(
      this.yearRows().map(r => ({ yearNumber: r.yearNumber, amount: r.finalAmount }))
    );
    const request: FeeFinalizationRequest = {
      totalFee:       this.totalOriginal(),
      discountAmount: this.totalDiscount() > 0 ? this.totalDiscount() : undefined,
      discountReason: this.discountReason().trim() || undefined,
      yearWiseFees:   yearWiseJson,
    };

    this.saving.set(true);
    this.enquiryService.finalizeFees(enquiry.id, request).subscribe({
      next: () => {
        this.toast.success('Fee finalized successfully');
        this.backToList();
        this.saving.set(false);
      },
      error: (error: unknown) => {
        this.toast.error(this.getFinalizeErrorMessage(error));
        this.saving.set(false);
      },
    });
  }

  private getFinalizeErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && typeof error.error?.message === 'string') {
      return error.error.message;
    }
    return 'Failed to finalize fee';
  }

  private amountToPaise(value: number | null | undefined): number {
    return Math.round((Number(value) || 0) * 100);
  }

  private paiseToAmount(value: number): number {
    return value / 100;
  }
}

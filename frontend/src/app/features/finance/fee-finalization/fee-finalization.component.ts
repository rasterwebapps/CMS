import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CurrencyPipe, DecimalPipe, PercentPipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { EnquiryService } from '../../enquiry/enquiry.service';
import { Enquiry, FeeFinalizationRequest } from '../../enquiry/enquiry.model';
import { LayoutService } from '../../../core/layout/layout.service';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { ToastService } from '../../../core/toast/toast.service';
import { environment } from '../../../../environments';

interface FeeStructureInfo {
  feeType: string;
  amount: number;
  yearAmounts: { yearNumber: number; amount: number }[];
}

interface YearFeeRow {
  yearNumber: number;
  yearLabel: string;
  originalAmount: number;
  finalAmount: number;
}

@Component({
  selector: 'app-fee-finalization',
  standalone: true,
  imports: [
    CurrencyPipe, DecimalPipe, PercentPipe,
    ReactiveFormsModule,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule,
    MatTableModule, MatTooltipModule,
    PageHeaderComponent,
  ],
  templateUrl: './fee-finalization.component.html',
  styleUrl: './fee-finalization.component.scss',
})
export class FeeFinalizationComponent implements OnInit {
  private readonly route    = inject(ActivatedRoute);
  private readonly router   = inject(Router);
  private readonly http     = inject(HttpClient);
  private readonly enquiryService = inject(EnquiryService);
  private readonly toast    = inject(ToastService);
  protected readonly layoutService = inject(LayoutService);

  protected readonly loading          = signal(false);
  protected readonly saving           = signal(false);
  protected readonly selectedEnquiry  = signal<Enquiry | null>(null);
  protected readonly yearRows         = signal<YearFeeRow[]>([]);
  // Signal-bound value for the global discount amount input (two-way synced)
  protected readonly globalDiscount   = signal<number>(0);

  protected readonly discountReasonCtrl = new FormControl('');

  protected readonly displayedColumns = [
    'name', 'programName', 'courseName', 'referralTypeName', 'finalCalculatedFee', 'actions',
  ];
  protected readonly dataSource = new MatTableDataSource<Enquiry>([]);

  // ── Derived totals ──────────────────────────────────────────────────────────
  protected readonly totalOriginal = computed(() =>
    this.yearRows().reduce((s, r) => s + r.originalAmount, 0)
  );
  protected readonly totalFinal = computed(() =>
    this.yearRows().reduce((s, r) => s + r.finalAmount, 0)
  );
  protected readonly totalDiscount = computed(() =>
    Math.max(0, this.totalOriginal() - this.totalFinal())
  );
  protected readonly discountPct = computed(() => {
    const orig = this.totalOriginal();
    return orig > 0 ? (this.totalDiscount() / orig) : 0;
  });
  protected readonly hasDiscount = computed(() => this.totalDiscount() > 0);

  // ─── Validation helpers ─────────────────────────────────────────────────────
  protected readonly anyYearBelowZero = computed(() =>
    this.yearRows().some(r => r.finalAmount < 0)
  );
  protected readonly discountExceedsTotal = computed(() =>
    this.totalDiscount() > this.totalOriginal()
  );
  protected readonly canSubmit = computed(() =>
    !this.anyYearBelowZero() &&
    !this.discountExceedsTotal() &&
    !!this.selectedEnquiry() &&
    this.yearRows().length > 0
  );

  ngOnInit(): void {
    const enquiryId = this.route.snapshot.queryParamMap.get('enquiryId');
    if (enquiryId) {
      this.loading.set(true);
      this.enquiryService.getEnquiryById(Number(enquiryId)).subscribe({
        next: (e) => { this.dataSource.data = [e]; this.selectEnquiry(e); this.loading.set(false); },
        error: () => { this.toast.error('Failed to load enquiry'); this.loadList(); },
      });
    } else {
      this.loadList();
    }
  }

  private loadList(): void {
    this.loading.set(true);
    this.enquiryService.getByStatus('INTERESTED').subscribe({
      next: (data) => { this.dataSource.data = data; this.loading.set(false); },
      error: () => { this.toast.error('Failed to load enquiries'); this.loading.set(false); },
    });
  }

  protected selectEnquiry(enquiry: Enquiry): void {
    this.selectedEnquiry.set(enquiry);
    this.discountReasonCtrl.setValue('');
    this.globalDiscount.set(0);
    this.initYearRows(enquiry);
  }

  private initYearRows(enquiry: Enquiry): void {
    // Path 1: yearWiseFees already stored on the enquiry (happy path for all new enquiries)
    if (enquiry.yearWiseFees) {
      try {
        const parsed: { yearNumber: number; amount: number }[] = JSON.parse(enquiry.yearWiseFees);
        if (parsed.length > 0) {
          this.applyYearRows(parsed);
          return;
        }
      } catch { /* fall through */ }
    }

    // Path 2: yearWiseFees missing (older enquiries) — derive from the fee structure
    // so we get the correct number of years for the program/course combination.
    if (enquiry.programId && enquiry.courseId) {
      const url = `${environment.apiUrl}/fee-structures?programId=${enquiry.programId}&courseId=${enquiry.courseId}`;
      this.http.get<FeeStructureInfo[]>(url).subscribe({
        next: (data) => {
          const studentType = enquiry.studentType;
          const relevant = data.filter(fs => {
            if (fs.feeType === 'HOSTEL_FEE')    return studentType === 'HOSTELER';
            if (fs.feeType === 'TRANSPORT_FEE') return studentType === 'DAY_SCHOLAR';
            return true;
          });

          const yearMap = new Map<number, number>();
          for (const fs of relevant) {
            for (const ya of fs.yearAmounts ?? []) {
              yearMap.set(ya.yearNumber, (yearMap.get(ya.yearNumber) ?? 0) + ya.amount);
            }
          }

          if (yearMap.size > 0) {
            const sorted = Array.from(yearMap.entries())
              .sort(([a], [b]) => a - b)
              .map(([yearNumber, amount]) => ({ yearNumber, amount }));
            this.applyYearRows(sorted);
          } else {
            this.applyEqualSplitFallback(enquiry);
          }
        },
        error: () => this.applyEqualSplitFallback(enquiry),
      });
      return; // rows set asynchronously above
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

  /** Last-resort fallback — equal split. Should never be reached for valid enquiries. */
  private applyEqualSplitFallback(enquiry: Enquiry): void {
    const total = enquiry.finalCalculatedFee ?? enquiry.feeGuidelineTotal ?? 0;
    const n = 4; // safest default for BSc/nursing programs
    const perYear = Math.floor(total / n);
    this.yearRows.set(
      Array.from({ length: n }, (_, i) => ({
        yearNumber: i + 1,
        yearLabel: `Year ${i + 1}`,
        originalAmount: i < n - 1 ? perYear : total - perYear * (n - 1),
        finalAmount: i < n - 1 ? perYear : total - perYear * (n - 1),
      }))
    );
  }

  // ── Per-year amount edit ─────────────────────────────────────────────────────
  protected updateYearAmount(index: number, raw: string): void {
    const val = Math.max(0, parseFloat(raw) || 0);
    const rows = this.yearRows().map((r, i) =>
      i === index ? { ...r, finalAmount: val } : r
    );
    this.yearRows.set(rows);
    // Sync global discount display
    this.globalDiscount.set(Math.max(0, this.totalOriginal() - rows.reduce((s, r) => s + r.finalAmount, 0)));
  }

  // ── Global discount applies proportionally to all years ──────────────────────
  protected applyGlobalDiscount(raw: string): void {
    const discount = Math.max(0, parseFloat(raw) || 0);
    this.globalDiscount.set(discount);
    const total = this.totalOriginal();
    if (total <= 0) return;

    const rows = this.yearRows().map((r, i, arr) => {
      if (i < arr.length - 1) {
        // proportional share, floored to 2 dp
        const share = Math.round((discount * (r.originalAmount / total)) * 100) / 100;
        return { ...r, finalAmount: Math.max(0, r.originalAmount - share) };
      } else {
        // last year absorbs remainder to prevent rounding drift
        const sumOthers = arr.slice(0, -1).reduce((s, x) => s + x.finalAmount, 0);
        // sumOthers already set by iterations above — recalculate from rows built so far
        const previousFinals = this.yearRows()
          .slice(0, -1)
          .map((r2, i2) => {
            const share = Math.round((discount * (r2.originalAmount / total)) * 100) / 100;
            return Math.max(0, r2.originalAmount - share);
          });
        const sumPrev = previousFinals.reduce((s, v) => s + v, 0);
        return { ...r, finalAmount: Math.max(0, total - discount - sumPrev) };
      }
    });
    this.yearRows.set(rows);
  }

  /** Reset all year amounts back to original (no discount). */
  protected resetDiscount(): void {
    this.yearRows.set(this.yearRows().map(r => ({ ...r, finalAmount: r.originalAmount })));
    this.globalDiscount.set(0);
  }

  protected backToList(): void {
    this.selectedEnquiry.set(null);
    this.yearRows.set([]);
    this.globalDiscount.set(0);
    this.discountReasonCtrl.setValue('');
    this.loadList();
  }

  protected onSubmit(): void {
    if (!this.canSubmit()) return;
    const enquiry = this.selectedEnquiry()!;

    const yearWiseJson = JSON.stringify(
      this.yearRows().map(r => ({ yearNumber: r.yearNumber, amount: r.finalAmount }))
    );

    const request: FeeFinalizationRequest = {
      totalFee:        this.totalOriginal(),
      discountAmount:  this.totalDiscount() > 0 ? this.totalDiscount() : undefined,
      discountReason:  this.discountReasonCtrl.value?.trim() || undefined,
      yearWiseFees:    yearWiseJson,
    };

    this.saving.set(true);
    this.enquiryService.finalizeFees(enquiry.id, request).subscribe({
      next: () => {
        this.toast.success('Fee finalized successfully');
        this.backToList();
        this.saving.set(false);
      },
      error: () => {
        this.toast.error('Failed to finalize fee');
        this.saving.set(false);
      },
    });
  }
}

import { Component, inject, OnInit, signal, computed, ViewChild, effect } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { CurrencyPipe, DecimalPipe, PercentPipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { EnquiryService } from '../../enquiry/enquiry.service';
import { Enquiry, FeeFinalizationRequest } from '../../enquiry/enquiry.model';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
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

interface Program { id: number; name: string; }

@Component({
  selector: 'app-fee-finalization',
  standalone: true,
  imports: [
    CurrencyPipe, DecimalPipe, PercentPipe,
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
  private readonly router   = inject(Router);
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

  // ── List filters ────────────────────────────────────────────────────────────
  protected readonly searchValue        = signal('');
  protected readonly selectedProgramId  = signal<number | null>(null);
  protected readonly programs           = signal<Program[]>([]);
  protected readonly allEnquiries       = signal<Enquiry[]>([]);

  protected readonly filteredEnquiries = computed(() => {
    const search  = this.searchValue().toLowerCase().trim();
    const progId  = this.selectedProgramId();
    return this.allEnquiries().filter(e => {
      if (progId != null && e.programId !== progId) return false;
      if (search && !e.name.toLowerCase().includes(search) &&
          !(e.programName?.toLowerCase().includes(search)) &&
          !(e.courseName?.toLowerCase().includes(search))) return false;
      return true;
    });
  });

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

  constructor() {
    effect(() => {
      this.dataSource.data = this.filteredEnquiries();
      if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
    });
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
      next: (data) => { this.allEnquiries.set(data); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load enquiries'); this.loading.set(false); },
    });
  }

  protected applySearch(event: Event): void {
    this.searchValue.set((event.target as HTMLInputElement).value);
  }

  protected onProgramFilter(id: number | null): void {
    this.selectedProgramId.set(id);
  }

  protected clearFilters(): void {
    this.searchValue.set('');
    this.selectedProgramId.set(null);
  }

  protected get hasActiveFilters(): boolean {
    return !!this.searchValue() || this.selectedProgramId() != null;
  }

  protected selectEnquiry(enquiry: Enquiry): void {
    this.selectedEnquiry.set(enquiry);
    this.discountReasonCtrl.setValue('');
    this.globalDiscount.set(0);
    this.initYearRows(enquiry);
  }

  private initYearRows(enquiry: Enquiry): void {
    if (enquiry.yearWiseFees) {
      try {
        const parsed: { yearNumber: number; amount: number }[] = JSON.parse(enquiry.yearWiseFees);
        if (parsed.length > 0) { this.applyYearRows(parsed); return; }
      } catch { /* fall through */ }
    }

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
      return;
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
    const total = enquiry.finalCalculatedFee ?? enquiry.feeGuidelineTotal ?? 0;
    const n = 4;
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

  protected updateYearAmount(index: number, raw: string): void {
    const val = Math.max(0, parseFloat(raw) || 0);
    const rows = this.yearRows().map((r, i) =>
      i === index ? { ...r, finalAmount: val } : r
    );
    this.yearRows.set(rows);
    this.globalDiscount.set(Math.max(0, this.totalOriginal() - rows.reduce((s, r) => s + r.finalAmount, 0)));
  }

  protected applyGlobalDiscount(raw: string): void {
    const discount = Math.max(0, parseFloat(raw) || 0);
    this.globalDiscount.set(discount);
    const total = this.totalOriginal();
    if (total <= 0) return;
    const rows = this.yearRows().map((r, i, arr) => {
      if (i < arr.length - 1) {
        const share = Math.round((discount * (r.originalAmount / total)) * 100) / 100;
        return { ...r, finalAmount: Math.max(0, r.originalAmount - share) };
      } else {
        const previousFinals = this.yearRows().slice(0, -1).map(r2 => {
          const share = Math.round((discount * (r2.originalAmount / total)) * 100) / 100;
          return Math.max(0, r2.originalAmount - share);
        });
        const sumPrev = previousFinals.reduce((s, v) => s + v, 0);
        return { ...r, finalAmount: Math.max(0, total - discount - sumPrev) };
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
      discountReason: this.discountReasonCtrl.value?.trim() || undefined,
      yearWiseFees:   yearWiseJson,
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

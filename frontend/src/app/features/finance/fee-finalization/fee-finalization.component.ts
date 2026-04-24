import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { EnquiryService } from '../../enquiry/enquiry.service';
import { Enquiry, FeeFinalizationRequest } from '../../enquiry/enquiry.model';
import { LayoutService } from '../../../core/layout/layout.service';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { ToastService } from '../../../core/toast/toast.service';

interface SemesterFeeRow {
  semesterNumber: number;
  semesterLabel: string;
  amount: number;
}

@Component({
  selector: 'app-fee-finalization',
  standalone: true,
  imports: [
    CurrencyPipe,
    DecimalPipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTableModule,
    MatPaginatorModule,
    MatChipsModule,
    MatTooltipModule,
    PageHeaderComponent],
  templateUrl: './fee-finalization.component.html',
  styleUrl: './fee-finalization.component.scss',
})
export class FeeFinalizationComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly enquiryService = inject(EnquiryService);
  private readonly toast = inject(ToastService);
  protected readonly layoutService = inject(LayoutService);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly selectedEnquiry = signal<Enquiry | null>(null);
  protected readonly semesterFees = signal<SemesterFeeRow[]>([]);

  private discountNotExceedTotalValidator(group: AbstractControl): ValidationErrors | null {
    const total = Number(group.get('totalFee')?.value) || 0;
    const discount = Number(group.get('discountAmount')?.value) || 0;
    return discount > total ? { discountExceedsTotal: true } : null;
  }

  protected readonly displayedColumns = [
    'name',
    'programName',
    'courseName',
    'referralTypeName',
    'finalCalculatedFee',
    'actions'];
  protected readonly dataSource = new MatTableDataSource<Enquiry>([]);

  protected readonly form: FormGroup = this.fb.group({
    totalFee: [null, [Validators.required, Validators.min(0)]],
    discountAmount: [0, [Validators.min(0)]],
    discountReason: [''],
  }, { validators: this.discountNotExceedTotalValidator });

  protected readonly netFee = computed(() => {
    const total = this.form.get('totalFee')?.value || 0;
    const discount = this.form.get('discountAmount')?.value || 0;
    return Math.max(0, total - discount);
  });

  protected readonly semesterTotal = computed(() =>
    this.semesterFees().reduce((sum, s) => sum + (Number(s.amount) || 0), 0)
  );

  protected readonly semesterSumMismatch = computed(() => {
    const net = this.netFee();
    const sum = this.semesterTotal();
    return net > 0 && Math.abs(sum - net) > 0.01;
  });

  ngOnInit(): void {
    const enquiryId = this.route.snapshot.queryParamMap.get('enquiryId');
    if (enquiryId) {
      this.loading.set(true);
      this.enquiryService.getEnquiryById(Number(enquiryId)).subscribe({
        next: (enquiry) => {
          this.dataSource.data = [enquiry];
          this.selectEnquiry(enquiry);
          this.loading.set(false);
        },
        error: () => {
          this.toast.error('Failed to load enquiry');
          this.loadInterestedEnquiries();
        },
      });
    } else {
      this.loadInterestedEnquiries();
    }
  }

  private loadInterestedEnquiries(): void {
    this.loading.set(true);
    this.enquiryService.getByStatus('INTERESTED').subscribe({
      next: (data) => {
        this.dataSource.data = data;
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load enquiries');
        this.loading.set(false);
      },
    });
  }

  protected selectEnquiry(enquiry: Enquiry): void {
    this.selectedEnquiry.set(enquiry);

    const totalFee = enquiry.finalCalculatedFee ?? enquiry.feeGuidelineTotal ?? 0;
    this.form.patchValue({
      totalFee,
      discountAmount: 0,
      discountReason: '',
    });

    this.initSemesterFees(enquiry, totalFee);
  }

  private initSemesterFees(enquiry: Enquiry, totalFee: number): void {
    // Use existing semester-wise data if already set on this enquiry
    if (enquiry.semesterWiseFees) {
      try {
        const parsed = JSON.parse(enquiry.semesterWiseFees) as SemesterFeeRow[];
        this.semesterFees.set(parsed);
        return;
      } catch {
        // fall through
      }
    }

    // Derive number of semesters from year_wise_fees; default to 4 (2-year programme)
    let numSemesters = 4;
    if (enquiry.yearWiseFees) {
      try {
        const years = JSON.parse(enquiry.yearWiseFees) as { yearNumber: number; amount: number }[];
        if (years.length > 0) numSemesters = years.length * 2;
      } catch {
        // default
      }
    }

    // Build equal-split rows using net fee (discount is 0 at this point)
    const perSemester = numSemesters > 0 ? Math.round((totalFee / numSemesters) * 100) / 100 : 0;
    const rows: SemesterFeeRow[] = [];
    for (let i = 1; i <= numSemesters; i++) {
      rows.push({ semesterNumber: i, semesterLabel: `Semester ${i}`, amount: perSemester });
    }
    this.semesterFees.set(rows);
  }

  protected updateSemesterAmount(index: number, value: string): void {
    const updated = [...this.semesterFees()];
    updated[index] = { ...updated[index], amount: Number(value) || 0 };
    this.semesterFees.set(updated);
  }

  /** Redistribute net fee equally across semesters. */
  protected redistributeSemesters(): void {
    const net = this.netFee();
    const count = this.semesterFees().length;
    if (count === 0 || net <= 0) return;
    const perSem = Math.round((net / count) * 100) / 100;
    this.semesterFees.set(
      this.semesterFees().map((s, i) => ({
        ...s,
        // Last semester absorbs any rounding remainder
        amount: i < count - 1 ? perSem : Math.round((net - perSem * (count - 1)) * 100) / 100,
      }))
    );
  }

  protected backToList(): void {
    this.selectedEnquiry.set(null);
    this.semesterFees.set([]);
    this.form.reset({ totalFee: null, discountAmount: 0, discountReason: '' });
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const enquiry = this.selectedEnquiry();
    if (!enquiry) return;

    const v = this.form.value;

    const request: FeeFinalizationRequest = {
      totalFee: v.totalFee,
      discountAmount: v.discountAmount || undefined,
      discountReason: v.discountReason?.trim() || undefined,
      semesterWiseFees: this.semesterFees().length > 0
        ? JSON.stringify(this.semesterFees())
        : undefined,
    };

    this.saving.set(true);
    this.enquiryService.finalizeFees(enquiry.id, request).subscribe({
      next: () => {
        this.toast.success('Fee finalized successfully');
        this.selectedEnquiry.set(null);
        this.semesterFees.set([]);
        this.loadInterestedEnquiries();
        this.saving.set(false);
      },
      error: () => {
        this.toast.error('Failed to finalize fee');
        this.saving.set(false);
      },
    });
  }
}

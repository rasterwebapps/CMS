import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { EnquiryService } from '../../enquiry/enquiry.service';
import { Enquiry, FeeFinalizationRequest } from '../../enquiry/enquiry.model';
import { LayoutService } from '../../../core/layout/layout.service';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';

interface YearFee {
  yearNumber: number;
  amount: number;
}

@Component({
  selector: 'app-fee-finalization',
  standalone: true,
  imports: [
    CurrencyPipe,
    DecimalPipe,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTableModule,
    MatPaginatorModule,
    MatChipsModule,
    MatTooltipModule,
    PageHeaderComponent,
  ],
  templateUrl: './fee-finalization.component.html',
  styleUrl: './fee-finalization.component.scss',
})
export class FeeFinalizationComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly enquiryService = inject(EnquiryService);
  private readonly snackBar = inject(MatSnackBar);
  protected readonly layoutService = inject(LayoutService);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly selectedEnquiry = signal<Enquiry | null>(null);
  protected readonly yearFees = signal<YearFee[]>([]);

  /** Cross-field validator: discount must not exceed total fee. */
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
    'actions',
  ];
  protected readonly dataSource = new MatTableDataSource<Enquiry>([]);

  protected readonly form: FormGroup = this.fb.group({
    totalFee: [null, [Validators.required, Validators.min(0)]],
    discountAmount: [0, [Validators.min(0)]],
    discountReason: [''],
  }, { validators: this.discountNotExceedTotalValidator });

  protected readonly netFee = computed(() => {
    const total = this.form.get('totalFee')?.value || 0;
    const discount = this.form.get('discountAmount')?.value || 0;
    return total - discount;
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
          this.snackBar.open('Failed to load enquiry', 'Close', { duration: 3000 });
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
        this.snackBar.open('Failed to load enquiries', 'Close', { duration: 3000 });
        this.loading.set(false);
      },
    });
  }

  protected selectEnquiry(enquiry: Enquiry): void {
    this.selectedEnquiry.set(enquiry);

    // Pre-populate form with enquiry data
    const totalFee = enquiry.finalCalculatedFee ?? enquiry.feeGuidelineTotal ?? 0;
    this.form.patchValue({
      totalFee,
      discountAmount: 0,
      discountReason: '',
    });

    // Parse year-wise fees from enquiry
    if (enquiry.yearWiseFees) {
      try {
        const parsed = JSON.parse(enquiry.yearWiseFees) as YearFee[];
        this.yearFees.set(parsed);
      } catch {
        this.yearFees.set([]);
      }
    } else {
      this.yearFees.set([]);
    }
  }

  protected backToList(): void {
    this.selectedEnquiry.set(null);
    this.yearFees.set([]);
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
    const yearWiseFeesJson =
      this.yearFees().length > 0
        ? JSON.stringify(this.yearFees())
        : undefined;

    const request: FeeFinalizationRequest = {
      totalFee: v.totalFee,
      discountAmount: v.discountAmount || undefined,
      discountReason: v.discountReason?.trim() || undefined,
      yearWiseFees: yearWiseFeesJson,
    };

    this.saving.set(true);
    this.enquiryService.finalizeFees(enquiry.id, request).subscribe({
      next: () => {
        this.snackBar.open('Fee finalized successfully', 'Close', { duration: 3000 });
        this.selectedEnquiry.set(null);
        this.loadInterestedEnquiries();
        this.saving.set(false);
      },
      error: () => {
        this.snackBar.open('Failed to finalize fee', 'Close', { duration: 3000 });
        this.saving.set(false);
      },
    });
  }
}

import { Component, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { FeeReportsService } from '../fee-reports.service';
import {
  FeeCollectionSummary,
  FeeDemandReport,
  StudentFeeLedgerReport,
  TermFeePaymentReport,
} from '../fee-reports.model';
import { ToastService } from '../../../core/toast/toast.service';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear, TermInstance } from '../../academic-year/academic-year.model';
import { PrintService } from '../../../core/print/print.service';
import { CsvExporterService, CsvColumn } from '../../../core/export/csv-exporter.service';

@Component({
  selector: 'app-fee-reports-dashboard',
  standalone: true,
  imports: [
    DatePipe,
    DecimalPipe,
    ReactiveFormsModule,
    MatTabsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './fee-reports-dashboard.component.html',
  styleUrl: './fee-reports-dashboard.component.scss',
})
export class FeeReportsDashboardComponent {
  private readonly feeReportsService = inject(FeeReportsService);
  private readonly academicYearService = inject(AcademicYearService);
  private readonly toast = inject(ToastService);
  private readonly printService = inject(PrintService);
  private readonly csvExporter = inject(CsvExporterService);
  private readonly fb = inject(FormBuilder);

  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly termInstances = signal<TermInstance[]>([]);

  protected readonly outstandingDemands = signal<FeeDemandReport[]>([]);
  protected readonly collectionSummary = signal<FeeCollectionSummary[]>([]);
  protected readonly lateFeePayments = signal<TermFeePaymentReport[]>([]);
  protected readonly ledger = signal<StudentFeeLedgerReport | null>(null);

  protected readonly loading = signal(false);

  readonly termFilterForm: FormGroup = this.fb.group({
    academicYearId: [null],
    termInstanceId: [null, Validators.required],
  });

  readonly studentLedgerForm: FormGroup = this.fb.group({
    studentId: [null, [Validators.required, Validators.min(1)]],
  });

  constructor() {
    this.academicYearService.getAllAcademicYears().subscribe({
      next: (data) => this.academicYears.set(data),
      error: () => this.toast.error('Failed to load academic years'),
    });
  }

  onAcademicYearChange(ayId: string): void {
    if (!ayId) {
      this.termInstances.set([]);
      return;
    }
    this.academicYearService.getTermInstancesByAcademicYear(Number(ayId)).subscribe({
      next: (data) => this.termInstances.set(data),
      error: () => this.toast.error('Failed to load term instances'),
    });
  }

  loadOutstanding(): void {
    const termId = this.termFilterForm.value.termInstanceId;
    if (!termId) return;
    this.loading.set(true);
    this.feeReportsService.getOutstandingDemands(Number(termId)).subscribe({
      next: (data) => {
        this.outstandingDemands.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load outstanding demands');
        this.loading.set(false);
      },
    });
  }

  loadCollectionSummary(): void {
    const termId = this.termFilterForm.value.termInstanceId;
    if (!termId) return;
    this.loading.set(true);
    this.feeReportsService.getCollectionSummary(Number(termId)).subscribe({
      next: (data) => {
        this.collectionSummary.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load collection summary');
        this.loading.set(false);
      },
    });
  }

  loadLateFees(): void {
    const termId = this.termFilterForm.value.termInstanceId;
    if (!termId) return;
    this.loading.set(true);
    this.feeReportsService.getLateFeeCollection(Number(termId)).subscribe({
      next: (data) => {
        this.lateFeePayments.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load late fee collection');
        this.loading.set(false);
      },
    });
  }

  loadStudentLedger(): void {
    if (this.studentLedgerForm.invalid) {
      this.studentLedgerForm.markAllAsTouched();
      return;
    }
    const studentId = this.studentLedgerForm.value.studentId;
    this.loading.set(true);
    this.feeReportsService.getStudentLedger(Number(studentId)).subscribe({
      next: (data) => {
        this.ledger.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Student not found or no fee data available');
        this.loading.set(false);
      },
    });
  }

  printReport(): void {
    this.printService.printRoute();
  }

  exportOutstandingCsv(): void {
    const columns: ReadonlyArray<CsvColumn<FeeDemandReport>> = [
      { key: 'studentName', header: 'Student' },
      { key: 'cohortCode', header: 'Cohort' },
      { key: 'termInstanceLabel', header: 'Term' },
      { key: 'totalAmount', header: 'Total (INR)' },
      { key: 'paidAmount', header: 'Paid (INR)' },
      { key: 'outstandingAmount', header: 'Outstanding (INR)' },
      { key: 'dueDate', header: 'Due Date' },
      { key: 'status', header: 'Status' },
    ];
    this.csvExporter.exportRows('outstanding-demands', columns, this.outstandingDemands());
  }

  exportSummaryCsv(): void {
    const columns: ReadonlyArray<CsvColumn<FeeCollectionSummary>> = [
      { key: 'programName', header: 'Program' },
      { key: 'programCode', header: 'Code' },
      { key: 'totalDemands', header: 'Total Demands' },
      { key: 'totalAmount', header: 'Total (INR)' },
      { key: 'collectedAmount', header: 'Collected (INR)' },
      { key: 'outstandingAmount', header: 'Outstanding (INR)' },
      { key: 'paidCount', header: 'Paid' },
      { key: 'partialCount', header: 'Partial' },
      { key: 'unpaidCount', header: 'Unpaid' },
    ];
    this.csvExporter.exportRows('fee-collection-summary', columns, this.collectionSummary());
  }
}

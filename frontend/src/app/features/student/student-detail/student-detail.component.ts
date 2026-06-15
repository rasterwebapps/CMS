import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { forkJoin } from 'rxjs';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { StudentService } from '../student.service';
import {
  CourseRegistration,
  Student,
  StudentFeeLedger,
  StudentLedgerEntry,
  StudentTermEnrollment,
  TermFeePaymentSummary,
} from '../student.model';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { CmsSkeletonComponent } from '../../../shared/skeleton/skeleton.component';
import { computeInitials } from '../../../shared/utils/initials';
import { ToastService } from '../../../core/toast/toast.service';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { PaymentModeLabelPipe } from '../../../shared/pipes/payment-mode-label.pipe';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { STUDENT_DETAIL_TOUR } from '../../../shared/tour/tours/student.tours';
import { ScholarshipService } from '../../scholarship/scholarship.service';
import {
  ScholarshipApplication,
  ScholarshipDisbursement,
  ScholarshipEligibility,
  ScholarshipType,
} from '../../scholarship/scholarship.model';
import { AdmissionService } from '../../admission/admission.service';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ProfileDocumentsComponent } from '../../../shared/profile-documents/profile-documents.component';
import { ProgramTransferDialogComponent, ProgramTransferDialogData } from '../program-transfer-dialog/program-transfer-dialog.component';
import { ProgramTransferRecord } from '../student.model';
import { Program } from '../../program/program.model';
import { ProgramService } from '../../program/program.service';
import {
  EligibilityEditDialogComponent,
} from '../../scholarship/eligibility-edit-dialog/eligibility-edit-dialog.component';
import {
  VerifyEligibilityDialogComponent,
} from '../../scholarship/verify-eligibility-dialog/verify-eligibility-dialog.component';
import { FeeReceiptDialogComponent } from '../../../shared/fee-receipt-dialog/fee-receipt-dialog.component';
import { FinanceService } from '../../finance/finance.service';
import { ReceiptDisplayData } from '../../finance/finance.model';
import { printFeeReceipt } from '../../../shared/utils/print-receipt.utils';

@Component({
  selector: 'app-student-detail',
  standalone: true,
  imports: [
    AppDatePipe,
    PaymentModeLabelPipe,
    InrPipe,
    RouterLink,
    DecimalPipe,
    MatTabsModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    CmsStatusBadgeComponent,
    CmsSkeletonComponent,
    CmsTourButtonComponent,
    ProfileDocumentsComponent,
    MatTooltipModule,
    FeeReceiptDialogComponent,
  ],
  templateUrl: './student-detail.component.html',
  styleUrl: './student-detail.component.scss',
})
export class StudentDetailComponent implements OnInit {
  private readonly studentService = inject(StudentService);
  private readonly admissionService = inject(AdmissionService);
  private readonly permissionService = inject(PermissionService);
  private readonly programService = inject(ProgramService);
  private readonly financeService = inject(FinanceService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly tourService = inject(TourService);
  private readonly scholarshipService = inject(ScholarshipService);
  private readonly dialog = inject(MatDialog);

  protected readonly student = signal<Student | null>(null);
  protected readonly loading = signal(false);
  protected readonly enrollments = signal<StudentTermEnrollment[]>([]);
  protected readonly loadingEnrollments = signal(false);
  protected readonly registrationsByEnrollment = signal<Map<number, CourseRegistration[]>>(new Map());
  protected readonly loadingRegistrations = signal(false);

  protected readonly feeLedger = signal<StudentFeeLedger | null>(null);
  protected readonly loadingLedger = signal(false);
  protected readonly scholarshipEligibility = signal<ScholarshipEligibility | null>(null);
  protected readonly eligibleScholarships = signal<ScholarshipType[]>([]);
  protected readonly scholarshipApplications = signal<ScholarshipApplication[]>([]);
  protected readonly scholarshipDisbursements = signal<ScholarshipDisbursement[]>([]);
  protected readonly loadingScholarships = signal(false);

  protected readonly admissionId = signal<number | null>(null);
  protected readonly passportPhotoUrl = signal<string | null>(null);
  protected readonly transferHistory = signal<ProgramTransferRecord[]>([]);
  protected readonly allPrograms = signal<Program[]>([]);
  protected readonly selectedTabIndex = signal(0);
  protected readonly expandedEnrollments = signal(new Set<number>());
  protected readonly selectedReceipt = signal<ReceiptDisplayData | null>(null);

  /** First + last initial of the student's full name. */
  protected readonly initials = computed(() => computeInitials(this.student()?.fullName));

  protected readonly sortedEnrollments = computed(() =>
    [...this.enrollments()].sort((a, b) => b.termNumber - a.termNumber),
  );

  protected readonly totalOutstanding = computed(() => {
    const l = this.feeLedger();
    return l ? l.entries.reduce((s, e) => s + e.outstandingAmount, 0) : 0;
  });

  protected readonly totalFeeDemand = computed(() => {
    const l = this.feeLedger();
    return l ? l.entries.reduce((s, e) => s + e.totalAmount, 0) : 0;
  });

  protected readonly totalFeePaid = computed(() => {
    const l = this.feeLedger();
    return l ? l.entries.reduce((s, e) => s + e.paidAmount, 0) : 0;
  });

  protected paidPercent(paid: number, total: number): number {
    return total > 0 ? Math.min(100, Math.round((paid / total) * 100)) : 0;
  }

  protected readonly activeScholarshipCount = computed(() =>
    this.scholarshipApplications().filter(a => a.status === 'APPROVED' || a.status === 'PENDING').length,
  );

  protected switchTab(index: number): void {
    this.selectedTabIndex.set(index);
  }

  protected toggleEnrollment(id: number): void {
    this.expandedEnrollments.update(set => {
      const next = new Set(set);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  }

  protected isExpanded(id: number): boolean {
    return this.expandedEnrollments().has(id);
  }

  ngOnInit(): void {
    this.tourService.register('student-detail', STUDENT_DETAIL_TOUR);
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.loadStudent(Number(idParam));
    }
  }

  protected editStudent(): void {
    const s = this.student();
    if (s) {
      void this.router.navigate(['/students', s.id, 'edit']);
    }
  }

  private loadStudent(id: number): void {
    this.loading.set(true);
    this.studentService.getById(id).subscribe({
      next: (student) => {
        this.student.set(student);
        this.loading.set(false);
        this.loadEnrollments(id);
        this.loadFeeLedger(id);
        this.loadScholarships(id);
        this.loadAdmission(id);
        this.loadTransferHistory(id);
      },
      error: () => {
        this.toast.error('Failed to load student');
        void this.router.navigate(['/students']);
      },
    });
  }

  private loadEnrollments(studentId: number): void {
    this.loadingEnrollments.set(true);
    this.studentService.getEnrollmentsByStudent(studentId).subscribe({
      next: (data) => {
        this.enrollments.set(data);
        this.loadingEnrollments.set(false);
        const first = [...data].sort((a, b) => b.termNumber - a.termNumber)[0];
        if (first) this.expandedEnrollments.set(new Set([first.id]));
        this.loadAllRegistrations(data);
      },
      error: () => {
        this.loadingEnrollments.set(false);
      },
    });
  }

  private loadAllRegistrations(enrollments: StudentTermEnrollment[]): void {
    if (enrollments.length === 0) return;
    this.loadingRegistrations.set(true);

    const requests = enrollments.map(enrollment =>
      this.studentService.getRegistrationsByEnrollment(enrollment.id)
    );

    forkJoin(requests).subscribe({
      next: (results) => {
        const map = new Map<number, CourseRegistration[]>();
        enrollments.forEach((enrollment, index) => {
          map.set(enrollment.id, results[index]);
        });
        this.registrationsByEnrollment.set(map);
        this.loadingRegistrations.set(false);
      },
      error: () => {
        this.loadingRegistrations.set(false);
      },
    });
  }

  protected getRegistrationsForEnrollment(enrollmentId: number): CourseRegistration[] {
    return this.registrationsByEnrollment().get(enrollmentId) ?? [];
  }

  private loadFeeLedger(studentId: number): void {
    this.loadingLedger.set(true);
    this.studentService.getStudentFeeLedger(studentId).subscribe({
      next: (ledger) => {
        this.feeLedger.set(ledger);
        this.loadingLedger.set(false);
      },
      error: () => {
        // Fee ledger may not exist yet — not a fatal error
        this.loadingLedger.set(false);
      },
    });
  }

  protected hasScholarshipForCurrentYear(): boolean {
    return this.scholarshipApplications().some(a => a.status === 'PENDING' || a.status === 'APPROVED');
  }

  protected applyForScholarship(type: ScholarshipType): void {
    const s = this.student();
    if (!s) return;
    this.scholarshipService.apply(s.id, { scholarshipTypeId: type.id }).subscribe({
      next: () => { this.toast.success('Scholarship application submitted'); this.loadScholarships(s.id); },
      error: err => this.toast.error(err?.error?.message ?? 'Failed to apply for scholarship'),
    });
  }

  protected renewScholarship(application: ScholarshipApplication): void {
    const s = this.student();
    if (!s) return;
    this.scholarshipService.renew(application.id).subscribe({
      next: () => { this.toast.success('Scholarship renewal submitted'); this.loadScholarships(s.id); },
      error: err => this.toast.error(err?.error?.message ?? 'Failed to renew scholarship'),
    });
  }

  protected openEligibilityEdit(): void {
    const s = this.student();
    if (!s) return;
    const ref = this.dialog.open(EligibilityEditDialogComponent, {
      width: '760px',
      maxWidth: '95vw',
      data: {
        studentId: s.id,
        studentName: s.fullName,
        eligibility: this.scholarshipEligibility(),
      },
    });
    ref.afterClosed().subscribe((updated: ScholarshipEligibility | undefined) => {
      if (updated) {
        this.scholarshipEligibility.set(updated);
        this.loadEligibleScholarships(s.id);
      }
    });
  }

  protected verifyEligibility(): void {
    const s = this.student();
    if (!s) return;
    const ref = this.dialog.open(VerifyEligibilityDialogComponent, {
      width: '480px',
      maxWidth: '95vw',
      data: {
        studentId: s.id,
        studentName: s.fullName,
        eligibility: this.scholarshipEligibility(),
      },
    });
    ref.afterClosed().subscribe((updated: ScholarshipEligibility | undefined) => {
      if (updated) {
        this.scholarshipEligibility.set(updated);
      }
    });
  }

  private loadEligibleScholarships(studentId: number): void {
    this.scholarshipService.getEligibleScholarships(studentId).subscribe({
      next: (eligible) => this.eligibleScholarships.set(eligible),
      error: () => {},
    });
  }

  private loadAdmission(studentId: number): void {
    this.admissionService.getByStudent(studentId).subscribe({
      next: (admission) => {
        this.admissionId.set(admission.id);
        this.loadPassportPhoto(admission.id);
      },
      error: () => {},
    });
  }

  private loadPassportPhoto(admissionId: number): void {
    this.admissionService.getDocuments(admissionId).subscribe({
      next: (docs) => {
        const photoDoc = docs.find((d) => d.documentType === 'PASSPORT_PHOTO' && d.hasFile);
        if (!photoDoc) return;
        this.admissionService.downloadDocumentBlob(photoDoc.id).subscribe({
          next: (response) => {
            const blob = response.body;
            if (!blob) return;
            const reader = new FileReader();
            reader.onload = () => this.passportPhotoUrl.set(reader.result as string);
            reader.readAsDataURL(blob);
          },
          error: () => {},
        });
      },
      error: () => {},
    });
  }

  private loadTransferHistory(studentId: number): void {
    this.studentService.getTransferHistory(studentId).subscribe({
      next: (history) => this.transferHistory.set(history),
      error: () => {},
    });
  }

  protected canManageDocuments(): boolean {
    return this.permissionService.has('DOCUMENT_SUBMISSION_MANAGE');
  }

  protected canEditStudent(): boolean {
    return this.permissionService.has('STUDENT_EDIT');
  }

  protected canTransferProgram(): boolean {
    const s = this.student()?.status;
    return this.permissionService.has('STUDENT_EDIT') && (s === 'ACTIVE' || s === 'ON_LEAVE');
  }

  protected openProgramTransfer(): void {
    const s = this.student();
    if (!s) return;

    if (this.allPrograms().length === 0) {
      this.programService.getAll().subscribe({
        next: (programs) => {
          this.allPrograms.set(programs);
          this.showProgramTransferDialog();
        },
        error: () => this.toast.error('Failed to load programs'),
      });
    } else {
      this.showProgramTransferDialog();
    }
  }

  private showProgramTransferDialog(): void {
    const s = this.student()!;
    const dialogData: ProgramTransferDialogData = {
      studentId: s.id,
      studentName: s.fullName,
      currentProgramId: s.programId,
      currentProgramName: s.programName,
      programs: this.allPrograms(),
    };
    const ref = this.dialog.open(ProgramTransferDialogComponent, {
      data: dialogData,
      width: '640px',
      maxWidth: '95vw',
      disableClose: true,
    });
    ref.afterClosed().subscribe((record: ProgramTransferRecord | undefined) => {
      if (record) {
        this.toast.success(`Program changed to ${record.newProgramName}`);
        this.loadStudent(s.id);
        this.loadTransferHistory(s.id);
      }
    });
  }

  private loadScholarships(studentId: number): void {
    this.loadingScholarships.set(true);
    forkJoin({
      eligibility: this.scholarshipService.getEligibility(studentId),
      eligible: this.scholarshipService.getEligibleScholarships(studentId),
      applications: this.scholarshipService.getStudentScholarships(studentId),
      disbursements: this.scholarshipService.getStudentDisbursements(studentId),
    }).subscribe({
      next: ({ eligibility, eligible, applications, disbursements }) => {
        this.scholarshipEligibility.set(eligibility);
        this.eligibleScholarships.set(eligible);
        this.scholarshipApplications.set(applications);
        this.scholarshipDisbursements.set(disbursements);
        this.loadingScholarships.set(false);
      },
      error: () => {
        this.loadingScholarships.set(false);
      },
    });
  }

  // ─── Receipt actions ─────────────────────────────────────────────────────

  /**
   * Opens the fee receipt slide-in panel for the given payment row.
   * Fetches full receipt details (including transaction reference) via the
   * unified receipts endpoint before showing the dialog.
   */
  protected viewReceipt(p: TermFeePaymentSummary, entry: StudentLedgerEntry): void {
    const s = this.student();
    // Try to fetch fuller receipt data first (to get transactionReference, etc.)
    this.financeService.getReceiptByNumber(p.receiptNumber).subscribe({
      next: (r) => {
        this.selectedReceipt.set({
          receiptNumber:        r.receiptNumber,
          payerType:            'STUDENT',
          payerName:            r.payerName,
          payerIdentifier:      r.payerIdentifier,
          programName:          r.programName,
          amountPaid:           r.amountPaid,
          paymentDate:          r.paymentDate,
          paymentMode:          r.paymentMode ?? '',
          transactionReference: r.transactionReference,
          remarks:              r.remarks,
          installmentsCovered:  r.installmentsCovered ?? entry.termLabel,
          installmentBreakdown: r.installmentsCovered
            ? [{ label: r.installmentsCovered, amount: r.amountPaid }]
            : [{ label: entry.termLabel, amount: p.amountPaid }],
          feeCategory:          r.feeCategory,
        });
      },
      error: () => {
        // Fallback: build from available data if the API call fails
        this.selectedReceipt.set({
          receiptNumber:        p.receiptNumber,
          payerType:            'STUDENT',
          payerName:            s?.fullName ?? '',
          payerIdentifier:      s?.rollNumber ?? null,
          programName:          s?.programName ?? null,
          amountPaid:           p.amountPaid,
          paymentDate:          p.paymentDate,
          paymentMode:          p.paymentMode,
          transactionReference: null,
          remarks:              p.remarks,
          installmentsCovered:  entry.termLabel,
          installmentBreakdown: [{ label: entry.termLabel, amount: p.amountPaid }],
          feeCategory:          null,
        });
      },
    });
  }

  /** Directly prints the receipt without opening the dialog. */
  protected printReceiptDirect(p: TermFeePaymentSummary, entry: StudentLedgerEntry): void {
    const s = this.student();
    printFeeReceipt({
      receiptNumber:        p.receiptNumber,
      payerName:            s?.fullName ?? '',
      payerIdentifier:      s?.rollNumber ?? '',
      programName:          s?.programName ?? '',
      amountPaid:           p.amountPaid,
      paymentDate:          p.paymentDate,
      paymentMode:          p.paymentMode,
      transactionReference: null,
      feeCategory:          null,
      installmentBreakdown: [{ installmentLabel: entry.termLabel, amountApplied: p.amountPaid }],
    });
  }
}

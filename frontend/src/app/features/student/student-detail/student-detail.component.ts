import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { forkJoin } from 'rxjs';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { StudentService } from '../student.service';
import {
  CourseRegistration,
  DemandStatus,
  Student,
  StudentFeeLedger,
  StudentTermEnrollment,
} from '../student.model';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { CmsSkeletonComponent } from '../../../shared/skeleton/skeleton.component';
import { computeInitials } from '../../../shared/utils/initials';
import { ToastService } from '../../../core/toast/toast.service';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
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
import {
  EligibilityEditDialogComponent,
} from '../../scholarship/eligibility-edit-dialog/eligibility-edit-dialog.component';
import {
  VerifyEligibilityDialogComponent,
} from '../../scholarship/verify-eligibility-dialog/verify-eligibility-dialog.component';

@Component({
  selector: 'app-student-detail',
  standalone: true,
  imports: [
    AppDatePipe,
    InrPipe,
    RouterLink,
    DecimalPipe,
    MatTabsModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    CmsStatusBadgeComponent,
    CmsSkeletonComponent,
    CmsTourButtonComponent],
  templateUrl: './student-detail.component.html',
  styleUrl: './student-detail.component.scss',
})
export class StudentDetailComponent implements OnInit {
  private readonly studentService = inject(StudentService);
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

  /** First + last initial of the student's full name. */
  protected readonly initials = computed(() => computeInitials(this.student()?.fullName));

  protected readonly sortedEnrollments = computed(() =>
    [...this.enrollments()].sort((a, b) => b.semesterNumber - a.semesterNumber),
  );

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

  protected getDemandStatusClass(status: DemandStatus): string {
    switch (status) {
      case 'PAID': return 'success';
      case 'PARTIAL': return 'warning';
      case 'UNPAID': return 'danger';
      case 'WAIVED': return 'default';
    }
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
}

import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { forkJoin } from 'rxjs';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
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

  protected readonly student = signal<Student | null>(null);
  protected readonly loading = signal(false);
  protected readonly enrollments = signal<StudentTermEnrollment[]>([]);
  protected readonly loadingEnrollments = signal(false);
  protected readonly registrationsByEnrollment = signal<Map<number, CourseRegistration[]>>(new Map());
  protected readonly loadingRegistrations = signal(false);

  protected readonly feeLedger = signal<StudentFeeLedger | null>(null);
  protected readonly loadingLedger = signal(false);

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
}

import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { StudentService } from '../student.service';
import { CourseRegistration, Student, StudentTermEnrollment } from '../student.model';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { CmsSkeletonComponent } from '../../../shared/skeleton/skeleton.component';
import { computeInitials } from '../../../shared/utils/initials';
import { ToastService } from '../../../core/toast/toast.service';

@Component({
  selector: 'app-student-detail',
  standalone: true,
  imports: [
    RouterLink,
    MatTabsModule,
    MatButtonModule,
    MatIconModule,
    CmsStatusBadgeComponent,
    CmsSkeletonComponent],
  templateUrl: './student-detail.component.html',
  styleUrl: './student-detail.component.scss',
})
export class StudentDetailComponent implements OnInit {
  private readonly studentService = inject(StudentService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  protected readonly student = signal<Student | null>(null);
  protected readonly loading = signal(false);
  protected readonly enrollments = signal<StudentTermEnrollment[]>([]);
  protected readonly loadingEnrollments = signal(false);
  protected readonly registrationsByEnrollment = signal<Map<number, CourseRegistration[]>>(new Map());
  protected readonly loadingRegistrations = signal(false);

  /** First + last initial of the student's full name. */
  protected readonly initials = computed(() => computeInitials(this.student()?.fullName));

  protected readonly sortedEnrollments = computed(() =>
    [...this.enrollments()].sort((a, b) => b.semesterNumber - a.semesterNumber),
  );

  ngOnInit(): void {
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
}

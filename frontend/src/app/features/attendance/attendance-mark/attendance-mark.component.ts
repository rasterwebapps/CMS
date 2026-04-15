import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatRadioModule } from '@angular/material/radio';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments';
import { AttendanceService } from '../attendance.service';
import { BulkAttendanceRequest } from '../attendance.model';

interface Course {
  id: number;
  name: string;
}

interface StudentAttendanceRow {
  studentId: number;
  studentName: string;
  rollNumber: string;
  status: string;
}

@Component({
  selector: 'app-attendance-mark',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatRadioModule,
  ],
  templateUrl: './attendance-mark.component.html',
  styleUrl: './attendance-mark.component.scss',
})
export class AttendanceMarkComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly http = inject(HttpClient);
  private readonly attendanceService = inject(AttendanceService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly courses = signal<Course[]>([]);
  protected readonly students = signal<StudentAttendanceRow[]>([]);
  protected readonly loadingStudents = signal(false);
  protected readonly saving = signal(false);

  protected readonly typeOptions = ['THEORY', 'LAB'];

  protected readonly form: FormGroup = this.fb.group({
    courseId: [null, [Validators.required]],
    date: ['', [Validators.required]],
    type: ['THEORY', [Validators.required]],
  });

  ngOnInit(): void {
    this.loadCourses();
    this.form.get('courseId')?.valueChanges.subscribe((courseId) => {
      if (courseId) {
        this.loadStudentsForCourse(courseId);
      }
    });
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.students().length === 0) {
      this.snackBar.open('No students to mark attendance for', 'Close', { duration: 3000 });
      return;
    }

    const request: BulkAttendanceRequest = {
      courseId: this.form.value.courseId,
      date: this.form.value.date,
      type: this.form.value.type,
      records: this.students().map((s) => ({
        studentId: s.studentId,
        status: s.status,
      })),
    };

    this.saving.set(true);
    this.attendanceService.markBulk(request).subscribe({
      next: () => {
        this.snackBar.open('Attendance marked successfully', 'Close', { duration: 3000 });
        void this.router.navigate(['/attendance']);
      },
      error: () => {
        this.snackBar.open('Failed to mark attendance', 'Close', { duration: 3000 });
        this.saving.set(false);
      },
    });
  }

  protected setAllStatus(status: string): void {
    this.students.update((rows) => rows.map((r) => ({ ...r, status })));
  }

  protected setStudentStatus(studentId: number, status: string): void {
    this.students.update((rows) =>
      rows.map((r) => (r.studentId === studentId ? { ...r, status } : r))
    );
  }

  private loadCourses(): void {
    this.http.get<Course[]>(`${environment.apiUrl}/courses`).subscribe({
      next: (courses) => this.courses.set(courses),
      error: () => this.snackBar.open('Failed to load courses', 'Close', { duration: 3000 }),
    });
  }

  private loadStudentsForCourse(courseId: number): void {
    this.loadingStudents.set(true);
    this.http
      .get<Array<{ id: number; fullName: string; rollNumber: string }>>(
        `${environment.apiUrl}/students?courseId=${courseId}`
      )
      .subscribe({
        next: (students) => {
          this.students.set(
            students.map((s) => ({
              studentId: s.id,
              studentName: s.fullName,
              rollNumber: s.rollNumber,
              status: 'PRESENT',
            }))
          );
          this.loadingStudents.set(false);
        },
        error: () => {
          this.http
            .get<Array<{ id: number; fullName: string; rollNumber: string }>>(
              `${environment.apiUrl}/students`
            )
            .subscribe({
              next: (students) => {
                this.students.set(
                  students.map((s) => ({
                    studentId: s.id,
                    studentName: s.fullName,
                    rollNumber: s.rollNumber,
                    status: 'PRESENT',
                  }))
                );
                this.loadingStudents.set(false);
              },
              error: () => {
                this.snackBar.open('Failed to load students', 'Close', { duration: 3000 });
                this.loadingStudents.set(false);
              },
            });
        },
      });
  }
}

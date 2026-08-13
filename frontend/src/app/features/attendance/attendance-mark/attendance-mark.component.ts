import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatRadioModule } from '@angular/material/radio';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { AttendanceService } from '../attendance.service';
import { AvailableSubject, BulkAttendanceRequest } from '../attendance.model';
import { ToastService } from '../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';

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
    MatButtonModule,
    MatProgressSpinnerModule,
    MatRadioModule,
    PageHeaderComponent],
  templateUrl: './attendance-mark.component.html',
  styleUrl: './attendance-mark.component.scss',
})
export class AttendanceMarkComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly attendanceService = inject(AttendanceService);
  private readonly toast = inject(ToastService);

  /** Day-mapping- and blocked-period-aware -- resolved from the selected date, not a flat
   *  course list, so a compensatory working day correctly offers the borrowed weekday's
   *  subjects instead of the date's own (usually empty) actual-weekday list. */
  protected readonly availableSubjects = signal<AvailableSubject[]>([]);
  protected readonly loadingSubjects = signal(false);
  protected readonly students = signal<StudentAttendanceRow[]>([]);
  protected readonly loadingStudents = signal(false);
  protected readonly saving = signal(false);

  protected readonly typeOptions = ['THEORY', 'LAB', 'CLINICAL'];

  protected readonly form: FormGroup = this.fb.group({
    date: ['', [Validators.required]],
    subjectId: [null, [Validators.required]],
    type: ['THEORY', [Validators.required]],
  });

  ngOnInit(): void {
    this.form.get('date')?.valueChanges.subscribe((date) => {
      this.form.get('subjectId')?.setValue(null);
      this.students.set([]);
      if (date) {
        this.loadAvailableSubjects(date);
      } else {
        this.availableSubjects.set([]);
      }
    });
    this.form.get('subjectId')?.valueChanges.subscribe((subjectId) => {
      if (subjectId) {
        this.loadRosterForSubject(subjectId);
      } else {
        this.students.set([]);
      }
    });
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }
    if (this.students().length === 0) {
      this.toast.warning('No students to mark attendance for');
      return;
    }

    const request: BulkAttendanceRequest = {
      subjectId: this.form.value.subjectId,
      date: this.form.value.date,
      type: this.form.value.type,
      studentAttendances: this.students().map((s) => ({
        studentId: s.studentId,
        status: s.status,
      })),
    };

    this.saving.set(true);
    this.attendanceService.markBulk(request).subscribe({
      next: () => {
        this.toast.success('Attendance marked successfully');
        void this.router.navigate(['/attendance']);
      },
      error: () => {
        this.toast.error('Failed to mark attendance');
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

  private loadAvailableSubjects(date: string): void {
    this.loadingSubjects.set(true);
    this.attendanceService.getAvailableSubjects(date).subscribe({
      next: (subjects) => {
        this.availableSubjects.set(subjects);
        this.loadingSubjects.set(false);
      },
      error: () => {
        this.toast.error('Failed to load subjects for this date');
        this.availableSubjects.set([]);
        this.loadingSubjects.set(false);
      },
    });
  }

  private loadRosterForSubject(subjectId: number): void {
    this.loadingStudents.set(true);
    this.attendanceService.getSubjectRoster(subjectId).subscribe({
      next: (roster) => {
        this.students.set(
          roster.map((s) => ({
            studentId: s.id,
            studentName: s.fullName,
            rollNumber: s.rollNumber,
            status: 'PRESENT',
          }))
        );
        this.loadingStudents.set(false);
      },
      error: () => {
        this.toast.error('Failed to load students');
        this.loadingStudents.set(false);
      },
    });
  }
}

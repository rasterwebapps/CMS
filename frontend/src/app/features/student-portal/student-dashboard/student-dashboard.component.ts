import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { AttendanceService } from '../../attendance/attendance.service';
import { Attendance } from '../../attendance/attendance.model';
import { ExaminationService } from '../../examination/examination.service';
import { ExamResult } from '../../examination/examination.model';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { ToastService } from '../../../core/toast/toast.service';

/**
 * Student self-service dashboard — own attendance + own exam results only.
 * Never accepts a studentId; the backend resolves the caller's own linked
 * Student record from the JWT (see AttendanceController#myAttendance /
 * ExamResultController#myResults), so this component cannot be pointed at
 * anyone else's data even by a modified request.
 */
@Component({
  selector: 'app-student-dashboard',
  standalone: true,
  imports: [DatePipe, CmsStatusBadgeComponent],
  templateUrl: './student-dashboard.component.html',
  styleUrl: './student-dashboard.component.scss',
})
export class StudentDashboardComponent implements OnInit {
  private readonly attendanceService = inject(AttendanceService);
  private readonly examinationService = inject(ExaminationService);
  private readonly toast = inject(ToastService);

  protected readonly attendance = signal<Attendance[]>([]);
  protected readonly attendanceLoading = signal(false);
  protected readonly results = signal<ExamResult[]>([]);
  protected readonly resultsLoading = signal(false);

  protected readonly presentCount = signal(0);
  protected readonly attendancePercent = signal<number | null>(null);

  ngOnInit(): void {
    this.loadAttendance();
    this.loadResults();
  }

  private loadAttendance(): void {
    this.attendanceLoading.set(true);
    this.attendanceService.getMyAttendance().subscribe({
      next: (rows) => {
        this.attendance.set(rows);
        const present = rows.filter((r) => r.status === 'PRESENT').length;
        this.presentCount.set(present);
        this.attendancePercent.set(rows.length ? Math.round((present / rows.length) * 10000) / 100 : null);
        this.attendanceLoading.set(false);
      },
      error: () => { this.toast.error('Failed to load your attendance'); this.attendanceLoading.set(false); },
    });
  }

  private loadResults(): void {
    this.resultsLoading.set(true);
    this.examinationService.getMyResults().subscribe({
      next: (rows) => { this.results.set(rows); this.resultsLoading.set(false); },
      error: () => { this.toast.error('Failed to load your exam results'); this.resultsLoading.set(false); },
    });
  }
}

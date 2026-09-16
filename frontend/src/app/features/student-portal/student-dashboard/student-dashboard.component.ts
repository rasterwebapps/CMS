import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AttendanceService } from '../../attendance/attendance.service';
import { Attendance } from '../../attendance/attendance.model';
import { ExaminationService } from '../../examination/examination.service';
import { ExamResult } from '../../examination/examination.model';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { FinanceService } from '../../finance/finance.service';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';

/**
 * Student self-service dashboard — own attendance, exam results, and a compact fee-dues summary
 * card (the full ledger lives at /student/my-fees, per the specialist-review UI-placement
 * decision). Never accepts a studentId; the backend resolves the caller's own linked Student
 * record from the JWT (see AttendanceController#myAttendance / ExamResultController#myResults /
 * StudentFeeController's /my/* endpoints), so this component cannot be pointed at anyone else's
 * data even by a modified request.
 */
@Component({
  selector: 'app-student-dashboard',
  standalone: true,
  imports: [DatePipe, RouterLink, CmsStatusBadgeComponent, InrPipe],
  templateUrl: './student-dashboard.component.html',
  styleUrl: './student-dashboard.component.scss',
})
export class StudentDashboardComponent implements OnInit {
  private readonly attendanceService = inject(AttendanceService);
  private readonly examinationService = inject(ExaminationService);
  private readonly financeService = inject(FinanceService);
  protected readonly permissionService = inject(PermissionService);
  private readonly toast = inject(ToastService);

  protected readonly attendance = signal<Attendance[]>([]);
  protected readonly attendanceLoading = signal(false);
  protected readonly results = signal<ExamResult[]>([]);
  protected readonly resultsLoading = signal(false);

  protected readonly presentCount = signal(0);
  protected readonly attendancePercent = signal<number | null>(null);

  protected readonly feeDuesLoading = signal(false);
  protected readonly feeDuesTotal = signal<number | null>(null);
  protected readonly feeDuesLoaded = signal(false);

  ngOnInit(): void {
    this.loadAttendance();
    this.loadResults();
    if (this.permissionService.has('MY_FEE_VIEW')) {
      this.loadFeeDues();
    }
  }

  private loadFeeDues(): void {
    this.feeDuesLoading.set(true);
    this.financeService.getMyFeeSummary().subscribe({
      next: (result) => {
        const totalPending = result
          ? result.installmentFees.reduce((sum, i) => sum + i.pendingAmount, 0)
          : null;
        this.feeDuesTotal.set(totalPending);
        this.feeDuesLoaded.set(true);
        this.feeDuesLoading.set(false);
      },
      error: () => { this.toast.error('Failed to load your fee dues'); this.feeDuesLoading.set(false); },
    });
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

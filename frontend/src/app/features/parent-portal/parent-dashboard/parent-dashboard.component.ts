import { Component, effect, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { AttendanceService } from '../../attendance/attendance.service';
import { Attendance } from '../../attendance/attendance.model';
import { ExaminationService } from '../../examination/examination.service';
import { ExamResult } from '../../examination/examination.model';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { GuardianFeeService } from '../../guardian/guardian-fee.service';
import { PermissionService } from '../../../core/permissions/permission.service';
import { WardContextService } from '../../../core/ward-context/ward-context.service';
import { AnnouncementWidgetComponent } from '../../../shared/announcement-widget/announcement-widget.component';
import { ToastService } from '../../../core/toast/toast.service';

/**
 * Guardian self-service dashboard for the currently selected ward (see WardContextService /
 * WardSwitcherComponent) -- attendance, exam results, and a compact fee-dues card, mirroring
 * StudentDashboardComponent's own shape. Every backend call still passes the ward's studentId,
 * but the backend independently re-validates that studentId is actually one of the caller's own
 * wards before returning anything (GuardianService#assertIsMyWard) -- this component switching
 * wards client-side cannot itself grant access to a student who isn't really a ward.
 */
@Component({
  selector: 'app-parent-dashboard',
  standalone: true,
  imports: [DatePipe, RouterLink, MatIconModule, CmsStatusBadgeComponent, InrPipe, AnnouncementWidgetComponent],
  templateUrl: './parent-dashboard.component.html',
  styleUrl: './parent-dashboard.component.scss',
})
export class ParentDashboardComponent {
  private readonly attendanceService = inject(AttendanceService);
  private readonly examinationService = inject(ExaminationService);
  private readonly guardianFeeService = inject(GuardianFeeService);
  protected readonly permissionService = inject(PermissionService);
  protected readonly wardContext = inject(WardContextService);
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

  constructor() {
    // Reload every ward-scoped section whenever the active ward changes (including the initial
    // load once WardContextService resolves the caller's primary ward).
    effect(() => {
      const studentId = this.wardContext.selectedWardId();
      if (studentId == null) return;
      this.loadAttendance(studentId);
      this.loadResults(studentId);
      if (this.permissionService.has('MY_WARD_FEE_VIEW')) {
        this.loadFeeDues(studentId);
      }
    });
  }

  private loadFeeDues(studentId: number): void {
    this.feeDuesLoading.set(true);
    this.guardianFeeService.getWardFeeSummary(studentId).subscribe({
      next: (result) => {
        const totalPending = result
          ? result.installmentFees.reduce((sum, i) => sum + i.pendingAmount, 0)
          : null;
        this.feeDuesTotal.set(totalPending);
        this.feeDuesLoaded.set(true);
        this.feeDuesLoading.set(false);
      },
      error: () => { this.toast.error("Failed to load your ward's fee dues"); this.feeDuesLoading.set(false); },
    });
  }

  private loadAttendance(studentId: number): void {
    this.attendanceLoading.set(true);
    this.attendanceService.getWardAttendance(studentId).subscribe({
      next: (rows) => {
        this.attendance.set(rows);
        const present = rows.filter((r) => r.status === 'PRESENT').length;
        this.presentCount.set(present);
        this.attendancePercent.set(rows.length ? Math.round((present / rows.length) * 10000) / 100 : null);
        this.attendanceLoading.set(false);
      },
      error: () => { this.toast.error("Failed to load your ward's attendance"); this.attendanceLoading.set(false); },
    });
  }

  private loadResults(studentId: number): void {
    this.resultsLoading.set(true);
    this.examinationService.getWardResults(studentId).subscribe({
      next: (rows) => { this.results.set(rows); this.resultsLoading.set(false); },
      error: () => { this.toast.error("Failed to load your ward's exam results"); this.resultsLoading.set(false); },
    });
  }
}

import { Component, inject, OnInit, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { KeyValuePipe } from '@angular/common';
import { ReportsService } from '../reports.service';
import { LabUtilizationReport, AttendanceAnalyticsReport } from '../reports.model';
import { ToastService } from '../../../core/toast/toast.service';
import { TourService } from '../../../shared/tour/tour.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { REPORTS_DASHBOARD_TOUR, REPORTS_DASHBOARD_FLOW_MAP } from '../../../shared/tour/tours/reports.tours';

@Component({
  selector: 'app-reports-dashboard',
  standalone: true,
  imports: [
    KeyValuePipe,
    MatIconModule,
    MatProgressSpinnerModule,
    CmsTourButtonComponent],
  templateUrl: './reports-dashboard.component.html',
  styleUrl: './reports-dashboard.component.scss',
})
export class ReportsDashboardComponent implements OnInit {
  private readonly reportsService = inject(ReportsService);
  private readonly toast = inject(ToastService);
  private readonly tourService = inject(TourService);

  protected readonly loading = signal(false);
  protected readonly labReport = signal<LabUtilizationReport | null>(null);
  protected readonly attendanceReport = signal<AttendanceAnalyticsReport | null>(null);

  ngOnInit(): void {
    this.tourService.register('reports-dashboard', REPORTS_DASHBOARD_TOUR);
    this.tourService.registerFlowMap('reports-dashboard', REPORTS_DASHBOARD_FLOW_MAP);

    this.loading.set(true);
    let completed = 0;
    const checkDone = () => { completed++; if (completed === 2) this.loading.set(false); };

    this.reportsService.getLabUtilization().subscribe({
      next: (data) => { this.labReport.set(data); checkDone(); },
      error: () => { this.toast.error('Failed to load lab utilization'); checkDone(); },
    });

    this.reportsService.getAttendanceAnalytics().subscribe({
      next: (data) => { this.attendanceReport.set(data); checkDone(); },
      error: () => { this.toast.error('Failed to load attendance analytics'); checkDone(); },
    });
  }

  protected formatStatus(status: string): string {
    return status.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
  }
}

import { Component, inject, OnInit, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { KeyValuePipe } from '@angular/common';
import { ReportsService } from '../reports.service';
import { LabUtilizationReport, AttendanceAnalyticsReport } from '../reports.model';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { ToastService } from '../../../core/toast/toast.service';

@Component({
  selector: 'app-reports-dashboard',
  standalone: true,
  imports: [
    PageHeaderComponent,
    KeyValuePipe,
    MatIconModule,
    MatProgressSpinnerModule],
  templateUrl: './reports-dashboard.component.html',
  styleUrl: './reports-dashboard.component.scss',
})
export class ReportsDashboardComponent implements OnInit {
  private readonly reportsService = inject(ReportsService);
  private readonly toast = inject(ToastService);

  protected readonly loading = signal(false);
  protected readonly labReport = signal<LabUtilizationReport | null>(null);
  protected readonly attendanceReport = signal<AttendanceAnalyticsReport | null>(null);

  ngOnInit(): void {
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

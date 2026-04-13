import { Component, inject, OnInit, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ReportsService } from '../reports.service';
import { LabUtilizationReport, AttendanceAnalyticsReport } from '../reports.model';

@Component({
  selector: 'app-reports-dashboard',
  standalone: true,
  imports: [
    MatCardModule, MatIconModule, MatProgressSpinnerModule, MatSnackBarModule,
  ],
  templateUrl: './reports-dashboard.component.html',
  styleUrl: './reports-dashboard.component.scss',
})
export class ReportsDashboardComponent implements OnInit {
  private readonly reportsService = inject(ReportsService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly loading = signal(false);
  protected readonly labReport = signal<LabUtilizationReport | null>(null);
  protected readonly attendanceReport = signal<AttendanceAnalyticsReport | null>(null);

  ngOnInit(): void {
    this.loading.set(true);
    let completed = 0;
    const checkDone = () => { completed++; if (completed === 2) this.loading.set(false); };

    this.reportsService.getLabUtilization().subscribe({
      next: (data) => { this.labReport.set(data); checkDone(); },
      error: () => { this.snackBar.open('Failed to load lab utilization', 'Close', { duration: 3000 }); checkDone(); },
    });

    this.reportsService.getAttendanceAnalytics().subscribe({
      next: (data) => { this.attendanceReport.set(data); checkDone(); },
      error: () => { this.snackBar.open('Failed to load attendance analytics', 'Close', { duration: 3000 }); checkDone(); },
    });
  }
}

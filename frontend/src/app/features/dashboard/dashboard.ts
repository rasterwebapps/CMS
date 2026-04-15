import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { KeyValuePipe } from '@angular/common';
import { AuthService } from '../../core/auth/auth.service';
import { environment } from '../../../environments';

export interface DashboardSummary {
  totalStudents: number;
  totalFaculty: number;
  totalDepartments: number;
  totalCourses: number;
  totalPrograms: number;
  totalLabs: number;
  totalEquipment: number;
  totalExaminations: number;
  totalFeePayments: number;
  totalMaintenanceRequests: number;
  totalAttendanceRecords: number;
  equipmentByStatus: Record<string, number>;
  maintenanceByStatus: Record<string, number>;
  studentsByStatus: Record<string, number>;
  attendanceByStatus: Record<string, number>;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [MatCardModule, MatIconModule, MatProgressSpinnerModule, KeyValuePipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class DashboardComponent implements OnInit {
  protected readonly authService = inject(AuthService);
  private readonly http = inject(HttpClient);

  protected readonly loading = signal(true);
  protected readonly summary = signal<DashboardSummary | null>(null);

  protected readonly cards = signal<
    { title: string; value: string; icon: string; color: string }[]
  >([
    { title: 'Total Students', value: '—', icon: 'school', color: 'primary' },
    { title: 'Total Faculty', value: '—', icon: 'person', color: 'accent' },
    { title: 'Departments', value: '—', icon: 'business', color: 'warn' },
    { title: 'Active Courses', value: '—', icon: 'menu_book', color: 'primary' },
    { title: 'Programs', value: '—', icon: 'auto_stories', color: 'accent' },
    { title: 'Labs', value: '—', icon: 'science', color: 'warn' },
    { title: 'Equipment', value: '—', icon: 'precision_manufacturing', color: 'primary' },
    { title: 'Examinations', value: '—', icon: 'quiz', color: 'accent' },
    { title: 'Fee Payments', value: '—', icon: 'payments', color: 'warn' },
    { title: 'Maintenance', value: '—', icon: 'build', color: 'primary' },
    { title: 'Attendance Records', value: '—', icon: 'fact_check', color: 'accent' },
  ]);

  ngOnInit(): void {
    this.http
      .get<DashboardSummary>(`${environment.apiUrl}/dashboard/summary`)
      .subscribe({
        next: (data) => {
          this.summary.set(data);
          this.cards.set([
            { title: 'Total Students', value: String(data.totalStudents), icon: 'school', color: 'primary' },
            { title: 'Total Faculty', value: String(data.totalFaculty), icon: 'person', color: 'accent' },
            { title: 'Departments', value: String(data.totalDepartments), icon: 'business', color: 'warn' },
            { title: 'Active Courses', value: String(data.totalCourses), icon: 'menu_book', color: 'primary' },
            { title: 'Programs', value: String(data.totalPrograms), icon: 'auto_stories', color: 'accent' },
            { title: 'Labs', value: String(data.totalLabs), icon: 'science', color: 'warn' },
            { title: 'Equipment', value: String(data.totalEquipment), icon: 'precision_manufacturing', color: 'primary' },
            { title: 'Examinations', value: String(data.totalExaminations), icon: 'quiz', color: 'accent' },
            { title: 'Fee Payments', value: String(data.totalFeePayments), icon: 'payments', color: 'warn' },
            { title: 'Maintenance', value: String(data.totalMaintenanceRequests), icon: 'build', color: 'primary' },
            { title: 'Attendance Records', value: String(data.totalAttendanceRecords), icon: 'fact_check', color: 'accent' },
          ]);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
        },
      });
  }

  protected formatStatus(status: string): string {
    return status.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
  }
}

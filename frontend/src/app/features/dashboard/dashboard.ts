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
    { title: 'Total Students', value: '—', icon: 'school', color: 'indigo' },
    { title: 'Total Faculty', value: '—', icon: 'groups', color: 'emerald' },
    { title: 'Departments', value: '—', icon: 'business', color: 'amber' },
    { title: 'Active Courses', value: '—', icon: 'menu_book', color: 'rose' },
    { title: 'Programs', value: '—', icon: 'auto_stories', color: 'blue' },
    { title: 'Labs', value: '—', icon: 'science', color: 'violet' },
    { title: 'Equipment', value: '—', icon: 'precision_manufacturing', color: 'cyan' },
    { title: 'Examinations', value: '—', icon: 'quiz', color: 'orange' },
    { title: 'Fee Payments', value: '—', icon: 'payments', color: 'teal' },
    { title: 'Maintenance', value: '—', icon: 'build', color: 'pink' },
    { title: 'Attendance', value: '—', icon: 'fact_check', color: 'sky' },
  ]);

  ngOnInit(): void {
    this.http
      .get<DashboardSummary>(`${environment.apiUrl}/dashboard/summary`)
      .subscribe({
        next: (data) => {
          this.summary.set(data);
          this.cards.set([
            { title: 'Total Students', value: String(data.totalStudents), icon: 'school', color: 'indigo' },
            { title: 'Total Faculty', value: String(data.totalFaculty), icon: 'groups', color: 'emerald' },
            { title: 'Departments', value: String(data.totalDepartments), icon: 'business', color: 'amber' },
            { title: 'Active Courses', value: String(data.totalCourses), icon: 'menu_book', color: 'rose' },
            { title: 'Programs', value: String(data.totalPrograms), icon: 'auto_stories', color: 'blue' },
            { title: 'Labs', value: String(data.totalLabs), icon: 'science', color: 'violet' },
            { title: 'Equipment', value: String(data.totalEquipment), icon: 'precision_manufacturing', color: 'cyan' },
            { title: 'Examinations', value: String(data.totalExaminations), icon: 'quiz', color: 'orange' },
            { title: 'Fee Payments', value: String(data.totalFeePayments), icon: 'payments', color: 'teal' },
            { title: 'Maintenance', value: String(data.totalMaintenanceRequests), icon: 'build', color: 'pink' },
            { title: 'Attendance', value: String(data.totalAttendanceRecords), icon: 'fact_check', color: 'sky' },
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

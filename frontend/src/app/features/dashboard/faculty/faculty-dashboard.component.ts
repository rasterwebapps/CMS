import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../core/auth/auth.service';
import { environment } from '../../../../environments';
import { FacultyDashboard } from '../dashboard.models';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';

/**
 * Faculty role dashboard. Renders the faculty member's classes for the day,
 * pending attendance submissions, and upcoming lab slots.
 *
 * Backed by `GET /api/dashboard/faculty`. If the endpoint is not yet available,
 * the component renders a full-page empty state — Phase 4 ships the shell so
 * the route is wired even before the backend is ready.
 */
@Component({
  selector: 'app-faculty-dashboard',
  standalone: true,
  imports: [
    AppDatePipe,RouterLink, DatePipe, MatIconModule],
  templateUrl: './faculty-dashboard.component.html',
  styleUrl: './faculty-dashboard.component.scss',
})
export class FacultyDashboardComponent implements OnInit {
  protected readonly authService = inject(AuthService);
  private readonly http = inject(HttpClient);

  protected readonly loading = signal(true);
  protected readonly facultyData = signal<FacultyDashboard | null>(null);
  /** True when the backend endpoint is missing — drives the full-page empty state. */
  protected readonly endpointMissing = signal(false);

  protected readonly today = new Date().toLocaleDateString('en-IN', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });

  ngOnInit(): void {
    this.http.get<FacultyDashboard>(`${environment.apiUrl}/dashboard/faculty`).subscribe({
      next: (data) => {
        this.facultyData.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.endpointMissing.set(true);
        this.loading.set(false);
      },
    });
  }
}

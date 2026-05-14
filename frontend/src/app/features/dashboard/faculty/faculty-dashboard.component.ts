import { Component, Input, inject, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../core/auth/auth.service';
import { environment } from '../../../../environments';
import { FacultyDashboard } from '../dashboard.models';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { ProfileService } from '../../profile/profile.service';
import { DocumentSlotsService } from '../services/document-slots.service';
import { DocumentStatsRowComponent } from '../widgets/document-stats-row/document-stats-row.component';
import { CompletionRingComponent } from '../widgets/completion-ring/completion-ring.component';
import { RecentActivityComponent } from '../widgets/recent-activity/recent-activity.component';
import { ConnectionsCardComponent } from '../widgets/connections-card/connections-card.component';

/**
 * Faculty role dashboard. Renders the faculty member's classes for the day,
 * pending attendance submissions, upcoming lab slots, plus the personal-document
 * widgets (stats row + completion ring) and operational widgets (recent activity,
 * colleagues) that previously lived on the Profile page.
 *
 * Backed by `GET /api/dashboard/faculty`. If the endpoint is not yet available,
 * the component renders a full-page empty state — Phase 4 ships the shell so
 * the route is wired even before the backend is ready.
 */
@Component({
  selector: 'app-faculty-dashboard',
  standalone: true,
  imports: [
    AppDatePipe, RouterLink, DatePipe, MatIconModule,
    DocumentStatsRowComponent, CompletionRingComponent,
    RecentActivityComponent, ConnectionsCardComponent,
  ],
  templateUrl: './faculty-dashboard.component.html',
  styleUrl: './faculty-dashboard.component.scss',
})
export class FacultyDashboardComponent implements OnInit {
  @Input() visibleWidgets: string[] | null = null;

  protected show(key: string): boolean {
    return this.visibleWidgets === null || this.visibleWidgets.includes(key);
  }

  protected readonly authService = inject(AuthService);
  private readonly http = inject(HttpClient);
  private readonly profileService = inject(ProfileService);
  protected readonly docSlots = inject(DocumentSlotsService);

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
    // Fetch the faculty's document slots so the doc-stats / completion widgets
    // are populated even if the user lands here directly (without first visiting
    // the Profile screen, which is the other publisher of the same signal).
    this.profileService.getMyProfile().subscribe({
      next: (id) => {
        if (id.entityType === 'FACULTY' && id.entityId) {
          this.docSlots.loadFaculty(id.entityId);
        }
      },
    });

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

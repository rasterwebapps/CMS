import { Component, Input, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../core/auth/auth.service';
import { ProfileService, ProfileIdentity } from '../../profile/profile.service';
import { DocumentSlotsService } from '../services/document-slots.service';
import { DocumentStatsRowComponent } from '../widgets/document-stats-row/document-stats-row.component';
import { CompletionRingComponent } from '../widgets/completion-ring/completion-ring.component';
import { RecentActivityComponent } from '../widgets/recent-activity/recent-activity.component';

/**
 * Student role dashboard. Shows the student's document checklist progress
 * (stats row + completion ring) and recent activity. Pure operational view —
 * personal identity / profile editing lives on the Profile screen.
 */
@Component({
  selector: 'app-student-dashboard',
  standalone: true,
  imports: [
    RouterLink, MatIconModule,
    DocumentStatsRowComponent, CompletionRingComponent, RecentActivityComponent,
  ],
  templateUrl: './student-dashboard.component.html',
  styleUrl: './student-dashboard.component.scss',
})
export class StudentDashboardComponent implements OnInit {
  @Input() visibleWidgets: string[] | null = null;

  protected show(key: string): boolean {
    return this.visibleWidgets === null || this.visibleWidgets.includes(key);
  }

  protected readonly authService = inject(AuthService);
  private readonly profileService = inject(ProfileService);
  protected readonly docSlots = inject(DocumentSlotsService);

  protected readonly identity = signal<ProfileIdentity | null>(null);

  protected readonly today = new Date().toLocaleDateString('en-IN', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });

  ngOnInit(): void {
    this.profileService.getMyProfile().subscribe({
      next: (id) => {
        this.identity.set(id);
        if (id.entityType === 'STUDENT' && id.admissionId) {
          this.docSlots.loadStudent(id.admissionId);
        }
      },
    });
  }
}


import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { DatePipe } from '@angular/common';
import { FacultyService } from '../faculty.service';
import { Faculty, FacultyStatus, DESIGNATION_OPTIONS, FACULTY_STATUS_OPTIONS } from '../faculty.model';
import { AuthService } from '../../../core/auth/auth.service';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { CmsSkeletonComponent } from '../../../shared/skeleton/skeleton.component';
import { computeInitials } from '../../../shared/utils/initials';

@Component({
  selector: 'app-faculty-detail',
  standalone: true,
  imports: [
    RouterLink,
    MatTabsModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    DatePipe,
    CmsStatusBadgeComponent,
    CmsSkeletonComponent,
  ],
  templateUrl: './faculty-detail.component.html',
  styleUrl: './faculty-detail.component.scss',
})
export class FacultyDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly facultyService = inject(FacultyService);
  private readonly snackBar = inject(MatSnackBar);
  protected readonly authService = inject(AuthService);

  protected readonly faculty = signal<Faculty | null>(null);
  protected readonly loading = signal(true);

  /** First + last initial of the faculty's full name. */
  protected readonly initials = computed(() => computeInitials(this.faculty()?.fullName));

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadFaculty(+id);
    }
  }

  private loadFaculty(id: number): void {
    this.loading.set(true);
    this.facultyService.getById(id).subscribe({
      next: (faculty) => {
        this.faculty.set(faculty);
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Error loading faculty details', 'Close', { duration: 3000 });
        this.loading.set(false);
        void this.router.navigate(['/faculty']);
      },
    });
  }

  protected getStatusColor(status: FacultyStatus): string {
    switch (status) {
      case 'ACTIVE':
        return 'primary';
      case 'ON_LEAVE':
        return 'accent';
      case 'SABBATICAL':
        return 'accent';
      default:
        return 'warn';
    }
  }

  protected getDesignationLabel(designation: string): string {
    const option = DESIGNATION_OPTIONS.find((o) => o.value === designation);
    return option ? option.label : designation;
  }

  protected getStatusLabel(status: FacultyStatus): string {
    const option = FACULTY_STATUS_OPTIONS.find((o) => o.value === status);
    return option ? option.label : status;
  }

  protected editFaculty(): void {
    const faculty = this.faculty();
    if (faculty) {
      void this.router.navigate(['/faculty', faculty.id, 'edit']);
    }
  }
}

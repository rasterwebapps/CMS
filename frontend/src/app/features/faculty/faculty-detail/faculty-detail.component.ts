import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { FacultyService } from '../faculty.service';
import { Faculty, FacultyStatus, DESIGNATION_OPTIONS, FACULTY_STATUS_OPTIONS } from '../faculty.model';
import { AuthService } from '../../../core/auth/auth.service';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';

@Component({
  selector: 'app-faculty-detail',
  standalone: true,
  imports: [
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    PageHeaderComponent,
    CmsStatusBadgeComponent,
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

  protected formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString();
  }

  protected editFaculty(): void {
    const faculty = this.faculty();
    if (faculty) {
      void this.router.navigate(['/faculty', faculty.id, 'edit']);
    }
  }
}

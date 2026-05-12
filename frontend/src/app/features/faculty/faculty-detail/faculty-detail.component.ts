import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { FacultyService } from '../faculty.service';
import { Faculty, DESIGNATION_OPTIONS } from '../faculty.model';
import { PermissionService } from '../../../core/permissions/permission.service';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { CmsSkeletonComponent } from '../../../shared/skeleton/skeleton.component';
import { computeInitials } from '../../../shared/utils/initials';
import { ToastService } from '../../../core/toast/toast.service';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';

@Component({
  selector: 'app-faculty-detail',
  standalone: true,
  imports: [
    AppDatePipe,
    RouterLink,
    MatTabsModule,
    MatButtonModule,
    MatIconModule,
    CmsStatusBadgeComponent,
    CmsSkeletonComponent],
  templateUrl: './faculty-detail.component.html',
  styleUrl: './faculty-detail.component.scss',
})
export class FacultyDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly facultyService = inject(FacultyService);
  private readonly toast = inject(ToastService);
  private readonly permissionService = inject(PermissionService);

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
        this.toast.error('Error loading faculty details');
        this.loading.set(false);
        void this.router.navigate(['/faculty']);
      },
    });
  }

  protected getDesignationLabel(designation: string): string {
    const option = DESIGNATION_OPTIONS.find((o) => o.value === designation);
    return option ? option.label : designation;
  }


  protected canManageFaculty(): boolean {
    return this.permissionService.has('FACULTY_MANAGE');
  }

  protected editFaculty(): void {
    if (!this.canManageFaculty()) return;

    const faculty = this.faculty();
    if (faculty) {
      void this.router.navigate(['/faculty', faculty.id, 'edit']);
    }
  }
}

import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { FacultyService } from '../faculty.service';
import { Faculty, FacultyQualification, FACULTY_QUALIFICATION_OPTIONS } from '../faculty.model';
import { PermissionService } from '../../../core/permissions/permission.service';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { CmsSkeletonComponent } from '../../../shared/skeleton/skeleton.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { ProfileDocumentsComponent } from '../../../shared/profile-documents/profile-documents.component';
import { computeInitials } from '../../../shared/utils/initials';
import { ToastService } from '../../../core/toast/toast.service';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';

@Component({
  selector: 'app-faculty-detail',
  standalone: true,
  imports: [
    AppDatePipe,
    MatTabsModule,
    MatButtonModule,
    MatIconModule,
    CmsStatusBadgeComponent,
    CmsSkeletonComponent,
    CmsEmptyStateComponent,
    ProfileDocumentsComponent],
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
  protected readonly selectedTabIndex = signal(0);

  /** First + last initial of the faculty's full name. */
  protected readonly initials = computed(() => computeInitials(this.faculty()?.fullName));

  ngOnInit(): void {
    this.route.fragment.subscribe((fragment) => {
      this.selectedTabIndex.set(fragment === 'documents' ? 4 : 0);
    });

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

  protected viewCourses(): void {
    void this.router.navigate(['/courses']);
  }

  protected viewLabSchedules(): void {
    void this.router.navigate(['/lab-schedules']);
  }

  protected getDesignationLabel(faculty: Faculty): string {
    return faculty.designationName ?? '—';
  }

  protected qualificationLabel(q: FacultyQualification): string {
    return FACULTY_QUALIFICATION_OPTIONS.find((o) => o.value === q)?.label ?? q;
  }


  protected canManageFaculty(): boolean {
    return this.permissionService.has('FACULTY_MANAGE');
  }

  protected canForceReplaceDocuments(): boolean {
    return this.permissionService.has('DOCUMENT_VERIFIED_OVERRIDE');
  }

  protected editFaculty(): void {
    if (!this.canManageFaculty()) return;

    const faculty = this.faculty();
    if (faculty) {
      void this.router.navigate(['/faculty', faculty.id, 'edit']);
    }
  }
}

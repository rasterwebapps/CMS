import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TitleCasePipe } from '@angular/common';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { ProfileService, ProfileIdentity } from './profile.service';
import { FacultyService } from '../faculty/faculty.service';
import { StudentService } from '../student/student.service';
import { Faculty } from '../faculty/faculty.model';
import { Student } from '../student/student.model';
import { ProfileDocumentsComponent } from '../../shared/profile-documents/profile-documents.component';
import { CmsSkeletonComponent } from '../../shared/skeleton/skeleton.component';
import { computeInitials } from '../../shared/utils/initials';
import { AppDatePipe } from '../../shared/pipes/app-date.pipe';
import { ToastService } from '../../core/toast/toast.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    RouterLink,
    TitleCasePipe,
    MatTabsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    ProfileDocumentsComponent,
    CmsSkeletonComponent,
    AppDatePipe,
  ],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss',
})
export class ProfileComponent implements OnInit {
  private readonly profileService = inject(ProfileService);
  private readonly facultyService = inject(FacultyService);
  private readonly studentService = inject(StudentService);
  private readonly toast = inject(ToastService);

  protected readonly loading = signal(true);
  protected readonly identity = signal<ProfileIdentity | null>(null);
  protected readonly faculty = signal<Faculty | null>(null);
  protected readonly student = signal<Student | null>(null);

  protected get initials(): string {
    const name = this.identity()?.displayName ?? '';
    return computeInitials(name);
  }

  ngOnInit(): void {
    this.profileService.getMyProfile().subscribe({
      next: (identity) => {
        this.identity.set(identity);
        if (identity.entityType === 'FACULTY' && identity.entityId) {
          this.loadFaculty(identity.entityId);
        } else if (identity.entityType === 'STUDENT' && identity.entityId) {
          this.loadStudent(identity.entityId);
        } else {
          this.loading.set(false);
        }
      },
      error: () => {
        this.toast.error('Failed to load profile');
        this.loading.set(false);
      },
    });
  }

  private loadFaculty(id: number): void {
    this.facultyService.getById(id).subscribe({
      next: (f) => { this.faculty.set(f); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load faculty profile'); this.loading.set(false); },
    });
  }

  private loadStudent(id: number): void {
    this.studentService.getById(id).subscribe({
      next: (s) => { this.student.set(s); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load student profile'); this.loading.set(false); },
    });
  }
}

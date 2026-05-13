import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TitleCasePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { catchError, of } from 'rxjs';

import { environment } from '../../../environments';
import { ProfileService, ProfileIdentity } from './profile.service';
import { FacultyService } from '../faculty/faculty.service';
import { StudentService } from '../student/student.service';
import { Faculty, FacultyQualification, FACULTY_QUALIFICATION_OPTIONS } from '../faculty/faculty.model';
import { Student } from '../student/student.model';
import { DashboardSummary } from '../dashboard/dashboard.models';
import { ProfileDocumentsComponent } from '../../shared/profile-documents/profile-documents.component';
import { computeInitials } from '../../shared/utils/initials';
import { AppDatePipe } from '../../shared/pipes/app-date.pipe';
import { ToastService } from '../../core/toast/toast.service';

export interface InfoItem { icon: string; label: string; value: string; }
export interface DocStats  { total: number; verified: number; pending: number; missing: number; }

export interface QuickLink {
  icon: string; label: string; route: string;
  desc: string; color: string;
}

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    RouterLink,
    TitleCasePipe,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    ProfileDocumentsComponent,
    AppDatePipe,
  ],
  templateUrl: './profile.component.html',
  styleUrl:    './profile.component.scss',
})
export class ProfileComponent implements OnInit {
  private readonly profileService = inject(ProfileService);
  private readonly facultyService = inject(FacultyService);
  private readonly studentService = inject(StudentService);
  private readonly http           = inject(HttpClient);
  private readonly toast          = inject(ToastService);

  protected readonly loading   = signal(true);
  protected readonly identity  = signal<ProfileIdentity | null>(null);
  protected readonly faculty   = signal<Faculty | null>(null);
  protected readonly student   = signal<Student | null>(null);
  protected readonly summary   = signal<DashboardSummary | null>(null);

  private readonly slots = signal<{ status: string }[]>([]);

  protected readonly docStats = computed<DocStats>(() => {
    const s = this.slots();
    return {
      total:    s.length,
      verified: s.filter(x => x.status === 'VERIFIED').length,
      pending:  s.filter(x => x.status === 'UPLOADED').length,
      missing:  s.filter(x => x.status !== 'VERIFIED' && x.status !== 'UPLOADED').length,
    };
  });

  protected readonly progressPct = computed(() => {
    const { total, verified } = this.docStats();
    return total === 0 ? 0 : Math.round((verified / total) * 100);
  });

  protected readonly progressDashOffset = computed(() =>
    113.1 - (113.1 * this.progressPct()) / 100,
  );

  protected get initials(): string {
    return computeInitials(this.identity()?.displayName ?? '');
  }

  /** Route to the canonical edit form for the current user (faculty/student). */
  protected readonly editRoute = computed<string | null>(() => {
    const id = this.identity();
    if (!id?.entityId) return null;
    if (id.entityType === 'FACULTY') return `/faculty/${id.entityId}/edit`;
    if (id.entityType === 'STUDENT') return `/students/${id.entityId}/edit`;
    return null;
  });

  readonly adminQuickLinks: QuickLink[] = [
    { icon: 'school',        label: 'Programs',       route: '/programs',     desc: 'Manage academic programs',      color: 'indigo'  },
    { icon: 'business',      label: 'Departments',    route: '/departments',  desc: 'Manage departments',            color: 'violet'  },
    { icon: 'groups',        label: 'Faculty',        route: '/faculty',      desc: 'Faculty members & documents',   color: 'blue'    },
    { icon: 'person',        label: 'Students',       route: '/students',     desc: 'Student records & profiles',    color: 'sky'     },
    { icon: 'contact_mail',  label: 'Enquiries',      route: '/enquiries',    desc: 'Admission enquiries',           color: 'teal'    },
    { icon: 'assignment_ind',label: 'Admissions',     route: '/admissions',   desc: 'Student admissions',            color: 'emerald' },
    { icon: 'upload_file',   label: 'Documents',      route: '/enquiries/document-submission', desc: 'Submit & verify documents', color: 'cyan' },
    { icon: 'account_balance_wallet', label: 'Fees',  route: '/student-fees', desc: 'Fee structures & payments',    color: 'rose'    },
    { icon: 'manage_accounts', label: 'Users',        route: '/user-management', desc: 'User roles & access',       color: 'pink'    },
    { icon: 'assessment',    label: 'Reports',        route: '/reports',      desc: 'Academic & financial reports',  color: 'amber'   },
  ];

  ngOnInit(): void {
    this.loading.set(true);
    this.identity.set(null);
    this.profileService.getMyProfile().subscribe({
      next: (id) => {
        this.identity.set(id);
        if (id.entityType === 'FACULTY' && id.entityId) {
          this.facultyService.getById(id.entityId).subscribe({
            next:  (f) => { this.faculty.set(f); this.loading.set(false); },
            error: () => { this.toast.error('Failed to load faculty profile'); this.loading.set(false); },
          });
        } else if (id.entityType === 'STUDENT' && id.entityId) {
          this.studentService.getById(id.entityId).subscribe({
            next:  (s) => { this.student.set(s); this.loading.set(false); },
            error: () => { this.toast.error('Failed to load student profile'); this.loading.set(false); },
          });
        } else {
          // Admin — load summary stats
          this.http.get<DashboardSummary>(`${environment.apiUrl}/dashboard/summary`)
            .pipe(catchError(() => of(null)))
            .subscribe(s => { this.summary.set(s); this.loading.set(false); });
        }
      },
      error: () => {
        this.toast.error('Failed to load profile');
        this.loading.set(false);
      },
    });
  }

  protected onSlotsChange(slots: { status: string }[]): void {
    this.slots.set(slots);
  }

  protected qualificationLabel(q: FacultyQualification): string {
    return FACULTY_QUALIFICATION_OPTIONS.find(o => o.value === q)?.label ?? q;
  }

  protected facultyInfoItems(f: Faculty): InfoItem[] {
    return [
      { icon: 'person',         label: 'Full Name',     value: f.fullName },
      { icon: 'email',          label: 'Email',         value: f.email },
      { icon: 'phone',          label: 'Phone',         value: f.phone ?? '' },
      { icon: 'business',       label: 'Department',    value: f.departmentName },
      { icon: 'work',           label: 'Designation',   value: f.designation.replace(/_/g,' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase()) },
      { icon: 'school',         label: 'Qualification', value: f.highestQualification ? this.qualificationLabel(f.highestQualification) : '' },
      { icon: 'badge',          label: 'Employee Code', value: f.employeeCode },
      { icon: 'calendar_today', label: 'Joining Date',  value: f.joiningDate },
      { icon: 'info',           label: 'Status',        value: f.status.replace(/_/g,' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase()) },
    ].filter(i => !!i.value);
  }

  protected studentInfoItems(s: Student): InfoItem[] {
    return [
      { icon: 'person',         label: 'Full Name',     value: s.fullName },
      { icon: 'email',          label: 'Email',         value: s.email },
      { icon: 'phone',          label: 'Phone',         value: s.phone ?? '' },
      { icon: 'school',         label: 'Program',       value: s.programName },
      { icon: 'layers',         label: 'Year of Study', value: `Year ${s.yearOfStudy}` },
      { icon: 'tag',            label: 'Roll Number',   value: s.rollNumber ?? '' },
      { icon: 'calendar_today', label: 'Admitted',      value: s.admissionDate },
      { icon: 'info',           label: 'Status',        value: s.status.replace(/_/g,' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase()) },
    ].filter(i => !!i.value);
  }
}

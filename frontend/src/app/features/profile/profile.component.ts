import {
  Component, computed, DestroyRef, HostListener, inject, OnInit, signal,
} from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { TitleCasePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, interval, of, take } from 'rxjs';

import { environment } from '../../../environments';
import { ProfileService, ProfileIdentity } from './profile.service';
import { FacultyService } from '../faculty/faculty.service';
import { StudentService } from '../student/student.service';
import { Faculty, FacultyQualification, FACULTY_QUALIFICATION_OPTIONS } from '../faculty/faculty.model';
import { Student } from '../student/student.model';
import { DashboardSummary } from '../dashboard/dashboard.models';
import { ThemeService, COLOR_SWATCHES, ColorSwatch } from '../../core/theme/theme.service';
import { ProfileDocumentsComponent } from '../../shared/profile-documents/profile-documents.component';
import { CmsSkeletonComponent } from '../../shared/skeleton/skeleton.component';
import { computeInitials } from '../../shared/utils/initials';
import { AppDatePipe } from '../../shared/pipes/app-date.pipe';
import { ToastService } from '../../core/toast/toast.service';

export interface InfoItem { icon: string; label: string; value: string; }
export interface DocStats { total: number; verified: number; pending: number; missing: number; }

interface DockItem       { icon: string; label: string; route: string; }
interface ActivityItem   { id: number; icon: string; text: string; time: string; color: string; }
interface ConnectionItem { id: number; name: string; initials: string; role: string; online: boolean; }
interface AdminLink      { icon: string; label: string; route: string; desc: string; cc: string; }

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    RouterLink, RouterLinkActive, TitleCasePipe,
    MatButtonModule, MatIconModule, MatTooltipModule,
    ProfileDocumentsComponent, CmsSkeletonComponent, AppDatePipe,
  ],
  templateUrl: './profile.component.html',
  styleUrl:    './profile.component.scss',
})
export class ProfileComponent implements OnInit {

  private readonly profileService = inject(ProfileService);
  private readonly facultyService = inject(FacultyService);
  private readonly studentService = inject(StudentService);
  private readonly themeService   = inject(ThemeService);
  private readonly http           = inject(HttpClient);
  private readonly toast          = inject(ToastService);
  private readonly destroyRef     = inject(DestroyRef);

  // ── Theme ─────────────────────────────────────────────────────────────────
  protected readonly swatches      = COLOR_SWATCHES;
  protected readonly activeSwatch  = this.themeService.activeSwatch;
  protected readonly primaryColor  = computed(() => this.activeSwatch().hex);
  protected readonly themeOpen     = signal(false);

  // ── Data ──────────────────────────────────────────────────────────────────
  protected readonly loading  = signal(true);
  protected readonly identity = signal<ProfileIdentity | null>(null);
  protected readonly faculty  = signal<Faculty | null>(null);
  protected readonly student  = signal<Student | null>(null);
  protected readonly summary  = signal<DashboardSummary | null>(null);

  // ── Document slots ────────────────────────────────────────────────────────
  private readonly rawSlots = signal<{ status: string }[]>([]);

  protected readonly docStats = computed<DocStats>(() => {
    const s = this.rawSlots();
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

  // SVG ring offsets — r=60→376.99 | r=18→113.1 | r=54→339.3
  protected readonly bigRingOffset    = computed(() => 376.99 - (376.99 * this.progressPct()) / 100);
  protected readonly miniRingOffset   = computed(() => 113.1  - (113.1  * this.progressPct()) / 100);
  protected readonly avatarRingOffset = computed(() => 339.3  - (339.3  * this.progressPct()) / 100);

  // ── Count-up animation signals ────────────────────────────────────────────
  protected readonly animTotal    = signal(0);
  protected readonly animVerified = signal(0);
  protected readonly animPending  = signal(0);
  protected readonly animMissing  = signal(0);

  // ── Static content ────────────────────────────────────────────────────────
  protected readonly activityItems: ActivityItem[] = [
    { id: 1, icon: 'verified',     text: 'Appointment Letter verified',   time: '2 days ago',  color: '#10b981' },
    { id: 2, icon: 'upload_file',  text: 'PG Degree document uploaded',   time: '5 days ago',  color: '#f59e0b' },
    { id: 3, icon: 'check_circle', text: 'Aadhaar Card verified',         time: '1 week ago',  color: '#10b981' },
    { id: 4, icon: 'cancel',       text: 'Transfer Cert. rejected',       time: '2 weeks ago', color: '#ef4444' },
    { id: 5, icon: 'upload_file',  text: 'UG Degree uploaded',            time: '3 weeks ago', color: '#f59e0b' },
  ];

  protected readonly connections: ConnectionItem[] = [
    { id: 1, name: 'Dr. Ananya Kumar', initials: 'AK', role: 'Professor',          online: true  },
    { id: 2, name: 'Prof. Meena Devi', initials: 'MD', role: 'Associate Professor', online: true  },
    { id: 3, name: 'Dr. Ramesh Iyer',  initials: 'RI', role: 'Head of Department',  online: false },
    { id: 4, name: 'Ms. Preethi S.',   initials: 'PS', role: 'Lecturer',            online: true  },
  ];

  protected readonly dockItems: DockItem[] = [
    { icon: 'dashboard',     label: 'Dashboard', route: '/dashboard'  },
    { icon: 'person',        label: 'Profile',   route: '/profile'    },
    { icon: 'folder_open',   label: 'Documents', route: '/profile'    },
    { icon: 'notifications', label: 'Alerts',    route: '/dashboard'  },
    { icon: 'settings',      label: 'Settings',  route: '/settings'   },
  ];

  protected readonly adminLinks: AdminLink[] = [
    { icon: 'school',                    label: 'Programs',    route: '/programs',                     desc: 'Academic programs',   cc: 'indigo'  },
    { icon: 'business',                  label: 'Departments', route: '/departments',                  desc: 'Manage departments',  cc: 'violet'  },
    { icon: 'groups',                    label: 'Faculty',     route: '/faculty',                      desc: 'Faculty & docs',      cc: 'blue'    },
    { icon: 'person',                    label: 'Students',    route: '/students',                     desc: 'Student records',     cc: 'sky'     },
    { icon: 'contact_mail',              label: 'Enquiries',   route: '/enquiries',                    desc: 'Admissions funnel',   cc: 'teal'    },
    { icon: 'assignment_ind',            label: 'Admissions',  route: '/admissions',                   desc: 'Student admissions',  cc: 'emerald' },
    { icon: 'upload_file',               label: 'Documents',   route: '/enquiries/document-submission',desc: 'Submit & verify',     cc: 'cyan'    },
    { icon: 'account_balance_wallet',    label: 'Fees',        route: '/student-fees',                 desc: 'Fee management',      cc: 'rose'    },
    { icon: 'manage_accounts',           label: 'Users',       route: '/user-management',              desc: 'Roles & access',      cc: 'pink'    },
    { icon: 'assessment',                label: 'Reports',     route: '/reports',                      desc: 'Academic & financial',cc: 'amber'   },
  ];

  // ── Computed helpers ──────────────────────────────────────────────────────
  protected get initials(): string { return computeInitials(this.identity()?.displayName ?? ''); }

  protected readonly editRoute = computed(() => {
    const id = this.identity();
    if (!id) return null;
    if (id.entityType === 'FACULTY' && id.entityId) return `/faculty/${id.entityId}/edit`;
    if (id.entityType === 'STUDENT' && id.entityId) return `/students/${id.entityId}/edit`;
    return null;
  });

  protected qualificationLabel(q: FacultyQualification): string {
    return FACULTY_QUALIFICATION_OPTIONS.find(o => o.value === q)?.label ?? q;
  }

  protected facultyInfoItems(f: Faculty): InfoItem[] {
    return [
      { icon: 'person',         label: 'Full Name',     value: f.fullName },
      { icon: 'email',          label: 'Email',         value: f.email },
      { icon: 'phone',          label: 'Phone',         value: f.phone ?? '' },
      { icon: 'business',       label: 'Department',    value: f.departmentName },
      { icon: 'work',           label: 'Designation',   value: f.designation.replace(/_/g,' ').toLowerCase().replace(/\b\w/g,c=>c.toUpperCase()) },
      { icon: 'school',         label: 'Qualification', value: f.highestQualification ? this.qualificationLabel(f.highestQualification) : '' },
      { icon: 'badge',          label: 'Employee Code', value: f.employeeCode },
      { icon: 'calendar_today', label: 'Joining Date',  value: f.joiningDate },
      { icon: 'info',           label: 'Status',        value: f.status.replace(/_/g,' ').toLowerCase().replace(/\b\w/g,c=>c.toUpperCase()) },
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
    ].filter(i => !!i.value);
  }

  // ── Lifecycle ─────────────────────────────────────────────────────────────
  ngOnInit(): void {
    this.loading.set(true);
    this.identity.set(null); this.faculty.set(null); this.student.set(null);

    this.profileService.getMyProfile().subscribe({
      next: id => {
        this.identity.set(id);
        if (id.entityType === 'FACULTY' && id.entityId) {
          this.facultyService.getById(id.entityId).subscribe({
            next:  f => { this.faculty.set(f); this.loading.set(false); },
            error: () => { this.toast.error('Failed to load faculty profile'); this.loading.set(false); },
          });
        } else if (id.entityType === 'STUDENT' && id.entityId) {
          this.studentService.getById(id.entityId).subscribe({
            next:  s => { this.student.set(s); this.loading.set(false); },
            error: () => { this.toast.error('Failed to load student profile'); this.loading.set(false); },
          });
        } else {
          this.http.get<DashboardSummary>(`${environment.apiUrl}/dashboard/summary`)
            .pipe(catchError(() => of(null)))
            .subscribe(sm => { this.summary.set(sm); this.loading.set(false); });
        }
      },
      error: () => { this.toast.error('Failed to load profile'); this.loading.set(false); },
    });
  }

  // ── Theme ─────────────────────────────────────────────────────────────────
  protected applyColor(swatch: ColorSwatch): void { this.themeService.applyTheme(swatch); }

  protected selectTheme(swatch: ColorSwatch): void {
    this.themeService.applyTheme(swatch);
    this.themeOpen.set(false);
  }

  @HostListener('document:click')
  protected closeThemeDropdown(): void { this.themeOpen.set(false); }

  // ── ProfileDocumentsComponent output ──────────────────────────────────────
  protected onSlotsChange(slots: { status: string }[]): void {
    this.rawSlots.set(slots);
    this.runCountUp();
  }

  // ── Count-up: 60 fps, ease-out-cubic over 1 s ─────────────────────────────
  private runCountUp(): void {
    const t = { ...this.docStats() };
    let step = 0;
    interval(16).pipe(take(61), takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      step++;
      const ease = 1 - Math.pow(1 - step / 60, 3);
      this.animTotal.set(Math.round(t.total * ease));
      this.animVerified.set(Math.round(t.verified * ease));
      this.animPending.set(Math.round(t.pending * ease));
      this.animMissing.set(Math.round(t.missing * ease));
    });
  }

  // ── Mouse spotlight ───────────────────────────────────────────────────────
  protected onCardMouseMove(e: MouseEvent): void {
    const el = e.currentTarget as HTMLElement;
    const r  = el.getBoundingClientRect();
    el.style.setProperty('--mx', `${((e.clientX - r.left) / r.width)  * 100}%`);
    el.style.setProperty('--my', `${((e.clientY - r.top)  / r.height) * 100}%`);
  }
  protected onCardMouseLeave(e: MouseEvent): void {
    const el = e.currentTarget as HTMLElement;
    el.style.removeProperty('--mx');
    el.style.removeProperty('--my');
  }
}

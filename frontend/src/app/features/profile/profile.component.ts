import {
  Component, computed, ElementRef, HostListener, inject, OnDestroy, OnInit, signal, ViewChild,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { TitleCasePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';

import { ProfileService, ProfileIdentity, SelfUpdateRequest } from './profile.service';
import { FacultyService } from '../faculty/faculty.service';
import { StudentService } from '../student/student.service';
import { Faculty, FacultyQualification, FACULTY_QUALIFICATION_OPTIONS } from '../faculty/faculty.model';
import { Student } from '../student/student.model';
import { ThemeService, COLOR_SWATCHES, ColorSwatch } from '../../core/theme/theme.service';
import { ProfileDocumentsComponent } from '../../shared/profile-documents/profile-documents.component';
import { computeInitials } from '../../shared/utils/initials';
import { AppDatePipe } from '../../shared/pipes/app-date.pipe';
import { ToastService } from '../../core/toast/toast.service';
import { DocumentSlotsService } from '../dashboard/services/document-slots.service';
import {
  PhotoCropDialogComponent,
  PhotoCropDialogData,
  PhotoCropDialogResult,
} from '../../shared/photo-crop-dialog/photo-crop-dialog.component';

export interface InfoItem { icon: string; label: string; value: string; }

interface QuickLink { icon: string; label: string; route: string; }

/**
 * Profile screen — focused identity + personal-document-vault page.
 *
 * Operational widgets (document stat counters, completion ring, recent activity,
 * colleagues, admin quick links, system health) live on the role-specific
 * Dashboard components. Profile keeps only:
 *   - Hero (avatar, name, role, contact info)
 *   - Theme picker
 *   - Bio / About card
 *   - Personal Info card
 *   - Document vault (`<cms-profile-documents>`)
 *
 * Document slot updates are forwarded to {@link DocumentSlotsService} so the
 * dashboard widgets stay in sync as the user uploads / verifies files here.
 */
@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    RouterLink, TitleCasePipe, FormsModule,
    MatButtonModule, MatIconModule, MatTooltipModule, MatDialogModule,
    ProfileDocumentsComponent, AppDatePipe,
  ],
  templateUrl: './profile.component.html',
  styleUrl:    './profile.component.scss',
})
export class ProfileComponent implements OnInit, OnDestroy {

  @ViewChild('photoInput') private photoInput?: ElementRef<HTMLInputElement>;

  private readonly profileService = inject(ProfileService);
  private readonly facultyService = inject(FacultyService);
  private readonly studentService = inject(StudentService);
  private readonly themeService   = inject(ThemeService);
  private readonly toast          = inject(ToastService);
  private readonly docSlots       = inject(DocumentSlotsService);
  private readonly dialog         = inject(MatDialog);

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

  // ── Profile photo ─────────────────────────────────────────────────────────
  /** Display URL for this page — reads from the shared service signal. */
  protected readonly photoUrl = this.profileService.avatarDataUrl;
  protected readonly uploadingPhoto = signal(false);
  private photoObjectUrl: string | null = null;

  // ── Inline self-edit ──────────────────────────────────────────────────────
  protected readonly editMode = signal(false);
  protected readonly savingSelfInfo = signal(false);
  protected editForm: SelfUpdateRequest = {};

  // ── Computed helpers ──────────────────────────────────────────────────────
  protected get initials(): string { return computeInitials(this.identity()?.displayName ?? ''); }

  protected readonly editRoute = computed(() => {
    const id = this.identity();
    if (!id) return null;
    if (id.entityType === 'FACULTY' && id.entityId) return `/faculty/${id.entityId}/edit`;
    if (id.entityType === 'STUDENT' && id.entityId) return `/students/${id.entityId}/edit`;
    return null;
  });

  protected readonly quickLinks = computed<QuickLink[]>(() => {
    const id = this.identity();
    if (!id) return [];
    if (id.entityType === 'FACULTY') {
      return [
        { icon: 'calendar_month', label: 'Academic Calendar', route: '/academic-calendar' },
        { icon: 'checklist', label: 'Attendance', route: '/attendance' },
        { icon: 'science', label: 'Lab Schedule', route: '/lab-schedules' },
        { icon: 'folder_open', label: 'My Documents', route: '/profile' },
      ];
    }
    if (id.entityType === 'STUDENT') {
      return [
        { icon: 'calendar_month', label: 'Academic Calendar', route: '/academic-calendar' },
        { icon: 'payments', label: 'My Fees', route: '/student-fees' },
        { icon: 'checklist', label: 'Attendance', route: '/attendance' },
        { icon: 'emoji_events', label: 'Exam Results', route: '/exam-results' },
      ];
    }
    return [
      { icon: 'school', label: 'Programs', route: '/programs' },
      { icon: 'groups', label: 'Faculty', route: '/faculty' },
      { icon: 'person', label: 'Students', route: '/students' },
      { icon: 'bar_chart', label: 'Reports', route: '/reports' },
      { icon: 'manage_accounts', label: 'Users', route: '/user-management' },
    ];
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

  protected contactInfoItems(): InfoItem[] {
    const f = this.faculty();
    const s = this.student();
    if (f) {
      return [
        { icon: 'phone', label: 'Phone', value: f.phone ?? '—' },
        { icon: 'bloodtype', label: 'Blood Group', value: f.bloodGroup ?? '—' },
        { icon: 'cake', label: 'Date of Birth', value: f.dateOfBirth ?? '—' },
        { icon: 'wc', label: 'Gender', value: f.gender ?? '—' },
        { icon: 'location_on', label: 'Address', value: this.formatFacultyAddress(f) },
      ];
    }
    if (s) {
      return [
        { icon: 'phone', label: 'Phone', value: s.phone ?? '—' },
        { icon: 'bloodtype', label: 'Blood Group', value: s.bloodGroup ?? '—' },
        { icon: 'cake', label: 'Date of Birth', value: s.dateOfBirth ?? '—' },
        { icon: 'wc', label: 'Gender', value: s.gender ?? '—' },
        { icon: 'family_restroom', label: 'Parent Mobile', value: s.parentMobile ?? s.fatherPhone ?? '—' },
        { icon: 'location_on', label: 'Address', value: this.formatStudentAddress(s) },
      ];
    }
    return [];
  }

  protected formatFacultyAddress(f: Faculty): string {
    const a = f.address;
    if (!a) return '—';
    return [a.street, a.city, a.district, a.state, a.pincode].filter(Boolean).join(', ') || '—';
  }

  protected formatStudentAddress(s: Student): string {
    return [s.street, s.city, s.district, s.state, s.pincode].filter(Boolean).join(', ') || '—';
  }

  // ── Lifecycle ─────────────────────────────────────────────────────────────
  ngOnInit(): void {
    this.loading.set(true);
    this.identity.set(null); this.faculty.set(null); this.student.set(null);

    this.profileService.getMyProfile().subscribe({
      next: id => {
        this.identity.set(id);
        this.loadPhoto();
        if (id.entityType === 'FACULTY' && id.entityId) {
          this.facultyService.getById(id.entityId).subscribe({
            next:  f => { this.faculty.set(f); this.initEditForm(); this.loading.set(false); },
            error: () => { this.toast.error('Failed to load faculty profile'); this.loading.set(false); },
          });
        } else if (id.entityType === 'STUDENT' && id.entityId) {
          this.studentService.getById(id.entityId).subscribe({
            next:  s => { this.student.set(s); this.initEditForm(); this.loading.set(false); },
            error: () => { this.toast.error('Failed to load student profile'); this.loading.set(false); },
          });
        } else {
          // Admin — no extra fetch needed; identity is enough.
          this.loading.set(false);
        }
      },
      error: () => { this.toast.error('Failed to load profile'); this.loading.set(false); },
    });
  }

  ngOnDestroy(): void {
    this.revokePhotoObjectUrl();
  }

  // ── Profile photo ─────────────────────────────────────────────────────────
  protected triggerPhotoUpload(): void {
    this.photoInput?.nativeElement.click();
  }

  protected onPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    input.value = '';

    if (!['image/jpeg', 'image/png'].includes(file.type)) {
      this.toast.error('Only JPEG or PNG images are allowed');
      return;
    }
    if (file.size > 10 * 1024 * 1024) {
      this.toast.error('Photo must be under 10 MB');
      return;
    }

    // Open the crop dialog
    const dialogRef = this.dialog.open<PhotoCropDialogComponent, PhotoCropDialogData, PhotoCropDialogResult>(
      PhotoCropDialogComponent,
      {
        data: { file },
        panelClass: 'pcd-dialog-panel',
        maxWidth: '96vw',
        disableClose: false,
      },
    );

    dialogRef.afterClosed().subscribe((result) => {
      if (!result?.blob) return;
      // Upload the cropped blob
      const croppedFile = new File([result.blob], file.name.replace(/\.(jpg|jpeg|png)$/i, '.jpg'), {
        type: 'image/jpeg',
      });
      this.uploadCroppedPhoto(croppedFile);
    });
  }

  private uploadCroppedPhoto(file: File): void {
    this.uploadingPhoto.set(true);
    this.profileService.uploadPhoto(file).subscribe({
      next: () => {
        this.toast.success('Profile photo updated');
        this.uploadingPhoto.set(false);
        this.loadPhoto();
      },
      error: err => {
        this.toast.error(err?.error?.message ?? 'Failed to upload profile photo');
        this.uploadingPhoto.set(false);
      },
    });
  }

  protected removePhoto(event: MouseEvent): void {
    event.stopPropagation();
    this.profileService.deletePhoto().subscribe({
      next: () => {
        this.toast.success('Profile photo removed');
        this.profileService.setAvatarDataUrl(null);
      },
      error: err => this.toast.error(err?.error?.message ?? 'Failed to remove profile photo'),
    });
  }

  private loadPhoto(): void {
    this.profileService.loadAvatar();
  }

  private revokePhotoObjectUrl(): void {
    if (this.photoObjectUrl) {
      URL.revokeObjectURL(this.photoObjectUrl);
      this.photoObjectUrl = null;
    }
  }

  // ── Self-edit ─────────────────────────────────────────────────────────────
  protected enterEditMode(): void {
    this.initEditForm();
    this.editMode.set(true);
  }

  protected cancelEdit(): void {
    this.initEditForm();
    this.editMode.set(false);
  }

  protected saveSelfInfo(): void {
    this.savingSelfInfo.set(true);
    this.profileService.updateSelfInfo(this.editForm).subscribe({
      next: () => {
        this.toast.success('Profile updated');
        this.savingSelfInfo.set(false);
        this.editMode.set(false);
        this.reloadLinkedProfile();
      },
      error: err => {
        this.toast.error(err?.error?.message ?? 'Failed to update profile');
        this.savingSelfInfo.set(false);
      },
    });
  }

  private initEditForm(): void {
    const f = this.faculty();
    const s = this.student();
    if (f) {
      this.editForm = {
        phone: f.phone ?? '',
        bloodGroup: f.bloodGroup ?? '',
        postalAddress: f.address?.postalAddress ?? '',
        street: f.address?.street ?? '',
        city: f.address?.city ?? '',
        district: f.address?.district ?? '',
        state: f.address?.state ?? '',
        pincode: f.address?.pincode ?? '',
      };
    } else if (s) {
      this.editForm = {
        phone: s.phone ?? '',
        bloodGroup: s.bloodGroup ?? '',
        postalAddress: s.postalAddress ?? '',
        street: s.street ?? '',
        city: s.city ?? '',
        district: s.district ?? '',
        state: s.state ?? '',
        pincode: s.pincode ?? '',
      };
    }
  }

  private reloadLinkedProfile(): void {
    const id = this.identity();
    if (id?.entityType === 'FACULTY' && id.entityId) {
      this.facultyService.getById(id.entityId).subscribe({ next: f => this.faculty.set(f) });
    }
    if (id?.entityType === 'STUDENT' && id.entityId) {
      this.studentService.getById(id.entityId).subscribe({ next: s => this.student.set(s) });
    }
  }

  // ── Theme ─────────────────────────────────────────────────────────────────
  protected applyColor(swatch: ColorSwatch): void { this.themeService.applyTheme(swatch); }

  protected toggleThemeDropdown(): void {
    this.themeOpen.update(open => !open);
  }

  protected selectTheme(swatch: ColorSwatch): void {
    this.themeService.applyTheme(swatch);
    this.themeOpen.set(false);
  }

  @HostListener('document:click')
  protected closeThemeDropdown(): void { this.themeOpen.set(false); }

  // ── Forward slot updates to the shared dashboard service ──────────────────
  protected onSlotsChange(slots: { status: string }[]): void {
    this.docSlots.setSlots(slots);
  }

  // ── Mouse spotlight (decorative card hover) ───────────────────────────────
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


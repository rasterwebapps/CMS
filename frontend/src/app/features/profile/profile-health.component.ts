import {
  Component, computed, ElementRef, HostListener, inject, OnDestroy, OnInit,
  signal, ViewChild,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { TitleCasePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule }    from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';

import { ProfileService, ProfileIdentity, SelfUpdateRequest } from './profile.service';
import { FacultyService } from '../faculty/faculty.service';
import { StudentService } from '../student/student.service';
import { Faculty, FacultyQualification, FACULTY_QUALIFICATION_OPTIONS } from '../faculty/faculty.model';
import { Student } from '../student/student.model';
import { ThemeService, COLOR_SWATCHES, ColorSwatch } from '../../core/theme/theme.service';
import { ProfileDocumentsComponent } from '../../shared/profile-documents/profile-documents.component';
import { computeInitials } from '../../shared/utils/initials';
import { ToastService } from '../../core/toast/toast.service';
import { DocumentSlotsService } from '../dashboard/services/document-slots.service';
import {
  PhotoCropDialogComponent,
  PhotoCropDialogData,
  PhotoCropDialogResult,
} from '../../shared/photo-crop-dialog/photo-crop-dialog.component';

export interface InfoItem { icon: string; label: string; value: string; }
interface QuickLink      { icon: string; label: string; route: string; color: string; }

@Component({
  selector:    'app-profile-health',
  standalone:  true,
  imports: [
    RouterLink, TitleCasePipe, FormsModule,
    MatIconModule, MatTooltipModule, MatDialogModule,
    ProfileDocumentsComponent,
  ],
  templateUrl: './profile-health.component.html',
  styleUrl:    './profile-health.component.scss',
})
export class ProfileHealthComponent implements OnInit, OnDestroy {

  @ViewChild('photoInput') private photoInput?: ElementRef<HTMLInputElement>;
  @ViewChild('coverInput') private coverInput?: ElementRef<HTMLInputElement>;

  private readonly profileService = inject(ProfileService);
  private readonly facultyService = inject(FacultyService);
  private readonly studentService = inject(StudentService);
  private readonly themeService   = inject(ThemeService);
  private readonly toast          = inject(ToastService);
  private readonly docSlots       = inject(DocumentSlotsService);
  private readonly dialog         = inject(MatDialog);

  // ── Theme ─────────────────────────────────────────────────────────────────
  protected readonly swatches     = COLOR_SWATCHES;
  protected readonly activeSwatch = this.themeService.activeSwatch;
  protected readonly primaryColor = computed(() => this.activeSwatch().hex);
  protected readonly themeOpen    = signal(false);

  // ── Data ──────────────────────────────────────────────────────────────────
  protected readonly loading  = signal(true);
  protected readonly identity = signal<ProfileIdentity | null>(null);
  protected readonly faculty  = signal<Faculty | null>(null);
  protected readonly student  = signal<Student | null>(null);

  // ── Photos ────────────────────────────────────────────────────────────────
  protected readonly photoUrl       = this.profileService.avatarDataUrl;
  protected readonly coverUrl       = this.profileService.coverDataUrl;
  protected readonly uploadingPhoto = signal(false);
  protected readonly uploadingCover = signal(false);

  // ── Edit mode ─────────────────────────────────────────────────────────────
  protected readonly editMode      = signal(false);
  protected readonly savingSelf    = signal(false);
  protected editForm: SelfUpdateRequest = {};

  // ── Computed ──────────────────────────────────────────────────────────────
  protected get initials(): string { return computeInitials(this.identity()?.displayName ?? ''); }

  protected readonly editRoute = computed(() => {
    const id = this.identity();
    if (!id) return null;
    if (id.entityType === 'FACULTY' && id.entityId) return `/faculty/${id.entityId}/edit`;
    if (id.entityType === 'STUDENT' && id.entityId) return `/students/${id.entityId}/edit`;
    return null;
  });

  protected readonly profileCompletion = computed(() => {
    const f = this.faculty();
    const s = this.student();
    const checks = [
      { label: 'Profile photo',  ok: !!this.photoUrl() },
      { label: 'Phone number',   ok: !!(f?.phone ?? s?.phone) },
      { label: 'Blood group',    ok: !!(f?.bloodGroup ?? s?.bloodGroup) },
      { label: 'Date of birth',  ok: !!(f?.dateOfBirth ?? s?.dateOfBirth) },
      { label: 'Address',        ok: !!(f?.address?.city ?? s?.city) },
      { label: 'Bio',            ok: !!(f?.bio ?? s?.bio) },
    ];
    const score   = checks.filter(c => c.ok).length;
    const missing = checks.filter(c => !c.ok).map(c => c.label);
    return { score, total: checks.length, missing, pct: Math.round((score / checks.length) * 100) };
  });

  protected readonly quickLinks = computed<QuickLink[]>(() => {
    const id = this.identity();
    if (!id) return [];
    if (id.entityType === 'FACULTY') return [
      { icon: 'calendar_month',  label: 'Academic Calendar', route: '/academic-calendar', color: '#FF6B6B' },
      { icon: 'checklist',       label: 'Attendance',        route: '/attendance',        color: '#4ECDC4' },
      { icon: 'science',         label: 'Lab Schedule',      route: '/lab-schedules',     color: '#A78BFA' },
      { icon: 'folder_open',     label: 'My Documents',      route: '/profile',           color: '#F59E0B' },
    ];
    if (id.entityType === 'STUDENT') return [
      { icon: 'calendar_month',  label: 'Academic Calendar', route: '/academic-calendar', color: '#FF6B6B' },
      { icon: 'payments',        label: 'My Fees',           route: '/student-fees',      color: '#22C55E' },
      { icon: 'checklist',       label: 'Attendance',        route: '/attendance',        color: '#4ECDC4' },
      { icon: 'emoji_events',    label: 'Exam Results',      route: '/exam-results',      color: '#F59E0B' },
    ];
    return [
      { icon: 'school',          label: 'Programs',          route: '/programs',          color: '#FF6B6B' },
      { icon: 'groups',          label: 'Faculty',           route: '/faculty',           color: '#4ECDC4' },
      { icon: 'person',          label: 'Student Explorer',  route: '/students',          color: '#22C55E' },
      { icon: 'bar_chart',       label: 'Reports',           route: '/reports',           color: '#A78BFA' },
      { icon: 'manage_accounts', label: 'User Management',   route: '/user-management',   color: '#F59E0B' },
    ];
  });

  // ── Lifecycle ─────────────────────────────────────────────────────────────
  ngOnInit(): void {
    this.loading.set(true);
    this.identity.set(null); this.faculty.set(null); this.student.set(null);

    this.profileService.getMyProfile().subscribe({
      next: id => {
        this.identity.set(id);
        this.profileService.loadAvatar();
        this.profileService.loadCover();
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
          this.loading.set(false);
        }
      },
      error: () => { this.toast.error('Failed to load profile'); this.loading.set(false); },
    });
  }

  ngOnDestroy(): void {}

  // ── Photo ─────────────────────────────────────────────────────────────────
  protected triggerPhotoUpload(): void { this.photoInput?.nativeElement.click(); }

  protected onPhotoSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    (event.target as HTMLInputElement).value = '';
    if (!['image/jpeg', 'image/png'].includes(file.type)) { this.toast.error('Only JPEG or PNG allowed'); return; }
    if (file.size > 10 * 1024 * 1024) { this.toast.error('Photo must be under 10 MB'); return; }

    const ref = this.dialog.open<PhotoCropDialogComponent, PhotoCropDialogData, PhotoCropDialogResult>(
      PhotoCropDialogComponent,
      { data: { file }, panelClass: 'pcd-dialog-panel', maxWidth: '96vw', disableClose: false },
    );
    ref.afterClosed().subscribe(result => {
      if (!result?.blob) return;
      const cropped = new File([result.blob], file.name.replace(/\.(jpg|jpeg|png|webp)$/i, '.png'), { type: 'image/png' });
      this.uploadingPhoto.set(true);
      this.profileService.uploadPhoto(cropped).subscribe({
        next: () => { this.profileService.loadAvatar(); this.uploadingPhoto.set(false); this.toast.success('Photo updated'); },
        error: err => { this.toast.error(err?.error?.message ?? 'Failed to upload photo'); this.uploadingPhoto.set(false); },
      });
    });
  }

  protected removePhoto(e: MouseEvent): void {
    e.stopPropagation();
    this.profileService.deletePhoto().subscribe({
      next:  () => { this.toast.success('Photo removed'); this.profileService.setAvatarDataUrl(null); },
      error: err => this.toast.error(err?.error?.message ?? 'Failed to remove photo'),
    });
  }

  // ── Cover ─────────────────────────────────────────────────────────────────
  protected triggerCoverUpload(): void { this.coverInput?.nativeElement.click(); }

  protected onCoverSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    (event.target as HTMLInputElement).value = '';
    if (!['image/jpeg', 'image/png'].includes(file.type)) { this.toast.error('Only JPEG or PNG allowed'); return; }
    if (file.size > 5 * 1024 * 1024) { this.toast.error('Cover must be under 5 MB'); return; }
    this.uploadingCover.set(true);
    this.profileService.uploadCover(file).subscribe({
      next: () => { this.profileService.loadCover(); this.uploadingCover.set(false); this.toast.success('Cover updated'); },
      error: () => { this.uploadingCover.set(false); this.toast.error('Failed to upload cover'); },
    });
  }

  protected removeCover(): void {
    this.profileService.deleteCover().subscribe({
      next:  () => { this.profileService.setCoverDataUrl(null); this.toast.success('Cover removed'); },
      error: () => this.toast.error('Failed to remove cover'),
    });
  }

  // ── Edit ──────────────────────────────────────────────────────────────────
  protected enterEditMode(): void { this.initEditForm(); this.editMode.set(true); }
  protected cancelEdit():    void { this.initEditForm(); this.editMode.set(false); }

  protected saveSelfInfo(): void {
    this.savingSelf.set(true);
    this.profileService.updateSelfInfo(this.editForm).subscribe({
      next: () => {
        this.toast.success('Profile updated');
        this.savingSelf.set(false);
        this.editMode.set(false);
        this.reloadLinked();
      },
      error: err => { this.toast.error(err?.error?.message ?? 'Failed to update'); this.savingSelf.set(false); },
    });
  }

  private initEditForm(): void {
    const f = this.faculty();
    const s = this.student();
    if (f) {
      this.editForm = { phone: f.phone ?? '', bloodGroup: f.bloodGroup ?? '', bio: f.bio ?? '',
        postalAddress: f.address?.postalAddress ?? '', street: f.address?.street ?? '',
        city: f.address?.city ?? '', district: f.address?.district ?? '',
        state: f.address?.state ?? '', pincode: f.address?.pincode ?? '' };
    } else if (s) {
      this.editForm = { phone: s.phone ?? '', bloodGroup: s.bloodGroup ?? '', bio: s.bio ?? '',
        postalAddress: s.postalAddress ?? '', street: s.street ?? '',
        city: s.city ?? '', district: s.district ?? '',
        state: s.state ?? '', pincode: s.pincode ?? '' };
    }
  }

  private reloadLinked(): void {
    const id = this.identity();
    if (id?.entityType === 'FACULTY' && id.entityId)
      this.facultyService.getById(id.entityId).subscribe({ next: f => this.faculty.set(f) });
    if (id?.entityType === 'STUDENT' && id.entityId)
      this.studentService.getById(id.entityId).subscribe({ next: s => this.student.set(s) });
  }

  // ── Theme ─────────────────────────────────────────────────────────────────
  protected toggleThemeDropdown(): void { this.themeOpen.update(o => !o); }
  protected selectTheme(sw: ColorSwatch): void { this.themeService.applyTheme(sw); this.themeOpen.set(false); }
  @HostListener('document:click') protected closeTheme(): void { this.themeOpen.set(false); }

  // ── Document slots ────────────────────────────────────────────────────────
  protected onSlotsChange(slots: { status: string }[]): void { this.docSlots.setSlots(slots); }

  // ── Info helpers ──────────────────────────────────────────────────────────
  protected qualificationLabel(q: FacultyQualification): string {
    return FACULTY_QUALIFICATION_OPTIONS.find(o => o.value === q)?.label ?? q;
  }

  protected facultyInfoRows(f: Faculty): InfoItem[] {
    return [
      { icon: 'badge',          label: 'Employee Code', value: f.employeeCode },
      { icon: 'business',       label: 'Speciality',    value: f.specialityName },
      { icon: 'work',           label: 'Designation',   value: f.designation.replace(/_/g,' ').toLowerCase().replace(/\b\w/g,c=>c.toUpperCase()) },
      { icon: 'school',         label: 'Qualification', value: f.highestQualification ? this.qualificationLabel(f.highestQualification) : '' },
      { icon: 'email',          label: 'Email',         value: f.email },
      { icon: 'phone',          label: 'Phone',         value: f.phone ?? '' },
      { icon: 'bloodtype',      label: 'Blood Group',   value: f.bloodGroup ?? '' },
      { icon: 'cake',           label: 'Date of Birth', value: f.dateOfBirth ?? '' },
      { icon: 'wc',             label: 'Gender',        value: f.gender ?? '' },
      { icon: 'calendar_today', label: 'Joined',        value: f.joiningDate },
      { icon: 'location_on',    label: 'Address',       value: this.facultyAddress(f) },
    ].filter(i => !!i.value);
  }

  protected studentInfoRows(s: Student): InfoItem[] {
    return [
      { icon: 'tag',            label: 'Roll Number',   value: s.rollNumber ?? '' },
      { icon: 'school',         label: 'Program',       value: s.programName },
      { icon: 'layers',         label: 'Year of Study', value: `Year ${s.yearOfStudy}` },
      { icon: 'email',          label: 'Email',         value: s.email },
      { icon: 'phone',          label: 'Phone',         value: s.phone ?? '' },
      { icon: 'bloodtype',      label: 'Blood Group',   value: s.bloodGroup ?? '' },
      { icon: 'cake',           label: 'Date of Birth', value: s.dateOfBirth ?? '' },
      { icon: 'wc',             label: 'Gender',        value: s.gender ?? '' },
      { icon: 'family_restroom',label: 'Parent Contact',value: s.parentMobile ?? s.fatherPhone ?? '' },
      { icon: 'calendar_today', label: 'Admitted',      value: s.admissionDate },
      { icon: 'location_on',    label: 'Address',       value: this.studentAddress(s) },
    ].filter(i => !!i.value);
  }

  private facultyAddress(f: Faculty): string {
    const a = f.address;
    return a ? [a.street, a.city, a.district, a.state, a.pincode].filter(Boolean).join(', ') : '';
  }

  private studentAddress(s: Student): string {
    return [s.street, s.city, s.district, s.state, s.pincode].filter(Boolean).join(', ');
  }
}

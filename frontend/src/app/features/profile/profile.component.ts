import {
  Component, computed, ElementRef, HostListener, inject, OnDestroy, OnInit, signal, ViewChild, ViewChildren, QueryList,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { TitleCasePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';

import { environment } from '../../../environments';
import { PermissionService } from '../../core/permissions/permission.service';
import { BloodGroupService } from '../blood-group/blood-group.service';
import { BloodGroup } from '../blood-group/blood-group.model';
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

// ── Notification preferences ──────────────────────────────────────────────────
interface NotifPrefs {
  channel:    'in-app' | 'email' | 'both';
  categories: Record<string, boolean>;
}
interface NotifCategory { key: string; label: string; icon: string; desc: string; }
const NOTIF_STORAGE_KEY = 'cms_notif_prefs';
const NOTIF_CATEGORIES: NotifCategory[] = [
  { key: 'feeAlerts',          label: 'Fee Alerts',            icon: 'payments',     desc: 'Due dates and payment confirmations' },
  { key: 'documentReminders',  label: 'Document Reminders',    icon: 'folder',       desc: 'Upload prompts and verification status' },
  { key: 'admissionUpdates',   label: 'Admission Updates',     icon: 'how_to_reg',   desc: 'Enquiry and admission status changes' },
  { key: 'systemAnnouncements',label: 'System Announcements',  icon: 'campaign',     desc: 'App updates and maintenance notices' },
  { key: 'examSchedule',       label: 'Exam & Schedule',       icon: 'event_note',   desc: 'Exam dates and timetable changes' },
];
const DEFAULT_NOTIF_PREFS: NotifPrefs = {
  channel: 'in-app',
  categories: { feeAlerts: true, documentReminders: true, admissionUpdates: true, systemAnnouncements: true, examSchedule: false },
};

// ── Permission module label map ───────────────────────────────────────────────
const MODULE_LABELS: Record<string, { label: string; icon: string }> = {
  USER:       { label: 'User Management',    icon: 'manage_accounts' },
  ROLE:       { label: 'Roles & Access',     icon: 'shield' },
  STUDENT:    { label: 'Student Explorer',    icon: 'school' },
  FACULTY:    { label: 'Faculty',            icon: 'groups' },
  FEE:        { label: 'Finance & Fees',     icon: 'payments' },
  ADMISSION:  { label: 'Admission Explorer',  icon: 'how_to_reg' },
  ENQUIRY:    { label: 'Enquiries',          icon: 'contact_mail' },
  LAB:        { label: 'Labs & Equipment',   icon: 'science' },
  EXAM:       { label: 'Examinations',       icon: 'quiz' },
  DEPARTMENT: { label: 'Departments',        icon: 'business' },
  PROGRAM:    { label: 'Programs',           icon: 'menu_book' },
  ATTENDANCE: { label: 'Attendance',         icon: 'checklist' },
  DASHBOARD:  { label: 'Dashboard',         icon: 'dashboard' },
  SYSTEM:     { label: 'System',             icon: 'settings' },
  DOCUMENT:   { label: 'Documents',          icon: 'folder_open' },
  COURSE:     { label: 'Courses',            icon: 'auto_stories' },
  INVENTORY:  { label: 'Inventory',          icon: 'inventory_2' },
  SCHOLARSHIP:{ label: 'Scholarships',       icon: 'workspace_premium' },
  REPORT:     { label: 'Reports',            icon: 'bar_chart' },
};


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
    RouterLink, TitleCasePipe, DecimalPipe, FormsModule,
    MatButtonModule, MatIconModule, MatTooltipModule, MatDialogModule,
    ProfileDocumentsComponent, AppDatePipe,
  ],
  templateUrl: './profile.component.html',
  styleUrl:    './profile.component.scss',
})
export class ProfileComponent implements OnInit, OnDestroy {

  @ViewChild('photoInput') private photoInput?: ElementRef<HTMLInputElement>;

  private readonly profileService    = inject(ProfileService);
  private readonly facultyService    = inject(FacultyService);
  private readonly studentService    = inject(StudentService);
  private readonly themeService      = inject(ThemeService);
  private readonly permService       = inject(PermissionService);
  private readonly bloodGroupService = inject(BloodGroupService);
  private readonly toast             = inject(ToastService);
  private readonly docSlots          = inject(DocumentSlotsService);
  private readonly dialog            = inject(MatDialog);

  protected readonly bloodGroups = signal<BloodGroup[]>([]);

  // ── Theme ─────────────────────────────────────────────────────────────────
  protected readonly swatches         = COLOR_SWATCHES;
  protected readonly activeSwatch     = this.themeService.activeSwatch;
  protected readonly primaryColor     = computed(() => this.activeSwatch().hex);
  protected readonly themePickerOpen  = signal(false);
  /** Fixed-position coordinates for the dropdown — escapes glass-card overflow:hidden */
  protected readonly themePickerPos   = signal<{ top: number; left: number }>({ top: 0, left: 0 });

  protected toggleThemePicker(e: MouseEvent): void {
    e.stopPropagation();
    if (!this.themePickerOpen()) {
      const rect = (e.currentTarget as HTMLElement).getBoundingClientRect();
      this.themePickerPos.set({ top: rect.bottom + 6, left: rect.left });
    }
    this.themePickerOpen.update(v => !v);
  }

  @HostListener('document:click')
  protected closeThemePicker(): void { this.themePickerOpen.set(false); }

  protected readonly changePasswordUrl = computed(() => {
    const { url, realm } = environment.keycloak;
    return `${url}/realms/${realm}/account/#/security/signingin`;
  });

  // ── Accessibility preferences (localStorage + CSS class on <html>) ─────────
  private readonly A11Y_KEY = 'cms_a11y_prefs';
  protected readonly reduceMotion = signal(this.loadA11y('reduceMotion', false));
  protected readonly largeText    = signal(this.loadA11y('largeText',    false));

  protected toggleReduceMotion(): void {
    const next = !this.reduceMotion();
    this.reduceMotion.set(next);
    document.documentElement.classList.toggle('reduce-motion', next);
    this.saveA11y();
  }

  protected toggleLargeText(): void {
    const next = !this.largeText();
    this.largeText.set(next);
    document.documentElement.classList.toggle('large-text', next);
    this.saveA11y();
  }

  private loadA11y(key: string, def: boolean): boolean {
    try {
      const raw = localStorage.getItem(this.A11Y_KEY);
      if (raw) return JSON.parse(raw)[key] ?? def;
    } catch { /* ignore */ }
    return def;
  }

  private saveA11y(): void {
    try {
      localStorage.setItem(this.A11Y_KEY, JSON.stringify({
        reduceMotion: this.reduceMotion(),
        largeText:    this.largeText(),
      }));
    } catch { /* ignore */ }
  }

  // ── Notification preferences (localStorage) ───────────────────────────────
  protected readonly notifCategories = NOTIF_CATEGORIES;
  protected readonly notifPrefs      = signal<NotifPrefs>(this.loadNotifPrefs());

  protected setNotifChannel(ch: NotifPrefs['channel']): void {
    this.notifPrefs.update(p => ({ ...p, channel: ch }));
    this.saveNotifPrefs();
  }

  protected toggleNotifCategory(key: string): void {
    this.notifPrefs.update(p => ({
      ...p,
      categories: { ...p.categories, [key]: !p.categories[key] },
    }));
    this.saveNotifPrefs();
  }

  private loadNotifPrefs(): NotifPrefs {
    try {
      const raw = localStorage.getItem(NOTIF_STORAGE_KEY);
      if (raw) return { ...DEFAULT_NOTIF_PREFS, ...JSON.parse(raw) };
    } catch { /* ignore */ }
    return { ...DEFAULT_NOTIF_PREFS, categories: { ...DEFAULT_NOTIF_PREFS.categories } };
  }

  private saveNotifPrefs(): void {
    try { localStorage.setItem(NOTIF_STORAGE_KEY, JSON.stringify(this.notifPrefs())); } catch { /* ignore */ }
  }

  // ── Role & Permissions ────────────────────────────────────────────────────
  protected readonly roleLabel       = this.permService.roleLabel;
  protected readonly roleLevel       = this.permService.level;
  protected readonly permsExpanded   = signal(false);

  protected readonly countReduce = (acc: number, g: { codes: string[] }) => acc + g.codes.length;

  protected readonly permGroups = computed(() => {
    const all = this.permService.permissions();
    const map = new Map<string, string[]>();
    for (const p of all) {
      const mod = p.split('_')[0];
      map.set(mod, [...(map.get(mod) ?? []), p]);
    }
    return Array.from(map.entries())
      .sort((a, b) => a[0].localeCompare(b[0]))
      .map(([mod, codes]) => ({
        mod,
        codes,
        label: MODULE_LABELS[mod]?.label ?? mod,
        icon:  MODULE_LABELS[mod]?.icon  ?? 'lock',
      }));
  });

  // ── Data ──────────────────────────────────────────────────────────────────
  protected readonly loading  = signal(true);
  protected readonly identity = signal<ProfileIdentity | null>(null);
  protected readonly faculty  = signal<Faculty | null>(null);
  protected readonly student  = signal<Student | null>(null);

  // ── Profile photo ─────────────────────────────────────────────────────────
  protected readonly photoUrl       = this.profileService.avatarDataUrl;
  protected readonly uploadingPhoto = signal(false);
  private photoObjectUrl: string | null = null;

  // ── Inline self-edit ──────────────────────────────────────────────────────
  protected readonly editMode = signal(false);
  protected readonly savingSelfInfo = signal(false);
  protected editForm: SelfUpdateRequest = {};

  // ── Profile completion ────────────────────────────────────────────────────
  protected readonly profileCompletion = computed<{ score: number; total: number; missing: string[]; segments: null[] }>(() => {
    const f  = this.faculty();
    const s  = this.student();
    const id = this.identity();
    const checks: Array<{ label: string; ok: boolean }> = [
      { label: 'Profile photo', ok: !!this.photoUrl() },
      { label: 'Bio',           ok: !!(f?.bio ?? s?.bio ?? id?.bio) },
      ...( (f || s) ? [
        { label: 'Phone number',  ok: !!(f?.phone ?? s?.phone) },
        { label: 'Blood group',   ok: !!(f?.bloodGroup ?? s?.bloodGroup) },
        { label: 'Date of birth', ok: !!(f?.dateOfBirth ?? s?.dateOfBirth) },
        { label: 'Address',       ok: !!(f?.address?.city ?? s?.city) },
      ] : []),
    ];
    const score   = checks.filter(c => c.ok).length;
    const missing = checks.filter(c => !c.ok).map(c => c.label);
    return { score, total: checks.length, missing, segments: Array(checks.length).fill(null) };
  });

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

  protected contactInfoItems(): InfoItem[] {
    const f = this.faculty();
    const s = this.student();
    if (f) {
      return [
        { icon: 'phone',           label: 'Phone',        value: f.phone ?? '—' },
        { icon: 'bloodtype',       label: 'Blood Group',  value: f.bloodGroup ?? '—' },
        { icon: 'cake',            label: 'Date of Birth',value: f.dateOfBirth ?? '—' },
        { icon: 'wc',              label: 'Gender',       value: f.gender ?? '—' },
        { icon: 'location_on',     label: 'Address',      value: this.formatFacultyAddress(f) },
        { icon: 'emergency',       label: 'Emergency Contact', value: f.emergencyContactName ? `${f.emergencyContactName}${f.emergencyContactRelationship ? ' (' + f.emergencyContactRelationship + ')' : ''}` : '—' },
        { icon: 'phone_forwarded', label: 'Emergency Phone',   value: f.emergencyContactPhone ?? '—' },
      ];
    }
    if (s) {
      return [
        { icon: 'phone',           label: 'Phone',        value: s.phone ?? '—' },
        { icon: 'bloodtype',       label: 'Blood Group',  value: s.bloodGroup ?? '—' },
        { icon: 'cake',            label: 'Date of Birth',value: s.dateOfBirth ?? '—' },
        { icon: 'wc',              label: 'Gender',       value: s.gender ?? '—' },
        { icon: 'family_restroom', label: 'Parent Mobile',value: s.parentMobile ?? s.fatherPhone ?? '—' },
        { icon: 'location_on',     label: 'Address',      value: this.formatStudentAddress(s) },
        { icon: 'emergency',       label: 'Emergency Contact', value: s.emergencyContactName ? `${s.emergencyContactName}${s.emergencyContactRelationship ? ' (' + s.emergencyContactRelationship + ')' : ''}` : '—' },
        { icon: 'phone_forwarded', label: 'Emergency Phone',   value: s.emergencyContactPhone ?? '—' },
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
    // Reapply saved accessibility prefs on page load
    document.documentElement.classList.toggle('reduce-motion', this.reduceMotion());
    document.documentElement.classList.toggle('large-text',    this.largeText());
    // Load blood group master
    this.bloodGroupService.getActiveBloodGroups().subscribe({
      next: groups => this.bloodGroups.set(groups),
    });
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
      // Upload the cropped blob as PNG to preserve transparency
      const croppedFile = new File([result.blob], file.name.replace(/\.(jpg|jpeg|png|webp)$/i, '.png'), {
        type: 'image/png',
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
        // Reload both identity (covers admin bio/phone/bloodGroup + faculty/student bio)
        // and the linked faculty/student record (covers all other self-editable fields)
        this.profileService.reloadIdentity().subscribe({
          next: id => this.identity.set(id),
        });
        this.reloadLinkedProfile();
        this.toast.success('Profile updated');
        this.savingSelfInfo.set(false);
        this.editMode.set(false);
      },
      error: err => {
        this.toast.error(err?.error?.message ?? 'Failed to update profile');
        this.savingSelfInfo.set(false);
      },
    });
  }

  private initEditForm(): void {
    const f  = this.faculty();
    const s  = this.student();
    const id = this.identity();
    if (f) {
      this.editForm = {
        phone: f.phone ?? '', bloodGroup: f.bloodGroup ?? '', bio: f.bio ?? '',
        postalAddress: f.address?.postalAddress ?? '', street: f.address?.street ?? '',
        city: f.address?.city ?? '', district: f.address?.district ?? '',
        state: f.address?.state ?? '', pincode: f.address?.pincode ?? '',
        emergencyContactName: f.emergencyContactName ?? '',
        emergencyContactRelationship: f.emergencyContactRelationship ?? '',
        emergencyContactPhone: f.emergencyContactPhone ?? '',
      };
    } else if (s) {
      this.editForm = {
        phone: s.phone ?? '', bloodGroup: s.bloodGroup ?? '', bio: s.bio ?? '',
        postalAddress: s.postalAddress ?? '', street: s.street ?? '',
        city: s.city ?? '', district: s.district ?? '',
        state: s.state ?? '', pincode: s.pincode ?? '',
        emergencyContactName: s.emergencyContactName ?? '',
        emergencyContactRelationship: s.emergencyContactRelationship ?? '',
        emergencyContactPhone: s.emergencyContactPhone ?? '',
      };
    } else {
      // Admin — bio, phone, bloodGroup stored on app_users
      this.editForm = {
        bio:        id?.bio        ?? '',
        phone:      id?.phone      ?? '',
        bloodGroup: id?.bloodGroup ?? '',
        emergencyContactName: '',
        emergencyContactRelationship: '',
        emergencyContactPhone: '',
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
  protected selectTheme(swatch: ColorSwatch): void {
    this.themeService.applyTheme(swatch);
    this.themePickerOpen.set(false);
  }

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


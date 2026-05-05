import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CmsPreviewCardComponent } from '../../../shared/preview-card/preview-card.component';
import { CmsTipsCardComponent, CmsTip } from '../../../shared/tips-card/tips-card.component';
import { FacultyService } from '../faculty.service';
import {
  FacultyRequest,
  FacultyDocument,
  FacultyDocumentSlot,
  Designation,
  FacultyStatus,
  FACULTY_DOCUMENT_SLOTS,
  DESIGNATION_OPTIONS,
  FACULTY_STATUS_OPTIONS,
  FACULTY_TYPE_OPTIONS,
  GENDER_OPTIONS,
  MARITAL_STATUS_OPTIONS,
  BANK_ACCOUNT_TYPE_OPTIONS,
} from '../faculty.model';
import { DepartmentService } from '../../department/department.service';
import { Department } from '../../department/department.model';
import { BloodGroupService } from '../../blood-group/blood-group.service';
import { BloodGroup } from '../../blood-group/blood-group.model';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { FACULTY_FORM_TOUR } from '../../../shared/tour/tours/faculty.tours';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';

@Component({
  selector: 'app-faculty-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    CmsTourButtonComponent,
    CmsPreviewCardComponent,
    CmsTipsCardComponent,
  ],
  templateUrl: './faculty-form.component.html',
  styleUrl: './faculty-form.component.scss',
})
export class FacultyFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly facultyService = inject(FacultyService);
  private readonly departmentService = inject(DepartmentService);
  private readonly bloodGroupService = inject(BloodGroupService);
  private readonly toast = inject(ToastService);
  private readonly tourService = inject(TourService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Faculty');
  protected readonly departments = signal<Department[]>([]);
  protected readonly bloodGroups = signal<BloodGroup[]>([]);

  protected readonly designationOptions = DESIGNATION_OPTIONS;
  protected readonly statusOptions = FACULTY_STATUS_OPTIONS;
  protected readonly facultyTypeOptions = FACULTY_TYPE_OPTIONS;
  protected readonly genderOptions = GENDER_OPTIONS;
  protected readonly maritalStatusOptions = MARITAL_STATUS_OPTIONS;
  protected readonly bankAccountTypeOptions = BANK_ACCOUNT_TYPE_OPTIONS;
  protected readonly documentSlots = FACULTY_DOCUMENT_SLOTS;

  // Documents (only available in edit mode — keyed by document type code)
  protected readonly documents = signal<Record<string, FacultyDocument>>({});
  protected readonly uploadingType = signal<string | null>(null);

  // Preview signals
  protected readonly previewFirst = signal('');
  protected readonly previewLast  = signal('');
  protected readonly previewCode  = signal('');
  protected readonly previewEmail = signal('');
  protected readonly previewPhone = signal('');
  protected readonly previewDesignation = signal<string | null>(null);
  protected readonly previewDeptId  = signal<number | null>(null);
  protected readonly previewFacultyType = signal<string | null>(null);
  protected readonly previewBlood = signal('');

  protected readonly previewFullName = computed(() => `${this.previewFirst()} ${this.previewLast()}`.trim());
  protected readonly previewInitials = computed(() => ((this.previewFirst()[0] ?? '') + (this.previewLast()[0] ?? '')).toUpperCase());
  protected readonly previewDeptName = computed(() => {
    const id = this.previewDeptId();
    if (!id) return '';
    return this.departments().find(d => d.id === id)?.name ?? '';
  });
  protected readonly previewDesignationLabel = computed(() => {
    const v = this.previewDesignation();
    if (!v) return '';
    return DESIGNATION_OPTIONS.find(o => o.value === v)?.label ?? v;
  });
  protected readonly previewFacultyTypeLabel = computed(() => {
    const v = this.previewFacultyType();
    if (!v) return '';
    return FACULTY_TYPE_OPTIONS.find(o => o.value === v)?.label ?? v;
  });

  protected readonly TIPS: CmsTip[] = [
    { icon: 'badge',     title: 'Employee Code', subtitle: 'Use a unique identifier per faculty (e.g., EMP001) — it cannot be changed later.' },
    { icon: 'mail',      title: 'Email',         subtitle: 'Used for login and notifications. Must be unique across all faculty.' },
    { icon: 'school',    title: 'Department',    subtitle: 'Assign to one academic department for course allocation and reports.' },
    { icon: 'cloud_upload', title: 'Documents',  subtitle: 'After saving, upload PAN, Aadhaar, degrees, appointment letter, and signed photo.' },
  ];

  private facultyId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    // Identity
    employeeCode: ['', [Validators.required, Validators.maxLength(50)]],
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    phone: ['', [Validators.maxLength(20)]],
    departmentId: [null as number | null, [Validators.required]],
    designation: [null as Designation | null, [Validators.required]],
    facultyType: [null as string | null],
    specialization: ['', [Validators.maxLength(255)]],
    labExpertise: ['', [Validators.maxLength(1000)]],
    joiningDate: ['', [Validators.required]],
    status: ['ACTIVE' as FacultyStatus],

    // Personal
    dateOfBirth: [''],
    gender: [null as string | null],
    maritalStatus: [null as string | null],
    nationality: [''],
    religion: [''],
    bloodGroup: [''],
    panNumber: ['', [Validators.maxLength(20)]],
    aadhaarNumber: ['', [Validators.maxLength(20)]],

    // Bank
    bankAccountHolder: [''],
    bankAccountNumber: [''],
    bankIfscCode: [''],
    bankName: [''],
    bankBranch: [''],
    bankAccountType: [null as string | null],

    // Address
    postalAddress: [''],
    street: [''],
    city: [''],
    district: [''],
    state: [''],
    pincode: [''],

    // Experience (years, decimals allowed)
    teachingExperienceUgYears: [null as number | null],
    teachingExperiencePgYears: [null as number | null],
    teachingExperiencePhdYears: [null as number | null],
    clinicalExperienceUgYears: [null as number | null],
    clinicalExperiencePgYears: [null as number | null],
    clinicalExperiencePhdYears: [null as number | null],
  });

  constructor() {
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(v => {
        this.previewFirst.set((v.firstName ?? '').trim());
        this.previewLast.set((v.lastName ?? '').trim());
        this.previewCode.set((v.employeeCode ?? '').trim());
        this.previewEmail.set((v.email ?? '').trim());
        this.previewPhone.set((v.phone ?? '').trim());
        this.previewDesignation.set(v.designation ?? null);
        this.previewDeptId.set(v.departmentId ?? null);
        this.previewFacultyType.set(v.facultyType ?? null);
        this.previewBlood.set(v.bloodGroup ?? '');
      });
  }

  ngOnInit(): void {
    this.tourService.register('faculty-form', FACULTY_FORM_TOUR);
    this.loadDepartments();
    this.loadBloodGroups();

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.facultyId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Faculty');
      this.loadFaculty();
      this.loadDocuments();
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }

    const v = this.form.value;
    const request: FacultyRequest = {
      employeeCode: (v.employeeCode ?? '').trim(),
      firstName: (v.firstName ?? '').trim(),
      lastName: (v.lastName ?? '').trim(),
      email: (v.email ?? '').trim(),
      phone: v.phone?.trim() || undefined,
      departmentId: v.departmentId,
      designation: v.designation,
      specialization: v.specialization?.trim() || undefined,
      labExpertise: v.labExpertise?.trim() || undefined,
      joiningDate: v.joiningDate,
      status: v.status || undefined,
      facultyType: v.facultyType || undefined,
      panNumber: v.panNumber?.trim()?.toUpperCase() || undefined,
      aadhaarNumber: v.aadhaarNumber?.trim() || undefined,
      dateOfBirth: v.dateOfBirth || undefined,
      gender: v.gender || undefined,
      maritalStatus: v.maritalStatus || undefined,
      nationality: v.nationality?.trim() || undefined,
      religion: v.religion?.trim() || undefined,
      bloodGroup: v.bloodGroup || undefined,
      bankAccountHolder: v.bankAccountHolder?.trim() || undefined,
      bankAccountNumber: v.bankAccountNumber?.trim() || undefined,
      bankIfscCode: v.bankIfscCode?.trim()?.toUpperCase() || undefined,
      bankName: v.bankName?.trim() || undefined,
      bankBranch: v.bankBranch?.trim() || undefined,
      bankAccountType: v.bankAccountType || undefined,
      address: {
        postalAddress: v.postalAddress?.trim() || undefined,
        street: v.street?.trim() || undefined,
        city: v.city?.trim() || undefined,
        district: v.district?.trim() || undefined,
        state: v.state?.trim() || undefined,
        pincode: v.pincode?.trim() || undefined,
      },
      teachingExperienceUgYears: this.numberOrUndefined(v.teachingExperienceUgYears),
      teachingExperiencePgYears: this.numberOrUndefined(v.teachingExperiencePgYears),
      teachingExperiencePhdYears: this.numberOrUndefined(v.teachingExperiencePhdYears),
      clinicalExperienceUgYears: this.numberOrUndefined(v.clinicalExperienceUgYears),
      clinicalExperiencePgYears: this.numberOrUndefined(v.clinicalExperiencePgYears),
      clinicalExperiencePhdYears: this.numberOrUndefined(v.clinicalExperiencePhdYears),
    };

    this.saving.set(true);

    const operation$ = this.isEditMode()
      ? this.facultyService.update(this.facultyId!, request)
      : this.facultyService.create(request);

    operation$.subscribe({
      next: () => {
        const message = this.isEditMode()
          ? 'Faculty updated successfully'
          : 'Faculty created successfully';
        this.toast.success(message);
        this.saving.set(false);
        // Always return to the faculty list after a successful save (both
        // create and update). Documents can be added/replaced later by
        // re-opening the record in Edit mode.
        void this.router.navigate(['/faculty']);
      },
      error: (err) => {
        const fallback = this.isEditMode()
          ? 'Failed to update faculty'
          : 'Failed to create faculty';
        this.toast.error(err?.error?.message ?? fallback);
        this.saving.set(false);
      },
    });
  }

  protected onFileSelected(slot: FacultyDocumentSlot, event: Event): void {
    if (!this.facultyId) return;
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    // Client-side guard so the user gets immediate feedback rather than a 413
    // round-trip when they pick a too-large file (backend max is 10 MB).
    const MAX_BYTES = 10 * 1024 * 1024;
    if (file.size > MAX_BYTES) {
      this.toast.error(`${slot.label} must be 10 MB or smaller (selected: ${this.formatBytes(file.size)})`);
      input.value = '';
      return;
    }

    this.uploadingType.set(slot.type);
    this.facultyService.uploadDocument(this.facultyId, slot.type, file).subscribe({
      next: (doc) => {
        this.documents.update(d => ({ ...d, [slot.type]: doc }));
        this.uploadingType.set(null);
        this.toast.success(`${slot.label} uploaded`);
        input.value = '';
      },
      error: (err) => {
        this.uploadingType.set(null);
        const detail = err?.error?.message
          ?? (err?.status === 413
            ? `${slot.label} is too large (max 10 MB)`
            : `Failed to upload ${slot.label}`);
        this.toast.error(detail);
        input.value = '';
      },
    });
  }

  protected viewDocument(slot: FacultyDocumentSlot): void {
    if (!this.facultyId) return;
    const doc = this.documents()[slot.type];
    if (!doc) return;

    this.facultyService.downloadDocumentBlob(this.facultyId, doc.id).subscribe({
      next: (response) => {
        const blob = response.body;
        if (!blob) {
          this.toast.error('Failed to open document');
          return;
        }
        const objectUrl = URL.createObjectURL(blob);
        // Open in a new tab; revoke after a short delay so the tab can load it.
        window.open(objectUrl, '_blank', 'noopener');
        setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000);
      },
      error: (err) => {
        const detail = err?.error?.message ?? 'Failed to open document';
        this.toast.error(detail);
      },
    });
  }

  protected removeDocument(slot: FacultyDocumentSlot): void {
    if (!this.facultyId) return;
    const doc = this.documents()[slot.type];
    if (!doc) return;
    if (!confirm(`Remove uploaded ${slot.label}?`)) return;

    this.facultyService.deleteDocument(this.facultyId, doc.id).subscribe({
      next: () => {
        const next = { ...this.documents() };
        delete next[slot.type];
        this.documents.set(next);
        this.toast.success(`${slot.label} removed`);
      },
      error: (err) => this.toast.error(err?.error?.message ?? 'Failed to remove document'),
    });
  }

  protected documentDownloadUrl(slot: FacultyDocumentSlot): string | null {
    if (!this.facultyId) return null;
    const doc = this.documents()[slot.type];
    if (!doc) return null;
    return this.facultyService.documentDownloadUrl(this.facultyId, doc.id);
  }

  protected documentFor(slot: FacultyDocumentSlot): FacultyDocument | undefined {
    return this.documents()[slot.type];
  }

  protected slotsByGroup(group: FacultyDocumentSlot['group']): FacultyDocumentSlot[] {
    return this.documentSlots.filter(s => s.group === group);
  }

  protected formatBytes(bytes: number | undefined): string {
    if (bytes == null) return '';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  protected getErrorMessage(fieldName: string): string {
    const control = this.form.get(fieldName);
    if (!control || !control.errors) return '';
    if (control.errors['required']) return `${this.getFieldLabel(fieldName)} is required`;
    if (control.errors['email']) return 'Please enter a valid email address';
    if (control.errors['maxlength']) {
      const maxLength = control.errors['maxlength'].requiredLength;
      return `${this.getFieldLabel(fieldName)} must be at most ${maxLength} characters`;
    }
    return '';
  }

  private getFieldLabel(fieldName: string): string {
    const labels: Record<string, string> = {
      employeeCode: 'Employee Code',
      firstName: 'First Name',
      lastName: 'Last Name',
      email: 'Email',
      phone: 'Phone',
      departmentId: 'Department',
      designation: 'Designation',
      specialization: 'Specialization',
      labExpertise: 'Lab Expertise',
      joiningDate: 'Joining Date',
      status: 'Status',
      panNumber: 'PAN Number',
      aadhaarNumber: 'Aadhaar Number',
    };
    return labels[fieldName] || fieldName;
  }

  private numberOrUndefined(v: unknown): number | undefined {
    if (v == null || v === '') return undefined;
    const n = Number(v);
    return isNaN(n) ? undefined : n;
  }

  private loadDepartments(): void {
    this.departmentService.getAll().subscribe({
      next: (departments) => this.departments.set(departments),
      error: () => this.toast.error('Failed to load departments'),
    });
  }

  private loadBloodGroups(): void {
    this.bloodGroupService.getActiveBloodGroups().subscribe({
      next: (data) => this.bloodGroups.set(data),
      error: () => { /* non-blocking — blood group is optional */ },
    });
  }

  private loadFaculty(): void {
    if (!this.facultyId) return;

    this.loading.set(true);
    this.facultyService.getById(this.facultyId).subscribe({
      next: (faculty) => {
        this.form.patchValue({
          employeeCode: faculty.employeeCode,
          firstName: faculty.firstName,
          lastName: faculty.lastName,
          email: faculty.email,
          phone: faculty.phone || '',
          departmentId: faculty.departmentId,
          designation: faculty.designation,
          facultyType: faculty.facultyType ?? null,
          specialization: faculty.specialization || '',
          labExpertise: faculty.labExpertise || '',
          joiningDate: faculty.joiningDate,
          status: faculty.status,
          dateOfBirth: faculty.dateOfBirth || '',
          gender: faculty.gender ?? null,
          maritalStatus: faculty.maritalStatus ?? null,
          nationality: faculty.nationality || '',
          religion: faculty.religion || '',
          bloodGroup: faculty.bloodGroup || '',
          panNumber: faculty.panNumber || '',
          aadhaarNumber: faculty.aadhaarNumber || '',
          bankAccountHolder: faculty.bankAccountHolder || '',
          bankAccountNumber: faculty.bankAccountNumber || '',
          bankIfscCode: faculty.bankIfscCode || '',
          bankName: faculty.bankName || '',
          bankBranch: faculty.bankBranch || '',
          bankAccountType: faculty.bankAccountType ?? null,
          postalAddress: faculty.address?.postalAddress || '',
          street: faculty.address?.street || '',
          city: faculty.address?.city || '',
          district: faculty.address?.district || '',
          state: faculty.address?.state || '',
          pincode: faculty.address?.pincode || '',
          teachingExperienceUgYears: faculty.teachingExperienceUgYears ?? null,
          teachingExperiencePgYears: faculty.teachingExperiencePgYears ?? null,
          teachingExperiencePhdYears: faculty.teachingExperiencePhdYears ?? null,
          clinicalExperienceUgYears: faculty.clinicalExperienceUgYears ?? null,
          clinicalExperiencePgYears: faculty.clinicalExperiencePgYears ?? null,
          clinicalExperiencePhdYears: faculty.clinicalExperiencePhdYears ?? null,
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load faculty');
        void this.router.navigate(['/faculty']);
      },
    });
  }

  private loadDocuments(): void {
    if (!this.facultyId) return;
    this.facultyService.getDocuments(this.facultyId).subscribe({
      next: (docs) => {
        const map: Record<string, FacultyDocument> = {};
        for (const d of docs) map[d.documentType] = d;
        this.documents.set(map);
      },
      error: () => { /* non-blocking */ },
    });
  }
}

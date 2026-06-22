import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatTabsModule } from '@angular/material/tabs';
import { MatIconModule } from '@angular/material/icon';
import { FormsModule } from '@angular/forms';
import { AdmissionService } from '../admission.service';
import {
  AcademicQualificationResponse,
  AdmissionDocumentResponse,
  AdmissionResponse,
} from '../admission.model';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsSkeletonComponent } from '../../../shared/skeleton/skeleton.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { computeInitials } from '../../../shared/utils/initials';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { ADMISSION_DETAIL_TOUR } from '../../../shared/tour/tours/admission.tours';
import { StudentService } from '../../student/student.service';
import { Student } from '../../student/student.model';
import { AdmissionFormData, viewAdmissionForm, printAdmissionForm, downloadAdmissionForm } from '../../../shared/utils/print-admission-form.utils';
import { ProfileDocumentsComponent } from '../../../shared/profile-documents/profile-documents.component';
import { PermissionService } from '../../../core/permissions/permission.service';
import { SettingsService } from '../../settings/settings.service';

@Component({
  selector: 'app-admission-detail',
  standalone: true,
  imports: [
    AppDatePipe,
    RouterLink,
    FormsModule,
    MatTabsModule,
    MatIconModule,
    CmsSkeletonComponent,
    CmsStatusBadgeComponent,
    CmsTourButtonComponent,
    ProfileDocumentsComponent,
  ],
  templateUrl: './admission-detail.component.html',
  styleUrl: './admission-detail.component.scss',
})
export class AdmissionDetailComponent implements OnInit {
  private static readonly TAB_INDEX_KEY = 'admission-detail-tab-index';

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly admissionService = inject(AdmissionService);
  private readonly studentService = inject(StudentService);
  private readonly toast = inject(ToastService);
  private readonly tourService = inject(TourService);
  private readonly permissionService = inject(PermissionService);
  private readonly settingsService = inject(SettingsService);

  readonly loading = signal(true);
  readonly admission = signal<AdmissionResponse | null>(null);
  readonly student = signal<Student | null>(null);
  readonly qualifications = signal<AcademicQualificationResponse[]>([]);
  readonly documents = signal<AdmissionDocumentResponse[]>([]);
  readonly checklist = signal<{ mandatory: Record<string, string>; optional: Record<string, string> }>({ mandatory: {}, optional: {} });
  readonly passportPhotoUrl = signal<string | null>(null);
  readonly collegeLogo = signal<string | null>(null);
  readonly collegeName = signal<string | null>(null);
  readonly collegeTrustLine = signal<string | null>(null);
  readonly collegeAddress = signal<string | null>(null);
  readonly collegePhone = signal<string | null>(null);
  readonly collegeEmail = signal<string | null>(null);

  readonly selectedTabIndex = signal(this.readSavedTabIndex());
  readonly expandedQuals = signal(new Set<number>());

  readonly initials = computed(() => computeInitials(this.admission()?.studentName));
  readonly canEdit  = computed(() =>
    !['GRADUATED', 'WITHDRAWN', 'EXPELLED'].includes(this.admission()?.studentStatus ?? ''),
  );

  readonly verifiedDocsCount = computed(
    () => this.documents().filter((doc) => doc.verificationStatus === 'VERIFIED').length,
  );

  ngOnInit(): void {
    this.tourService.register('admission-detail', ADMISSION_DETAIL_TOUR);
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.loadAll(id);
    this.loadBranding();
  }

  private loadBranding(): void {
    this.settingsService.getAll().subscribe({
      next: (configs) => {
        const val = (key: string) => configs.find(c => c.configKey === key)?.configValue || null;
        this.collegeName.set(val('college.name'));
        this.collegeAddress.set(val('college.address'));
        this.collegePhone.set(val('college.phone'));
        this.collegeEmail.set(val('college.email'));

        const trustName = val('college.trust_name');
        const regNum = val('college.registration_number');
        if (trustName) {
          const trustLine = regNum
            ? `Run By ${trustName} (Regn. No. ${regNum})`
            : `Run By ${trustName}`;
          this.collegeTrustLine.set(trustLine);
        }

        // logo_data is a base64 data URL stored directly in the DB via the Branding settings page
        const logoData = val('college.logo_data');
        if (logoData) this.collegeLogo.set(logoData);
      },
      error: () => {},
    });
  }

  private loadAll(id: number): void {
    this.admissionService.getById(id).subscribe({
      next: (a) => {
        this.admission.set(a);
        this.loadDocuments(id);
        this.loadQualifications(id);
        this.studentService.getById(a.studentId).subscribe({
          next: (s) => this.student.set(s),
          error: () => {},
        });
      },
      error: () => {
        this.toast.error('Failed to load admission');
        this.loading.set(false);
      },
    });
  }

  private loadQualifications(id: number): void {
    this.admissionService.getQualifications(id).subscribe({
      next: (q) => this.qualifications.set(q),
      error: () => {},
    });
  }

  private loadDocuments(id: number): void {
    this.admissionService.getDocuments(id).subscribe({
      next: (docs) => {
        this.documents.set(docs);
        this.loading.set(false);
        const photoDoc = docs.find((d) => d.documentType === 'PASSPORT_PHOTO' && d.hasFile);
        if (photoDoc) {
          this.loadPassportPhoto(photoDoc.id);
        } else {
          this.passportPhotoUrl.set(null);
        }
      },
      error: () => this.loading.set(false),
    });
    this.admissionService.getDocumentChecklist(id).subscribe({
      next: (cl) => this.checklist.set(cl),
      error: () => {},
    });
  }

  private loadPassportPhoto(documentId: number): void {
    this.admissionService.downloadDocumentBlob(documentId).subscribe({
      next: (response) => {
        const blob = response.body;
        if (!blob) return;
        const reader = new FileReader();
        reader.onload = () => this.passportPhotoUrl.set(reader.result as string);
        reader.readAsDataURL(blob);
      },
      error: () => {},
    });
  }

  getChecklistEntries(): { type: string; status: string }[] {
    const cl = this.checklist();
    return [
      ...Object.entries(cl.mandatory),
      ...Object.entries(cl.optional),
    ].map(([type, status]) => ({ type, status }));
  }

  switchTab(index: number): void {
    this.selectedTabIndex.set(index);
    try {
      localStorage.setItem(AdmissionDetailComponent.TAB_INDEX_KEY, String(index));
    } catch {
      // Ignore storage failures (private browsing, quota, etc.).
    }
  }

  toggleQual(id: number): void {
    this.expandedQuals.update((set) => {
      const next = new Set(set);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  }

  isQualExpanded(id: number): boolean {
    return this.expandedQuals().has(id);
  }

  yesNo(value: boolean | null): string {
    if (value == null) return '—';
    return value ? 'Yes' : 'No';
  }

  handleDocumentSlotsChange(): void {
    const admissionId = this.admission()?.id;
    if (admissionId) this.loadDocuments(admissionId);
  }

  canManageDocuments(): boolean {
    return this.permissionService.has('DOCUMENT_SUBMISSION_MANAGE');
  }

  canForceReplaceDocuments(): boolean {
    return this.permissionService.has('DOCUMENT_VERIFIED_OVERRIDE');
  }

  viewForm(): void {
    const data = this.buildFormData();
    if (data) viewAdmissionForm(data);
  }

  printForm(): void {
    const data = this.buildFormData();
    if (data) printAdmissionForm(data);
  }

  downloadForm(): void {
    const data = this.buildFormData();
    if (data) downloadAdmissionForm(data);
  }

  private buildFormData(): AdmissionFormData | null {
    const a = this.admission();
    if (!a) return null;
    const s = this.student();
    const checklistDocuments = this.getChecklistEntries().map((d) => ({
      documentType:       d.type,
      verificationStatus: d.status,
    }));
    return {
      admissionNumber:   a.admissionNumber ?? '',
      applicationDate:   a.applicationDate,
      admissionDate:     s?.admissionDate ?? '',
      academicYear:      a.joiningAcademicYearName ?? '',
      programName:       a.programName ?? '',
      courseName:        a.courseName,
      yearOfStudy:       a.yearOfStudy ?? null,
      studentType:       a.studentType ?? null,
      admissionQuota:    null,
      collegeLogo:       this.collegeLogo(),
      collegeName:       this.collegeName(),
      collegeTrustLine:  this.collegeTrustLine(),
      collegeAddress:    this.collegeAddress(),
      collegePhone:      this.collegePhone(),
      collegeEmail:      this.collegeEmail(),
      studentName:       a.studentName,
      dateOfBirth:       s?.dateOfBirth ?? null,
      gender:            s?.gender ?? null,
      bloodGroup:        s?.bloodGroup ?? null,
      physicalDisability: a.physicalDisability ?? s?.physicalDisability ?? null,
      aadharNumber:      null,
      nationality:       s?.nationality ?? null,
      religion:          s?.religion ?? null,
      communityCategory: s?.communityCategory ?? null,
      caste:             s?.caste ?? null,
      phone:             s?.phone ?? null,
      email:             s?.email ?? null,
      postalAddress:     s?.postalAddress ?? null,
      street:            s?.street ?? null,
      city:              s?.city ?? null,
      district:          s?.district ?? null,
      state:             s?.state ?? null,
      pincode:           s?.pincode ?? null,
      fatherName:        s?.fatherName ?? null,
      fatherPhone:       s?.fatherPhone ?? null,
      fatherEmail:       s?.fatherEmail ?? null,
      motherName:        s?.motherName ?? null,
      motherPhone:       s?.motherPhone ?? null,
      motherEmail:       s?.motherEmail ?? null,
      qualifications: this.qualifications().map(q => ({
        qualificationType:      q.qualificationType,
        schoolName:             q.schoolName,
        universityOrBoard:      q.universityOrBoard,
        monthAndYearOfPassing:  q.monthAndYearOfPassing,
        percentage:             q.percentage,
        totalMarks:             q.totalMarks,
        majorSubject:           q.majorSubject,
      })),
      documents: (checklistDocuments.length ? checklistDocuments : this.documents()).map(d => ({
        documentType:       d.documentType,
        verificationStatus: d.verificationStatus,
      })),
      passportPhotoUrl:      this.passportPhotoUrl(),
      declarationPlace:      a.declarationPlace,
      declarationDate:       a.declarationDate ? String(a.declarationDate) : null,
      parentConsentGiven:    a.parentConsentGiven,
      applicantConsentGiven: a.applicantConsentGiven,
    };
  }

  edit(): void {
    const a = this.admission();
    if (a) void this.router.navigate(['/admissions', a.id, 'edit']);
  }

  private readSavedTabIndex(): number {
    try {
      const value = Number(localStorage.getItem(AdmissionDetailComponent.TAB_INDEX_KEY));
      return Number.isInteger(value) && value >= 0 && value <= 2 ? value : 0;
    } catch {
      return 0;
    }
  }
}

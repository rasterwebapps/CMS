import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { EnquiryService } from '../enquiry.service';
import { Enquiry, EnquiryConversionPrefillResponse, EnquiryConversionRequest, DocumentVerificationStatus } from '../enquiry.model';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear } from '../../academic-year/academic-year.model';
import { CommunityService } from '../../community/community.service';
import { BloodGroupService } from '../../blood-group/blood-group.service';
import { Community } from '../../community/community.model';
import { BloodGroup } from '../../blood-group/blood-group.model';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { ENQUIRY_CONVERT_TOUR } from '../../../shared/tour/tours/enquiry.tours';
import { computeInitials } from '../../../shared/utils/initials';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';
import { CmsCountryStateDistrictSelectorComponent } from '../../../shared/country-state-district-selector/country-state-district-selector.component';
import { AdmissionService } from '../../admission/admission.service';
import { AdmissionDocumentResponse } from '../../admission/admission.model';
import { AdmissionFormData, viewAdmissionForm, printAdmissionForm, downloadAdmissionForm } from '../../../shared/utils/print-admission-form.utils';
import { SettingsService } from '../../settings/settings.service';

interface SuccessState {
  admissionNumber: string;
  studentId: number;
  studentName: string;
  enquiry: Enquiry;
  academicYearName: string;
}

@Component({
  selector: 'app-enquiry-convert',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    InrPipe,
    CmsTourButtonComponent,
    CmsCountryStateDistrictSelectorComponent,
  ],
  templateUrl: './enquiry-convert.component.html',
  styleUrl: './enquiry-convert.component.scss',
})
export class EnquiryConvertComponent implements OnInit {
  private readonly route             = inject(ActivatedRoute);
  private readonly router            = inject(Router);
  private readonly fb                = inject(FormBuilder);
  private readonly enquiryService    = inject(EnquiryService);
  private readonly academicYearSvc   = inject(AcademicYearService);
  private readonly admissionService  = inject(AdmissionService);
  private readonly toast             = inject(ToastService);
  private readonly tourService       = inject(TourService);

  protected readonly computeInitials = computeInitials;

  private readonly communityService  = inject(CommunityService);
  private readonly bloodGroupService = inject(BloodGroupService);
  private readonly settingsService   = inject(SettingsService);

  protected readonly enquiry               = signal<Enquiry | null>(null);
  protected readonly prefill               = signal<EnquiryConversionPrefillResponse | null>(null);
  protected readonly academicYears         = signal<AcademicYear[]>([]);
  protected readonly communities           = signal<Community[]>([]);
  protected readonly bloodGroups           = signal<BloodGroup[]>([]);
  protected readonly selectedAcademicYearId = signal<number | null>(null);
  protected readonly loading               = signal(true);
  protected readonly saving                = signal(false);
  protected readonly successState          = signal<SuccessState | null>(null);
  protected readonly admissionDocs         = signal<AdmissionDocumentResponse[]>([]);
  protected readonly admissionChecklist    = signal<Record<string, string>>({});
  protected readonly printReady            = signal(false);

  private readonly collegeLogo      = signal<string | null>(null);
  private readonly collegeName      = signal<string | null>(null);
  private readonly collegeTrustLine = signal<string | null>(null);
  private readonly collegeAddress   = signal<string | null>(null);
  private readonly collegePhone     = signal<string | null>(null);
  private readonly collegeEmail     = signal<string | null>(null);

  /** Document verification status — loaded alongside the enquiry. */
  protected readonly docVerification  = signal<DocumentVerificationStatus | null>(null);
  protected readonly docsNotVerified  = signal(false);

  // Consent document files
  protected readonly parentConsentFile    = signal<File | null>(null);
  protected readonly applicantConsentFile = signal<File | null>(null);

  protected readonly genderOptions = ['MALE', 'FEMALE', 'OTHER'] as const;

  protected readonly form: FormGroup = this.fb.group({
    firstName:    ['', Validators.required],
    lastName:     ['', Validators.required],
    email:        ['', [Validators.required, Validators.email]],
    phone:        ['', Validators.required],
    admissionDate: ['', Validators.required],

    applicationDate: ['', Validators.required],

    // Always 1 for new admissions — hidden from UI
    yearOfStudy: [1, [Validators.required, Validators.min(1)]],

    dateOfBirth:       ['', Validators.required],
    gender:            ['', Validators.required],
    aadharNumber:      ['', Validators.required],
    nationality:       ['', Validators.required],
    religion:          ['', Validators.required],
    communityCategory: ['', Validators.required],
    caste:             ['', Validators.required],
    bloodGroup:        ['', Validators.required],
    physicalDisability: [false],
    fatherName:        ['', Validators.required],
    fatherPhone:       ['', Validators.required],
    fatherEmail:       ['', [Validators.required, Validators.email]],
    motherName:        ['', Validators.required],
    motherPhone:       ['', Validators.required],
    motherEmail:       ['', [Validators.required, Validators.email]],
    address: this.fb.group({
      country:       [null as number | null, Validators.required],
      postalAddress: ['', Validators.required],
      street:        ['', Validators.required],
      city:          ['', Validators.required],
      district:      ['', Validators.required],
      state:         ['', Validators.required],
      pincode:       ['', Validators.required],
    }),

    declarationPlace: ['', Validators.required],
    declarationDate:  ['', Validators.required],
    parentConsentGiven:    [false, Validators.requiredTrue],
    applicantConsentGiven: [false, Validators.requiredTrue],
  });

  /** Expose the nested address FormGroup for cms-state-district-selector */
  get addressForm(): FormGroup {
    return this.form.get('address') as FormGroup;
  }

  ngOnInit(): void {
    this.tourService.register('enquiry-convert', ENQUIRY_CONVERT_TOUR);
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) this.load(id);
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
          this.collegeTrustLine.set(regNum
            ? `Run By ${trustName} (Regn. No. ${regNum})`
            : `Run By ${trustName}`);
        }
        const logoData = val('college.logo_data');
        if (logoData) this.collegeLogo.set(logoData);
      },
      error: () => {},
    });
  }

  private load(id: number): void {
    forkJoin({
      enquiry: this.enquiryService.getEnquiryById(id),
      prefill:  this.enquiryService.getConversionPrefill(id),
      years:    this.academicYearSvc.getAllAcademicYears(),
      communities: this.communityService.getActiveCommunities(),
      bloodGroups: this.bloodGroupService.getActiveBloodGroups(),
      verification: this.enquiryService.getDocumentVerificationStatus(id),
    }).subscribe({
      next: ({ enquiry, prefill, years, communities, bloodGroups, verification }) => {
        this.enquiry.set(enquiry);
        if (enquiry.status !== 'DOCUMENTS_VERIFIED') {
          this.docsNotVerified.set(true);
          this.loading.set(false);
          this.toast.warning('Complete Admission is allowed only after documents are verified.');
          const target = enquiry.status === 'DOCUMENTS_SUBMITTED'
            ? ['/enquiries/document-verification', enquiry.id]
            : ['/enquiries', enquiry.id];
          void this.router.navigate(target);
          return;
        }

        this.prefill.set(prefill);
        this.communities.set(communities);
        this.bloodGroups.set(bloodGroups);
        this.docVerification.set(verification);
        this.docsNotVerified.set(!verification.allVerified);

        // Sort years newest-first for the dropdown
        const sorted = [...years].sort((a, b) =>
          new Date(b.startDate).getTime() - new Date(a.startDate).getTime()
        );
        this.academicYears.set(sorted);

        // Auto-select the academic year that matches the suggested year
        const match = sorted.find(y =>
          new Date(y.startDate).getFullYear() === prefill.suggestedAcademicYearFrom
        ) ?? sorted.find(y => y.isCurrent) ?? sorted[0];
        if (match) {
          this.selectedAcademicYearId.set(match.id);
          this.form.patchValue({
            academicYearFrom: new Date(match.startDate).getFullYear(),
            academicYearTo:   new Date(match.endDate).getFullYear(),
          });
        }

        this.form.patchValue({
          firstName:    prefill.firstName,
          lastName:     prefill.lastName,
          email:        prefill.email ?? '',
          phone:        prefill.phone ?? '',
          dateOfBirth:  prefill.dateOfBirth ?? '',
          gender:       prefill.gender ?? '',
          admissionDate:   new Date().toISOString().split('T')[0],
          applicationDate: prefill.suggestedApplicationDate,
          declarationDate: prefill.suggestedApplicationDate,
          address: {
            country:  prefill.countryId ?? null,
            state:    prefill.state ?? '',
            district: prefill.district ?? '',
          },
        });

        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load admission data');
        this.loading.set(false);
      },
    });
  }

  protected onAcademicYearSelect(event: Event): void {
    const id = Number((event.target as HTMLSelectElement).value);
    const year = this.academicYears().find(y => y.id === id);
    if (!year) return;
    this.selectedAcademicYearId.set(id);
    this.form.patchValue({
      academicYearFrom: new Date(year.startDate).getFullYear(),
      academicYearTo:   new Date(year.endDate).getFullYear(),
    });
  }

  protected onParentConsentFile(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    this.parentConsentFile.set(file);
  }

  protected onApplicantConsentFile(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    this.applicantConsentFile.set(file);
  }

  protected onSubmit(): void {
    if (this.form.invalid) { scrollToFirstInvalid(this.form); return; }
    if (this.enquiry()?.status !== 'DOCUMENTS_VERIFIED') {
      this.toast.warning('Complete Admission is allowed only for Documents Verified enquiries.');
      return;
    }
    if (this.docsNotVerified()) {
      this.toast.warning('All mandatory documents must be verified before completing admission.');
      return;
    }
    if (!this.selectedAcademicYearId()) {
      this.toast.warning('Please select an academic year before creating admission.');
      return;
    }
    const id = this.enquiry()?.id;
    if (!id) return;
    this.saving.set(true);

    this.enquiryService.convertEnquiry(id, this.buildRequest()).subscribe({
      next: (enquiry) => {
        this.uploadConsentDocs(id, enquiry);
      },
      error: (error: HttpErrorResponse) => {
        this.toast.error(this.getErrorMessage(error, 'Failed to create admission'));
        this.saving.set(false);
      },
    });
  }

  private uploadConsentDocs(enquiryId: number, enquiry: Enquiry): void {
    const uploads = [];
    if (this.parentConsentFile()) {
      uploads.push(
        this.enquiryService.uploadDocumentFile(enquiryId, 'SIGNED_AFFIDAVIT', this.parentConsentFile()!)
      );
    }
    if (this.applicantConsentFile()) {
      uploads.push(
        this.enquiryService.uploadDocumentFile(enquiryId, 'PROVISIONAL_CERTIFICATE', this.applicantConsentFile()!)
      );
    }

    const done = () => {
      this.toast.success('Admission created and student enrolled successfully');
      this.saving.set(false);
      this.showSuccess(enquiry);
    };

    if (uploads.length === 0) { done(); return; }

    forkJoin(uploads).subscribe({
      next: () => done(),
      error: () => {
        this.toast.warning('Admission created but consent document upload failed. You can re-upload from the enquiry documents screen.');
        this.saving.set(false);
        this.showSuccess(enquiry);
      },
    });
  }

  private showSuccess(enquiry: Enquiry): void {
    const v = this.form.value as Record<string, unknown>;
    const year = this.academicYears().find(y => y.id === this.selectedAcademicYearId());
    this.successState.set({
      admissionNumber: enquiry.admissionNumber ?? '',
      studentId: enquiry.convertedStudentId!,
      studentName: `${String(v['firstName'] ?? '')} ${String(v['lastName'] ?? '')}`.trim(),
      enquiry,
      academicYearName: year?.name ?? '',
    });
    const studentId = enquiry.convertedStudentId;
    if (!studentId) { this.printReady.set(true); return; }
    this.admissionService.getByStudent(studentId).subscribe({
      next: (admission) => {
        forkJoin({
          docs: this.admissionService.getDocuments(admission.id),
          checklist: this.admissionService.getDocumentChecklist(admission.id),
        }).subscribe({
          next: ({ docs, checklist }) => {
            this.admissionDocs.set(docs);
            this.admissionChecklist.set(checklist);
            this.printReady.set(true);
          },
          error: () => this.printReady.set(true),
        });
      },
      error: () => this.printReady.set(true),
    });
  }

  protected viewForm(): void {
    const data = this.buildFormData();
    if (data) viewAdmissionForm(data);
  }

  protected printForm(): void {
    const data = this.buildFormData();
    if (data) printAdmissionForm(data);
  }

  protected downloadForm(): void {
    const data = this.buildFormData();
    if (data) downloadAdmissionForm(data);
  }

  protected goToStudents(): void {
    void this.router.navigate(['/students']);
  }

  private buildFormData(): AdmissionFormData | null {
    const s = this.successState();
    if (!s) return null;
    const v = this.form.value as Record<string, unknown>;
    const addr = (v['address'] ?? {}) as Record<string, unknown>;
    const checklistDocuments = Object.entries(this.admissionChecklist()).map(([documentType, verificationStatus]) => ({
      documentType,
      verificationStatus,
    }));
    return {
      admissionNumber:   s.admissionNumber,
      applicationDate:   String(v['applicationDate'] ?? ''),
      admissionDate:     String(v['admissionDate'] ?? ''),
      academicYear:      s.academicYearName,
      programName:       s.enquiry.programName,
      courseName:        s.enquiry.courseName,
      yearOfStudy:       v['yearOfStudy'] as number ?? null,
      studentType:       s.enquiry.studentType ?? null,
      admissionQuota:    s.enquiry.admissionQuota ?? null,
      collegeLogo:       this.collegeLogo(),
      collegeName:       this.collegeName(),
      collegeTrustLine:  this.collegeTrustLine(),
      collegeAddress:    this.collegeAddress(),
      collegePhone:      this.collegePhone(),
      collegeEmail:      this.collegeEmail(),
      studentName:       s.studentName,
      dateOfBirth:       v['dateOfBirth'] as string | null ?? null,
      gender:            v['gender'] as string | null ?? null,
      bloodGroup:        v['bloodGroup'] as string | null ?? null,
      physicalDisability: v['physicalDisability'] as boolean | null ?? null,
      aadharNumber:      v['aadharNumber'] as string | null ?? null,
      nationality:       v['nationality'] as string | null ?? null,
      religion:          v['religion'] as string | null ?? null,
      communityCategory: v['communityCategory'] as string | null ?? null,
      caste:             v['caste'] as string | null ?? null,
      phone:             v['phone'] as string | null ?? null,
      email:             v['email'] as string | null ?? null,
      postalAddress:     addr['postalAddress'] as string | null ?? null,
      street:            addr['street'] as string | null ?? null,
      city:              addr['city'] as string | null ?? null,
      district:          addr['district'] as string | null ?? null,
      state:             addr['state'] as string | null ?? null,
      pincode:           addr['pincode'] as string | null ?? null,
      fatherName:        v['fatherName'] as string | null ?? null,
      fatherPhone:       v['fatherPhone'] as string | null ?? null,
      fatherEmail:       v['fatherEmail'] as string | null ?? null,
      motherName:        v['motherName'] as string | null ?? null,
      motherPhone:       v['motherPhone'] as string | null ?? null,
      motherEmail:       v['motherEmail'] as string | null ?? null,
      qualifications:    [],
      documents:         (checklistDocuments.length ? checklistDocuments : this.admissionDocs()).map(d => ({
        documentType:        d.documentType,
        verificationStatus:  d.verificationStatus,
      })),
      declarationPlace:      v['declarationPlace'] as string | null ?? null,
      declarationDate:       v['declarationDate'] as string | null ?? null,
      parentConsentGiven:    v['parentConsentGiven'] as boolean | null ?? null,
      applicantConsentGiven: v['applicantConsentGiven'] as boolean | null ?? null,
    };
  }

  private nullable<T>(value: T): T | undefined {
    if (value === '' || value === null) return undefined;
    return value;
  }

  private buildRequest(): EnquiryConversionRequest {
    const v = this.form.value as Record<string, unknown> & { address?: Record<string, unknown> };
    const addr = v['address'] ?? {};
    const hasAddress = Object.values(addr).some((x) => x !== '' && x !== null && x !== undefined);

    return {
      firstName:   v['firstName'] as string,
      lastName:    v['lastName'] as string,
      email:       v['email'] as string,
      phone:       this.nullable(v['phone'] as string),
      semester:    v['yearOfStudy'] as number,
      admissionDate:        v['admissionDate'] as string,
      joiningAcademicYearId: this.selectedAcademicYearId()!,
      applicationDate:      v['applicationDate'] as string,
      parentConsentGiven:    v['parentConsentGiven'] as boolean,
      applicantConsentGiven: v['applicantConsentGiven'] as boolean,

      dateOfBirth:        this.nullable(v['dateOfBirth'] as string) ?? null,
      gender:             (this.nullable(v['gender']) as EnquiryConversionRequest['gender']) ?? null,
      aadharNumber:       this.nullable(v['aadharNumber'] as string) ?? null,
      nationality:        this.nullable(v['nationality'] as string) ?? null,
      religion:           this.nullable(v['religion'] as string) ?? null,
      communityCategory:  this.nullable(v['communityCategory'] as string) ?? null,
      caste:              this.nullable(v['caste'] as string) ?? null,
      bloodGroup:         this.nullable(v['bloodGroup'] as string) ?? null,
      physicalDisability: Boolean(v['physicalDisability']),
      fatherName:         this.nullable(v['fatherName'] as string) ?? null,
      fatherPhone:        this.nullable(v['fatherPhone'] as string) ?? null,
      fatherEmail:        this.nullable(v['fatherEmail'] as string) ?? null,
      motherName:         this.nullable(v['motherName'] as string) ?? null,
      motherPhone:        this.nullable(v['motherPhone'] as string) ?? null,
      motherEmail:        this.nullable(v['motherEmail'] as string) ?? null,

      address: hasAddress ? {
        countryId:     (addr['country'] as number | null) ?? null,
        postalAddress: this.nullable(addr['postalAddress'] as string) ?? null,
        street:        this.nullable(addr['street'] as string) ?? null,
        city:          this.nullable(addr['city'] as string) ?? null,
        district:      this.nullable(addr['district'] as string) ?? null,
        state:         this.nullable(addr['state'] as string) ?? null,
        pincode:       this.nullable(addr['pincode'] as string) ?? null,
      } : null,

      declarationPlace: this.nullable(v['declarationPlace'] as string) ?? null,
      declarationDate:  this.nullable(v['declarationDate'] as string) ?? null,
    };
  }

  private getErrorMessage(error: HttpErrorResponse, fallback: string): string {
    if (typeof error.error === 'string' && error.error.trim()) return error.error;
    if (error.error?.message) return error.error.message;
    if (error.error?.error) return error.error.error;
    return fallback;
  }
}

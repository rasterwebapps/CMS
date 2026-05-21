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
  private readonly route           = inject(ActivatedRoute);
  private readonly router          = inject(Router);
  private readonly fb              = inject(FormBuilder);
  private readonly enquiryService  = inject(EnquiryService);
  private readonly academicYearSvc = inject(AcademicYearService);
  private readonly toast           = inject(ToastService);
  private readonly tourService     = inject(TourService);

  protected readonly computeInitials = computeInitials;

  private readonly communityService = inject(CommunityService);
  private readonly bloodGroupService = inject(BloodGroupService);

  protected readonly enquiry          = signal<Enquiry | null>(null);
  protected readonly prefill          = signal<EnquiryConversionPrefillResponse | null>(null);
  protected readonly academicYears    = signal<AcademicYear[]>([]);
  protected readonly communities      = signal<Community[]>([]);
  protected readonly bloodGroups      = signal<BloodGroup[]>([]);
  protected readonly selectedAcademicYearId = signal<number | null>(null);
  protected readonly loading          = signal(true);
  protected readonly saving           = signal(false);

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
    phone:        [''],
    admissionDate: ['', Validators.required],

    applicationDate: ['', Validators.required],

    // Always 1 for new admissions — hidden from UI
    yearOfStudy: [1, [Validators.required, Validators.min(1)]],

    dateOfBirth:       [''],
    gender:            [''],
    aadharNumber:      [''],
    nationality:       [''],
    religion:          [''],
    communityCategory: [''],
    caste:             [''],
    bloodGroup:        [''],
    fatherName:        [''],
    fatherPhone:       [''],
    fatherEmail:       ['', Validators.email],
    motherName:        [''],
    motherPhone:       [''],
    motherEmail:       ['', Validators.email],
    address: this.fb.group({
      country:       [null as number | null],
      postalAddress: [''],
      street:        [''],
      city:          [''],
      district:      [''],
      state:         [''],
      pincode:       [''],
    }),

    declarationPlace: [''],
    declarationDate:  [''],
    parentConsentGiven:    [false],
    applicantConsentGiven: [false],
  });

  /** Expose the nested address FormGroup for cms-state-district-selector */
  get addressForm(): FormGroup {
    return this.form.get('address') as FormGroup;
  }

  ngOnInit(): void {
    this.tourService.register('enquiry-convert', ENQUIRY_CONVERT_TOUR);
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) this.load(id);
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
      next: () => {
        this.uploadConsentDocs(id);
      },
      error: (error: HttpErrorResponse) => {
        this.toast.error(this.getErrorMessage(error, 'Failed to create admission'));
        this.saving.set(false);
      },
    });
  }

  private uploadConsentDocs(enquiryId: number): void {
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
      void this.router.navigate(['/students']);
    };

    if (uploads.length === 0) { done(); return; }

    forkJoin(uploads).subscribe({
      next: () => done(),
      error: () => {
        // Admission was already created — just warn about the upload failure
        this.toast.warning('Admission created but consent document upload failed. You can re-upload from the enquiry documents screen.');
        void this.router.navigate(['/students']);
      },
    });
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

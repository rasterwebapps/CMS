import { Component, computed, inject, OnInit, signal, AfterViewInit, OnDestroy, DestroyRef } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin, EMPTY } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatIconModule } from '@angular/material/icon';
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
import { IndiaLocationService } from '../../india-location/india-location.service';
import { Country, IndiaState, IndiaDistrict } from '../../india-location/india-location.model';
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

const MAX_CONSENT_UPLOAD_BYTES = 10 * 1024 * 1024;
const ALLOWED_UPLOAD_MIME_TYPES = new Set([
  'application/pdf',
  'image/jpeg',
  'image/png',
]);
const ALLOWED_UPLOAD_EXTENSIONS = new Set(['pdf', 'jpg', 'jpeg', 'png']);

// Step definitions
const CONV_STEPS = [
  { label: 'Student Details', description: 'Name, contact & dates'     },
  { label: 'Academic Year',   description: 'Year & program'            },
  { label: 'Personal Info',   description: 'DOB, gender & Aadhar'      },
  { label: 'Demographics',    description: 'Nationality & community'   },
  { label: 'Family',          description: 'Parents & contacts'        },
  { label: 'Address',         description: 'Residential address'       },
  { label: 'Declaration',     description: 'Consent & signatures'      },
  { label: 'Documents',       description: 'Uploads (optional)'        },
] as const;

// Form fields required for each section (for validation and completion tracking)
const CONV_STEP_FIELDS: Record<number, string[]> = {
  0: ['firstName', 'lastName', 'email', 'phone', 'admissionDate', 'applicationDate'],
  1: [], // academic year uses selectedAcademicYearId signal
  2: ['dateOfBirth', 'gender', 'aadharNumber'],
  3: ['nationality', 'religion', 'communityCategory', 'caste', 'bloodGroup'],
  4: ['fatherName', 'fatherPhone', 'fatherEmail', 'motherName', 'motherPhone', 'motherEmail'],
  5: ['address.postalAddress', 'address.street', 'address.city',
      'address.country', 'address.state', 'address.district'],
  6: ['declarationPlace', 'declarationDate', 'parentConsentGiven', 'applicantConsentGiven'],
  7: [], // consent documents are optional
};

@Component({
  selector: 'app-enquiry-convert',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatIconModule,
    InrPipe,
    CmsTourButtonComponent,
  ],
  templateUrl: './enquiry-convert.component.html',
  styleUrl: './enquiry-convert.component.scss',
})
export class EnquiryConvertComponent implements OnInit, AfterViewInit, OnDestroy {
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
  private readonly locationService   = inject(IndiaLocationService);
  private readonly destroyRef        = inject(DestroyRef);

  // ── Address cascade signals ───────────────────────────────────────────────
  protected readonly addrCountries       = signal<Country[]>([]);
  protected readonly addrStates          = signal<IndiaState[]>([]);
  protected readonly addrDistricts       = signal<IndiaDistrict[]>([]);
  protected readonly addrLoadingCountries = signal(false);
  protected readonly addrLoadingStates   = signal(false);
  protected readonly addrLoadingDistricts = signal(false);

  // ── Stepper state ────────────────────────────────────────────────────────
  protected readonly steps           = CONV_STEPS;
  protected readonly currentStep     = signal(0);
  protected readonly academicYearTouched = signal(false);

  // Bumped by form.valueChanges / statusChanges subscriptions to keep computeds fresh
  private readonly formVersion       = signal(0);

  protected readonly isStepComplete = computed(() => {
    this.formVersion();
    return (i: number) => {
      const fields = CONV_STEP_FIELDS[i] ?? [];
      if (i === 1) return !!this.selectedAcademicYearId();
      if (fields.length === 0) return true; // documents step is always optional/complete
      return fields.every(key => {
        const ctrl = this.form.get(key);
        return ctrl ? ctrl.valid : true;
      });
    };
  });

  protected readonly isStepError = computed(() => {
    this.formVersion();
    return (i: number) => {
      const fields = CONV_STEP_FIELDS[i] ?? [];
      if (i === 1) return this.academicYearTouched() && !this.selectedAcademicYearId();
      return fields.some(key => {
        const ctrl = this.form.get(key);
        return ctrl ? ctrl.invalid && ctrl.touched : false;
      });
    };
  });

  protected readonly completedCount = computed(() => {
    this.formVersion();
    const check = this.isStepComplete();
    return this.steps.filter((_, i) => check(i)).length;
  });

  // ── Scroll-spy (rAF-throttled scroll listener) ────────────────────────────
  private scrollContainer: Element | null = null;
  private scrollHandler: (() => void) | null = null;
  private rafId: number | null = null;

  protected scrollToSection(index: number): void {
    const section = document.getElementById(`conv-section-${index}`);
    const container = this.scrollContainer ?? document.querySelector('main.app-content');
    if (!section || !container) { this.currentStep.set(index); return; }

    const containerRect = container.getBoundingClientRect();
    const sectionRect   = section.getBoundingClientRect();
    const targetTop     = container.scrollTop + (sectionRect.top - containerRect.top) - 16;
    container.scrollTo({ top: Math.max(0, targetTop), behavior: 'smooth' });
    this.currentStep.set(index);
  }

  private setupScrollSpy(): void {
    this.scrollContainer = document.querySelector('main.app-content');
    if (!this.scrollContainer) return;

    this.updateActiveSection();

    const onScroll = () => {
      if (this.rafId !== null) return;
      this.rafId = requestAnimationFrame(() => {
        this.rafId = null;
        this.updateActiveSection();
      });
    };

    this.scrollHandler = onScroll;
    this.scrollContainer.addEventListener('scroll', onScroll, { passive: true });
  }

  private updateActiveSection(): void {
    const container = this.scrollContainer;
    if (!container) return;

    const containerRect  = container.getBoundingClientRect();
    const triggerY       = containerRect.top + containerRect.height * 0.75;

    let active = 0;
    for (let i = 0; i < this.steps.length; i++) {
      const el = document.getElementById(`conv-section-${i}`);
      if (!el) continue;
      if (el.getBoundingClientRect().top <= triggerY) {
        active = i;
      } else {
        break;
      }
    }
    this.currentStep.set(active);
  }

  // ── Data ─────────────────────────────────────────────────────────────────
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
  protected readonly admissionChecklist    = signal<{ mandatory: Record<string, string>; optional: Record<string, string> }>({ mandatory: {}, optional: {} });
  protected readonly printReady            = signal(false);
  protected readonly seatWarning           = signal<string | null>(null);
  protected readonly seatWarningSoft       = signal(false);

  private readonly collegeLogo      = signal<string | null>(null);
  private readonly collegeName      = signal<string | null>(null);
  private readonly collegeTrustLine = signal<string | null>(null);
  private readonly collegeAddress   = signal<string | null>(null);
  private readonly collegePhone     = signal<string | null>(null);
  private readonly collegeEmail     = signal<string | null>(null);

  protected readonly docVerification  = signal<DocumentVerificationStatus | null>(null);
  protected readonly docsNotVerified  = signal(false);

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

  get addressForm(): FormGroup {
    return this.form.get('address') as FormGroup;
  }

  ngOnInit(): void {
    this.tourService.register('enquiry-convert', ENQUIRY_CONVERT_TOUR);
    this.initAddressCascade();
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) this.load(id);
    this.loadBranding();

    // Keep reactive completion/error indicators in sync with form state
    this.form.valueChanges.subscribe(() => this.formVersion.update(v => v + 1));
    this.form.statusChanges.subscribe(() => this.formVersion.update(v => v + 1));
  }

  ngAfterViewInit(): void {
    // Scroll spy is activated after data loads (see load() success handler)
  }

  ngOnDestroy(): void {
    if (this.scrollContainer && this.scrollHandler) {
      this.scrollContainer.removeEventListener('scroll', this.scrollHandler);
    }
    if (this.rafId !== null) cancelAnimationFrame(this.rafId);
  }

  private initAddressCascade(): void {
    this.addrLoadingCountries.set(true);
    this.locationService.getCountries(true).pipe(
      takeUntilDestroyed(this.destroyRef),
      catchError(() => { this.addrLoadingCountries.set(false); return EMPTY; }),
    ).subscribe(countries => {
      this.addrCountries.set(countries);
      this.addrLoadingCountries.set(false);
      const ctrl = this.form.get('address.country');
      if (ctrl && !ctrl.value) {
        const india = countries.find(c => c.isoCode === 'IN');
        if (india) ctrl.setValue(india.id, { emitEvent: true });
      }
    });

    const countryCtrl = this.form.get('address.country')!;
    if (countryCtrl.value) this.loadAddrStatesForCountry(countryCtrl.value as number);
    countryCtrl.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(id => {
      this.form.get('address.state')!.setValue('');
      this.form.get('address.district')!.setValue('');
      this.addrStates.set([]);
      this.addrDistricts.set([]);
      if (id) this.loadAddrStatesForCountry(id as number);
    });

    this.form.get('address.state')!.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(name => {
      this.form.get('address.district')!.setValue('');
      this.addrDistricts.set([]);
      if (name) this.loadAddrDistrictsForStateName(name as string);
    });
  }

  private loadAddrStatesForCountry(countryId: number): void {
    this.addrLoadingStates.set(true);
    this.locationService.getStatesByCountry(countryId, true).pipe(
      takeUntilDestroyed(this.destroyRef),
      catchError(() => { this.addrLoadingStates.set(false); return EMPTY; }),
    ).subscribe(states => {
      this.addrStates.set(states);
      this.addrLoadingStates.set(false);
      const stateCtrl = this.form.get('address.state')!;
      if (stateCtrl.value) {
        this.loadAddrDistrictsForStateName(stateCtrl.value as string);
      } else {
        const match = states.find(s => s.name.toLowerCase() === 'tamil nadu');
        if (match) stateCtrl.setValue(match.name, { emitEvent: true });
      }
    });
  }

  private loadAddrDistrictsForStateName(stateName: string): void {
    const state = this.addrStates().find(s => s.name.toLowerCase() === stateName.toLowerCase());
    if (!state) return;
    this.addrLoadingDistricts.set(true);
    this.locationService.getDistricts(state.id, true).pipe(
      takeUntilDestroyed(this.destroyRef),
      catchError(() => { this.addrLoadingDistricts.set(false); return EMPTY; }),
    ).subscribe(districts => {
      this.addrDistricts.set(districts);
      this.addrLoadingDistricts.set(false);
      const districtCtrl = this.form.get('address.district')!;
      if (!districtCtrl.value) {
        const match = districts.find(d => d.name.toLowerCase() === 'salem');
        if (match) districtCtrl.setValue(match.name);
      }
    });
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

        const sorted = [...years].sort((a, b) =>
          new Date(b.startDate).getTime() - new Date(a.startDate).getTime()
        );
        this.academicYears.set(sorted);

        const match = sorted.find(y =>
          new Date(y.startDate).getFullYear() === prefill.suggestedAcademicYearFrom
        ) ?? sorted.find(y => y.isCurrent) ?? sorted[0];
        if (match) {
          this.selectedAcademicYearId.set(match.id);
          this.form.patchValue({
            academicYearFrom: new Date(match.startDate).getFullYear(),
            academicYearTo:   new Date(match.endDate).getFullYear(),
          });
          this.checkSeatAvailability(match.id);
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

        // Activate scroll-spy once the form sections are in the DOM
        setTimeout(() => this.setupScrollSpy(), 100);
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
    this.checkSeatAvailability(id);
  }

  private checkSeatAvailability(academicYearId: number): void {
    const enquiry = this.enquiry();
    if (!enquiry?.courseId || !enquiry?.admissionQuota) return;
    this.academicYearSvc.getSeatAvailability(enquiry.courseId, academicYearId, enquiry.admissionQuota).subscribe({
      next: (status) => {
        if (status.closed) {
          const label = enquiry.admissionQuota === 'MANAGEMENT' ? 'Management' : 'Counselling (Govt.)';
          this.seatWarning.set(`${label} quota is closed for this cohort. Contact admin to reopen before completing this admission.`);
          this.seatWarningSoft.set(false);
        } else if (status.full) {
          const label = enquiry.admissionQuota === 'MANAGEMENT' ? 'Management' : 'Counselling (Govt.)';
          this.seatWarning.set(`${label} seats are exhausted (${status.filled}/${status.total}). Contact admin to increase the seat allocation.`);
          this.seatWarningSoft.set(false);
        } else if (enquiry.admissionQuota === 'MANAGEMENT' && status.overManagementQuota) {
          this.seatWarning.set(
            `Management allocation exceeded (${status.filled}/${status.total ?? '∞'} seats). ` +
            `This admission is beyond the management quota limit — proceed only if authorised.`
          );
          this.seatWarningSoft.set(true);
        } else {
          this.seatWarning.set(null);
          this.seatWarningSoft.set(false);
        }
      },
      error: () => { /* non-blocking */ },
    });
  }

  protected onParentConsentFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    if (!file) {
      this.parentConsentFile.set(null);
      return;
    }
    if (!this.isSupportedConsentFile(file)) {
      this.toast.warning('Only PDF, JPG, PNG files are allowed (max 10 MB)');
      input.value = '';
      this.parentConsentFile.set(null);
      return;
    }
    this.parentConsentFile.set(file);
  }

  protected onApplicantConsentFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    if (!file) {
      this.applicantConsentFile.set(null);
      return;
    }
    if (!this.isSupportedConsentFile(file)) {
      this.toast.warning('Only PDF, JPG, PNG files are allowed (max 10 MB)');
      input.value = '';
      this.applicantConsentFile.set(null);
      return;
    }
    this.applicantConsentFile.set(file);
  }

  private isSupportedConsentFile(file: File): boolean {
    if (file.size > MAX_CONSENT_UPLOAD_BYTES) {
      return false;
    }
    const extension = file.name.includes('.')
      ? file.name.split('.').pop()?.toLowerCase() ?? ''
      : '';
    const hasAllowedMime = ALLOWED_UPLOAD_MIME_TYPES.has(file.type.toLowerCase());
    const hasAllowedExtension = ALLOWED_UPLOAD_EXTENSIONS.has(extension);
    return hasAllowedMime || hasAllowedExtension;
  }

  protected onSubmit(): void {
    // Touch academic year signal so its error shows in the stepper
    this.academicYearTouched.set(true);
    this.formVersion.update(v => v + 1);

    if (this.enquiry()?.status !== 'DOCUMENTS_VERIFIED') {
      this.toast.warning('Complete Admission is allowed only for Documents Verified enquiries.');
      return;
    }
    if (this.docsNotVerified()) {
      this.toast.warning('All mandatory documents must be verified before completing admission.');
      return;
    }

    // Validate the whole form at once — scrolls to and focuses first invalid field
    if (this.form.invalid) { scrollToFirstInvalid(this.form); return; }

    if (!this.selectedAcademicYearId()) {
      this.scrollToSection(1);
      return;
    }

    const id = this.enquiry()?.id;
    if (!id) return;
    this.saving.set(true);

    this.enquiryService.convertEnquiry(id, this.buildRequest()).subscribe({
      next: (enquiry) => { this.uploadConsentDocs(id, enquiry); },
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
      error: (err) => {
        this.toast.warning(err?.error?.message ?? 'Admission created but consent document upload failed. You can re-upload from the enquiry documents screen.');
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
    const cl = this.admissionChecklist();
    const checklistDocuments = [
      ...Object.entries(cl.mandatory),
      ...Object.entries(cl.optional),
    ].map(([documentType, verificationStatus]) => ({
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

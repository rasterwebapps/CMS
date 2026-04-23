import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { EnquiryService } from '../enquiry.service';
import { Enquiry, EnquiryConversionPrefillResponse, EnquiryConversionRequest } from '../enquiry.model';
import { LayoutService } from '../../../core/layout/layout.service';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { ToastService } from '../../../core/toast/toast.service';

@Component({
  selector: 'app-enquiry-convert',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    PageHeaderComponent],
  templateUrl: './enquiry-convert.component.html',
  styleUrl: './enquiry-convert.component.scss',
})
export class EnquiryConvertComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly enquiryService = inject(EnquiryService);
  private readonly toast = inject(ToastService);
  protected readonly layoutService = inject(LayoutService);

  protected readonly enquiry = signal<Enquiry | null>(null);
  protected readonly prefill = signal<EnquiryConversionPrefillResponse | null>(null);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);

  protected readonly genderOptions = ['MALE', 'FEMALE', 'OTHER'] as const;
  protected readonly communityOptions = ['SC', 'ST', 'BC', 'MBC', 'DNC', 'OC', 'OTHERS'] as const;
  protected readonly bloodGroupOptions = [
    'A_POSITIVE', 'A_NEGATIVE',
    'B_POSITIVE', 'B_NEGATIVE',
    'O_POSITIVE', 'O_NEGATIVE',
    'AB_POSITIVE', 'AB_NEGATIVE',
  ] as const;

  protected readonly form: FormGroup = this.fb.group({
    // Student basic
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    phone: [''],
    semester: [1, [Validators.required, Validators.min(1)]],
    admissionDate: ['', Validators.required],

    // Admission basic
    academicYearFrom: [null, Validators.required],
    academicYearTo: [null, Validators.required],
    applicationDate: ['', Validators.required],

    // Student personal information
    dateOfBirth: [''],
    gender: [''],
    aadharNumber: [''],

    // Student demographics
    nationality: [''],
    religion: [''],
    communityCategory: [''],
    caste: [''],
    bloodGroup: [''],

    // Student family information
    fatherName: [''],
    motherName: [''],
    parentMobile: [''],

    // Student address
    address: this.fb.group({
      postalAddress: [''],
      street: [''],
      city: [''],
      district: [''],
      state: [''],
      pincode: [''],
    }),

    // Admission declaration
    declarationPlace: [''],
    declarationDate: [''],

    // Consents
    parentConsentGiven: [false],
    applicantConsentGiven: [false],
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) this.load(id);
  }

  private load(id: number): void {
    this.enquiryService.getEnquiryById(id).subscribe({ next: (e) => this.enquiry.set(e) });
    this.enquiryService.getConversionPrefill(id).subscribe({
      next: (p) => {
        this.prefill.set(p);
        this.form.patchValue({
          firstName: p.firstName,
          lastName: p.lastName,
          email: p.email ?? '',
          phone: p.phone ?? '',
          semester: p.suggestedSemester,
          admissionDate: new Date().toISOString().split('T')[0],
          academicYearFrom: p.suggestedAcademicYearFrom,
          academicYearTo: p.suggestedAcademicYearTo,
          applicationDate: p.suggestedApplicationDate,
          declarationDate: p.suggestedApplicationDate,
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load prefill data');
        this.loading.set(false);
      },
    });
  }

  protected onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const id = this.enquiry()?.id;
    if (!id) return;
    this.saving.set(true);
    const request = this.buildRequest();
    this.enquiryService.convertEnquiry(id, request).subscribe({
      next: () => {
        this.toast.success('Admission created and student enrolled successfully');
        void this.router.navigate(['/students']);
      },
      error: () => {
        this.toast.error('Failed to create admission');
        this.saving.set(false);
      },
    });
  }

  /** Convert empty strings to undefined so the backend treats them as null/optional. */
  private nullable<T>(value: T): T | undefined {
    if (value === '' || value === null) return undefined;
    return value;
  }

  private buildRequest(): EnquiryConversionRequest {
    const v = this.form.value as Record<string, unknown> & {
      address?: Record<string, unknown>;
    };
    const addr = v.address ?? {};
    const hasAddress = Object.values(addr).some((x) => x !== '' && x !== null && x !== undefined);

    return {
      firstName: v['firstName'] as string,
      lastName: v['lastName'] as string,
      email: v['email'] as string,
      phone: this.nullable(v['phone'] as string),
      semester: v['semester'] as number,
      admissionDate: v['admissionDate'] as string,
      academicYearFrom: v['academicYearFrom'] as number,
      academicYearTo: v['academicYearTo'] as number,
      applicationDate: v['applicationDate'] as string,
      parentConsentGiven: v['parentConsentGiven'] as boolean,
      applicantConsentGiven: v['applicantConsentGiven'] as boolean,

      dateOfBirth: this.nullable(v['dateOfBirth'] as string) ?? null,
      gender: (this.nullable(v['gender']) as EnquiryConversionRequest['gender']) ?? null,
      aadharNumber: this.nullable(v['aadharNumber'] as string) ?? null,

      nationality: this.nullable(v['nationality'] as string) ?? null,
      religion: this.nullable(v['religion'] as string) ?? null,
      communityCategory:
        (this.nullable(v['communityCategory']) as EnquiryConversionRequest['communityCategory']) ?? null,
      caste: this.nullable(v['caste'] as string) ?? null,
      bloodGroup:
        (this.nullable(v['bloodGroup']) as EnquiryConversionRequest['bloodGroup']) ?? null,

      fatherName: this.nullable(v['fatherName'] as string) ?? null,
      motherName: this.nullable(v['motherName'] as string) ?? null,
      parentMobile: this.nullable(v['parentMobile'] as string) ?? null,

      address: hasAddress
        ? {
            postalAddress: this.nullable(addr['postalAddress'] as string) ?? null,
            street: this.nullable(addr['street'] as string) ?? null,
            city: this.nullable(addr['city'] as string) ?? null,
            district: this.nullable(addr['district'] as string) ?? null,
            state: this.nullable(addr['state'] as string) ?? null,
            pincode: this.nullable(addr['pincode'] as string) ?? null,
          }
        : null,

      declarationPlace: this.nullable(v['declarationPlace'] as string) ?? null,
      declarationDate: this.nullable(v['declarationDate'] as string) ?? null,
    };
  }
}


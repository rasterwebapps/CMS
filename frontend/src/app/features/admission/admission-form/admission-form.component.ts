import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CurrencyPipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AdmissionService } from '../admission.service';
import { ADMISSION_STATUSES, AdmissionRequest, QUALIFICATION_TYPES } from '../admission.model';
import { StudentService } from '../../student/student.service';
import { Student } from '../../student/student.model';
import { EnquiryService } from '../../enquiry/enquiry.service';
import { Enquiry, EnquiryConversionPrefillResponse, EnquiryConversionRequest } from '../../enquiry/enquiry.model';
import { LayoutService } from '../../../core/layout/layout.service';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { ToastService } from '../../../core/toast/toast.service';

type Mode = 'from-enquiry' | 'manual';

@Component({
  selector: 'app-admission-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    CurrencyPipe,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
    MatTableModule,
    MatProgressSpinnerModule,
    PageHeaderComponent],
  templateUrl: './admission-form.component.html',
  styleUrl: './admission-form.component.scss',
})
export class AdmissionFormComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly admissionService = inject(AdmissionService);
  private readonly studentService = inject(StudentService);
  private readonly enquiryService = inject(EnquiryService);
  private readonly toast = inject(ToastService);
  protected readonly layoutService = inject(LayoutService);

  protected readonly students = signal<Student[]>([]);
  protected readonly pendingEnquiries = signal<Enquiry[]>([]);
  protected readonly selectedEnquiry = signal<Enquiry | null>(null);
  protected readonly prefill = signal<EnquiryConversionPrefillResponse | null>(null);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEdit = signal(false);
  protected readonly mode = signal<Mode>('from-enquiry');

  protected readonly statuses = ADMISSION_STATUSES;
  protected readonly qualificationTypes = QUALIFICATION_TYPES;
  protected readonly genderOptions = ['MALE', 'FEMALE', 'OTHER'] as const;
  protected readonly communityOptions = ['SC', 'ST', 'BC', 'MBC', 'DNC', 'OC', 'OTHERS'] as const;
  protected readonly bloodGroupOptions = [
    'A_POSITIVE', 'A_NEGATIVE',
    'B_POSITIVE', 'B_NEGATIVE',
    'O_POSITIVE', 'O_NEGATIVE',
    'AB_POSITIVE', 'AB_NEGATIVE',
  ] as const;

  protected readonly qualColumns = ['qualificationType', 'schoolName', 'percentage', 'monthAndYearOfPassing', 'actions'];

  protected readonly form: FormGroup = this.fb.group({
    // ── Manual / Edit mode ──────────────────────────────────────────────────
    studentId: [null],
    status: ['SUBMITTED'],
    qualifications: this.fb.array([]),

    // ── From-Enquiry mode ───────────────────────────────────────────────────
    enquiryId: [null],
    firstName: [''],
    lastName: [''],
    email: [''],
    phone: [''],
    semester: [1],
    admissionDate: [''],
    dateOfBirth: [''],
    gender: [''],
    aadharNumber: [''],
    nationality: [''],
    religion: [''],
    communityCategory: [''],
    caste: [''],
    bloodGroup: [''],
    fatherName: [''],
    motherName: [''],
    parentMobile: [''],
    address: this.fb.group({
      postalAddress: [''],
      street: [''],
      city: [''],
      district: [''],
      state: [''],
      pincode: [''],
    }),

    // ── Common (both modes) ─────────────────────────────────────────────────
    academicYearFrom: [new Date().getFullYear(), Validators.required],
    academicYearTo: [new Date().getFullYear() + 1, Validators.required],
    applicationDate: [new Date().toISOString().split('T')[0], Validators.required],
    declarationPlace: [''],
    declarationDate: [''],
    parentConsentGiven: [false],
    applicantConsentGiven: [false],
  });

  get qualifications(): FormArray {
    return this.form.get('qualifications') as FormArray;
  }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.isEdit.set(true);
      this.loading.set(true);
      // Edit mode always uses the manual flow; load students for the dropdown.
      this.studentService.getAll().subscribe({ next: (s) => this.students.set(s) });
      this.updateValidators('manual');
      this.admissionService.getById(id).subscribe({
        next: (a) => {
          this.form.patchValue(a);
          this.loading.set(false);
        },
        error: () => {
          this.toast.error('Failed to load admission');
          void this.router.navigate(['/admissions']);
        },
      });
    } else {
      // New admission: default to "from-enquiry" mode.
      this.updateValidators('from-enquiry');
      this.enquiryService.getAdmissionPending().subscribe({
        next: (list) => this.pendingEnquiries.set(list),
        error: () => this.toast.error('Failed to load pending enquiries'),
      });
      // Pre-load students in background so manual mode works without an extra call.
      this.studentService.getAll().subscribe({ next: (s) => this.students.set(s) });
    }
  }

  protected setMode(m: Mode): void {
    this.mode.set(m);
    this.updateValidators(m);
    // Reset mode-specific selection state when switching modes.
    this.selectedEnquiry.set(null);
    this.prefill.set(null);
    this.form.patchValue({ enquiryId: null, studentId: null });
  }

  protected onEnquiryChange(event: Event): void {
    const id = Number((event.target as HTMLSelectElement).value);
    if (!id) {
      this.selectedEnquiry.set(null);
      this.prefill.set(null);
      return;
    }
    const enq = this.pendingEnquiries().find((e) => e.id === id) ?? null;
    this.selectedEnquiry.set(enq);
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
      },
      error: () => this.toast.error('Failed to load enquiry prefill data'),
    });
  }

  protected addQualification(): void {
    this.qualifications.push(this.fb.group({
      qualificationType: ['', Validators.required],
      schoolName: [''],
      majorSubject: [''],
      totalMarks: [null],
      percentage: [null],
      monthAndYearOfPassing: [''],
      universityOrBoard: [''],
    }));
  }

  protected removeQualification(i: number): void {
    this.qualifications.removeAt(i);
  }

  protected getQualGroup(i: number): FormGroup {
    return this.qualifications.at(i) as FormGroup;
  }

  protected onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    if (!this.isEdit() && this.mode() === 'from-enquiry') {
      this.submitFromEnquiry();
    } else {
      this.submitManual();
    }
  }

  // ── Private helpers ───────────────────────────────────────────────────────

  private updateValidators(m: Mode): void {
    if (m === 'from-enquiry') {
      this.form.get('enquiryId')?.setValidators([Validators.required]);
      this.form.get('firstName')?.setValidators([Validators.required]);
      this.form.get('lastName')?.setValidators([Validators.required]);
      this.form.get('email')?.setValidators([Validators.required, Validators.email]);
      this.form.get('semester')?.setValidators([Validators.required, Validators.min(1)]);
      this.form.get('admissionDate')?.setValidators([Validators.required]);
      this.form.get('studentId')?.clearValidators();
    } else {
      this.form.get('enquiryId')?.clearValidators();
      this.form.get('firstName')?.clearValidators();
      this.form.get('lastName')?.clearValidators();
      this.form.get('email')?.clearValidators();
      this.form.get('semester')?.clearValidators();
      this.form.get('admissionDate')?.clearValidators();
      this.form.get('studentId')?.setValidators([Validators.required]);
    }
    ['enquiryId', 'firstName', 'lastName', 'email', 'semester', 'admissionDate', 'studentId'].forEach((ctrl) => {
      this.form.get(ctrl)?.updateValueAndValidity({ emitEvent: false });
    });
  }

  private submitFromEnquiry(): void {
    const enquiryId = this.form.value['enquiryId'] as number;
    const request = this.buildConversionRequest();
    this.enquiryService.convertEnquiry(enquiryId, request).subscribe({
      next: () => {
        this.toast.success('Admission created and student enrolled successfully');
        void this.router.navigate(['/admissions']);
      },
      error: () => {
        this.toast.error('Failed to create admission');
        this.saving.set(false);
      },
    });
  }

  private submitManual(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    // Strip out from-enquiry-only fields before sending to the admission API.
    const {
      qualifications,
      enquiryId: _eq, firstName: _fn, lastName: _ln, email: _em, phone: _ph,
      semester: _se, admissionDate: _ad, dateOfBirth: _dob, gender: _ge,
      aadharNumber: _aa, nationality: _na, religion: _re, communityCategory: _cc,
      caste: _ca, bloodGroup: _bg, fatherName: _fa, motherName: _mo, parentMobile: _pm,
      address: _addr,
      ...admissionData
    } = this.form.value as Record<string, unknown>;

    const save$ = this.isEdit()
      ? this.admissionService.update(id, admissionData as unknown as AdmissionRequest)
      : this.admissionService.create(admissionData as unknown as AdmissionRequest);

    save$.subscribe({
      next: (admission) => {
        const quals: unknown[] = (qualifications as unknown[]) ?? [];
        if (!this.isEdit() && quals.length > 0) {
          let remaining = quals.length;
          quals.forEach((q) => {
            this.admissionService.addQualification(admission.id, q as Parameters<AdmissionService['addQualification']>[1]).subscribe({
              next: () => { if (--remaining === 0) this.finish(); },
              error: () => { if (--remaining === 0) this.finish(); },
            });
          });
        } else {
          this.finish();
        }
      },
      error: () => {
        this.toast.error('Failed to save admission');
        this.saving.set(false);
      },
    });
  }

  private buildConversionRequest(): EnquiryConversionRequest {
    const v = this.form.value as Record<string, unknown> & { address?: Record<string, unknown> };
    const addr = (v['address'] as Record<string, unknown>) ?? {};
    const hasAddress = Object.values(addr).some((x) => x !== '' && x !== null && x !== undefined);
    const nullable = <T>(value: T): T | undefined => (value === '' || value === null ? undefined : value);

    return {
      firstName: v['firstName'] as string,
      lastName: v['lastName'] as string,
      email: v['email'] as string,
      phone: nullable(v['phone'] as string),
      semester: v['semester'] as number,
      admissionDate: v['admissionDate'] as string,
      academicYearFrom: v['academicYearFrom'] as number,
      academicYearTo: v['academicYearTo'] as number,
      applicationDate: v['applicationDate'] as string,
      parentConsentGiven: v['parentConsentGiven'] as boolean,
      applicantConsentGiven: v['applicantConsentGiven'] as boolean,
      dateOfBirth: nullable(v['dateOfBirth'] as string) ?? null,
      gender: (nullable(v['gender']) as EnquiryConversionRequest['gender']) ?? null,
      aadharNumber: nullable(v['aadharNumber'] as string) ?? null,
      nationality: nullable(v['nationality'] as string) ?? null,
      religion: nullable(v['religion'] as string) ?? null,
      communityCategory: (nullable(v['communityCategory']) as EnquiryConversionRequest['communityCategory']) ?? null,
      caste: nullable(v['caste'] as string) ?? null,
      bloodGroup: (nullable(v['bloodGroup']) as EnquiryConversionRequest['bloodGroup']) ?? null,
      fatherName: nullable(v['fatherName'] as string) ?? null,
      motherName: nullable(v['motherName'] as string) ?? null,
      parentMobile: nullable(v['parentMobile'] as string) ?? null,
      address: hasAddress
        ? {
            postalAddress: nullable(addr['postalAddress'] as string) ?? null,
            street: nullable(addr['street'] as string) ?? null,
            city: nullable(addr['city'] as string) ?? null,
            district: nullable(addr['district'] as string) ?? null,
            state: nullable(addr['state'] as string) ?? null,
            pincode: nullable(addr['pincode'] as string) ?? null,
          }
        : null,
      declarationPlace: nullable(v['declarationPlace'] as string) ?? null,
      declarationDate: nullable(v['declarationDate'] as string) ?? null,
    };
  }

  private finish(): void {
    this.toast.success('Admission saved successfully');
    void this.router.navigate(['/admissions']);
  }
}

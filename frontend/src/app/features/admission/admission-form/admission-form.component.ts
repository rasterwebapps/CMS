import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { AdmissionService } from '../admission.service';
import { AdmissionRequest, QUALIFICATION_TYPES } from '../admission.model';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear } from '../../academic-year/academic-year.model';
import { StudentService } from '../../student/student.service';
import { Student } from '../../student/student.model';
import { EnquiryService } from '../../enquiry/enquiry.service';
import { Enquiry, EnquiryConversionPrefillResponse, EnquiryConversionRequest } from '../../enquiry/enquiry.model';
import { CommunityService } from '../../community/community.service';
import { BloodGroupService } from '../../blood-group/blood-group.service';
import { Community } from '../../community/community.model';
import { BloodGroup } from '../../blood-group/blood-group.model';
import { LayoutService } from '../../../core/layout/layout.service';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { ADMISSION_FORM_TOUR } from '../../../shared/tour/tours/admission.tours';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';

type Mode = 'from-enquiry' | 'manual';

@Component({
  selector: 'app-admission-form',
  standalone: true,
  imports: [
    InrPipe,
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
    MatTableModule,
    MatProgressSpinnerModule,
    PageHeaderComponent,
    CmsTourButtonComponent],
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
  private readonly tourService = inject(TourService);

  private readonly academicYearSvc = inject(AcademicYearService);
  private readonly communityService = inject(CommunityService);
  private readonly bloodGroupService = inject(BloodGroupService);

  protected readonly students = signal<Student[]>([]);
  protected readonly pendingEnquiries = signal<Enquiry[]>([]);
  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly communities = signal<Community[]>([]);
  protected readonly bloodGroups = signal<BloodGroup[]>([]);
  protected readonly selectedAcademicYearId = signal<number | null>(null);
  protected readonly selectedEnquiry = signal<Enquiry | null>(null);
  protected readonly prefill = signal<EnquiryConversionPrefillResponse | null>(null);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEdit = signal(false);
  protected readonly mode = signal<Mode>('from-enquiry');
  protected readonly editStudentId = signal<number | null>(null);

  protected readonly qualificationTypes = QUALIFICATION_TYPES;
  protected readonly genderOptions = ['MALE', 'FEMALE', 'OTHER'] as const;

  private static readonly FROM_ENQUIRY_CONTROLS: ReadonlyArray<string> = [
    'enquiryId', 'firstName', 'lastName', 'email', 'yearOfStudy', 'admissionDate',
  ];
  private static readonly MANUAL_CONTROLS: ReadonlyArray<string> = ['studentId'];
  private static readonly MODE_TOGGLED_CONTROLS: ReadonlyArray<string> = [
    ...AdmissionFormComponent.FROM_ENQUIRY_CONTROLS,
    ...AdmissionFormComponent.MANUAL_CONTROLS,
  ];

  protected readonly qualColumns = ['qualificationType', 'schoolName', 'percentage', 'monthAndYearOfPassing', 'actions'];

  protected readonly form: FormGroup = this.fb.group({
    // ── Manual / Edit mode ──────────────────────────────────────────────────
    studentId: [null],
    qualifications: this.fb.array([]),

    // ── From-Enquiry mode ───────────────────────────────────────────────────
    enquiryId: [null],
    firstName: [''],
    lastName: [''],
    email: [''],
    phone: [''],
    yearOfStudy: [1],
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
    fatherPhone: [''],
    fatherEmail: ['', Validators.email],
    motherName: [''],
    motherPhone: [''],
    motherEmail: ['', Validators.email],
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
    joiningAcademicYearId: [null, Validators.required],
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
    this.tourService.register('admission-form', ADMISSION_FORM_TOUR);

    // Load communities and blood groups for demographic dropdowns
    this.communityService.getActiveCommunities().subscribe({
      next: (data) => this.communities.set(data),
    });
    this.bloodGroupService.getActiveBloodGroups().subscribe({
      next: (data) => this.bloodGroups.set(data),
    });

    // Always load academic years for the dropdown
    this.academicYearSvc.getAllAcademicYears().subscribe({
      next: (years) => {
        const sorted = [...years].sort((a, b) =>
          new Date(b.startDate).getTime() - new Date(a.startDate).getTime()
        );
        this.academicYears.set(sorted);
        // Auto-select current year for new forms
        const current = sorted.find(y => y.isCurrent) ?? sorted[0];
        if (current && !this.isEdit()) {
          this.selectedAcademicYearId.set(current.id);
          this.form.patchValue({ joiningAcademicYearId: current.id });
        }
      },
    });

    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.isEdit.set(true);
      this.loading.set(true);
      this.studentService.getAll().subscribe({ next: (s) => this.students.set(s) });
      this.updateValidators('manual');
      this.admissionService.getById(id).subscribe({
        next: (a) => {
          this.selectedAcademicYearId.set(a.joiningAcademicYearId);
          this.editStudentId.set(a.studentId);
          this.form.patchValue({ ...a, joiningAcademicYearId: a.joiningAcademicYearId });
          this.loading.set(false);
        },
        error: () => {
          this.toast.error('Failed to load admission');
          void this.router.navigate(['/admissions']);
        },
      });
    } else {
      this.updateValidators('from-enquiry');
      this.enquiryService.getAdmissionPending().subscribe({
        next: (list) => this.pendingEnquiries.set(list),
        error: () => this.toast.error('Failed to load pending enquiries'),
      });
      this.studentService.getAll().subscribe({ next: (s) => this.students.set(s) });
    }
  }

  protected onAcademicYearSelect(event: Event): void {
    const id = Number((event.target as HTMLSelectElement).value);
    this.selectedAcademicYearId.set(id);
    this.form.patchValue({ joiningAcademicYearId: id });
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
          yearOfStudy: p.suggestedYearOfStudy,
          admissionDate: new Date().toISOString().split('T')[0],
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
    if (this.form.invalid) { scrollToFirstInvalid(this.form); return; }
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
      this.form.get('yearOfStudy')?.setValidators([Validators.required, Validators.min(1)]);
      this.form.get('admissionDate')?.setValidators([Validators.required]);
      this.form.get('studentId')?.clearValidators();
    } else {
      this.form.get('enquiryId')?.clearValidators();
      this.form.get('firstName')?.clearValidators();
      this.form.get('lastName')?.clearValidators();
      this.form.get('email')?.clearValidators();
      this.form.get('yearOfStudy')?.clearValidators();
      this.form.get('admissionDate')?.clearValidators();
      this.form.get('studentId')?.setValidators([Validators.required]);
    }
    AdmissionFormComponent.MODE_TOGGLED_CONTROLS.forEach((ctrl) => {
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
    // Pick only the fields relevant to AdmissionRequest; ignore from-enquiry-only fields.
    const v = this.form.value as Record<string, unknown>;
    const admissionData: AdmissionRequest = {
      studentId: v['studentId'] as number,
      joiningAcademicYearId: v['joiningAcademicYearId'] as number,
      applicationDate: v['applicationDate'] as string,
      declarationPlace: v['declarationPlace'] as string | undefined,
      declarationDate: v['declarationDate'] as string | undefined,
      parentConsentGiven: v['parentConsentGiven'] as boolean | undefined,
      applicantConsentGiven: v['applicantConsentGiven'] as boolean | undefined,
    };
    const qualifications = v['qualifications'] as unknown[] ?? [];

    const save$ = this.isEdit()
      ? this.admissionService.update(id, admissionData)
      : this.admissionService.create(admissionData);

    save$.subscribe({
      next: (admission) => {
        const quals: unknown[] = qualifications;
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

    return {
      firstName: v['firstName'] as string,
      lastName: v['lastName'] as string,
      email: v['email'] as string,
      phone: this.nullableStr(v['phone'] as string) ?? undefined,
      yearOfStudy: v['yearOfStudy'] as number,
      admissionDate: v['admissionDate'] as string,
      joiningAcademicYearId: this.selectedAcademicYearId()!,
      applicationDate: v['applicationDate'] as string,
      parentConsentGiven: v['parentConsentGiven'] as boolean,
      applicantConsentGiven: v['applicantConsentGiven'] as boolean,
      dateOfBirth: this.nullableStr(v['dateOfBirth'] as string),
      gender: this.nullable(v['gender']) as EnquiryConversionRequest['gender'],
      aadharNumber: this.nullableStr(v['aadharNumber'] as string),
      nationality: this.nullableStr(v['nationality'] as string),
      religion: this.nullableStr(v['religion'] as string),
      communityCategory: this.nullableStr(v['communityCategory'] as string),
      caste: this.nullableStr(v['caste'] as string),
      bloodGroup: this.nullableStr(v['bloodGroup'] as string),
      fatherName: this.nullableStr(v['fatherName'] as string),
      fatherPhone: this.nullableStr(v['fatherPhone'] as string),
      fatherEmail: this.nullableStr(v['fatherEmail'] as string),
      motherName: this.nullableStr(v['motherName'] as string),
      motherPhone: this.nullableStr(v['motherPhone'] as string),
      motherEmail: this.nullableStr(v['motherEmail'] as string),
      parentMobile: this.nullableStr(v['parentMobile'] as string),
      address: this.hasValidAddressFields(addr)
        ? {
            postalAddress: this.nullableStr(addr['postalAddress'] as string),
            street: this.nullableStr(addr['street'] as string),
            city: this.nullableStr(addr['city'] as string),
            district: this.nullableStr(addr['district'] as string),
            state: this.nullableStr(addr['state'] as string),
            pincode: this.nullableStr(addr['pincode'] as string),
          }
        : null,
      declarationPlace: this.nullableStr(v['declarationPlace'] as string),
      declarationDate: this.nullableStr(v['declarationDate'] as string),
    };
  }

  private hasValidAddressFields(addr: Record<string, unknown>): boolean {
    return Object.values(addr).some((x) => x !== '' && x !== null && x !== undefined);
  }

  /** Returns null when value is an empty string or null; otherwise returns the value. */
  private nullable<T>(value: T): T | null {
    return value === '' || value === null ? null : value;
  }

  /** Typed convenience wrapper for string fields. */
  private nullableStr(value: string | null | undefined): string | null {
    return value === '' || value == null ? null : value;
  }

  private finish(): void {
    this.toast.success('Admission saved successfully');
    void this.router.navigate(['/admissions']);
  }
}

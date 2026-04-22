import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { EnquiryService } from '../enquiry.service';
import { Enquiry, EnquiryConversionPrefillResponse } from '../enquiry.model';
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

  protected readonly form: FormGroup = this.fb.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    phone: [''],
    semester: [1, [Validators.required, Validators.min(1)]],
    admissionDate: ['', Validators.required],
    academicYearFrom: [null, Validators.required],
    academicYearTo: [null, Validators.required],
    applicationDate: ['', Validators.required],
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
    this.enquiryService.convertEnquiry(id, this.form.value).subscribe({
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
}

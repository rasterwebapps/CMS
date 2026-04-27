import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DatePipe, CurrencyPipe, NgClass, DecimalPipe } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { AcademicYearService } from '../academic-year.service';
import {
  AcademicYear,
  CourseOffering,
  CourseRegistration,
  DemandStatus,
  FeeDemand,
  StudentTermEnrollment,
  TermInstance,
  TermInstanceStatus,
  TermBillingSchedule,
  TermBillingScheduleRequest,
  TermFeePayment,
  TermFeePaymentRequest,
  LateFeeType,
} from '../academic-year.model';
import { ToastService } from '../../../core/toast/toast.service';
import { FeePaymentDialogComponent } from './fee-payment-dialog.component';

@Component({
  selector: 'app-academic-year-detail',
  standalone: true,
  imports: [
    RouterLink,
    DatePipe,
    CurrencyPipe,
    NgClass,
    DecimalPipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    FeePaymentDialogComponent,
  ],
  templateUrl: './academic-year-detail.component.html',
  styleUrl: './academic-year-detail.component.scss',
})
export class AcademicYearDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly academicYearService = inject(AcademicYearService);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);
  private readonly dialog = inject(MatDialog);

  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly academicYear = signal<AcademicYear | null>(null);
  protected readonly termInstances = signal<TermInstance[]>([]);
  protected readonly billingSchedules = signal<TermBillingSchedule[]>([]);
  protected readonly enrollments = signal<StudentTermEnrollment[]>([]);
  protected readonly generatingEnrollments = signal(false);

  protected readonly courseOfferings = signal<CourseOffering[]>([]);
  protected readonly generatingOfferings = signal(false);
  protected readonly semesterFilter = signal<number | null>(null);
  protected readonly editingOffering = signal<number | null>(null);

  protected readonly courseRegistrations = signal<CourseRegistration[]>([]);
  protected readonly generatingRegistrations = signal(false);

  protected readonly feeDemands = signal<FeeDemand[]>([]);
  protected readonly generatingDemands = signal(false);
  protected readonly demandStatusFilter = signal<DemandStatus | ''>('');

  protected readonly filteredDemands = computed(() => {
    const filter = this.demandStatusFilter();
    return filter
      ? this.feeDemands().filter(d => d.status === filter)
      : this.feeDemands();
  });

  protected readonly oddTermInstance = computed(() =>
    this.termInstances().find(t => t.termType === 'ODD') ?? null
  );
  protected readonly evenTermInstance = computed(() =>
    this.termInstances().find(t => t.termType === 'EVEN') ?? null
  );
  protected readonly oddBilling = computed(() =>
    this.billingSchedules().find(b => b.termType === 'ODD') ?? null
  );
  protected readonly evenBilling = computed(() =>
    this.billingSchedules().find(b => b.termType === 'EVEN') ?? null
  );

  protected readonly filteredOfferings = computed(() => {
    const filter = this.semesterFilter();
    return filter != null
      ? this.courseOfferings().filter(o => o.semesterNumber === filter)
      : this.courseOfferings();
  });

  protected readonly oddTermForm: FormGroup = this.fb.group({
    startDate: [''],
    endDate: [''],
  });

  protected readonly evenTermForm: FormGroup = this.fb.group({
    startDate: [''],
    endDate: [''],
  });

  protected readonly oddBillingForm: FormGroup = this.fb.group({
    dueDate: ['', Validators.required],
    lateFeeType: ['FLAT' as LateFeeType, Validators.required],
    lateFeeAmount: [0, [Validators.required, Validators.min(0)]],
    graceDays: [0, [Validators.required, Validators.min(0)]],
  });

  protected readonly evenBillingForm: FormGroup = this.fb.group({
    dueDate: ['', Validators.required],
    lateFeeType: ['FLAT' as LateFeeType, Validators.required],
    lateFeeAmount: [0, [Validators.required, Validators.min(0)]],
    graceDays: [0, [Validators.required, Validators.min(0)]],
  });

  protected readonly offeringEditForm: FormGroup = this.fb.group({
    facultyId: [null],
    sectionLabel: [''],
  });

  private academicYearId!: number;

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) {
      void this.router.navigate(['/academic-years']);
      return;
    }
    this.academicYearId = Number(idParam);
    this.loadData();
  }

  private loadData(): void {
    this.loading.set(true);
    this.academicYearService.getAcademicYearById(this.academicYearId).subscribe({
      next: (ay) => {
        this.academicYear.set(ay);
        this.loadTermInstances();
      },
      error: () => {
        this.toast.error('Failed to load academic year');
        void this.router.navigate(['/academic-years']);
      },
    });
  }

  private loadTermInstances(): void {
    this.academicYearService.getTermInstancesByAcademicYear(this.academicYearId).subscribe({
      next: (terms) => {
        this.termInstances.set(terms);
        const odd = terms.find(t => t.termType === 'ODD');
        const even = terms.find(t => t.termType === 'EVEN');
        if (odd) {
          this.oddTermForm.patchValue({ startDate: odd.startDate, endDate: odd.endDate });
          if (odd.status === 'OPEN' || odd.status === 'LOCKED') {
            this.loadCourseOfferings(odd.id);
            this.loadCourseRegistrations(odd.id);
            this.loadFeeDemands(odd.id);
          }
        }
        if (even) {
          this.evenTermForm.patchValue({ startDate: even.startDate, endDate: even.endDate });
          if (even.status === 'OPEN' || even.status === 'LOCKED') {
            this.loadCourseOfferings(even.id);
            this.loadCourseRegistrations(even.id);
            this.loadFeeDemands(even.id);
          }
        }
        this.loadBillingSchedules();
      },
      error: () => {
        this.loadBillingSchedules();
      },
    });
  }

  private loadBillingSchedules(): void {
    this.academicYearService.getTermBillingSchedulesByAcademicYear(this.academicYearId).subscribe({
      next: (schedules) => {
        this.billingSchedules.set(schedules);
        const odd = schedules.find(b => b.termType === 'ODD');
        const even = schedules.find(b => b.termType === 'EVEN');
        if (odd) {
          this.oddBillingForm.patchValue({
            dueDate: odd.dueDate,
            lateFeeType: odd.lateFeeType,
            lateFeeAmount: odd.lateFeeAmount,
            graceDays: odd.graceDays,
          });
        }
        if (even) {
          this.evenBillingForm.patchValue({
            dueDate: even.dueDate,
            lateFeeType: even.lateFeeType,
            lateFeeAmount: even.lateFeeAmount,
            graceDays: even.graceDays,
          });
        }
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }

  protected advanceTermStatus(term: TermInstance): void {
    const nextStatus = this.getNextStatus(term.status);
    if (!nextStatus) return;

    this.academicYearService.updateTermInstance(term.id, { status: nextStatus }).subscribe({
      next: () => {
        this.toast.success(`Term status advanced to ${nextStatus}`);
        this.loadTermInstances();
      },
      error: () => {
        this.toast.error('Failed to update term status');
      },
    });
  }

  protected saveTermDates(termType: 'ODD' | 'EVEN'): void {
    const term = termType === 'ODD' ? this.oddTermInstance() : this.evenTermInstance();
    const formValue = termType === 'ODD' ? this.oddTermForm.value : this.evenTermForm.value;
    if (!term) return;

    this.academicYearService.updateTermInstance(term.id, {
      startDate: formValue.startDate || null,
      endDate: formValue.endDate || null,
    }).subscribe({
      next: () => {
        this.toast.success('Term dates updated');
        this.loadTermInstances();
      },
      error: () => {
        this.toast.error('Failed to update term dates');
      },
    });
  }

  protected saveBillingSchedule(termType: 'ODD' | 'EVEN'): void {
    const form = termType === 'ODD' ? this.oddBillingForm : this.evenBillingForm;
    if (form.invalid) {
      form.markAllAsTouched();
      return;
    }
    const v = form.value;
    const request: TermBillingScheduleRequest = {
      academicYearId: this.academicYearId,
      termType,
      dueDate: v.dueDate,
      lateFeeType: v.lateFeeType as LateFeeType,
      lateFeeAmount: v.lateFeeAmount,
      graceDays: v.graceDays,
    };
    this.saving.set(true);
    this.academicYearService.createOrUpdateTermBillingSchedule(request).subscribe({
      next: () => {
        this.toast.success(`${termType} term billing schedule saved`);
        this.loadBillingSchedules();
        this.saving.set(false);
      },
      error: () => {
        this.toast.error('Failed to save billing schedule');
        this.saving.set(false);
      },
    });
  }

  protected loadEnrollmentsForTerm(termInstanceId: number): void {
    this.academicYearService.getEnrollmentsByTermInstance(termInstanceId).subscribe({
      next: (data) => this.enrollments.set(data),
      error: () => this.toast.error('Failed to load enrollments'),
    });
  }

  protected generateEnrollments(term: TermInstance): void {
    this.generatingEnrollments.set(true);
    this.academicYearService.generateEnrollments(term.id).subscribe({
      next: (result) => {
        this.toast.success(`Generated ${result.enrollmentsCreated} enrollment(s)`);
        this.loadEnrollmentsForTerm(term.id);
        this.generatingEnrollments.set(false);
      },
      error: () => {
        this.toast.error('Failed to generate enrollments');
        this.generatingEnrollments.set(false);
      },
    });
  }

  protected loadCourseOfferings(termInstanceId: number): void {
    this.academicYearService.getCourseOfferingsByTermInstance(termInstanceId).subscribe({
      next: (data) => this.courseOfferings.set(data),
      error: () => this.toast.error('Failed to load course offerings'),
    });
  }

  protected generateCourseOfferings(term: TermInstance): void {
    this.generatingOfferings.set(true);
    this.academicYearService.generateCourseOfferings(term.id).subscribe({
      next: (result) => {
        this.toast.success(`Generated ${result.offeringsCreated} course offering(s)`);
        this.loadCourseOfferings(term.id);
        this.generatingOfferings.set(false);
      },
      error: () => {
        this.toast.error('Failed to generate course offerings');
        this.generatingOfferings.set(false);
      },
    });
  }

  protected startEditOffering(offering: CourseOffering): void {
    this.editingOffering.set(offering.id);
    this.offeringEditForm.patchValue({
      facultyId: offering.facultyId,
      sectionLabel: offering.sectionLabel,
    });
  }

  protected cancelEditOffering(): void {
    this.editingOffering.set(null);
  }

  protected saveOffering(offering: CourseOffering, termInstanceId: number): void {
    const v = this.offeringEditForm.value;
    this.academicYearService.updateCourseOffering(offering.id, {
      facultyId: v.facultyId ? Number(v.facultyId) : null,
      sectionLabel: v.sectionLabel || null,
    }).subscribe({
      next: () => {
        this.toast.success('Offering updated');
        this.editingOffering.set(null);
        this.loadCourseOfferings(termInstanceId);
      },
      error: () => this.toast.error('Failed to update offering'),
    });
  }

  protected setSemesterFilter(value: string): void {
    const num = Number(value);
    this.semesterFilter.set(value === '' ? null : num);
  }

  protected loadCourseRegistrations(termInstanceId: number): void {
    // Load registrations count by fetching from all offerings for summary
    const offerings = this.courseOfferings();
    if (offerings.length === 0) {
      return;
    }
    // Load registrations for the first offering as a representative sample for display
    this.academicYearService.getCourseRegistrationsByCourseOffering(offerings[0].id).subscribe({
      next: (data) => this.courseRegistrations.set(data),
      error: () => {},
    });
  }

  protected generateCourseRegistrations(term: TermInstance): void {
    this.generatingRegistrations.set(true);
    this.academicYearService.generateCourseRegistrations(term.id).subscribe({
      next: (result) => {
        this.toast.success(`Generated ${result.registrationsCreated} course registration(s)`);
        this.loadCourseOfferings(term.id);
        this.generatingRegistrations.set(false);
      },
      error: () => {
        this.toast.error('Failed to generate course registrations');
        this.generatingRegistrations.set(false);
      },
    });
  }

  protected loadFeeDemands(termInstanceId: number): void {
    this.academicYearService.getFeeDemandsByTermInstance(termInstanceId).subscribe({
      next: (data) => this.feeDemands.set(data),
      error: () => this.toast.error('Failed to load fee demands'),
    });
  }

  protected generateFeeDemands(term: TermInstance): void {
    this.generatingDemands.set(true);
    this.academicYearService.generateFeeDemands(term.id).subscribe({
      next: (result) => {
        this.toast.success(`Generated ${result.demandsCreated} fee demand(s)`);
        this.loadFeeDemands(term.id);
        this.generatingDemands.set(false);
      },
      error: () => {
        this.toast.error('Failed to generate fee demands');
        this.generatingDemands.set(false);
      },
    });
  }

  protected openPaymentDialog(demand: FeeDemand, term: TermInstance): void {
    const ref = this.dialog.open(FeePaymentDialogComponent, {
      width: '480px',
      data: { demand },
    });
    ref.afterClosed().subscribe((result: TermFeePaymentRequest | null) => {
      if (result) {
        this.academicYearService.recordFeePayment(result).subscribe({
          next: (payment) => {
            this.toast.success(`Payment recorded! Receipt: ${payment.receiptNumber}`);
            this.loadFeeDemands(term.id);
          },
          error: () => this.toast.error('Failed to record payment'),
        });
      }
    });
  }

  protected setDemandStatusFilter(value: string): void {
    this.demandStatusFilter.set(value as DemandStatus | '');
  }

  protected getDemandStatusClass(status: DemandStatus): string {
    switch (status) {
      case 'PAID': return 'cms-badge--soft-success';
      case 'PARTIAL': return 'cms-badge--soft-warning';
      case 'UNPAID': return 'cms-badge--soft-error';
      case 'WAIVED': return 'cms-badge--soft-default';
    }
  }

  protected getNextStatus(current: TermInstanceStatus): TermInstanceStatus | null {
    switch (current) {
      case 'PLANNED': return 'OPEN';
      case 'OPEN': return 'LOCKED';
      case 'LOCKED': return null;
    }
  }

  protected getStatusStepClass(current: TermInstanceStatus, step: TermInstanceStatus): string {
    const order: TermInstanceStatus[] = ['PLANNED', 'OPEN', 'LOCKED'];
    const currentIdx = order.indexOf(current);
    const stepIdx = order.indexOf(step);
    if (stepIdx < currentIdx) return 'step--done';
    if (stepIdx === currentIdx) return 'step--active';
    return 'step--pending';
  }

  protected getStatusLabel(status: TermInstanceStatus): string {
    switch (status) {
      case 'PLANNED': return 'Planned';
      case 'OPEN': return 'Open';
      case 'LOCKED': return 'Locked';
    }
  }
}

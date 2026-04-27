import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AcademicYearService } from '../academic-year.service';
import {
  AcademicYear,
  StudentTermEnrollment,
  TermInstance,
  TermInstanceStatus,
  TermBillingSchedule,
  TermBillingScheduleRequest,
  LateFeeType,
} from '../academic-year.model';
import { ToastService } from '../../../core/toast/toast.service';

@Component({
  selector: 'app-academic-year-detail',
  standalone: true,
  imports: [
    RouterLink,
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
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

  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly academicYear = signal<AcademicYear | null>(null);
  protected readonly termInstances = signal<TermInstance[]>([]);
  protected readonly billingSchedules = signal<TermBillingSchedule[]>([]);
  protected readonly enrollments = signal<StudentTermEnrollment[]>([]);
  protected readonly generatingEnrollments = signal(false);

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
        }
        if (even) {
          this.evenTermForm.patchValue({ startDate: even.startDate, endDate: even.endDate });
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

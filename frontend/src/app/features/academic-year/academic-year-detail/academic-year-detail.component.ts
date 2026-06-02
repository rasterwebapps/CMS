import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AcademicYearService } from '../academic-year.service';
import {
  AcademicYear,
  CohortSummary,
  TermBillingSchedule,
  TermInstance,
  TermInstanceStatus,
} from '../academic-year.model';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';

@Component({
  selector: 'app-academic-year-detail',
  standalone: true,
  imports: [AppDatePipe, RouterLink],
  templateUrl: './academic-year-detail.component.html',
  styleUrl: './academic-year-detail.component.scss',
})
export class AcademicYearDetailComponent implements OnInit {
  private readonly route               = inject(ActivatedRoute);
  private readonly router              = inject(Router);
  private readonly academicYearService = inject(AcademicYearService);
  private readonly permissionService   = inject(PermissionService);
  private readonly toast               = inject(ToastService);

  protected readonly loading          = signal(true);
  protected readonly advancingOdd     = signal(false);
  protected readonly advancingEven    = signal(false);
  protected readonly generatingOdd    = signal(false);
  protected readonly generatingEven   = signal(false);
  protected readonly academicYear     = signal<AcademicYear | null>(null);
  protected readonly termInstances    = signal<TermInstance[]>([]);
  protected readonly billingSchedules = signal<TermBillingSchedule[]>([]);
  protected readonly cohorts             = signal<CohortSummary[]>([]);
  protected readonly initializingCohorts    = signal(false);
  protected readonly togglingCounsellingId  = signal<number | null>(null);

  protected readonly oddTermInstance  = computed(() => this.termInstances().find(t => t.termType === 'ODD')  ?? null);
  protected readonly evenTermInstance = computed(() => this.termInstances().find(t => t.termType === 'EVEN') ?? null);
  protected readonly oddBilling       = computed(() => this.billingSchedules().find(b => b.termType === 'ODD')  ?? null);
  protected readonly evenBilling      = computed(() => this.billingSchedules().find(b => b.termType === 'EVEN') ?? null);

  protected readonly grandTotals = computed(() => {
    let management = 0, counselling = 0, total = 0;
    for (const c of this.cohorts()) {
      management  += (c.managementSeats  ?? 0);
      counselling += (c.counsellingSeats ?? 0);
      total       += (c.totalSeats ?? (c.managementSeats ?? 0) + (c.counsellingSeats ?? 0));
    }
    return { management, counselling, total };
  });

  protected academicYearId!: number;

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) { void this.router.navigate(['/academic-years']); return; }
    this.academicYearId = Number(idParam);
    this.loadData();
  }

  protected canEdit(): boolean {
    return this.permissionService.has('ACADEMIC_YEAR_MANAGE');
  }

  private loadData(): void {
    this.loading.set(true);
    forkJoin([
      this.academicYearService.getAcademicYearById(this.academicYearId),
      this.academicYearService.getTermInstancesByAcademicYear(this.academicYearId),
      this.academicYearService.getTermBillingSchedulesByAcademicYear(this.academicYearId),
      this.academicYearService.getCohortsByAcademicYear(this.academicYearId),
    ]).subscribe({
      next: ([ay, terms, schedules, cohorts]) => {
        this.academicYear.set(ay);
        this.termInstances.set(terms);
        this.billingSchedules.set(schedules);
        this.cohorts.set(cohorts);
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load academic year');
        void this.router.navigate(['/academic-years']);
      },
    });
  }

  protected initializeCohorts(): void {
    this.initializingCohorts.set(true);
    this.academicYearService.initializeCohorts(this.academicYearId).subscribe({
      next: (cohorts) => {
        this.cohorts.set(cohorts);
        this.initializingCohorts.set(false);
        this.toast.success(`${cohorts.length} cohort(s) initialized`);
      },
      error: () => {
        this.toast.error('Failed to initialize cohorts');
        this.initializingCohorts.set(false);
      },
    });
  }

  protected advanceTermStatus(termType: 'ODD' | 'EVEN'): void {
    const term = termType === 'ODD' ? this.oddTermInstance() : this.evenTermInstance();
    const next = term ? this.getNextStatus(term.status) : null;
    if (!term || !next) return;
    const advancing = termType === 'ODD' ? this.advancingOdd : this.advancingEven;
    advancing.set(true);
    this.academicYearService.updateTermInstance(term.id, { status: next }).subscribe({
      next: () => { this.toast.success(`Term advanced to ${next}`); this.loadData(); advancing.set(false); },
      error: () => { this.toast.error('Failed to advance term status'); advancing.set(false); },
    });
  }

  protected generateEnrollments(termType: 'ODD' | 'EVEN'): void {
    const term = termType === 'ODD' ? this.oddTermInstance() : this.evenTermInstance();
    if (!term) return;
    const generating = termType === 'ODD' ? this.generatingOdd : this.generatingEven;
    generating.set(true);
    this.academicYearService.generateEnrollments(term.id).subscribe({
      next: (res) => { this.toast.success(`Generated ${res.enrollmentsCreated} enrollment(s)`); generating.set(false); },
      error: () => { this.toast.error('Failed to generate enrollments'); generating.set(false); },
    });
  }

  protected getNextStatus(current: TermInstanceStatus): TermInstanceStatus | null {
    if (current === 'PLANNED') return 'OPEN';
    if (current === 'OPEN')    return 'LOCKED';
    return null;
  }

  protected getStatusStepClass(current: TermInstanceStatus, step: TermInstanceStatus): string {
    const order: TermInstanceStatus[] = ['PLANNED', 'OPEN', 'LOCKED'];
    const ci = order.indexOf(current), si = order.indexOf(step);
    if (si < ci) return 'step--done';
    if (si === ci) return 'step--active';
    return 'step--pending';
  }

  protected toggleCounsellingStatus(cohort: import('../academic-year.model').CohortSummary): void {
    this.togglingCounsellingId.set(cohort.id);
    const closing = !cohort.counsellingClosed;
    this.academicYearService.setQuotaStatus(cohort.id, 'COUNSELLING', closing).subscribe({
      next: (updated: import('../academic-year.model').CohortSummary) => {
        this.cohorts.update(list => list.map(c => c.id === updated.id ? updated : c));
        this.togglingCounsellingId.set(null);
        this.toast.success(closing ? 'Counselling closed — seats are now lapsed' : 'Counselling reopened');
      },
      error: () => {
        this.toast.error('Failed to update counselling status');
        this.togglingCounsellingId.set(null);
      },
    });
  }

  protected lateFeeLabel(type: string, amount: number): string {
    return type === 'FLAT' ? `₹${amount} flat` : `₹${amount}/day`;
  }
}

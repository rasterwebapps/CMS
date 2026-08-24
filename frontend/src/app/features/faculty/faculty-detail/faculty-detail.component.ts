import { Component, HostListener, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { FacultyService } from '../faculty.service';
import { Faculty, FacultyQualification, FacultyScheduleWorkload, FacultyWorkloadAssignment, FacultyWorkloadDetail, FACULTY_QUALIFICATION_OPTIONS } from '../faculty.model';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear, TermInstance } from '../../academic-year/academic-year.model';
import { CourseOfferingEditDialogComponent, FacultyOption } from '../../course-offering/course-offering-edit-dialog/course-offering-edit-dialog.component';
import { RaiseCapFlyoutComponent } from './raise-cap-flyout.component';
import { WEEK_GRID_DAYS, WEEK_GRID_DAY_LABELS } from '../../../shared/week-grid/week-grid.model';
import { CmsTypeBadgeComponent } from '../../../shared/type-badge/type-badge.component';
import { LabScheduleService } from '../../lab-schedule/lab-schedule.service';
import { LabSchedule } from '../../lab-schedule/lab-schedule.model';
import { PermissionService } from '../../../core/permissions/permission.service';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { CmsSkeletonComponent } from '../../../shared/skeleton/skeleton.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { ProfileDocumentsComponent } from '../../../shared/profile-documents/profile-documents.component';
import { computeInitials } from '../../../shared/utils/initials';
import { ToastService } from '../../../core/toast/toast.service';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';

@Component({
  selector: 'app-faculty-detail',
  standalone: true,
  imports: [
    AppDatePipe,
    DecimalPipe,
    FormsModule,
    MatTabsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    CmsStatusBadgeComponent,
    CmsSkeletonComponent,
    CmsEmptyStateComponent,
    CmsTypeBadgeComponent,
    RaiseCapFlyoutComponent,
    ProfileDocumentsComponent],
  templateUrl: './faculty-detail.component.html',
  styleUrl: './faculty-detail.component.scss',
})
export class FacultyDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly facultyService = inject(FacultyService);
  private readonly academicYearService = inject(AcademicYearService);
  private readonly labScheduleService = inject(LabScheduleService);
  private readonly toast = inject(ToastService);
  private readonly permissionService = inject(PermissionService);
  private readonly dialog = inject(MatDialog);

  protected readonly faculty = signal<Faculty | null>(null);
  protected readonly loading = signal(true);
  protected readonly selectedTabIndex = signal(0);

  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly termInstances = signal<TermInstance[]>([]);
  protected readonly termsLoading = signal(false);
  protected selectedAcademicYearId: number | null = null;
  protected selectedTermInstanceId: number | null = null;

  protected readonly workload = signal<FacultyWorkloadDetail | null>(null);
  protected readonly workloadLoading = signal(false);
  protected readonly scheduleWorkload = signal<FacultyScheduleWorkload | null>(null);
  protected readonly labSchedules = signal<LabSchedule[]>([]);
  protected readonly labSchedulesLoading = signal(false);
  protected readonly dayLabels = WEEK_GRID_DAY_LABELS;
  /** Day chip display order — real placements can land on Saturday as an overflow day (see the
   *  Skeleton Builder automation's own Mon-Fri-first/Saturday-fallback rule), so it's included. */
  protected readonly weekDays = WEEK_GRID_DAYS;

  protected readonly facultyOptions = signal<FacultyOption[]>([]);
  protected readonly showRaiseCap = signal(false);

  /** First + last initial of the faculty's full name. */
  protected readonly initials = computed(() => computeInitials(this.faculty()?.fullName));

  ngOnInit(): void {
    this.route.fragment.subscribe((fragment) => {
      this.selectedTabIndex.set(fragment === 'documents' ? 4 : fragment === 'courses' ? 2 : 0);
    });

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadFaculty(+id);
    }

    if (this.canViewWorkload() || this.canViewLabSchedules()) {
      // Both Courses (FACULTY_WORKLOAD_VIEW) and Lab Schedules (LAB_SCHEDULE_VIEW) share this one
      // term selector, so either permission alone is enough to need it loaded — each tab's own
      // content still individually gates on its own permission below.
      // Deep-linked from e.g. the Global Auto-Schedule flyout's over-capacity card, which already
      // knows which term it's checking capacity for — honor that instead of defaulting to the
      // current academic year, mirroring the editOfferingId/suggestedFacultyId pattern Assign
      // Faculty already uses for the same kind of "land already scoped" deep link.
      const qp = this.route.snapshot.queryParamMap;
      const deepLinkYearId = Number(qp.get('academicYearId')) || null;
      const deepLinkTermId = Number(qp.get('termInstanceId')) || null;

      this.academicYearService.getAllAcademicYears().subscribe({
        next: (years) => {
          this.academicYears.set(years);
          const initialYearId = deepLinkYearId ?? years.find((y) => y.isCurrent)?.id ?? years[0]?.id ?? null;
          if (initialYearId) {
            this.selectedAcademicYearId = initialYearId;
            this.loadTermInstances(initialYearId, deepLinkTermId ?? undefined);
          }
        },
        error: () => this.toast.error('Failed to load academic years'),
      });
    }

    if (this.canReassign()) {
      this.facultyService.getAll().subscribe({
        next: (all) => this.facultyOptions.set(all.map((f) => ({ id: f.id, name: f.fullName, specialityId: f.specialityId }))),
        error: () => this.toast.error('Failed to load faculty options'),
      });
    }
  }

  private loadFaculty(id: number): void {
    this.loading.set(true);
    this.facultyService.getById(id).subscribe({
      next: (faculty) => {
        this.faculty.set(faculty);
        this.loading.set(false);
        // Faculty and academic-year/term data load in parallel (ngOnInit fires both) — whichever
        // resolves last is the one that actually has everything loadWorkload() needs, so both
        // paths call it; the guard inside no-ops if the other half isn't ready yet.
        this.loadWorkload();
        this.loadScheduleWorkload();
        this.loadLabSchedules();
      },
      error: () => {
        this.toast.error('Error loading faculty details');
        this.loading.set(false);
        void this.router.navigate(['/faculty']);
      },
    });
  }

  protected canViewWorkload(): boolean {
    return this.permissionService.has('FACULTY_WORKLOAD_VIEW');
  }

  /** "Reassign…" opens Assign Faculty in a new tab — clicking it gives no direct signal here
   *  whether the reassignment actually happened. Re-run the workload fetch automatically the
   *  moment this tab regains focus (the user just came back from doing something elsewhere), so
   *  the table/stats visibly reflect whatever the real current state is rather than leaving the
   *  admin guessing whether it took effect. */
  @HostListener('window:focus')
  protected onWindowFocus(): void {
    if (this.selectedTermInstanceId && !this.workloadLoading() && !this.labSchedulesLoading()) {
      this.loadWorkload();
      this.loadScheduleWorkload();
      this.loadLabSchedules();
    }
  }

  protected canViewLabSchedules(): boolean {
    return this.permissionService.has('LAB_SCHEDULE_VIEW');
  }

  /** Gates the "Reassign…" button — matches the permission the underlying
   *  `PUT /course-offerings/{id}` save actually enforces (`COURSE_MANAGE`), not FACULTY_MANAGE,
   *  so the button's visibility never promises something the save would then reject. */
  protected canReassign(): boolean {
    return this.permissionService.has('COURSE_MANAGE');
  }

  /** Opens the exact same dialog Assign Faculty uses, in place — no navigation, no new tab.
   *  Fetches the full offering first since the dialog needs the complete CourseOffering shape,
   *  not just the id this workload row carries. */
  protected onReassign(assignment: FacultyWorkloadAssignment): void {
    this.academicYearService.getCourseOfferingById(assignment.courseOfferingId).subscribe({
      next: (offering) => {
        this.dialog.open(CourseOfferingEditDialogComponent, {
          data: { offering, facultyOptions: this.facultyOptions(), suggestedFacultyId: null },
          width: '480px',
        }).afterClosed().subscribe((updated) => {
          if (updated) {
            this.loadWorkload();
            this.loadScheduleWorkload();
          }
        });
      },
      error: () => this.toast.error('Failed to load offering details'),
    });
  }

  protected openRaiseCap(): void {
    this.showRaiseCap.set(true);
  }

  protected onRaiseCapClosed(): void {
    this.showRaiseCap.set(false);
  }

  protected onRaiseCapSaved(): void {
    this.showRaiseCap.set(false);
    const id = this.faculty()?.id;
    if (id) {
      this.facultyService.getById(id).subscribe({ next: (f) => this.faculty.set(f) });
    }
    this.loadWorkload();
  }

  protected onAcademicYearChange(): void {
    this.selectedTermInstanceId = null;
    this.workload.set(null);
    this.scheduleWorkload.set(null);
    this.labSchedules.set([]);
    if (this.selectedAcademicYearId) this.loadTermInstances(this.selectedAcademicYearId);
  }

  protected onTermChange(): void {
    this.loadWorkload();
    this.loadScheduleWorkload();
    this.loadLabSchedules();
  }

  private loadTermInstances(academicYearId: number, preselectTermInstanceId?: number): void {
    this.termsLoading.set(true);
    this.academicYearService.getTermInstancesByAcademicYear(academicYearId).subscribe({
      next: (terms) => {
        this.termInstances.set(terms);
        this.termsLoading.set(false);
        const preselect = preselectTermInstanceId && terms.some((t) => t.id === preselectTermInstanceId)
          ? preselectTermInstanceId
          : terms[0]?.id ?? null;
        this.selectedTermInstanceId = preselect;
        this.loadWorkload();
        this.loadScheduleWorkload();
        this.loadLabSchedules();
      },
      error: () => { this.toast.error('Failed to load term instances'); this.termsLoading.set(false); },
    });
  }

  private loadWorkload(): void {
    const facultyId = this.faculty()?.id;
    if (!this.canViewWorkload() || !facultyId || !this.selectedTermInstanceId) {
      this.workload.set(null);
      return;
    }
    this.workloadLoading.set(true);
    this.facultyService.getWorkload(facultyId, this.selectedTermInstanceId).subscribe({
      next: (detail) => {
        this.workload.set(detail);
        this.workloadLoading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load workload');
        this.workload.set(null);
        this.workloadLoading.set(false);
      },
    });
  }

  private loadScheduleWorkload(): void {
    const facultyId = this.faculty()?.id;
    if (!this.canViewWorkload() || !facultyId || !this.selectedTermInstanceId) {
      this.scheduleWorkload.set(null);
      return;
    }
    this.facultyService.getScheduleWorkload(facultyId, this.selectedTermInstanceId).subscribe({
      next: (detail) => this.scheduleWorkload.set(detail),
      error: () => this.scheduleWorkload.set(null),
    });
  }

  private loadLabSchedules(): void {
    const facultyId = this.faculty()?.id;
    if (!this.canViewLabSchedules() || !facultyId || !this.selectedTermInstanceId) {
      this.labSchedules.set([]);
      return;
    }
    this.labSchedulesLoading.set(true);
    this.labScheduleService.getByFacultyAndTerm(facultyId, this.selectedTermInstanceId).subscribe({
      next: (schedules) => {
        this.labSchedules.set(schedules);
        this.labSchedulesLoading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load lab schedules');
        this.labSchedules.set([]);
        this.labSchedulesLoading.set(false);
      },
    });
  }

  protected hoursForDay(day: string): number {
    return this.scheduleWorkload()?.byDay.find((d) => d.dayOfWeek === day)?.hours ?? 0;
  }

  protected tierLabel(tier: string): string {
    switch (tier) {
      case 'FACULTY_OVERRIDE': return 'this faculty’s own override';
      case 'DESIGNATION_DEFAULT': return 'their designation’s default';
      default: return 'the institution-wide default';
    }
  }

  protected assignmentRoleLabel(a: FacultyWorkloadAssignment): string {
    if (a.cohortSectionId != null) return a.cohortSectionLabel ?? 'Section';
    if (a.batchId != null) return a.batchName ?? 'Batch';
    return 'Primary';
  }

  protected sessionTypeLabel(sessionType: string | null): string {
    switch (sessionType) {
      case 'THEORY': return 'Theory';
      case 'LAB': return 'Lab';
      case 'CLINICAL': return 'Clinical';
      case 'LAB_CLINICAL': return 'Lab/Clinical';
      default: return '—';
    }
  }

  protected getDesignationLabel(faculty: Faculty): string {
    return faculty.designationName ?? '—';
  }

  protected qualificationLabel(q: FacultyQualification): string {
    return FACULTY_QUALIFICATION_OPTIONS.find((o) => o.value === q)?.label ?? q;
  }


  protected canManageFaculty(): boolean {
    return this.permissionService.has('FACULTY_MANAGE');
  }

  protected canForceReplaceDocuments(): boolean {
    return this.permissionService.has('DOCUMENT_VERIFIED_OVERRIDE');
  }

  protected editFaculty(): void {
    if (!this.canManageFaculty()) return;

    const faculty = this.faculty();
    if (faculty) {
      void this.router.navigate(['/faculty', faculty.id, 'edit']);
    }
  }
}

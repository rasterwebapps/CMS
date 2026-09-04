import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource, MatTable } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear, CourseOffering, GenerateCourseOfferingsResponse, TermInstance } from '../../academic-year/academic-year.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { CmsIconToggleStatusComponent } from '../../../shared/icons';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';
import { TourService } from '../../../shared/tour/tour.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { COURSE_OFFERING_LIST_TOUR, COURSE_OFFERING_LIST_FLOW_MAP } from '../../../shared/tour/tours/course-offering.tours';
import { violationText } from '../../../shared/util/violation-text';

@Component({
  selector: 'app-course-offering-list',
  standalone: true,
  imports: [
    RouterLink, FormsModule, MatTableModule, MatPaginatorModule, MatSortModule,
    MatProgressSpinnerModule, MatDialogModule, MatTooltipModule,
    CmsEmptyStateComponent, CmsRowActionButtonComponent, CmsStatusBadgeComponent,
    CmsIconToggleStatusComponent,
    CmsTourButtonComponent,
  ],
  templateUrl: './course-offering-list.component.html',
  styleUrl: './course-offering-list.component.scss',
})
export class CourseOfferingListComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly route = inject(ActivatedRoute);
  private readonly permissionService = inject(PermissionService);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly tourService = inject(TourService);

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  protected readonly displayedColumns = [
    'subjectCode', 'subjectName', 'termNumber', 'cohort', 'status', 'actions',
  ];
  protected readonly dataSource = new MatTableDataSource<CourseOffering>([]);
  protected readonly loading = signal(false);
  protected readonly generating = signal(false);
  protected readonly termsLoading = signal(false);
  protected readonly searchValue = signal('');

  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly termInstances = signal<TermInstance[]>([]);

  /** All offerings loaded for the selected term instance, before the semester dropdown narrows
   *  them into dataSource.data — kept separate so switching the semester filter doesn't require
   *  a reload. */
  private readonly offerings = signal<CourseOffering[]>([]);
  protected readonly selectedSemester = signal<number | 'ALL'>('ALL');
  protected readonly semesterOptions = computed(() =>
    Array.from(new Set(this.offerings().map((o) => o.termNumber))).sort((a, b) => a - b));

  /** A row can belong to more than one cohort (see CourseOffering.cohortNames) when multiple
   *  intake years share one curriculum version — the filter still works fine against that, since
   *  a row simply matches if the selected cohort is among its (possibly multiple) names. */
  protected readonly selectedCohort = signal<string | 'ALL'>('ALL');
  protected readonly cohortOptions = computed(() =>
    Array.from(new Set(this.offerings().flatMap((o) => o.cohortNames))).sort());

  protected selectedAcademicYearId: number | null = null;
  protected selectedTermInstanceId: number | null = null;
  /** Only true when this screen was navigated to from the Academic Year detail/edit
   *  screen's "View Course Offerings" link (which passes academicYearId as a query param) —
   *  the back-to-academic-year button only makes sense in that context, not when the user
   *  opened Course Offerings directly from the Academics nav menu. */
  protected readonly cameFromAcademicYear = signal(false);

  protected readonly selectedTerm = computed(() =>
    this.termInstances().find((t) => t.id === this.selectedTermInstanceId) ?? null);

  protected canManage(): boolean {
    return this.permissionService.has('COURSE_MANAGE');
  }

  ngOnInit(): void {
    this.tourService.register('course-offering-list', COURSE_OFFERING_LIST_TOUR);
    this.tourService.registerFlowMap('course-offering-list', COURSE_OFFERING_LIST_FLOW_MAP);

    // MatTableDataSource's default sortingDataAccessor does row[sortHeaderId] — fine for columns
    // whose matColumnDef id matches a real CourseOffering property (subjectCode, subjectName,
    // termNumber), but 'cohort'/'status' are rendered from cohortNames/isActive, not fields of
    // those literal names, so the default accessor silently returned undefined for every row.
    this.dataSource.sortingDataAccessor = (row: CourseOffering, sortHeaderId: string) => {
      switch (sortHeaderId) {
        case 'cohort': return row.cohortNames.join(', ').toLowerCase();
        case 'status': return row.isActive ? 1 : 0;
        default: return (row as unknown as Record<string, string | number>)[sortHeaderId] ?? '';
      }
    };

    const qpAcademicYearId = Number(this.route.snapshot.queryParamMap.get('academicYearId')) || null;
    const qpTermInstanceId = Number(this.route.snapshot.queryParamMap.get('termInstanceId')) || null;
    // The "back to academic year" button only makes sense when we arrived via the Academic
    // Year detail screen's own "View Course Offerings" link.
    this.cameFromAcademicYear.set(this.route.snapshot.queryParamMap.has('academicYearId'));

    this.academicYearService.getAllAcademicYears().subscribe({
      next: (years) => {
        this.academicYears.set(years);
        const initialYearId = qpAcademicYearId
          ?? years.find((y) => y.isCurrent)?.id
          ?? years[0]?.id
          ?? null;
        if (initialYearId) {
          this.selectedAcademicYearId = initialYearId;
          this.loadTermInstances(initialYearId, qpTermInstanceId ?? undefined);
        }
      },
      error: () => { this.toast.error('Failed to load academic years'); },
    });
  }

  protected onAcademicYearChange(): void {
    this.selectedTermInstanceId = null;
    this.selectedSemester.set('ALL');
    this.selectedCohort.set('ALL');
    this.dataSource.data = [];
    if (this.selectedAcademicYearId) this.loadTermInstances(this.selectedAcademicYearId);
  }

  protected onTermChange(): void {
    this.selectedSemester.set('ALL');
    this.selectedCohort.set('ALL');
    if (this.selectedTermInstanceId) this.loadOfferings(this.selectedTermInstanceId);
    else this.dataSource.data = [];
  }

  protected onSemesterChange(): void {
    this.applyRowFilters();
  }

  protected onCohortChange(): void {
    this.applyRowFilters();
  }

  protected cohortLabel(row: CourseOffering): string {
    return row.cohortNames.length > 0 ? row.cohortNames.join(', ') : '—';
  }

  protected applyFilter(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchValue.set(value);
    this.dataSource.filter = value.trim().toLowerCase();
    if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
  }

  protected clearFilter(): void {
    this.searchValue.set('');
    this.dataSource.filter = '';
  }

  protected generateOfferings(): void {
    const termInstanceId = this.selectedTermInstanceId;
    if (!termInstanceId) return;
    this.generating.set(true);
    this.academicYearService.generateCourseOfferings(termInstanceId).subscribe({
      next: (res) => {
        this.reportGenerateResult(res);
        this.generating.set(false);
        this.loadOfferings(termInstanceId);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to generate offerings');
        this.generating.set(false);
      },
    });
  }

  /** "0 offering(s) generated" alone doesn't tell the user what to do next — surface the actual
   *  blocker(s) the backend detected (no active cohorts, cohorts missing a curriculum version,
   *  programs missing a total-terms value) or, if none of those apply, that everything already
   *  exists for this term. */
  private reportGenerateResult(res: GenerateCourseOfferingsResponse): void {
    if (res.activeCohortCount === 0) {
      this.toast.warning('No offerings generated — there are no active cohorts for this term.', { durationMs: 0 });
      return;
    }

    const blockers: string[] = [];
    if (res.cohortsWithoutCurriculumVersion.length > 0) {
      blockers.push(
        `${res.cohortsWithoutCurriculumVersion.length} cohort(s) have no active curriculum version assigned: ` +
        `${res.cohortsWithoutCurriculumVersion.join(', ')}. Assign one under Curriculum Versions first.`
      );
    }
    if (res.cohortsWithoutProgramTotalTerms > 0) {
      blockers.push(
        `${res.cohortsWithoutProgramTotalTerms} cohort(s)' programs have no total-terms/semesters value set.`
      );
    }
    if (res.subjectsWithoutFacultyPool.length > 0) {
      blockers.push(
        `${res.subjectsWithoutFacultyPool.length} subject(s) have a Speciality set but no active faculty eligible ` +
        `to teach them: ${res.subjectsWithoutFacultyPool.join(', ')}. Add a matching-Speciality faculty or add ` +
        `one to the subject's Eligible Faculty list before generating.`
      );
    }

    if (blockers.length > 0) {
      const prefix = res.offeringsCreated > 0 ? `${res.offeringsCreated} offering(s) generated, but: ` : 'No offerings generated — ';
      this.toast.warning(prefix + blockers.join(' '), { durationMs: 0 });
      return;
    }

    if (res.offeringsCreated > 0) {
      this.toast.success(`${res.offeringsCreated} offering(s) generated`);
      return;
    }

    if (res.offeringsAlreadyExisting > 0) {
      this.toast.info(`No new offerings — all ${res.offeringsAlreadyExisting} offering(s) for this term already exist.`);
      return;
    }

    this.toast.warning('No offerings generated — the assigned curriculum has no subjects mapped for this semester.', { durationMs: 0 });
  }

  /** Bidirectional — deactivating is blocked server-side (surfaced as an error toast, not a
   *  client-side guess) when the offering already has sessions placed in Skeleton Builder or
   *  batches with students rostered. Reactivating has no such restriction. */
  protected toggleStatus(row: CourseOffering): void {
    const nextAction = row.isActive ? 'Deactivate' : 'Activate';
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: `${nextAction} Course Offering`,
        message: row.isActive
          ? `Deactivate "${row.subjectName}" (Semester ${row.termNumber}) for this term? Existing student registrations are unaffected, but no further exam events can be scheduled against it.`
          : `Reactivate "${row.subjectName}" (Semester ${row.termNumber}) for this term?`,
        confirmText: nextAction,
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => { if (confirmed) this.performToggle(row); });
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
        if (preselect) this.loadOfferings(preselect);
        else this.dataSource.data = [];
      },
      error: () => { this.toast.error('Failed to load term instances'); this.termsLoading.set(false); },
    });
  }

  private loadOfferings(termInstanceId: number): void {
    this.loading.set(true);
    this.academicYearService.getCourseOfferingsByTermInstance(termInstanceId).subscribe({
      next: (data) => {
        this.offerings.set(data);
        this.applyRowFilters();
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load course offerings'); this.loading.set(false); },
    });
  }

  private applyRowFilters(): void {
    const semester = this.selectedSemester();
    const cohort = this.selectedCohort();
    let rows = this.offerings();
    if (semester !== 'ALL') rows = rows.filter((o) => o.termNumber === semester);
    if (cohort !== 'ALL') rows = rows.filter((o) => o.cohortNames.includes(cohort));
    this.dataSource.data = rows;
    if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
  }

  private performToggle(row: CourseOffering): void {
    this.loading.set(true);
    const termInstanceId = this.selectedTermInstanceId;
    const nextActive = !row.isActive;
    this.academicYearService.updateCourseOfferingStatus(row.id, { isActive: nextActive }).subscribe({
      next: () => {
        this.toast.success(`Course offering ${nextActive ? 'activated' : 'deactivated'}`);
        if (termInstanceId) this.loadOfferings(termInstanceId);
      },
      error: (err) => {
        this.toast.error(violationText(err) ?? 'Failed to update status');
        this.loading.set(false);
      },
    });
  }
}

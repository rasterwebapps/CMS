import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatTableModule, MatTableDataSource, MatTable } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { environment } from '../../../environments/environment';
import { AcademicYearService } from '../academic-year/academic-year.service';
import { AcademicYear, CourseOffering, CourseOfferingFacultySummary, OfferingAssignmentStatus, TermInstance } from '../academic-year/academic-year.model';
import { CmsEmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { CmsRowActionButtonComponent } from '../../shared/row-action-button/row-action-button.component';
import { CmsStatusBadgeComponent } from '../../shared/status-badge/status-badge.component';
import { CmsIconEditComponent } from '../../shared/icons';
import { ColumnPickerState, CmsColumnPickerComponent } from '../../shared/column-picker';
import { ColumnResizeDirective, CmsWrapTextToggleComponent } from '../../shared/column-resize';
import { PermissionService } from '../../core/permissions/permission.service';
import { ToastService } from '../../core/toast/toast.service';
import { TourService } from '../../shared/tour/tour.service';
import { CmsTourButtonComponent } from '../../shared/tour/tour-button.component';
import { ASSIGN_FACULTY_TOUR, ASSIGN_FACULTY_FLOW_MAP } from '../../shared/tour/tours/assign-faculty.tours';
import { FacultyOption } from '../course-offering/course-offering-edit-dialog/course-offering-edit-dialog.component';
import {
  TeachingAssignmentDialogComponent,
  TeachingAssignmentDialogData,
} from './teaching-assignment-dialog/teaching-assignment-dialog.component';
import {
  ClassInchargeDialogComponent,
  ClassInchargeDialogData,
} from './class-incharge-dialog/class-incharge-dialog.component';
import {
  ClinicalShiftGroupDialogComponent,
  ClinicalShiftGroupDialogData,
} from '../course-offering/clinical-shift-group-dialog/clinical-shift-group-dialog.component';

/** Sort rank for the Assigned column — least-complete first, so a term needing attention floats to
 *  the top; NOT_APPLICABLE (nothing to assign) sorts last, alongside FULL, since neither needs the
 *  admin's attention. */
const ASSIGNMENT_STATUS_SORT_RANK: Record<OfferingAssignmentStatus, number> = {
  NONE: 0,
  PARTIAL: 1,
  FULL: 2,
  NOT_APPLICABLE: 3,
};

/**
 * Deliberately separate from Course Offerings: generating/deactivating/batching an offering is a
 * structural/lifecycle concern, while deciding WHO teaches it is a staffing concern that happens
 * later — after Generate Offerings and after Elective Assignment tells you which elective options
 * actually have real enrolled students. "Assign Faculty" opens TeachingAssignmentDialogComponent,
 * covering both Theory (Section Faculty) and Lab/Clinical batch coordinator assignment in one
 * dialog — previously two separate buttons/dialogs, which meant coordinators routinely went
 * unassigned since that half lived behind the less-visible "Manage Batches" button.
 */
@Component({
  selector: 'app-assign-faculty-list',
  standalone: true,
  imports: [
    FormsModule, MatTableModule, MatPaginatorModule, MatSortModule,
    MatProgressSpinnerModule, MatDialogModule,
    CmsEmptyStateComponent, CmsRowActionButtonComponent, CmsStatusBadgeComponent, CmsIconEditComponent,
    CmsColumnPickerComponent, ColumnResizeDirective, CmsWrapTextToggleComponent, CmsTourButtonComponent,
  ],
  templateUrl: './assign-faculty-list.component.html',
  styleUrl: './assign-faculty-list.component.scss',
})
export class AssignFacultyListComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly http = inject(HttpClient);
  private readonly route = inject(ActivatedRoute);
  private readonly permissionService = inject(PermissionService);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly tourService = inject(TourService);

  @ViewChild(MatTable) private _matTable?: MatTable<unknown>;
  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  protected readonly colState = new ColumnPickerState({
    storageKey: 'assign-faculty-list-cols',
    columns: [
      { key: 'subjectCode', label: 'Code' },
      { key: 'subjectName', label: 'Subject', mandatory: true },
      { key: 'termNumber', label: 'Semester' },
      { key: 'cohort', label: 'Cohort' },
      { key: 'faculty', label: 'Faculty' },
      { key: 'assignmentStatus', label: 'Assigned' },
      { key: 'status', label: 'Status' },
      { key: 'actions', label: 'Actions', mandatory: true, pinnable: false },
    ],
  });
  protected readonly displayedColumns = computed(() => this.colState.visibleColumns());
  protected readonly dataSource = new MatTableDataSource<CourseOffering>([]);
  protected readonly loading = signal(false);
  protected readonly termsLoading = signal(false);
  protected readonly searchValue = signal('');

  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly termInstances = signal<TermInstance[]>([]);
  protected readonly faculty = signal<FacultyOption[]>([]);

  /** Assignment summary per offering, keyed by offering id — present for every offering in the
   *  term now (see backend CourseOfferingFacultySummaryDto). Assignment is per-cohort (per-section,
   *  if split) now, so a single offering can list more than one Theory faculty name when it's
   *  shared by more than one cohort. */
  private readonly facultySummaryByOfferingId = signal<Map<number, CourseOfferingFacultySummary>>(new Map());

  private readonly offerings = signal<CourseOffering[]>([]);
  protected readonly selectedSemester = signal<number | 'ALL'>('ALL');
  protected readonly semesterOptions = computed(() =>
    Array.from(new Set(this.offerings().map((o) => o.termNumber))).sort((a, b) => a - b));

  protected readonly selectedCohort = signal<string | 'ALL'>('ALL');
  protected readonly cohortOptions = computed(() =>
    Array.from(new Set(this.offerings().flatMap((o) => o.cohortNames))).sort());

  protected selectedAcademicYearId: number | null = null;
  protected selectedTermInstanceId: number | null = null;

  /** Set from an `editOfferingId`/`suggestedFacultyId` deep link (e.g. Skeleton Builder's Global
   *  Auto-Schedule capacity report suggesting "move this offering to faculty X") — consumed once,
   *  the first time this offering's term finishes loading, to auto-open its Assign Faculty dialog
   *  with that faculty pre-selected. Never applied without the admin confirming Save themselves. */
  private pendingDeepLinkEdit: { offeringId: number; suggestedFacultyId: number | null } | null = null;

  protected readonly selectedTerm = computed(() =>
    this.termInstances().find((t) => t.id === this.selectedTermInstanceId) ?? null);

  protected onPinChange(): void { this._matTable?.updateStickyColumnStyles(); }

  protected canManage(): boolean {
    return this.permissionService.has('COURSE_MANAGE');
  }

  /** Manage Batches hits BatchController endpoints gated on BATCH_MANAGE (see V273), not
   *  COURSE_MANAGE -- a role holding one but not the other must not see a button that then
   *  403s on every action inside the dialog. */
  protected canManageBatches(): boolean {
    return this.permissionService.has('BATCH_MANAGE');
  }

  /** Assign Faculty now covers Theory AND Lab/Clinical coordinator assignment in one dialog (was
   *  two separate buttons/dialogs -- coordinators went unassigned because that half lived behind
   *  "Manage Batches" and admins never opened it). Shown if the user holds either permission; the
   *  dialog itself greys out whichever half they lack, same pattern as everywhere else two
   *  differently-permissioned actions share one surface. */
  protected canAssignFaculty(): boolean {
    return this.canManage() || this.canManageBatches();
  }

  protected canViewClassIncharge(): boolean {
    return this.permissionService.has('CLASS_INCHARGE_VIEW');
  }

  protected canManageClinicalShift(): boolean {
    return this.permissionService.has('TIMETABLE_CLINICAL_SHIFT_MANAGE');
  }

  ngOnInit(): void {
    this.tourService.register('assign-faculty', ASSIGN_FACULTY_TOUR);
    this.tourService.registerFlowMap('assign-faculty', ASSIGN_FACULTY_FLOW_MAP);

    // Same client-side sort-accessor fix as Course Offerings — 'faculty'/'status'/'assignmentStatus'
    // are rendered from facultyId/isActive/a computed summary, not fields of those literal names.
    this.dataSource.sortingDataAccessor = (row: CourseOffering, sortHeaderId: string) => {
      switch (sortHeaderId) {
        case 'cohort': return row.cohortNames.join(', ').toLowerCase();
        case 'faculty': return this.facultySummaryText(row).toLowerCase();
        case 'status': return row.isActive ? 1 : 0;
        case 'assignmentStatus': return ASSIGNMENT_STATUS_SORT_RANK[this.assignmentStatusFor(row)];
        default: return (row as unknown as Record<string, string | number>)[sortHeaderId] ?? '';
      }
    };

    this.http.get<{ id: number; fullName: string; specialityId: number | null }[]>(`${environment.apiUrl}/faculty`).subscribe({
      next: (data) => this.faculty.set(data.map((f) => ({ id: f.id, name: f.fullName, specialityId: f.specialityId }))),
      error: () => { this.toast.error('Failed to load faculty'); },
    });

    const qpAcademicYearId = Number(this.route.snapshot.queryParamMap.get('academicYearId')) || null;
    const qpTermInstanceId = Number(this.route.snapshot.queryParamMap.get('termInstanceId')) || null;
    const qpEditOfferingId = Number(this.route.snapshot.queryParamMap.get('editOfferingId')) || null;
    const qpSuggestedFacultyId = Number(this.route.snapshot.queryParamMap.get('suggestedFacultyId')) || null;
    if (qpEditOfferingId) {
      this.pendingDeepLinkEdit = { offeringId: qpEditOfferingId, suggestedFacultyId: qpSuggestedFacultyId };
    }

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

  protected assignFaculty(row: CourseOffering, suggestedFacultyId: number | null = null): void {
    const data: TeachingAssignmentDialogData = {
      offering: row,
      suggestedFacultyId,
    };
    this.dialog.open(TeachingAssignmentDialogComponent, { data, width: '1100px', maxWidth: '95vw' })
      .afterClosed().subscribe(() => {
        // Reload unconditionally, not just when the dialog reports a change -- every pick inside
        // saves immediately regardless of how the dialog is dismissed (Close button, backdrop
        // click, Escape), so there's no reliable "nothing changed" signal to gate on. No spinner:
        // keeps the table mounted so the user's current sort/column state survives the refresh.
        if (this.selectedTermInstanceId) this.loadOfferings(this.selectedTermInstanceId, false);
      });
  }

  /** OC-175, merged with the former standalone "Clinical Shift Config" dialog per OC-187: manage
   *  recurring off-campus clinical shift windows for this offering -- several clinical Batches
   *  (different venues) sharing one shift + shared reconvened theory block. Duration/buffer are
   *  configured inline, the first time they're needed, right inside this same dialog. */
  protected manageClinicalShiftGroups(row: CourseOffering): void {
    const data: ClinicalShiftGroupDialogData = { offering: row };
    this.dialog.open(ClinicalShiftGroupDialogComponent, { data, width: '620px' })
      .afterClosed().subscribe(() => {
        if (this.selectedTermInstanceId) this.loadOfferings(this.selectedTermInstanceId, false);
      });
  }

  /** Class Incharge isn't tied to any one subject/offering, so it's a standalone term-wide action
   *  rather than a per-row one — every committed CohortSection across every cohort in the
   *  selected term, not just this screen's currently-filtered offerings. */
  protected openClassIncharge(): void {
    const term = this.selectedTerm();
    if (!this.selectedTermInstanceId || !term) return;
    const data: ClassInchargeDialogData = {
      termInstanceId: this.selectedTermInstanceId,
      termLabel: `${term.academicYearName} · ${term.termType} Term`,
      facultyOptions: this.faculty(),
    };
    this.dialog.open(ClassInchargeDialogComponent, { data, width: '560px' });
  }

  /** Comma-joined assigned Theory faculty names for this offering's row, or "Unassigned" if it has
   *  no assignment rows at all yet — open the Assign Faculty dialog for the per-cohort/section
   *  detail. */
  protected facultySummaryText(row: CourseOffering): string {
    const names = this.facultySummaryByOfferingId().get(row.id)?.assignedFacultyNames;
    return names && names.length > 0 ? names.join(', ') : 'Unassigned';
  }

  /** Whether every Theory row and Lab/Clinical batch coordinator this offering needs is actually
   *  filled — covers more than facultySummaryText (which is Theory-only). Falls back to
   *  NOT_APPLICABLE (renders as a dash, no badge) if the summary hasn't loaded yet. */
  protected assignmentStatusFor(row: CourseOffering): OfferingAssignmentStatus {
    return this.facultySummaryByOfferingId().get(row.id)?.assignmentStatus ?? 'NOT_APPLICABLE';
  }

  protected assignmentStatusLabel(status: OfferingAssignmentStatus): string {
    switch (status) {
      case 'FULL': return 'Fully Assigned';
      case 'PARTIAL': return 'Partial';
      case 'NONE': return 'Unassigned';
      case 'NOT_APPLICABLE': return '—';
    }
  }

  protected assignmentStatusBadgeClass(status: OfferingAssignmentStatus): string {
    switch (status) {
      case 'FULL': return 'cms-badge--green';
      case 'PARTIAL': return 'cms-badge--amber';
      case 'NONE': return 'cms-badge--red';
      case 'NOT_APPLICABLE': return '';
    }
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

  // showSpinner defaults true for a genuine fresh load (term/year switch). A dialog-close
  // refresh passes false: the spinner branch in the template unmounts the whole <table>, which
  // destroys and recreates MatSort/MatPaginator from scratch -- silently discarding whatever
  // column the user had sorted by (looked like a full page reload resetting the sort).
  private loadOfferings(termInstanceId: number, showSpinner = true): void {
    if (showSpinner) this.loading.set(true);
    this.academicYearService.getCourseOfferingsByTermInstance(termInstanceId).subscribe({
      next: (data) => {
        this.offerings.set(data);
        this.applyRowFilters();
        if (showSpinner) this.loading.set(false);
        this.consumePendingDeepLinkEdit(data);
      },
      error: () => { this.toast.error('Failed to load course offerings'); if (showSpinner) this.loading.set(false); },
    });

    this.academicYearService.getFacultyAssignmentSummary(termInstanceId).subscribe({
      next: (summaries) => {
        this.facultySummaryByOfferingId.set(new Map(summaries.map((s) => [s.offeringId, s])));
      },
      error: () => { /* non-fatal — column falls back to "Unassigned" for every row */ },
    });
  }

  private consumePendingDeepLinkEdit(data: CourseOffering[]): void {
    const pending = this.pendingDeepLinkEdit;
    if (!pending) return;
    this.pendingDeepLinkEdit = null; // one-shot — only auto-opens on the first term load after arrival
    const row = data.find((o) => o.id === pending.offeringId);
    if (row) this.assignFaculty(row, pending.suggestedFacultyId);
    else this.toast.warning('Could not find the requested course offering — it may be in a different term.');
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
}

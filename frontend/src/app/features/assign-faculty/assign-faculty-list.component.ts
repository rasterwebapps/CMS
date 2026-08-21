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
import { AcademicYear, CourseOffering, TermInstance } from '../academic-year/academic-year.model';
import { CmsEmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { CmsRowActionButtonComponent } from '../../shared/row-action-button/row-action-button.component';
import { CmsStatusBadgeComponent } from '../../shared/status-badge/status-badge.component';
import { CmsIconEditComponent } from '../../shared/icons';
import { PermissionService } from '../../core/permissions/permission.service';
import { ToastService } from '../../core/toast/toast.service';
import {
  CourseOfferingEditDialogComponent,
  CourseOfferingEditDialogData,
  FacultyOption,
} from '../course-offering/course-offering-edit-dialog/course-offering-edit-dialog.component';
import {
  BatchManageDialogComponent,
  BatchManageDialogData,
} from '../course-offering/batch-manage-dialog/batch-manage-dialog.component';

/**
 * Deliberately separate from Course Offerings: generating/deactivating/batching an offering is a
 * structural/lifecycle concern, while deciding WHO teaches it is a staffing concern that happens
 * later — after Generate Offerings and after Elective Assignment tells you which elective options
 * actually have real enrolled students. Same underlying data and dialog (CourseOfferingEditDialogComponent,
 * COURSE_MANAGE permission, live capacity check, Section Faculty) as when this lived inside the
 * Course Offerings screen — only the entry point moved.
 */
@Component({
  selector: 'app-assign-faculty-list',
  standalone: true,
  imports: [
    FormsModule, MatTableModule, MatPaginatorModule, MatSortModule,
    MatProgressSpinnerModule, MatDialogModule,
    CmsEmptyStateComponent, CmsRowActionButtonComponent, CmsStatusBadgeComponent, CmsIconEditComponent,
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

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  protected readonly displayedColumns = ['subjectCode', 'subjectName', 'termNumber', 'cohort', 'faculty', 'secondaryFaculty', 'status', 'actions'];
  protected readonly dataSource = new MatTableDataSource<CourseOffering>([]);
  protected readonly loading = signal(false);
  protected readonly termsLoading = signal(false);
  protected readonly searchValue = signal('');

  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly termInstances = signal<TermInstance[]>([]);
  protected readonly faculty = signal<FacultyOption[]>([]);
  private readonly facultyById = computed(() => new Map(this.faculty().map((f) => [f.id, f.name])));

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

  protected canManage(): boolean {
    return this.permissionService.has('COURSE_MANAGE');
  }

  ngOnInit(): void {
    // Same client-side sort-accessor fix as Course Offerings — 'faculty'/'secondaryFaculty'/'status'
    // are rendered from facultyId/secondaryFacultyId/isActive, not fields of those literal names.
    this.dataSource.sortingDataAccessor = (row: CourseOffering, sortHeaderId: string) => {
      switch (sortHeaderId) {
        case 'cohort': return row.cohortNames.join(', ').toLowerCase();
        case 'faculty': return this.facultyName(row.facultyId).toLowerCase();
        case 'secondaryFaculty': return row.secondaryFacultyId ? this.facultyName(row.secondaryFacultyId).toLowerCase() : '';
        case 'status': return row.isActive ? 1 : 0;
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
    const data: CourseOfferingEditDialogData = {
      offering: row,
      facultyOptions: this.faculty(),
      suggestedFacultyId,
    };
    this.dialog.open(CourseOfferingEditDialogComponent, { data, width: '480px' })
      .afterClosed().subscribe((updated) => {
        if (updated && this.selectedTermInstanceId) this.loadOfferings(this.selectedTermInstanceId);
      });
  }

  /** Moved here from Course Offerings: creating batches (name/venue/headcount) happens in
   *  Capacity Planner as part of committing a room allocation, but assigning a batch's
   *  coordinator faculty and its student roster is a staffing concern, not a structural one. */
  protected manageBatches(row: CourseOffering): void {
    const data: BatchManageDialogData = {
      offering: row,
      facultyOptions: this.faculty(),
    };
    this.dialog.open(BatchManageDialogComponent, { data, width: '560px' });
  }

  protected facultyName(id: number | null): string {
    if (id == null) return 'Unassigned';
    return this.facultyById().get(id) ?? 'Unassigned';
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
        this.consumePendingDeepLinkEdit(data);
      },
      error: () => { this.toast.error('Failed to load course offerings'); this.loading.set(false); },
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

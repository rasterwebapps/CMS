import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatTableModule, MatTableDataSource, MatTable } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { environment } from '../../../../environments/environment';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear, CourseOffering, TermInstance } from '../../academic-year/academic-year.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { CmsIconDeleteComponent, CmsIconEditComponent } from '../../../shared/icons';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';
import {
  CourseOfferingEditDialogComponent,
  CourseOfferingEditDialogData,
  FacultyOption,
} from '../course-offering-edit-dialog/course-offering-edit-dialog.component';
import {
  BatchManageDialogComponent,
  BatchManageDialogData,
} from '../batch-manage-dialog/batch-manage-dialog.component';

@Component({
  selector: 'app-course-offering-list',
  standalone: true,
  imports: [
    RouterLink, FormsModule, MatTableModule, MatPaginatorModule, MatSortModule,
    MatProgressSpinnerModule, MatDialogModule, MatTooltipModule,
    CmsEmptyStateComponent, CmsRowActionButtonComponent, CmsStatusBadgeComponent,
    CmsIconDeleteComponent, CmsIconEditComponent,
  ],
  templateUrl: './course-offering-list.component.html',
  styleUrl: './course-offering-list.component.scss',
})
export class CourseOfferingListComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly http = inject(HttpClient);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly permissionService = inject(PermissionService);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  protected readonly displayedColumns = [
    'subjectCode', 'subjectName', 'semesterNumber', 'faculty', 'sectionLabel', 'status', 'actions',
  ];
  protected readonly dataSource = new MatTableDataSource<CourseOffering>([]);
  protected readonly loading = signal(false);
  protected readonly generating = signal(false);
  protected readonly termsLoading = signal(false);
  protected readonly searchValue = signal('');

  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly termInstances = signal<TermInstance[]>([]);
  protected readonly faculty = signal<FacultyOption[]>([]);
  private readonly facultyById = computed(() => new Map(this.faculty().map((f) => [f.id, f.name])));

  protected selectedAcademicYearId: number | null = null;
  protected selectedTermInstanceId: number | null = null;

  protected readonly selectedTerm = computed(() =>
    this.termInstances().find((t) => t.id === this.selectedTermInstanceId) ?? null);

  protected canManage(): boolean {
    return this.permissionService.has('COURSE_MANAGE');
  }

  protected canAssignElectives(): boolean {
    return this.permissionService.has('COURSE_REGISTRATION_ELECTIVE_ASSIGN');
  }

  protected goToElectiveAssignment(): void {
    if (!this.selectedTermInstanceId) return;
    void this.router.navigate(['/elective-assignment'], {
      queryParams: { termInstanceId: this.selectedTermInstanceId }
    });
  }

  ngOnInit(): void {
    this.http.get<{ id: number; fullName: string }[]>(`${environment.apiUrl}/faculty`).subscribe({
      next: (data) => this.faculty.set(data.map((f) => ({ id: f.id, name: f.fullName }))),
      error: () => { this.toast.error('Failed to load faculty'); },
    });

    const qpAcademicYearId = Number(this.route.snapshot.queryParamMap.get('academicYearId')) || null;
    const qpTermInstanceId = Number(this.route.snapshot.queryParamMap.get('termInstanceId')) || null;

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
    this.dataSource.data = [];
    if (this.selectedAcademicYearId) this.loadTermInstances(this.selectedAcademicYearId);
  }

  protected onTermChange(): void {
    if (this.selectedTermInstanceId) this.loadOfferings(this.selectedTermInstanceId);
    else this.dataSource.data = [];
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
        this.toast.success(`${res.offeringsCreated} offering(s) generated`);
        this.generating.set(false);
        this.loadOfferings(termInstanceId);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to generate offerings');
        this.generating.set(false);
      },
    });
  }

  protected edit(row: CourseOffering): void {
    const data: CourseOfferingEditDialogData = {
      offering: row,
      facultyOptions: this.faculty(),
    };
    this.dialog.open(CourseOfferingEditDialogComponent, { data, width: '480px' })
      .afterClosed().subscribe((updated) => {
        if (updated && this.selectedTermInstanceId) this.loadOfferings(this.selectedTermInstanceId);
      });
  }

  protected manageBatches(row: CourseOffering): void {
    const data: BatchManageDialogData = {
      offering: row,
      facultyOptions: this.faculty(),
    };
    this.dialog.open(BatchManageDialogComponent, { data, width: '560px' });
  }

  protected deactivate(row: CourseOffering): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Deactivate Course Offering',
        message: `Deactivate "${row.subjectName}" (Semester ${row.semesterNumber}) for this term? Existing student registrations are unaffected, but no further exam events can be scheduled against it.`,
        confirmText: 'Deactivate',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => { if (confirmed) this.doDeactivate(row); });
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
      next: (data) => { this.dataSource.data = data; this.loading.set(false); },
      error: () => { this.toast.error('Failed to load course offerings'); this.loading.set(false); },
    });
  }

  private doDeactivate(row: CourseOffering): void {
    this.loading.set(true);
    const termInstanceId = this.selectedTermInstanceId;
    this.academicYearService.deactivateCourseOffering(row.id).subscribe({
      next: () => {
        this.toast.success('Course offering deactivated');
        if (termInstanceId) this.loadOfferings(termInstanceId);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to deactivate');
        this.loading.set(false);
      },
    });
  }
}

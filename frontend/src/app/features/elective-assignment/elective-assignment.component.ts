import { Component, inject, OnInit, signal, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { AcademicYearService } from '../academic-year/academic-year.service';
import {
  AcademicYear,
  TermInstance,
  StudentTermEnrollment,
  CourseOffering,
  ElectiveSelectionMode,
} from '../academic-year/academic-year.model';
import { CmsEmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog/confirm-dialog.component';
import { ToastService } from '../../core/toast/toast.service';
import { PermissionService } from '../../core/permissions/permission.service';
import { SkeletonBuilderService } from '../timetable/skeleton-builder/skeleton-builder.service';
import { ElectiveGroupScheduleResponse } from '../timetable/skeleton-builder/skeleton-builder.model';
import { TourService } from '../../shared/tour/tour.service';
import { CmsTourButtonComponent } from '../../shared/tour/tour-button.component';
import { ELECTIVE_ASSIGNMENT_TOUR, ELECTIVE_ASSIGNMENT_FLOW_MAP } from '../../shared/tour/tours/elective-assignment.tours';

interface ElectiveGroupOption {
  electiveGroupId: number;
  electiveGroupName: string;
  curriculumVersionId: number;
  termNumber: number;
  selectionMode: ElectiveSelectionMode;
}

interface AssignmentRow {
  enrollment: StudentTermEnrollment;
  currentChoiceOfferingId: number | null;
  currentChoiceLabel: string | null;
}

@Component({
  selector: 'app-elective-assignment',
  standalone: true,
  imports: [
    FormsModule, RouterLink, MatTableModule, MatPaginatorModule, MatSortModule,
    MatProgressSpinnerModule, MatDialogModule, CmsEmptyStateComponent,
    CmsTourButtonComponent,
  ],
  templateUrl: './elective-assignment.component.html',
  styleUrl: './elective-assignment.component.scss',
})
export class ElectiveAssignmentComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly skeletonBuilderService = inject(SkeletonBuilderService);
  private readonly route = inject(ActivatedRoute);
  private readonly toast = inject(ToastService);
  private readonly permissionService = inject(PermissionService);
  private readonly dialog = inject(MatDialog);
  private readonly tourService = inject(TourService);

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  protected readonly displayedColumns = ['studentName', 'cohortCode', 'currentChoice', 'action'];
  protected readonly dataSource = new MatTableDataSource<AssignmentRow>([]);

  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly termInstances = signal<TermInstance[]>([]);
  protected readonly electiveGroupOptions = signal<ElectiveGroupOption[]>([]);
  protected readonly offeringOptions = signal<CourseOffering[]>([]);
  protected readonly loading = signal(false);
  protected readonly assigning = signal<number | null>(null);
  protected readonly selectedOfferingByEnrollment = new Map<number, number>();
  protected readonly scheduleStatus = signal<ElectiveGroupScheduleResponse | null>(null);

  protected readonly bulkOfferingId = signal<number | null>(null);
  protected readonly bulkAssigning = signal(false);
  protected readonly modeSaving = signal(false);

  protected selectedAcademicYearId: number | null = null;
  protected selectedTermInstanceId: number | null = null;
  protected selectedElectiveGroupId: number | null = null;

  // Plain methods, not computed() — computed() only re-evaluates on signal-dependency
  // changes, but selectedElectiveGroupId is a plain field driven by [(ngModel)], so a
  // computed() here would cache its first result and never notice the field changing.
  protected selectedGroup(): ElectiveGroupOption | null {
    return this.electiveGroupOptions().find((g) => g.electiveGroupId === this.selectedElectiveGroupId) ?? null;
  }

  protected isInstitutionDecided(): boolean {
    return this.selectedGroup()?.selectionMode === 'INSTITUTION_DECIDED';
  }

  protected canManageElectiveGroups(): boolean {
    return this.permissionService.has('CURRICULUM_ELECTIVE_GROUP_MANAGE');
  }

  ngOnInit(): void {
    this.tourService.register('elective-assignment', ELECTIVE_ASSIGNMENT_TOUR);
    this.tourService.registerFlowMap('elective-assignment', ELECTIVE_ASSIGNMENT_FLOW_MAP);

    // AssignmentRow nests the real values under `enrollment` — MatTableDataSource's default
    // sortingDataAccessor only reads a shallow row[sortHeaderId], so without this override
    // clicking the Student/Cohort column headers silently sorts nothing (every row reads as
    // the same undefined value).
    this.dataSource.sortingDataAccessor = (row, sortHeaderId) => {
      switch (sortHeaderId) {
        case 'studentName': return row.enrollment.studentName;
        case 'cohortCode': return row.enrollment.cohortCode;
        default: return '';
      }
    };

    const qpTermInstanceId = Number(this.route.snapshot.queryParamMap.get('termInstanceId')) || null;

    this.academicYearService.getAllAcademicYears().subscribe({
      next: (years) => {
        this.academicYears.set(years);
        const initialYearId = years.find((y) => y.isCurrent)?.id ?? years[0]?.id ?? null;
        if (initialYearId) {
          this.selectedAcademicYearId = initialYearId;
          this.loadTermInstances(initialYearId, qpTermInstanceId ?? undefined);
        }
      },
      error: () => this.toast.error('Failed to load academic years'),
    });
  }

  protected onAcademicYearChange(): void {
    this.selectedTermInstanceId = null;
    this.resetGroupState();
    if (this.selectedAcademicYearId) this.loadTermInstances(this.selectedAcademicYearId);
  }

  protected onTermChange(): void {
    this.resetGroupState();
    if (this.selectedTermInstanceId) this.loadElectiveGroupOptions(this.selectedTermInstanceId);
  }

  protected onGroupChange(): void {
    this.dataSource.data = [];
    this.scheduleStatus.set(null);
    this.bulkOfferingId.set(null);
    const termInstanceId = this.selectedTermInstanceId;
    const group = this.selectedGroup();
    if (!termInstanceId || !group) return;

    this.loading.set(true);
    this.academicYearService.getElectiveOfferingOptions(termInstanceId, group.electiveGroupId).subscribe({
      next: (offerings) => {
        this.offeringOptions.set(offerings);
        this.loadEnrollments(termInstanceId, group);
      },
      error: () => { this.toast.error('Failed to load elective options'); this.loading.set(false); },
    });

    this.skeletonBuilderService.getElectiveGroupSchedule(group.electiveGroupId, termInstanceId).subscribe({
      next: (status) => this.scheduleStatus.set(status),
      error: () => this.scheduleStatus.set(null),
    });
  }

  protected assign(row: AssignmentRow): void {
    const offeringId = this.selectedOfferingByEnrollment.get(row.enrollment.id);
    if (!offeringId) return;
    this.assigning.set(row.enrollment.id);
    this.academicYearService.assignElectiveChoice(row.enrollment.id, offeringId).subscribe({
      next: () => {
        this.toast.success('Elective assigned');
        this.assigning.set(null);
        const termInstanceId = this.selectedTermInstanceId;
        const group = this.selectedGroup();
        if (termInstanceId && group) this.loadEnrollments(termInstanceId, group);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to assign elective');
        this.assigning.set(null);
      },
    });
  }

  protected onSelectOffering(enrollmentId: number, event: Event): void {
    const value = Number((event.target as HTMLSelectElement).value);
    if (value) this.selectedOfferingByEnrollment.set(enrollmentId, value);
    else this.selectedOfferingByEnrollment.delete(enrollmentId);
  }

  protected onBulkOfferingSelect(event: Event): void {
    const value = Number((event.target as HTMLSelectElement).value);
    this.bulkOfferingId.set(value || null);
  }

  protected applyToAll(): void {
    const termInstanceId = this.selectedTermInstanceId;
    const group = this.selectedGroup();
    const offeringId = this.bulkOfferingId();
    if (!termInstanceId || !group || !offeringId) return;
    const offering = this.offeringOptions().find((o) => o.id === offeringId);
    const offeringLabel = offering ? `${offering.subjectCode} — ${offering.subjectName}` : 'the selected offering';

    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Apply to All Students',
        message: `Assign every eligible student in "${group.electiveGroupName}" to "${offeringLabel}"? ` +
          `This overwrites any choice a student already has in this group — it cannot be undone from here.`,
        confirmText: 'Apply to All',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) this.doApplyToAll(termInstanceId, group.electiveGroupId, offeringId);
    });
  }

  private doApplyToAll(termInstanceId: number, electiveGroupId: number, offeringId: number): void {
    this.bulkAssigning.set(true);
    this.academicYearService.bulkAssignElectiveChoice(termInstanceId, electiveGroupId, offeringId).subscribe({
      next: (res) => {
        this.toast.success(`Assigned ${res.assignedCount} of ${res.eligibleStudentCount} eligible student(s)`);
        this.bulkAssigning.set(false);
        const group = this.selectedGroup();
        if (group) this.loadEnrollments(termInstanceId, group);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to apply to all');
        this.bulkAssigning.set(false);
      },
    });
  }

  protected setSelectionMode(mode: ElectiveSelectionMode): void {
    const group = this.selectedGroup();
    if (!group || group.selectionMode === mode) return;
    this.modeSaving.set(true);
    this.academicYearService.updateElectiveGroupSelectionMode(group.electiveGroupId, mode).subscribe({
      next: () => {
        this.electiveGroupOptions.update((groups) =>
          groups.map((g) => g.electiveGroupId === group.electiveGroupId ? { ...g, selectionMode: mode } : g));
        this.bulkOfferingId.set(null);
        this.toast.success('Selection mode updated');
        this.modeSaving.set(false);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to update selection mode');
        this.modeSaving.set(false);
      },
    });
  }

  private resetGroupState(): void {
    this.selectedElectiveGroupId = null;
    this.electiveGroupOptions.set([]);
    this.offeringOptions.set([]);
    this.dataSource.data = [];
    this.scheduleStatus.set(null);
    this.bulkOfferingId.set(null);
  }

  private loadTermInstances(academicYearId: number, preselectTermInstanceId?: number): void {
    this.academicYearService.getTermInstancesByAcademicYear(academicYearId).subscribe({
      next: (terms) => {
        this.termInstances.set(terms);
        const preselect = preselectTermInstanceId && terms.some((t) => t.id === preselectTermInstanceId)
          ? preselectTermInstanceId
          : terms[0]?.id ?? null;
        this.selectedTermInstanceId = preselect;
        if (preselect) this.loadElectiveGroupOptions(preselect);
      },
      error: () => this.toast.error('Failed to load term instances'),
    });
  }

  private loadElectiveGroupOptions(termInstanceId: number): void {
    this.loading.set(true);
    this.academicYearService.getCourseOfferingsByTermInstance(termInstanceId).subscribe({
      next: (offerings) => {
        const electiveOfferings = offerings.filter((o) => o.isElective && o.electiveGroupId);
        const groupsById = new Map<number, ElectiveGroupOption>();
        for (const o of electiveOfferings) {
          if (!groupsById.has(o.electiveGroupId!)) {
            groupsById.set(o.electiveGroupId!, {
              electiveGroupId: o.electiveGroupId!,
              electiveGroupName: o.electiveGroupName ?? `Group ${o.electiveGroupId}`,
              curriculumVersionId: o.curriculumVersionId,
              termNumber: o.termNumber,
              selectionMode: o.electiveGroupSelectionMode ?? 'STUDENT_CHOICE',
            });
          }
        }
        this.electiveGroupOptions.set(Array.from(groupsById.values()));
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load elective groups for this term'); this.loading.set(false); },
    });
  }

  private loadEnrollments(termInstanceId: number, group: ElectiveGroupOption): void {
    this.loading.set(true);
    this.academicYearService.getEnrollmentsByElectiveGroup(termInstanceId, group.electiveGroupId).subscribe({
      next: (relevant) => {
        if (relevant.length === 0) {
          this.dataSource.data = [];
          this.loading.set(false);
          return;
        }
        let pending = relevant.length;
        const rows: AssignmentRow[] = [];
        const optionIds = new Set(this.offeringOptions().map((o) => o.id));
        for (const enrollment of relevant) {
          this.academicYearService.getCourseRegistrationsByEnrollment(enrollment.id).subscribe({
            next: (registrations) => {
              const current = registrations.find(
                (r) => optionIds.has(r.courseOfferingId) && r.status !== 'DROPPED');
              rows.push({
                enrollment,
                currentChoiceOfferingId: current?.courseOfferingId ?? null,
                currentChoiceLabel: current ? `${current.subjectCode} — ${current.subjectName}` : null,
              });
              pending--;
              if (pending === 0) {
                this.dataSource.data = rows;
                this.loading.set(false);
              }
            },
            error: () => {
              pending--;
              if (pending === 0) {
                this.dataSource.data = rows;
                this.loading.set(false);
              }
            },
          });
        }
      },
      error: () => { this.toast.error('Failed to load enrollments'); this.loading.set(false); },
    });
  }
}

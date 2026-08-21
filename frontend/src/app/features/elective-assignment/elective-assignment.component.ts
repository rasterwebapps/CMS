import { Component, inject, OnInit, signal, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
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
  ElectiveGroupSummary,
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
    FormsModule, MatTableModule, MatPaginatorModule, MatSortModule,
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

  protected readonly displayedColumns = ['rollNumber', 'studentName', 'cohortCode', 'currentChoice', 'action'];
  protected readonly dataSource = new MatTableDataSource<AssignmentRow>([]);

  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly termInstances = signal<TermInstance[]>([]);
  protected readonly electiveGroupOptions = signal<ElectiveGroupOption[]>([]);
  protected readonly groupSummaries = signal<ElectiveGroupSummary[]>([]);
  protected readonly offeringOptions = signal<CourseOffering[]>([]);
  protected readonly loading = signal(false);
  protected readonly assigning = signal<number | null>(null);
  protected readonly selectedOfferingByEnrollment = new Map<number, number>();
  protected readonly scheduleStatus = signal<ElectiveGroupScheduleResponse | null>(null);

  protected readonly bulkOfferingId = signal<number | null>(null);
  protected readonly bulkAssigning = signal(false);
  protected readonly modeSaving = signal(false);
  // A fully-assigned group opens read-only by default -- Apply to All / per-row Assign sit right
  // there ready to overwrite everyone, and a completed group is far more likely being *reviewed*
  // than *worked on*. A group still in progress opens directly editable since that IS the normal
  // working state. Either way, one click flips it -- this is a safety default, not a lock.
  protected readonly editMode = signal(true);

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

  protected progressPercent(summary: ElectiveGroupSummary): number {
    return summary.eligibleCount === 0 ? 0 : Math.round((summary.assignedCount / summary.eligibleCount) * 100);
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
        case 'rollNumber': return row.enrollment.rollNumber ?? '';
        case 'studentName': return row.enrollment.studentName;
        case 'cohortCode': return row.enrollment.cohortCode;
        case 'currentChoice': return row.currentChoiceLabel ?? '';
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
    if (this.selectedTermInstanceId) {
      this.loadElectiveGroupOptions(this.selectedTermInstanceId);
      this.loadGroupSummaries(this.selectedTermInstanceId);
    }
  }

  /** Clicking a card in the group-launcher strip selects that group and loads its roster —
   *  the same effect as picking it from the dropdown below, just one click instead of two. */
  protected selectGroupFromSummary(summary: ElectiveGroupSummary): void {
    this.selectedElectiveGroupId = summary.electiveGroupId;
    this.editMode.set(!(summary.eligibleCount > 0 && summary.assignedCount >= summary.eligibleCount));
    this.onGroupChange();
  }

  protected toggleEditMode(): void {
    this.editMode.update((v) => !v);
  }

  private loadGroupSummaries(termInstanceId: number): void {
    this.academicYearService.getElectiveGroupSummaries(termInstanceId).subscribe({
      next: (summaries) => this.groupSummaries.set(summaries),
      error: () => this.groupSummaries.set([]),
    });
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

  protected isChange(row: AssignmentRow): boolean {
    const offeringId = this.selectedOfferingByEnrollment.get(row.enrollment.id);
    return !!row.currentChoiceOfferingId && !!offeringId && offeringId !== row.currentChoiceOfferingId;
  }

  protected assign(row: AssignmentRow): void {
    const offeringId = this.selectedOfferingByEnrollment.get(row.enrollment.id);
    if (!offeringId) return;
    if (this.isChange(row)) {
      this.dialog.open(ConfirmDialogComponent, {
        data: {
          title: 'Change Elective Choice',
          message: `"${row.enrollment.studentName}" is currently assigned to "${row.currentChoiceLabel}". ` +
            `Change their choice? The previous registration will be dropped.`,
          confirmText: 'Change',
          cancelText: 'Cancel',
        },
      }).afterClosed().subscribe((confirmed) => {
        if (confirmed) this.doAssign(row, offeringId);
      });
      return;
    }
    this.doAssign(row, offeringId);
  }

  private doAssign(row: AssignmentRow, offeringId: number): void {
    this.assigning.set(row.enrollment.id);
    this.academicYearService.assignElectiveChoice(row.enrollment.id, offeringId).subscribe({
      next: () => {
        this.toast.success('Elective assigned');
        this.assigning.set(null);
        this.selectedOfferingByEnrollment.delete(row.enrollment.id);
        const termInstanceId = this.selectedTermInstanceId;
        const group = this.selectedGroup();
        if (termInstanceId && group) {
          this.loadEnrollments(termInstanceId, group);
          this.loadGroupSummaries(termInstanceId);
        }
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
        const message = res.blockedCount > 0
          ? `Assigned ${res.assignedCount} of ${res.eligibleStudentCount} eligible student(s) — ` +
            `${res.blockedCount} skipped (already scheduled or attendance recorded, see roster below)`
          : `Assigned ${res.assignedCount} of ${res.eligibleStudentCount} eligible student(s)`;
        this.toast.success(message);
        this.bulkAssigning.set(false);
        this.bulkOfferingId.set(null);
        const group = this.selectedGroup();
        if (group) {
          this.loadEnrollments(termInstanceId, group);
          this.loadGroupSummaries(termInstanceId);
        }
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
    this.groupSummaries.set([]);
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
        if (preselect) {
          this.loadElectiveGroupOptions(preselect);
          this.loadGroupSummaries(preselect);
        }
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
          this.dataSource.paginator?.firstPage();
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
                this.dataSource.paginator?.firstPage();
                this.loading.set(false);
              }
            },
            error: () => {
              pending--;
              if (pending === 0) {
                this.dataSource.data = rows;
                this.dataSource.paginator?.firstPage();
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

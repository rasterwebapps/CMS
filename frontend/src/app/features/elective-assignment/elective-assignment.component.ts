import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AcademicYearService } from '../academic-year/academic-year.service';
import {
  AcademicYear,
  TermInstance,
  StudentTermEnrollment,
  CourseOffering,
} from '../academic-year/academic-year.model';
import { CmsEmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { ToastService } from '../../core/toast/toast.service';

interface ElectiveGroupOption {
  electiveGroupId: number;
  electiveGroupName: string;
  curriculumVersionId: number;
  semesterNumber: number;
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
    MatProgressSpinnerModule, CmsEmptyStateComponent,
  ],
  templateUrl: './elective-assignment.component.html',
  styleUrl: './elective-assignment.component.scss',
})
export class ElectiveAssignmentComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly route = inject(ActivatedRoute);
  private readonly toast = inject(ToastService);

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

  protected selectedAcademicYearId: number | null = null;
  protected selectedTermInstanceId: number | null = null;
  protected selectedElectiveGroupId: number | null = null;

  protected readonly selectedGroup = computed(() =>
    this.electiveGroupOptions().find((g) => g.electiveGroupId === this.selectedElectiveGroupId) ?? null);

  ngOnInit(): void {
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

  private resetGroupState(): void {
    this.selectedElectiveGroupId = null;
    this.electiveGroupOptions.set([]);
    this.offeringOptions.set([]);
    this.dataSource.data = [];
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
              semesterNumber: o.semesterNumber,
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
    this.academicYearService.getEnrollmentsByTermInstance(termInstanceId).subscribe({
      next: (enrollments) => {
        const relevant = enrollments.filter((e) => e.semesterNumber === group.semesterNumber);
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

import { Component, inject, signal, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CurriculumVersionService } from '../curriculum-version.service';
import {
  CurriculumFullView,
  CurriculumSemesterCourse,
  CurriculumSemesterCourseRequest,
  CurriculumElectiveGroup,
  SubjectType,
  AttendanceThreshold,
  AttendanceComponentType,
} from '../curriculum-version.model';
import { ToastService } from '../../../core/toast/toast.service';
import { environment } from '../../../../environments';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';

@Component({
  selector: 'app-curriculum-map',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
  ],
  templateUrl: './curriculum-map.component.html',
  styleUrl: './curriculum-map.component.scss',
})
export class CurriculumMapComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(CurriculumVersionService);
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  protected readonly loading = signal(true);
  protected readonly adding = signal<number | null>(null);
  protected readonly removing = signal<number | null>(null);
  protected readonly curriculum = signal<CurriculumFullView | null>(null);
  protected readonly subjects = signal<{ id: number; name: string; code: string }[]>([]);
  protected readonly showAddForm = signal<number | null>(null);
  protected readonly editingCourseId = signal<number | null>(null);
  protected readonly electiveGroups = signal<CurriculumElectiveGroup[]>([]);
  protected readonly loadingElectiveGroups = signal(false);
  protected readonly creatingElectiveGroup = signal(false);
  protected readonly thresholds = signal<AttendanceThreshold[]>([]);
  protected readonly savingThresholds = signal(false);

  protected readonly newElectiveGroupNameControl = this.fb.control('');

  protected readonly subjectTypes: SubjectType[] = ['CORE', 'FOUNDATIONAL', 'ELECTIVE'];

  protected readonly addCourseForm: FormGroup = this.fb.group({
    subjectId: [null, Validators.required],
    sortOrder: [null],
    theoryEnabled: [false],
    theoryHours: [{ value: 0, disabled: true }],
    labEnabled: [false],
    labHours: [{ value: 0, disabled: true }],
    clinicalEnabled: [false],
    clinicalHours: [{ value: 0, disabled: true }],
    subjectType: ['CORE', Validators.required],
    isElective: [false],
    electiveGroupId: [null],
    theoryThreshold: [null],
    labThreshold: [null],
    clinicalThreshold: [null],
  });

  private curriculumVersionId!: number;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      void this.router.navigate(['/curriculum-versions']);
      return;
    }
    this.curriculumVersionId = Number(id);

    this.http.get<{ id: number; name: string; code: string }[]>(`${environment.apiUrl}/subjects?activeOnly=true`)
      .subscribe({ next: (data) => this.subjects.set(data) });

    this.loadCurriculum();
  }

  protected toggleAddForm(termNumber: number): void {
    if (this.showAddForm() === termNumber && this.editingCourseId() === null) {
      this.showAddForm.set(null);
    } else {
      this.showAddForm.set(termNumber);
      this.editingCourseId.set(null);
      this.addCourseForm.reset({
        subjectId: null,
        sortOrder: null,
        theoryEnabled: false,
        theoryHours: 0,
        labEnabled: false,
        labHours: 0,
        clinicalEnabled: false,
        clinicalHours: 0,
        subjectType: 'CORE',
        isElective: false,
        electiveGroupId: null,
        theoryThreshold: null,
        labThreshold: null,
        clinicalThreshold: null,
      });
      this.setHourFieldState('theoryEnabled', 'theoryHours', false);
      this.setHourFieldState('labEnabled', 'labHours', false);
      this.setHourFieldState('clinicalEnabled', 'clinicalHours', false);
      this.thresholds.set([]);
      this.loadElectiveGroupsForTerm(termNumber);
    }
  }

  protected startEditCourse(course: CurriculumSemesterCourse): void {
    this.showAddForm.set(course.termNumber);
    this.editingCourseId.set(course.id);
    this.addCourseForm.reset({
      subjectId: course.subjectId,
      sortOrder: course.sortOrder ?? null,
      theoryEnabled: course.theoryHours > 0,
      theoryHours: course.theoryHours,
      labEnabled: course.labHours > 0,
      labHours: course.labHours,
      clinicalEnabled: course.clinicalHours > 0,
      clinicalHours: course.clinicalHours,
      subjectType: course.subjectType,
      isElective: course.isElective,
      electiveGroupId: course.electiveGroupId,
      theoryThreshold: null,
      labThreshold: null,
      clinicalThreshold: null,
    });
    this.addCourseForm.get('subjectId')?.disable();
    this.setHourFieldState('theoryEnabled', 'theoryHours', course.theoryHours > 0);
    this.setHourFieldState('labEnabled', 'labHours', course.labHours > 0);
    this.setHourFieldState('clinicalEnabled', 'clinicalHours', course.clinicalHours > 0);
    this.loadElectiveGroupsForTerm(course.termNumber);
    this.loadThresholds(course.id);
  }

  private loadThresholds(curriculumTermCourseId: number): void {
    this.service.getAttendanceThresholds(curriculumTermCourseId).subscribe({
      next: (thresholds) => {
        this.thresholds.set(thresholds);
        const byType = (type: AttendanceComponentType) =>
          thresholds.find((t) => t.attendanceType === type)?.minPercentage ?? null;
        this.addCourseForm.patchValue({
          theoryThreshold: byType('THEORY'),
          labThreshold: byType('LAB'),
          clinicalThreshold: byType('CLINICAL'),
        });
      },
      error: () => this.thresholds.set([]),
    });
  }

  private saveThresholds(curriculumTermCourseId: number): void {
    const v = this.addCourseForm.getRawValue();
    const componentThresholds: { type: AttendanceComponentType; enabled: boolean; value: number | null }[] = [
      { type: 'THEORY', enabled: v.theoryEnabled, value: v.theoryThreshold },
      { type: 'LAB', enabled: v.labEnabled, value: v.labThreshold },
      { type: 'CLINICAL', enabled: v.clinicalEnabled, value: v.clinicalThreshold },
    ];
    for (const { type, enabled, value } of componentThresholds) {
      if (!enabled) continue;
      const existing = this.thresholds().find((t) => t.attendanceType === type);
      if (value !== null && value !== undefined) {
        this.service.upsertAttendanceThreshold({
          curriculumTermCourseId, attendanceType: type, minPercentage: Number(value),
        }).subscribe();
      } else if (existing) {
        this.service.deleteAttendanceThreshold(existing.id).subscribe();
      }
    }
  }

  protected cancelForm(): void {
    this.showAddForm.set(null);
    this.editingCourseId.set(null);
    this.addCourseForm.get('subjectId')?.enable();
  }

  protected onHourToggle(enabledControl: string, hoursControl: string): void {
    const enabled = !!this.addCourseForm.get(enabledControl)?.value;
    this.setHourFieldState(enabledControl, hoursControl, enabled);
  }

  private setHourFieldState(enabledControl: string, hoursControl: string, enabled: boolean): void {
    const hours = this.addCourseForm.get(hoursControl);
    if (enabled) {
      hours?.enable({ emitEvent: false });
    } else {
      hours?.setValue(0, { emitEvent: false });
      hours?.disable({ emitEvent: false });
    }
  }

  protected loadElectiveGroupsForTerm(termNumber: number): void {
    this.loadingElectiveGroups.set(true);
    this.service.getElectiveGroups(this.curriculumVersionId, termNumber).subscribe({
      next: (groups) => {
        this.electiveGroups.set(groups);
        this.loadingElectiveGroups.set(false);
      },
      error: () => {
        this.electiveGroups.set([]);
        this.loadingElectiveGroups.set(false);
      },
    });
  }

  protected createElectiveGroup(termNumber: number): void {
    const groupName = this.newElectiveGroupNameControl.value?.trim();
    if (!groupName) return;
    this.creatingElectiveGroup.set(true);
    this.service.createElectiveGroup({
      curriculumVersionId: this.curriculumVersionId,
      termNumber,
      groupName,
    }).subscribe({
      next: (group) => {
        this.electiveGroups.update((groups) => [...groups, group]);
        this.addCourseForm.get('electiveGroupId')?.setValue(group.id);
        this.newElectiveGroupNameControl.setValue('');
        this.creatingElectiveGroup.set(false);
        this.toast.success('Elective group created');
      },
      error: () => {
        this.creatingElectiveGroup.set(false);
        this.toast.error('Failed to create elective group');
      },
    });
  }

  protected submitAddCourse(termNumber: number): void {
    if (this.addCourseForm.invalid) {
      scrollToFirstInvalid(this.addCourseForm);
      return;
    }
    const v = this.addCourseForm.getRawValue();
    if (v.isElective && !v.electiveGroupId) {
      this.toast.error('Select or create an elective group for this subject');
      return;
    }
    const request: CurriculumSemesterCourseRequest = {
      curriculumVersionId: this.curriculumVersionId,
      termNumber,
      subjectId: v.subjectId,
      sortOrder: v.sortOrder ?? undefined,
      theoryHours: v.theoryEnabled ? v.theoryHours : 0,
      labHours: v.labEnabled ? v.labHours : 0,
      clinicalHours: v.clinicalEnabled ? v.clinicalHours : 0,
      subjectType: v.subjectType,
      isElective: v.isElective,
      electiveGroupId: v.isElective ? v.electiveGroupId : null,
    };
    const editingId = this.editingCourseId();
    this.adding.set(termNumber);
    const call = editingId
      ? this.service.updateCourse(editingId, request)
      : this.service.addCourse(request);
    call.subscribe({
      next: () => {
        if (editingId) {
          this.saveThresholds(editingId);
        }
        this.toast.success(editingId ? 'Subject updated' : 'Subject added');
        this.showAddForm.set(null);
        this.editingCourseId.set(null);
        this.addCourseForm.get('subjectId')?.enable();
        this.addCourseForm.reset();
        this.adding.set(null);
        this.loadCurriculum();
      },
      error: () => {
        this.toast.error(editingId ? 'Failed to update subject' : 'Failed to add subject');
        this.adding.set(null);
      },
    });
  }

  protected removeCourse(id: number): void {
    this.removing.set(id);
    this.service.removeCourse(id).subscribe({
      next: () => {
        this.toast.success('Subject removed');
        this.removing.set(null);
        this.loadCurriculum();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to remove subject');
        this.removing.set(null);
      },
    });
  }

  protected getTermNumbers(): number[] {
    const c = this.curriculum();
    if (!c) return [];
    return Array.from({ length: c.totalTerms }, (_, i) => i + 1);
  }

  protected termLabel(n: number): string {
    return this.curriculum()?.assessmentPattern === 'YEARLY'
      ? `Year ${n}`
      : `Term ${n}`;
  }

  protected getCoursesForTerm(termNumber: number) {
    const c = this.curriculum();
    if (!c) return [];
    return c.terms.find(s => s.termNumber === termNumber)?.courses ?? [];
  }

  protected backToVersions(): void {
    const c = this.curriculum();
    void this.router.navigate(['/curriculum-versions'], {
      queryParams: c ? { programId: c.programId } : {}
    });
  }

  private loadCurriculum(): void {
    this.loading.set(true);
    this.service.getFullCurriculum(this.curriculumVersionId).subscribe({
      next: (data) => {
        this.curriculum.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load curriculum');
        this.loading.set(false);
      },
    });
  }
}

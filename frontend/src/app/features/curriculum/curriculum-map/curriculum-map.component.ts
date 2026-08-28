import { Component, computed, inject, signal, DestroyRef, OnInit } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { Observable, map, of, switchMap } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { SyllabusUnitDialogComponent } from '../syllabus-unit-dialog/syllabus-unit-dialog.component';
import { PermissionService } from '../../../core/permissions/permission.service';
import { CurriculumVersionService } from '../curriculum-version.service';
import {
  CurriculumFullView,
  CurriculumSemesterCourse,
  CurriculumSemesterCourseRequest,
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
    MatAutocompleteModule,
    MatButtonModule,
    MatDialogModule,
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
  private readonly dialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);
  private readonly permissionService = inject(PermissionService);

  protected readonly canViewUnits = computed(() => this.permissionService.has('SYLLABUS_UNIT_VIEW'));

  protected readonly loading = signal(true);
  protected readonly adding = signal<number | null>(null);
  protected readonly removing = signal<number | null>(null);
  protected readonly curriculum = signal<CurriculumFullView | null>(null);
  protected readonly subjects = signal<{ id: number; name: string; code: string }[]>([]);
  protected readonly showAddForm = signal<number | null>(null);
  protected readonly editingCourseId = signal<number | null>(null);
  protected readonly thresholds = signal<AttendanceThreshold[]>([]);
  protected readonly savingThresholds = signal(false);

  protected readonly subjectTypes: SubjectType[] = ['CORE', 'FOUNDATIONAL', 'ELECTIVE'];

  protected readonly addCourseForm: FormGroup = this.fb.group({
    subjectId: [null, Validators.required],
    theoryEnabled: [false],
    theoryHours: [{ value: 0, disabled: true }],
    labEnabled: [false],
    labHours: [{ value: 0, disabled: true }],
    clinicalEnabled: [false],
    clinicalHours: [{ value: 0, disabled: true }],
    subjectType: ['CORE', Validators.required],
    isElective: [false],
    theoryThreshold: [null],
    labThreshold: [null],
    clinicalThreshold: [null],
  });

  /** Search box driving the Subject autocomplete; holds free-typed text until an option is picked,
   *  then holds the selected subject's numeric id (matAutocomplete writes it back via displayWith). */
  protected readonly subjectSearchControl = this.fb.control<string | number>('');
  private readonly subjectSearchQuery = toSignal(this.subjectSearchControl.valueChanges, { initialValue: '' });

  /** Subjects available to pick for the term currently being added/edited, minus the ones already
   *  mapped into that same term (a subject may still repeat across different terms), filtered by the
   *  autocomplete search text. */
  protected readonly filteredSubjects = computed(() => {
    const term = this.showAddForm();
    if (term === null) return [];
    const rawQuery = this.subjectSearchQuery();
    const query = typeof rawQuery === 'string' ? rawQuery.trim().toLowerCase() : '';
    const available = this.getAvailableSubjects(term);
    if (!query) return available;
    return available.filter((s) => s.name.toLowerCase().includes(query) || s.code.toLowerCase().includes(query));
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
    this.syncSubjectTypeWithElectiveFlag();
  }

  /** Subject Type and the "choice-based elective" checkbox must never contradict each other:
   *  picking CORE/FOUNDATIONAL always clears the elective flag, and ticking/unticking the
   *  elective flag always drives Subject Type to/away from ELECTIVE. Each side sets the other
   *  with emitEvent:false so the two listeners don't recurse into each other. */
  private syncSubjectTypeWithElectiveFlag(): void {
    const subjectTypeControl = this.addCourseForm.get('subjectType')!;
    const isElectiveControl = this.addCourseForm.get('isElective')!;

    subjectTypeControl.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((type: SubjectType) => {
      const shouldBeElective = type === 'ELECTIVE';
      if (isElectiveControl.value !== shouldBeElective) {
        isElectiveControl.setValue(shouldBeElective, { emitEvent: false });
      }
    });

    isElectiveControl.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((checked: boolean) => {
      if (checked && subjectTypeControl.value !== 'ELECTIVE') {
        subjectTypeControl.setValue('ELECTIVE', { emitEvent: false });
      } else if (!checked && subjectTypeControl.value === 'ELECTIVE') {
        subjectTypeControl.setValue('CORE', { emitEvent: false });
      }
    });
  }

  protected toggleAddForm(termNumber: number): void {
    if (this.showAddForm() === termNumber) {
      this.cancelForm();
    } else {
      this.showAddForm.set(termNumber);
      this.editingCourseId.set(null);
      this.addCourseForm.reset({
        subjectId: null,
        theoryEnabled: false,
        theoryHours: 0,
        labEnabled: false,
        labHours: 0,
        clinicalEnabled: false,
        clinicalHours: 0,
        subjectType: 'CORE',
        isElective: false,
        theoryThreshold: null,
        labThreshold: null,
        clinicalThreshold: null,
      });
      this.subjectSearchControl.enable({ emitEvent: false });
      this.subjectSearchControl.setValue('', { emitEvent: false });
      this.setSubjectDependentFieldsEnabled(false);
      this.thresholds.set([]);
    }
  }

  protected startEditCourse(course: CurriculumSemesterCourse): void {
    this.showAddForm.set(course.termNumber);
    this.editingCourseId.set(course.id);
    this.addCourseForm.reset({
      subjectId: course.subjectId,
      theoryEnabled: course.theoryHours > 0,
      theoryHours: course.theoryHours,
      labEnabled: course.labHours > 0,
      labHours: course.labHours,
      clinicalEnabled: course.clinicalHours > 0,
      clinicalHours: course.clinicalHours,
      subjectType: course.subjectType,
      isElective: course.isElective,
      theoryThreshold: null,
      labThreshold: null,
      clinicalThreshold: null,
    });
    this.addCourseForm.get('subjectId')?.disable();
    this.subjectSearchControl.setValue(course.subjectId, { emitEvent: false });
    this.subjectSearchControl.disable({ emitEvent: false });
    this.setSubjectDependentFieldsEnabled(true);
    this.setHourFieldState('theoryEnabled', 'theoryHours', course.theoryHours > 0);
    this.setHourFieldState('labEnabled', 'labHours', course.labHours > 0);
    this.setHourFieldState('clinicalEnabled', 'clinicalHours', course.clinicalHours > 0);
    this.loadThresholds(course.id);
  }

  /** Subjects still available for a term — excludes subjects already mapped into that same
   *  term (a subject may still repeat across different terms), but keeps the subject currently
   *  being edited in the list so its disabled Subject field still displays a name. */
  protected getAvailableSubjects(termNumber: number): { id: number; name: string; code: string }[] {
    const editingId = this.editingCourseId();
    const usedIds = new Set(
      this.getCoursesForTerm(termNumber)
        .filter((c) => c.id !== editingId)
        .map((c) => c.subjectId)
    );
    return this.subjects().filter((s) => !usedIds.has(s.id));
  }

  protected readonly subjectDisplayFn = (id: number | string | null): string => {
    if (id === null || id === undefined || id === '') return '';
    const subjectId = typeof id === 'string' ? Number(id) : id;
    const s = this.subjects().find((x) => x.id === subjectId);
    return s ? `${s.code} — ${s.name}` : '';
  };

  protected onSubjectSelected(event: MatAutocompleteSelectedEvent): void {
    this.addCourseForm.get('subjectId')?.setValue(event.option.value);
    this.setSubjectDependentFieldsEnabled(true);
  }

  /** Sort Order, Subject Type, elective toggle, and the hour checkboxes are meaningless
   *  until a Subject is chosen, so they stay disabled until then. */
  private setSubjectDependentFieldsEnabled(enabled: boolean): void {
    for (const name of ['subjectType', 'isElective', 'theoryEnabled', 'labEnabled', 'clinicalEnabled']) {
      const control = this.addCourseForm.get(name);
      if (enabled) control?.enable({ emitEvent: false });
      else control?.disable({ emitEvent: false });
    }
    if (!enabled) {
      this.setHourFieldState('theoryEnabled', 'theoryHours', false);
      this.setHourFieldState('labEnabled', 'labHours', false);
      this.setHourFieldState('clinicalEnabled', 'clinicalHours', false);
    }
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
    this.subjectSearchControl.enable({ emitEvent: false });
    this.subjectSearchControl.setValue('', { emitEvent: false });
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

  /** A term has at most one elective group. Always checks the server fresh at submit time
   *  (never a cached signal, to avoid racing an earlier in-flight fetch into creating a
   *  duplicate) and reuses the existing group if found, otherwise creates it with an
   *  auto-filled name — there is no manual group picker for the user to choose/name one. */
  private resolveElectiveGroupId(termNumber: number): Observable<number> {
    return this.service.getElectiveGroups(this.curriculumVersionId, termNumber).pipe(
      switchMap((groups) => {
        const existing = groups[0];
        if (existing) return of(existing.id);
        return this.service.createElectiveGroup({
          curriculumVersionId: this.curriculumVersionId,
          termNumber,
          groupName: `Elective — ${this.termLabel(termNumber)}`,
        }).pipe(map((group) => group.id));
      }),
    );
  }

  protected submitAddCourse(termNumber: number): void {
    if (this.addCourseForm.invalid) {
      scrollToFirstInvalid(this.addCourseForm);
      return;
    }
    const v = this.addCourseForm.getRawValue();
    const editingId = this.editingCourseId();
    this.adding.set(termNumber);

    const electiveGroupId$: Observable<number | null> = v.isElective
      ? this.resolveElectiveGroupId(termNumber)
      : of(null);
    electiveGroupId$.subscribe({
      next: (electiveGroupId) => {
        const request: CurriculumSemesterCourseRequest = {
          curriculumVersionId: this.curriculumVersionId,
          termNumber,
          subjectId: v.subjectId,
          // theory_hours/lab_hours/clinical_hours are NOT NULL columns — a checked component
          // left blank (user cleared the input) must still submit as 0, never null.
          theoryHours: v.theoryEnabled ? (v.theoryHours ?? 0) : 0,
          labHours: v.labEnabled ? (v.labHours ?? 0) : 0,
          clinicalHours: v.clinicalEnabled ? (v.clinicalHours ?? 0) : 0,
          subjectType: v.subjectType,
          isElective: v.isElective,
          electiveGroupId,
        };
        const call = editingId
          ? this.service.updateCourse(editingId, request)
          : this.service.addCourse(request);
        call.subscribe({
          next: () => {
            if (editingId) {
              this.saveThresholds(editingId);
            }
            this.toast.success(editingId ? 'Subject updated' : 'Subject added');
            this.cancelForm();
            this.addCourseForm.reset();
            this.adding.set(null);
            this.loadCurriculum();
          },
          error: () => {
            this.toast.error(editingId ? 'Failed to update subject' : 'Failed to add subject');
            this.adding.set(null);
          },
        });
      },
      error: () => {
        this.toast.error('Failed to prepare elective group');
        this.adding.set(null);
      },
    });
  }

  protected openUnitsDialog(course: CurriculumSemesterCourse): void {
    this.dialog.open(SyllabusUnitDialogComponent, {
      width: '600px',
      data: {
        curriculumTermCourseId: course.id,
        subjectName: course.subjectName,
        subjectCode: course.subjectCode,
        theoryHours: course.theoryHours,
        labHours: course.labHours,
        clinicalHours: course.clinicalHours,
      },
    });
  }

  protected confirmRemoveCourse(course: CurriculumSemesterCourse): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Remove Subject',
        message: `Remove "${course.subjectName}" (${course.subjectCode}) from ${this.termLabel(course.termNumber)}? This cannot be undone.`,
        confirmText: 'Remove',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) this.removeCourse(course.id);
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

  /** Display order for subject-type groups — CORE (the profession-defining subjects) first,
   *  then FOUNDATIONAL. There is no manual sort-order field in the UI; order is always derived
   *  from type + name so it stays predictable without an extra field to fill in. */
  private static readonly SUBJECT_TYPE_RANK: Record<SubjectType, number> = { CORE: 0, FOUNDATIONAL: 1, ELECTIVE: 2 };

  /** Non-elective subjects for a term — each rendered as its own row, CORE subjects before
   *  FOUNDATIONAL, alphabetical by name within each type. */
  protected getRegularCoursesForTerm(termNumber: number): CurriculumSemesterCourse[] {
    return this.getCoursesForTerm(termNumber)
      .filter(c => !c.isElective)
      .sort((a, b) =>
        CurriculumMapComponent.SUBJECT_TYPE_RANK[a.subjectType] - CurriculumMapComponent.SUBJECT_TYPE_RANK[b.subjectType]
        || a.subjectName.localeCompare(b.subjectName)
      );
  }

  /**
   * Elective subjects for a term, bucketed by elective group. Since only one
   * option in a group is ever chosen by a student, the options are rendered
   * together under one group heading instead of as separate flat rows.
   */
  protected getElectiveGroupsForTerm(
    termNumber: number
  ): { groupId: number | null; groupName: string; courses: CurriculumSemesterCourse[] }[] {
    const electives = this.getCoursesForTerm(termNumber).filter(c => c.isElective);
    const byGroup = new Map<number | string, { groupId: number | null; groupName: string; courses: CurriculumSemesterCourse[] }>();
    for (const course of electives) {
      const key = course.electiveGroupId ?? 'ungrouped';
      const bucket = byGroup.get(key) ?? {
        groupId: course.electiveGroupId,
        groupName: course.electiveGroupName ?? 'Ungrouped Electives',
        courses: [],
      };
      bucket.courses.push(course);
      byGroup.set(key, bucket);
    }
    for (const bucket of byGroup.values()) {
      bucket.courses.sort((a, b) => a.subjectName.localeCompare(b.subjectName));
    }
    return Array.from(byGroup.values());
  }

  /**
   * Total theory/lab/clinical hours for a term. Regular subjects all count; for each
   * elective group only one representative subject's hours count, since a student only
   * ever takes 1 of N options — summing every option would overstate the term's real hours.
   */
  protected getTermHoursSummary(termNumber: number): { total: number; theory: number; lab: number; clinical: number } {
    const regular = this.getRegularCoursesForTerm(termNumber);
    const electiveReps = this.getElectiveGroupsForTerm(termNumber)
      .map(group => group.courses[0])
      .filter((c): c is CurriculumSemesterCourse => !!c);
    const totals = [...regular, ...electiveReps].reduce(
      (acc, c) => ({
        theory: acc.theory + c.theoryHours,
        lab: acc.lab + c.labHours,
        clinical: acc.clinical + c.clinicalHours,
      }),
      { theory: 0, lab: 0, clinical: 0 }
    );
    return { total: totals.theory + totals.lab + totals.clinical, ...totals };
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

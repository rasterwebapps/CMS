import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, ActivatedRoute } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { of, Subject } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

import { TimetableViewComponent } from './timetable-view.component';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear, TermInstance } from '../../academic-year/academic-year.model';
import { TimetableService } from '../timetable.service';
import { ClassSchedule, ClassScheduleOccurrence } from '../timetable.model';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';
import { WeekGridSession } from '../../../shared/week-grid/week-grid.model';

// The Date-wise-weekly view replaced the old calendar-month view on the browse-all screen. These
// tests cover exactly that: real-dated occurrence loading scoped to one Mon-Sat week at a time
// (scope=browse), the Saturday-column signal threaded through from the selected term, and mapping
// occurrence status onto cms-week-grid's session shape -- not a full re-test of the pre-existing
// Generic/Day view or faculty/room/batch filtering machinery.
describe('TimetableViewComponent', () => {
  let fixture: ComponentFixture<TimetableViewComponent>;
  let component: TimetableViewComponent;
  let timetableService: {
    getPublished: ReturnType<typeof vi.fn>;
    getOccurrences: ReturnType<typeof vi.fn>;
  };

  const academicYears: AcademicYear[] = [
    { id: 1, name: '2026-2027', startDate: '2026-06-01', endDate: '2027-05-31', isCurrent: true, createdAt: '', updatedAt: '' },
  ];
  const termInstances: TermInstance[] = [
    { id: 10, academicYearId: 1, academicYearName: '2026-2027', termType: 'ODD', startDate: '2026-06-01', endDate: '2026-11-30', status: 'OPEN', createdAt: '', updatedAt: '', workingSaturdayCount: 12 },
  ];

  const internal = () => component as unknown as {
    viewMode: () => 'week' | 'dateWise' | 'day';
    dateWiseSessions: () => WeekGridSession[];
    setViewMode(mode: 'week' | 'dateWise' | 'day'): void;
    weekStart: () => string;
    selectedTerm: () => TermInstance | null;
    onTermChange(value: number | null): void;
    facultyOptions: () => (string | null)[];
  };

  beforeEach(async () => {
    timetableService = {
      getPublished: vi.fn(() => of([] as ClassSchedule[])),
      getOccurrences: vi.fn(() => of([] as ClassScheduleOccurrence[])),
    };

    await TestBed.configureTestingModule({
      imports: [TimetableViewComponent],
      providers: [
        provideRouter([]),
        { provide: AcademicYearService, useValue: {
          getAllAcademicYears: vi.fn(() => of(academicYears)),
          getTermInstancesByAcademicYear: vi.fn(() => of(termInstances)),
          getAllCohorts: vi.fn(() => of([])),
        } },
        { provide: TimetableService, useValue: timetableService },
        { provide: PermissionService, useValue: { has: vi.fn(() => false) } },
        { provide: ToastService, useValue: { success: vi.fn(), error: vi.fn() } },
        { provide: MatDialog, useValue: { open: vi.fn() } },
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: { get: () => null } } } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TimetableViewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges(); // loads academic years/terms, defaults weekStart
  });

  it('carries the selected term\'s workingSaturdayCount through to the Generic week grid', () => {
    expect(internal().selectedTerm()?.workingSaturdayCount).toBe(12);
  });

  it('fetches exactly one Mon-Sat week of real occurrences, scope=browse, on switching to dateWise', () => {
    internal().setViewMode('dateWise');

    expect(timetableService.getOccurrences).toHaveBeenCalledWith(
      10, internal().weekStart(), expect.any(String), 'browse', null);
    const [, , to] = timetableService.getOccurrences.mock.calls.at(-1)!;
    const daysApart = (new Date(to).getTime() - new Date(internal().weekStart()).getTime()) / 86400000;
    expect(daysApart).toBe(5);
  });

  // Real bug: a term starting mid-week (e.g. a Thursday) has that week's own Monday fall BEFORE
  // the term actually starts. Defaulting the Date-wise weekStart there put Mon-Wed on screen with
  // no real occurrences at all (the term hadn't started), while the Generic (recurring, date-
  // agnostic) view of the same timetable still showed those days occupied every week -- reading as
  // if the app had silently lost that cohort's Monday-Wednesday sessions. The default should skip
  // forward to the next Monday instead, so the landing week is fully inside the term.
  it('skips the default weekStart forward a full week when the term starts mid-week, instead of landing on its own partial week', () => {
    const midWeekTerm: TermInstance = {
      id: 20, academicYearId: 1, academicYearName: '2026-2027', termType: 'EVEN',
      startDate: '2035-06-07', endDate: '2035-12-31', status: 'PLANNED', createdAt: '', updatedAt: '', workingSaturdayCount: 0,
    };
    // Stub still only serves the original term list -- inject the mid-week term directly to
    // isolate this test to clampWeekStartToTerm's own forward-skip behavior.
    (component as unknown as { termInstances: { set(v: TermInstance[]): void } }).termInstances
      .set([...termInstances, midWeekTerm]);
    internal().onTermChange(20);

    expect(internal().weekStart()).toBe('2035-06-11'); // NOT '2035-06-04', the Monday before startDate
  });

  // Real bug: ngOnInit fires two independent chains -- academic-year -> term-instance resolution
  // (calls loadPublished as soon as a term is preselected) and the separate all-cohorts-in-the-
  // college fetch (calls loadPublished again once a cohort is preselected) -- each reading
  // selectedCohortId at its own call time, with no cancellation between the two resulting
  // getPublished requests. The all-cohorts fetch is the heavier one, so it's entirely possible for
  // the earlier, unfiltered (cohortId still null) request's response to resolve AFTER the later,
  // correctly cohort-filtered one -- silently overwriting the screen with every published cohort's
  // sessions merged together (including other cohorts' Clinical Shift entries) instead of just the
  // selected cohort's.
  it('discards a stale unfiltered getPublished response that resolves after a newer cohort-filtered one', () => {
    const unfiltered = new Subject<ClassSchedule[]>();
    const filtered = new Subject<ClassSchedule[]>();
    timetableService.getPublished.mockImplementation((_termId: number, cohortId: number | null) =>
      cohortId == null ? unfiltered.asObservable() : filtered.asObservable());

    const comp = component as unknown as {
      selectedCohortId: number | null;
      loadPublished(termInstanceId: number): void;
      sessions: () => ClassSchedule[];
    };

    comp.selectedCohortId = null;
    comp.loadPublished(10); // the term-instance chain's call, before cohorts have resolved
    comp.selectedCohortId = 8;
    comp.loadPublished(10); // the cohort chain's call, once cohort 8 is preselected

    filtered.next([{ id: 1 } as unknown as ClassSchedule]); // the correct, later request answers first
    unfiltered.next([ // the stale, earlier request answers late and must be discarded
      { id: 1 } as unknown as ClassSchedule, { id: 2 } as unknown as ClassSchedule, { id: 3 } as unknown as ClassSchedule,
    ]);

    expect(comp.sessions()).toEqual([{ id: 1 }]);
  });

  // Real bug: the Faculty filter dropdown was built only from Generic's recurring sessions() list,
  // never from Date-wise/Day's real occurrences() -- so a SUBSTITUTED occurrence's stand-in
  // faculty (real only for that specific date, never part of the recurring template) never showed
  // up as a filterable option after switching to Date-wise/Day, reading as though the filter
  // dropdown's contents simply weren't refreshing when toggling between views.
  it('includes a Date-wise substitute faculty in the Faculty filter options, not just Generic\'s own', () => {
    timetableService.getPublished.mockReturnValue(of([
      { id: 1, sessionType: 'THEORY', status: 'PUBLISHED', subjectName: 'Anatomy', subjectCode: 'A1',
        facultyName: 'Naveen Kumar', roomName: 'Room 101', batchName: null, dayOfWeek: 'MONDAY',
        periodId: 1, startTime: '09:00', endTime: '09:50', slotName: 'Period 1' } as unknown as ClassSchedule,
    ]));
    timetableService.getOccurrences.mockReturnValue(of([
      {
        date: '2026-09-21', occurrenceStatus: 'SUBSTITUTED', cancelReason: null,
        session: {
          id: 1, sessionType: 'THEORY', status: 'PUBLISHED', subjectName: 'Anatomy', subjectCode: 'A1',
          facultyName: 'Divya Krishnan', roomName: 'Room 101', batchName: null, dayOfWeek: 'MONDAY',
          periodId: 1, startTime: '09:00', endTime: '09:50', slotName: 'Period 1',
        },
      } as unknown as ClassScheduleOccurrence,
    ]));

    internal().onTermChange(10);
    internal().setViewMode('dateWise');

    expect(internal().facultyOptions()).toEqual(['Divya Krishnan', 'Naveen Kumar']);
  });

  it('maps occurrence status/cancelReason onto the sessions fed to cms-week-grid', () => {
    const occ: ClassScheduleOccurrence = {
      date: '2026-09-21',
      occurrenceStatus: 'SUBSTITUTED',
      cancelReason: null,
      session: {
        id: 2, sessionType: 'LAB', status: 'PUBLISHED', subjectName: 'Physiology', subjectCode: 'PHYS101',
        facultyName: 'Dr. Iyer', roomName: 'Lab 2', batchName: 'B1', dayOfWeek: 'TUESDAY', periodId: 2,
        startTime: '10:00', endTime: '10:50', slotName: '2nd Period',
      },
    };
    timetableService.getOccurrences.mockReturnValue(of([occ]));

    internal().setViewMode('dateWise');

    expect(internal().dateWiseSessions()).toEqual([
      { ...occ.session, occurrenceStatus: 'SUBSTITUTED', cancelReason: null },
    ]);
  });
});

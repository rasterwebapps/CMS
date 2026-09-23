import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, ActivatedRoute } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';
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
      10, internal().weekStart(), expect.any(String), 'browse');
    const [, , to] = timetableService.getOccurrences.mock.calls.at(-1)!;
    const daysApart = (new Date(to).getTime() - new Date(internal().weekStart()).getTime()) / 86400000;
    expect(daysApart).toBe(5);
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

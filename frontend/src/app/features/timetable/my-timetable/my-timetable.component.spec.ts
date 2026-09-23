import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, ActivatedRoute } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

import { MyTimetableComponent } from './my-timetable.component';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear, TermInstance } from '../../academic-year/academic-year.model';
import { TimetableService } from '../timetable.service';
import { ClassScheduleOccurrence, MyTimetableResponse } from '../timetable.model';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';
import { WeekGridSession } from '../../../shared/week-grid/week-grid.model';

// My Timetable now serves two separate routes/screens sharing one component -- /my-timetable/student
// (MY_TIMETABLE_VIEW_STUDENT) and /my-timetable/staff (MY_TIMETABLE_VIEW_STAFF) -- distinguished
// only by route `data.audience`. These tests cover exactly that new split (Log Progress gating,
// which permission-scoped endpoint each lands on) and the new Date-wise-weekly view's real-dated
// occurrence loading, not a full re-test of the pre-existing Generic/Day view machinery.
describe('MyTimetableComponent', () => {
  let fixture: ComponentFixture<MyTimetableComponent>;
  let component: MyTimetableComponent;
  let timetableService: {
    getMyTimetable: ReturnType<typeof vi.fn>;
    getOccurrences: ReturnType<typeof vi.fn>;
  };
  let permissionService: { has: ReturnType<typeof vi.fn> };

  const academicYears: AcademicYear[] = [
    { id: 1, name: '2026-2027', startDate: '2026-06-01', endDate: '2027-05-31', isCurrent: true, createdAt: '', updatedAt: '' },
  ];
  const termInstances: TermInstance[] = [
    { id: 10, academicYearId: 1, academicYearName: '2026-2027', termType: 'ODD', startDate: '2026-06-01', endDate: '2026-11-30', status: 'OPEN', createdAt: '', updatedAt: '', workingSaturdayCount: 0 },
  ];
  const myTimetableResponse: MyTimetableResponse = { sessions: [], holidays: [] };

  const internal = () => component as unknown as {
    audience: 'STUDENT' | 'STAFF';
    canLogProgress: () => boolean;
    viewMode: () => 'week' | 'dateWise' | 'day';
    dateWiseSessions: () => WeekGridSession[];
    setViewMode(mode: 'week' | 'dateWise' | 'day'): void;
    selectedTermInstanceId: number | null;
    weekStart: string;
  };

  function configure(audience: 'STUDENT' | 'STAFF' | undefined): void {
    timetableService = {
      getMyTimetable: vi.fn(() => of(myTimetableResponse)),
      getOccurrences: vi.fn(() => of([] as ClassScheduleOccurrence[])),
    };
    permissionService = { has: vi.fn(() => true) };

    TestBed.configureTestingModule({
      imports: [MyTimetableComponent],
      providers: [
        provideRouter([]),
        { provide: AcademicYearService, useValue: {
          getAllAcademicYears: vi.fn(() => of(academicYears)),
          getTermInstancesByAcademicYear: vi.fn(() => of(termInstances)),
        } },
        { provide: TimetableService, useValue: timetableService },
        { provide: PermissionService, useValue: permissionService },
        { provide: ToastService, useValue: { success: vi.fn(), error: vi.fn() } },
        { provide: MatDialog, useValue: { open: vi.fn() } },
        { provide: ActivatedRoute, useValue: { snapshot: { data: audience ? { audience } : {} } } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MyTimetableComponent);
    component = fixture.componentInstance;
  }

  describe('audience (route-data split)', () => {
    it('defaults to STUDENT when no route data is set (e.g. a fresh/unconfigured route)', () => {
      configure(undefined);
      fixture.detectChanges();
      expect(internal().audience).toBe('STUDENT');
    });

    it('never shows Log Progress for a STUDENT even if PROGRESS_LOG_CREATE is somehow granted', () => {
      configure('STUDENT');
      fixture.detectChanges();
      expect(permissionService.has).not.toHaveBeenCalledWith('PROGRESS_LOG_CREATE');
      expect(internal().canLogProgress()).toBe(false);
    });
  });

  describe('audience STAFF', () => {
    beforeEach(() => configure('STAFF'));

    it('shows Log Progress for STAFF when PROGRESS_LOG_CREATE is granted', () => {
      fixture.detectChanges();
      expect(internal().canLogProgress()).toBe(true);
    });

    it('hides Log Progress for STAFF when PROGRESS_LOG_CREATE is not granted', () => {
      permissionService.has.mockReturnValue(false);
      fixture.detectChanges();
      expect(internal().canLogProgress()).toBe(false);
    });
  });

  describe('Date-wise-weekly view', () => {
    beforeEach(() => {
      configure('STUDENT');
      fixture.detectChanges(); // loads term, defaults weekStart
    });

    it('fetches exactly one Mon-Sat week of real occurrences, scope=personal, on switching to dateWise', () => {
      internal().setViewMode('dateWise');

      expect(timetableService.getOccurrences).toHaveBeenCalledWith(
        10, internal().weekStart, expect.any(String), 'personal');
      const [, , to] = timetableService.getOccurrences.mock.calls.at(-1)!;
      const daysApart = (new Date(to).getTime() - new Date(internal().weekStart).getTime()) / 86400000;
      expect(daysApart).toBe(5);
    });

    it('maps occurrence status/cancelReason onto the sessions fed to cms-week-grid', () => {
      const occ: ClassScheduleOccurrence = {
        date: '2026-09-21',
        occurrenceStatus: 'CANCELLED',
        cancelReason: 'Holiday',
        session: {
          id: 1, sessionType: 'THEORY', status: 'PUBLISHED', subjectName: 'Anatomy', subjectCode: 'ANAT101',
          facultyName: 'Dr. Rao', roomName: 'Room 1', batchName: null, dayOfWeek: 'MONDAY', periodId: 1,
          startTime: '09:00', endTime: '09:50', slotName: '1st Period',
        },
      };
      timetableService.getOccurrences.mockReturnValue(of([occ]));

      internal().setViewMode('dateWise');

      expect(internal().dateWiseSessions()).toEqual([
        { ...occ.session, occurrenceStatus: 'CANCELLED', cancelReason: 'Holiday' },
      ]);
    });
  });
});

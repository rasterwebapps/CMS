import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

import { TimetableDraftReviewComponent } from './timetable-draft-review.component';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear, TermInstance } from '../../academic-year/academic-year.model';
import { TimetableService } from '../timetable.service';
import { ClassSchedule, CohortTermStatusSummary, TimetableActionResponse } from '../timetable.model';
import { ConflictInspectorService } from '../conflict-inspector/conflict-inspector.service';
import { ConflictAcknowledgmentStatus } from '../conflict-inspector/conflict-inspector.model';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';

// Draft Review lands on a per-cohort status summary table for the selected term instance
// (visibility was the whole point of this change) instead of immediately loading the combined
// grid; a cohort row click switches into the existing, unchanged term-wide grid. These tests
// cover exactly that new landing/drill-in behavior, not a full re-test of the pre-existing
// Approve/Revert/Discard/swap machinery.
describe('TimetableDraftReviewComponent', () => {
  let fixture: ComponentFixture<TimetableDraftReviewComponent>;
  let component: TimetableDraftReviewComponent;
  let timetableService: {
    getDraft: ReturnType<typeof vi.fn>;
    getPublished: ReturnType<typeof vi.fn>;
    getClinicalShiftSummary: ReturnType<typeof vi.fn>;
    getCohortStatusSummary: ReturnType<typeof vi.fn>;
    approve: ReturnType<typeof vi.fn>;
  };
  let conflictInspectorService: {
    getAcknowledgmentStatus: ReturnType<typeof vi.fn>;
  };

  const academicYears: AcademicYear[] = [
    { id: 1, name: '2024-2025', startDate: '2024-06-01', endDate: '2025-05-31', isCurrent: true, createdAt: '', updatedAt: '' },
  ];
  const termInstances: TermInstance[] = [
    { id: 10, academicYearId: 1, academicYearName: '2024-2025', termType: 'ODD', startDate: '2024-06-01', endDate: '2024-11-30', status: 'OPEN', createdAt: '', updatedAt: '' },
  ];
  const summaryRows: CohortTermStatusSummary[] = [
    { cohortId: 5, cohortName: 'BSc Nursing 2024', courseName: 'BSc Nursing', admissionYearName: '2024-2025', status: 'PARTIALLY_PUBLISHED', draftCount: 1, publishedCount: 2, unassignedHours: 0 },
    { cohortId: 6, cohortName: 'GNM 2024', courseName: 'GNM', admissionYearName: '2024-2025', status: 'DRAFT', draftCount: 3, publishedCount: 0, unassignedHours: 12.5 },
  ];

  const internal = () => component as unknown as {
    viewMode: () => 'summary' | 'grid';
    cohortSummary: () => CohortTermStatusSummary[];
    summaryLoading: () => boolean;
    sessions: () => ClassSchedule[];
    conflictAcknowledged: () => boolean | null;
    publishDisabledReason(): string | null;
    onCohortRowClick(): void;
    onBackToSummary(): void;
    onTermChange(): void;
    onApprove(): void;
    selectedTermInstanceId: number | null;
  };

  beforeEach(async () => {
    timetableService = {
      getDraft: vi.fn(() => of([] as ClassSchedule[])),
      getPublished: vi.fn(() => of([] as ClassSchedule[])),
      getClinicalShiftSummary: vi.fn(() => of([])),
      getCohortStatusSummary: vi.fn(() => of(summaryRows)),
      approve: vi.fn(() => of({ affectedCount: 3 } as TimetableActionResponse)),
    };
    conflictInspectorService = {
      getAcknowledgmentStatus: vi.fn(() => of({ termInstanceId: 10, acknowledged: true, acknowledgedAt: '2024-06-01T00:00:00Z' } as ConflictAcknowledgmentStatus)),
    };

    await TestBed.configureTestingModule({
      imports: [TimetableDraftReviewComponent],
      providers: [
        provideRouter([]),
        { provide: AcademicYearService, useValue: {
          getAllAcademicYears: vi.fn(() => of(academicYears)),
          getTermInstancesByAcademicYear: vi.fn(() => of(termInstances)),
        } },
        { provide: TimetableService, useValue: timetableService },
        { provide: ConflictInspectorService, useValue: conflictInspectorService },
        { provide: PermissionService, useValue: { has: vi.fn(() => true) } },
        { provide: ToastService, useValue: { success: vi.fn(), error: vi.fn() } },
        { provide: MatDialog, useValue: { open: vi.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TimetableDraftReviewComponent);
    component = fixture.componentInstance;
  });

  it('lands on the cohort status summary table, not the grid, once a term is selected', () => {
    fixture.detectChanges();

    expect(internal().viewMode()).toBe('summary');
    expect(timetableService.getCohortStatusSummary).toHaveBeenCalledWith(10);
    expect(internal().cohortSummary()).toEqual(summaryRows);
    expect(timetableService.getDraft).not.toHaveBeenCalled();
  });

  it('shows an empty state when no cohorts are enrolled for the term', () => {
    timetableService.getCohortStatusSummary.mockReturnValue(of([]));

    fixture.detectChanges();

    expect(internal().cohortSummary()).toEqual([]);
    expect(internal().summaryLoading()).toBe(false);
  });

  it('clicking a cohort row opens the unchanged term-wide grid without filtering by cohort', () => {
    fixture.detectChanges();

    internal().onCohortRowClick();

    expect(internal().viewMode()).toBe('grid');
    // loadDraft only ever takes termInstanceId -- no cohort id is threaded through, matching the
    // explicit product decision that Publish/Revert/Discard stay term-wide, not cohort-scoped.
    expect(timetableService.getDraft).toHaveBeenCalledWith(10);
    expect(timetableService.getDraft).toHaveBeenCalledTimes(1);
  });

  it('"back to summary" returns to the summary view', () => {
    fixture.detectChanges();
    internal().onCohortRowClick();

    internal().onBackToSummary();

    expect(internal().viewMode()).toBe('summary');
  });

  it('publishing refreshes the cohort summary in addition to the grid', () => {
    fixture.detectChanges();
    internal().onCohortRowClick();
    timetableService.getCohortStatusSummary.mockClear();

    internal().onApprove();

    expect(timetableService.approve).toHaveBeenCalledWith(10, false, undefined);
    expect(timetableService.getCohortStatusSummary).toHaveBeenCalledWith(10);
  });

  it('switching terms reloads the summary for the newly selected term', () => {
    fixture.detectChanges();
    timetableService.getCohortStatusSummary.mockClear();
    internal().selectedTermInstanceId = 10;

    internal().onTermChange();

    expect(internal().viewMode()).toBe('summary');
    expect(timetableService.getCohortStatusSummary).toHaveBeenCalledWith(10);
  });

  // OC-258: Draft Review's Publish action is now gated on a fresh Conflict Inspector
  // acknowledgment for the term (see TimetableConflictInspectorService#isAcknowledgmentValid) --
  // enforced server-side too, but the grid fetches the same status to disable Publish + show a
  // banner up front instead of only failing after the user clicks it.
  describe('conflict acknowledgment gate', () => {
    it('fetches the acknowledgment status for the term when the grid is opened', () => {
      fixture.detectChanges();

      internal().onCohortRowClick();

      expect(conflictInspectorService.getAcknowledgmentStatus).toHaveBeenCalledWith(10);
    });

    it('leaves Publish unblocked once the term is acknowledged', () => {
      conflictInspectorService.getAcknowledgmentStatus.mockReturnValue(
        of({ termInstanceId: 10, acknowledged: true, acknowledgedAt: '2024-06-01T00:00:00Z' } as ConflictAcknowledgmentStatus));
      fixture.detectChanges();

      internal().onCohortRowClick();

      expect(internal().conflictAcknowledged()).toBe(true);
      expect(internal().publishDisabledReason()).toBeNull();
    });

    it('blocks Publish and reports a reason when the term has not been acknowledged', () => {
      conflictInspectorService.getAcknowledgmentStatus.mockReturnValue(
        of({ termInstanceId: 10, acknowledged: false, acknowledgedAt: null } as ConflictAcknowledgmentStatus));
      fixture.detectChanges();

      internal().onCohortRowClick();

      expect(internal().conflictAcknowledged()).toBe(false);
      expect(internal().publishDisabledReason()).not.toBeNull();
    });

    it('re-checks the acknowledgment status after a failed publish attempt reports it stale', () => {
      timetableService.approve.mockReturnValue(throwError(() => ({ error: { code: 'TIMETABLE_CONFLICT_ACKNOWLEDGMENT_REQUIRED', message: 'stale' } })));
      fixture.detectChanges();
      internal().onCohortRowClick();
      conflictInspectorService.getAcknowledgmentStatus.mockClear();
      conflictInspectorService.getAcknowledgmentStatus.mockReturnValue(
        of({ termInstanceId: 10, acknowledged: false, acknowledgedAt: null } as ConflictAcknowledgmentStatus));

      internal().onApprove();

      expect(conflictInspectorService.getAcknowledgmentStatus).toHaveBeenCalledWith(10);
      expect(internal().conflictAcknowledged()).toBe(false);
    });
  });
});

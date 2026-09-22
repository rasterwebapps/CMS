import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

import { ConflictInspectorComponent } from './conflict-inspector.component';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { AcademicYear, TermInstance } from '../../academic-year/academic-year.model';
import { ConflictInspectorService } from './conflict-inspector.service';
import { ConflictScanResponse } from './conflict-inspector.model';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';

// OC-258: Conflict Inspector is now the middle step of Timetable Builder -> Conflict Inspector ->
// Draft Review. These tests cover the two things added for that: honoring a deep-linked
// termInstanceId/academicYearId (from Timetable Builder's "Check Conflicts" link) instead of always
// defaulting to the first term, and "Proceed to Review" acknowledging the term then navigating on.
describe('ConflictInspectorComponent', () => {
  let fixture: ComponentFixture<ConflictInspectorComponent>;
  let conflictInspectorService: {
    scan: ReturnType<typeof vi.fn>;
    acknowledge: ReturnType<typeof vi.fn>;
  };
  let router: { navigate: ReturnType<typeof vi.fn> };

  const academicYears: AcademicYear[] = [
    { id: 1, name: '2024-2025', startDate: '2024-06-01', endDate: '2025-05-31', isCurrent: true, createdAt: '', updatedAt: '' },
  ];
  const termInstances: TermInstance[] = [
    { id: 10, academicYearId: 1, academicYearName: '2024-2025', termType: 'ODD', startDate: '2024-06-01', endDate: '2024-11-30', status: 'OPEN', createdAt: '', updatedAt: '', workingSaturdayCount: 0 },
    { id: 20, academicYearId: 1, academicYearName: '2024-2025', termType: 'EVEN', startDate: '2024-12-01', endDate: '2025-05-31', status: 'OPEN', createdAt: '', updatedAt: '', workingSaturdayCount: 0 },
  ];
  const cleanScan: ConflictScanResponse = {
    termInstanceId: 20, termLabel: '2024-2025 EVEN', scannedAt: '', scannedCellCount: 2, violationCellCount: 0, violationCount: 0, countsByCode: {}, rows: [],
  };

  const internal = () => fixture.componentInstance as unknown as {
    scan: () => ConflictScanResponse | null;
    proceeding: () => boolean;
    selectedAcademicYearId: number | null;
    selectedTermInstanceId: number | null;
    onProceedToReview(): void;
  };

  function configure(queryParams: Record<string, string> = {}): void {
    conflictInspectorService = {
      scan: vi.fn(() => of(cleanScan)),
      acknowledge: vi.fn(() => of({ termInstanceId: 20, acknowledged: true, acknowledgedAt: '2024-06-01T00:00:00Z' })),
    };
    router = { navigate: vi.fn() };

    TestBed.configureTestingModule({
      imports: [ConflictInspectorComponent],
      providers: [
        { provide: AcademicYearService, useValue: {
          getAllAcademicYears: vi.fn(() => of(academicYears)),
          getTermInstancesByAcademicYear: vi.fn(() => of(termInstances)),
        } },
        { provide: ConflictInspectorService, useValue: conflictInspectorService },
        { provide: PermissionService, useValue: { has: vi.fn(() => true) } },
        { provide: ToastService, useValue: { success: vi.fn(), error: vi.fn() } },
        { provide: Router, useValue: router },
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap(queryParams) } } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ConflictInspectorComponent);
  }

  it('defaults to the current academic year and its first term with no query params', () => {
    configure();
    fixture.detectChanges();

    expect(internal().selectedAcademicYearId).toBe(1);
    expect(internal().selectedTermInstanceId).toBe(10);
  });

  it('honors a deep-linked termInstanceId (e.g. from Timetable Builder\'s "Check Conflicts" link)', () => {
    configure({ academicYearId: '1', termInstanceId: '20' });
    fixture.detectChanges();

    expect(internal().selectedTermInstanceId).toBe(20);
    expect(conflictInspectorService.scan).toHaveBeenCalledWith(20);
  });

  it('acknowledges the term then navigates to Draft Review on Proceed to Review', () => {
    configure({ academicYearId: '1', termInstanceId: '20' });
    fixture.detectChanges();

    internal().onProceedToReview();

    expect(conflictInspectorService.acknowledge).toHaveBeenCalledWith(20);
    expect(router.navigate).toHaveBeenCalledWith(['/timetable/timetable-builder']);
  });

  it('rescans instead of navigating when acknowledgment is rejected as no longer clean', () => {
    configure({ academicYearId: '1', termInstanceId: '20' });
    fixture.detectChanges();
    conflictInspectorService.acknowledge.mockReturnValue(throwError(() => ({ error: { message: 'no longer clean' } })));
    conflictInspectorService.scan.mockClear();

    internal().onProceedToReview();

    expect(router.navigate).not.toHaveBeenCalled();
    expect(conflictInspectorService.scan).toHaveBeenCalledWith(20);
  });
});

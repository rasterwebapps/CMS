import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

import { AttendanceListComponent } from './attendance-list.component';
import { AttendanceService } from '../attendance.service';
import { SubjectService } from '../../subject/subject.service';
import { Attendance } from '../attendance.model';
import { Subject } from '../../subject/subject.model';

// OC-242: this screen was completely unreachable before the fix -- getAll() always 400'd, so a
// subject must now be explicitly selected before any record loads. These tests cover exactly
// that fixed behavior, not a full re-test of the whole component.
describe('AttendanceListComponent', () => {
  let fixture: ComponentFixture<AttendanceListComponent>;
  let component: AttendanceListComponent;
  let attendanceService: { getBySubject: ReturnType<typeof vi.fn>; delete: ReturnType<typeof vi.fn> };
  let subjectService: { getAll: ReturnType<typeof vi.fn> };

  const subjects: Subject[] = [
    { id: 1, name: 'Adult Health Nursing I' } as Subject,
    { id: 2, name: 'Adult Health Nursing II' } as Subject,
  ];

  const records: Attendance[] = [
    { id: 100, studentId: 1, studentName: 'A', subjectId: 1, subjectName: 'AHN I', date: '2026-08-01', status: 'PRESENT', type: 'THEORY', createdAt: '', updatedAt: '' },
    { id: 101, studentId: 2, studentName: 'B', subjectId: 1, subjectName: 'AHN I', date: '2026-08-01', status: 'EXCUSED', type: 'THEORY', createdAt: '', updatedAt: '' },
  ];

  beforeEach(async () => {
    attendanceService = { getBySubject: vi.fn(() => of(records)), delete: vi.fn(() => of(undefined)) };
    subjectService = { getAll: vi.fn(() => of(subjects)) };

    await TestBed.configureTestingModule({
      imports: [AttendanceListComponent],
      providers: [
        provideRouter([]),
        { provide: AttendanceService, useValue: attendanceService },
        { provide: SubjectService, useValue: subjectService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AttendanceListComponent);
    component = fixture.componentInstance;
  });

  // onSubjectChange/onDateChange/onStatusFilterChange/dataSource are `protected` -- the
  // component's own public surface is its template, not these internals. Reaching into them
  // here (rather than driving the DOM) keeps this first example focused on the fixed OC-242
  // behavior itself; see the template/DOM-driven style in *-list.component.spec.ts files added
  // after this one for the alternative approach where it reads better.
  const internal = () => component as unknown as {
    dataSource: { data: Attendance[] };
    onSubjectChange(value: string): void;
    onDateChange(value: string): void;
    onStatusFilterChange(value: string): void;
  };

  it('loads the subject list on init but does not load any records yet', () => {
    fixture.detectChanges();

    expect(subjectService.getAll).toHaveBeenCalled();
    expect(attendanceService.getBySubject).not.toHaveBeenCalled();
    expect(internal().dataSource.data).toEqual([]);
  });

  it('loads records for the selected subject', () => {
    fixture.detectChanges();

    internal().onSubjectChange('1');

    expect(attendanceService.getBySubject).toHaveBeenCalledWith(1, undefined);
    expect(internal().dataSource.data).toEqual(records);
  });

  it('clears the table when the subject is deselected', () => {
    fixture.detectChanges();
    internal().onSubjectChange('1');

    internal().onSubjectChange('');

    expect(internal().dataSource.data).toEqual([]);
  });

  it('filters the loaded records by status client-side, without a new request', () => {
    fixture.detectChanges();
    internal().onSubjectChange('1');
    attendanceService.getBySubject.mockClear();

    internal().onStatusFilterChange('EXCUSED');

    expect(attendanceService.getBySubject).not.toHaveBeenCalled();
    expect(internal().dataSource.data).toEqual([records[1]]);
  });

  it('passes the date filter through to the backend when a subject is already selected', () => {
    fixture.detectChanges();
    internal().onSubjectChange('1');
    attendanceService.getBySubject.mockClear();

    internal().onDateChange('2026-08-01');

    expect(attendanceService.getBySubject).toHaveBeenCalledWith(1, '2026-08-01');
  });
});

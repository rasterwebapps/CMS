import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { describe, it, expect, beforeEach } from 'vitest';

import { CmsDayAgendaComponent, DayAgendaPeriod } from './day-agenda.component';
import { ClassScheduleOccurrence } from '../../features/timetable/timetable.model';

function occurrence(overrides: Partial<ClassScheduleOccurrence['session']> & { id: number }): ClassScheduleOccurrence {
  return {
    date: '2026-10-01',
    occurrenceStatus: 'HELD',
    cancelReason: null,
    session: {
      sessionType: 'THEORY',
      status: 'PUBLISHED',
      subjectName: 'Subject',
      subjectCode: 'SUB101',
      facultyName: 'Faculty',
      roomName: 'Room',
      batchName: null,
      dayOfWeek: 'THURSDAY',
      periodId: null,
      startTime: '09:00',
      endTime: '09:50',
      slotName: 'Period',
      ...overrides,
    },
  };
}

// Regression for a real user-reported bug: two concurrent sections (e.g. Lab - Section 1 - Batch 1
// vs Batch 2) at the same start/end time rendered as two separate same-time rows instead of one
// row with both sessions stacked together.
describe('CmsDayAgendaComponent', () => {
  let fixture: ComponentFixture<CmsDayAgendaComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [CmsDayAgendaComponent] }).compileComponents();
    fixture = TestBed.createComponent(CmsDayAgendaComponent);
    fixture.componentInstance.date = '2026-10-01';
  });

  it('without a periods list, stacks concurrent same-time occurrences into one row instead of two', () => {
    fixture.componentInstance.occurrences = [
      occurrence({ id: 1, subjectName: 'Nursing Research', batchName: 'Batch 1' }),
      occurrence({ id: 2, subjectName: 'Library', batchName: 'Batch 2' }),
    ];
    fixture.detectChanges();

    const rows = fixture.debugElement.queryAll(By.css('.day-agenda__row'));
    expect(rows.length).toBe(1);
    expect(rows[0].queryAll(By.css('.day-agenda__item')).length).toBe(2);
  });

  it('with a periods list, renders every period as its own row, blank ("Free") when nothing is scheduled', () => {
    const periods: DayAgendaPeriod[] = [
      { id: 1, name: 'Period 1', startTime: '09:00', endTime: '09:50', periodOrder: 1 },
      { id: 2, name: 'Period 2', startTime: '09:50', endTime: '10:40', periodOrder: 2 },
    ];
    fixture.componentInstance.periods = periods;
    fixture.componentInstance.occurrences = [occurrence({ id: 1, periodId: 1, startTime: '09:00', endTime: '09:50' })];
    fixture.detectChanges();

    const rows = fixture.debugElement.queryAll(By.css('.day-agenda__row'));
    expect(rows.length).toBe(2);
    expect(rows[0].queryAll(By.css('.day-agenda__item')).length).toBe(1);
    expect(rows[1].query(By.css('.day-agenda__free'))).toBeTruthy();
  });

  it('groups by periodId even when two occurrences fall in the same period, and falls back to time-matching for a null periodId', () => {
    const periods: DayAgendaPeriod[] = [{ id: 1, name: 'Period 1', startTime: '09:00', endTime: '09:50', periodOrder: 1 }];
    fixture.componentInstance.periods = periods;
    fixture.componentInstance.occurrences = [
      occurrence({ id: 1, periodId: 1, batchName: 'Batch 1' }),
      occurrence({ id: 2, periodId: 1, batchName: 'Batch 2' }),
      occurrence({ id: 3, periodId: null, startTime: '13:20', endTime: '14:10' }),
    ];
    fixture.detectChanges();

    const rows = fixture.debugElement.queryAll(By.css('.day-agenda__row'));
    expect(rows.length).toBe(2);
    expect(rows[0].queryAll(By.css('.day-agenda__item')).length).toBe(2);
    expect(rows[1].queryAll(By.css('.day-agenda__item')).length).toBe(1);
  });

  it('shows the empty state when there are no occurrences and no periods list', () => {
    fixture.componentInstance.occurrences = [];
    fixture.detectChanges();

    expect(fixture.debugElement.query(By.css('cms-empty-state'))).toBeTruthy();
    expect(fixture.debugElement.query(By.css('.day-agenda__row'))).toBeFalsy();
  });
});

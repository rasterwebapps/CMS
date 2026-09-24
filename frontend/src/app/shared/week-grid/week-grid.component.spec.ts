import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { describe, it, expect, beforeEach } from 'vitest';

import { CmsWeekGridComponent } from './week-grid.component';
import { WeekGridSession } from './week-grid.model';

// Regression for a real user-reported bug: LIBRARY sessions were showing an "Unstaffed — needs
// staffing" ribbon with no way to resolve it anywhere in the app. The backend
// (TimetableGenerationService's approve() unstaffedCount gate) already explicitly exempts LIBRARY
// rows from ever needing faculty -- they publish with just a classroom -- so the frontend showing
// this warning on every Library cell was a false alarm, never something a user could act on.
describe('CmsWeekGridComponent', () => {
  let fixture: ComponentFixture<CmsWeekGridComponent>;

  const baseSession: WeekGridSession = {
    id: 1,
    sessionType: 'THEORY',
    status: 'DRAFT',
    subjectName: 'Anatomy',
    subjectCode: 'ANAT101',
    facultyName: null,
    roomName: 'Room 101',
    batchName: null,
    dayOfWeek: 'MONDAY',
    periodId: 1,
    startTime: '09:00',
    endTime: '09:50',
    slotName: '1st Period',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CmsWeekGridComponent],
    }).compileComponents();
    fixture = TestBed.createComponent(CmsWeekGridComponent);
  });

  it('shows the "needs staffing" ribbon for an unstaffed THEORY session', () => {
    fixture.componentInstance.sessions = [{ ...baseSession, sessionType: 'THEORY' }];
    fixture.detectChanges();

    const chip = fixture.debugElement.query(By.css('.session-chip'));
    expect(chip.classes['session-chip--unstaffed']).toBe(true);
    expect(chip.nativeElement.textContent).toContain('Unstaffed — needs staffing');
  });

  it('does not show the "needs staffing" ribbon for an unstaffed LIBRARY session', () => {
    fixture.componentInstance.sessions = [{ ...baseSession, sessionType: 'LIBRARY', facultyName: null }];
    fixture.detectChanges();

    const chip = fixture.debugElement.query(By.css('.session-chip'));
    expect(chip.classes['session-chip--unstaffed']).toBeFalsy();
    expect(chip.nativeElement.textContent).not.toContain('Unstaffed');
  });

  it('still shows the "needs staffing" ribbon for an unstaffed SPORTS session (staffing applies there)', () => {
    fixture.componentInstance.sessions = [{ ...baseSession, sessionType: 'SPORTS', facultyName: null }];
    fixture.detectChanges();

    const chip = fixture.debugElement.query(By.css('.session-chip'));
    expect(chip.classes['session-chip--unstaffed']).toBe(true);
  });

  it('shows the assigned faculty name, not the ribbon, once a session is staffed', () => {
    fixture.componentInstance.sessions = [{ ...baseSession, facultyName: 'Dr. Rao' }];
    fixture.detectChanges();

    const chip = fixture.debugElement.query(By.css('.session-chip'));
    expect(chip.classes['session-chip--unstaffed']).toBeFalsy();
    expect(chip.nativeElement.textContent).toContain('Dr. Rao');
  });

  // A synthetic entry (e.g. a Clinical Shift Group duty roster block -- see backend
  // TimetableSkeletonService#findClinicalShiftGridEntries) has a negative id and no real
  // ClassSchedule row behind it, so it must render read-only: no swap/detail click emitted.
  it('renders a synthetic (negative-id) session read-only and does not emit sessionClick', () => {
    fixture.componentInstance.sessions = [{ ...baseSession, id: -1000001, sessionType: 'CLINICAL' }];
    fixture.detectChanges();
    let emitted: WeekGridSession | undefined;
    fixture.componentInstance.sessionClick.subscribe((s) => { emitted = s; });

    const chip = fixture.debugElement.query(By.css('.session-chip'));
    expect(chip.classes['session-chip--readonly']).toBe(true);
    chip.nativeElement.click();

    expect(emitted).toBeUndefined();
  });

  // Real bug: a Clinical Shift duty window (periodId null, its time window covering Periods 1-5,
  // e.g. a 06:00-14:10 bus-departure/return buffer) used to mint its own extra column labeled with
  // the raw time range instead of occupying the real Period 1-5 columns it actually blocks. Periods
  // 1-5 only have real (periodId-bearing) sessions on THURSDAY here -- mirrors production, where a
  // clinical day's own Periods 1-5 carry nothing else and the columns come from another day.
  it('spans a periodId-null entry across the real Period columns its time window covers, instead of adding a new column', () => {
    const periods = [
      { periodId: 1, startTime: '09:00:00', endTime: '09:50:00', slotName: 'Period 1' },
      { periodId: 2, startTime: '09:50:00', endTime: '10:40:00', slotName: 'Period 2' },
      { periodId: 3, startTime: '10:40:00', endTime: '11:30:00', slotName: 'Period 3' },
      { periodId: 4, startTime: '11:30:00', endTime: '12:20:00', slotName: 'Period 4' },
      { periodId: 5, startTime: '12:20:00', endTime: '13:10:00', slotName: 'Period 5' },
    ];
    const thursdaySessions: WeekGridSession[] = periods.map((p, i) => ({
      ...baseSession, id: 100 + i, dayOfWeek: 'THURSDAY', periodId: p.periodId,
      startTime: p.startTime, endTime: p.endTime, slotName: p.slotName,
    }));
    const clinicalShift: WeekGridSession = {
      ...baseSession, id: -1000001, sessionType: 'CLINICAL', dayOfWeek: 'MONDAY', periodId: null,
      startTime: '06:00:00', endTime: '14:10:00', slotName: '', subjectName: 'Community Health Nursing II — Off-campus Clinical Shift',
    };
    fixture.componentInstance.sessions = [...thursdaySessions, clinicalShift];
    fixture.detectChanges();

    const columnHeaders = fixture.debugElement.queryAll(By.css('.week-grid__period-hdr'));
    expect(columnHeaders.length).toBe(5); // no 6th "06:00:00–14:10:00" column

    const shiftCells = fixture.debugElement.queryAll(By.css('.week-grid__cell--shift'));
    expect(shiftCells.length).toBe(1);
    expect(shiftCells[0].nativeElement.style.gridColumn).toBe('span 5');
    expect(shiftCells[0].nativeElement.textContent).toContain('Community Health Nursing II');
  });

  it('emits sessionClick for a real (positive-id) session', () => {
    fixture.componentInstance.sessions = [baseSession];
    fixture.detectChanges();
    let emitted: WeekGridSession | undefined;
    fixture.componentInstance.sessionClick.subscribe((s) => { emitted = s; });

    const chip = fixture.debugElement.query(By.css('.session-chip'));
    chip.nativeElement.click();

    expect(emitted?.id).toBe(1);
  });

  // OC-258 Conflict Inspector acknowledgment gate: Draft Review disables Publish via this input
  // rather than hiding the toolbar, so the reason must show as a tooltip, not just a disabled state
  // that leaves the user with no idea why.
  describe('publishDisabledReason (review mode Publish gate)', () => {
    beforeEach(() => {
      fixture.componentInstance.mode = 'review';
      fixture.componentInstance.allowManage = true;
      fixture.componentInstance.sessions = [baseSession];
    });

    it('leaves Publish enabled with no tooltip when the reason is null', () => {
      fixture.componentInstance.publishDisabledReason = null;
      fixture.detectChanges();

      const publishBtn = fixture.debugElement.query(By.css('.week-grid-toolbar .btn-primary'));
      expect(publishBtn.nativeElement.disabled).toBe(false);
    });

    it('disables Publish and carries the reason as its tooltip when set', () => {
      fixture.componentInstance.publishDisabledReason = 'Run Conflict Inspector first.';
      fixture.detectChanges();

      const publishBtn = fixture.debugElement.query(By.css('.week-grid-toolbar .btn-primary'));
      expect(publishBtn.nativeElement.disabled).toBe(true);
    });
  });

  // The Date-wise-weekly view feeds this grid real dated occurrences instead of the recurring
  // template, so a specific date's CANCELLED/SUBSTITUTED outcome must render distinctly.
  describe('occurrenceStatus (Date-wise-weekly view)', () => {
    it('renders a CANCELLED session struck-through with its reason, hiding room/faculty', () => {
      fixture.componentInstance.sessions = [
        { ...baseSession, occurrenceStatus: 'CANCELLED', cancelReason: 'Republic Day' },
      ];
      fixture.detectChanges();

      const chip = fixture.debugElement.query(By.css('.session-chip'));
      expect(chip.classes['session-chip--cancelled']).toBe(true);
      expect(chip.nativeElement.textContent).toContain('Cancelled — Republic Day');
      expect(chip.nativeElement.textContent).not.toContain('Room 101');
    });

    it('renders a SUBSTITUTED session with its own styling and full details', () => {
      fixture.componentInstance.sessions = [
        { ...baseSession, occurrenceStatus: 'SUBSTITUTED', facultyName: 'Dr. Iyer' },
      ];
      fixture.detectChanges();

      const chip = fixture.debugElement.query(By.css('.session-chip'));
      expect(chip.classes['session-chip--substituted']).toBe(true);
      expect(chip.nativeElement.textContent).toContain('Dr. Iyer');
    });

    it('renders a plain session with no occurrenceStatus exactly as before (no cancelled/substituted class)', () => {
      fixture.componentInstance.sessions = [baseSession];
      fixture.detectChanges();

      const chip = fixture.debugElement.query(By.css('.session-chip'));
      expect(chip.classes['session-chip--cancelled']).toBeFalsy();
      expect(chip.classes['session-chip--substituted']).toBeFalsy();
    });
  });

  // Real user-reported gap: a term starting/ending mid-week (e.g. Thursday) still lets the week
  // navigator page back to that partial week (Thu-Sat of a term starting mid-week has real
  // sessions), but the Mon-Wed columns before the term started just rendered every period empty
  // with no explanation -- reading as if the app had silently lost those days' sessions, when
  // really the term just hadn't reached them yet. Same treatment as Holiday: a dimmed column plus
  // a small badge, so it's clear at a glance those columns are outside the term, not missing data.
  describe('isOutOfTerm ("Not in Term" tagging)', () => {
    beforeEach(() => {
      fixture.componentInstance.sessions = [baseSession]; // avoids the empty-state early return
    });

    it('tags Mon-Wed with "Not in Term" when the term starts Thursday of the viewed week', () => {
      fixture.componentInstance.weekStart = '2026-09-28'; // Monday; term starts Thu 2026-10-01
      fixture.componentInstance.termStartDate = '2026-10-01';
      fixture.componentInstance.termEndDate = '2027-03-31';
      fixture.detectChanges();

      const dayCells = fixture.debugElement.queryAll(By.css('.week-grid__day-cell'));
      const [mon, tue, wed, thu] = dayCells;
      for (const cell of [mon, tue, wed]) {
        expect(cell.classes['week-grid__day-cell--holiday']).toBe(true);
        expect(cell.nativeElement.textContent).toContain('Not in Term');
      }
      expect(thu.classes['week-grid__day-cell--holiday']).toBeFalsy();
      expect(thu.nativeElement.textContent).not.toContain('Not in Term');
    });

    it('tags Fri-Sat with "Not in Term" when the term ends Thursday of the viewed week', () => {
      fixture.componentInstance.weekStart = '2027-03-29'; // Monday; term ends Thu 2027-04-01
      fixture.componentInstance.termStartDate = '2026-10-01';
      fixture.componentInstance.termEndDate = '2027-04-01';
      fixture.detectChanges();

      const dayCells = fixture.debugElement.queryAll(By.css('.week-grid__day-cell'));
      const [mon, , , thu, fri, sat] = dayCells;
      expect(mon.nativeElement.textContent).not.toContain('Not in Term');
      expect(thu.nativeElement.textContent).not.toContain('Not in Term');
      expect(fri.nativeElement.textContent).toContain('Not in Term');
      expect(sat.nativeElement.textContent).toContain('Not in Term');
    });

    it('never tags any day when termStartDate/termEndDate are not passed (existing consumers unaffected)', () => {
      fixture.componentInstance.weekStart = '2026-09-28';
      fixture.detectChanges();

      const dayCells = fixture.debugElement.queryAll(By.css('.week-grid__day-cell'));
      expect(dayCells.some((d) => d.nativeElement.textContent.includes('Not in Term'))).toBe(false);
    });

    it('prefers the real Holiday badge over "Not in Term" if a day is somehow both', () => {
      fixture.componentInstance.weekStart = '2026-09-28';
      fixture.componentInstance.termStartDate = '2026-10-01';
      fixture.componentInstance.termEndDate = '2027-03-31';
      fixture.componentInstance.holidays = [{ dayIndex: 0, title: 'Gandhi Jayanti', category: 'GOVERNMENT' }];
      fixture.detectChanges();

      const monCell = fixture.debugElement.queryAll(By.css('.week-grid__day-cell'))[0];
      expect(monCell.nativeElement.textContent).toContain('Holiday');
      expect(monCell.nativeElement.textContent).not.toContain('Not in Term');
    });
  });

  // The published Week/Generic grid must hide Saturday for a 5-day term the same way Skeleton
  // Builder already does, driven by the term's real workingSaturdayCount (see
  // WorkingSaturdayCalculator#workingSaturdayCount, surfaced on TermInstanceDto).
  describe('workingSaturdayCount (Saturday column visibility)', () => {
    it('shows all 6 day columns when workingSaturdayCount is not passed (default, existing consumers unaffected)', () => {
      fixture.componentInstance.sessions = [baseSession];
      fixture.detectChanges();

      const dayCells = fixture.debugElement.queryAll(By.css('.week-grid__day-cell'));
      expect(dayCells.length).toBe(6);
      expect(dayCells.map((d) => d.nativeElement.textContent)).toEqual(
        expect.arrayContaining([expect.stringContaining('Sat')]));
    });

    it('hides the Saturday column when workingSaturdayCount is 0', () => {
      fixture.componentInstance.sessions = [baseSession];
      fixture.componentInstance.workingSaturdayCount = 0;
      fixture.detectChanges();

      const dayCells = fixture.debugElement.queryAll(By.css('.week-grid__day-cell'));
      expect(dayCells.length).toBe(5);
      expect(dayCells.some((d) => d.nativeElement.textContent.includes('Sat'))).toBe(false);
    });

    it('shows all 6 day columns when workingSaturdayCount is positive', () => {
      fixture.componentInstance.sessions = [baseSession];
      fixture.componentInstance.workingSaturdayCount = 12;
      fixture.detectChanges();

      const dayCells = fixture.debugElement.queryAll(By.css('.week-grid__day-cell'));
      expect(dayCells.length).toBe(6);
    });
  });
});

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
});

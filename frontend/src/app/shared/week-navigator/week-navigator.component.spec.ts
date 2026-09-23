import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { describe, it, expect, beforeEach } from 'vitest';

import { CmsWeekNavigatorComponent } from './week-navigator.component';

describe('CmsWeekNavigatorComponent', () => {
  let fixture: ComponentFixture<CmsWeekNavigatorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CmsWeekNavigatorComponent],
    }).compileComponents();
    fixture = TestBed.createComponent(CmsWeekNavigatorComponent);
    fixture.componentInstance.weekStart = '2026-09-21'; // a Monday
    fixture.detectChanges();
  });

  it('emits the Monday 7 days later on Next', () => {
    let emitted: string | undefined;
    fixture.componentInstance.weekStartChange.subscribe((v) => { emitted = v; });

    fixture.debugElement.query(By.css('button[aria-label="Next week"]')).nativeElement.click();

    expect(emitted).toBe('2026-09-28');
  });

  it('emits the Monday 7 days earlier on Previous', () => {
    let emitted: string | undefined;
    fixture.componentInstance.weekStartChange.subscribe((v) => { emitted = v; });

    fixture.debugElement.query(By.css('button[aria-label="Previous week"]')).nativeElement.click();

    expect(emitted).toBe('2026-09-14');
  });

  it('disables Next once the following week would fall outside max, and does not emit', () => {
    fixture.componentInstance.max = '2026-09-27';
    fixture.detectChanges();
    let emitted: string | undefined;
    fixture.componentInstance.weekStartChange.subscribe((v) => { emitted = v; });

    const nextBtn = fixture.debugElement.query(By.css('button[aria-label="Next week"]'));
    expect(nextBtn.nativeElement.disabled).toBe(true);
    nextBtn.nativeElement.click();

    expect(emitted).toBeUndefined();
  });

  it('disables Previous once the prior week would fall outside min, and does not emit', () => {
    fixture.componentInstance.min = '2026-09-15';
    fixture.detectChanges();
    let emitted: string | undefined;
    fixture.componentInstance.weekStartChange.subscribe((v) => { emitted = v; });

    const prevBtn = fixture.debugElement.query(By.css('button[aria-label="Previous week"]'));
    expect(prevBtn.nativeElement.disabled).toBe(true);
    prevBtn.nativeElement.click();

    expect(emitted).toBeUndefined();
  });

  it('clamps Today into [min, max] when the real current week falls outside the term', () => {
    fixture.componentInstance.min = '2020-01-06';
    fixture.componentInstance.max = '2020-01-13';
    fixture.detectChanges();
    let emitted: string | undefined;
    fixture.componentInstance.weekStartChange.subscribe((v) => { emitted = v; });

    fixture.debugElement.query(By.css('.week-navigator__today-btn')).nativeElement.click();

    expect(emitted).toBe('2020-01-13');
  });

  it('shows the Mon-Sat range label for the current weekStart', () => {
    const label = fixture.debugElement.query(By.css('.week-navigator__range'));
    expect(label.nativeElement.textContent).toContain('21 Sep');
    expect(label.nativeElement.textContent).toContain('2026');
    expect(label.nativeElement.textContent).toMatch(/26 Sept?/);
  });
});

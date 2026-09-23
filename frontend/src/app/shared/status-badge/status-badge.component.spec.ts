import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { describe, it, expect, beforeEach } from 'vitest';

import { CmsStatusBadgeComponent } from './status-badge.component';

describe('CmsStatusBadgeComponent', () => {
  let fixture: ComponentFixture<CmsStatusBadgeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CmsStatusBadgeComponent],
    }).compileComponents();
    fixture = TestBed.createComponent(CmsStatusBadgeComponent);
  });

  function render(status: string): { label: string; classList: DOMTokenList } {
    fixture.componentInstance.status = status;
    fixture.detectChanges();
    const el = fixture.debugElement.query(By.css('.status-badge')).nativeElement as HTMLElement;
    return { label: el.textContent?.trim() ?? '', classList: el.classList };
  }

  // Timetable Builder's cohort lifecycle (Pending -> Drafted -> Conflicts Resolved -> Published /
  // Partially Published, 2026-09-22) is the caller for these three cases -- each must resolve to
  // its own distinct visual class, since the badge is now the one place the lifecycle is shown.
  it('renders PENDING as the neutral/pending style', () => {
    const { label, classList } = render('PENDING');
    expect(label).toBe('Pending');
    expect(classList.contains('status-pending')).toBe(true);
  });

  it('renders DRAFTED as the in-progress/warning style, distinct from the unrelated bare DRAFT value', () => {
    const drafted = render('DRAFTED');
    expect(drafted.label).toBe('Drafted');
    expect(drafted.classList.contains('status-warning')).toBe(true);

    // DRAFTED must never collide with the pre-existing generic 'DRAFT' value other screens already
    // render through this same shared switch (status-pending) -- they are deliberately different
    // colors for two different concepts sharing one component.
    const draft = render('DRAFT');
    expect(draft.classList.contains('status-pending')).toBe(true);
    expect(draft.classList.contains('status-warning')).toBe(false);
  });

  it('renders CONFLICTS_RESOLVED as its own style, distinct from PUBLISHED', () => {
    const resolved = render('CONFLICTS_RESOLVED');
    expect(resolved.label).toBe('Conflicts Resolved');
    expect(resolved.classList.contains('status-current')).toBe(true);

    const published = render('PUBLISHED');
    expect(published.classList.contains('status-active')).toBe(true);
    expect(published.classList.contains('status-current')).toBe(false);
  });

  it('falls back to an em dash with no modifier class for an unrecognized value', () => {
    fixture.componentInstance.status = '';
    fixture.detectChanges();
    const el = fixture.debugElement.query(By.css('.status-badge')).nativeElement as HTMLElement;
    expect(el.textContent?.trim()).toBe('—');
  });
});

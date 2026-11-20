import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { describe, it, expect, beforeEach, vi } from 'vitest';

import {
  CounsellingStateConfirmDialogComponent,
  CounsellingStateConfirmDialogData,
} from './counselling-state-confirm-dialog.component';

describe('CounsellingStateConfirmDialogComponent', () => {
  let fixture: ComponentFixture<CounsellingStateConfirmDialogComponent>;
  let closeSpy: ReturnType<typeof vi.fn>;

  const dialogData: CounsellingStateConfirmDialogData = {
    title: 'Confirm Counselling Quota for Non-Tamil Nadu State',
    personLabel: 'Candidate',
    personName: 'Anjali Menon',
    categoryFieldLabel: 'Admission Quota',
    categoryValueLabel: 'Counselling Quota',
    stateName: 'Kerala',
    warningMessage:
      'Selected state is "Kerala" (outside Tamil Nadu). Counselling Quota is typically applicable only to Tamil Nadu state candidates.',
    confirmText: 'Confirm & Save Enquiry',
  };

  beforeEach(async () => {
    closeSpy = vi.fn();
    await TestBed.configureTestingModule({
      imports: [CounsellingStateConfirmDialogComponent],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: dialogData },
        { provide: MatDialogRef, useValue: { close: closeSpy } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CounsellingStateConfirmDialogComponent);
    fixture.detectChanges();
  });

  it('displays candidate name, quota, state, and warning message', () => {
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Anjali Menon');
    expect(text).toContain('Counselling Quota');
    expect(text).toContain('Kerala');
    expect(text).toContain('outside Tamil Nadu');
    expect(text).toContain('Confirm & Save Enquiry');
  });

  it('closes with true on confirm and false on cancel', () => {
    const buttons = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('button'),
    );
    const cancelBtn = buttons.find((b) => b.textContent?.includes('Cancel'))!;
    const confirmBtn = buttons.find((b) => b.textContent?.includes('Confirm & Save Enquiry'))!;

    cancelBtn.click();
    expect(closeSpy).toHaveBeenCalledWith(false);

    confirmBtn.click();
    expect(closeSpy).toHaveBeenCalledWith(true);
  });
});

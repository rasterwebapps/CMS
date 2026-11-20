import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { describe, it, expect, beforeEach, vi } from 'vitest';

import {
  AdmissionCategoryConfirmDialogComponent,
  AdmissionCategoryConfirmDialogData,
} from './admission-category-confirm-dialog.component';

describe('AdmissionCategoryConfirmDialogComponent', () => {
  let closeSpy: ReturnType<typeof vi.fn>;

  const baseDialogData: AdmissionCategoryConfirmDialogData = {
    studentName: 'AGALYA R',
    enquiryCategory: 'MANAGEMENT',
    oldFinalCategory: 'MANAGEMENT',
    newFinalCategory: 'MANAGEMENT_TO_COUNSELLING',
    studentState: 'Tamil Nadu',
  };

  async function createDialog(
    overrides: Partial<AdmissionCategoryConfirmDialogData> = {},
  ): Promise<ComponentFixture<AdmissionCategoryConfirmDialogComponent>> {
    closeSpy = vi.fn();
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [AdmissionCategoryConfirmDialogComponent],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: { ...baseDialogData, ...overrides } },
        { provide: MatDialogRef, useValue: { close: closeSpy } },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(AdmissionCategoryConfirmDialogComponent);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => {
    TestBed.resetTestingModule();
  });

  it('displays formatted categories and Tamil Nadu state without state mismatch warning', async () => {
    const fixture = await createDialog({ studentState: 'Tamil Nadu' });
    const el = fixture.nativeElement as HTMLElement;
    const text = el.textContent ?? '';
    expect(text).toContain('AGALYA R');
    expect(text).toContain('Management');
    expect(text).toContain('Management to Counselling');
    expect(text).toContain('Tamil Nadu');
    expect(text).toContain('Please verify before confirming.');
    expect(el.querySelector('.state-mismatch-banner')).toBeNull();
  });

  it('renders state eligibility advisory banner when changing to MANAGEMENT_TO_COUNSELLING or COUNSELLING for non-Tamil Nadu student', async () => {
    const keralaFixture = await createDialog({
      newFinalCategory: 'MANAGEMENT_TO_COUNSELLING',
      studentState: 'Kerala',
    });
    const keralaEl = keralaFixture.nativeElement as HTMLElement;
    expect(keralaEl.querySelector('.state-mismatch-banner')).not.toBeNull();
    expect(keralaEl.textContent).toContain('Kerala');
    expect(keralaEl.textContent).toContain('State Eligibility Advisory');

    const counsellingFixture = await createDialog({
      newFinalCategory: 'COUNSELLING',
      studentState: 'Uttar Pradesh',
    });
    const counsellingEl = counsellingFixture.nativeElement as HTMLElement;
    expect(counsellingEl.querySelector('.state-mismatch-banner')).not.toBeNull();
    expect(counsellingEl.textContent).toContain('Uttar Pradesh');
  });

  it('does not render state eligibility advisory when changing to LAPSED even for non-Tamil Nadu student', async () => {
    const fixture = await createDialog({
      newFinalCategory: 'LAPSED',
      studentState: 'Kerala',
    });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.state-mismatch-banner')).toBeNull();
  });

  it('keeps confirm disabled when reason is blank and closes with trimmed reason when provided', async () => {
    const fixture = await createDialog();
    const buttons = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('button'),
    );
    const confirmBtn = buttons.find((b) => b.textContent?.includes('Confirm'))!;
    expect(confirmBtn.disabled).toBe(true);

    const textarea = (fixture.nativeElement as HTMLElement).querySelector(
      '#adm-cat-change-reason',
    ) as HTMLTextAreaElement;
    textarea.value = '  Allotted via TN Govt Counselling Round 2  ';
    textarea.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(confirmBtn.disabled).toBe(false);
    confirmBtn.click();
    expect(closeSpy).toHaveBeenCalledWith('Allotted via TN Govt Counselling Round 2');
  });

  it('closes with null when cancelled', async () => {
    const fixture = await createDialog();
    const buttons = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('button'),
    );
    const cancelBtn = buttons.find((b) => b.textContent?.includes('Cancel'))!;
    cancelBtn.click();
    expect(closeSpy).toHaveBeenCalledWith(null);
  });
});

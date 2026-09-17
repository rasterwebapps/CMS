import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

import { ChangePasswordDialogComponent } from './change-password-dialog.component';
import { ProfileService } from '../../features/profile/profile.service';
import { AuthService } from '../../core/auth/auth.service';

// This dialog replaced a plain external link to Keycloak's own account console —
// these tests cover the client-side policy gating (so an invalid password never
// even reaches the backend) and the success → forced-logout hand-off, which is
// the whole point of the OC change: no more redirect to a Keycloak-branded page.
describe('ChangePasswordDialogComponent', () => {
  let fixture: ComponentFixture<ChangePasswordDialogComponent>;
  let component: ChangePasswordDialogComponent;
  let profileService: { changePassword: ReturnType<typeof vi.fn> };
  let authService: { logout: ReturnType<typeof vi.fn> };
  let dialogRef: { close: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    profileService = { changePassword: vi.fn(() => of(undefined)) };
    authService = { logout: vi.fn(() => Promise.resolve()) };
    dialogRef = { close: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [ChangePasswordDialogComponent],
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: ProfileService, useValue: profileService },
        { provide: AuthService, useValue: authService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ChangePasswordDialogComponent);
    component = fixture.componentInstance;
  });

  const internal = () => component as unknown as {
    currentPassword: { set(v: string): void };
    newPassword: { set(v: string): void };
    confirmPassword: { set(v: string): void };
    canSubmit(): boolean;
    submit(): void;
    cancel(): void;
  };

  it('blocks submit until every policy rule and the confirmation match are satisfied', () => {
    fixture.detectChanges();
    const c = internal();

    expect(c.canSubmit()).toBe(false);

    c.currentPassword.set('OldPass1');
    c.newPassword.set('short');
    c.confirmPassword.set('short');
    expect(c.canSubmit()).toBe(false); // too short, no uppercase requirement met either

    c.newPassword.set('longenough1');
    c.confirmPassword.set('longenough1');
    expect(c.canSubmit()).toBe(false); // no uppercase

    c.newPassword.set('Longenough1');
    c.confirmPassword.set('Longenough1');
    expect(c.canSubmit()).toBe(true);
  });

  it('rejects a new password identical to the current password', () => {
    fixture.detectChanges();
    const c = internal();

    c.currentPassword.set('SamePass1');
    c.newPassword.set('SamePass1');
    c.confirmPassword.set('SamePass1');

    expect(c.canSubmit()).toBe(false);
  });

  it('rejects a confirmation that does not match the new password', () => {
    fixture.detectChanges();
    const c = internal();

    c.currentPassword.set('OldPass1');
    c.newPassword.set('NewPass2');
    c.confirmPassword.set('NewPass3');

    expect(c.canSubmit()).toBe(false);
  });

  it('never calls the API when the form is invalid', () => {
    fixture.detectChanges();
    internal().submit();

    expect(profileService.changePassword).not.toHaveBeenCalled();
  });

  it('submits the current and new password to the API when valid', () => {
    fixture.detectChanges();
    const c = internal();
    c.currentPassword.set('OldPass1');
    c.newPassword.set('NewPass2');
    c.confirmPassword.set('NewPass2');

    c.submit();

    expect(profileService.changePassword).toHaveBeenCalledWith('OldPass1', 'NewPass2');
  });

  it('shows the success state, then closes the dialog and logs out shortly after', () => {
    vi.useFakeTimers();
    try {
      fixture.detectChanges();
      const c = internal();
      c.currentPassword.set('OldPass1');
      c.newPassword.set('NewPass2');
      c.confirmPassword.set('NewPass2');

      c.submit();
      expect((component as unknown as { success: () => boolean }).success()).toBe(true);
      expect(dialogRef.close).not.toHaveBeenCalled();

      vi.advanceTimersByTime(1800);

      expect(dialogRef.close).toHaveBeenCalledWith(true);
      expect(authService.logout).toHaveBeenCalled();
    } finally {
      vi.useRealTimers();
    }
  });

  it('surfaces the backend error message (e.g. wrong current password) without logging out', () => {
    profileService.changePassword.mockReturnValue(
      throwError(() => ({ error: { message: 'Current password is incorrect' } })));
    fixture.detectChanges();
    const c = internal();
    c.currentPassword.set('WrongPass');
    c.newPassword.set('NewPass2');
    c.confirmPassword.set('NewPass2');

    c.submit();

    expect((component as unknown as { errorMessage: () => string | null }).errorMessage())
      .toBe('Current password is incorrect');
    expect(authService.logout).not.toHaveBeenCalled();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('cancel closes the dialog without calling the API', () => {
    fixture.detectChanges();
    internal().cancel();

    expect(dialogRef.close).toHaveBeenCalledWith(false);
    expect(profileService.changePassword).not.toHaveBeenCalled();
  });
});

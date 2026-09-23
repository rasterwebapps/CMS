import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

import { AuthService } from '../../core/auth/auth.service';
import { ProfileService } from '../../features/profile/profile.service';

/**
 * Self-service "Change Password" dialog — entirely in-app, no redirect to
 * Keycloak's own account console. Verifies the current password server-side,
 * then the app logs the user out (their other sessions/devices are also
 * revoked server-side) so they sign back in with the new password.
 */
@Component({
  selector: 'app-change-password-dialog',
  standalone: true,
  imports: [FormsModule, MatDialogModule, MatIconModule, MatTooltipModule],
  templateUrl: './change-password-dialog.component.html',
  styleUrl: './change-password-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChangePasswordDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<ChangePasswordDialogComponent>);
  private readonly profileService = inject(ProfileService);
  private readonly authService = inject(AuthService);

  protected readonly currentPassword = signal('');
  protected readonly newPassword = signal('');
  protected readonly confirmPassword = signal('');

  protected readonly showCurrent = signal(false);
  protected readonly showNew = signal(false);
  protected readonly showConfirm = signal(false);

  protected readonly submitting = signal(false);
  protected readonly success = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  // ── Live policy checks (mirrors the realm's actual password policy) ────────
  protected readonly hasMinLength = computed(() => this.newPassword().length >= 8);
  protected readonly hasUppercase = computed(() => /[A-Z]/.test(this.newPassword()));
  protected readonly hasDigit = computed(() => /[0-9]/.test(this.newPassword()));
  protected readonly isDifferentFromCurrent = computed(() =>
    this.newPassword().length === 0 || this.newPassword() !== this.currentPassword());
  protected readonly passwordsMatch = computed(() =>
    this.confirmPassword().length > 0 && this.confirmPassword() === this.newPassword());

  protected readonly canSubmit = computed(() =>
    this.currentPassword().length > 0 &&
    this.hasMinLength() && this.hasUppercase() && this.hasDigit() &&
    this.isDifferentFromCurrent() && this.passwordsMatch() &&
    !this.submitting());

  protected cancel(): void {
    this.dialogRef.close(false);
  }

  protected submit(): void {
    if (!this.canSubmit()) return;

    this.submitting.set(true);
    this.errorMessage.set(null);

    this.profileService.changePassword(this.currentPassword(), this.newPassword()).subscribe({
      next: () => {
        this.submitting.set(false);
        this.success.set(true);
        // Give the user a moment to read the confirmation before the forced
        // sign-out redirect takes over the page.
        setTimeout(() => this.finishWithLogout(), 1800);
      },
      error: (err) => {
        this.submitting.set(false);
        this.errorMessage.set(err?.error?.message ?? 'Failed to change password. Please try again.');
      },
    });
  }

  private finishWithLogout(): void {
    this.dialogRef.close(true);
    void this.authService.logout();
  }
}

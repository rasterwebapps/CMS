import { Component, Input, OnChanges, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { GuardianService } from '../guardian.service';
import { StudentGuardianResponse } from '../guardian.model';
import { GuardianLinkDialogComponent, GuardianLinkDialogData } from '../guardian-link-dialog/guardian-link-dialog.component';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';
import { computeInitials } from '../../../shared/utils/initials';

/** Self-contained Guardians panel embedded as a tab on Student Detail. Kept as its own component
 *  (rather than folded into StudentDetailComponent directly) so that already-large component
 *  doesn't grow further -- this owns its own load/link/unlink state entirely. */
@Component({
  selector: 'app-student-guardians-tab',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, MatDialogModule, MatProgressSpinnerModule, MatTooltipModule],
  templateUrl: './student-guardians-tab.component.html',
  styleUrl: './student-guardians-tab.component.scss',
})
export class StudentGuardiansTabComponent implements OnChanges {
  @Input({ required: true }) studentId!: number;
  @Input({ required: true }) studentName!: string;

  private readonly guardianService = inject(GuardianService);
  protected readonly permissionService = inject(PermissionService);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);

  protected readonly guardians = signal<StudentGuardianResponse[]>([]);
  protected readonly loading = signal(false);
  protected readonly unlinkingId = signal<number | null>(null);

  ngOnChanges(): void {
    if (this.studentId) this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.guardianService.findByStudent(this.studentId).subscribe({
      next: (guardians) => { this.guardians.set(guardians); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load guardians'); this.loading.set(false); },
    });
  }

  protected canManage(): boolean {
    return this.permissionService.has('GUARDIAN_MANAGE');
  }

  protected initials(g: StudentGuardianResponse): string {
    return computeInitials(`${g.firstName} ${g.lastName}`);
  }

  protected openAddGuardian(): void {
    const dialogData: GuardianLinkDialogData = {
      studentId: this.studentId,
      studentName: this.studentName,
      alreadyLinkedGuardianIds: this.guardians().map((g) => g.id),
    };
    const ref = this.dialog.open(GuardianLinkDialogComponent, {
      width: '560px',
      maxWidth: '95vw',
      data: dialogData,
    });
    ref.afterClosed().subscribe((linked: boolean | undefined) => {
      if (linked) this.load();
    });
  }

  protected unlinkGuardian(g: StudentGuardianResponse): void {
    const dialogData: ConfirmDialogData = {
      title: 'Remove guardian?',
      message: `Remove ${g.firstName} ${g.lastName} as a guardian for ${this.studentName}?`,
      confirmText: 'Remove',
    };
    this.dialog.open(ConfirmDialogComponent, { width: '420px', data: dialogData })
      .afterClosed().subscribe((confirmed: boolean | undefined) => {
        if (!confirmed) return;
        this.unlinkingId.set(g.id);
        this.guardianService.unlinkWard(g.id, this.studentId).subscribe({
          next: () => {
            this.toast.success('Guardian removed');
            this.unlinkingId.set(null);
            this.load();
          },
          error: (err) => {
            this.toast.error(err?.error?.message ?? 'Failed to remove guardian');
            this.unlinkingId.set(null);
          },
        });
      });
  }
}

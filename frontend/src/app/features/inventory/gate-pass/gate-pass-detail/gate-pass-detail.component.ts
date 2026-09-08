import { Component, inject, OnInit, computed, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { GatePassService } from '../gate-pass.service';
import { GatePass } from '../gate-pass.model';
import { ConfirmDialogComponent } from '../../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsStatusBadgeComponent } from '../../../../shared/status-badge/status-badge.component';
import { ToastService } from '../../../../core/toast/toast.service';
import { PermissionService } from '../../../../core/permissions/permission.service';

@Component({
  selector: 'app-gate-pass-detail',
  standalone: true,
  imports: [
    RouterLink,
    FormsModule,
    DatePipe,
    MatDialogModule,
    MatProgressSpinnerModule,
    CmsStatusBadgeComponent,
  ],
  templateUrl: './gate-pass-detail.component.html',
  styleUrl: './gate-pass-detail.component.scss',
})
export class GatePassDetailComponent implements OnInit {
  private readonly route           = inject(ActivatedRoute);
  private readonly router          = inject(Router);
  private readonly gatePassService = inject(GatePassService);
  private readonly dialog          = inject(MatDialog);
  private readonly toast           = inject(ToastService);
  private readonly permissionService = inject(PermissionService);

  protected readonly loading = signal(false);
  protected readonly busy    = signal(false);
  protected readonly pass    = signal<GatePass | null>(null);

  protected readonly canApprove = computed(() => this.permissionService.has('INVENTORY_GATE_PASS_APPROVE'));
  protected readonly canVerify  = computed(() => this.permissionService.has('INVENTORY_GATE_PASS_VERIFY'));
  protected readonly canReturn  = computed(() => this.permissionService.has('INVENTORY_GATE_PASS_RETURN'));

  protected rejectReason = '';
  protected returnNotes = '';

  private passId!: number;

  ngOnInit(): void {
    this.passId = Number(this.route.snapshot.paramMap.get('id'));
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.gatePassService.getById(this.passId).subscribe({
      next: (p) => { this.pass.set(p); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load gate pass'); this.loading.set(false); },
    });
  }

  protected approve(): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Approve Gate Pass', message: 'This approves the gate pass. Continue?', confirmText: 'Approve', cancelText: 'Cancel' },
    }).afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.busy.set(true);
      this.gatePassService.approve(this.passId).subscribe({
        next: () => { this.toast.success('Gate pass approved'); this.busy.set(false); this.load(); },
        error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to approve gate pass'); this.busy.set(false); },
      });
    });
  }

  protected reject(): void {
    if (!this.rejectReason.trim()) {
      this.toast.error('A reason is required to reject a gate pass');
      return;
    }
    this.busy.set(true);
    this.gatePassService.reject(this.passId, { reason: this.rejectReason.trim() }).subscribe({
      next: () => { this.toast.success('Gate pass rejected'); this.busy.set(false); this.load(); },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to reject gate pass'); this.busy.set(false); },
    });
  }

  protected verifyGate(): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Verify at Gate', message: 'This confirms security has physically verified the item at the gate. Continue?', confirmText: 'Verify', cancelText: 'Cancel' },
    }).afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.busy.set(true);
      this.gatePassService.verifyGate(this.passId).subscribe({
        next: () => { this.toast.success('Gate pass verified'); this.busy.set(false); this.load(); },
        error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to verify gate pass'); this.busy.set(false); },
      });
    });
  }

  protected markReturned(): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Mark Returned', message: 'This marks the item as returned. Continue?', confirmText: 'Mark Returned', cancelText: 'Cancel' },
    }).afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.busy.set(true);
      this.gatePassService.markReturned(this.passId, { notes: this.returnNotes.trim() || undefined }).subscribe({
        next: () => { this.toast.success('Gate pass marked returned'); this.busy.set(false); this.load(); },
        error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to mark gate pass returned'); this.busy.set(false); },
      });
    });
  }

  protected goBack(): void {
    void this.router.navigate(['/inventory/gate-pass/gate-passes']);
  }
}

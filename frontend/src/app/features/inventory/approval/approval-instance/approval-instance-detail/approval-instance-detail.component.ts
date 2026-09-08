import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ApprovalInstanceService } from '../approval-instance.service';
import { ApprovalAction, ApprovalInstance } from '../approval-instance.model';
import { CmsStatusBadgeComponent } from '../../../../../shared/status-badge/status-badge.component';
import { ToastService } from '../../../../../core/toast/toast.service';

@Component({
  selector: 'app-approval-instance-detail',
  standalone: true,
  imports: [
    RouterLink,
    FormsModule,
    DatePipe,
    MatProgressSpinnerModule,
    CmsStatusBadgeComponent,
  ],
  templateUrl: './approval-instance-detail.component.html',
  styleUrl: './approval-instance-detail.component.scss',
})
export class ApprovalInstanceDetailComponent implements OnInit {
  private readonly route           = inject(ActivatedRoute);
  private readonly router          = inject(Router);
  private readonly instanceService = inject(ApprovalInstanceService);
  private readonly toast           = inject(ToastService);

  protected readonly loading  = signal(false);
  protected readonly busy     = signal(false);
  protected readonly instance = signal<ApprovalInstance | null>(null);

  protected notesByAction: Record<number, string> = {};

  private instanceId!: number;

  ngOnInit(): void {
    this.instanceId = Number(this.route.snapshot.paramMap.get('id'));
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.instanceService.getById(this.instanceId).subscribe({
      next: (i) => { this.instance.set(i); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load approval'); this.loading.set(false); },
    });
  }

  protected approveAction(action: ApprovalAction): void {
    this.busy.set(true);
    this.instanceService.approveAction(this.instanceId, action.id, { notes: this.notesByAction[action.id]?.trim() || undefined }).subscribe({
      next: () => { this.toast.success('Step approved'); this.busy.set(false); this.load(); },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to approve step'); this.busy.set(false); },
    });
  }

  protected rejectAction(action: ApprovalAction): void {
    this.busy.set(true);
    this.instanceService.rejectAction(this.instanceId, action.id, { notes: this.notesByAction[action.id]?.trim() || undefined }).subscribe({
      next: () => { this.toast.success('Approval rejected'); this.busy.set(false); this.load(); },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to reject step'); this.busy.set(false); },
    });
  }

  protected goBack(): void {
    void this.router.navigate(['/inventory/approval/instances']);
  }
}

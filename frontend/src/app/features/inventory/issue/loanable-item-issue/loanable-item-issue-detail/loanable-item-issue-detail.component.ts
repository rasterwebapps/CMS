import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { LoanableItemIssueService } from '../loanable-item-issue.service';
import { LoanableItemIssue } from '../loanable-item-issue.model';
import { ConfirmDialogComponent } from '../../../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsStatusBadgeComponent } from '../../../../../shared/status-badge/status-badge.component';
import { ToastService } from '../../../../../core/toast/toast.service';

@Component({
  selector: 'app-loanable-item-issue-detail',
  standalone: true,
  imports: [
    RouterLink,
    FormsModule,
    DatePipe,
    MatDialogModule,
    MatProgressSpinnerModule,
    CmsStatusBadgeComponent,
  ],
  templateUrl: './loanable-item-issue-detail.component.html',
  styleUrl: './loanable-item-issue-detail.component.scss',
})
export class LoanableItemIssueDetailComponent implements OnInit {
  private readonly route        = inject(ActivatedRoute);
  private readonly router       = inject(Router);
  private readonly issueService = inject(LoanableItemIssueService);
  private readonly dialog       = inject(MatDialog);
  private readonly toast        = inject(ToastService);

  protected readonly loading = signal(false);
  protected readonly busy    = signal(false);
  protected readonly issue   = signal<LoanableItemIssue | null>(null);

  protected conditionOnReturn = '';

  private issueId!: number;

  ngOnInit(): void {
    this.issueId = Number(this.route.snapshot.paramMap.get('id'));
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.issueService.getById(this.issueId).subscribe({
      next: (i) => { this.issue.set(i); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load loanable item issue'); this.loading.set(false); },
    });
  }

  protected markReturned(): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Mark Returned',
        message: 'This marks the item as returned. Continue?',
        confirmText: 'Mark Returned',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.busy.set(true);
      this.issueService.markReturned(this.issueId, { conditionOnReturn: this.conditionOnReturn.trim() || undefined }).subscribe({
        next: () => { this.toast.success('Item marked returned'); this.busy.set(false); this.load(); },
        error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to mark item returned'); this.busy.set(false); },
      });
    });
  }

  protected goBack(): void {
    void this.router.navigate(['/inventory/issue/loanable-item-issues']);
  }
}

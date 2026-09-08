import { Component, inject, OnInit, computed, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ServiceTicketService } from '../ticket.service';
import { ServiceTicket } from '../ticket.model';
import { ConfirmDialogComponent } from '../../../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsStatusBadgeComponent } from '../../../../../shared/status-badge/status-badge.component';
import { ToastService } from '../../../../../core/toast/toast.service';
import { PermissionService } from '../../../../../core/permissions/permission.service';

@Component({
  selector: 'app-service-ticket-detail',
  standalone: true,
  imports: [
    RouterLink,
    FormsModule,
    DatePipe,
    MatDialogModule,
    MatProgressSpinnerModule,
    CmsStatusBadgeComponent,
  ],
  templateUrl: './ticket-detail.component.html',
  styleUrl: './ticket-detail.component.scss',
})
export class ServiceTicketDetailComponent implements OnInit {
  private readonly route             = inject(ActivatedRoute);
  private readonly router            = inject(Router);
  private readonly ticketService     = inject(ServiceTicketService);
  private readonly dialog            = inject(MatDialog);
  private readonly toast             = inject(ToastService);
  private readonly permissionService = inject(PermissionService);

  protected readonly loading = signal(false);
  protected readonly busy    = signal(false);
  protected readonly ticket  = signal<ServiceTicket | null>(null);

  protected readonly canAssign  = computed(() => this.permissionService.has('INVENTORY_SERVICE_TICKET_ASSIGN'));
  protected readonly canResolve = computed(() => this.permissionService.has('INVENTORY_SERVICE_TICKET_RESOLVE'));
  protected readonly canClose   = computed(() => this.permissionService.has('INVENTORY_SERVICE_TICKET_CLOSE'));
  protected readonly canManage  = computed(() => this.permissionService.has('INVENTORY_SERVICE_TICKET_MANAGE'));

  protected assignedTo = '';
  protected resolutionNotes = '';
  protected feedbackRating: number | null = null;
  protected cancelReason = '';

  private ticketId!: number;

  ngOnInit(): void {
    this.ticketId = Number(this.route.snapshot.paramMap.get('id'));
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.ticketService.getById(this.ticketId).subscribe({
      next: (t) => { this.ticket.set(t); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load service ticket'); this.loading.set(false); },
    });
  }

  protected assign(): void {
    if (!this.assignedTo.trim()) {
      this.toast.error('Enter who this ticket is being assigned to');
      return;
    }
    this.busy.set(true);
    this.ticketService.assign(this.ticketId, { assignedTo: this.assignedTo.trim() }).subscribe({
      next: () => { this.toast.success('Ticket assigned'); this.busy.set(false); this.load(); },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to assign ticket'); this.busy.set(false); },
    });
  }

  protected resolve(): void {
    if (!this.resolutionNotes.trim()) {
      this.toast.error('Enter resolution notes');
      return;
    }
    this.busy.set(true);
    this.ticketService.resolve(this.ticketId, { resolutionNotes: this.resolutionNotes.trim() }).subscribe({
      next: () => { this.toast.success('Ticket resolved'); this.busy.set(false); this.load(); },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to resolve ticket'); this.busy.set(false); },
    });
  }

  protected close(): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Close Ticket', message: 'This closes the ticket. Continue?', confirmText: 'Close Ticket', cancelText: 'Cancel' },
    }).afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.busy.set(true);
      this.ticketService.close(this.ticketId, { feedbackRating: this.feedbackRating ?? undefined }).subscribe({
        next: () => { this.toast.success('Ticket closed'); this.busy.set(false); this.load(); },
        error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to close ticket'); this.busy.set(false); },
      });
    });
  }

  protected cancel(): void {
    if (!this.cancelReason.trim()) {
      this.toast.error('A reason is required to cancel a ticket');
      return;
    }
    this.busy.set(true);
    this.ticketService.cancel(this.ticketId, { reason: this.cancelReason.trim() }).subscribe({
      next: () => { this.toast.success('Ticket cancelled'); this.busy.set(false); this.load(); },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to cancel ticket'); this.busy.set(false); },
    });
  }

  protected goBack(): void {
    void this.router.navigate(['/inventory/ticket/tickets']);
  }
}

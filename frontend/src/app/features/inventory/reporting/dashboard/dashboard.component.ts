import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { Router } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { InventoryDashboardService } from './dashboard.service';
import { InventoryDashboard } from './dashboard.model';
import { ToastService } from '../../../../core/toast/toast.service';

interface StatTile {
  label: string;
  value: number | string;
  route: string;
  accent: boolean;
}

@Component({
  selector: 'app-inventory-dashboard',
  standalone: true,
  imports: [
    DatePipe,
    DecimalPipe,
    MatProgressSpinnerModule,
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class InventoryDashboardComponent implements OnInit {
  private readonly dashboardService = inject(InventoryDashboardService);
  private readonly router           = inject(Router);
  private readonly toast            = inject(ToastService);

  protected readonly loading   = signal(false);
  protected readonly dashboard = signal<InventoryDashboard | null>(null);
  protected readonly tiles     = signal<StatTile[]>([]);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.dashboardService.get().subscribe({
      next: (d) => {
        this.dashboard.set(d);
        this.tiles.set([
          { label: 'Open Purchase Orders', value: d.openPurchaseOrders, route: '/inventory/procurement/purchase-orders', accent: false },
          { label: 'Pending Purchase Requisitions', value: d.pendingPurchaseRequisitions, route: '/inventory/procurement/purchase-requisitions', accent: false },
          { label: 'Pending Wanted List Items', value: d.pendingWantedListItems, route: '/inventory/procurement/wanted-list', accent: false },
          { label: 'Active Approvals', value: d.activeApprovalInstances, route: '/inventory/approval/instances', accent: false },
          { label: 'Overdue Gate Passes', value: d.overdueGatePasses, route: '/inventory/gate-pass/gate-passes', accent: d.overdueGatePasses > 0 },
          { label: 'Overdue Loanable Items', value: d.overdueLoanableItems, route: '/inventory/issue/loanable-item-issues', accent: d.overdueLoanableItems > 0 },
          { label: 'Open Service Tickets', value: d.openServiceTickets, route: '/inventory/ticket/tickets', accent: false },
          { label: 'Urgent Open Tickets', value: d.urgentOpenServiceTickets, route: '/inventory/ticket/tickets', accent: d.urgentOpenServiceTickets > 0 },
          { label: 'Over-Allocated Budgets', value: d.overAllocatedBudgets, route: '/inventory/budget/budgets', accent: d.overAllocatedBudgets > 0 },
          { label: 'Consignment Outstanding Liability', value: d.consignmentOutstandingLiability, route: '/inventory/consignment/stock-lines', accent: false },
          { label: 'Assets Under Maintenance', value: d.assetsUnderMaintenance, route: '/inventory/asset/assets', accent: false },
        ]);
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load the inventory dashboard'); this.loading.set(false); },
    });
  }

  protected openTile(tile: StatTile): void {
    void this.router.navigate([tile.route]);
  }
}

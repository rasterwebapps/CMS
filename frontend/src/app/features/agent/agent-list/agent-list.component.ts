import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AgentService } from '../agent.service';
import { Agent } from '../agent.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { AGENT_LIST_TOUR } from '../../../shared/tour/tours/agent.tours';

@Component({
  selector: 'app-agent-list',
  standalone: true,
  imports: [
    RouterLink,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatDialogModule,
    MatTooltipModule,
    CmsEmptyStateComponent,
    CmsTourButtonComponent,
  ],
  templateUrl: './agent-list.component.html',
  styleUrl: './agent-list.component.scss',
})
export class AgentListComponent implements OnInit {
  private readonly agentService = inject(AgentService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly tourService = inject(TourService);

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  private readonly VIEW_MODE_KEY = 'agent-view-mode';

  protected readonly displayedColumns = ['name', 'phone', 'email', 'area', 'allottedSeats', 'isActive', 'actions'];
  protected readonly dataSource = new MatTableDataSource<Agent>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly viewMode = signal<'card' | 'table'>(this.loadViewMode());

  private readonly allItems = signal<Agent[]>([]);

  protected readonly totalCount = computed(() => this.allItems().length);
  protected readonly activeCount = computed(() => this.allItems().filter(a => a.isActive).length);

  protected readonly filteredAgents = computed(() => {
    const q = this.searchValue().trim().toLowerCase();
    if (!q) return this.allItems();
    return this.allItems().filter(
      a =>
        a.name.toLowerCase().includes(q) ||
        (a.phone?.toLowerCase().includes(q) ?? false) ||
        (a.email?.toLowerCase().includes(q) ?? false) ||
        (a.area?.toLowerCase().includes(q) ?? false),
    );
  });

  ngOnInit(): void {
    this.tourService.register('agent-list', AGENT_LIST_TOUR);
    this.load();
  }

  protected setViewMode(mode: 'card' | 'table'): void {
    this.viewMode.set(mode);
    localStorage.setItem(this.VIEW_MODE_KEY, mode);
  }

  protected applyFilter(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchValue.set(value);
    this.dataSource.filter = value.trim().toLowerCase();
    if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
  }

  protected clearFilter(): void {
    this.searchValue.set('');
    this.dataSource.filter = '';
  }

  protected edit(item: Agent): void {
    void this.router.navigate(['/agents', item.id, 'edit']);
  }

  protected delete(item: Agent): void {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: { title: 'Delete Agent', message: `Delete "${item.name}"?`, confirmText: 'Delete', cancelText: 'Cancel' },
      })
      .afterClosed()
      .subscribe((confirmed) => { if (confirmed) this.doDelete(item); });
  }

  protected handleEmptyAction(): void {
    if (this.searchValue()) {
      this.clearFilter();
    } else {
      void this.router.navigate(['/agents/new']);
    }
  }

  private loadViewMode(): 'card' | 'table' {
    const stored = localStorage.getItem(this.VIEW_MODE_KEY);
    return stored === 'table' ? 'table' : 'card';
  }

  private doDelete(item: Agent): void {
    this.loading.set(true);
    this.agentService.deleteAgent(item.id).subscribe({
      next: () => { this.toast.success('Deleted successfully'); this.load(); },
      error: () => { this.toast.error('Failed to delete'); this.loading.set(false); },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.agentService.getAgents().subscribe({
      next: (data) => {
        this.allItems.set(data);
        this.dataSource.data = data;
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load'); this.loading.set(false); },
    });
  }
}

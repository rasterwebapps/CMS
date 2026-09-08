import { Component, inject, OnInit, OnDestroy, ViewChild, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { Subject, Subscription } from 'rxjs';
import { ApprovalWorkflowService } from '../approval-workflow.service';
import { ApprovalWorkflow } from '../approval-workflow.model';
import { CmsEmptyStateComponent } from '../../../../../shared/empty-state/empty-state.component';
import { CmsRowActionButtonComponent } from '../../../../../shared/row-action-button/row-action-button.component';
import { CmsIconEditComponent } from '../../../../../shared/icons';
import { ToastService } from '../../../../../core/toast/toast.service';

@Component({
  selector: 'app-approval-workflow-list',
  standalone: true,
  imports: [
    FormsModule,
    MatTableModule,
    MatPaginatorModule,
    CmsEmptyStateComponent,
    CmsRowActionButtonComponent,
    CmsIconEditComponent,
  ],
  templateUrl: './approval-workflow-list.component.html',
  styleUrl: './approval-workflow-list.component.scss',
})
export class ApprovalWorkflowListComponent implements OnInit, OnDestroy {
  private readonly workflowService = inject(ApprovalWorkflowService);
  private readonly router          = inject(Router);
  private readonly toast           = inject(ToastService);

  private readonly destroy$ = new Subject<void>();
  private _paginator?: MatPaginator;
  private _paginatorSub?: Subscription;

  @ViewChild(MatPaginator) set paginatorRef(p: MatPaginator | undefined) {
    if (!p || p === this._paginator) return;
    this._paginatorSub?.unsubscribe();
    this._paginator = p;
    p.pageIndex = this.currentPage;
    p.pageSize = this.currentPageSize;
    this._paginatorSub = p.page.pipe().subscribe((e: PageEvent) => {
      this.currentPage = e.pageIndex;
      this.currentPageSize = e.pageSize;
      this.loadPage();
    });
  }

  protected readonly displayedColumns = ['name', 'documentType', 'locationVirtualName', 'minAmount', 'stepCount', 'isActive', 'actions'];
  protected readonly dataSource = new MatTableDataSource<ApprovalWorkflow>([]);
  protected readonly loading = signal(false);

  protected documentTypeFilter: string | null = null;
  protected totalElements = 0;
  protected currentPage = 0;
  protected currentPageSize = 25;

  ngOnInit(): void {
    this.loadPage();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this._paginatorSub?.unsubscribe();
  }

  protected onFilterChange(): void {
    this.currentPage = 0;
    this.loadPage();
  }

  protected goToNew(): void {
    void this.router.navigate(['/inventory/approval/workflows/new']);
  }

  protected editWorkflow(item: ApprovalWorkflow): void {
    void this.router.navigate(['/inventory/approval/workflows', item.id, 'edit']);
  }

  private loadPage(): void {
    this.loading.set(true);
    this.workflowService.getPage({
      documentType: this.documentTypeFilter,
      page: this.currentPage,
      size: this.currentPageSize,
    }).subscribe({
      next: (page) => {
        this.dataSource.data = page.content;
        this.totalElements = page.totalElements;
        if (this._paginator) {
          this._paginator.length = page.totalElements;
          this._paginator.pageIndex = page.number;
        }
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load approval workflows'); this.loading.set(false); },
    });
  }
}

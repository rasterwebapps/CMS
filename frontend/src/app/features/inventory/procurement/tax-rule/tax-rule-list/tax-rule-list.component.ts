import { Component, inject, OnInit, OnDestroy, ViewChild, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { TaxRuleService } from '../tax-rule.service';
import { TaxRule } from '../tax-rule.model';
import { ConfirmDialogComponent } from '../../../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../../../shared/empty-state/empty-state.component';
import { CmsStatusBadgeComponent } from '../../../../../shared/status-badge/status-badge.component';
import { CmsRowActionButtonComponent } from '../../../../../shared/row-action-button/row-action-button.component';
import { CmsIconEditComponent, CmsIconToggleStatusComponent } from '../../../../../shared/icons';
import { ToastService } from '../../../../../core/toast/toast.service';

@Component({
  selector: 'app-tax-rule-list',
  standalone: true,
  imports: [
    FormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatDialogModule,
    CmsEmptyStateComponent,
    CmsStatusBadgeComponent,
    CmsRowActionButtonComponent,
    CmsIconEditComponent,
    CmsIconToggleStatusComponent,
  ],
  templateUrl: './tax-rule-list.component.html',
  styleUrl: './tax-rule-list.component.scss',
})
export class TaxRuleListComponent implements OnInit, OnDestroy {
  private readonly taxRuleService = inject(TaxRuleService);
  private readonly router         = inject(Router);
  private readonly toast          = inject(ToastService);
  private readonly dialog         = inject(MatDialog);

  private readonly destroy$ = new Subject<void>();
  private readonly searchSubject = new Subject<string>();
  private _paginator?: MatPaginator;
  private _paginatorSub?: Subscription;

  @ViewChild(MatPaginator) set paginatorRef(p: MatPaginator | undefined) {
    if (!p || p === this._paginator) return;
    this._paginatorSub?.unsubscribe();
    this._paginator = p;
    p.pageIndex = this.currentPage;
    p.pageSize = this.currentPageSize;
    this._paginatorSub = p.page.pipe(takeUntil(this.destroy$)).subscribe((e: PageEvent) => {
      this.currentPage = e.pageIndex;
      this.currentPageSize = e.pageSize;
      this.loadPage();
    });
  }

  protected readonly displayedColumns = ['name', 'ratePercent', 'isActive', 'actions'];
  protected readonly dataSource = new MatTableDataSource<TaxRule>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');

  protected totalElements = 0;
  protected currentPage = 0;
  protected currentPageSize = 25;

  ngOnInit(): void {
    this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      takeUntil(this.destroy$),
    ).subscribe(() => {
      this.currentPage = 0;
      this.loadPage();
    });
    this.loadPage();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this._paginatorSub?.unsubscribe();
  }

  protected applyFilter(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchValue.set(value);
    this.searchSubject.next(value);
  }

  protected clearFilter(): void {
    this.searchValue.set('');
    this.searchSubject.next('');
  }

  protected goToNew(): void {
    void this.router.navigate(['/inventory/procurement/tax-rules/new']);
  }

  protected editTaxRule(item: TaxRule): void {
    void this.router.navigate(['/inventory/procurement/tax-rules', item.id, 'edit']);
  }

  protected toggleStatus(item: TaxRule): void {
    const nextAction = item.isActive ? 'Deactivate' : 'Activate';
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: `${nextAction} Tax Rule`,
        message: `${nextAction} "${item.name}"?`,
        confirmText: nextAction,
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe(confirmed => {
      if (confirmed) this.performToggle(item);
    });
  }

  protected handleEmptyAction(): void {
    if (this.searchValue()) {
      this.clearFilter();
    } else {
      this.goToNew();
    }
  }

  private performToggle(item: TaxRule): void {
    this.loading.set(true);
    this.taxRuleService.updateStatus(item.id, { isActive: !item.isActive }).subscribe({
      next: () => {
        this.toast.success(`Tax rule ${item.isActive ? 'deactivated' : 'activated'} successfully`);
        this.loadPage();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? `Failed to ${item.isActive ? 'deactivate' : 'activate'} tax rule`);
        this.loading.set(false);
      },
    });
  }

  private loadPage(): void {
    this.loading.set(true);
    const search = this.searchValue().trim() || undefined;
    this.taxRuleService.getPage({ search, page: this.currentPage, size: this.currentPageSize }).subscribe({
      next: (page) => {
        this.dataSource.data = page.content;
        this.totalElements = page.totalElements;
        if (this._paginator) {
          this._paginator.length = page.totalElements;
          this._paginator.pageIndex = page.number;
        }
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load tax rules'); this.loading.set(false); },
    });
  }
}

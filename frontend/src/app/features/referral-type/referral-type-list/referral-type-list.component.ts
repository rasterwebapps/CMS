import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CurrencyPipe } from '@angular/common';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { ReferralTypeService } from '../referral-type.service';
import { ReferralType } from '../referral-type.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { ToastService } from '../../../core/toast/toast.service';

@Component({
  selector: 'app-referral-type-list',
  standalone: true,
  imports: [
    PageHeaderComponent,
    RouterLink,
    FormsModule,
    CurrencyPipe,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatTooltipModule,
    MatChipsModule],
  templateUrl: './referral-type-list.component.html',
  styleUrl: './referral-type-list.component.scss',
})
export class ReferralTypeListComponent implements OnInit {
  private readonly referralTypeService = inject(ReferralTypeService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  // ── Column visibility ────────────────────────────────────────────────────
  protected readonly ALL_COLS = ['name', 'code', 'hasCommission', 'commissionAmount', 'isActive', 'actions'];
  protected readonly COLUMN_LABELS: Record<string, string> = {
    name: 'Name',
    code: 'Code',
    hasCommission: 'Commission',
    commissionAmount: 'Amount',
    isActive: 'Active',
    actions: 'Actions',
  };
  private readonly COLS_KEY = 'referral-type-list-cols';
  private readonly _visibleCols = signal<Set<string>>(this._loadColPrefs());
  protected readonly displayedColumns = computed(() => this.ALL_COLS.filter(c => this._visibleCols().has(c)));
  protected readonly dataSource = new MatTableDataSource<ReferralType>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');

  ngOnInit(): void {
    this.load();
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

  protected edit(item: ReferralType): void {
    void this.router.navigate(['/referral-types', item.id, 'edit']);
  }

  protected delete(item: ReferralType): void {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: {
          title: 'Delete Referral Type',
          message: `Delete "${item.name}"?`,
          confirmText: 'Delete',
          cancelText: 'Cancel',
        },
      })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) this.doDelete(item);
      });
  }

  private _loadColPrefs(): Set<string> {
    try {
      const s = localStorage.getItem(this.COLS_KEY);
      if (s) return new Set<string>(JSON.parse(s) as string[]);
    } catch { /* empty */ }
    return new Set<string>(this.ALL_COLS);
  }

  protected toggleColumn(col: string): void {
    this._visibleCols.update(s => {
      const next = new Set(s);
      if (next.size > 1 && next.has(col)) { next.delete(col); } else { next.add(col); }
      localStorage.setItem(this.COLS_KEY, JSON.stringify([...next]));
      return next;
    });
  }

  protected isColumnVisible(col: string): boolean { return this._visibleCols().has(col); }

  private doDelete(item: ReferralType): void {
    this.loading.set(true);
    this.referralTypeService.deleteReferralType(item.id).subscribe({
      next: () => {
        this.toast.success('Deleted successfully');
        this.load();
      },
      error: () => {
        this.toast.error('Failed to delete');
        this.loading.set(false);
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.referralTypeService.getReferralTypes().subscribe({
      next: (data) => {
        this.dataSource.data = data;
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load');
        this.loading.set(false);
      },
    });
  }
}

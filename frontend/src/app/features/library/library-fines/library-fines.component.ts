import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe, DecimalPipe } from '@angular/common';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { LibraryService } from '../library.service';
import {
  LibraryFineDetail,
  FineStatus,
  LibraryMemberType,
  FINE_STATUS_OPTIONS,
} from '../library.model';
import { ToastService } from '../../../core/toast/toast.service';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsTypeBadgeComponent } from '../../../shared/type-badge/type-badge.component';

@Component({
  selector: 'app-library-fines',
  standalone: true,
  imports: [
    FormsModule,
    DatePipe,
    DecimalPipe,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    CmsRowActionButtonComponent,
    CmsTypeBadgeComponent,
    CmsEmptyStateComponent,
  ],
  templateUrl: './library-fines.component.html',
  styleUrl: './library-fines.component.scss',
})
export class LibraryFinesComponent implements OnInit {
  private readonly libraryService = inject(LibraryService);
  private readonly toast          = inject(ToastService);
  private readonly dialog         = inject(MatDialog);

  @ViewChild(MatPaginator) set paginator(v: MatPaginator) { if (v) this.dataSource.paginator = v; }
  @ViewChild(MatSort)      set sort(v: MatSort)           { if (v) this.dataSource.sort = v; }

  protected readonly displayedColumns = [
    'accessionNumber', 'bookTitle', 'memberName', 'overdueDays',
    'totalFine', 'status', 'resolvedBy', 'actions',
  ];

  protected readonly dataSource    = new MatTableDataSource<LibraryFineDetail>([]);
  protected readonly loading       = signal(false);
  protected readonly searchValue   = signal('');
  protected readonly statusFilter  = signal<FineStatus | null>(null);
  protected readonly memberFilter  = signal<LibraryMemberType | null>(null);

  private readonly allFines        = signal<LibraryFineDetail[]>([]);
  protected readonly statusOptions = FINE_STATUS_OPTIONS;

  protected readonly filteredFines = computed(() => {
    const q      = this.searchValue().toLowerCase().trim();
    const status = this.statusFilter();
    const member = this.memberFilter();
    return this.allFines().filter(f => {
      if (status && f.status     !== status)  return false;
      if (member && f.memberType !== member)  return false;
      if (q && !(
        f.accessionNumber.toLowerCase().includes(q) ||
        f.bookTitle.toLowerCase().includes(q)       ||
        f.memberName.toLowerCase().includes(q)      ||
        (f.memberCode ?? '').toLowerCase().includes(q)
      )) return false;
      return true;
    });
  });

  protected readonly totalPending   = computed(() => this.allFines().filter(f => f.status === 'PENDING').length);
  protected readonly totalAmount    = computed(() =>
    this.allFines().filter(f => f.status === 'PENDING').reduce((s, f) => s + f.totalFine, 0));
  protected readonly totalCollected = computed(() =>
    this.allFines().filter(f => f.status === 'COLLECTED').reduce((s, f) => s + f.totalFine, 0));

  protected readonly hasActiveFilters = computed(() =>
    this.statusFilter() !== null || this.memberFilter() !== null || this.searchValue().length > 0);

  ngOnInit(): void {
    this.dataSource.sortingDataAccessor = (item, prop) =>
      String((item as unknown as Record<string, unknown>)[prop] ?? '').toLowerCase();
    this.loadFines();
  }

  protected applyFilter(event: Event): void {
    this.searchValue.set((event.target as HTMLInputElement).value);
    this.syncTable();
  }

  protected clearFilters(): void {
    this.searchValue.set('');
    this.statusFilter.set(null);
    this.memberFilter.set(null);
    this.syncTable();
  }

  protected statusLabel(status: FineStatus): string {
    return FINE_STATUS_OPTIONS.find(o => o.value === status)?.label ?? status;
  }

  protected statusClass(status: FineStatus): string {
    return FINE_STATUS_OPTIONS.find(o => o.value === status)?.colorClass ?? '';
  }

  protected resolvedBy(fine: LibraryFineDetail): string {
    if (fine.status === 'WAIVED')    return fine.waivedBy    ? `Waived by ${fine.waivedBy}` : 'Waived';
    if (fine.status === 'COLLECTED') return fine.collectedAt ? `Collected ${new DatePipe('en-IN').transform(fine.collectedAt, 'dd MMM yyyy')}` : 'Collected';
    return '—';
  }

  protected confirmWaive(fine: LibraryFineDetail): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Waive Fine',
        message: `Waive ₹${fine.totalFine} fine for "${fine.bookTitle}"?\nMember: ${fine.memberName}${fine.memberCode ? ' (' + fine.memberCode + ')' : ''}`,
        confirmText: 'Waive Fine',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe(confirmed => {
      if (confirmed) this.performWaive(fine);
    });
  }

  protected confirmCollect(fine: LibraryFineDetail): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Collect Fine',
        message: `Mark ₹${fine.totalFine} fine as collected from ${fine.memberName}?\nBook: "${fine.bookTitle}" (${fine.accessionNumber})`,
        confirmText: 'Mark Collected',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe(confirmed => {
      if (confirmed) this.performCollect(fine);
    });
  }

  protected syncTable(): void {
    this.dataSource.data = this.filteredFines();
    this.dataSource.paginator?.firstPage();
  }

  private performWaive(fine: LibraryFineDetail): void {
    this.libraryService.waiveFine(fine.id).subscribe({
      next: updated => {
        this.toast.success(`Fine of ₹${updated.totalFine} waived for ${updated.memberName}.`);
        this.loadFines();
      },
      error: err => this.toast.error(err?.error?.message ?? 'Failed to waive fine'),
    });
  }

  private performCollect(fine: LibraryFineDetail): void {
    this.libraryService.collectFine(fine.id).subscribe({
      next: updated => {
        this.toast.success(`₹${updated.totalFine} collected from ${updated.memberName}.`);
        this.loadFines();
      },
      error: err => this.toast.error(err?.error?.message ?? 'Failed to collect fine'),
    });
  }

  private loadFines(): void {
    this.loading.set(true);
    this.libraryService.getFines().subscribe({
      next: fines => {
        this.allFines.set(fines);
        this.syncTable();
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load fines');
        this.loading.set(false);
      },
    });
  }
}

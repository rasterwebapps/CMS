import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { EnquiryService } from '../enquiry.service';
import { Enquiry } from '../enquiry.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { AuthService } from '../../../core/auth/auth.service';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { ToastService } from '../../../core/toast/toast.service';

@Component({
  selector: 'app-enquiry-list',
  standalone: true,
  imports: [
    PageHeaderComponent,
    RouterLink, FormsModule, MatTableModule, MatPaginatorModule, MatSortModule,
    MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, MatDialogModule, MatTooltipModule,
    MatMenuModule],
  templateUrl: './enquiry-list.component.html',
  styleUrl: './enquiry-list.component.scss',
})
export class EnquiryListComponent implements OnInit {
  private readonly enquiryService = inject(EnquiryService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);

  private _sort?: MatSort;
  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    this._sort = value;
    if (value) {
      this.dataSource.sort = value;
      if (!value.active) {
        value.active = 'enquiryDate';
        value.direction = 'asc';
        value.sortChange.emit({ active: 'enquiryDate', direction: 'asc' });
      }
    }
  }

  protected readonly dataSource = new MatTableDataSource<Enquiry>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly statusFilter = signal('');

  // ── Column visibility ────────────────────────────────────────────────────
  protected readonly ALL_COLS = [
    'name', 'phone', 'programName', 'studentType',
    'enquiryDate', 'referralTypeName', 'status', 'agentName', 'actions'];
  protected readonly COLUMN_LABELS: Record<string, string> = {
    name: 'Name', phone: 'Phone', programName: 'Program', studentType: 'Type',
    enquiryDate: 'Date', referralTypeName: 'Referral', status: 'Status',
    agentName: 'Agent', actions: 'Actions',
  };
  private readonly COLS_KEY = 'enquiry-list-cols';
  private readonly _visibleCols = signal<Set<string>>(this._loadColPrefs());
  protected readonly displayedColumns = computed(() => this.ALL_COLS.filter(c => this._visibleCols().has(c)));

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

  // ── Search debounce ──────────────────────────────────────────────────────
  private _searchTimer: ReturnType<typeof setTimeout> | null = null;
  protected readonly statuses = ['ENQUIRED', 'INTERESTED', 'NOT_INTERESTED', 'FEES_FINALIZED', 'FEES_PAID', 'PARTIALLY_PAID', 'DOCUMENTS_SUBMITTED', 'ADMITTED', 'CLOSED'];

  /** Date range filter — defaults to current month (YYYY-MM-DD strings for native date inputs) */
  protected dateFrom: string;
  protected dateTo: string;

  constructor() {
    const now = new Date();
    this.dateFrom = this.toDateString(new Date(now.getFullYear(), now.getMonth(), 1));
    this.dateTo = this.toDateString(new Date(now.getFullYear(), now.getMonth() + 1, 0));
  }

  ngOnInit(): void {
    this.load();
  }

  protected applyFilter(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchValue.set(value);
    if (this._searchTimer) clearTimeout(this._searchTimer);
    this._searchTimer = setTimeout(() => {
      this.dataSource.filter = value.trim().toLowerCase();
      if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
    }, 300);
  }

  protected clearFilter(): void {
    if (this._searchTimer) clearTimeout(this._searchTimer);
    this.searchValue.set('');
    this.dataSource.filter = '';
  }

  protected onStatusFilterChange(status: string): void {
    this.statusFilter.set(status);
    this.load();
  }

  protected onDateRangeChange(): void {
    if (this.dateFrom && this.dateTo) {
      this.load();
    }
  }

  protected clearDateFilter(): void {
    const now = new Date();
    this.dateFrom = this.toDateString(new Date(now.getFullYear(), now.getMonth(), 1));
    this.dateTo = this.toDateString(new Date(now.getFullYear(), now.getMonth() + 1, 0));
    this.load();
  }

  protected getStatusColor(status: string): string {
    switch (status) {
      case 'ENQUIRED': return 'primary';
      case 'INTERESTED': return 'accent';
      case 'NOT_INTERESTED': return 'warn';
      case 'FEES_FINALIZED': return 'primary';
      case 'FEES_PAID': return 'primary';
      case 'PARTIALLY_PAID': return 'accent';
      case 'DOCUMENTS_SUBMITTED': return 'primary';
      case 'ADMITTED': return 'primary';
      case 'CLOSED': return '';
      default: return '';
    }
  }

  /** Returns the list of allowed next statuses for the given current status (manual transitions only). */
  protected getNextStatuses(currentStatus: string): string[] {
    switch (currentStatus) {
      case 'ENQUIRED': return ['INTERESTED', 'NOT_INTERESTED'];
      case 'INTERESTED': return [];
      case 'NOT_INTERESTED': return ['INTERESTED'];
      case 'FEES_FINALIZED': return ['NOT_INTERESTED'];
      case 'FEES_PAID': return [];
      case 'PARTIALLY_PAID': return [];
      case 'DOCUMENTS_SUBMITTED': return [];
      case 'ADMITTED': return [];
      case 'CLOSED': return ['ENQUIRED'];
      default: return [];
    }
  }

  protected canChangeStatus(item: Enquiry): boolean {
    return item.status !== 'ADMITTED' && item.status !== 'CONVERTED' && this.getNextStatuses(item.status).length > 0;
  }

  protected onStatusUpdate(item: Enquiry, newStatus: string): void {
    this.enquiryService.updateStatus(item.id, newStatus).subscribe({
      next: (updated) => {
        const data = this.dataSource.data;
        const idx = data.findIndex((e) => e.id === item.id);
        if (idx >= 0) {
          data[idx] = { ...data[idx], status: updated.status };
          this.dataSource.data = [...data];
        }
        this.toast.success(`Status updated to ${updated.status}`);
      },
      error: () => {
        this.toast.error('Failed to update status');
      },
    });
  }

  protected canConvert(item: Enquiry): boolean {
    return item.status === 'DOCUMENTS_SUBMITTED';
  }

  protected convert(item: Enquiry): void {
    void this.router.navigate(['/enquiries', item.id, 'convert']);
  }

   protected canFinalizeFee(item: Enquiry): boolean {
     return item.status === 'INTERESTED' && (this.authService.isAdmin() || this.authService.isCollegeAdmin());
   }

   protected canCollectPayment(item: Enquiry): boolean {
     return (item.status === 'FEES_FINALIZED' || item.status === 'PARTIALLY_PAID') &&
       (this.authService.isAdmin() || this.authService.isCollegeAdmin() || this.authService.isCashier());
   }

   protected canSubmitDocuments(item: Enquiry): boolean {
     return (item.status === 'FEES_PAID' || item.status === 'PARTIALLY_PAID') &&
       (this.authService.isAdmin() || this.authService.isCollegeAdmin() || this.authService.isFrontOffice());
   }

  protected finalizeFee(item: Enquiry): void {
    void this.router.navigate(['/student-fees/finalize'], { queryParams: { enquiryId: item.id } });
  }

  protected collectPayment(item: Enquiry): void {
    void this.router.navigate(['/student-fees/collect-payment'], { queryParams: { enquiryId: item.id } });
  }

  protected submitDocuments(item: Enquiry): void {
    void this.router.navigate(['/enquiries/document-submission', item.id]);
  }

  protected edit(item: Enquiry): void { void this.router.navigate(['/enquiries', item.id, 'edit']); }

  protected view(item: Enquiry): void { void this.router.navigate(['/enquiries', item.id]); }

  protected canDelete(item: Enquiry): boolean {
    return item.status === 'ENQUIRED';
  }

  protected delete(item: Enquiry): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Delete Enquiry', message: `Delete enquiry for "${item.name}"?`, confirmText: 'Delete', cancelText: 'Cancel' },
    }).afterClosed().subscribe((confirmed) => { if (confirmed) this.doDelete(item); });
  }

  private doDelete(item: Enquiry): void {
    this.loading.set(true);
    this.enquiryService.deleteEnquiry(item.id).subscribe({
      next: () => { this.toast.success('Deleted successfully'); this.load(); },
      error: () => { this.toast.error('Failed to delete'); this.loading.set(false); },
    });
  }

  protected exportCsv(): void {
    const rows = this.dataSource.filteredData;
    const headers = ['Name', 'Phone', 'Program', 'Type', 'Date', 'Referral', 'Status', 'Agent'];
    const cells = rows.map(e => [
      e.name, e.phone ?? '', e.programName ?? '', e.studentType ?? '',
      e.enquiryDate, e.referralTypeName ?? '', e.status, e.agentName ?? '']);
    const csv = [headers, ...cells]
      .map(r => r.map(c => `"${String(c).replace(/"/g, '""')}"`).join(','))
      .join('\n');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8;' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = `enquiries-${new Date().toISOString().slice(0, 10)}.csv`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }

  private toDateString(d: Date): string {
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }

  private load(): void {
    this.loading.set(true);
    const fromStr = this.dateFrom;
    const toStr = this.dateTo;
    const status = this.statusFilter();

    this.enquiryService.getEnquiriesByDateRange(fromStr, toStr, status || undefined).subscribe({
      next: (data) => {
        this.dataSource.data = data;
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load'); this.loading.set(false); },
    });
  }
}

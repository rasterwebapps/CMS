import { Component, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { EnquiryService } from '../enquiry.service';
import { Enquiry } from '../enquiry.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { AuthService } from '../../../core/auth/auth.service';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';

@Component({
  selector: 'app-enquiry-list',
  standalone: true,
  imports: [
    PageHeaderComponent,
    RouterLink, FormsModule, MatTableModule, MatPaginatorModule, MatSortModule,
    MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, MatSnackBarModule, MatDialogModule, MatTooltipModule,
    MatMenuModule,
  ],
  templateUrl: './enquiry-list.component.html',
  styleUrl: './enquiry-list.component.scss',
})
export class EnquiryListComponent implements OnInit {
  private readonly enquiryService = inject(EnquiryService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
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

  protected readonly displayedColumns = ['name', 'phone', 'programName', 'studentType', 'enquiryDate', 'referralTypeName', 'status', 'agentName', 'actions'];
  protected readonly dataSource = new MatTableDataSource<Enquiry>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly statusFilter = signal('');
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
    this.dataSource.filter = value.trim().toLowerCase();
    if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
  }

  protected clearFilter(): void { this.searchValue.set(''); this.dataSource.filter = ''; }

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
        this.snackBar.open(`Status updated to ${updated.status}`, 'Close', { duration: 3000 });
      },
      error: () => {
        this.snackBar.open('Failed to update status', 'Close', { duration: 3000 });
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
    return item.status === 'INTERESTED' && this.authService.isAdmin();
  }

  protected canCollectPayment(item: Enquiry): boolean {
    return (item.status === 'FEES_FINALIZED' || item.status === 'PARTIALLY_PAID') &&
      (this.authService.isAdmin() || this.authService.isFrontOffice());
  }

  protected canSubmitDocuments(item: Enquiry): boolean {
    return (item.status === 'FEES_PAID' || item.status === 'PARTIALLY_PAID') &&
      (this.authService.isAdmin() || this.authService.isFrontOffice());
  }

  protected finalizeFee(item: Enquiry): void {
    void this.router.navigate(['/student-fees/finalize'], { queryParams: { enquiryId: item.id } });
  }

  protected collectPayment(item: Enquiry): void {
    void this.router.navigate(['/student-fees/collect-payment'], { queryParams: { enquiryId: item.id } });
  }

  protected submitDocuments(item: Enquiry): void {
    this.enquiryService.submitDocuments(item.id).subscribe({
      next: () => {
        this.snackBar.open('Documents submitted successfully', 'Close', { duration: 3000 });
        this.load();
      },
      error: (err) => {
        if (err.status === 400 && err.error?.missingDocumentTypes?.length > 0) {
          const missing = (err.error.missingDocumentTypes as string[])
            .map((d) => d.replace(/_/g, ' '))
            .join(', ');
          this.snackBar.open(`Missing documents: ${missing}`, 'Close', { duration: 7000 });
        } else {
          this.snackBar.open('Failed to submit documents', 'Close', { duration: 3000 });
        }
      },
    });
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
      next: () => { this.snackBar.open('Deleted successfully', 'Close', { duration: 3000 }); this.load(); },
      error: () => { this.snackBar.open('Failed to delete', 'Close', { duration: 3000 }); this.loading.set(false); },
    });
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
      error: () => { this.snackBar.open('Failed to load', 'Close', { duration: 3000 }); this.loading.set(false); },
    });
  }
}

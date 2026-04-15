import { Component, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatMenuModule } from '@angular/material/menu';
import { EnquiryService } from '../enquiry.service';
import { Enquiry } from '../enquiry.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-enquiry-list',
  standalone: true,
  imports: [
    RouterLink, FormsModule, MatTableModule, MatPaginatorModule, MatSortModule,
    MatInputModule, MatFormFieldModule, MatButtonModule, MatIconModule, MatCardModule,
    MatProgressSpinnerModule, MatSnackBarModule, MatDialogModule, MatTooltipModule,
    MatChipsModule, MatSelectModule, MatDatepickerModule, MatNativeDateModule,
    MatMenuModule,
  ],
  templateUrl: './enquiry-list.component.html',
  styleUrl: './enquiry-list.component.scss',
})
export class EnquiryListComponent implements OnInit {
  private readonly enquiryService = inject(EnquiryService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  protected readonly displayedColumns = ['name', 'phone', 'programName', 'enquiryDate', 'source', 'status', 'agentName', 'actions'];
  protected readonly dataSource = new MatTableDataSource<Enquiry>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly statusFilter = signal('');
  protected readonly statuses = ['NEW', 'CONTACTED', 'FEE_DISCUSSED', 'INTERESTED', 'CONVERTED', 'NOT_INTERESTED', 'CLOSED'];

  /** Date range filter — defaults to current month */
  protected dateFrom: Date;
  protected dateTo: Date;

  constructor() {
    const now = new Date();
    this.dateFrom = new Date(now.getFullYear(), now.getMonth(), 1);
    this.dateTo = new Date(now.getFullYear(), now.getMonth() + 1, 0);
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
    this.dateFrom = new Date(now.getFullYear(), now.getMonth(), 1);
    this.dateTo = new Date(now.getFullYear(), now.getMonth() + 1, 0);
    this.load();
  }

  protected getStatusColor(status: string): string {
    switch (status) {
      case 'NEW': return 'primary';
      case 'CONTACTED': return 'accent';
      case 'FEE_DISCUSSED': return 'accent';
      case 'INTERESTED': return 'primary';
      case 'CONVERTED': return 'primary';
      case 'NOT_INTERESTED': return 'warn';
      case 'CLOSED': return '';
      default: return '';
    }
  }

  /** Returns the list of allowed next statuses for the given current status. */
  protected getNextStatuses(currentStatus: string): string[] {
    switch (currentStatus) {
      case 'NEW': return ['CONTACTED', 'NOT_INTERESTED', 'CLOSED'];
      case 'CONTACTED': return ['FEE_DISCUSSED', 'INTERESTED', 'NOT_INTERESTED', 'CLOSED'];
      case 'FEE_DISCUSSED': return ['INTERESTED', 'NOT_INTERESTED', 'CLOSED'];
      case 'INTERESTED': return ['FEE_DISCUSSED', 'NOT_INTERESTED', 'CLOSED'];
      case 'NOT_INTERESTED': return ['CONTACTED', 'CLOSED'];
      case 'CLOSED': return ['NEW'];
      default: return [];
    }
  }

  protected canChangeStatus(item: Enquiry): boolean {
    return item.status !== 'CONVERTED' && this.getNextStatuses(item.status).length > 0;
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
    return item.status === 'INTERESTED' || item.status === 'FEE_DISCUSSED';
  }

  protected convert(item: Enquiry): void {
    void this.router.navigate(['/students/new'], { queryParams: { fromEnquiry: item.id } });
  }

  protected edit(item: Enquiry): void { void this.router.navigate(['/enquiries', item.id, 'edit']); }

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

  private formatDate(d: Date): string {
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }

  private load(): void {
    this.loading.set(true);
    const fromStr = this.formatDate(this.dateFrom);
    const toStr = this.formatDate(this.dateTo);
    const status = this.statusFilter();

    this.enquiryService.getEnquiriesByDateRange(fromStr, toStr, status || undefined).subscribe({
      next: (data) => {
        this.dataSource.data = data;
        setTimeout(() => {
          this.dataSource.paginator = this.paginator;
          this.dataSource.sort = this.sort;
          if (this.sort && !this.sort.active) {
            this.sort.active = 'enquiryDate';
            this.sort.direction = 'asc';
            this.sort.sortChange.emit({ active: 'enquiryDate', direction: 'asc' });
          }
        });
        this.loading.set(false);
      },
      error: () => { this.snackBar.open('Failed to load', 'Close', { duration: 3000 }); this.loading.set(false); },
    });
  }
}

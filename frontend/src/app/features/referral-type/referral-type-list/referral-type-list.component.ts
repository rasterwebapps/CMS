import { Component, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CurrencyPipe } from '@angular/common';
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
import { ReferralTypeService } from '../referral-type.service';
import { ReferralType } from '../referral-type.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';

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
    MatInputModule,
    MatFormFieldModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDialogModule,
    MatTooltipModule,
    MatChipsModule,
  ],
  templateUrl: './referral-type-list.component.html',
  styleUrl: './referral-type-list.component.scss',
})
export class ReferralTypeListComponent implements OnInit {
  private readonly referralTypeService = inject(ReferralTypeService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  protected readonly displayedColumns = [
    'name',
    'code',
    'hasCommission',
    'commissionAmount',
    'isActive',
    'actions',
  ];
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

  private doDelete(item: ReferralType): void {
    this.loading.set(true);
    this.referralTypeService.deleteReferralType(item.id).subscribe({
      next: () => {
        this.snackBar.open('Deleted successfully', 'Close', { duration: 3000 });
        this.load();
      },
      error: () => {
        this.snackBar.open('Failed to delete', 'Close', { duration: 3000 });
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
        this.snackBar.open('Failed to load', 'Close', { duration: 3000 });
        this.loading.set(false);
      },
    });
  }
}

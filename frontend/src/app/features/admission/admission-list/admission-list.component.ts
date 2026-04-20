import { Component, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AdmissionService } from '../admission.service';
import { AdmissionResponse, ADMISSION_STATUSES } from '../admission.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';

@Component({
  selector: 'app-admission-list',
  standalone: true,
  imports: [
    PageHeaderComponent,
    RouterLink,
    FormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatSelectModule,
    MatFormFieldModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDialogModule,
    MatTooltipModule,
  ],
  templateUrl: './admission-list.component.html',
  styleUrl: './admission-list.component.scss',
})
export class AdmissionListComponent implements OnInit {
  private readonly admissionService = inject(AdmissionService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  @ViewChild(MatPaginator) paginator!: MatPaginator;

  protected readonly statuses = ADMISSION_STATUSES;
  protected readonly displayedColumns = ['studentName', 'applicationDate', 'academicYear', 'status', 'actions'];
  protected readonly dataSource = new MatTableDataSource<AdmissionResponse>([]);
  protected readonly loading = signal(false);
  protected selectedStatus = '';

  ngOnInit(): void {
    this.load();
  }

  protected onStatusChange(): void {
    this.applyStatusFilter();
  }

  private load(): void {
    this.loading.set(true);
    this.admissionService.getAll().subscribe({
      next: (data) => {
        this.dataSource.data = data;
        setTimeout(() => { this.dataSource.paginator = this.paginator; });
        this.applyStatusFilter();
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Failed to load admissions', 'Close', { duration: 3000 });
        this.loading.set(false);
      },
    });
  }

  private applyStatusFilter(): void {
    this.dataSource.filterPredicate = (row) =>
      !this.selectedStatus || row.status === this.selectedStatus;
    this.dataSource.filter = this.selectedStatus || ' ';
    if (!this.selectedStatus) this.dataSource.filter = '';
  }

  protected view(item: AdmissionResponse): void {
    void this.router.navigate(['/admissions', item.id]);
  }

  protected edit(item: AdmissionResponse): void {
    void this.router.navigate(['/admissions', item.id, 'edit']);
  }

  protected delete(item: AdmissionResponse): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Delete Admission', message: `Delete admission for "${item.studentName}"?`, confirmText: 'Delete', cancelText: 'Cancel' },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.admissionService.delete(item.id).subscribe({
          next: () => { this.snackBar.open('Deleted', 'Close', { duration: 3000 }); this.load(); },
          error: () => this.snackBar.open('Failed to delete', 'Close', { duration: 3000 }),
        });
      }
    });
  }
}

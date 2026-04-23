import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AdmissionService } from '../admission.service';
import { AdmissionResponse, ADMISSION_STATUSES } from '../admission.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { ToastService } from '../../../core/toast/toast.service';

@Component({
  selector: 'app-admission-list',
  standalone: true,
  imports: [
    PageHeaderComponent,
    CmsStatusBadgeComponent,
    RouterLink,
    FormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatTooltipModule],
  templateUrl: './admission-list.component.html',
  styleUrl: './admission-list.component.scss',
})
export class AdmissionListComponent implements OnInit {
  private readonly admissionService = inject(AdmissionService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }

  protected readonly statuses = ADMISSION_STATUSES;
  // ── Column visibility ────────────────────────────────────────────────────
  protected readonly ALL_COLS = ['studentName', 'applicationDate', 'academicYear', 'status', 'actions'];
  protected readonly COLUMN_LABELS: Record<string, string> = {
    studentName: 'Student',
    applicationDate: 'Application Date',
    academicYear: 'Academic Year',
    status: 'Status',
    actions: 'Actions',
  };
  private readonly COLS_KEY = 'admission-list-cols';
  private readonly _visibleCols = signal<Set<string>>(this._loadColPrefs());
  protected readonly displayedColumns = computed(() => this.ALL_COLS.filter(c => this._visibleCols().has(c)));
  protected readonly dataSource = new MatTableDataSource<AdmissionResponse>([]);
  protected readonly loading = signal(false);
  protected selectedStatus = '';

  ngOnInit(): void {
    this.load();
  }

  protected onStatusChange(): void {
    this.applyStatusFilter();
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

  private load(): void {
    this.loading.set(true);
    this.admissionService.getAll().subscribe({
      next: (data) => {
        this.dataSource.data = data;
        this.applyStatusFilter();
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load admissions');
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
          next: () => { this.toast.success('Deleted'); this.load(); },
          error: () => this.toast.error('Failed to delete'),
        });
      }
    });
  }
}

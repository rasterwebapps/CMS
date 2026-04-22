import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
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
import { ExaminationService } from '../examination.service';
import { Examination } from '../examination.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';

@Component({
  selector: 'app-examination-list',
  standalone: true,
  imports: [
    PageHeaderComponent,
    RouterLink, FormsModule, MatTableModule, MatPaginatorModule, MatSortModule,
    MatProgressSpinnerModule, MatSnackBarModule, MatDialogModule, MatTooltipModule,
  ],
  templateUrl: './examination-list.component.html',
  styleUrl: './examination-list.component.scss',
})
export class ExaminationListComponent implements OnInit {
  private readonly examinationService = inject(ExaminationService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  // ── Column visibility ────────────────────────────────────────────────────
  protected readonly ALL_COLS = ['name', 'courseName', 'examType', 'date', 'duration', 'maxMarks', 'actions'];
  protected readonly COLUMN_LABELS: Record<string, string> = {
    name: 'Name',
    courseName: 'Course',
    examType: 'Type',
    date: 'Date',
    duration: 'Duration',
    maxMarks: 'Max Marks',
    actions: 'Actions',
  };
  private readonly COLS_KEY = 'examination-list-cols';
  private readonly _visibleCols = signal<Set<string>>(this._loadColPrefs());
  protected readonly displayedColumns = computed(() => this.ALL_COLS.filter(c => this._visibleCols().has(c)));
  protected readonly dataSource = new MatTableDataSource<Examination>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');

  ngOnInit(): void { this.load(); }

  protected applyFilter(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchValue.set(value);
    this.dataSource.filter = value.trim().toLowerCase();
    if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
  }

  protected clearFilter(): void { this.searchValue.set(''); this.dataSource.filter = ''; }

  protected edit(item: Examination): void { void this.router.navigate(['/examinations', item.id, 'edit']); }

  protected delete(item: Examination): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Delete Examination', message: `Delete "${item.name}"?`, confirmText: 'Delete', cancelText: 'Cancel' },
    }).afterClosed().subscribe((confirmed) => { if (confirmed) this.doDelete(item); });
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

  private doDelete(item: Examination): void {
    this.loading.set(true);
    this.examinationService.delete(item.id).subscribe({
      next: () => { this.snackBar.open('Deleted successfully', 'Close', { duration: 3000 }); this.load(); },
      error: () => { this.snackBar.open('Failed to delete', 'Close', { duration: 3000 }); this.loading.set(false); },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.examinationService.getAll().subscribe({
      next: (data) => {
        this.dataSource.data = data;
        this.loading.set(false);
      },
      error: () => { this.snackBar.open('Failed to load', 'Close', { duration: 3000 }); this.loading.set(false); },
    });
  }
}

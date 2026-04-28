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
import { CurriculumService } from '../curriculum.service';
import { Syllabus } from '../curriculum.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { SYLLABUS_LIST_TOUR } from '../../../shared/tour/tours/syllabus.tours';

@Component({
  selector: 'app-syllabus-list',
  standalone: true,
  imports: [
    PageHeaderComponent,
    RouterLink,
    FormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatTooltipModule,
    CmsTourButtonComponent],
  templateUrl: './syllabus-list.component.html',
  styleUrl: './syllabus-list.component.scss',
})
export class SyllabusListComponent implements OnInit {
  private readonly curriculumService = inject(CurriculumService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly tourService = inject(TourService);

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  // ── Column visibility ────────────────────────────────────────────────────
  protected readonly ALL_COLS = ['courseName', 'courseCode', 'version', 'theoryHours', 'labHours', 'isActive', 'actions'];
  protected readonly COLUMN_LABELS: Record<string, string> = {
    courseName: 'Course',
    courseCode: 'Course Code',
    version: 'Version',
    theoryHours: 'Theory Hrs',
    labHours: 'Lab Hrs',
    isActive: 'Active',
    actions: 'Actions',
  };
  private readonly COLS_KEY = 'syllabus-list-cols';
  private readonly _visibleCols = signal<Set<string>>(this._loadColPrefs());
  protected readonly displayedColumns = computed(() => this.ALL_COLS.filter(c => this._visibleCols().has(c)));
  protected readonly dataSource = new MatTableDataSource<Syllabus>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');

  ngOnInit(): void {
    this.tourService.register('syllabus-list', SYLLABUS_LIST_TOUR);
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

  protected edit(item: Syllabus): void {
    void this.router.navigate(['/syllabi', item.id, 'edit']);
  }

  protected delete(item: Syllabus): void {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: {
          title: 'Delete Syllabus',
          message: `Delete syllabus for "${item.courseName}" v${item.version}?`,
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

  private doDelete(item: Syllabus): void {
    this.loading.set(true);
    this.curriculumService.deleteSyllabus(item.id).subscribe({
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
    this.curriculumService.getAllSyllabi().subscribe({
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

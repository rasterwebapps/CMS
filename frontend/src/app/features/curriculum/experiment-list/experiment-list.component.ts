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
import { CurriculumService } from '../curriculum.service';
import { Experiment } from '../curriculum.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';

@Component({
  selector: 'app-experiment-list',
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
    MatSnackBarModule,
    MatDialogModule,
    MatTooltipModule,
  ],
  templateUrl: './experiment-list.component.html',
  styleUrl: './experiment-list.component.scss',
})
export class ExperimentListComponent implements OnInit {
  private readonly curriculumService = inject(CurriculumService);
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
    'experimentNumber',
    'name',
    'courseName',
    'courseCode',
    'estimatedDurationMinutes',
    'isActive',
    'actions',
  ];
  protected readonly dataSource = new MatTableDataSource<Experiment>([]);
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

  protected edit(item: Experiment): void {
    void this.router.navigate(['/experiments', item.id, 'edit']);
  }

  protected delete(item: Experiment): void {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: {
          title: 'Delete Experiment',
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

  private doDelete(item: Experiment): void {
    this.loading.set(true);
    this.curriculumService.deleteExperiment(item.id).subscribe({
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
    this.curriculumService.getAllExperiments().subscribe({
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

import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ProgramService } from '../program.service';
import { Program } from '../program.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { ToastService } from '../../../core/toast/toast.service';

@Component({
  selector: 'app-program-list',
  standalone: true,
  imports: [
    RouterLink,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatDialogModule,
    MatTooltipModule,
    CmsEmptyStateComponent,
  ],
  templateUrl: './program-list.component.html',
  styleUrl: './program-list.component.scss',
})
export class ProgramListComponent implements OnInit {
  private readonly programService = inject(ProgramService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  protected readonly displayedColumns = ['code', 'name', 'durationYears', 'actions'];
  protected readonly dataSource = new MatTableDataSource<Program>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');

  private readonly allPrograms = signal<Program[]>([]);

  protected readonly programCount = computed(() => this.allPrograms().length);

  protected readonly filteredPrograms = computed(() => {
    const q = this.searchValue().trim().toLowerCase();
    if (!q) return this.allPrograms();
    return this.allPrograms().filter(
      p => p.name.toLowerCase().includes(q) || p.code.toLowerCase().includes(q),
    );
  });

  private readonly VIEW_MODE_KEY = 'program-view-mode';
  protected readonly viewMode = signal<'card' | 'table'>(this.loadViewMode());

  ngOnInit(): void {
    this.loadPrograms();
  }

  protected setViewMode(mode: 'card' | 'table'): void {
    this.viewMode.set(mode);
    localStorage.setItem(this.VIEW_MODE_KEY, mode);
  }

  protected applyFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value;
    this.searchValue.set(filterValue);
    this.dataSource.filter = filterValue.trim().toLowerCase();
    if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
  }

  protected clearFilter(): void {
    this.searchValue.set('');
    this.dataSource.filter = '';
  }

  protected handleEmptyAction(): void {
    if (this.searchValue()) {
      this.clearFilter();
    } else {
      void this.router.navigate(['/programs/new']);
    }
  }

  protected editProgram(program: Program): void {
    void this.router.navigate(['/programs', program.id, 'edit']);
  }

  protected deleteProgram(program: Program): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Program',
        message: `Are you sure you want to delete "${program.name}"?`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) this.performDelete(program);
    });
  }

  private loadViewMode(): 'card' | 'table' {
    const stored = localStorage.getItem(this.VIEW_MODE_KEY);
    return stored === 'table' ? 'table' : 'card';
  }

  private performDelete(program: Program): void {
    this.loading.set(true);
    this.programService.delete(program.id).subscribe({
      next: () => {
        this.toast.success('Program deleted successfully');
        this.loadPrograms();
      },
      error: () => {
        this.toast.error('Failed to delete program');
        this.loading.set(false);
      },
    });
  }

  private loadPrograms(): void {
    this.loading.set(true);
    this.programService.getAll().subscribe({
      next: (programs) => {
        this.allPrograms.set(programs);
        this.dataSource.data = programs;
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load programs');
        this.loading.set(false);
      },
    });
  }
}


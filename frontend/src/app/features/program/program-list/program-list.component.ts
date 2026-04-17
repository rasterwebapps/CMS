import { Component, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ProgramService } from '../program.service';
import { Program } from '../program.model';
import { DepartmentService } from '../../department/department.service';
import { Department } from '../../department/department.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-program-list',
  standalone: true,
  imports: [
    RouterLink,
    FormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatInputModule,
    MatFormFieldModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDialogModule,
    MatTooltipModule,
  ],
  templateUrl: './program-list.component.html',
  styleUrl: './program-list.component.scss',
})
export class ProgramListComponent implements OnInit {
  private readonly programService = inject(ProgramService);
  private readonly departmentService = inject(DepartmentService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  protected readonly displayedColumns = [
    'code',
    'name',
    'durationYears',
    'department',
    'actions',
  ];
  protected readonly dataSource = new MatTableDataSource<Program>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly selectedDepartmentId = signal<number | null>(null);
  protected readonly departments = signal<Department[]>([]);

  ngOnInit(): void {
    this.loadDepartments();
    this.loadPrograms();
  }

  protected applyFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value;
    this.searchValue.set(filterValue);
    this.applyFilters();
  }

  protected clearFilter(): void {
    this.searchValue.set('');
    this.applyFilters();
  }

  protected onDepartmentFilterChange(departmentId: number | null): void {
    this.selectedDepartmentId.set(departmentId);
    this.loadPrograms();
  }

  protected editProgram(program: Program): void {
    void this.router.navigate(['/programs', program.id, 'edit']);
  }

  protected deleteProgram(program: Program): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Program',
        message: `Are you sure you want to delete "${program.name}"?`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.performDelete(program);
      }
    });
  }

  protected getDepartmentNames(program: Program): string {
    if (!program.departments || program.departments.length === 0) {
      return '—';
    }
    return program.departments.map((d) => d.name).join(', ');
  }

  private performDelete(program: Program): void {
    this.loading.set(true);
    this.programService.delete(program.id).subscribe({
      next: () => {
        this.snackBar.open('Program deleted successfully', 'Close', {
          duration: 3000,
        });
        this.loadPrograms();
      },
      error: () => {
        this.snackBar.open('Failed to delete program', 'Close', {
          duration: 3000,
        });
        this.loading.set(false);
      },
    });
  }

  private loadDepartments(): void {
    this.departmentService.getAll().subscribe({
      next: (departments) => {
        this.departments.set(departments);
      },
      error: () => {
        this.snackBar.open('Failed to load departments', 'Close', {
          duration: 3000,
        });
      },
    });
  }

  private loadPrograms(): void {
    this.loading.set(true);
    const departmentId = this.selectedDepartmentId();

    const programs$ = departmentId
      ? this.programService.getByDepartment(departmentId)
      : this.programService.getAll();

    programs$.subscribe({
      next: (programs) => {
        this.dataSource.data = programs;
        this.dataSource.paginator = this.paginator;
        this.dataSource.sort = this.sort;
        this.applyFilters();
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Failed to load programs', 'Close', {
          duration: 3000,
        });
        this.loading.set(false);
      },
    });
  }

  private applyFilters(): void {
    this.dataSource.filter = this.searchValue().trim().toLowerCase();

    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }
}

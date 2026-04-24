import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CourseService } from '../course.service';
import { Course } from '../course.model';
import { ProgramService } from '../../program/program.service';
import { Program } from '../../program/program.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { ToastService } from '../../../core/toast/toast.service';

@Component({
  selector: 'app-course-list',
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
  templateUrl: './course-list.component.html',
  styleUrl: './course-list.component.scss',
})
export class CourseListComponent implements OnInit {
  private readonly courseService = inject(CourseService);
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

  private readonly VIEW_MODE_KEY = 'course-view-mode';

  protected readonly displayedColumns = ['code', 'name', 'specialization', 'program', 'actions'];
  protected readonly dataSource = new MatTableDataSource<Course>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly selectedProgramId = signal<number | null>(null);
  protected readonly programs = signal<Program[]>([]);
  protected readonly viewMode = signal<'card' | 'table'>(this.loadViewMode());

  private readonly allCourses = signal<Course[]>([]);

  protected readonly totalCount = computed(() => this.allCourses().length);
  protected readonly uniqueSpecCount = computed(() =>
    new Set(this.allCourses().map(c => c.specialization).filter(Boolean)).size,
  );

  protected readonly filteredCourses = computed(() => {
    const q = this.searchValue().trim().toLowerCase();
    const pid = this.selectedProgramId();
    return this.allCourses().filter(c => {
      const matchesProg = pid == null || c.program?.id === pid;
      if (!matchesProg) return false;
      if (!q) return true;
      return (
        c.name.toLowerCase().includes(q) ||
        c.code.toLowerCase().includes(q) ||
        (c.specialization?.toLowerCase().includes(q) ?? false)
      );
    });
  });

  ngOnInit(): void {
    this.loadPrograms();
    this.loadCourses();
  }

  protected setViewMode(mode: 'card' | 'table'): void {
    this.viewMode.set(mode);
    localStorage.setItem(this.VIEW_MODE_KEY, mode);
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

  protected onProgramFilterChange(programIdStr: string): void {
    this.selectedProgramId.set(programIdStr ? +programIdStr : null);
  }

  protected editCourse(course: Course): void {
    void this.router.navigate(['/courses', course.id, 'edit']);
  }

  protected deleteCourse(course: Course): void {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: {
          title: 'Delete Course',
          message: `Are you sure you want to delete "${course.name}"?`,
          confirmText: 'Delete',
          cancelText: 'Cancel',
        },
      })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) this.performDelete(course);
      });
  }

  protected handleEmptyAction(): void {
    if (this.searchValue()) {
      this.clearFilter();
    } else {
      void this.router.navigate(['/courses/new']);
    }
  }

  private loadViewMode(): 'card' | 'table' {
    const stored = localStorage.getItem(this.VIEW_MODE_KEY);
    return stored === 'table' ? 'table' : 'card';
  }

  private performDelete(course: Course): void {
    this.loading.set(true);
    this.courseService.delete(course.id).subscribe({
      next: () => {
        this.toast.success('Course deleted successfully');
        this.loadCourses();
      },
      error: () => {
        this.toast.error('Failed to delete course');
        this.loading.set(false);
      },
    });
  }

  private loadPrograms(): void {
    this.programService.getAll().subscribe({
      next: (programs) => this.programs.set(programs),
      error: () => this.toast.error('Failed to load programs'),
    });
  }

  private loadCourses(): void {
    this.loading.set(true);
    this.courseService.getAll().subscribe({
      next: (courses) => {
        this.allCourses.set(courses);
        this.dataSource.data = courses;
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load courses');
        this.loading.set(false);
      },
    });
  }
}

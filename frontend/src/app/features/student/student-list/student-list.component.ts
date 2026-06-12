import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { LowerCasePipe, TitleCasePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { StudentService } from '../student.service';
import { Student } from '../student.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { STUDENT_LIST_TOUR } from '../../../shared/tour/tours/student.tours';
import { ToastService } from '../../../core/toast/toast.service';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { computeInitials } from '../../../shared/utils/initials';

@Component({
  selector: 'app-student-list',
  standalone: true,
  imports: [
    CmsEmptyStateComponent,
    CmsTourButtonComponent,
    AppDatePipe,
    TitleCasePipe,
    LowerCasePipe,
    RouterLink,
    FormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatDialogModule,
    MatTooltipModule,
  ],
  templateUrl: './student-list.component.html',
  styleUrl: './student-list.component.scss',
})
export class StudentListComponent implements OnInit {
  private readonly studentService = inject(StudentService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly tourService = inject(TourService);

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) {
      this.dataSource.sort = value;
      value.sort({ id: 'admissionNumber', start: 'asc', disableClear: false });
    }
  }

  protected colMenuOpen       = false;
  protected moreFiltersOpen   = false;

  protected get moreFiltersCount(): number {
    return (this.filterSemester()     !== 'ALL' ? 1 : 0)
         + (this.filterSpeciality()   !== 'ALL' ? 1 : 0)
         + (this.filterAcademicYear() !== 'ALL' ? 1 : 0)
         + (this.filterFeeStatus()    !== 'ALL' ? 1 : 0);
  }
  protected readonly computeInitials = computeInitials;

  // ── Filters ──────────────────────────────────────────────────────────────
  protected filterProgram     = signal<string>('ALL');
  protected filterStatus      = signal<string>('ALL');
  protected filterSemester    = signal<string>('ALL');
  protected filterSpeciality  = signal<string>('ALL');
  protected filterAcademicYear = signal<string>('ALL');
  protected filterFeeStatus   = signal<string>('ALL');
  private readonly allStudents = signal<Student[]>([]);
  protected readonly programs = computed(() =>
    [...new Set(this.allStudents().map(s => s.programName).filter(Boolean))].sort() as string[]
  );
  protected readonly semesters = computed(() =>
    [...new Set(this.allStudents().map(s => String(s.yearOfStudy)).filter(Boolean))].sort((a, b) => +a - +b)
  );
  protected readonly specialities = computed(() =>
    [...new Set(this.allStudents().map(s => s.specialityName).filter(Boolean))].sort() as string[]
  );
  protected readonly academicYears = computed(() =>
    [...new Set(this.allStudents().map(s => s.admissionAcademicYearName).filter(Boolean))].sort() as string[]
  );
  protected readonly FEE_STATUSES = [
    { value: 'PAID',         label: 'Paid' },
    { value: 'PARTIAL',      label: 'Partially Paid' },
    { value: 'UNPAID',       label: 'Unpaid' },
    { value: 'NOT_ASSIGNED', label: 'Not Assigned' },
  ];
  protected readonly STUDENT_STATUSES = ['ACTIVE', 'INACTIVE', 'GRADUATED', 'DROPPED'];
  protected readonly hasActiveFilters = computed(() =>
    this.searchValue()        !== '' ||
    this.filterProgram()      !== 'ALL' ||
    this.filterStatus()       !== 'ALL' ||
    this.filterSemester()     !== 'ALL' ||
    this.filterSpeciality()   !== 'ALL' ||
    this.filterAcademicYear() !== 'ALL' ||
    this.filterFeeStatus()    !== 'ALL'
  );

  // ── Stats ─────────────────────────────────────────────────────────────────
  protected readonly totalCount  = computed(() => this.allStudents().length);
  protected readonly activeCount = computed(() => this.allStudents().filter(s => s.status === 'ACTIVE').length);

  // ── Column visibility ────────────────────────────────────────────────────
  protected readonly ALL_COLS = ['admissionNumber', 'rollNumber', 'fullName', 'programName', 'yearOfStudy', 'admissionDate', 'phone', 'email', 'universityRegistrationNumber', 'labBatch', 'status', 'actions'];
  protected readonly COLUMN_LABELS: Record<string, string> = {
    admissionNumber: 'Admission No.',
    rollNumber: 'Roll No.',
    fullName: 'Name',
    programName: 'Program',
    yearOfStudy: 'Year of Study',
    admissionDate: 'Admission Date',
    phone: 'Phone',
    email: 'Email',
    universityRegistrationNumber: 'Univ. Reg. No.',
    labBatch: 'Lab Batch',
    status: 'Status',
    actions: 'Actions',
  };
  private readonly COLS_KEY = 'student-list-cols-v2';
  private readonly DEFAULT_COLS = new Set(['admissionNumber', 'fullName', 'programName', 'yearOfStudy', 'phone', 'status', 'actions']);
  private readonly _visibleCols = signal<Set<string>>(this._loadColPrefs());
  protected readonly displayedColumns = computed(() => this.ALL_COLS.filter(c => this._visibleCols().has(c)));
  protected readonly dataSource = new MatTableDataSource<Student>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');

  ngOnInit(): void {
    this.tourService.register('student-list', STUDENT_LIST_TOUR);
    this.loadStudents();
  }

  protected applyFilter(event: Event): void {
    this.searchValue.set((event.target as HTMLInputElement).value);
    this._applyFilters();
  }

  protected clearFilter(): void {
    this.searchValue.set('');
    this._applyFilters();
  }

  protected onFilterChange(): void { this._applyFilters(); }

  protected clearAllFilters(): void {
    this.searchValue.set('');
    this.filterProgram.set('ALL');
    this.filterStatus.set('ALL');
    this.filterSemester.set('ALL');
    this.filterSpeciality.set('ALL');
    this.filterAcademicYear.set('ALL');
    this.filterFeeStatus.set('ALL');
    this._applyFilters();
  }

  private _applyFilters(): void {
    const term        = this.searchValue().toLowerCase().trim();
    const program     = this.filterProgram();
    const status      = this.filterStatus();
    const semester    = this.filterSemester();
    const speciality  = this.filterSpeciality();
    const academicYear = this.filterAcademicYear();
    const feeStatus   = this.filterFeeStatus();

    this.dataSource.filterPredicate = (s) => {
      if (program      !== 'ALL' && s.programName !== program)                                        return false;
      if (status       !== 'ALL' && s.status !== status)                                              return false;
      if (semester     !== 'ALL' && String(s.yearOfStudy) !== semester)                               return false;
      if (speciality   !== 'ALL' && (s.specialityName ?? '') !== speciality)                          return false;
      if (academicYear !== 'ALL' && (s.admissionAcademicYearName ?? '') !== academicYear)             return false;
      if (feeStatus    !== 'ALL' && (s.feeStatus ?? 'NOT_ASSIGNED') !== feeStatus)                   return false;
      if (!term) return true;
      return (
        s.fullName.toLowerCase().includes(term) ||
        (s.admissionNumber ?? '').toLowerCase().includes(term) ||
        (s.rollNumber ?? '').toLowerCase().includes(term) ||
        (s.phone ?? '').includes(term) ||
        (s.email ?? '').toLowerCase().includes(term)
      );
    };
    const anyFilter = term || program !== 'ALL' || status !== 'ALL' || semester !== 'ALL' ||
                      speciality !== 'ALL' || academicYear !== 'ALL' || feeStatus !== 'ALL';
    this.dataSource.filter = anyFilter
      ? (term || program || status || semester || speciality || academicYear || feeStatus || '_')
      : '';
    if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
  }

  protected viewStudent(student: Student): void {
    void this.router.navigate(['/students', student.id]);
  }

  protected editStudent(student: Student): void {
    void this.router.navigate(['/students', student.id, 'edit']);
  }

  protected deleteStudent(student: Student): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Student',
        message: `Are you sure you want to delete "${student.fullName}"?`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    });
    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) this.performDelete(student);
    });
  }

  private _loadColPrefs(): Set<string> {
    try {
      const s = localStorage.getItem(this.COLS_KEY);
      if (s) return new Set<string>(JSON.parse(s) as string[]);
    } catch { /* empty */ }
    return new Set<string>(this.DEFAULT_COLS);
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

  private performDelete(student: Student): void {
    this.loading.set(true);
    this.studentService.delete(student.id).subscribe({
      next: () => {
        this.toast.success('Student deleted successfully');
        this.loadStudents();
      },
      error: () => {
        this.toast.error('Failed to delete student');
        this.loading.set(false);
      },
    });
  }

  private loadStudents(): void {
    this.loading.set(true);
    this.studentService.getAll().subscribe({
      next: (students) => {
        this.allStudents.set(students);
        this.dataSource.data = students;
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load students');
        this.loading.set(false);
      },
    });
  }
}

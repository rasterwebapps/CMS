import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTooltipModule } from '@angular/material/tooltip';
import { StudentService } from '../student.service';
import { Student } from '../student.model';
import { ProgramService } from '../../program/program.service';
import { CourseService } from '../../course/course.service';
import { Program } from '../../program/program.model';
import { Course } from '../../course/course.model';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { ToastService } from '../../../core/toast/toast.service';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { ROLL_NUMBER_ASSIGNMENT_TOUR } from '../../../shared/tour/tours/student.tours';
import { computeInitials } from '../../../shared/utils/initials';

interface RollAssignment {
  student: Student;
  rollNumber: string;
}

@Component({
  selector: 'app-roll-number-assignment',
  standalone: true,
  imports: [
    CmsEmptyStateComponent,
    CmsTourButtonComponent,
    CmsRowActionButtonComponent,
    AppDatePipe,
    FormsModule,
    MatTableModule,
    MatSortModule,
    MatTooltipModule,
  ],
  templateUrl: './roll-number-assignment.component.html',
  styleUrl: './roll-number-assignment.component.scss',
})
export class RollNumberAssignmentComponent implements OnInit {
  private readonly studentService = inject(StudentService);
  private readonly programService = inject(ProgramService);
  private readonly courseService  = inject(CourseService);
  private readonly toast          = inject(ToastService);
  private readonly tourService    = inject(TourService);

  protected readonly computeInitials = computeInitials;

  protected readonly programs    = signal<Program[]>([]);
  protected readonly courses     = signal<Course[]>([]);
  protected readonly assignments = signal<RollAssignment[]>([]);
  protected readonly loading     = signal(false);
  protected readonly saving      = signal(false);

  protected selectedProgramId: number | null = null;
  protected selectedCourseId:  number | null = null;

  protected readonly totalCount  = computed(() => this.assignments().length);
  protected readonly filledCount = computed(() => this.assignments().filter(a => a.rollNumber.trim()).length);

  protected readonly displayedColumns = ['name', 'programName', 'yearOfStudy', 'admissionDate', 'rollNumber', 'actions'];

  // All rows load in one shot (no backend pagination here), so sort is applied
  // client-side against the in-memory list rather than re-fetched from the server.
  protected sortActive = 'name';
  protected sortDirection: 'asc' | 'desc' = 'asc';

  ngOnInit(): void {
    this.tourService.register('roll-number-assignment', ROLL_NUMBER_ASSIGNMENT_TOUR);
    this.programService.getAll().subscribe({ next: (p) => this.programs.set(p) });
    this.loadCourses();
    this.loadStudents();
  }

  protected onProgramChange(): void {
    this.selectedCourseId = null;
    if (this.selectedProgramId) {
      this.courseService.getByProgram(this.selectedProgramId).subscribe({ next: (c) => this.courses.set(c) });
    } else {
      this.loadCourses();
    }
    this.loadStudents();
  }

  protected onCourseChange(): void { this.loadStudents(); }

  protected clearFilters(): void {
    this.selectedProgramId = null;
    this.selectedCourseId  = null;
    this.loadCourses();
    this.loadStudents();
  }

  protected get hasActiveFilters(): boolean {
    return this.selectedProgramId !== null || this.selectedCourseId !== null;
  }

  private loadCourses(): void {
    this.courseService.getAll().subscribe({ next: (c) => this.courses.set(c) });
  }

  private loadStudents(): void {
    this.loading.set(true);
    this.studentService.getStudentsWithoutRollNumber(
      this.selectedCourseId  ?? undefined,
      this.selectedProgramId ?? undefined,
    ).subscribe({
      next: (students) => {
        this.assignments.set(students.map((s) => ({ student: s, rollNumber: '' })));
        this.applySort();
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load students');
        this.loading.set(false);
      },
    });
  }

  protected onSortChange(sort: Sort): void {
    this.sortActive    = sort.active;
    this.sortDirection = (sort.direction || 'asc') as 'asc' | 'desc';
    this.applySort();
  }

  private applySort(): void {
    const dir = this.sortDirection === 'desc' ? -1 : 1;
    const value = (item: RollAssignment): string | number => {
      switch (this.sortActive) {
        case 'programName':   return item.student.programName ?? '';
        case 'yearOfStudy':   return item.student.yearOfStudy ?? 0;
        case 'admissionDate': return item.student.admissionDate ?? '';
        case 'rollNumber':    return item.rollNumber;
        default:              return item.student.fullName ?? '';
      }
    };
    const sorted = [...this.assignments()].sort((a, b) => {
      const av = value(a), bv = value(b);
      if (typeof av === 'number' && typeof bv === 'number') return (av - bv) * dir;
      return String(av).localeCompare(String(bv)) * dir;
    });
    this.assignments.set(sorted);
  }

  protected assignOne(item: RollAssignment): void {
    if (!item.rollNumber.trim()) {
      this.toast.warning('Enter a roll number first');
      return;
    }
    this.studentService.assignRollNumber(item.student.id, item.rollNumber.trim()).subscribe({
      next:  () => { this.toast.success(`Roll number assigned to ${item.student.fullName}`); this.loadStudents(); },
      error: (err) => this.toast.error(err?.error?.message ?? 'Failed to assign roll number'),
    });
  }

  protected saveAll(): void {
    const valid = this.assignments().filter((a) => a.rollNumber.trim());
    if (!valid.length) { this.toast.warning('No roll numbers to save'); return; }
    this.saving.set(true);
    this.studentService.bulkAssignRollNumbers(
      valid.map((a) => ({ studentId: a.student.id, rollNumber: a.rollNumber.trim() })),
    ).subscribe({
      next:  () => { this.toast.success('Roll numbers saved successfully'); this.loadStudents(); this.saving.set(false); },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to save roll numbers'); this.saving.set(false); },
    });
  }
}

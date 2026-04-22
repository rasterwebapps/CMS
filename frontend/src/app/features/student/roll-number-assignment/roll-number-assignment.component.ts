import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { StudentService } from '../student.service';
import { Student } from '../student.model';
import { ProgramService } from '../../program/program.service';
import { CourseService } from '../../course/course.service';
import { Program } from '../../program/program.model';
import { Course } from '../../course/course.model';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';

interface RollAssignment {
  student: Student;
  rollNumber: string;
}

@Component({
  selector: 'app-roll-number-assignment',
  standalone: true,
  imports: [
    PageHeaderComponent,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
  templateUrl: './roll-number-assignment.component.html',
  styleUrl: './roll-number-assignment.component.scss',
})
export class RollNumberAssignmentComponent implements OnInit {
  private readonly studentService = inject(StudentService);
  private readonly programService = inject(ProgramService);
  private readonly courseService = inject(CourseService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly programs = signal<Program[]>([]);
  protected readonly courses = signal<Course[]>([]);
  protected readonly assignments = signal<RollAssignment[]>([]);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);

  protected selectedProgramId: number | null = null;
  protected selectedCourseId: number | null = null;

  protected readonly displayedColumns = ['name', 'programName', 'admissionDate', 'rollNumber', 'actions'];

  ngOnInit(): void {
    this.programService.getAll().subscribe({ next: (p) => this.programs.set(p) });
    this.courseService.getAll().subscribe({ next: (c) => this.courses.set(c) });
    this.loadStudents();
  }

  protected onProgramChange(): void {
    this.selectedCourseId = null;
    if (this.selectedProgramId) {
      this.courseService.getByProgram(this.selectedProgramId).subscribe({ next: (c) => this.courses.set(c) });
    }
    this.loadStudents();
  }

  protected onCourseChange(): void {
    this.loadStudents();
  }

  private loadStudents(): void {
    this.loading.set(true);
    this.studentService.getStudentsWithoutRollNumber(
      this.selectedCourseId ?? undefined,
      this.selectedProgramId ?? undefined,
    ).subscribe({
      next: (students) => {
        this.assignments.set(students.map((s) => ({ student: s, rollNumber: '' })));
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Failed to load students', 'Close', { duration: 3000 });
        this.loading.set(false);
      },
    });
  }

  protected assignOne(item: RollAssignment): void {
    if (!item.rollNumber.trim()) {
      this.snackBar.open('Enter a roll number first', 'Close', { duration: 2000 });
      return;
    }
    this.studentService.assignRollNumber(item.student.id, item.rollNumber.trim()).subscribe({
      next: () => {
        this.snackBar.open(`Roll number assigned to ${item.student.fullName}`, 'Close', { duration: 3000 });
        this.loadStudents();
      },
      error: () => this.snackBar.open('Failed to assign roll number', 'Close', { duration: 3000 }),
    });
  }

  protected saveAll(): void {
    const valid = this.assignments().filter((a) => a.rollNumber.trim());
    if (!valid.length) {
      this.snackBar.open('No roll numbers to save', 'Close', { duration: 2000 });
      return;
    }
    this.saving.set(true);
    this.studentService.bulkAssignRollNumbers(
      valid.map((a) => ({ studentId: a.student.id, rollNumber: a.rollNumber.trim() })),
    ).subscribe({
      next: () => {
        this.snackBar.open('Roll numbers saved successfully', 'Close', { duration: 3000 });
        this.loadStudents();
        this.saving.set(false);
      },
      error: () => {
        this.snackBar.open('Failed to save roll numbers', 'Close', { duration: 3000 });
        this.saving.set(false);
      },
    });
  }
}

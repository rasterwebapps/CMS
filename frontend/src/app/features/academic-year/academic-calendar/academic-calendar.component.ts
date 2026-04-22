import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { AcademicYearService } from '../academic-year.service';
import { AcademicYear, Semester } from '../academic-year.model';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';

@Component({
  selector: 'app-academic-calendar',
  standalone: true,
  imports: [
    DatePipe,
    RouterLink,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    PageHeaderComponent,
  ],
  templateUrl: './academic-calendar.component.html',
  styleUrl: './academic-calendar.component.scss',
})
export class AcademicCalendarComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly loading = signal(false);
  protected readonly currentAcademicYear = signal<AcademicYear | null>(null);
  protected readonly semesters = signal<Semester[]>([]);
  protected readonly hasError = signal(false);

  ngOnInit(): void {
    this.loadCurrentAcademicYear();
  }

  private loadCurrentAcademicYear(): void {
    this.loading.set(true);
    this.hasError.set(false);

    this.academicYearService.getCurrentAcademicYear().subscribe({
      next: (academicYear) => {
        this.currentAcademicYear.set(academicYear);
        this.loadSemesters(academicYear.id);
      },
      error: () => {
        this.hasError.set(true);
        this.loading.set(false);
      },
    });
  }

  private loadSemesters(academicYearId: number): void {
    this.academicYearService.getSemestersByAcademicYear(academicYearId).subscribe({
      next: (semesters) => {
        const sortedSemesters = semesters.sort((a, b) => a.semesterNumber - b.semesterNumber);
        this.semesters.set(sortedSemesters);
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Failed to load semesters', 'Close', { duration: 3000 });
        this.loading.set(false);
      },
    });
  }

  protected isCurrentSemester(semester: Semester): boolean {
    const today = new Date();
    const startDate = new Date(semester.startDate);
    const endDate = new Date(semester.endDate);
    return today >= startDate && today <= endDate;
  }

  protected getSemesterStatus(semester: Semester): 'upcoming' | 'current' | 'completed' {
    const today = new Date();
    const startDate = new Date(semester.startDate);
    const endDate = new Date(semester.endDate);

    if (today < startDate) {
      return 'upcoming';
    } else if (today > endDate) {
      return 'completed';
    } else {
      return 'current';
    }
  }
}

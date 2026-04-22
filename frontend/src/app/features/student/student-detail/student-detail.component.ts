import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { StudentService } from '../student.service';
import { Student } from '../student.model';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { CmsSkeletonComponent } from '../../../shared/skeleton/skeleton.component';
import { computeInitials } from '../../../shared/utils/initials';

@Component({
  selector: 'app-student-detail',
  standalone: true,
  imports: [
    RouterLink,
    MatTabsModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    CmsStatusBadgeComponent,
    CmsSkeletonComponent,
  ],
  templateUrl: './student-detail.component.html',
  styleUrl: './student-detail.component.scss',
})
export class StudentDetailComponent implements OnInit {
  private readonly studentService = inject(StudentService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly student = signal<Student | null>(null);
  protected readonly loading = signal(false);

  /** First + last initial of the student's full name. */
  protected readonly initials = computed(() => computeInitials(this.student()?.fullName));

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.loadStudent(Number(idParam));
    }
  }

  protected editStudent(): void {
    const s = this.student();
    if (s) {
      void this.router.navigate(['/students', s.id, 'edit']);
    }
  }

  private loadStudent(id: number): void {
    this.loading.set(true);
    this.studentService.getById(id).subscribe({
      next: (student) => {
        this.student.set(student);
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Failed to load student', 'Close', { duration: 3000 });
        void this.router.navigate(['/students']);
      },
    });
  }
}

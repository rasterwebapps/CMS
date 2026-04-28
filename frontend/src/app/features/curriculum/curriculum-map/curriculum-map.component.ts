import { Component, inject, signal, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CurriculumVersionService } from '../curriculum-version.service';
import { CurriculumFullView, CurriculumSemesterCourseRequest } from '../curriculum-version.model';
import { ToastService } from '../../../core/toast/toast.service';
import { environment } from '../../../../environments';

@Component({
  selector: 'app-curriculum-map',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
  ],
  templateUrl: './curriculum-map.component.html',
  styleUrl: './curriculum-map.component.scss',
})
export class CurriculumMapComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(CurriculumVersionService);
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  protected readonly loading = signal(true);
  protected readonly adding = signal<number | null>(null);
  protected readonly removing = signal<number | null>(null);
  protected readonly curriculum = signal<CurriculumFullView | null>(null);
  protected readonly subjects = signal<{ id: number; name: string; code: string }[]>([]);
  protected readonly showAddForm = signal<number | null>(null);

  protected readonly addCourseForm: FormGroup = this.fb.group({
    subjectId: [null, Validators.required],
    sortOrder: [null],
  });

  private curriculumVersionId!: number;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      void this.router.navigate(['/curriculum-versions']);
      return;
    }
    this.curriculumVersionId = Number(id);

    this.http.get<{ id: number; name: string; code: string }[]>(`${environment.apiUrl}/courses`)
      .subscribe({ next: (data) => this.subjects.set(data) });

    this.loadCurriculum();
  }

  protected toggleAddForm(semesterNumber: number): void {
    if (this.showAddForm() === semesterNumber) {
      this.showAddForm.set(null);
    } else {
      this.showAddForm.set(semesterNumber);
      this.addCourseForm.reset();
    }
  }

  protected submitAddCourse(semesterNumber: number): void {
    if (this.addCourseForm.invalid) {
      this.addCourseForm.markAllAsTouched();
      return;
    }
    const v = this.addCourseForm.value;
    const request: CurriculumSemesterCourseRequest = {
      curriculumVersionId: this.curriculumVersionId,
      semesterNumber,
      subjectId: v.subjectId,
      sortOrder: v.sortOrder ?? undefined,
    };
    this.adding.set(semesterNumber);
    this.service.addCourse(request).subscribe({
      next: () => {
        this.toast.success('Course added');
        this.showAddForm.set(null);
        this.addCourseForm.reset();
        this.adding.set(null);
        this.loadCurriculum();
      },
      error: () => {
        this.toast.error('Failed to add course');
        this.adding.set(null);
      },
    });
  }

  protected removeCourse(id: number): void {
    this.removing.set(id);
    this.service.removeCourse(id).subscribe({
      next: () => {
        this.toast.success('Course removed');
        this.removing.set(null);
        this.loadCurriculum();
      },
      error: () => {
        this.toast.error('Failed to remove course');
        this.removing.set(null);
      },
    });
  }

  protected getSemesterNumbers(): number[] {
    const c = this.curriculum();
    if (!c) return [];
    return Array.from({ length: c.totalSemesters }, (_, i) => i + 1);
  }

  protected getCoursesForSemester(semesterNumber: number) {
    const c = this.curriculum();
    if (!c) return [];
    return c.semesters.find(s => s.semesterNumber === semesterNumber)?.courses ?? [];
  }

  protected backToVersions(): void {
    const c = this.curriculum();
    void this.router.navigate(['/curriculum-versions'], {
      queryParams: c ? { programId: c.programId } : {}
    });
  }

  private loadCurriculum(): void {
    this.loading.set(true);
    this.service.getFullCurriculum(this.curriculumVersionId).subscribe({
      next: (data) => {
        this.curriculum.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load curriculum');
        this.loading.set(false);
      },
    });
  }
}

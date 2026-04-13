import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ExaminationService } from '../examination.service';
import { ExaminationRequest } from '../examination.model';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-examination-form',
  standalone: true,
  imports: [
    RouterLink, ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatCardModule, MatProgressSpinnerModule, MatSnackBarModule,
  ],
  templateUrl: './examination-form.component.html',
  styleUrl: './examination-form.component.scss',
})
export class ExaminationFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly examinationService = inject(ExaminationService);
  private readonly http = inject(HttpClient);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Examination');
  protected readonly courses = signal<{ id: number; name: string }[]>([]);
  protected readonly semesters = signal<{ id: number; name: string }[]>([]);

  private itemId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    courseId: [null, Validators.required],
    examType: ['', Validators.required],
    date: [''],
    duration: [null, [Validators.min(1)]],
    maxMarks: [null, [Validators.min(0)]],
    semesterId: [null],
  });

  ngOnInit(): void {
    this.http.get<{ id: number; name: string }[]>(`${environment.apiUrl}/courses`).subscribe({
      next: (data) => this.courses.set(data),
    });
    this.http.get<{ id: number; name: string }[]>(`${environment.apiUrl}/semesters`).subscribe({
      next: (data) => this.semesters.set(data),
    });
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.itemId = Number(id);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Examination');
      this.loading.set(true);
      this.examinationService.getById(this.itemId).subscribe({
        next: (item) => {
          this.form.patchValue({ name: item.name, courseId: item.courseId, examType: item.examType, date: item.date || '', duration: item.duration, maxMarks: item.maxMarks, semesterId: item.semesterId });
          this.loading.set(false);
        },
        error: () => { this.snackBar.open('Failed to load', 'Close', { duration: 3000 }); void this.router.navigate(['/examinations']); },
      });
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.value;
    const request: ExaminationRequest = { name: v.name.trim(), courseId: v.courseId, examType: v.examType, date: v.date || undefined, duration: v.duration || undefined, maxMarks: v.maxMarks ?? undefined, semesterId: v.semesterId || undefined };
    this.saving.set(true);
    const op$ = this.isEditMode() ? this.examinationService.update(this.itemId!, request) : this.examinationService.create(request);
    op$.subscribe({
      next: () => { this.snackBar.open(this.isEditMode() ? 'Updated' : 'Created', 'Close', { duration: 3000 }); void this.router.navigate(['/examinations']); },
      error: () => { this.snackBar.open('Failed to save', 'Close', { duration: 3000 }); this.saving.set(false); },
    });
  }
}

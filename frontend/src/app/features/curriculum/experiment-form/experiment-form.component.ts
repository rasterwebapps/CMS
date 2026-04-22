import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { CurriculumService } from '../curriculum.service';
import { ExperimentRequest } from '../curriculum.model';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-experiment-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
  templateUrl: './experiment-form.component.html',
  styleUrl: './experiment-form.component.scss',
})
export class ExperimentFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly curriculumService = inject(CurriculumService);
  private readonly http = inject(HttpClient);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Experiment');
  protected readonly courses = signal<{ id: number; name: string; code: string }[]>([]);

  private itemId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    courseId: [null, Validators.required],
    experimentNumber: [null, [Validators.required, Validators.min(1)]],
    name: ['', [Validators.required, Validators.maxLength(255)]],
    description: [''],
    aim: [''],
    apparatus: [''],
    procedure: [''],
    expectedOutcome: [''],
    learningOutcomes: [''],
    estimatedDurationMinutes: [null, [Validators.min(0)]],
    isActive: [true],
  });

  ngOnInit(): void {
    this.http
      .get<{ id: number; name: string; code: string }[]>(`${environment.apiUrl}/courses`)
      .subscribe({
        next: (data) => this.courses.set(data),
        error: () => {
          this.snackBar.open('Failed to load courses', 'Close', { duration: 3000 });
        },
      });
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.itemId = Number(id);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Experiment');
      this.loading.set(true);
      this.curriculumService.getExperimentById(this.itemId).subscribe({
        next: (item) => {
          this.form.patchValue({
            courseId: item.courseId,
            experimentNumber: item.experimentNumber,
            name: item.name,
            description: item.description || '',
            aim: item.aim || '',
            apparatus: item.apparatus || '',
            procedure: item.procedure || '',
            expectedOutcome: item.expectedOutcome || '',
            learningOutcomes: item.learningOutcomes || '',
            estimatedDurationMinutes: item.estimatedDurationMinutes,
            isActive: item.isActive,
          });
          this.loading.set(false);
        },
        error: () => {
          this.snackBar.open('Failed to load', 'Close', { duration: 3000 });
          void this.router.navigate(['/experiments']);
        },
      });
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.value;
    const request: ExperimentRequest = {
      courseId: v.courseId,
      experimentNumber: v.experimentNumber,
      name: v.name.trim(),
      description: v.description?.trim() || undefined,
      aim: v.aim?.trim() || undefined,
      apparatus: v.apparatus?.trim() || undefined,
      procedure: v.procedure?.trim() || undefined,
      expectedOutcome: v.expectedOutcome?.trim() || undefined,
      learningOutcomes: v.learningOutcomes?.trim() || undefined,
      estimatedDurationMinutes: v.estimatedDurationMinutes ?? undefined,
      isActive: v.isActive,
    };
    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.curriculumService.updateExperiment(this.itemId!, request)
      : this.curriculumService.createExperiment(request);
    op$.subscribe({
      next: () => {
        this.snackBar.open(this.isEditMode() ? 'Updated' : 'Created', 'Close', { duration: 3000 });
        void this.router.navigate(['/experiments']);
      },
      error: () => {
        this.snackBar.open('Failed to save', 'Close', { duration: 3000 });
        this.saving.set(false);
      },
    });
  }
}

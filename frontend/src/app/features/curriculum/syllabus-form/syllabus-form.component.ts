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
import { SyllabusRequest } from '../curriculum.model';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-syllabus-form',
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
  templateUrl: './syllabus-form.component.html',
  styleUrl: './syllabus-form.component.scss',
})
export class SyllabusFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly curriculumService = inject(CurriculumService);
  private readonly http = inject(HttpClient);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Syllabus');
  protected readonly courses = signal<{ id: number; name: string; code: string }[]>([]);

  private itemId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    courseId: [null, Validators.required],
    version: [null, [Validators.required, Validators.min(1)]],
    theoryHours: [null, [Validators.min(0)]],
    labHours: [null, [Validators.min(0)]],
    tutorialHours: [null, [Validators.min(0)]],
    objectives: [''],
    content: [''],
    textBooks: [''],
    referenceBooks: [''],
    courseOutcomes: [''],
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
      this.pageTitle.set('Edit Syllabus');
      this.loading.set(true);
      this.curriculumService.getSyllabusById(this.itemId).subscribe({
        next: (item) => {
          this.form.patchValue({
            courseId: item.courseId,
            version: item.version,
            theoryHours: item.theoryHours,
            labHours: item.labHours,
            tutorialHours: item.tutorialHours,
            objectives: item.objectives || '',
            content: item.content || '',
            textBooks: item.textBooks || '',
            referenceBooks: item.referenceBooks || '',
            courseOutcomes: item.courseOutcomes || '',
            isActive: item.isActive,
          });
          this.loading.set(false);
        },
        error: () => {
          this.snackBar.open('Failed to load', 'Close', { duration: 3000 });
          void this.router.navigate(['/syllabi']);
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
    const request: SyllabusRequest = {
      courseId: v.courseId,
      version: v.version,
      theoryHours: v.theoryHours ?? undefined,
      labHours: v.labHours ?? undefined,
      tutorialHours: v.tutorialHours ?? undefined,
      objectives: v.objectives?.trim() || undefined,
      content: v.content?.trim() || undefined,
      textBooks: v.textBooks?.trim() || undefined,
      referenceBooks: v.referenceBooks?.trim() || undefined,
      courseOutcomes: v.courseOutcomes?.trim() || undefined,
      isActive: v.isActive,
    };
    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.curriculumService.updateSyllabus(this.itemId!, request)
      : this.curriculumService.createSyllabus(request);
    op$.subscribe({
      next: () => {
        this.snackBar.open(this.isEditMode() ? 'Updated' : 'Created', 'Close', { duration: 3000 });
        void this.router.navigate(['/syllabi']);
      },
      error: () => {
        this.snackBar.open('Failed to save', 'Close', { duration: 3000 });
        this.saving.set(false);
      },
    });
  }
}

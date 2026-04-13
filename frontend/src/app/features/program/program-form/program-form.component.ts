import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ProgramService } from '../program.service';
import { DegreeType, DEGREE_TYPES, ProgramRequest } from '../program.model';
import { DepartmentService } from '../../department/department.service';
import { Department } from '../../department/department.model';

@Component({
  selector: 'app-program-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
  templateUrl: './program-form.component.html',
  styleUrl: './program-form.component.scss',
})
export class ProgramFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly programService = inject(ProgramService);
  private readonly departmentService = inject(DepartmentService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Program');
  protected readonly departments = signal<Department[]>([]);
  protected readonly degreeTypes = DEGREE_TYPES;

  private programId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    code: ['', [Validators.required, Validators.maxLength(20)]],
    degreeType: ['', [Validators.required]],
    durationYears: [4, [Validators.required, Validators.min(1), Validators.max(10)]],
    departmentId: [null as number | null, [Validators.required]],
  });

  ngOnInit(): void {
    this.loadDepartments();

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.programId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Program');
      this.loadProgram();
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const request: ProgramRequest = {
      name: (this.form.value.name ?? '').trim(),
      code: (this.form.value.code ?? '').trim(),
      degreeType: this.form.value.degreeType as DegreeType,
      durationYears: this.form.value.durationYears,
      departmentId: this.form.value.departmentId,
    };

    this.saving.set(true);

    const operation$ = this.isEditMode()
      ? this.programService.update(this.programId!, request)
      : this.programService.create(request);

    operation$.subscribe({
      next: () => {
        const message = this.isEditMode()
          ? 'Program updated successfully'
          : 'Program created successfully';
        this.snackBar.open(message, 'Close', { duration: 3000 });
        void this.router.navigate(['/programs']);
      },
      error: () => {
        const message = this.isEditMode()
          ? 'Failed to update program'
          : 'Failed to create program';
        this.snackBar.open(message, 'Close', { duration: 3000 });
        this.saving.set(false);
      },
    });
  }

  protected getErrorMessage(fieldName: string): string {
    const control = this.form.get(fieldName);
    if (!control || !control.errors) {
      return '';
    }

    if (control.errors['required']) {
      return `${this.getFieldLabel(fieldName)} is required`;
    }
    if (control.errors['maxlength']) {
      const maxLength = control.errors['maxlength'].requiredLength;
      return `${this.getFieldLabel(fieldName)} must be at most ${maxLength} characters`;
    }
    if (control.errors['min']) {
      const min = control.errors['min'].min;
      return `${this.getFieldLabel(fieldName)} must be at least ${min}`;
    }
    if (control.errors['max']) {
      const max = control.errors['max'].max;
      return `${this.getFieldLabel(fieldName)} must be at most ${max}`;
    }

    return '';
  }

  private getFieldLabel(fieldName: string): string {
    const labels: Record<string, string> = {
      name: 'Name',
      code: 'Code',
      degreeType: 'Degree Type',
      durationYears: 'Duration',
      departmentId: 'Department',
    };
    return labels[fieldName] || fieldName;
  }

  private loadDepartments(): void {
    this.departmentService.getAll().subscribe({
      next: (departments) => {
        this.departments.set(departments);
      },
      error: () => {
        this.snackBar.open('Failed to load departments', 'Close', { duration: 3000 });
      },
    });
  }

  private loadProgram(): void {
    if (!this.programId) return;

    this.loading.set(true);
    this.programService.getById(this.programId).subscribe({
      next: (program) => {
        this.form.patchValue({
          name: program.name,
          code: program.code,
          degreeType: program.degreeType,
          durationYears: program.durationYears,
          departmentId: program.department?.id,
        });
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Failed to load program', 'Close', { duration: 3000 });
        void this.router.navigate(['/programs']);
      },
    });
  }
}

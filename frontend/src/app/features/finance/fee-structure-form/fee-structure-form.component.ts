import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { DecimalPipe } from '@angular/common';
import { FinanceService } from '../finance.service';
import { FeeStructureRequest } from '../finance.model';
import { environment } from '../../../../environments';

interface Program {
  id: number;
  name: string;
}

interface Course {
  id: number;
  name: string;
  durationYears: number;
}

interface AcademicYear {
  id: number;
  name: string;
}

@Component({
  selector: 'app-fee-structure-form',
  standalone: true,
  imports: [
    RouterLink, ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatCardModule, MatProgressSpinnerModule, MatSnackBarModule,
    MatSlideToggleModule, DecimalPipe,
  ],
  templateUrl: './fee-structure-form.component.html',
  styleUrl: './fee-structure-form.component.scss',
})
export class FeeStructureFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly financeService = inject(FinanceService);
  private readonly http = inject(HttpClient);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Fee Structure');
  protected readonly programs = signal<Program[]>([]);
  protected readonly courses = signal<Course[]>([]);
  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly selectedCourseDuration = signal(0);
  protected readonly calculatedTotal = signal(0);

  protected readonly feeTypes = ['TUITION', 'HOSTEL', 'TRANSPORT', 'LAB', 'LIBRARY', 'EXAM', 'OTHER'];

  private itemId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    programId: [null as number | null, Validators.required],
    courseId: [null as number | null],
    academicYearId: [null as number | null, Validators.required],
    feeType: ['', Validators.required],
    amount: [0, [Validators.required, Validators.min(0)]],
    description: [''],
    isMandatory: [true],
    isActive: [true],
    yearAmounts: this.fb.array([]),
  });

  get yearAmounts(): FormArray {
    return this.form.get('yearAmounts') as FormArray;
  }

  ngOnInit(): void {
    this.http.get<Program[]>(`${environment.apiUrl}/programs`).subscribe({
      next: (data) => this.programs.set(data),
    });
    this.http.get<AcademicYear[]>(`${environment.apiUrl}/academic-years`).subscribe({
      next: (data) => this.academicYears.set(data),
    });

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.itemId = Number(id);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Fee Structure');
      this.loading.set(true);
      this.financeService.getFeeStructureById(this.itemId).subscribe({
        next: (item) => {
          // Load courses for the program first
          if (item.programId) {
            this.http.get<Course[]>(`${environment.apiUrl}/courses/program/${item.programId}`).subscribe({
              next: (courses) => {
                this.courses.set(courses);
                const course = courses.find((c) => c.id === item.courseId);
                if (course) {
                  this.selectedCourseDuration.set(course.durationYears);
                }
              },
            });
          }

          this.form.patchValue({
            programId: item.programId,
            courseId: item.courseId,
            academicYearId: item.academicYearId,
            feeType: item.feeType,
            amount: item.amount,
            description: item.description,
            isMandatory: item.isMandatory,
            isActive: item.isActive,
          });

          // Rebuild year amounts from response
          this.yearAmounts.clear();
          if (item.yearAmounts && item.yearAmounts.length > 0) {
            for (const ya of item.yearAmounts) {
              this.yearAmounts.push(
                this.fb.group({
                  yearNumber: [ya.yearNumber],
                  yearLabel: [ya.yearLabel],
                  amount: [ya.amount, [Validators.required, Validators.min(0)]],
                }),
              );
            }
            this.recalculateTotal();
          }

          this.loading.set(false);
        },
        error: () => {
          this.snackBar.open('Failed to load', 'Close', { duration: 3000 });
          void this.router.navigate(['/fee-structures']);
        },
      });
    }
  }

  protected onProgramChange(programId: number): void {
    this.form.patchValue({ courseId: null });
    this.courses.set([]);
    this.selectedCourseDuration.set(0);
    this.yearAmounts.clear();
    this.recalculateTotal();

    if (programId) {
      this.http.get<Course[]>(`${environment.apiUrl}/courses/program/${programId}`).subscribe({
        next: (data) => this.courses.set(data),
      });
    }
  }

  protected onCourseChange(courseId: number): void {
    const course = this.courses().find((c) => c.id === courseId);
    const duration = course ? course.durationYears : 0;
    this.selectedCourseDuration.set(duration);

    this.yearAmounts.clear();
    for (let i = 1; i <= duration; i++) {
      this.yearAmounts.push(
        this.fb.group({
          yearNumber: [i],
          yearLabel: [`Year ${i}`],
          amount: [0, [Validators.required, Validators.min(0)]],
        }),
      );
    }
    this.recalculateTotal();
  }

  protected onYearAmountChange(): void {
    this.recalculateTotal();
  }

  private recalculateTotal(): void {
    let total = 0;
    for (let i = 0; i < this.yearAmounts.length; i++) {
      total += Number(this.yearAmounts.at(i).get('amount')?.value) || 0;
    }
    this.calculatedTotal.set(total);
    if (this.yearAmounts.length > 0) {
      this.form.patchValue({ amount: total }, { emitEvent: false });
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.value;
    const request: FeeStructureRequest = {
      programId: v.programId,
      academicYearId: v.academicYearId,
      feeType: v.feeType,
      amount: v.amount,
      description: v.description || undefined,
      isMandatory: v.isMandatory,
      isActive: v.isActive,
      courseId: v.courseId || undefined,
      yearAmounts:
        v.yearAmounts && v.yearAmounts.length > 0
          ? v.yearAmounts.map((ya: { yearNumber: number; yearLabel: string; amount: number }) => ({
              yearNumber: ya.yearNumber,
              yearLabel: ya.yearLabel,
              amount: ya.amount,
            }))
          : undefined,
    };
    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.financeService.updateFeeStructure(this.itemId!, request)
      : this.financeService.createFeeStructure(request);
    op$.subscribe({
      next: () => {
        this.snackBar.open(this.isEditMode() ? 'Updated' : 'Created', 'Close', { duration: 3000 });
        void this.router.navigate(['/fee-structures']);
      },
      error: () => {
        this.snackBar.open('Failed to save', 'Close', { duration: 3000 });
        this.saving.set(false);
      },
    });
  }
}

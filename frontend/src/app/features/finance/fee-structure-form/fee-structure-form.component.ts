import { Component, inject, OnInit, signal, computed } from '@angular/core';
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
import { MatTooltipModule } from '@angular/material/tooltip';
import { DecimalPipe } from '@angular/common';
import { FinanceService } from '../finance.service';
import { BulkFeeStructureRequest, FeeStructureItemRequest, FeeStructureRequest } from '../finance.model';
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
    MatSlideToggleModule, MatTooltipModule, DecimalPipe,
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
  protected readonly pageTitle = signal('Add Fee Structures');
  protected readonly programs = signal<Program[]>([]);
  protected readonly courses = signal<Course[]>([]);
  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly selectedCourseDuration = signal(0);

  // Edit mode — single item form (unchanged)
  protected readonly calculatedTotal = signal(0);

  protected readonly feeTypes = [
    'TUITION', 'LAB_FEE', 'LIBRARY_FEE', 'EXAMINATION_FEE',
    'HOSTEL_FEE', 'TRANSPORT_FEE', 'MISCELLANEOUS', 'LATE_FEE',
  ];

  protected readonly feeTypeLabels: Record<string, string> = {
    TUITION: 'Tuition Fee',
    LAB_FEE: 'Lab Fee',
    LIBRARY_FEE: 'Library Fee',
    EXAMINATION_FEE: 'Examination Fee',
    HOSTEL_FEE: 'Hostel Fee',
    TRANSPORT_FEE: 'Transport Fee',
    MISCELLANEOUS: 'Miscellaneous',
    LATE_FEE: 'Late Fee',
  };

  private itemId: number | null = null;

  // ── Edit mode form (single fee type) ──────────────────────────────────────
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

  // ── Create mode form (bulk — bill-style) ──────────────────────────────────
  protected readonly bulkForm: FormGroup = this.fb.group({
    programId: [null as number | null, Validators.required],
    courseId: [null as number | null],
    academicYearId: [null as number | null, Validators.required],
    items: this.fb.array([]),
  });

  get feeItems(): FormArray {
    return this.bulkForm.get('items') as FormArray;
  }

  private readonly _grandTotalVersion = signal(0);

  protected readonly grandTotal = computed(() => {
    this._grandTotalVersion(); // track changes
    let total = 0;
    for (let i = 0; i < this.feeItems.length; i++) {
      const itemGroup = this.feeItems.at(i) as FormGroup;
      const itemYearAmounts = itemGroup.get('yearAmounts') as FormArray;
      if (itemYearAmounts && itemYearAmounts.length > 0) {
        for (let j = 0; j < itemYearAmounts.length; j++) {
          total += Number(itemYearAmounts.at(j).get('amount')?.value) || 0;
        }
      } else {
        total += Number(itemGroup.get('amount')?.value) || 0;
      }
    }
    return total;
  });

  protected getItemGroup(i: number): FormGroup {
    return this.feeItems.at(i) as FormGroup;
  }

  protected getItemYearAmounts(i: number): FormArray {
    return (this.feeItems.at(i) as FormGroup).get('yearAmounts') as FormArray;
  }

  protected getAvailableFeeTypes(currentIndex: number): string[] {
    const selectedInOtherRows = new Set<string>();
    for (let i = 0; i < this.feeItems.length; i++) {
      if (i !== currentIndex) {
        const val = this.feeItems.at(i).get('feeType')?.value;
        if (val) selectedInOtherRows.add(val);
      }
    }
    return this.feeTypes.filter((ft) => !selectedInOtherRows.has(ft));
  }

  protected get canAddMoreItems(): boolean {
    return this.feeItems.length < this.feeTypes.length;
  }

  protected getItemRowTotal(i: number): number {
    const itemGroup = this.feeItems.at(i) as FormGroup;
    const itemYearAmounts = itemGroup.get('yearAmounts') as FormArray;
    if (itemYearAmounts && itemYearAmounts.length > 0) {
      let t = 0;
      for (let j = 0; j < itemYearAmounts.length; j++) {
        t += Number(itemYearAmounts.at(j).get('amount')?.value) || 0;
      }
      return t;
    }
    return Number(itemGroup.get('amount')?.value) || 0;
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
    } else {
      // Create mode — start with one blank item row
      this.addItem();
    }
  }

  // ── Edit mode: program/course/year-amount helpers ──────────────────────────

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

  // ── Create mode (bulk): program/course/item helpers ────────────────────────

  protected onBulkProgramChange(programId: number): void {
    this.bulkForm.patchValue({ courseId: null });
    this.courses.set([]);
    this.selectedCourseDuration.set(0);
    this.clearAllItemYearAmounts();

    if (programId) {
      this.http.get<Course[]>(`${environment.apiUrl}/courses/program/${programId}`).subscribe({
        next: (data) => this.courses.set(data),
      });
    }
  }

  protected onBulkCourseChange(courseId: number): void {
    const course = this.courses().find((c) => c.id === courseId);
    const duration = course ? course.durationYears : 0;
    this.selectedCourseDuration.set(duration);
    this.rebuildAllItemYearAmounts(duration);
  }

  private clearAllItemYearAmounts(): void {
    for (let i = 0; i < this.feeItems.length; i++) {
      const ya = (this.feeItems.at(i) as FormGroup).get('yearAmounts') as FormArray;
      ya.clear();
      (this.feeItems.at(i) as FormGroup).patchValue({ amount: 0 }, { emitEvent: false });
    }
  }

  private rebuildAllItemYearAmounts(duration: number): void {
    for (let i = 0; i < this.feeItems.length; i++) {
      this.buildYearAmountsForItem(i, duration);
    }
  }

  private buildYearAmountsForItem(itemIndex: number, duration: number): void {
    const ya = (this.feeItems.at(itemIndex) as FormGroup).get('yearAmounts') as FormArray;
    ya.clear();
    for (let i = 1; i <= duration; i++) {
      ya.push(
        this.fb.group({
          yearNumber: [i],
          yearLabel: [`Year ${i}`],
          amount: [0, [Validators.required, Validators.min(0)]],
        }),
      );
    }
  }

  protected onItemYearAmountChange(itemIndex: number): void {
    const ya = this.getItemYearAmounts(itemIndex);
    let total = 0;
    for (let j = 0; j < ya.length; j++) {
      total += Number(ya.at(j).get('amount')?.value) || 0;
    }
    (this.feeItems.at(itemIndex) as FormGroup).patchValue({ amount: total }, { emitEvent: false });
    this._grandTotalVersion.update((v) => v + 1);
  }

  protected onItemAmountChange(): void {
    this._grandTotalVersion.update((v) => v + 1);
  }

  protected addItem(): void {
    const newGroup = this.fb.group({
      feeType: ['', Validators.required],
      amount: [0, [Validators.required, Validators.min(0.01)]],
      description: [''],
      isMandatory: [true],
      isActive: [true],
      yearAmounts: this.fb.array([]),
    });
    this.feeItems.push(newGroup);

    const duration = this.selectedCourseDuration();
    if (duration > 1) {
      this.buildYearAmountsForItem(this.feeItems.length - 1, duration);
    }
    this._grandTotalVersion.update((v) => v + 1);
  }

  protected removeItem(index: number): void {
    this.feeItems.removeAt(index);
    this._grandTotalVersion.update((v) => v + 1);
  }

  // ── Submit ────────────────────────────────────────────────────────────────

  protected onSubmit(): void {
    if (this.isEditMode()) {
      this.submitEdit();
    } else {
      this.submitBulkCreate();
    }
  }

  private submitEdit(): void {
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
    this.financeService.updateFeeStructure(this.itemId!, request).subscribe({
      next: () => {
        this.snackBar.open('Updated', 'Close', { duration: 3000 });
        void this.router.navigate(['/fee-structures']);
      },
      error: () => {
        this.snackBar.open('Failed to save', 'Close', { duration: 3000 });
        this.saving.set(false);
      },
    });
  }

  private submitBulkCreate(): void {
    if (this.bulkForm.invalid) {
      this.bulkForm.markAllAsTouched();
      return;
    }
    if (this.feeItems.length === 0) {
      this.snackBar.open('Add at least one fee item', 'Close', { duration: 3000 });
      return;
    }
    const bv = this.bulkForm.value;
    const items: FeeStructureItemRequest[] = bv.items.map(
      (item: {
        feeType: string;
        amount: number;
        description: string;
        isMandatory: boolean;
        isActive: boolean;
        yearAmounts: { yearNumber: number; yearLabel: string; amount: number }[];
      }) => ({
        feeType: item.feeType,
        amount: item.amount,
        description: item.description || undefined,
        isMandatory: item.isMandatory,
        isActive: item.isActive,
        yearAmounts:
          item.yearAmounts && item.yearAmounts.length > 0
            ? item.yearAmounts.map((ya) => ({
                yearNumber: ya.yearNumber,
                yearLabel: ya.yearLabel,
                amount: ya.amount,
              }))
            : undefined,
      }),
    );
    const request: BulkFeeStructureRequest = {
      programId: bv.programId,
      academicYearId: bv.academicYearId,
      courseId: bv.courseId || undefined,
      items,
    };
    this.saving.set(true);
    this.financeService.bulkCreateFeeStructures(request).subscribe({
      next: (created) => {
        this.snackBar.open(`${created.length} fee structure(s) created`, 'Close', { duration: 3000 });
        void this.router.navigate(['/fee-structures']);
      },
      error: (err) => {
        const msg = err?.error?.message ?? 'Failed to save fee structures';
        this.snackBar.open(msg, 'Close', { duration: 4000 });
        this.saving.set(false);
      },
    });
  }
}

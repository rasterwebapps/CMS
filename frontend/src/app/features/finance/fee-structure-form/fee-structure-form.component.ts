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
import { BulkFeeStructureRequest, FeeStructureItemRequest } from '../finance.model';
import { environment } from '../../../../environments';

interface Program {
  id: number;
  name: string;
  durationYears: number;
}

interface Course {
  id: number;
  name: string;
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
  protected readonly selectedProgramDuration = signal(0);
  private readonly _courseSelected = signal(false);

  /** Show fee items only after a course is selected (create mode) or always in edit mode.
   *  Fallback: also show when a program is selected but the program has no courses. */
  protected readonly showFeeItems = computed(
    () =>
      this.isEditMode() ||
      this._courseSelected() ||
      (this.selectedProgramDuration() > 0 && this.courses().length === 0),
  );

  /** All fee types in display order (generic first, then additional). */
  protected readonly feeTypes = [
    'TUITION', 'LAB_FEE', 'LIBRARY_FEE', 'EXAMINATION_FEE',
    'MISCELLANEOUS', 'LATE_FEE', 'HOSTEL_FEE', 'TRANSPORT_FEE',
  ];

  /** Generic fee types — included in the course total. */
  protected readonly genericFeeTypes = [
    'TUITION', 'LAB_FEE', 'LIBRARY_FEE', 'EXAMINATION_FEE', 'MISCELLANEOUS', 'LATE_FEE',
  ];

  /** Additional fee types — NOT included in the generic course total. */
  protected readonly additionalFeeTypes = ['HOSTEL_FEE', 'TRANSPORT_FEE'];

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

  // Bulk form — used for both create and edit
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

  /** Course total — includes only Generic fee types (excludes HOSTEL_FEE and TRANSPORT_FEE). */
  protected readonly grandTotal = computed(() => {
    this._grandTotalVersion();
    let total = 0;
    for (let i = 0; i < this.feeItems.length; i++) {
      const itemGroup = this.feeItems.at(i) as FormGroup;
      const feeType: string = itemGroup.get('feeType')?.value;
      if (!this.genericFeeTypes.includes(feeType)) continue;
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

  /** Additional fees total (HOSTEL_FEE + TRANSPORT_FEE). */
  protected readonly additionalTotal = computed(() => {
    this._grandTotalVersion();
    let total = 0;
    for (let i = 0; i < this.feeItems.length; i++) {
      const itemGroup = this.feeItems.at(i) as FormGroup;
      const feeType: string = itemGroup.get('feeType')?.value;
      if (!this.additionalFeeTypes.includes(feeType)) continue;
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
      next: (data) => {
        this.programs.set(data);
        // In edit mode, set program duration once programs are loaded
        const pId = this.route.snapshot.queryParamMap.get('programId');
        if (pId) {
          const program = data.find((p) => p.id === Number(pId));
          if (program) this.selectedProgramDuration.set(program.durationYears);
        }
      },
    });
    this.http.get<AcademicYear[]>(`${environment.apiUrl}/academic-years`).subscribe({
      next: (data) => this.academicYears.set(data),
    });

    const qp = this.route.snapshot.queryParamMap;
    const programId = qp.get('programId');
    const academicYearId = qp.get('academicYearId');
    const courseId = qp.get('courseId');

    if (programId && academicYearId) {
      // Edit mode — load group via query params
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Fee Structures');
      this.loading.set(true);

      const pId = Number(programId);
      const ayId = Number(academicYearId);
      const cId = courseId ? Number(courseId) : undefined;

      // Load courses for the program
      this.http.get<Course[]>(`${environment.apiUrl}/courses/program/${pId}`).subscribe({
        next: (courses) => this.courses.set(courses),
      });

      this.financeService.getGroupedFeeStructures({ programId: pId, academicYearId: ayId, courseId: cId }).subscribe({
        next: (groups) => {
          const group = groups.length > 0 ? groups[0] : null;
          this.bulkForm.patchValue({
            programId: pId,
            academicYearId: ayId,
            courseId: cId ?? null,
          });
          this.feeItems.clear();
          if (group) {
            for (const item of group.items) {
              const newGroup = this.fb.group({
                feeType: [item.feeType, Validators.required],
                amount: [item.amount, [Validators.required, Validators.min(0)]],
                description: [item.description || ''],
                yearAmounts: this.fb.array([]),
              });
              if (item.yearAmounts && item.yearAmounts.length > 0) {
                const ya = newGroup.get('yearAmounts') as FormArray;
                for (const y of item.yearAmounts) {
                  ya.push(this.fb.group({
                    yearNumber: [y.yearNumber],
                    yearLabel: [y.yearLabel],
                    amount: [y.amount, [Validators.required, Validators.min(0)]],
                  }));
                }
              }
              this.feeItems.push(newGroup);
            }
          }
          // Fill in missing fee types with amount 0
          const existingTypes = new Set(group ? group.items.map((i) => i.feeType) : []);
          const duration = this.selectedProgramDuration();
          for (const ft of this.feeTypes) {
            if (!existingTypes.has(ft)) {
              const newGroup = this.fb.group({
                feeType: [ft, Validators.required],
                amount: [0, [Validators.required, Validators.min(0)]],
                description: [''],
                yearAmounts: this.fb.array([]),
              });
              if (duration > 1) {
                const ya = newGroup.get('yearAmounts') as FormArray;
                for (let i = 1; i <= duration; i++) {
                  ya.push(this.fb.group({ yearNumber: [i], yearLabel: [`Year ${i}`], amount: [0, [Validators.required, Validators.min(0)]] }));
                }
              }
              this.feeItems.push(newGroup);
            }
          }
          this._grandTotalVersion.update((v) => v + 1);
          this.loading.set(false);
        },
        error: () => {
          this.snackBar.open('Failed to load fee structures', 'Close', { duration: 3000 });
          void this.router.navigate(['/fee-structures']);
        },
      });
    } else {
      // Create mode — pre-populate all 8 fee types
      for (const ft of this.feeTypes) {
        this.addItemWithType(ft);
      }
    }
  }

  // ── Program/course helpers ─────────────────────────────────────────────────

  protected onBulkProgramChange(programId: number): void {
    this.bulkForm.patchValue({ courseId: null });
    this.courses.set([]);
    this._courseSelected.set(false);
    this.clearAllItemYearAmounts();

    if (programId) {
      const program = this.programs().find((p) => p.id === programId);
      const duration = program ? program.durationYears : 0;
      this.selectedProgramDuration.set(duration);
      this.rebuildAllItemYearAmounts(duration);

      this.http.get<Course[]>(`${environment.apiUrl}/courses/program/${programId}`).subscribe({
        next: (data) => this.courses.set(data),
      });
    } else {
      this.selectedProgramDuration.set(0);
    }
  }

  protected onBulkCourseChange(courseId: number): void {
    this._courseSelected.set(!!courseId);
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
      ya.push(this.fb.group({
        yearNumber: [i],
        yearLabel: [`Year ${i}`],
        amount: [0, [Validators.required, Validators.min(0)]],
      }));
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

  private addItemWithType(feeType: string): void {
    const newGroup = this.fb.group({
      feeType: [feeType, Validators.required],
      amount: [0, [Validators.required, Validators.min(0)]],
      description: [''],
      yearAmounts: this.fb.array([]),
    });
    this.feeItems.push(newGroup);
    const duration = this.selectedProgramDuration();
    if (duration > 1) {
      this.buildYearAmountsForItem(this.feeItems.length - 1, duration);
    }
    this._grandTotalVersion.update((v) => v + 1);
  }

  // ── Submit ────────────────────────────────────────────────────────────────

  protected onSubmit(): void {
    if (this.bulkForm.invalid) {
      this.bulkForm.markAllAsTouched();
      return;
    }
    if (this.feeItems.length === 0) {
      this.snackBar.open('Add at least one fee item', 'Close', { duration: 3000 });
      return;
    }
    const bv = this.bulkForm.value;
    // Filter out items with zero amount (optional fee types not filled in)
    const nonZeroItems = (bv.items as {
      feeType: string;
      amount: number;
      description: string;
      yearAmounts: { yearNumber: number; yearLabel: string; amount: number }[];
    }[]).filter((item) => {
      if (item.yearAmounts && item.yearAmounts.length > 0) {
        return item.yearAmounts.some((ya) => Number(ya.amount) > 0);
      }
      return Number(item.amount) > 0;
    });

    if (nonZeroItems.length === 0) {
      this.snackBar.open('Enter an amount for at least one fee type', 'Close', { duration: 3000 });
      return;
    }

    const items: FeeStructureItemRequest[] = nonZeroItems.map((item) => ({
      feeType: item.feeType,
      amount: item.amount,
      description: item.description || undefined,
      yearAmounts:
        item.yearAmounts && item.yearAmounts.length > 0
          ? item.yearAmounts.map((ya) => ({
              yearNumber: ya.yearNumber,
              yearLabel: ya.yearLabel,
              amount: ya.amount,
            }))
          : undefined,
    }));

    const request: BulkFeeStructureRequest = {
      programId: bv.programId,
      academicYearId: bv.academicYearId,
      courseId: bv.courseId || undefined,
      items,
    };

    this.saving.set(true);

    if (this.isEditMode()) {
      this.financeService.bulkUpdateFeeStructures(request).subscribe({
        next: () => {
          this.snackBar.open('Updated successfully', 'Close', { duration: 3000 });
          void this.router.navigate(['/fee-structures']);
        },
        error: (err) => {
          const msg = err?.error?.message ?? 'Failed to update fee structures';
          this.snackBar.open(msg, 'Close', { duration: 4000 });
          this.saving.set(false);
        },
      });
    } else {
      this.financeService.bulkCreateFeeStructures(request).subscribe({
        next: (created) => {
          this.snackBar.open(`${created.length} fee structure(s) saved`, 'Close', { duration: 3000 });
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
}

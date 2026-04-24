import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DecimalPipe } from '@angular/common';
import { FinanceService } from '../finance.service';
import { BulkFeeStructureRequest, FeeStructureItemRequest } from '../finance.model';
import { environment } from '../../../../environments';
import { LayoutService } from '../../../core/layout/layout.service';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { ToastService } from '../../../core/toast/toast.service';

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
    RouterLink, ReactiveFormsModule,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule, MatTooltipModule, DecimalPipe,
    PageHeaderComponent],
  templateUrl: './fee-structure-form.component.html',
  styleUrl: './fee-structure-form.component.scss',
})
export class FeeStructureFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly financeService = inject(FinanceService);
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);
  protected readonly layoutService = inject(LayoutService);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Fee Structures');
  protected readonly programs = signal<Program[]>([]);
  protected readonly courses = signal<Course[]>([]);
  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly selectedProgramDuration = signal(0);
  private readonly _courseSelected = signal(false);

  /** Existing grouped fee structures for the selected academic year + program combination,
   *  used to disable course options that already have a fee structure. */
  private readonly _existingGroups = signal<{ courseId: number | null }[]>([]);

  /** Set of courseIds that already have a fee structure for the current academic year + program. */
  protected readonly existingCourseIds = computed(
    () => new Set(this._existingGroups().map((g) => g.courseId).filter((id): id is number => id !== null)),
  );

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
    'MISCELLANEOUS', 'LATE_FEE', 'HOSTEL_FEE', 'TRANSPORT_FEE'];

  /** Generic fee types — included in the course total. */
  protected readonly genericFeeTypes = [
    'TUITION', 'LAB_FEE', 'LIBRARY_FEE', 'EXAMINATION_FEE', 'MISCELLANEOUS', 'LATE_FEE'];

  /** Additional fee types — NOT included in the generic course total. */
  protected readonly additionalFeeTypes = ['HOSTEL_FEE', 'TRANSPORT_FEE'];

  protected readonly feeTypeMeta: Record<string, { label: string; icon: string }> = {
    TUITION:        { label: 'Tuition Fee',      icon: 'school' },
    LAB_FEE:        { label: 'Lab Fee',           icon: 'science' },
    LIBRARY_FEE:    { label: 'Library Fee',       icon: 'menu_book' },
    EXAMINATION_FEE:{ label: 'Examination Fee',  icon: 'assignment' },
    HOSTEL_FEE:     { label: 'Hostel Fee',        icon: 'hotel' },
    TRANSPORT_FEE:  { label: 'Transport Fee',     icon: 'directions_bus' },
    MISCELLANEOUS:  { label: 'Miscellaneous',     icon: 'category' },
    LATE_FEE:       { label: 'Late Fee',          icon: 'schedule' },
  };

  // Bulk form — used for both create and edit
  // programId and courseId start disabled; they are enabled progressively as upstream fields are filled
  protected readonly bulkForm: FormGroup = this.fb.group({
    programId: [{ value: null as number | null, disabled: true }, Validators.required],
    courseId: [{ value: null as number | null, disabled: true }],
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

  // ── Grid view helpers ────────────────────────────────────────────────────

  private readonly _expandedNotes = signal<Set<number>>(new Set<number>());

  /** Year numbers for column headers: [1, 2, …, N] or [1] when N ≤ 1. */
  protected readonly yearRange = computed(() => {
    const d = this.selectedProgramDuration();
    return d > 1 ? Array.from({ length: d }, (_, i) => i + 1) : [1];
  });

  /** CSS grid-template-columns value shared by every row in the fee grid. */
  protected readonly gridTemplateColumns = computed(() => {
    const cols = Math.max(this.selectedProgramDuration(), 1);
    return `minmax(180px, 220px) repeat(${cols}, minmax(96px, 1fr)) 110px`;
  });

  /** Per-year column totals for generic (course) fee types. */
  protected readonly yearTotals = computed(() => {
    this._grandTotalVersion();
    const cols = Math.max(this.selectedProgramDuration(), 1);
    const totals = new Array<number>(cols).fill(0);
    for (let i = 0; i < this.feeItems.length; i++) {
      const ig = this.feeItems.at(i) as FormGroup;
      if (!this.genericFeeTypes.includes(ig.get('feeType')?.value as string)) continue;
      const ya = ig.get('yearAmounts') as FormArray;
      if (ya && ya.length > 0) {
        for (let j = 0; j < Math.min(ya.length, cols); j++) {
          totals[j] += Number(ya.at(j).get('amount')?.value) || 0;
        }
      } else {
        totals[0] += Number(ig.get('amount')?.value) || 0;
      }
    }
    return totals;
  });

  /** Per-year column totals for additional fee types. */
  protected readonly additionalYearTotals = computed(() => {
    this._grandTotalVersion();
    const cols = Math.max(this.selectedProgramDuration(), 1);
    const totals = new Array<number>(cols).fill(0);
    for (let i = 0; i < this.feeItems.length; i++) {
      const ig = this.feeItems.at(i) as FormGroup;
      if (!this.additionalFeeTypes.includes(ig.get('feeType')?.value as string)) continue;
      const ya = ig.get('yearAmounts') as FormArray;
      if (ya && ya.length > 0) {
        for (let j = 0; j < Math.min(ya.length, cols); j++) {
          totals[j] += Number(ya.at(j).get('amount')?.value) || 0;
        }
      } else {
        totals[0] += Number(ig.get('amount')?.value) || 0;
      }
    }
    return totals;
  });

  protected toggleNote(index: number): void {
    this._expandedNotes.update(s => {
      const next = new Set(s);
      if (next.has(index)) next.delete(index); else next.add(index);
      return next;
    });
  }

  protected isNoteExpanded(index: number): boolean {
    return this._expandedNotes().has(index);
  }

  // ─────────────────────────────────────────────────────────────────────────

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
    this.http.get<AcademicYear[]>(`${environment.apiUrl}/academic-years`).subscribe({
      next: (data) => this.academicYears.set(data),
    });

    const qp = this.route.snapshot.queryParamMap;
    const programId = qp.get('programId');
    const academicYearId = qp.get('academicYearId');
    const courseId = qp.get('courseId');

    if (programId && academicYearId) {
      // Edit mode — programs must load first so duration is known before filling missing fee types
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Fee Structures');
      this.loading.set(true);

      const pId = Number(programId);
      const ayId = Number(academicYearId);
      const cId = courseId ? Number(courseId) : undefined;

      this.http.get<Program[]>(`${environment.apiUrl}/programs`).subscribe({
        next: (data) => {
          this.programs.set(data);
          const program = data.find((p) => p.id === pId);
          if (program) this.selectedProgramDuration.set(program.durationYears);

          // Courses and fee structures load in parallel now that duration is known
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
                    amount: [item.amount, [Validators.min(0)]],
                    description: [item.description || ''],
                    yearAmounts: this.fb.array([]),
                  });
                  if (item.yearAmounts && item.yearAmounts.length > 0) {
                    const ya = newGroup.get('yearAmounts') as FormArray;
                    for (const y of item.yearAmounts) {
                      ya.push(this.fb.group({
                        yearNumber: [y.yearNumber],
                        yearLabel: [y.yearLabel],
                        amount: [y.amount, [Validators.min(0)]],
                      }));
                    }
                  }
                  this.feeItems.push(newGroup);
                }
              }
              // Fill in missing fee types — duration is guaranteed to be set at this point
              const existingTypes = new Set(group ? group.items.map((i) => i.feeType) : []);
              const duration = this.selectedProgramDuration();
              for (const ft of this.feeTypes) {
                if (!existingTypes.has(ft)) {
                  const newGroup = this.fb.group({
                    feeType: [ft, Validators.required],
                    amount: [0, [Validators.min(0)]],
                    description: [''],
                    yearAmounts: this.fb.array([]),
                  });
                  if (duration > 1) {
                    const ya = newGroup.get('yearAmounts') as FormArray;
                    for (let i = 1; i <= duration; i++) {
                      ya.push(this.fb.group({ yearNumber: [i], yearLabel: [`Year ${i}`], amount: [0, [Validators.min(0)]] }));
                    }
                  }
                  this.feeItems.push(newGroup);
                }
              }
              this._grandTotalVersion.update((v) => v + 1);
              this.loading.set(false);
              // Lock all criteria dropdowns in edit mode — they must not be changed
              this.bulkForm.get('academicYearId')?.disable();
              this.bulkForm.get('programId')?.disable();
              this.bulkForm.get('courseId')?.disable();
            },
            error: () => {
              this.toast.error('Failed to load fee structures');
              void this.router.navigate(['/fee-structures']);
            },
          });
        },
      });
    } else {
      // Create mode — programs load in parallel, pre-populate all 8 fee types
      this.http.get<Program[]>(`${environment.apiUrl}/programs`).subscribe({
        next: (data) => this.programs.set(data),
      });
      for (const ft of this.feeTypes) {
        this.addItemWithType(ft);
      }
    }
  }

  // ── Program/course helpers ─────────────────────────────────────────────────

  protected onBulkAcademicYearChange(yearId: number): void {
    this.bulkForm.patchValue({ programId: null, courseId: null });
    this.bulkForm.get('courseId')?.disable();
    if (yearId) {
      this.bulkForm.get('programId')?.enable();
    } else {
      this.bulkForm.get('programId')?.disable();
    }
    this.courses.set([]);
    this._courseSelected.set(false);
    this._existingGroups.set([]);
    this.selectedProgramDuration.set(0);
    this.clearAllItemYearAmounts();
  }

  protected onBulkProgramChange(programId: number): void {
    this.bulkForm.patchValue({ courseId: null });
    this.bulkForm.get('courseId')?.disable();
    this.courses.set([]);
    this._courseSelected.set(false);
    this._existingGroups.set([]);
    this.clearAllItemYearAmounts();

    if (programId) {
      const program = this.programs().find((p) => p.id === programId);
      const duration = program ? program.durationYears : 0;
      this.selectedProgramDuration.set(duration);
      this.rebuildAllItemYearAmounts(duration);

      this.http.get<Course[]>(`${environment.apiUrl}/courses/program/${programId}`).subscribe({
        next: (data) => {
          this.courses.set(data);
          if (data.length > 0) {
            this.bulkForm.get('courseId')?.enable();
          }
        },
      });

      // Load existing fee structures to prevent duplicate course selection
      const academicYearId = this.bulkForm.get('academicYearId')?.value as number | null;
      if (academicYearId) {
        this.financeService
          .getGroupedFeeStructures({ programId, academicYearId })
          .subscribe({ next: (groups) => this._existingGroups.set(groups) });
      }
    } else {
      this.selectedProgramDuration.set(0);
    }
  }

  protected onBulkCourseChange(courseId: number): void {
    this._courseSelected.set(!!courseId);
    // Reset all fee amounts when a different course is selected
    this.clearAllItemYearAmounts();
    const duration = this.selectedProgramDuration();
    if (duration > 1) {
      this.rebuildAllItemYearAmounts(duration);
    }
    this._grandTotalVersion.update((v) => v + 1);
  }

  protected isCourseDisabled(courseId: number): boolean {
    return this.existingCourseIds().has(courseId);
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
        amount: [0, [Validators.min(0)]],
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
      amount: [0, [Validators.min(0)]],
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
      this.toast.warning('Add at least one fee item');
      return;
    }
    const rv = this.bulkForm.getRawValue();
    // Treat blank/null amounts as 0 and filter out items where the total is zero
    const nonZeroItems = (rv.items as {
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

    // The grand total across all generic fee types must be > 0
    const totalFee = nonZeroItems
      .filter((item) => this.genericFeeTypes.includes(item.feeType))
      .reduce((sum, item) => {
        if (item.yearAmounts && item.yearAmounts.length > 0) {
          return sum + item.yearAmounts.reduce((s, ya) => s + (Number(ya.amount) || 0), 0);
        }
        return sum + (Number(item.amount) || 0);
      }, 0);

    if (totalFee === 0) {
      this.toast.warning('Total course fee must be greater than zero');
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
      programId: rv.programId,
      academicYearId: rv.academicYearId,
      courseId: rv.courseId || undefined,
      items,
    };

    this.saving.set(true);

    if (this.isEditMode()) {
      this.financeService.bulkUpdateFeeStructures(request).subscribe({
        next: () => {
          this.toast.success('Updated successfully');
          void this.router.navigate(['/fee-structures']);
        },
        error: (err) => {
          const msg = err?.error?.message ?? 'Failed to update fee structures';
          this.toast.error(msg);
          this.saving.set(false);
        },
      });
    } else {
      this.financeService.bulkCreateFeeStructures(request).subscribe({
        next: (created) => {
          this.toast.success(`${created.length} fee structure(s) saved`);
          void this.router.navigate(['/fee-structures']);
        },
        error: (err) => {
          const msg = err?.error?.message ?? 'Failed to save fee structures';
          this.toast.error(msg);
          this.saving.set(false);
        },
      });
    }
  }
}

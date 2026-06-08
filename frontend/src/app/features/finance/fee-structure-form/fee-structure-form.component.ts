import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormArray, FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FinanceService } from '../finance.service';
import { BulkFeeStructureRequest, FeeState, FeeStructureItemRequest, GroupedFeeStructure, YearAmountRequest } from '../finance.model';
import { environment } from '../../../../environments';
import { LayoutService } from '../../../core/layout/layout.service';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { FEE_STRUCTURE_FORM_TOUR } from '../../../shared/tour/tours/fee-structure.tours';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';

interface Program { id: number; name: string; durationYears: number; }
interface Course  { id: number; name: string; }
interface AcademicYear { id: number; name: string; }

interface RawYearAmountValue   { yearNumber: number; yearLabel: string; amount: number | null; }
interface RawFeeStructureItemValue {
  feeType: string; amount: number | null; description: string; yearAmounts: RawYearAmountValue[];
}
interface NormalizedFeeStructureItemValue {
  feeType: string; amount: number; description: string;
  yearAmounts?: { yearNumber: number; yearLabel: string; amount: number }[];
}

type Quota  = 'MANAGEMENT' | 'COUNSELLING';
type Gender = 'MALE' | 'FEMALE' | 'OTHER';

interface ReplicationTarget {
  quota: Quota;
  feeStateId: number;
  feeStateName: string;
  gender: Gender;
}

@Component({
  selector: 'app-fee-structure-form',
  standalone: true,
  imports: [
    InrPipe, RouterLink, ReactiveFormsModule,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule, MatTooltipModule,
    PageHeaderComponent, CmsTourButtonComponent,
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
  private readonly toast = inject(ToastService);
  protected readonly layoutService = inject(LayoutService);
  private readonly tourService = inject(TourService);

  protected readonly loading   = signal(false);
  protected readonly saving    = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Fee Structures');
  protected readonly finalizedCount = signal(0);
  protected readonly reasonCtrl = new FormControl('');
  protected readonly showReasonField = computed(() => this.isEditMode() && this.finalizedCount() > 0);

  // ── Replication options (create mode only) ────────────────────────────────
  protected readonly replicateAllGenders = signal(false);
  protected readonly replicateAllStates  = signal(false);
  protected readonly replicateAllQuotas  = signal(false);

  protected readonly programs      = signal<Program[]>([]);
  protected readonly courses       = signal<Course[]>([]);
  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly feeStates     = signal<FeeState[]>([]);
  protected readonly selectedProgramDuration = signal(0);

  private readonly _allCriteriaFilled  = signal(false);
  private readonly _dimensionVersion   = signal(0);
  private readonly _existingGroups    = signal<GroupedFeeStructure[]>([]);
  protected readonly duplicateGroup   = signal<GroupedFeeStructure | null>(null);

  /** Show fee items only when all criteria are selected and no duplicate exists, or always in edit mode. */
  protected readonly showFeeItems = computed(
    () => this.isEditMode() || (this._allCriteriaFilled() && !this.duplicateGroup())
  );

  readonly quotaOptions: { value: Quota; label: string }[] = [
    { value: 'MANAGEMENT',  label: 'Management Quota' },
    { value: 'COUNSELLING', label: 'Counselling Quota' },
  ];

  readonly genderOptions: { value: Gender; label: string }[] = [
    { value: 'FEMALE', label: 'Female' },
    { value: 'MALE',   label: 'Male' },
    { value: 'OTHER',  label: 'Other' },
  ];

  protected readonly feeTypes = [
    'TUITION', 'LABORATORY_FEE', 'CLINICAL_FEE', 'LIBRARY_FEE', 'EXAMINATION_FEE',
    'BOOK_AND_PACKET_FEE', 'UNIFORM_AND_SHOES_FEE', 'UNIVERSITY_REGISTRATION_FEE',
    'MISCELLANEOUS', 'TRANSPORT_FEE', 'HOSTEL_FEE',
  ];

  protected readonly genericFeeTypes = [
    'TUITION', 'LABORATORY_FEE', 'CLINICAL_FEE', 'LIBRARY_FEE', 'EXAMINATION_FEE',
    'BOOK_AND_PACKET_FEE', 'UNIFORM_AND_SHOES_FEE', 'UNIVERSITY_REGISTRATION_FEE',
    'MISCELLANEOUS',
  ];

  protected readonly courseFeeTypes = [
    'TUITION', 'LABORATORY_FEE', 'CLINICAL_FEE', 'LIBRARY_FEE', 'EXAMINATION_FEE',
    'BOOK_AND_PACKET_FEE', 'UNIFORM_AND_SHOES_FEE', 'UNIVERSITY_REGISTRATION_FEE',
    'MISCELLANEOUS', 'TRANSPORT_FEE',
  ];

  protected readonly additionalFeeTypes = ['HOSTEL_FEE'];

  protected readonly feeTypeMeta: Record<string, { label: string; icon: string }> = {
    TUITION:                    { label: 'Tuition Fee',               icon: 'school' },
    LABORATORY_FEE:             { label: 'Laboratory Fee',            icon: 'science' },
    CLINICAL_FEE:               { label: 'Clinical Fee',              icon: 'medical_services' },
    LIBRARY_FEE:                { label: 'Library Fee',               icon: 'menu_book' },
    EXAMINATION_FEE:            { label: 'Examination Fee',           icon: 'assignment' },
    BOOK_AND_PACKET_FEE:        { label: 'Book & Packet Fee',         icon: 'import_contacts' },
    UNIFORM_AND_SHOES_FEE:      { label: 'Uniform & Shoes Fee',       icon: 'checkroom' },
    UNIVERSITY_REGISTRATION_FEE:{ label: 'University Registration Fee', icon: 'how_to_reg' },
    HOSTEL_FEE:                 { label: 'Hostel Fee',                icon: 'hotel' },
    TRANSPORT_FEE:              { label: 'Transport Fee',             icon: 'directions_bus' },
    MISCELLANEOUS:              { label: 'Miscellaneous',             icon: 'category' },
  };

  protected readonly bulkForm: FormGroup = this.fb.group({
    academicYearId: [null as number | null, Validators.required],
    programId:      [{ value: null as number | null, disabled: true }, Validators.required],
    courseId:       [{ value: null as number | null, disabled: true }],
    quota:      ['MANAGEMENT' as Quota, Validators.required],
    feeStateId: [null as number | null, Validators.required],
    gender:     ['FEMALE' as Gender, Validators.required],
    items: this.fb.array([]),
  });

  get feeItems(): FormArray { return this.bulkForm.get('items') as FormArray; }

  /** All quota×state×gender combos that will be created/skipped on replicate. */
  protected readonly replicationPreview = computed(() => {
    this._dimensionVersion(); // track dimension changes
    if (this.isEditMode()) return { toCreate: [], toSkip: [] };
    if (!this.replicateAllGenders() && !this.replicateAllStates() && !this.replicateAllQuotas()) {
      return { toCreate: [], toSkip: [] };
    }
    const v = this.bulkForm.getRawValue();
    if (!v.programId || !v.academicYearId || !v.quota || !v.feeStateId || !v.gender) {
      return { toCreate: [], toSkip: [] };
    }
    return this._computeReplicationTargets(v.quota, v.feeStateId, v.gender);
  });

  private readonly _grandTotalVersion = signal(0);

  protected readonly grandTotal = computed(() => {
    this._grandTotalVersion();
    let total = 0;
    for (let i = 0; i < this.feeItems.length; i++) {
      const ig = this.feeItems.at(i) as FormGroup;
      if (!this.courseFeeTypes.includes(ig.get('feeType')?.value)) continue;
      const ya = ig.get('yearAmounts') as FormArray;
      if (ya?.length > 0) {
        for (let j = 0; j < ya.length; j++) total += Number(ya.at(j).get('amount')?.value) || 0;
      } else { total += Number(ig.get('amount')?.value) || 0; }
    }
    return total;
  });

  protected readonly additionalTotal = computed(() => {
    this._grandTotalVersion();
    let total = 0;
    for (let i = 0; i < this.feeItems.length; i++) {
      const ig = this.feeItems.at(i) as FormGroup;
      if (!this.additionalFeeTypes.includes(ig.get('feeType')?.value)) continue;
      const ya = ig.get('yearAmounts') as FormArray;
      if (ya?.length > 0) {
        for (let j = 0; j < ya.length; j++) total += Number(ya.at(j).get('amount')?.value) || 0;
      } else { total += Number(ig.get('amount')?.value) || 0; }
    }
    return total;
  });

  protected readonly hostelerTotal    = computed(() => this.grandTotal() + this.additionalTotal());
  protected readonly hostelerYearTotals = computed(() =>
    this.yearTotals().map((yt, i) => yt + (this.additionalYearTotals()[i] ?? 0))
  );

  protected getItemGroup(i: number): FormGroup     { return this.feeItems.at(i) as FormGroup; }
  protected getItemYearAmounts(i: number): FormArray {
    return (this.feeItems.at(i) as FormGroup).get('yearAmounts') as FormArray;
  }

  private readonly _expandedNotes = signal<Set<number>>(new Set<number>());

  protected readonly yearRange = computed(() => {
    const d = this.selectedProgramDuration();
    return d > 1 ? Array.from({ length: d }, (_, i) => i + 1) : [1];
  });

  protected readonly gridTemplateColumns = computed(() => {
    const cols = Math.max(this.selectedProgramDuration(), 1);
    return `minmax(180px, 220px) repeat(${cols}, minmax(96px, 1fr)) 110px`;
  });

  protected readonly yearTotals = computed(() => {
    this._grandTotalVersion();
    const cols = Math.max(this.selectedProgramDuration(), 1);
    const totals = new Array<number>(cols).fill(0);
    for (let i = 0; i < this.feeItems.length; i++) {
      const ig = this.feeItems.at(i) as FormGroup;
      if (!this.courseFeeTypes.includes(ig.get('feeType')?.value as string)) continue;
      const ya = ig.get('yearAmounts') as FormArray;
      if (ya?.length > 0) {
        for (let j = 0; j < Math.min(ya.length, cols); j++) totals[j] += Number(ya.at(j).get('amount')?.value) || 0;
      } else { totals[0] += Number(ig.get('amount')?.value) || 0; }
    }
    return totals;
  });

  protected readonly additionalYearTotals = computed(() => {
    this._grandTotalVersion();
    const cols = Math.max(this.selectedProgramDuration(), 1);
    const totals = new Array<number>(cols).fill(0);
    for (let i = 0; i < this.feeItems.length; i++) {
      const ig = this.feeItems.at(i) as FormGroup;
      if (!this.additionalFeeTypes.includes(ig.get('feeType')?.value as string)) continue;
      const ya = ig.get('yearAmounts') as FormArray;
      if (ya?.length > 0) {
        for (let j = 0; j < Math.min(ya.length, cols); j++) totals[j] += Number(ya.at(j).get('amount')?.value) || 0;
      } else { totals[0] += Number(ig.get('amount')?.value) || 0; }
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
  protected isNoteExpanded(i: number): boolean { return this._expandedNotes().has(i); }

  protected getItemRowTotal(i: number): number {
    const ig = this.feeItems.at(i) as FormGroup;
    const ya = ig.get('yearAmounts') as FormArray;
    if (ya?.length > 0) {
      let t = 0;
      for (let j = 0; j < ya.length; j++) t += Number(ya.at(j).get('amount')?.value) || 0;
      return t;
    }
    return Number(ig.get('amount')?.value) || 0;
  }

  // ── Lifecycle ──────────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.tourService.register('fee-structure-form', FEE_STRUCTURE_FORM_TOUR);

    // Load lookups in parallel
    this.http.get<AcademicYear[]>(`${environment.apiUrl}/academic-years`).subscribe({ next: d => this.academicYears.set(d) });
    this.http.get<Program[]>(`${environment.apiUrl}/programs`).subscribe({ next: d => this.programs.set(d) });
    this.financeService.getFeeStates().subscribe({
      next: states => {
        this.feeStates.set(states);
        const defaultState = states.find(s => s.isDefault) ?? states[0];
        if (defaultState && !this.bulkForm.get('feeStateId')?.value) {
          this.bulkForm.patchValue({ feeStateId: defaultState.id });
        }
      },
    });

    const qp = this.route.snapshot.queryParamMap;
    const programId     = qp.get('programId');
    const academicYearId = qp.get('academicYearId');
    const courseId      = qp.get('courseId');
    const quota      = qp.get('quota');
    const feeStateId = qp.get('feeStateId');
    const gender     = qp.get('gender');

    if (programId && academicYearId && quota && feeStateId && gender) {
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Fee Structures');
      this.loading.set(true);

      const pId  = Number(programId);
      const ayId = Number(academicYearId);
      const cId  = courseId ? Number(courseId) : undefined;

      this.bulkForm.patchValue({
        programId: pId,
        academicYearId: ayId,
        courseId: cId ?? null,
        quota,
        feeStateId: Number(feeStateId),
        gender,
      });
      this.bulkForm.disable();

      this.http.get<Course[]>(`${environment.apiUrl}/courses/program/${pId}`).subscribe({ next: d => this.courses.set(d) });

      const program = this.programs().find(p => p.id === pId);
      if (program) this.selectedProgramDuration.set(program.durationYears);

      this.http.get<{ count: number }>(`${environment.apiUrl}/fee-structures/finalized-count`, {
        params: { programId: pId, quota: quota!, feeStateId: Number(feeStateId), gender: gender! },
      }).subscribe({ next: r => this.finalizedCount.set(r.count) });

      this.financeService.getGroupedFeeStructures({ programId: pId, academicYearId: ayId, courseId: cId }).subscribe({
        next: groups => {
          const group = groups.find(g =>
            g.quota === quota && g.feeStateId === Number(feeStateId) &&
            g.gender === gender
          ) ?? null;

          const program = this.programs().find(p => p.id === pId);
          if (program && !this.selectedProgramDuration()) this.selectedProgramDuration.set(program.durationYears);
          const duration = this.selectedProgramDuration();

          this.feeItems.clear();
          const existingTypes = new Set<string>();

          if (group) {
            for (const item of group.items) {
              existingTypes.add(item.feeType);
              const g = this.fb.group({
                feeType: [item.feeType, Validators.required],
                amount: [item.amount, [Validators.min(0)]],
                description: [item.description || ''],
                yearAmounts: this.fb.array([]),
              });
              if (item.yearAmounts?.length > 0) {
                const ya = g.get('yearAmounts') as FormArray;
                for (const y of item.yearAmounts) {
                  ya.push(this.fb.group({ yearNumber: [y.yearNumber], yearLabel: [y.yearLabel], amount: [y.amount, [Validators.min(0)]] }));
                }
              }
              this.feeItems.push(g);
            }
          }

          for (const ft of this.feeTypes) {
            if (!existingTypes.has(ft)) {
              const g = this.fb.group({
                feeType: [ft, Validators.required],
                amount: [0, [Validators.min(0)]],
                description: [''],
                yearAmounts: this.fb.array([]),
              });
              if (duration > 1) {
                const ya = g.get('yearAmounts') as FormArray;
                for (let i = 1; i <= duration; i++) {
                  ya.push(this.fb.group({ yearNumber: [i], yearLabel: [`Year ${i}`], amount: [0, [Validators.min(0)]] }));
                }
              }
              this.feeItems.push(g);
            }
          }
          this._grandTotalVersion.update(v => v + 1);
          this.loading.set(false);
        },
        error: () => { this.toast.error('Failed to load fee structures'); void this.router.navigate(['/fee-structures']); },
      });
    } else {
      // Create mode — pre-populate all fee type rows
      for (const ft of this.feeTypes) this.addItemWithType(ft);
      // Load existing groups once for in-memory duplicate detection
      this.financeService.getGroupedFeeStructures().subscribe({
        next: groups => this._existingGroups.set(groups),
      });
    }
  }

  // ── Criteria change handlers ───────────────────────────────────────────────

  protected onAcademicYearChange(yearId: number | null): void {
    this.bulkForm.patchValue({ programId: null, courseId: null });
    this.bulkForm.get('courseId')?.disable();
    if (yearId) this.bulkForm.get('programId')?.enable();
    else        this.bulkForm.get('programId')?.disable();
    this.courses.set([]);
    this.selectedProgramDuration.set(0);
    this._allCriteriaFilled.set(false);
    this.clearAllItemYearAmounts();
  }

  protected onProgramChange(programId: number | null): void {
    this.bulkForm.patchValue({ courseId: null });
    this.bulkForm.get('courseId')?.disable();
    this.courses.set([]);
    this._allCriteriaFilled.set(false);
    this.clearAllItemYearAmounts();

    if (programId) {
      const program = this.programs().find(p => p.id === programId);
      const duration = program?.durationYears ?? 0;
      this.selectedProgramDuration.set(duration);
      this.rebuildAllItemYearAmounts(duration);

      this.http.get<Course[]>(`${environment.apiUrl}/courses/program/${programId}`).subscribe({
        next: data => {
          this.courses.set(data);
          if (data.length > 0) this.bulkForm.get('courseId')?.enable();
          else this._checkAllCriteria();
        },
      });
    } else {
      this.selectedProgramDuration.set(0);
    }
  }

  protected onCourseChange(_courseId: number): void {
    this.clearAllItemYearAmounts();
    const d = this.selectedProgramDuration();
    if (d > 1) this.rebuildAllItemYearAmounts(d);
    this._checkAllCriteria();
    this._grandTotalVersion.update(v => v + 1);
  }

  protected onDimensionChange(): void {
    this._checkAllCriteria();
  }

  private _checkAllCriteria(): void {
    const v = this.bulkForm.getRawValue();
    const hasCourse = this.courses().length === 0 || v.courseId !== null;
    const filled =
      !!v.academicYearId && !!v.programId && hasCourse &&
      !!v.quota && !!v.feeStateId && !!v.gender;
    this._allCriteriaFilled.set(filled);
    this._dimensionVersion.update(n => n + 1);

    if (filled && !this.isEditMode()) {
      const dup = this._existingGroups().find(g =>
        g.programId      === v.programId &&
        g.academicYearId === v.academicYearId &&
        g.quota          === v.quota &&
        g.feeStateId     === v.feeStateId &&
        g.gender         === v.gender &&
        (v.courseId ? g.courseId === v.courseId : g.courseId === null)
      ) ?? null;
      this.duplicateGroup.set(dup);
    } else {
      this.duplicateGroup.set(null);
    }
  }

  // ── Amount change helpers ──────────────────────────────────────────────────

  protected onItemYearAmountChange(itemIndex: number): void {
    const ya = this.getItemYearAmounts(itemIndex);
    let total = 0;
    for (let j = 0; j < ya.length; j++) total += Number(ya.at(j).get('amount')?.value) || 0;
    (this.feeItems.at(itemIndex) as FormGroup).patchValue({ amount: total }, { emitEvent: false });
    this._grandTotalVersion.update(v => v + 1);
  }

  protected onItemAmountChange(): void { this._grandTotalVersion.update(v => v + 1); }

  private clearAllItemYearAmounts(): void {
    for (let i = 0; i < this.feeItems.length; i++) {
      const ya = (this.feeItems.at(i) as FormGroup).get('yearAmounts') as FormArray;
      ya.clear();
      (this.feeItems.at(i) as FormGroup).patchValue({ amount: 0 }, { emitEvent: false });
    }
  }

  private rebuildAllItemYearAmounts(duration: number): void {
    for (let i = 0; i < this.feeItems.length; i++) this.buildYearAmountsForItem(i, duration);
  }

  private buildYearAmountsForItem(itemIndex: number, duration: number): void {
    const ya = (this.feeItems.at(itemIndex) as FormGroup).get('yearAmounts') as FormArray;
    ya.clear();
    for (let i = 1; i <= duration; i++) {
      ya.push(this.fb.group({ yearNumber: [i], yearLabel: [`Year ${i}`], amount: [0, [Validators.min(0)]] }));
    }
  }

  private addItemWithType(feeType: string): void {
    const g = this.fb.group({
      feeType: [feeType, Validators.required],
      amount: [0, [Validators.min(0)]],
      description: [''],
      yearAmounts: this.fb.array([]),
    });
    this.feeItems.push(g);
    const d = this.selectedProgramDuration();
    if (d > 1) this.buildYearAmountsForItem(this.feeItems.length - 1, d);
    this._grandTotalVersion.update(v => v + 1);
  }

  // ── Replication helpers ────────────────────────────────────────────────────

  protected onReplicateChange(): void {
    // trigger recompute of preview
  }

  private _computeReplicationTargets(
    primaryQuota: string, primaryStateId: number, primaryGender: string
  ): { toCreate: ReplicationTarget[]; toSkip: ReplicationTarget[] } {
    const allQuotas  = this.replicateAllQuotas()  ? ['MANAGEMENT', 'COUNSELLING'] : [primaryQuota];
    const allStateIds = this.replicateAllStates()
      ? this.feeStates().map(s => s.id)
      : [primaryStateId];
    const allGenders = this.replicateAllGenders()
      ? (['MALE', 'FEMALE', 'OTHER'] as Gender[])
      : [primaryGender as Gender];

    const v = this.bulkForm.getRawValue();
    const toCreate: ReplicationTarget[] = [];
    const toSkip:   ReplicationTarget[] = [];

    for (const quota of allQuotas) {
      for (const stateId of allStateIds) {
        const state = this.feeStates().find(s => s.id === stateId);
        if (!state) continue;
        // Counselling quota is not applicable for fallback (Other State)
        if (quota === 'COUNSELLING' && state.isFallback) continue;
        for (const gender of allGenders) {
          // Skip the primary combination itself
          if (quota === primaryQuota && stateId === primaryStateId && gender === primaryGender) continue;
          const target: ReplicationTarget = {
            quota: quota as 'MANAGEMENT' | 'COUNSELLING',
            feeStateId: stateId,
            feeStateName: state.name,
            gender,
          };
          const exists = this._existingGroups().some(g =>
            g.programId      === v.programId &&
            g.academicYearId === v.academicYearId &&
            g.quota          === quota &&
            g.feeStateId     === stateId &&
            g.gender         === gender &&
            (v.courseId ? g.courseId === v.courseId : g.courseId === null)
          );
          if (exists) toSkip.push(target);
          else        toCreate.push(target);
        }
      }
    }
    return { toCreate, toSkip };
  }

  protected genderLabel(g: string): string {
    return g === 'FEMALE' ? 'Female' : g === 'MALE' ? 'Male' : 'Other';
  }

  protected quotaLabel(q: string): string {
    return q === 'MANAGEMENT' ? 'Management' : 'Counselling';
  }

  // ── Submit ─────────────────────────────────────────────────────────────────

  protected onSubmit(): void {
    const rawForm = this.bulkForm.getRawValue();

    if (!rawForm.academicYearId || !rawForm.programId || !rawForm.quota ||
        !rawForm.feeStateId || !rawForm.gender) {
      this.toast.warning('Please fill in all required criteria before saving');
      scrollToFirstInvalid(this.bulkForm);
      return;
    }

    if (this.feeItems.length === 0) {
      this.toast.warning('Add at least one fee item');
      return;
    }

    if (this.showReasonField() && !this.reasonCtrl.value?.trim()) {
      this.toast.warning('A reason is required — students are already finalized against this fee structure');
      return;
    }

    const rv = rawForm;
    const normalizedItems: NormalizedFeeStructureItemValue[] = (rv.items as RawFeeStructureItemValue[]).map(item => {
      if (item.yearAmounts?.length > 0) {
        const yearAmounts = item.yearAmounts.map(ya => ({ ...ya, amount: Number(ya.amount) || 0 }));
        const amount = yearAmounts.reduce((s, ya) => s + ya.amount, 0);
        return { feeType: item.feeType, amount, description: item.description, yearAmounts };
      }
      return { feeType: item.feeType, amount: Number(item.amount) || 0, description: item.description };
    });

    const nonZeroItems = normalizedItems.filter(item => item.amount > 0);

    const totalFee = nonZeroItems
      .filter(item => this.genericFeeTypes.includes(item.feeType))
      .reduce((sum, item) => {
        if (item.yearAmounts && item.yearAmounts.length > 0) return sum + item.yearAmounts.reduce((s, ya) => s + ya.amount, 0);
        return sum + item.amount;
      }, 0);

    if (totalFee === 0) { this.toast.warning('Total course fee must be greater than zero'); return; }

    const items: FeeStructureItemRequest[] = nonZeroItems.map(item => ({
      feeType: item.feeType,
      amount: item.amount,
      description: item.description || undefined,
      yearAmounts: item.yearAmounts?.map(ya => ({
        yearNumber: ya.yearNumber, yearLabel: ya.yearLabel, amount: Number(ya.amount) || 0,
      } as YearAmountRequest)),
    }));

    const request: BulkFeeStructureRequest = {
      programId: rv.programId,
      academicYearId: rv.academicYearId,
      courseId: rv.courseId || undefined,
      quota: rv.quota as 'MANAGEMENT' | 'COUNSELLING',
      feeStateId: rv.feeStateId,
      gender: rv.gender as 'MALE' | 'FEMALE' | 'OTHER',
      items,
      reason: this.reasonCtrl.value?.trim() || undefined,
    };

    this.saving.set(true);

    const op$ = this.isEditMode()
      ? this.financeService.bulkUpdateFeeStructures(request)
      : this.financeService.bulkCreateFeeStructures(request);

    op$.subscribe({
      next: () => {
        if (this.isEditMode()) {
          this.toast.success('Updated successfully');
          void this.router.navigate(['/fee-structures']);
          return;
        }

        // After primary save, create replicated combinations
        const { toCreate } = this.replicationPreview();
        if (toCreate.length === 0) {
          this.toast.success('Fee structure saved');
          void this.router.navigate(['/fee-structures']);
          return;
        }

        this._saveReplicationTargets(request, toCreate);
      },
      error: err => {
        this.toast.error(err?.error?.message ?? (this.isEditMode() ? 'Failed to update' : 'Failed to save'));
        this.saving.set(false);
      },
    });
  }

  private _saveReplicationTargets(
    primaryRequest: BulkFeeStructureRequest,
    targets: ReplicationTarget[]
  ): void {
    const requests = targets.map(t => ({
      ...primaryRequest,
      quota: t.quota,
      feeStateId: t.feeStateId,
      gender: t.gender,
    }));

    let completed = 0;
    let failed    = 0;

    const tryNext = (index: number): void => {
      if (index >= requests.length) {
        const skipped = this.replicationPreview().toSkip.length;
        const parts: string[] = [`Primary + ${completed} combination(s) saved`];
        if (skipped > 0)  parts.push(`${skipped} skipped (already exist)`);
        if (failed > 0)   parts.push(`${failed} failed`);
        this.toast.success(parts.join(' · '));
        void this.router.navigate(['/fee-structures']);
        return;
      }
      this.financeService.bulkCreateFeeStructures(requests[index]).subscribe({
        next:  () => { completed++; tryNext(index + 1); },
        error: () => { failed++;    tryNext(index + 1); },
      });
    };

    tryNext(0);
  }
}

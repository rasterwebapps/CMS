import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';
import { uniqueFieldValidator } from '../../../shared/validators/unique-field.validator';
import { environment } from '../../../../environments';
import { CurriculumVersionService } from '../curriculum-version.service';
import { AttendanceComponentType, SyllabusUnit, SyllabusUnitRequest } from '../curriculum-version.model';

const COMPONENT_TYPE_LABELS: Record<AttendanceComponentType, string> = {
  THEORY: 'Theory',
  LAB: 'Lab',
  CLINICAL: 'Clinical',
};

export interface SyllabusUnitDialogData {
  curriculumTermCourseId: number;
  subjectName: string;
  subjectCode: string;
  theoryHours: number;
  labHours: number;
  clinicalHours: number;
}

@Component({
  selector: 'app-syllabus-unit-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
  ],
  templateUrl: './syllabus-unit-dialog.component.html',
  styleUrl: './syllabus-unit-dialog.component.scss',
})
export class SyllabusUnitDialogComponent implements OnInit {
  protected readonly dialogRef = inject(MatDialogRef<SyllabusUnitDialogComponent>);
  protected readonly data: SyllabusUnitDialogData = inject(MAT_DIALOG_DATA);
  private readonly service = inject(CurriculumVersionService);
  private readonly permissionService = inject(PermissionService);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);
  private readonly dialog = inject(MatDialog);
  private readonly http = inject(HttpClient);

  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly removingId = signal<number | null>(null);
  protected readonly units = signal<SyllabusUnit[]>([]);
  protected readonly showForm = signal(false);
  protected readonly editingId = signal<number | null>(null);

  protected readonly canManage = computed(() => this.permissionService.has('SYLLABUS_UNIT_MANAGE'));

  protected readonly componentTypeLabels = COMPONENT_TYPE_LABELS;

  /** Only offer a component type the subject actually has hours declared for -- falls back to
   *  the full list if the subject has zero hours in every bucket (nothing to allocate against
   *  yet, so no basis to narrow the choice). */
  protected readonly availableComponentTypes = computed<AttendanceComponentType[]>(() => {
    const all: AttendanceComponentType[] = ['THEORY', 'LAB', 'CLINICAL'];
    const withHours = all.filter((type) => this.bucketTotal(type) > 0);
    return withHours.length ? withHours : all;
  });

  /** Hours already allocated to each bucket across every unit, for the inline "X / Yh" hint --
   *  matches the same per-type sum the backend enforces. */
  protected readonly allocatedHoursByType = computed<Record<AttendanceComponentType, number>>(() => {
    const totals: Record<AttendanceComponentType, number> = { THEORY: 0, LAB: 0, CLINICAL: 0 };
    for (const unit of this.units()) {
      totals[unit.componentType] += unit.plannedHours ?? 0;
    }
    return totals;
  });

  protected readonly form: FormGroup = this.fb.group({
    unitNumber: [null, [Validators.required, Validators.min(1)]],
    title: ['', [Validators.required]],
    componentType: ['THEORY' as AttendanceComponentType, [Validators.required]],
    plannedHours: [null, [Validators.min(0)]],
    description: [''],
    sortOrder: [null],
  });

  protected bucketTotal(type: AttendanceComponentType): number {
    return type === 'THEORY' ? this.data.theoryHours
      : type === 'LAB' ? this.data.labHours
      : this.data.clinicalHours;
  }

  ngOnInit(): void {
    this.form.get('unitNumber')?.setAsyncValidators(
      uniqueFieldValidator(
        this.http,
        `${environment.apiUrl}/syllabus-units/unit-number-exists`,
        () => this.editingId(),
        () => ({ curriculumTermCourseId: this.data.curriculumTermCourseId }),
      ),
    );
    this.loadUnits();
  }

  private loadUnits(): void {
    this.loading.set(true);
    this.service.getSyllabusUnits(this.data.curriculumTermCourseId).subscribe({
      next: (units) => {
        this.units.set(units);
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load syllabus units');
        this.loading.set(false);
      },
    });
  }

  protected openAddForm(): void {
    this.editingId.set(null);
    this.form.reset({
      unitNumber: this.nextUnitNumber(),
      title: '',
      componentType: this.availableComponentTypes()[0],
      plannedHours: null,
      description: '',
      sortOrder: null,
    });
    this.showForm.set(true);
  }

  protected openEditForm(unit: SyllabusUnit): void {
    this.editingId.set(unit.id);
    this.form.reset({
      unitNumber: unit.unitNumber,
      title: unit.title,
      componentType: unit.componentType,
      plannedHours: unit.plannedHours,
      description: unit.description,
      sortOrder: unit.sortOrder,
    });
    this.showForm.set(true);
  }

  protected cancelForm(): void {
    this.showForm.set(false);
    this.editingId.set(null);
  }

  private nextUnitNumber(): number {
    const existing = this.units().map((u) => u.unitNumber);
    return existing.length ? Math.max(...existing) + 1 : 1;
  }

  protected submit(): void {
    if (this.form.invalid || this.form.pending) {
      scrollToFirstInvalid(this.form);
      return;
    }
    const v = this.form.getRawValue();
    const request: SyllabusUnitRequest = {
      curriculumTermCourseId: this.data.curriculumTermCourseId,
      unitNumber: v.unitNumber,
      title: (v.title ?? '').trim(),
      componentType: v.componentType,
      plannedHours: v.plannedHours,
      description: v.description || null,
      sortOrder: v.sortOrder,
    };
    const editingId = this.editingId();
    this.saving.set(true);
    const call = editingId
      ? this.service.updateSyllabusUnit(editingId, request)
      : this.service.createSyllabusUnit(request);
    call.subscribe({
      next: () => {
        this.toast.success(editingId ? 'Unit updated' : 'Unit added');
        this.saving.set(false);
        this.cancelForm();
        this.loadUnits();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? (editingId ? 'Failed to update unit' : 'Failed to add unit'));
        this.saving.set(false);
      },
    });
  }

  protected confirmDelete(unit: SyllabusUnit): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Remove Unit',
        message: `Remove Unit ${unit.unitNumber} — "${unit.title}"? This cannot be undone.`,
        confirmText: 'Remove',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) this.removeUnit(unit.id);
    });
  }

  private removeUnit(id: number): void {
    this.removingId.set(id);
    this.service.deleteSyllabusUnit(id).subscribe({
      next: () => {
        this.toast.success('Unit removed');
        this.removingId.set(null);
        this.loadUnits();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to remove unit');
        this.removingId.set(null);
      },
    });
  }

  protected close(): void {
    this.dialogRef.close();
  }
}

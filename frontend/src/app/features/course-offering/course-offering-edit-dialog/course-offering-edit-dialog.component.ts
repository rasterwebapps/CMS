import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CourseOffering, CourseOfferingUpdateRequest } from '../../academic-year/academic-year.model';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { ToastService } from '../../../core/toast/toast.service';

export interface FacultyOption {
  id: number;
  name: string;
  specialityId: number | null;
}

export interface CourseOfferingEditDialogData {
  offering: CourseOffering;
  facultyOptions: FacultyOption[];
}

@Component({
  selector: 'app-course-offering-edit-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './course-offering-edit-dialog.component.html',
  styleUrl: './course-offering-edit-dialog.component.scss',
})
export class CourseOfferingEditDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<CourseOfferingEditDialogComponent>);
  protected readonly data: CourseOfferingEditDialogData = inject(MAT_DIALOG_DATA);
  private readonly academicYearService = inject(AcademicYearService);
  private readonly toast = inject(ToastService);

  protected readonly saving = signal(false);

  /** Faculty must belong to the subject's own department (Speciality) to be assignable — the
   *  faculty already on this offering stays visible/selectable even if it predates the rule
   *  (grandfathered), so an admin editing just the section label doesn't lose their current
   *  faculty from the list. No restriction at all when the subject has no speciality set. */
  protected readonly eligibleFacultyOptions: FacultyOption[] = (() => {
    const specialityId = this.data.offering.subjectSpecialityId;
    if (!specialityId) return this.data.facultyOptions;
    return this.data.facultyOptions.filter((f) =>
      f.specialityId === specialityId || f.id === this.data.offering.facultyId);
  })();

  protected readonly form: FormGroup = this.fb.group({
    facultyId: [this.data.offering.facultyId],
    sectionLabel: [this.data.offering.sectionLabel ?? ''],
  });

  protected onSubmit(): void {
    const v = this.form.value;
    const request: CourseOfferingUpdateRequest = {
      facultyId: v.facultyId ?? null,
      sectionLabel: (v.sectionLabel ?? '').trim() || null,
    };

    this.saving.set(true);
    this.academicYearService.updateCourseOffering(this.data.offering.id, request).subscribe({
      next: (updated) => {
        this.saving.set(false);
        this.toast.success('Course offering updated');
        this.dialogRef.close(updated);
      },
      error: (err) => {
        this.saving.set(false);
        this.toast.error(err?.error?.message ?? 'Failed to update course offering');
      },
    });
  }

  protected onCancel(): void {
    this.dialogRef.close();
  }
}

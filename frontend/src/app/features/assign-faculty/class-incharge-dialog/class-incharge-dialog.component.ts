import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ClassInchargeAssignment } from '../../academic-year/academic-year.model';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';
import { FacultyOption } from '../../course-offering/course-offering-edit-dialog/course-offering-edit-dialog.component';

export interface ClassInchargeDialogData {
  termInstanceId: number;
  termLabel: string;
  facultyOptions: FacultyOption[];
}

/** Class Teacher / Class Incharge assignment for every committed CohortSection in one term,
 *  across all cohorts — structurally distinct from the per-offering Section Faculty panel (which
 *  lives inside CourseOfferingEditDialogComponent) since a class incharge isn't tied to any one
 *  subject, so this is a standalone top-level action on Assign Faculty rather than nested in a
 *  row's edit dialog. */
@Component({
  selector: 'app-class-incharge-dialog',
  standalone: true,
  imports: [FormsModule, MatDialogModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './class-incharge-dialog.component.html',
  styleUrl: './class-incharge-dialog.component.scss',
})
export class ClassInchargeDialogComponent implements OnInit {
  private readonly dialogRef = inject(MatDialogRef<ClassInchargeDialogComponent>);
  protected readonly data: ClassInchargeDialogData = inject(MAT_DIALOG_DATA);
  private readonly academicYearService = inject(AcademicYearService);
  private readonly permissionService = inject(PermissionService);
  private readonly toast = inject(ToastService);

  protected readonly loading = signal(true);
  protected readonly sections = signal<ClassInchargeAssignment[]>([]);
  protected readonly savingId = signal<number | null>(null);
  protected readonly canManage = computed(() => this.permissionService.has('CLASS_INCHARGE_MANAGE'));

  ngOnInit(): void {
    this.academicYearService.getClassIncharge(this.data.termInstanceId).subscribe({
      next: (rows) => { this.sections.set(rows); this.loading.set(false); },
      error: () => {
        this.toast.error('Failed to load class sections');
        this.loading.set(false);
      },
    });
  }

  protected onFacultyChange(row: ClassInchargeAssignment, facultyId: number | null): void {
    this.savingId.set(row.cohortSectionId);
    this.academicYearService.updateClassIncharge(row.cohortSectionId, facultyId).subscribe({
      next: (updated) => {
        this.savingId.set(null);
        this.sections.update((rows) =>
          rows.map((r) => (r.cohortSectionId === updated.cohortSectionId ? updated : r)));
        this.toast.success(`${row.cohortName} — ${row.sectionLabel} updated`);
      },
      error: (err) => {
        this.savingId.set(null);
        this.toast.error(err?.error?.message ?? 'Failed to update class incharge');
      },
    });
  }

  protected onClose(): void {
    this.dialogRef.close();
  }
}

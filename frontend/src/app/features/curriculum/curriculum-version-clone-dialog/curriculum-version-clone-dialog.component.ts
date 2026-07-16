import { Component, OnInit, inject, input, output } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CmsFlyoutPanelComponent } from '../../../shared/flyout-panel/flyout-panel.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { ToastService } from '../../../core/toast/toast.service';
import { environment } from '../../../../environments';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { CurriculumVersionService } from '../curriculum-version.service';
import { CurriculumVersion } from '../curriculum-version.model';
import { uniqueFieldValidator } from '../../../shared/validators/unique-field.validator';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';

export interface CurriculumVersionCloneDialogData {
  source: CurriculumVersion;
}

@Component({
  selector: 'app-curriculum-version-clone-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatProgressSpinnerModule, CmsFlyoutPanelComponent, CmsStatusBadgeComponent],
  templateUrl: './curriculum-version-clone-dialog.component.html',
  styleUrl: './curriculum-version-clone-dialog.component.scss',
})
export class CurriculumVersionCloneDialogComponent implements OnInit {
  readonly data = input.required<CurriculumVersionCloneDialogData>();
  readonly closed = output<CurriculumVersion | undefined>();

  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);
  private readonly academicYearService = inject(AcademicYearService);
  private readonly curriculumVersionService = inject(CurriculumVersionService);

  protected academicYears: { id: number; name: string }[] = [];
  protected submitting = false;

  protected form: FormGroup = this.fb.group({
    newVersionName: ['', Validators.required],
    newEffectiveAcademicYearId: [null, Validators.required],
  });

  ngOnInit(): void {
    const source = this.data().source;
    this.form.patchValue({ newVersionName: `${source.versionName} (Copy)` });
    this.form.get('newVersionName')?.setAsyncValidators(
      uniqueFieldValidator(this.http, `${environment.apiUrl}/curriculum-versions/name-exists`, () => null,
        () => ({ programId: source.programId, courseId: source.courseId })),
    );

    this.academicYearService.getAllAcademicYears().subscribe({
      next: (years) => { this.academicYears = years; },
      error: () => this.toast.error('Failed to load academic years'),
    });
  }

  protected submit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }
    const source = this.data().source;
    const { newVersionName, newEffectiveAcademicYearId } = this.form.value;
    this.submitting = true;
    this.curriculumVersionService.clone(source.id, newVersionName, newEffectiveAcademicYearId).subscribe({
      next: (cloned) => {
        this.toast.success('Curriculum version cloned');
        this.submitting = false;
        this.closed.emit(cloned);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to clone version');
        this.submitting = false;
      },
    });
  }

  protected cancel(): void {
    this.closed.emit(undefined);
  }
}

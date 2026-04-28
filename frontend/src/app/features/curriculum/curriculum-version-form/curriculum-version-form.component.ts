import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CurriculumVersionService } from '../curriculum-version.service';
import { CurriculumVersionRequest } from '../curriculum-version.model';
import { ToastService } from '../../../core/toast/toast.service';
import { environment } from '../../../../environments';

@Component({
  selector: 'app-curriculum-version-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './curriculum-version-form.component.html',
  styleUrl: './curriculum-version-form.component.scss',
})
export class CurriculumVersionFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(CurriculumVersionService);
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('New Curriculum Version');
  protected readonly programs = signal<{ id: number; name: string; code: string }[]>([]);
  protected readonly academicYears = signal<{ id: number; name: string }[]>([]);

  private versionId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    programId: [null, Validators.required],
    versionName: ['', [Validators.required, Validators.maxLength(100)]],
    effectiveFromAcademicYearId: [null, Validators.required],
    isActive: [true],
  });

  ngOnInit(): void {
    this.http.get<{ id: number; name: string; code: string }[]>(`${environment.apiUrl}/programs`)
      .subscribe({ next: (data) => this.programs.set(data) });

    this.http.get<{ id: number; name: string }[]>(`${environment.apiUrl}/academic-years`)
      .subscribe({ next: (data) => this.academicYears.set(data) });

    const programId = this.route.snapshot.queryParamMap.get('programId');
    if (programId) {
      this.form.patchValue({ programId: Number(programId) });
    }

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.versionId = Number(id);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Curriculum Version');
      this.loadVersion();
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.value;
    const request: CurriculumVersionRequest = {
      programId: v.programId,
      versionName: v.versionName.trim(),
      effectiveFromAcademicYearId: v.effectiveFromAcademicYearId,
      isActive: v.isActive,
    };
    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.service.update(this.versionId!, request)
      : this.service.create(request);
    op$.subscribe({
      next: (result) => {
        this.toast.success(this.isEditMode() ? 'Updated successfully' : 'Created successfully');
        void this.router.navigate(['/curriculum-versions'], {
          queryParams: { programId: result.programId }
        });
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to save');
        this.saving.set(false);
      },
    });
  }

  private loadVersion(): void {
    if (!this.versionId) return;
    this.loading.set(true);
    this.service.getById(this.versionId).subscribe({
      next: (v) => {
        this.form.patchValue({
          programId: v.programId,
          versionName: v.versionName,
          effectiveFromAcademicYearId: v.effectiveFromAcademicYearId,
          isActive: v.isActive,
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load version');
        void this.router.navigate(['/curriculum-versions']);
      },
    });
  }
}

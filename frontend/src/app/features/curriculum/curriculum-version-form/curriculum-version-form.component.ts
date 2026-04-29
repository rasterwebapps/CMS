import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpClient } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CurriculumVersionService } from '../curriculum-version.service';
import { CurriculumVersionRequest } from '../curriculum-version.model';
import { ToastService } from '../../../core/toast/toast.service';
import { environment } from '../../../../environments';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { CURRICULUM_VERSION_FORM_TOUR } from '../../../shared/tour/tours/curriculum-version.tours';
import { CmsPreviewCardComponent } from '../../../shared/preview-card/preview-card.component';
import { CmsTipsCardComponent, CmsTip } from '../../../shared/tips-card/tips-card.component';

@Component({
  selector: 'app-curriculum-version-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    CmsTourButtonComponent,
    CmsPreviewCardComponent,
    CmsTipsCardComponent,
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
  private readonly tourService = inject(TourService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('New Curriculum Version');
  protected readonly programs = signal<{ id: number; name: string; code: string }[]>([]);
  protected readonly academicYears = signal<{ id: number; name: string }[]>([]);

  // Preview signals
  protected readonly previewVersion   = signal('');
  protected readonly previewProgramId = signal<number | null>(null);
  protected readonly previewAyId      = signal<number | null>(null);
  protected readonly previewActive    = signal(true);
  protected readonly previewProgramName = computed(() => {
    const p = this.programs().find(x => x.id === this.previewProgramId());
    return p ? `${p.name} (${p.code})` : '';
  });
  protected readonly previewAyName = computed(() => this.academicYears().find(a => a.id === this.previewAyId())?.name ?? '');

  protected readonly TIPS: CmsTip[] = [
    { icon: 'fork_right',  title: 'Versioning',     subtitle: 'Create a new version when course structure changes — never edit a deployed version in place.' },
    { icon: 'event',       title: 'Effective from', subtitle: 'New batches starting in this academic year onwards will follow this version.' },
    { icon: 'visibility',  title: 'Active flag',    subtitle: 'Only one version per program can be Active at a time — used for enrollment.' },
  ];

  private versionId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    programId: [null, Validators.required],
    versionName: ['', [Validators.required, Validators.maxLength(100)]],
    effectiveFromAcademicYearId: [null, Validators.required],
    isActive: [true],
  });

  constructor() {
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(v => {
        this.previewVersion.set((v.versionName ?? '').trim());
        this.previewProgramId.set(v.programId ? Number(v.programId) : null);
        this.previewAyId.set(v.effectiveFromAcademicYearId ? Number(v.effectiveFromAcademicYearId) : null);
        this.previewActive.set(!!v.isActive);
      });
  }

  ngOnInit(): void {
    this.tourService.register('curriculum-version-form', CURRICULUM_VERSION_FORM_TOUR);

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

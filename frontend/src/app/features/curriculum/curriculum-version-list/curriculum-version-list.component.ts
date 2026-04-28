import { Component, inject, signal, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CurriculumVersionService } from '../curriculum-version.service';
import { CurriculumVersion } from '../curriculum-version.model';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { ToastService } from '../../../core/toast/toast.service';
import { environment } from '../../../../environments';

@Component({
  selector: 'app-curriculum-version-list',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    CmsEmptyStateComponent,
  ],
  templateUrl: './curriculum-version-list.component.html',
  styleUrl: './curriculum-version-list.component.scss',
})
export class CurriculumVersionListComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly curriculumVersionService = inject(CurriculumVersionService);
  private readonly toast = inject(ToastService);
  private readonly http = inject(HttpClient);
  private readonly fb = inject(FormBuilder);

  protected readonly loading = signal(false);
  protected readonly cloning = signal<number | null>(null);
  protected readonly versions = signal<CurriculumVersion[]>([]);
  protected readonly programs = signal<{ id: number; name: string; code: string }[]>([]);
  protected readonly selectedProgramId = signal<number | null>(null);
  protected readonly showCloneForm = signal<number | null>(null);

  protected readonly cloneForm: FormGroup = this.fb.group({
    newVersionName: ['', Validators.required],
    newEffectiveAcademicYearId: [null, Validators.required],
  });

  protected readonly academicYears = signal<{ id: number; name: string }[]>([]);

  ngOnInit(): void {
    const programIdParam = this.route.snapshot.queryParamMap.get('programId');
    if (programIdParam) {
      this.selectedProgramId.set(Number(programIdParam));
    }

    this.http.get<{ id: number; name: string; code: string }[]>(`${environment.apiUrl}/programs`)
      .subscribe({ next: (data) => { this.programs.set(data); } });

    this.http.get<{ id: number; name: string }[]>(`${environment.apiUrl}/academic-years`)
      .subscribe({ next: (data) => { this.academicYears.set(data); } });

    if (this.selectedProgramId()) {
      this.loadVersions();
    }
  }

  protected onProgramChange(event: Event): void {
    const id = Number((event.target as HTMLSelectElement).value);
    this.selectedProgramId.set(id || null);
    if (id) {
      this.loadVersions();
    } else {
      this.versions.set([]);
    }
  }

  protected navigateToNew(): void {
    const params = this.selectedProgramId() ? { queryParams: { programId: this.selectedProgramId() } } : {};
    void this.router.navigate(['/curriculum-versions/new'], params);
  }

  protected editVersion(version: CurriculumVersion): void {
    void this.router.navigate(['/curriculum-versions', version.id, 'edit']);
  }

  protected viewCurriculum(version: CurriculumVersion): void {
    void this.router.navigate(['/curriculum-map', version.id]);
  }

  protected openCloneForm(version: CurriculumVersion): void {
    this.showCloneForm.set(version.id);
    this.cloneForm.reset({ newVersionName: `${version.versionName} (Copy)`, newEffectiveAcademicYearId: null });
  }

  protected cancelClone(): void {
    this.showCloneForm.set(null);
    this.cloneForm.reset();
  }

  protected submitClone(versionId: number): void {
    if (this.cloneForm.invalid) {
      this.cloneForm.markAllAsTouched();
      return;
    }
    const { newVersionName, newEffectiveAcademicYearId } = this.cloneForm.value;
    this.cloning.set(versionId);
    this.curriculumVersionService.clone(versionId, newVersionName, newEffectiveAcademicYearId).subscribe({
      next: () => {
        this.toast.success('Curriculum version cloned');
        this.cancelClone();
        this.cloning.set(null);
        this.loadVersions();
      },
      error: () => {
        this.toast.error('Failed to clone version');
        this.cloning.set(null);
      },
    });
  }

  private loadVersions(): void {
    const pid = this.selectedProgramId();
    if (!pid) return;
    this.loading.set(true);
    this.curriculumVersionService.getByProgram(pid).subscribe({
      next: (data) => {
        this.versions.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load curriculum versions');
        this.loading.set(false);
      },
    });
  }
}

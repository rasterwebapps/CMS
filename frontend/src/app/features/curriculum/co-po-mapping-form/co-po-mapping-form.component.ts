import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CurriculumService } from '../curriculum.service';
import { Experiment, LabCurriculumMappingRequest } from '../curriculum.model';
import { ToastService } from '../../../core/toast/toast.service';

@Component({
  selector: 'app-co-po-mapping-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule],
  templateUrl: './co-po-mapping-form.component.html',
  styleUrl: './co-po-mapping-form.component.scss',
})
export class CoPoMappingFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly curriculumService = inject(CurriculumService);
  private readonly toast = inject(ToastService);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Curriculum Mapping');
  protected readonly experiments = signal<Experiment[]>([]);
  protected readonly outcomeTypes = [
    'COURSE_OUTCOME',
    'PROGRAM_OUTCOME',
    'PROGRAM_SPECIFIC_OUTCOME'];
  protected readonly mappingLevels = ['LOW', 'MEDIUM', 'HIGH'];

  private itemId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    experimentId: [null, Validators.required],
    outcomeType: ['', Validators.required],
    outcomeCode: ['', [Validators.required, Validators.maxLength(50)]],
    outcomeDescription: [''],
    mappingLevel: ['', Validators.required],
    justification: [''],
  });

  ngOnInit(): void {
    this.curriculumService.getAllExperiments().subscribe({
      next: (data) => this.experiments.set(data),
      error: () => {
        this.toast.error('Failed to load experiments');
      },
    });
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.itemId = Number(id);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Curriculum Mapping');
      this.loading.set(true);
      this.curriculumService.getMappingById(this.itemId).subscribe({
        next: (item) => {
          this.form.patchValue({
            experimentId: item.experimentId,
            outcomeType: item.outcomeType,
            outcomeCode: item.outcomeCode,
            outcomeDescription: item.outcomeDescription || '',
            mappingLevel: item.mappingLevel,
            justification: item.justification || '',
          });
          this.loading.set(false);
        },
        error: () => {
          this.toast.error('Failed to load');
          void this.router.navigate(['/curriculum-mappings']);
        },
      });
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.value;
    const request: LabCurriculumMappingRequest = {
      experimentId: v.experimentId,
      outcomeType: v.outcomeType,
      outcomeCode: v.outcomeCode.trim(),
      outcomeDescription: v.outcomeDescription?.trim() || undefined,
      mappingLevel: v.mappingLevel,
      justification: v.justification?.trim() || undefined,
    };
    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.curriculumService.updateMapping(this.itemId!, request)
      : this.curriculumService.createMapping(request);
    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Updated' : 'Created');
        void this.router.navigate(['/curriculum-mappings']);
      },
      error: () => {
        this.toast.error('Failed to save');
        this.saving.set(false);
      },
    });
  }
}

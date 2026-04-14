import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { CurriculumService } from '../curriculum.service';
import { Experiment, LabCurriculumMappingRequest } from '../curriculum.model';

@Component({
  selector: 'app-co-po-mapping-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
  templateUrl: './co-po-mapping-form.component.html',
  styleUrl: './co-po-mapping-form.component.scss',
})
export class CoPoMappingFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly curriculumService = inject(CurriculumService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Curriculum Mapping');
  protected readonly experiments = signal<Experiment[]>([]);
  protected readonly outcomeTypes = [
    'COURSE_OUTCOME',
    'PROGRAM_OUTCOME',
    'PROGRAM_SPECIFIC_OUTCOME',
  ];
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
        this.snackBar.open('Failed to load experiments', 'Close', { duration: 3000 });
      },
    });
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.itemId = Number(id);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Curriculum Mapping');
      this.loading.set(true);
      this.curriculumService.getAllMappings().subscribe({
        next: (data) => {
          const item = data.find((m) => m.id === this.itemId);
          if (item) {
            this.form.patchValue({
              experimentId: item.experimentId,
              outcomeType: item.outcomeType,
              outcomeCode: item.outcomeCode,
              outcomeDescription: item.outcomeDescription || '',
              mappingLevel: item.mappingLevel,
              justification: item.justification || '',
            });
          }
          this.loading.set(false);
        },
        error: () => {
          this.snackBar.open('Failed to load', 'Close', { duration: 3000 });
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
        this.snackBar.open(this.isEditMode() ? 'Updated' : 'Created', 'Close', { duration: 3000 });
        void this.router.navigate(['/curriculum-mappings']);
      },
      error: () => {
        this.snackBar.open('Failed to save', 'Close', { duration: 3000 });
        this.saving.set(false);
      },
    });
  }
}

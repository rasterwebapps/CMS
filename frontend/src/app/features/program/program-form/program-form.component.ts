import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ProgramService } from '../program.service';
import { ProgramRequest, ProgramStatus } from '../program.model';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { PROGRAM_FORM_TOUR } from '../../../shared/tour/tours/program.tours';
import { CmsPreviewCardComponent } from '../../../shared/preview-card/preview-card.component';
import { CmsTipsCardComponent, CmsTip } from '../../../shared/tips-card/tips-card.component';

@Component({
  selector: 'app-program-form',
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
  templateUrl: './program-form.component.html',
  styleUrl: './program-form.component.scss',
})
export class ProgramFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly programService = inject(ProgramService);
  private readonly toast = inject(ToastService);
  private readonly tourService = inject(TourService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Program');

  // ── Live preview signals ────────────────────────────────────
  protected readonly previewName     = signal('');
  protected readonly previewCode     = signal('');
  protected readonly previewDuration = signal<number>(0);
  protected readonly previewStatus   = signal<ProgramStatus>('ACTIVE');

  protected readonly TIPS: CmsTip[] = [
    { icon: 'tag',         title: 'Unique Code',  subtitle: 'Use 3–6 uppercase letters as an identifier (e.g., BAC, MAS).' },
    { icon: 'event',       title: 'Duration',     subtitle: 'Number of years a student takes to complete the program.' },
    { icon: 'toggle_on',   title: 'Status',       subtitle: 'Inactive programs are hidden from new admissions but kept for historical records.' },
  ];

  private programId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    code: ['', [Validators.required, Validators.maxLength(20)]],
    durationYears: [null as number | null, [Validators.required, Validators.min(1), Validators.max(10)]],
    status: ['ACTIVE' as ProgramStatus, Validators.required],
  });

  constructor() {
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(v => {
        this.previewName.set((v.name ?? '').trim());
        this.previewCode.set((v.code ?? '').toUpperCase().trim());
        this.previewDuration.set(Number(v.durationYears) || 0);
        this.previewStatus.set((v.status ?? 'ACTIVE') as ProgramStatus);
      });
  }

  ngOnInit(): void {
    this.tourService.register('program-form', PROGRAM_FORM_TOUR);
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.programId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Program');
      this.loadProgram();
    }
  }

  protected onCodeInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const upper = input.value.toUpperCase();
    if (upper !== input.value) {
      this.form.get('code')?.setValue(upper, { emitEvent: true });
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const request: ProgramRequest = {
      name: (this.form.value.name ?? '').trim(),
      code: (this.form.value.code ?? '').trim(),
      durationYears: this.form.value.durationYears,
      status: this.form.value.status as ProgramStatus,
    };

    this.saving.set(true);

    const operation$ = this.isEditMode()
      ? this.programService.update(this.programId!, request)
      : this.programService.create(request);

    operation$.subscribe({
      next: () => {
        const message = this.isEditMode()
          ? 'Program updated successfully'
          : 'Program created successfully';
        this.toast.success(message);
        void this.router.navigate(['/programs']);
      },
      error: (err) => {
        const message = err?.error?.message
          ?? (this.isEditMode() ? 'Failed to update program' : 'Failed to create program');
        this.toast.error(message);
        this.saving.set(false);
      },
    });
  }

  protected getErrorMessage(fieldName: string): string {
    const control = this.form.get(fieldName);
    if (!control || !control.errors) {
      return '';
    }

    if (control.errors['required']) {
      return `${this.getFieldLabel(fieldName)} is required`;
    }
    if (control.errors['maxlength']) {
      const maxLength = control.errors['maxlength'].requiredLength;
      return `${this.getFieldLabel(fieldName)} must be at most ${maxLength} characters`;
    }
    if (control.errors['min']) {
      const min = control.errors['min'].min;
      return `${this.getFieldLabel(fieldName)} must be at least ${min}`;
    }
    if (control.errors['max']) {
      const max = control.errors['max'].max;
      return `${this.getFieldLabel(fieldName)} must be at most ${max}`;
    }

    return '';
  }

  private getFieldLabel(fieldName: string): string {
    const labels: Record<string, string> = {
      name: 'Name',
      code: 'Code',
      durationYears: 'Duration',
      status: 'Status',
    };
    return labels[fieldName] || fieldName;
  }

  private loadProgram(): void {
    if (!this.programId) return;

    this.loading.set(true);
    this.programService.getById(this.programId).subscribe({
      next: (program) => {
        this.form.patchValue({
          name: program.name,
          code: program.code,
          durationYears: program.durationYears,
          status: program.status,
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load program');
        void this.router.navigate(['/programs']);
      },
    });
  }
}

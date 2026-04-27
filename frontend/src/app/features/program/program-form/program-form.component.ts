import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ProgramService } from '../program.service';
import { ProgramRequest, ProgramStatus } from '../program.model';
import { ToastService } from '../../../core/toast/toast.service';

@Component({
  selector: 'app-program-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule],
  templateUrl: './program-form.component.html',
  styleUrl: './program-form.component.scss',
})
export class ProgramFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly programService = inject(ProgramService);
  private readonly toast = inject(ToastService);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Program');

  private programId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    code: ['', [Validators.required, Validators.maxLength(20)]],
    durationYears: [4, [Validators.required, Validators.min(1), Validators.max(10)]],
    status: ['ACTIVE' as ProgramStatus, Validators.required],
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.programId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Program');
      this.loadProgram();
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


import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { toSignal } from '@angular/core/rxjs-interop';
import { startWith } from 'rxjs/operators';
import { CmsPreviewCardComponent } from '../../../shared/preview-card/preview-card.component';
import { CmsTipsCardComponent } from '../../../shared/tips-card/tips-card.component';
import { DepartmentService } from '../department.service';
import { DepartmentRequest } from '../department.model';
import { computeInitials } from '../../../shared/utils/initials';

@Component({
  selector: 'app-department-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    CmsPreviewCardComponent,
    CmsTipsCardComponent,
  ],
  templateUrl: './department-form.component.html',
  styleUrl: './department-form.component.scss',
})
export class DepartmentFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly departmentService = inject(DepartmentService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  /** Brief success state on the submit button before navigation (~600ms). */
  protected readonly succeeded = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Department');

  static readonly SUCCESS_STATE_DURATION_MS = 600;

  // Live preview signals
  protected readonly previewCode = signal('');
  protected readonly previewName = signal('');
  protected readonly previewHod = signal('');
  protected readonly previewDesc = signal('');
  protected readonly previewDescription = this.previewDesc;
  protected readonly codeCharCount = signal(0);

  protected readonly hodInitials = computed(() => computeInitials(this.previewHod()) || '?');

  private departmentId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    code: ['', [Validators.required, Validators.maxLength(20)]],
    description: ['', [Validators.maxLength(500)]],
    hodName: ['', [Validators.maxLength(100)]],
  });

  constructor() {
    // Wire each form control to its preview signal so the live preview
    // updates as the user types. takeUntilDestroyed() unsubscribes
    // automatically when the component is destroyed.
    this.form.get('name')!.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((v: string | null) => this.previewName.set(v ?? ''));
    this.form.get('code')!.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((v: string | null) => this.previewCode.set((v ?? '').toUpperCase()));
    this.form.get('description')!.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((v: string | null) => this.previewDescription.set(v ?? ''));
    this.form.get('hodName')!.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((v: string | null) => this.previewHod.set(v ?? ''));
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.departmentId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Department');
      this.loadDepartment();
    }

    // Sync live preview from form value changes
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(v => {
        const code = (v.code ?? '').toUpperCase().trim();
        this.previewCode.set(code);
        this.codeCharCount.set(code.length);
        this.previewName.set((v.name ?? '').trim());
        this.previewHod.set((v.hodName ?? '').trim());
        this.previewDesc.set((v.description ?? '').trim());
      });
  }

  /** Auto-uppercase the code field on every keystroke, preserving cursor position. */
  protected onCodeInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const start = input.selectionStart ?? 0;
    const end = input.selectionEnd ?? 0;
    const upper = input.value.toUpperCase();
    if (upper !== input.value) {
      this.form.get('code')?.setValue(upper, { emitEvent: true });
      setTimeout(() => input.setSelectionRange(start, end), 0);
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const request: DepartmentRequest = {
      name: (this.form.value.name ?? '').trim(),
      code: (this.form.value.code ?? '').trim(),
      description: this.form.value.description?.trim() || undefined,
      hodName: this.form.value.hodName?.trim() || undefined,
    };

    this.saving.set(true);

    const operation$ = this.isEditMode()
      ? this.departmentService.update(this.departmentId!, request)
      : this.departmentService.create(request);

    operation$.subscribe({
      next: () => {
        const message = this.isEditMode()
          ? 'Department updated successfully'
          : 'Department created successfully';
        this.snackBar.open(message, 'Close', { duration: 3000 });
        // Show the brief success state on the submit button, then navigate.
        this.saving.set(false);
        this.succeeded.set(true);
        setTimeout(() => {
          void this.router.navigate(['/departments']);
        }, DepartmentFormComponent.SUCCESS_STATE_DURATION_MS);
      },
      error: (err) => {
        const message = err?.error?.message
          ?? (this.isEditMode() ? 'Failed to update department' : 'Failed to create department');
        this.snackBar.open(message, 'Close', { duration: 4000 });
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

    return '';
  }

  private getFieldLabel(fieldName: string): string {
    const labels: Record<string, string> = {
      name: 'Name',
      code: 'Code',
      description: 'Description',
      hodName: 'Head of Department',
    };
    return labels[fieldName] || fieldName;
  }

  private loadDepartment(): void {
    if (!this.departmentId) return;

    this.loading.set(true);
    this.departmentService.getById(this.departmentId).subscribe({
      next: (department) => {
        this.form.patchValue({
          name: department.name,
          code: department.code,
          description: department.description || '',
          hodName: department.hodName || '',
        });
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Failed to load department', 'Close', { duration: 4000 });
        void this.router.navigate(['/departments']);
      },
    });
  }
}

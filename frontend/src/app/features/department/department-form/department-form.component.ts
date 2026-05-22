import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { DepartmentService } from '../department.service';
import { DepartmentRequest } from '../department.model';
import { FacultyService } from '../../faculty/faculty.service';
import { Faculty } from '../../faculty/faculty.model';
import { computeInitials } from '../../../shared/utils/initials';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { DEPT_FORM_TOUR } from '../../../shared/tour/tours/department.tours';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';
import { noConsecutiveSpaces, noInternalSpaces, trimmedMinLength, cmsFieldError, stripSpaces } from '../../../shared/validators/cms-validators';
import { environment } from '../../../../environments';
import { uniqueFieldValidator } from '../../../shared/validators/unique-field.validator';

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
    CmsTourButtonComponent,
  ],
  templateUrl: './department-form.component.html',
  styleUrl: './department-form.component.scss',
})
export class DepartmentFormComponent implements OnInit {
  private readonly fb                = inject(FormBuilder);
  private readonly route             = inject(ActivatedRoute);
  private readonly router            = inject(Router);
  private readonly departmentService = inject(DepartmentService);
  private readonly facultyService    = inject(FacultyService);
  private readonly snackBar          = inject(MatSnackBar);
  private readonly destroyRef        = inject(DestroyRef);
  private readonly tourService       = inject(TourService);
  private readonly http              = inject(HttpClient);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly succeeded = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Department');
  protected readonly faculties        = signal<Faculty[]>([]);
  protected readonly facultiesLoading = signal(false);

  protected readonly previewCode = signal('');
  protected readonly previewName = signal('');
  protected readonly previewHod = signal('');
  protected readonly previewDesc = signal('');
  protected readonly codeCharCount = signal(0);

  /** HOD name received from server when loading in edit mode (used for fallback name-based pre-selection). */
  private hodNameFromServer: string | null = null;

  protected readonly hodInitials = computed(() => computeInitials(this.previewHod()) || '?');

  private static readonly SUCCESS_STATE_DURATION_MS = 600;
  private departmentId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, trimmedMinLength(2), Validators.maxLength(100), noConsecutiveSpaces()]],
    code: ['', [Validators.required, Validators.maxLength(20), noInternalSpaces()]],
    description: ['', [Validators.maxLength(500)]],
    hodFacultyId: [null as number | null],
  });

  constructor() {
    this.form.get('name')!.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((v: string | null) => this.previewName.set(v ?? ''));
    this.form.get('code')!.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((v: string | null) => this.previewCode.set((v ?? '').toUpperCase()));
    this.form.get('description')!.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((v: string | null) => this.previewDesc.set(v ?? ''));
    this.form.get('hodFacultyId')!.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((id: number | null) => this.updateHodPreview(id));
  }

  ngOnInit(): void {
    this.tourService.register('dept-form', DEPT_FORM_TOUR);
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.departmentId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Department');
      this.loadDepartment();
    }

    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(v => {
        const code = (v.code ?? '').toUpperCase().trim();
        this.previewCode.set(code);
        this.codeCharCount.set(code.length);
        this.previewName.set((v.name ?? '').trim());
        this.previewDesc.set((v.description ?? '').trim());
      });

    this.loadFaculties();
    this.setupUniquenessValidators();
  }

  private setupUniquenessValidators(): void {
    const nameCtrl = this.form.get('name');
    if (nameCtrl) {
      nameCtrl.setAsyncValidators(
        uniqueFieldValidator(this.http, `${environment.apiUrl}/departments/name-exists`, () => this.departmentId)
      );
      nameCtrl.updateValueAndValidity({ emitEvent: false });
    }
    const codeCtrl = this.form.get('code');
    if (codeCtrl) {
      codeCtrl.setAsyncValidators(
        uniqueFieldValidator(this.http, `${environment.apiUrl}/departments/code-exists`, () => this.departmentId)
      );
      codeCtrl.updateValueAndValidity({ emitEvent: false });
    }
  }

  private loadFaculties(): void {
    this.facultiesLoading.set(true);
    this.facultyService.getAll().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (list) => {
        this.faculties.set(list);
        this.facultiesLoading.set(false);
        // If no hodFacultyId is selected yet but we have the server's hodName, try to match by name
        const currentId = this.form.get('hodFacultyId')?.value;
        if (!currentId && this.hodNameFromServer) {
          const match = list.find(f => f.fullName === this.hodNameFromServer);
          if (match) {
            this.form.get('hodFacultyId')!.setValue(match.id, { emitEvent: true });
          }
        }
        // Re-evaluate preview after faculties load (covers edit-mode pre-selection)
        this.updateHodPreview(this.form.get('hodFacultyId')?.value);
      },
      error: () => this.facultiesLoading.set(false),
    });
  }

  private updateHodPreview(id: number | null | undefined): void {
    if (!id) { this.previewHod.set(''); return; }
    const faculty = this.faculties().find(f => f.id === id);
    this.previewHod.set(faculty?.fullName ?? '');
  }

  protected onCodeInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const start = input.selectionStart ?? 0;
    const end = input.selectionEnd ?? 0;
    // Strip all spaces (BR-29 CODE rule) then uppercase
    const cleaned = stripSpaces(input.value).toUpperCase();
    if (cleaned !== input.value) {
      this.form.get('code')?.setValue(cleaned, { emitEvent: true });
      setTimeout(() => input.setSelectionRange(start, end), 0);
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }

    const request: DepartmentRequest = {
      name: (this.form.value.name ?? '').trim(),
      code: (this.form.value.code ?? '').trim(),
      description: this.form.value.description?.trim() || undefined,
      hodFacultyId: this.form.value.hodFacultyId ?? undefined,
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

  private static readonly FIELD_LABELS: Record<string, string> = {
    name: 'Name', code: 'Code', description: 'Description', hodFacultyId: 'Head of Department',
  };

  protected getErrorMessage(fieldName: string): string {
    return cmsFieldError(this.form.get(fieldName), DepartmentFormComponent.FIELD_LABELS[fieldName] ?? fieldName);
  }

  private loadDepartment(): void {
    if (!this.departmentId) return;

    this.loading.set(true);
    this.departmentService.getById(this.departmentId).subscribe({
      next: (department) => {
        // Store hodName for fallback name-based pre-selection (hodFacultyId may be null)
        this.hodNameFromServer = department.hodName ?? null;
        const hodId = department.hodFacultyId ?? null;
        // If faculties are already loaded, try name-based fallback immediately
        if (!hodId && department.hodName) {
          const match = this.faculties().find(f => f.fullName === department.hodName);
          this.form.patchValue({
            name:         department.name,
            code:         department.code,
            description:  department.description || '',
            hodFacultyId: match ? match.id : null,
          });
        } else {
          this.form.patchValue({
            name:         department.name,
            code:         department.code,
            description:  department.description || '',
            hodFacultyId: hodId,
          });
        }
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Failed to load department', 'Close', { duration: 4000 });
        void this.router.navigate(['/departments']);
      },
    });
  }
}

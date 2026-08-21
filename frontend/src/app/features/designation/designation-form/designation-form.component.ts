import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DesignationService } from '../designation.service';
import { DesignationRequest } from '../designation.model';
import { ToastService } from '../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';
import { noConsecutiveSpaces, noInternalSpaces, trimmedMinLength, cmsFieldError, stripSpaces } from '../../../shared/validators/cms-validators';
import { environment } from '../../../../environments';
import { uniqueFieldValidator } from '../../../shared/validators/unique-field.validator';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { DESIGNATION_FORM_TOUR, DESIGNATION_FORM_FLOW_MAP } from '../../../shared/tour/tours/designation.tours';

@Component({
  selector: 'app-designation-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    CmsTourButtonComponent,
  ],
  templateUrl: './designation-form.component.html',
  styleUrl: './designation-form.component.scss',
})
export class DesignationFormComponent implements OnInit {
  private readonly fb                  = inject(FormBuilder);
  private readonly route               = inject(ActivatedRoute);
  private readonly router              = inject(Router);
  private readonly designationService  = inject(DesignationService);
  private readonly toast               = inject(ToastService);
  private readonly destroyRef          = inject(DestroyRef);
  private readonly http                = inject(HttpClient);
  private readonly tourService         = inject(TourService);

  protected readonly loading   = signal(false);
  protected readonly saving    = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Designation');

  protected readonly previewName = signal('');
  protected readonly previewCode = signal('');
  protected readonly previewDesc = signal('');
  protected readonly codeCharCount = signal(0);

  private designationId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name:        ['', [Validators.required, trimmedMinLength(2), Validators.maxLength(100), noConsecutiveSpaces()]],
    code:        ['', [Validators.required, Validators.maxLength(50), noInternalSpaces()]],
    description: ['', [Validators.maxLength(500)]],
    defaultWeeklyTeachingHours: [null, [Validators.min(0)]],
    defaultDailyTeachingHours: [null, [Validators.min(0)]],
    defaultContinuousTeachingHours: [null, [Validators.min(0)]],
  });

  constructor() {
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(v => {
        this.previewName.set((v.name ?? '').trim());
        const code = (v.code ?? '').toUpperCase().trim();
        this.previewCode.set(code);
        this.codeCharCount.set(code.length);
        this.previewDesc.set((v.description ?? '').trim());
      });
  }

  ngOnInit(): void {
    this.tourService.register('designation-form', DESIGNATION_FORM_TOUR);
    this.tourService.registerFlowMap('designation-form', DESIGNATION_FORM_FLOW_MAP);
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.designationId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Designation');
      this.loadDesignation();
    }
    this.setupUniquenessValidators();
  }

  private setupUniquenessValidators(): void {
    const nameCtrl = this.form.get('name');
    if (nameCtrl) {
      nameCtrl.setAsyncValidators(
        uniqueFieldValidator(this.http, `${environment.apiUrl}/designations/name-exists`, () => this.designationId)
      );
      nameCtrl.updateValueAndValidity({ emitEvent: false });
    }
    const codeCtrl = this.form.get('code');
    if (codeCtrl) {
      codeCtrl.setAsyncValidators(
        uniqueFieldValidator(this.http, `${environment.apiUrl}/designations/code-exists`, () => this.designationId)
      );
      codeCtrl.updateValueAndValidity({ emitEvent: false });
    }
  }

  protected onCodeInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const start = input.selectionStart ?? 0;
    const end   = input.selectionEnd ?? 0;
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

    const request: DesignationRequest = {
      name:        (this.form.value.name ?? '').trim(),
      code:        (this.form.value.code ?? '').trim().toUpperCase(),
      description: this.form.value.description?.trim() || undefined,
      defaultWeeklyTeachingHours: this.form.value.defaultWeeklyTeachingHours ?? undefined,
      defaultDailyTeachingHours: this.form.value.defaultDailyTeachingHours ?? undefined,
      defaultContinuousTeachingHours: this.form.value.defaultContinuousTeachingHours ?? undefined,
    };

    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.designationService.update(this.designationId!, request)
      : this.designationService.create(request);

    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Designation updated successfully' : 'Designation created successfully');
        this.saving.set(false);
        void this.router.navigate(['/designations']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? (this.isEditMode() ? 'Failed to update designation' : 'Failed to create designation'));
        this.saving.set(false);
      },
    });
  }

  private static readonly FIELD_LABELS: Record<string, string> = {
    name: 'Name', code: 'Code', description: 'Description',
    defaultWeeklyTeachingHours: 'Default weekly teaching hours',
    defaultDailyTeachingHours: 'Default daily teaching hours',
    defaultContinuousTeachingHours: 'Default continuous teaching hours',
  };

  protected getErrorMessage(fieldName: string): string {
    return cmsFieldError(this.form.get(fieldName), DesignationFormComponent.FIELD_LABELS[fieldName] ?? fieldName);
  }

  private loadDesignation(): void {
    if (!this.designationId) return;
    this.loading.set(true);
    this.designationService.getById(this.designationId).subscribe({
      next: (d) => {
        this.form.patchValue({
          name: d.name,
          code: d.code,
          description: d.description || '',
          defaultWeeklyTeachingHours: d.defaultWeeklyTeachingHours ?? null,
          defaultDailyTeachingHours: d.defaultDailyTeachingHours ?? null,
          defaultContinuousTeachingHours: d.defaultContinuousTeachingHours ?? null,
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load designation');
        void this.router.navigate(['/designations']);
      },
    });
  }
}

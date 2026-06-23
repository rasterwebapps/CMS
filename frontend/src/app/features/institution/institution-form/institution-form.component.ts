import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { InstitutionService } from '../institution.service';
import { InstitutionRequest } from '../institution.model';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsPreviewCardComponent } from '../../../shared/preview-card/preview-card.component';
import { CmsTipsCardComponent, CmsTip } from '../../../shared/tips-card/tips-card.component';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';
import {
  noConsecutiveSpaces,
  noInternalSpaces,
  trimmedMinLength,
  cmsFieldError,
  stripSpaces,
} from '../../../shared/validators/cms-validators';
import { environment } from '../../../../environments';
import { uniqueFieldValidator } from '../../../shared/validators/unique-field.validator';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { INSTITUTION_FORM_TOUR } from '../../../shared/tour/tours/institution.tours';

@Component({
  selector: 'app-institution-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatProgressSpinnerModule,
    MatSlideToggleModule,
    CmsPreviewCardComponent,
    CmsTipsCardComponent,
    CmsTourButtonComponent,
  ],
  templateUrl: './institution-form.component.html',
  styleUrl: './institution-form.component.scss',
})
export class InstitutionFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly institutionService = inject(InstitutionService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly http = inject(HttpClient);
  private readonly tourService = inject(TourService);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);

  protected readonly previewName = signal('');
  protected readonly previewCode = signal('');
  protected readonly previewActive = signal(true);

  protected readonly TIPS: CmsTip[] = [
    {
      icon: 'account_balance',
      title: 'Institution Name',
      subtitle: 'Use the official name of the sister-concern institution.',
    },
    {
      icon: 'tag',
      title: 'Short Code',
      subtitle: 'Codes are auto-uppercase and shown in staff referrer dropdowns.',
    },
    {
      icon: 'toggle_on',
      title: 'Active Status',
      subtitle: 'Inactive institutions stay in history but are hidden from new staff referrer forms.',
    },
  ];

  private itemId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name: [
      '',
      [Validators.required, trimmedMinLength(1), Validators.maxLength(100), noConsecutiveSpaces()],
    ],
    code: ['', [Validators.required, Validators.maxLength(50), noInternalSpaces()]],
    isActive: [true],
  });

  constructor() {
    this.form.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((v) => {
      const cleaned = stripSpaces(v.code ?? '').toUpperCase();
      if (cleaned !== (v.code ?? '')) {
        this.form.get('code')!.setValue(cleaned, { emitEvent: false });
      }

      this.previewName.set((v.name ?? '').trim());
      this.previewCode.set(cleaned);
      this.previewActive.set(!!v.isActive);
    });
  }

  ngOnInit(): void {
    this.tourService.register('institution-form', INSTITUTION_FORM_TOUR);
    const id = this.route.snapshot.paramMap.get('id');
    if (id && id !== 'new') {
      this.itemId = Number(id);
      this.isEditMode.set(true);
      this.loadItem(this.itemId);
    }
    this.setupUniquenessValidators();
  }

  private setupUniquenessValidators(): void {
    const nameCtrl = this.form.get('name');
    if (nameCtrl) {
      nameCtrl.setAsyncValidators(
        uniqueFieldValidator(this.http, `${environment.apiUrl}/institutions/name-exists`, () => this.itemId)
      );
      nameCtrl.updateValueAndValidity({ emitEvent: false });
    }
    const codeCtrl = this.form.get('code');
    if (codeCtrl) {
      codeCtrl.setAsyncValidators(
        uniqueFieldValidator(this.http, `${environment.apiUrl}/institutions/code-exists`, () => this.itemId)
      );
      codeCtrl.updateValueAndValidity({ emitEvent: false });
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }
    this.saving.set(true);
    const request = this.buildRequest();
    const op = this.isEditMode()
      ? this.institutionService.update(this.itemId!, request)
      : this.institutionService.create(request);
    op.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Institution updated' : 'Institution created');
        void this.router.navigate(['/institutions']);
      },
      error: (err) => {
        const msg =
          err?.error?.message ??
          (this.isEditMode() ? 'Failed to update institution' : 'Failed to create institution');
        this.toast.error(msg);
        this.saving.set(false);
      },
    });
  }

  private loadItem(id: number): void {
    this.loading.set(true);
    this.institutionService.getById(id).subscribe({
      next: (item) => {
        this.form.patchValue({ name: item.name, code: item.code, isActive: item.isActive });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load institution');
        this.loading.set(false);
      },
    });
  }

  protected getFieldError(field: string): string {
    const labels: Record<string, string> = { name: 'Name', code: 'Code' };
    return cmsFieldError(this.form.get(field), labels[field] ?? field);
  }

  private buildRequest(): InstitutionRequest {
    const v = this.form.value as InstitutionRequest & { isActive: boolean };
    return { name: v.name.trim(), code: v.code.trim(), isActive: v.isActive };
  }
}

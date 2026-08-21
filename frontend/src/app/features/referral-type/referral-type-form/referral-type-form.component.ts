import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { ReferralTypeService } from '../referral-type.service';
import { ReferralTypeRequest } from '../referral-type.model';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { REFERRAL_TYPE_FORM_TOUR, REFERRAL_TYPE_FORM_FLOW_MAP } from '../../../shared/tour/tours/referral-type.tours';
import { CmsPreviewCardComponent } from '../../../shared/preview-card/preview-card.component';
import { CmsTipsCardComponent, CmsTip } from '../../../shared/tips-card/tips-card.component';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';
import { noConsecutiveSpaces, noInternalSpaces, trimmedMinLength, cmsFieldError, stripSpaces } from '../../../shared/validators/cms-validators';
import { environment } from '../../../../environments';
import { uniqueFieldValidator } from '../../../shared/validators/unique-field.validator';

@Component({
  selector: 'app-referral-type-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSlideToggleModule,
    CmsTourButtonComponent,
    CmsPreviewCardComponent,
    CmsTipsCardComponent,
  ],
  templateUrl: './referral-type-form.component.html',
  styleUrl: './referral-type-form.component.scss',
})
export class ReferralTypeFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly referralTypeService = inject(ReferralTypeService);
  private readonly toast = inject(ToastService);
  private readonly tourService = inject(TourService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly http = inject(HttpClient);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Referral Type');
  protected readonly isSystemDefined = signal(false);

  // Preview signals
  protected readonly previewName    = signal('');
  protected readonly previewCode    = signal('');
  protected readonly previewDesc    = signal('');
  protected readonly previewHasComm = signal(false);
  protected readonly previewComm    = signal<number>(0);
  protected readonly previewActive  = signal(true);

  protected readonly TIPS: CmsTip[] = [
    { icon: 'tag',           title: 'Code',       subtitle: 'Use uppercase + underscores (e.g., WALK_IN, AGENT). Used by the API.' },
    { icon: 'currency_rupee',title: 'Commission', subtitle: 'When enabled, this amount is paid per successful admission via this referral.' },
    { icon: 'visibility',    title: 'Active',     subtitle: 'Inactive types are hidden from new enquiries but kept for historical records.' },
  ];

  private itemId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(255), trimmedMinLength(2), noConsecutiveSpaces()]],
    code: ['', [Validators.required, Validators.maxLength(50), Validators.pattern(/^[A-Z][A-Z0-9_]*$/), noInternalSpaces()]],
    hasCommission: [false],
    commissionAmount: [0, [Validators.min(0)]],
    description: [''],
    isActive: [true],
  });

  constructor() {
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        const v = this.form.getRawValue();
        this.previewName.set((v.name ?? '').trim());
        this.previewCode.set(stripSpaces(v.code ?? '').toUpperCase());
        this.previewDesc.set((v.description ?? '').trim());
        this.previewHasComm.set(!!v.hasCommission);
        this.previewComm.set(Number(v.commissionAmount) || 0);
        this.previewActive.set(!!v.isActive);

        // When commission is toggled off, zero out the stored amount so stale
        // values are never persisted to the backend with hasCommission = false.
        if (!v.hasCommission && Number(v.commissionAmount) !== 0) {
          this.form.get('commissionAmount')?.setValue(0, { emitEvent: false });
        }

        // Dynamically require commissionAmount > 0 when hasCommission is enabled.
        this.updateCommissionAmountValidator(!!v.hasCommission);
      });
  }

  private updateCommissionAmountValidator(hasCommission: boolean): void {
    const ctrl = this.form.get('commissionAmount');
    if (hasCommission) {
      ctrl?.setValidators([Validators.required, Validators.min(0.01)]);
    } else {
      ctrl?.setValidators([Validators.min(0)]);
    }
    ctrl?.updateValueAndValidity({ emitEvent: false });
  }

  protected onCodeInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const upper = input.value.toUpperCase();
    if (upper !== input.value) {
      this.form.get('code')?.setValue(upper, { emitEvent: true });
    }
  }

  ngOnInit(): void {
    this.tourService.register('referral-type-form', REFERRAL_TYPE_FORM_TOUR);
    this.tourService.registerFlowMap('referral-type-form', REFERRAL_TYPE_FORM_FLOW_MAP);
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.itemId = Number(id);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Referral Type');
      this.loading.set(true);
      this.referralTypeService.getReferralTypeById(this.itemId).subscribe({
        next: (item) => {
          this.isSystemDefined.set(item.isSystemDefined ?? false);
          this.form.patchValue({
            name: item.name,
            code: item.code,
            hasCommission: item.hasCommission,
            commissionAmount: item.commissionAmount,
            description: item.description,
            isActive: item.isActive,
          });
          if (item.isSystemDefined) {
            this.form.get('name')?.disable();
            this.form.get('code')?.disable();
            this.form.get('hasCommission')?.disable();
          }
          // Initialise commission validator based on loaded value
          this.updateCommissionAmountValidator(item.hasCommission ?? false);
          this.loading.set(false);
        },
        error: () => {
          this.toast.error('Failed to load');
          void this.router.navigate(['/referral-types']);
        },
      });
    }
    this.setupUniquenessValidators();
  }

  private setupUniquenessValidators(): void {
    const nameCtrl = this.form.get('name');
    if (nameCtrl) {
      nameCtrl.setAsyncValidators(
        uniqueFieldValidator(this.http, `${environment.apiUrl}/referral-types/name-exists`, () => this.itemId)
      );
      nameCtrl.updateValueAndValidity({ emitEvent: false });
    }
    const codeCtrl = this.form.get('code');
    if (codeCtrl) {
      codeCtrl.setAsyncValidators(
        uniqueFieldValidator(this.http, `${environment.apiUrl}/referral-types/code-exists`, () => this.itemId)
      );
      codeCtrl.updateValueAndValidity({ emitEvent: false });
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }
    const v = this.form.getRawValue();
    const request: ReferralTypeRequest = {
      name: v.name.trim(),
      code: v.code.trim(),
      hasCommission: v.hasCommission,
      commissionAmount: v.commissionAmount,
      description: v.description || undefined,
      isActive: v.isActive,
    };
    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.referralTypeService.updateReferralType(this.itemId!, request)
      : this.referralTypeService.createReferralType(request);
    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Updated' : 'Created');
        void this.router.navigate(['/referral-types']);
      },
      error: (err) => {
        const message = err?.error?.message ?? (this.isEditMode() ? 'Failed to update' : 'Failed to save');
        this.toast.error(message);
        this.saving.set(false);
      },
    });
  }

  protected getFieldError(field: string): string {
    const labels: Record<string, string> = {
      name: 'Name',
      code: 'Code',
      description: 'Description',
      commissionAmount: 'Commission Amount',
    };
    return cmsFieldError(this.form.get(field), labels[field] ?? field);
  }
}

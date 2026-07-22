import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CmsPreviewCardComponent } from '../../../shared/preview-card/preview-card.component';
import { HostelRoomTypeService } from '../hostel-room-type.service';
import { HostelRoomTypeRequest } from '../hostel-room-type.model';
import { ToastService } from '../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';
import { noConsecutiveSpaces, noInternalSpaces, trimmedMinLength, cmsFieldError, stripSpaces } from '../../../shared/validators/cms-validators';
import { environment } from '../../../../environments';
import { uniqueFieldValidator } from '../../../shared/validators/unique-field.validator';

@Component({
  selector: 'app-hostel-room-type-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatCheckboxModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    CmsPreviewCardComponent,
  ],
  templateUrl: './hostel-room-type-form.component.html',
  styleUrl: './hostel-room-type-form.component.scss',
})
export class HostelRoomTypeFormComponent implements OnInit {
  private readonly fb              = inject(FormBuilder);
  private readonly route           = inject(ActivatedRoute);
  private readonly router          = inject(Router);
  private readonly roomTypeService = inject(HostelRoomTypeService);
  private readonly toast           = inject(ToastService);
  private readonly destroyRef      = inject(DestroyRef);
  private readonly http            = inject(HttpClient);

  protected readonly loading    = signal(false);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle  = signal('Add Hostel Room Type');

  protected readonly previewName            = signal('');
  protected readonly previewCode            = signal('');
  protected readonly previewSharingCapacity = signal<number | null>(null);
  protected readonly previewIsAc            = signal(false);
  protected readonly previewFeeAmount       = signal<number | null>(null);
  protected readonly codeCharCount          = signal(0);

  private roomTypeId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name:               ['', [Validators.required, trimmedMinLength(2), Validators.maxLength(100), noConsecutiveSpaces()]],
    code:               ['', [Validators.required, Validators.maxLength(50), noInternalSpaces()]],
    sharingCapacity:    [null, [Validators.required, Validators.min(1)]],
    isAc:               [false],
    feeAmountPerYear:   [null, [Validators.required, Validators.min(0)]],
    description:        ['', [Validators.maxLength(500)]],
  });

  constructor() {
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(v => {
        this.previewName.set((v.name ?? '').trim());
        const code = (v.code ?? '').toUpperCase().trim();
        this.previewCode.set(code);
        this.codeCharCount.set(code.length);
        this.previewSharingCapacity.set(v.sharingCapacity ?? null);
        this.previewIsAc.set(!!v.isAc);
        this.previewFeeAmount.set(v.feeAmountPerYear ?? null);
      });
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.roomTypeId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Hostel Room Type');
      this.loadRoomType();
    }
    this.setupUniquenessValidators();
  }

  private setupUniquenessValidators(): void {
    const nameCtrl = this.form.get('name');
    if (nameCtrl) {
      nameCtrl.setAsyncValidators(
        uniqueFieldValidator(this.http, `${environment.apiUrl}/hostel-room-types/name-exists`, () => this.roomTypeId)
      );
      nameCtrl.updateValueAndValidity({ emitEvent: false });
    }
    const codeCtrl = this.form.get('code');
    if (codeCtrl) {
      codeCtrl.setAsyncValidators(
        uniqueFieldValidator(this.http, `${environment.apiUrl}/hostel-room-types/code-exists`, () => this.roomTypeId)
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

    const request: HostelRoomTypeRequest = {
      name:             (this.form.value.name ?? '').trim(),
      code:             (this.form.value.code ?? '').trim().toUpperCase(),
      sharingCapacity:  this.form.value.sharingCapacity,
      isAc:             !!this.form.value.isAc,
      feeAmountPerYear: this.form.value.feeAmountPerYear,
      description:      this.form.value.description?.trim() || undefined,
    };

    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.roomTypeService.update(this.roomTypeId!, request)
      : this.roomTypeService.create(request);

    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Hostel room type updated successfully' : 'Hostel room type created successfully');
        this.saving.set(false);
        void this.router.navigate(['/hostel-room-types']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? (this.isEditMode() ? 'Failed to update hostel room type' : 'Failed to create hostel room type'));
        this.saving.set(false);
      },
    });
  }

  private static readonly FIELD_LABELS: Record<string, string> = {
    name: 'Name', code: 'Code', sharingCapacity: 'Sharing capacity',
    feeAmountPerYear: 'Fee amount per year', description: 'Description',
  };

  protected getErrorMessage(fieldName: string): string {
    return cmsFieldError(this.form.get(fieldName), HostelRoomTypeFormComponent.FIELD_LABELS[fieldName] ?? fieldName);
  }

  private loadRoomType(): void {
    if (!this.roomTypeId) return;
    this.loading.set(true);
    this.roomTypeService.getById(this.roomTypeId).subscribe({
      next: (r) => {
        this.form.patchValue({
          name: r.name,
          code: r.code,
          sharingCapacity: r.sharingCapacity,
          isAc: r.isAc,
          feeAmountPerYear: r.feeAmountPerYear,
          description: r.description || '',
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load hostel room type');
        void this.router.navigate(['/hostel-room-types']);
      },
    });
  }
}

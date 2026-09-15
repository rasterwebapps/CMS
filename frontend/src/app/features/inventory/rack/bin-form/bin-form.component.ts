import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { InventoryRackService } from '../inventory-rack.service';
import { InventoryBinRequest, InventoryRack } from '../inventory-rack.model';
import { ToastService } from '../../../../core/toast/toast.service';
import { CmsPreviewCardComponent } from '../../../../shared/preview-card/preview-card.component';
import { CmsTipsCardComponent, CmsTip } from '../../../../shared/tips-card/tips-card.component';
import { scrollToFirstInvalid } from '../../../../shared/utils/scroll-to-invalid';
import {
  noConsecutiveSpaces,
  noInternalSpaces,
  trimmedMinLength,
  cmsFieldError,
  stripSpaces,
} from '../../../../shared/validators/cms-validators';
import { environment } from '../../../../../environments';
import { uniqueFieldValidator } from '../../../../shared/validators/unique-field.validator';

const BIN_FORM_IMPORTS = [
  RouterLink,
  ReactiveFormsModule,
  MatProgressSpinnerModule,
  MatSlideToggleModule,
  CmsPreviewCardComponent,
  CmsTipsCardComponent,
];

@Component({
  selector: 'app-inventory-bin-form',
  standalone: true,
  imports: BIN_FORM_IMPORTS,
  templateUrl: './bin-form.component.html',
  styleUrl: './bin-form.component.scss',
})
export class BinFormComponent implements OnInit {
  private readonly fb           = inject(FormBuilder);
  private readonly route        = inject(ActivatedRoute);
  private readonly router       = inject(Router);
  private readonly rackService  = inject(InventoryRackService);
  private readonly toast        = inject(ToastService);
  private readonly destroyRef   = inject(DestroyRef);
  private readonly http         = inject(HttpClient);

  protected readonly loading    = signal(true);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle  = signal('Add Bin');
  protected readonly rackName   = signal('');
  protected readonly rackCode   = signal('');
  protected readonly locationName = signal('');

  protected readonly previewName = signal('');
  protected readonly previewCode = signal('');
  protected readonly previewDesc = signal('');
  protected readonly previewActive = signal(true);

  protected readonly TIPS: CmsTip[] = [
    {
      icon: 'inventory_2',
      title: 'Bins hold the stock',
      subtitle: 'A bin is the leaf location a specific quantity of stock physically sits in, e.g. Bin 1, Bin 2.',
    },
    {
      icon: 'tag',
      title: 'Bin code',
      subtitle: 'A short, unique code within this rack, e.g. B-01. Spaces are removed automatically.',
    },
    {
      icon: 'visibility',
      title: 'Active status',
      subtitle: 'Inactive bins are hidden from new stock assignments but retained for existing records.',
    },
  ];

  private binId: number | null = null;
  private rackId!: number;

  protected readonly form: FormGroup = this.fb.group({
    name:        ['', [Validators.required, trimmedMinLength(2), Validators.maxLength(200), noConsecutiveSpaces()]],
    code:        ['', [Validators.required, Validators.maxLength(50), noInternalSpaces()]],
    description: ['', [Validators.maxLength(500)]],
    isActive:    [true],
  });

  constructor() {
    this.form.get('code')!.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((v: string) => {
      const cleaned = stripSpaces(v ?? '').toUpperCase();
      if (cleaned !== v) this.form.get('code')!.setValue(cleaned, { emitEvent: false });
    });

    this.form.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(v => {
      this.previewName.set((v.name ?? '').trim());
      this.previewCode.set(stripSpaces(v.code ?? '').toUpperCase());
      this.previewDesc.set((v.description ?? '').trim());
      this.previewActive.set(!!v.isActive);
    });
  }

  ngOnInit(): void {
    const rackIdParam = this.route.snapshot.paramMap.get('rackId');
    if (!rackIdParam) {
      void this.router.navigate(['/inventory/racks']);
      return;
    }
    this.rackId = Number(rackIdParam);

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.binId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Bin');
    }

    this.setupUniquenessValidators();

    this.rackService.getRackById(this.rackId).subscribe({
      next: (rack: InventoryRack) => {
        this.rackName.set(rack.name);
        this.rackCode.set(rack.code);
        this.locationName.set(rack.locationName ?? '');
        if (this.binId) {
          this.loadBin(this.binId);
        } else {
          this.loading.set(false);
        }
      },
      error: () => {
        this.toast.error('Failed to load rack details');
        void this.router.navigate(['/inventory/racks']);
      },
    });
  }

  private setupUniquenessValidators(): void {
    const nameCtrl = this.form.get('name');
    if (nameCtrl) {
      nameCtrl.setAsyncValidators(
        uniqueFieldValidator(
          this.http,
          `${environment.apiUrl}/inventory/bins/name-exists`,
          () => this.binId,
          () => ({ rackId: this.rackId }),
        ),
      );
      nameCtrl.updateValueAndValidity({ emitEvent: false });
    }
    const codeCtrl = this.form.get('code');
    if (codeCtrl) {
      codeCtrl.setAsyncValidators(
        uniqueFieldValidator(
          this.http,
          `${environment.apiUrl}/inventory/bins/code-exists`,
          () => this.binId,
          () => ({ rackId: this.rackId }),
        ),
      );
      codeCtrl.updateValueAndValidity({ emitEvent: false });
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }

    const request: InventoryBinRequest = {
      rackId:      this.rackId,
      name:        (this.form.value.name ?? '').trim(),
      code:        stripSpaces(this.form.value.code ?? '').toUpperCase(),
      description: this.form.value.description?.trim() || undefined,
      isActive:    this.form.value.isActive,
    };

    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.rackService.updateBin(this.binId!, request)
      : this.rackService.createBin(request);

    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Bin updated successfully' : 'Bin created successfully');
        this.saving.set(false);
        void this.router.navigate(['/inventory/racks', this.rackId, 'bins']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? (this.isEditMode() ? 'Failed to update bin' : 'Failed to create bin'));
        this.saving.set(false);
      },
    });
  }

  private static readonly FIELD_LABELS: Record<string, string> = {
    name: 'Name', code: 'Code', description: 'Description',
  };

  protected getFieldError(fieldName: string): string {
    return cmsFieldError(this.form.get(fieldName), BinFormComponent.FIELD_LABELS[fieldName] ?? fieldName);
  }

  protected get binsListLink(): unknown[] {
    return ['/inventory/racks', this.rackId, 'bins'];
  }

  private loadBin(id: number): void {
    this.rackService.getBinById(id).subscribe({
      next: (bin) => {
        this.form.patchValue({
          name: bin.name,
          code: bin.code,
          description: bin.description || '',
          isActive: bin.isActive,
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load bin');
        void this.router.navigate(['/inventory/racks', this.rackId, 'bins']);
      },
    });
  }
}

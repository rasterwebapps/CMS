import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { InventoryRackService } from '../inventory-rack.service';
import { InventoryRackRequest } from '../inventory-rack.model';
import { InventoryLocationService } from '../../location/inventory-location.service';
import { InventoryLocation } from '../../location/inventory-location.model';
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

const RACK_FORM_IMPORTS = [
  RouterLink,
  ReactiveFormsModule,
  MatProgressSpinnerModule,
  MatSlideToggleModule,
  CmsPreviewCardComponent,
  CmsTipsCardComponent,
];

@Component({
  selector: 'app-inventory-rack-form',
  standalone: true,
  imports: RACK_FORM_IMPORTS,
  templateUrl: './rack-form.component.html',
  styleUrl: './rack-form.component.scss',
})
export class RackFormComponent implements OnInit {
  private readonly fb             = inject(FormBuilder);
  private readonly route          = inject(ActivatedRoute);
  private readonly router         = inject(Router);
  private readonly rackService    = inject(InventoryRackService);
  private readonly locationService = inject(InventoryLocationService);
  private readonly toast          = inject(ToastService);
  private readonly destroyRef     = inject(DestroyRef);
  private readonly http           = inject(HttpClient);

  protected readonly loading    = signal(true);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle  = signal('Add Rack');
  protected readonly locations  = signal<InventoryLocation[]>([]);

  protected readonly previewName = signal('');
  protected readonly previewCode = signal('');
  protected readonly previewDesc = signal('');
  protected readonly previewActive = signal(true);
  protected readonly previewLocationName = signal('');

  protected readonly TIPS: CmsTip[] = [
    {
      icon: 'inventory_2',
      title: 'Rack code',
      subtitle: 'A short, unique code within this location, e.g. R-01. Spaces are removed automatically.',
    },
    {
      icon: 'shelves',
      title: 'Add bins next',
      subtitle: 'After saving this rack, add its storage bins from the rack list.',
    },
    {
      icon: 'visibility',
      title: 'Active status',
      subtitle: 'Inactive racks stay linked to existing bins but are hidden from new bin assignments.',
    },
  ];

  private rackId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    locationId:  [null as number | null, [Validators.required]],
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

    this.form.get('locationId')!.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.form.get('name')!.updateValueAndValidity();
      this.form.get('code')!.updateValueAndValidity();
    });

    this.form.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(v => {
      this.previewName.set((v.name ?? '').trim());
      this.previewCode.set(stripSpaces(v.code ?? '').toUpperCase());
      this.previewDesc.set((v.description ?? '').trim());
      this.previewActive.set(!!v.isActive);
      const location = this.locations().find(l => l.id === v.locationId);
      this.previewLocationName.set(location?.virtualName ?? '');
    });
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.rackId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Rack');
    }

    this.setupUniquenessValidators();

    this.locationService.getAll(true).subscribe({
      next: (locations) => {
        this.locations.set(locations);
        if (this.rackId) {
          this.loadRack(this.rackId);
        } else {
          this.loading.set(false);
        }
      },
      error: () => {
        this.toast.error('Failed to load inventory locations');
        this.loading.set(false);
      },
    });
  }

  private setupUniquenessValidators(): void {
    const nameCtrl = this.form.get('name');
    if (nameCtrl) {
      nameCtrl.setAsyncValidators(
        uniqueFieldValidator(
          this.http,
          `${environment.apiUrl}/inventory/racks/name-exists`,
          () => this.rackId,
          () => (this.form.value.locationId != null ? { locationId: this.form.value.locationId } : null),
        ),
      );
      nameCtrl.updateValueAndValidity({ emitEvent: false });
    }
    const codeCtrl = this.form.get('code');
    if (codeCtrl) {
      codeCtrl.setAsyncValidators(
        uniqueFieldValidator(
          this.http,
          `${environment.apiUrl}/inventory/racks/code-exists`,
          () => this.rackId,
          () => (this.form.value.locationId != null ? { locationId: this.form.value.locationId } : null),
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

    const request: InventoryRackRequest = {
      locationId:  this.form.value.locationId,
      name:        (this.form.value.name ?? '').trim(),
      code:        stripSpaces(this.form.value.code ?? '').toUpperCase(),
      description: this.form.value.description?.trim() || undefined,
      isActive:    this.form.value.isActive,
    };

    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.rackService.updateRack(this.rackId!, request)
      : this.rackService.createRack(request);

    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Rack updated successfully' : 'Rack created successfully');
        this.saving.set(false);
        void this.router.navigate(['/inventory/racks']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? (this.isEditMode() ? 'Failed to update rack' : 'Failed to create rack'));
        this.saving.set(false);
      },
    });
  }

  private static readonly FIELD_LABELS: Record<string, string> = {
    locationId: 'Location', name: 'Name', code: 'Code', description: 'Description',
  };

  protected getFieldError(fieldName: string): string {
    return cmsFieldError(this.form.get(fieldName), RackFormComponent.FIELD_LABELS[fieldName] ?? fieldName);
  }

  private loadRack(id: number): void {
    this.rackService.getRackById(id).subscribe({
      next: (rack) => {
        this.form.patchValue({
          locationId: rack.locationId,
          name: rack.name,
          code: rack.code,
          description: rack.description || '',
          isActive: rack.isActive,
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load rack');
        void this.router.navigate(['/inventory/racks']);
      },
    });
  }
}

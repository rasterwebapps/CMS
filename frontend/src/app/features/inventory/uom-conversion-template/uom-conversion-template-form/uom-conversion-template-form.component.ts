import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CmsPreviewCardComponent } from '../../../../shared/preview-card/preview-card.component';
import { UomConversionTemplateService } from '../uom-conversion-template.service';
import { UomConversionTemplateLevelRequest, UomConversionTemplateRequest } from '../uom-conversion-template.model';
import { UomService } from '../../uom/uom.service';
import { Uom } from '../../uom/uom.model';
import { ToastService } from '../../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../../shared/utils/scroll-to-invalid';
import { noConsecutiveSpaces, trimmedMinLength, cmsFieldError } from '../../../../shared/validators/cms-validators';
import { environment } from '../../../../../environments';
import { uniqueFieldValidator } from '../../../../shared/validators/unique-field.validator';

@Component({
  selector: 'app-uom-conversion-template-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    CmsPreviewCardComponent,
  ],
  templateUrl: './uom-conversion-template-form.component.html',
  styleUrl: './uom-conversion-template-form.component.scss',
})
export class UomConversionTemplateFormComponent implements OnInit {
  private readonly fb              = inject(FormBuilder);
  private readonly route           = inject(ActivatedRoute);
  private readonly router          = inject(Router);
  private readonly templateService = inject(UomConversionTemplateService);
  private readonly uomService      = inject(UomService);
  private readonly toast           = inject(ToastService);
  private readonly destroyRef      = inject(DestroyRef);
  private readonly http            = inject(HttpClient);

  protected readonly loading    = signal(false);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle  = signal('Add UOM Conversion Template');
  protected readonly uoms       = signal<Uom[]>([]);

  protected readonly previewName = signal('');

  private templateId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name:        ['', [Validators.required, trimmedMinLength(2), Validators.maxLength(150), noConsecutiveSpaces()]],
    description: ['', [Validators.maxLength(500)]],
    baseUomId:   [null as number | null, [Validators.required]],
    isActive:    [true],
    // Levels above the base — level 0 (baseUomId, factor 1) is always implied and never part of
    // this editable array, same convention as ProductFormComponent's Unit Hierarchy section.
    levels: this.fb.array([] as FormGroup[]),
  });

  get levels(): FormArray {
    return this.form.get('levels') as FormArray;
  }

  constructor() {
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(v => {
        this.previewName.set((v.name ?? '').trim());
      });
  }

  ngOnInit(): void {
    this.uomService.getAll(true).subscribe({ next: (u) => this.uoms.set(u) });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.templateId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit UOM Conversion Template');
      this.loadTemplate();
    }
    this.setupUniquenessValidators();
  }

  private setupUniquenessValidators(): void {
    const nameCtrl = this.form.get('name');
    nameCtrl?.setAsyncValidators(
      uniqueFieldValidator(this.http, `${environment.apiUrl}/inventory/uom-conversion-templates/name-exists`, () => this.templateId),
    );
    nameCtrl?.updateValueAndValidity({ emitEvent: false });
  }

  protected uomName(uomId: number | null): string {
    return this.uoms().find(u => u.id === uomId)?.name ?? '—';
  }

  private newLevelGroup(uomId: number | null = null, factorToBase: number | null = null, isDefaultPurchase = false): FormGroup {
    return this.fb.group({
      uomId: [uomId as number | null, [Validators.required]],
      factorToBase: [factorToBase as number | null, [Validators.required, Validators.min(0.000001)]],
      isDefaultPurchase: [isDefaultPurchase],
    });
  }

  protected addLevel(): void {
    this.levels.push(this.newLevelGroup());
  }

  protected removeLevel(index: number): void {
    this.levels.removeAt(index);
  }

  protected getErrorMessage(fieldName: string): string {
    const labels: Record<string, string> = { name: 'Name', baseUomId: 'Base unit of measure' };
    return cmsFieldError(this.form.get(fieldName), labels[fieldName] ?? fieldName);
  }

  protected onSubmit(): void {
    if (this.form.invalid || this.levels.invalid) {
      this.levels.markAllAsTouched();
      scrollToFirstInvalid(this.form);
      return;
    }
    const baseUomId = this.form.value.baseUomId;

    const levels: UomConversionTemplateLevelRequest[] = [
      { uomId: baseUomId, levelRank: 0, factorToBase: 1, isDefaultPurchase: false },
      ...this.levels.controls.map((g, i) => ({
        uomId: g.value.uomId,
        levelRank: i + 1,
        factorToBase: g.value.factorToBase,
        isDefaultPurchase: !!g.value.isDefaultPurchase,
      })),
    ];

    const request: UomConversionTemplateRequest = {
      name: (this.form.value.name ?? '').trim(),
      description: this.form.value.description?.trim() || undefined,
      baseUomId,
      isActive: this.form.value.isActive,
      levels,
    };

    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.templateService.update(this.templateId!, request)
      : this.templateService.create(request);

    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Template updated successfully' : 'Template created successfully');
        this.saving.set(false);
        void this.router.navigate(['/inventory/uom-conversion-templates']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? (this.isEditMode() ? 'Failed to update template' : 'Failed to create template'));
        this.saving.set(false);
      },
    });
  }

  private loadTemplate(): void {
    if (!this.templateId) return;
    this.loading.set(true);
    this.templateService.getById(this.templateId).subscribe({
      next: (t) => {
        this.form.patchValue({
          name: t.name,
          description: t.description || '',
          baseUomId: t.baseUomId,
          isActive: t.isActive,
        });
        this.levels.clear();
        const aboveBase = (t.levels ?? []).filter(l => l.levelRank > 0).sort((a, b) => a.levelRank - b.levelRank);
        for (const level of aboveBase) {
          this.levels.push(this.newLevelGroup(level.uomId, level.factorToBase, level.isDefaultPurchase));
        }
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load template');
        void this.router.navigate(['/inventory/uom-conversion-templates']);
      },
    });
  }
}
